package com.company.docs.storage.repository.sqlite;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.company.docs.common.enums.VersionStatusEnum;
import com.company.docs.common.exception.NotFoundException;
import com.company.docs.common.model.bo.ChunkBO;
import com.company.docs.common.model.bo.PageSnapshotBO;
import com.company.docs.common.model.bo.ScrapeResultBO;
import com.company.docs.common.model.bo.SearchHitBO;
import com.company.docs.common.util.JsonUtil;
import com.company.docs.storage.mapper.DocumentMapper;
import com.company.docs.storage.mapper.JobMapper;
import com.company.docs.storage.mapper.LibraryMapper;
import com.company.docs.storage.mapper.PageMapper;
import com.company.docs.storage.mapper.VersionMapper;
import com.company.docs.storage.model.entity.DocumentSearchRow;
import com.company.docs.storage.model.entity.JobDO;
import com.company.docs.storage.model.entity.LibraryDO;
import com.company.docs.storage.model.entity.LibraryVersionStatDO;
import com.company.docs.storage.model.entity.PageDO;
import com.company.docs.storage.model.entity.VersionDO;
import com.company.docs.storage.model.entity.VersionWithLibraryDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 存储门面服务。
 * <p>
 * 设计目的：
 * 1. 对上层屏蔽多表操作细节。
 * 2. 把“版本状态写穿、页面与分片一致性”固化在事务里。
 * 3. 对齐 TypeScript 版核心行为语义。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageFacadeService {

    private final LibraryMapper libraryMapper;
    private final VersionMapper versionMapper;
    private final PageMapper pageMapper;
    private final DocumentMapper documentMapper;
    private final JobMapper jobMapper;

    /**
     * 确保库与版本存在。
     *
     * @return versionId
     */
    @Transactional(rollbackFor = Exception.class)
    public Long ensureLibraryAndVersion(String library, String version) {
        Long libraryId = ensureLibrary(library);
        return ensureVersion(libraryId, normalizeVersion(version));
    }

    /**
     * 确保库存在。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long ensureLibrary(String library) {
        String normalizedLibrary = normalizeLibrary(library);
        LibraryDO libraryDO = libraryMapper.selectByNameIgnoreCase(normalizedLibrary);
        if (libraryDO != null) {
            return libraryDO.getId();
        }
        libraryMapper.insertIgnore(normalizedLibrary);
        LibraryDO inserted = libraryMapper.selectByNameIgnoreCase(normalizedLibrary);
        if (inserted == null) {
            throw new IllegalStateException("创建文档库失败: " + normalizedLibrary);
        }
        return inserted.getId();
    }

    /**
     * 确保版本存在。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long ensureVersion(Long libraryId, String version) {
        String normalizedVersion = normalizeVersion(version);
        VersionDO versionDO = versionMapper.selectByLibraryAndName(libraryId, normalizedVersion);
        if (versionDO != null) {
            return versionDO.getId();
        }
        versionMapper.insertIgnore(libraryId, normalizedVersion);
        VersionDO inserted = versionMapper.selectByLibraryAndName(libraryId, normalizedVersion);
        if (inserted == null) {
            throw new IllegalStateException("创建版本失败: libraryId=" + libraryId + ", version=" + normalizedVersion);
        }
        return inserted.getId();
    }

    /**
     * 获取文档库。
     */
    public LibraryDO getLibrary(String library) {
        return libraryMapper.selectByNameIgnoreCase(normalizeLibrary(library));
    }

    /**
     * 获取版本。
     */
    public VersionDO getVersion(String library, String version) {
        LibraryDO libraryDO = getLibrary(library);
        if (libraryDO == null) {
            return null;
        }
        return versionMapper.selectByLibraryAndName(libraryDO.getId(), normalizeVersion(version));
    }

    /**
     * 根据 ID 获取版本。
     */
    public VersionDO getVersionById(Long versionId) {
        return versionMapper.selectOneById(versionId);
    }

    /**
     * 更新版本状态。
     */
    public void updateVersionStatus(Long versionId, VersionStatusEnum status, String errorMessage) {
        versionMapper.updateStatus(versionId, status.name(), errorMessage);
    }

    /**
     * 更新版本进度。
     */
    public void updateVersionProgress(Long versionId, Integer pages, Integer maxPages) {
        versionMapper.updateProgress(versionId, pages, maxPages);
    }

    /**
     * 存储抓取参数，供刷新/恢复复用。
     */
    public void storeScraperOptions(Long versionId, String sourceUrl, Object options) {
        versionMapper.updateScraperOptions(versionId, sourceUrl, JsonUtil.toJson(options));
    }

    /**
     * 查询指定状态的版本。
     */
    public List<VersionWithLibraryDO> getVersionsByStatus(List<VersionStatusEnum> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        String statusSql = statuses.stream()
                .map(VersionStatusEnum::name)
                .map(s -> "'" + s + "'")
                .collect(Collectors.joining(","));
        return versionMapper.selectByStatusesRaw(statusSql);
    }

    /**
     * 根据版本 ID 获取页面快照列表。
     */
    public List<PageSnapshotBO> getPagesByVersionId(Long versionId) {
        List<PageDO> pages = pageMapper.selectByVersionId(versionId);
        List<PageSnapshotBO> snapshots = new ArrayList<>(pages.size());
        for (PageDO page : pages) {
            PageSnapshotBO snapshot = new PageSnapshotBO();
            snapshot.setPageId(page.getId());
            snapshot.setUrl(page.getUrl());
            snapshot.setEtag(page.getEtag());
            snapshot.setDepth(page.getDepth());
            snapshots.add(snapshot);
        }
        return snapshots;
    }

    /**
     * 添加抓取结果。
     */
    @Transactional(rollbackFor = Exception.class)
    public void addScrapeResult(String library, String version, Integer depth, ScrapeResultBO result) {
        Long versionId = ensureLibraryAndVersion(library, version);
        Long pageId = upsertPage(versionId, result.getUrl(), result.getTitle(), result.getEtag(),
                result.getLastModified(), result.getContentType(), depth);

        // 刷新时会先删再写，上层已保证；这里仍做一次兜底清理，避免重复分片。
        documentMapper.deleteByPageId(pageId);

        List<ChunkBO> chunks = result.getChunks();
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        int order = 0;
        for (ChunkBO chunk : chunks) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("level", chunk.getLevel());
            metadata.put("path", chunk.getPath());
            metadata.put("types", chunk.getTypes());
            documentMapper.insertOne(pageId, chunk.getContent(), JsonUtil.toJson(metadata), order++);
        }
    }

    /**
     * 删除某个页面及其分片。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deletePage(Long pageId) {
        documentMapper.deleteByPageId(pageId);
        pageMapper.deleteById(pageId);
    }

    /**
     * 清空指定库版本的页面与分片。
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeAllDocuments(String library, String version) {
        VersionDO versionDO = getVersion(library, version);
        if (versionDO == null) {
            return;
        }
        documentMapper.deleteByVersionId(versionDO.getId());
        pageMapper.delete(new QueryWrapper<PageDO>().eq("version_id", versionDO.getId()));
    }

    /**
     * 完整删除版本。
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeVersion(String library, String version) {
        LibraryDO libraryDO = getLibrary(library);
        if (libraryDO == null) {
            throw new NotFoundException("文档库不存在: " + library);
        }
        VersionDO versionDO = versionMapper.selectByLibraryAndName(libraryDO.getId(), normalizeVersion(version));
        if (versionDO == null) {
            throw new NotFoundException("版本不存在: " + library + "@" + normalizeVersion(version));
        }

        documentMapper.deleteByVersionId(versionDO.getId());
        pageMapper.delete(new QueryWrapper<PageDO>().eq("version_id", versionDO.getId()));
        versionMapper.deleteById(versionDO.getId());

        int remain = versionMapper.countByLibraryId(libraryDO.getId());
        if (remain == 0) {
            libraryMapper.deleteById(libraryDO.getId());
        }
    }

    /**
     * 检查指定库是否存在。
     */
    public boolean libraryExists(String library) {
        return getLibrary(library) != null;
    }

    /**
     * 检查指定库版本是否存在文档。
     */
    public boolean hasDocuments(String library, String version) {
        return documentMapper.countByLibraryVersion(normalizeLibrary(library), normalizeVersion(version)) > 0;
    }

    /**
     * 列出库版本统计信息。
     */
    public List<LibraryVersionStatDO> listLibraryVersionStats() {
        return versionMapper.queryLibraryVersions();
    }

    /**
     * 按库查询版本。
     */
    public List<VersionDO> listVersions(String library) {
        LibraryDO libraryDO = getLibrary(library);
        if (libraryDO == null) {
            return List.of();
        }
        return versionMapper.selectByLibraryId(libraryDO.getId());
    }

    /**
     * 检索文档。
     * <p>
     * 优先 FTS，失败后回退 LIKE，保证在 SQLite 裁剪场景下仍可工作。
     * </p>
     */
    public List<SearchHitBO> search(String library, String version, String query, Integer limit) {
        List<DocumentSearchRow> rows;
        try {
            rows = documentMapper.searchByFts(normalizeLibrary(library), normalizeVersion(version), query, limit);
        } catch (Exception ex) {
            log.warn("FTS 查询失败，降级 LIKE 检索。query={}", query, ex);
            rows = documentMapper.searchByLike(normalizeLibrary(library), normalizeVersion(version), query, limit);
        }
        return rows.stream().map(this::toSearchHit).collect(Collectors.toList());
    }

    /**
     * 新建任务记录。
     */
    public void saveJob(JobDO jobDO) {
        jobMapper.insert(jobDO);
    }

    /**
     * 更新任务状态。
     */
    public void updateJobStatus(String jobId, String status, String errorMessage) {
        jobMapper.updateStatus(jobId, status, errorMessage);
    }

    /**
     * 更新任务进度。
     */
    public void updateJobProgress(String jobId, Integer pages, Integer maxPages) {
        jobMapper.updateProgress(jobId, pages, maxPages);
    }

    /**
     * 查询任务。
     */
    public JobDO getJob(String jobId) {
        return jobMapper.selectById(jobId);
    }

    /**
     * 查询所有任务。
     */
    public List<JobDO> listJobs() {
        return jobMapper.selectAllJobs();
    }

    /**
     * 查询同库同版本的活动任务。
     */
    public List<JobDO> listActiveJobs(String library, String version) {
        VersionDO versionDO = getVersion(library, version);
        if (versionDO == null) {
            return List.of();
        }
        return jobMapper.selectActiveJobs(versionDO.getLibraryId(), versionDO.getId());
    }

    /**
     * 清理已结束任务。
     */
    public int clearFinishedJobs() {
        return jobMapper.clearFinishedJobs();
    }

    /**
     * 读取版本抓取参数。
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> readScraperOptions(Long versionId) {
        VersionDO versionDO = versionMapper.selectOneById(versionId);
        if (versionDO == null || versionDO.getScraperOptions() == null || versionDO.getScraperOptions().isBlank()) {
            return Map.of();
        }
        return JsonUtil.fromJson(versionDO.getScraperOptions(), Map.class);
    }

    /**
     * 根据版本 ID 查询关联文档库。
     */
    public LibraryDO getLibraryByVersionId(Long versionId) {
        VersionDO versionDO = versionMapper.selectOneById(versionId);
        if (versionDO == null) {
            return null;
        }
        return libraryMapper.selectById(versionDO.getLibraryId());
    }

    private Long upsertPage(Long versionId,
                            String url,
                            String title,
                            String etag,
                            String lastModified,
                            String contentType,
                            Integer depth) {
        pageMapper.upsert(versionId, url, title, etag, lastModified, contentType, depth == null ? 0 : depth);
        PageDO pageDO = pageMapper.selectByVersionAndUrl(versionId, url);
        if (pageDO == null) {
            throw new IllegalStateException("页面入库失败: " + url);
        }
        return pageDO.getId();
    }

    private SearchHitBO toSearchHit(DocumentSearchRow row) {
        SearchHitBO hitBO = new SearchHitBO();
        hitBO.setUrl(row.getUrl());
        hitBO.setTitle(row.getTitle());
        hitBO.setContent(row.getContent());
        hitBO.setScore(row.getScore());
        hitBO.setVersion(row.getVersion());
        return hitBO;
    }

    private String normalizeLibrary(String library) {
        return Objects.requireNonNullElse(library, "").trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeVersion(String version) {
        if (version == null || version.isBlank()) {
            return "";
        }
        return version.trim().toLowerCase(Locale.ROOT);
    }
}
