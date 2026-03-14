package com.company.docs.search.manager;

import com.company.docs.common.constant.CommonConstants;
import com.company.docs.common.exception.NotFoundException;
import com.company.docs.common.exception.ValidationException;
import com.company.docs.common.model.bo.SearchHitBO;
import com.company.docs.common.util.VersionUtil;
import com.company.docs.search.model.SearchResultEnvelopeBO;
import com.company.docs.storage.model.entity.VersionDO;
import com.company.docs.storage.repository.sqlite.StorageFacadeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 搜索管理器。
 * <p>
 * 语义对齐目标：
 * 1. exactMatch=true 时必须指定明确版本，且不能 latest。
 * 2. exactMatch=false 时自动计算最佳版本。
 * 3. 无匹配 semver 但存在无版本文档时，回退无版本检索。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchManager {

    private final StorageFacadeService storageFacadeService;

    /**
     * 执行搜索。
     */
    public SearchResultEnvelopeBO search(String library,
                                         String version,
                                         String query,
                                         Integer limit,
                                         Boolean exactMatch) {
        validateSearchInput(library, query, limit, exactMatch, version);

        String normalizedLibrary = library.trim().toLowerCase(Locale.ROOT);
        String normalizedVersion = version == null ? null : version.trim().toLowerCase(Locale.ROOT);
        int resolvedLimit = limit == null ? CommonConstants.DEFAULT_SEARCH_LIMIT : limit;
        boolean exact = Boolean.TRUE.equals(exactMatch);

        if (!storageFacadeService.libraryExists(normalizedLibrary)) {
            throw new NotFoundException("文档库不存在: " + normalizedLibrary);
        }

        String targetVersion;
        if (exact) {
            targetVersion = normalizeExactVersion(normalizedVersion);
        } else {
            targetVersion = findBestVersion(normalizedLibrary, normalizedVersion);
        }

        List<SearchHitBO> hits = storageFacadeService.search(normalizedLibrary, targetVersion, query.trim(), resolvedLimit);

        SearchResultEnvelopeBO envelope = new SearchResultEnvelopeBO();
        envelope.setResolvedVersion(targetVersion == null || targetVersion.isBlank() ? null : targetVersion);
        envelope.setResults(hits);
        return envelope;
    }

    /**
     * 查询最佳版本。
     */
    public String findBestVersion(String library, String requestedVersion) {
        List<VersionDO> versions = storageFacadeService.listVersions(library);
        boolean hasUnversioned = storageFacadeService.hasDocuments(library, "");

        List<String> semverVersions = new ArrayList<>();
        for (VersionDO versionDO : versions) {
            String name = versionDO.getName();
            if (name == null || name.isBlank()) {
                continue;
            }
            try {
                semverVersions.add(Objects.requireNonNull(VersionUtil.normalizeOrNull(name)));
            } catch (Exception ignored) {
                // 非语义化版本不参与自动匹配
            }
        }
        semverVersions = semverVersions.stream().distinct().sorted(VersionUtil.descComparator()).toList();

        if (requestedVersion == null || requestedVersion.isBlank() || "latest".equalsIgnoreCase(requestedVersion)) {
            if (!semverVersions.isEmpty()) {
                return semverVersions.get(0);
            }
            if (hasUnversioned) {
                return "";
            }
            throw new NotFoundException("未找到可用版本: " + library);
        }

        String request = requestedVersion.trim().toLowerCase(Locale.ROOT);

        // x-range 语义：优先匹配同主/次版本下的最新版本
        if (request.endsWith(".x")) {
            String candidate = semverVersions.stream()
                    .filter(v -> VersionUtil.matches(v, request))
                    .sorted(VersionUtil.descComparator())
                    .findFirst()
                    .orElse(null);
            if (candidate != null) {
                return candidate;
            }
            if (hasUnversioned) {
                return "";
            }
            throw new NotFoundException("未匹配到版本: " + library + "@" + request);
        }

        // 明确版本：优先选择 <= 请求版本 的最高版本（向后兼容策略）
        try {
            String normalizedRequest = VersionUtil.normalizeOrNull(request);
            if (normalizedRequest != null) {
                String candidate = semverVersions.stream()
                        .filter(v -> VersionUtil.compareSemantic(v, normalizedRequest) <= 0)
                        .sorted(VersionUtil.descComparator())
                        .findFirst()
                        .orElse(null);
                if (candidate != null) {
                    return candidate;
                }
            }
        } catch (Exception ignored) {
            // 版本格式非法时保留回退逻辑
        }

        if (hasUnversioned) {
            return "";
        }

        throw new NotFoundException("未匹配到版本: " + library + "@" + request);
    }

    private void validateSearchInput(String library,
                                     String query,
                                     Integer limit,
                                     Boolean exactMatch,
                                     String version) {
        if (library == null || library.isBlank()) {
            throw new ValidationException("library 不能为空");
        }
        if (query == null || query.isBlank()) {
            throw new ValidationException("query 不能为空");
        }
        if (limit != null && (limit < 1 || limit > CommonConstants.MAX_SEARCH_LIMIT)) {
            throw new ValidationException("limit 必须在 1-100 之间");
        }
        if (Boolean.TRUE.equals(exactMatch) && (version == null || version.isBlank() || "latest".equalsIgnoreCase(version))) {
            throw new ValidationException("exactMatch=true 时必须提供明确版本，且不能为 latest");
        }
    }

    private String normalizeExactVersion(String version) {
        try {
            return VersionUtil.normalizeOrNull(version);
        } catch (Exception ex) {
            throw new ValidationException("exactMatch 版本格式非法: " + version);
        }
    }
}
