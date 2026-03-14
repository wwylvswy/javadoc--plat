package com.company.docs.task.manager;

import com.company.docs.common.enums.JobStatusEnum;
import com.company.docs.common.enums.VersionStatusEnum;
import com.company.docs.common.exception.NotFoundException;
import com.company.docs.common.exception.ValidationException;
import com.company.docs.common.model.bo.PageSnapshotBO;
import com.company.docs.common.util.JsonUtil;
import com.company.docs.scraper.manager.ScraperManager;
import com.company.docs.scraper.model.QueueItemBO;
import com.company.docs.scraper.model.ScrapeProgressBO;
import com.company.docs.scraper.model.ScraperOptionsBO;
import com.company.docs.storage.model.entity.JobDO;
import com.company.docs.storage.model.entity.LibraryDO;
import com.company.docs.storage.model.entity.VersionDO;
import com.company.docs.storage.model.entity.VersionWithLibraryDO;
import com.company.docs.storage.repository.sqlite.StorageFacadeService;
import com.company.docs.task.model.PipelineJobContext;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Pipeline 任务管理器。
 * <p>
 * 核心职责：
 * 1. 维护队列与并发度。
 * 2. 管理任务状态机与取消语义。
 * 3. 将状态/进度写穿到 versions/jobs 表。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineManager {

    private final StorageFacadeService storageFacadeService;
    private final ScraperManager scraperManager;

    @Qualifier("pipelineTaskExecutor")
    private final Executor pipelineTaskExecutor;

    private final Map<String, PipelineJobContext> jobMap = new ConcurrentHashMap<>();
    private final Deque<String> queue = new ArrayDeque<>();
    private final Map<String, Boolean> running = new ConcurrentHashMap<>();

    private volatile int maxConcurrency = 4;

    /**
     * 服务启动后恢复中断任务。
     */
    @PostConstruct
    public void recoverInterruptedJobs() {
        try {
            List<VersionWithLibraryDO> interrupted = storageFacadeService.getVersionsByStatus(
                    List.of(VersionStatusEnum.QUEUED, VersionStatusEnum.RUNNING)
            );
            if (interrupted.isEmpty()) {
                return;
            }
            log.info("检测到中断版本任务 {} 个，开始恢复。", interrupted.size());
            for (VersionWithLibraryDO item : interrupted) {
                try {
                    enqueueRefreshJob(item.getLibraryName(), item.getName());
                } catch (Exception ex) {
                    storageFacadeService.updateVersionStatus(item.getId(), VersionStatusEnum.FAILED,
                            "恢复失败: " + ex.getMessage());
                }
            }
        } catch (Exception ex) {
            log.error("恢复中断任务失败", ex);
        }
    }

    /**
     * 入队抓取任务。
     */
    public synchronized String enqueueScrapeJob(String library, String version, ScraperOptionsBO options) {
        String normalizedLibrary = normalizeLibrary(library);
        String normalizedVersion = normalizeVersion(version);

        if (normalizedLibrary.isBlank()) {
            throw new ValidationException("library 不能为空");
        }
        if (options == null || options.getUrl() == null || options.getUrl().isBlank()) {
            throw new ValidationException("url 不能为空");
        }

        Long versionId = storageFacadeService.ensureLibraryAndVersion(normalizedLibrary, normalizedVersion);

        // 同库同版本去重：取消旧任务，保留最新用户意图。
        cancelDuplicateJobs(normalizedLibrary, normalizedVersion);

        String jobId = UUID.randomUUID().toString();
        PipelineJobContext context = new PipelineJobContext();
        context.setJobId(jobId);
        context.setLibrary(normalizedLibrary);
        context.setVersion(normalizedVersion);
        context.setVersionId(versionId);
        context.setStatus(JobStatusEnum.QUEUED);
        context.setOptions(options);
        context.setPagesScraped(0);
        context.setTotalPages(0);
        context.setCreatedAt(LocalDateTime.now());
        context.setCompletionFuture(new CompletableFuture<>());
        context.setCancelled(new AtomicBoolean(false));

        jobMap.put(jobId, context);
        queue.offer(jobId);

        JobDO jobDO = buildJobDO(context);
        jobDO.setSourceUrl(options.getUrl());
        jobDO.setScraperOptions(JsonUtil.toJson(options));
        storageFacadeService.saveJob(jobDO);

        // 存储抓取参数，供 refresh/recover 复用。
        storageFacadeService.storeScraperOptions(versionId, options.getUrl(), options);

        updateStatus(context, JobStatusEnum.QUEUED, null);
        dispatch();
        return jobId;
    }

    /**
     * 入队刷新任务。
     */
    public synchronized String enqueueRefreshJob(String library, String version) {
        String normalizedLibrary = normalizeLibrary(library);
        String normalizedVersion = normalizeVersion(version);

        VersionDO versionDO = storageFacadeService.getVersion(normalizedLibrary, normalizedVersion);
        if (versionDO == null) {
            throw new NotFoundException("待刷新版本不存在: " + normalizedLibrary + "@" + normalizedVersion);
        }

        List<PageSnapshotBO> pages = storageFacadeService.getPagesByVersionId(versionDO.getId());
        if (pages.isEmpty()) {
            throw new ValidationException("版本下无历史页面，无法刷新: " + normalizedLibrary + "@" + normalizedVersion);
        }

        Map<String, Object> storedOptions = storageFacadeService.readScraperOptions(versionDO.getId());
        ScraperOptionsBO options = mergeRefreshOptions(storedOptions, normalizedLibrary, normalizedVersion, pages);
        return enqueueScrapeJob(normalizedLibrary, normalizedVersion, options);
    }

    /**
     * 查询任务。
     */
    public JobDO getJob(String jobId) {
        return storageFacadeService.getJob(jobId);
    }

    /**
     * 查询任务列表。
     */
    public List<JobDO> getJobs() {
        return storageFacadeService.listJobs();
    }

    /**
     * 等待任务完成。
     */
    public void waitForJobCompletion(String jobId) {
        PipelineJobContext context = jobMap.get(jobId);
        if (context == null) {
            JobDO job = storageFacadeService.getJob(jobId);
            if (job == null) {
                throw new NotFoundException("任务不存在: " + jobId);
            }
            if (isFinishedStatus(job.getStatus())) {
                return;
            }
            throw new ValidationException("任务仍在运行但运行态上下文缺失，请稍后重试");
        }
        context.getCompletionFuture().join();
    }

    /**
     * 取消任务。
     */
    public synchronized void cancelJob(String jobId) {
        PipelineJobContext context = jobMap.get(jobId);
        if (context == null) {
            return;
        }

        if (context.getStatus() == JobStatusEnum.QUEUED) {
            queue.remove(jobId);
            context.setCancelled(new AtomicBoolean(true));
            updateStatus(context, JobStatusEnum.CANCELLED, null);
            context.getCompletionFuture().complete(null);
            return;
        }

        if (context.getStatus() == JobStatusEnum.RUNNING) {
            context.getCancelled().set(true);
            updateStatus(context, JobStatusEnum.CANCELLING, null);
        }
    }

    /**
     * 清理结束任务。
     */
    public int clearCompletedJobs() {
        return storageFacadeService.clearFinishedJobs();
    }

    private synchronized void cancelDuplicateJobs(String library, String version) {
        for (PipelineJobContext context : jobMap.values()) {
            if (!Objects.equals(context.getLibrary(), library) || !Objects.equals(context.getVersion(), version)) {
                continue;
            }
            if (context.getStatus() == JobStatusEnum.QUEUED || context.getStatus() == JobStatusEnum.RUNNING) {
                cancelJob(context.getJobId());
            }
        }
    }

    private synchronized void dispatch() {
        while (running.size() < maxConcurrency && !queue.isEmpty()) {
            String jobId = queue.poll();
            if (jobId == null) {
                continue;
            }
            PipelineJobContext context = jobMap.get(jobId);
            if (context == null || context.getStatus() != JobStatusEnum.QUEUED) {
                continue;
            }
            running.put(jobId, Boolean.TRUE);
            pipelineTaskExecutor.execute(() -> runJob(context));
        }
    }

    private void runJob(PipelineJobContext context) {
        try {
            updateStatus(context, JobStatusEnum.RUNNING, null);

            // 非刷新且 clean=true 时先清空。
            if (!Boolean.TRUE.equals(context.getOptions().getRefresh())
                    && !Boolean.FALSE.equals(context.getOptions().getClean())) {
                storageFacadeService.removeAllDocuments(context.getLibrary(), context.getVersion());
            }

            AtomicBoolean cancelled = context.getCancelled();
            scraperManager.scrape(context.getOptions(), progress -> handleProgress(context, progress), cancelled);

            if (cancelled.get()) {
                updateStatus(context, JobStatusEnum.CANCELLED, null);
            } else {
                updateStatus(context, JobStatusEnum.COMPLETED, null);
            }
            context.getCompletionFuture().complete(null);
        } catch (Exception ex) {
            log.error("任务执行失败: {}", context.getJobId(), ex);
            updateStatus(context, JobStatusEnum.FAILED, ex.getMessage());
            context.getCompletionFuture().completeExceptionally(ex);
        } finally {
            running.remove(context.getJobId());
            dispatch();
        }
    }

    private void handleProgress(PipelineJobContext context, ScrapeProgressBO progress) {
        storageFacadeService.updateJobProgress(context.getJobId(), progress.getPagesScraped(), progress.getTotalPages());
        storageFacadeService.updateVersionProgress(context.getVersionId(), progress.getPagesScraped(), progress.getTotalPages());

        context.setPagesScraped(progress.getPagesScraped());
        context.setTotalPages(progress.getTotalPages());

        if (Boolean.TRUE.equals(progress.getDeleted()) && progress.getPageId() != null) {
            storageFacadeService.deletePage(progress.getPageId());
            return;
        }

        if (progress.getResult() != null) {
            if (progress.getPageId() != null) {
                // 刷新场景先删旧页再写新页。
                storageFacadeService.deletePage(progress.getPageId());
            }
            storageFacadeService.addScrapeResult(
                    context.getLibrary(),
                    context.getVersion(),
                    progress.getDepth(),
                    progress.getResult()
            );
        }
    }

    private void updateStatus(PipelineJobContext context, JobStatusEnum status, String errorMessage) {
        context.setStatus(status);
        context.setErrorMessage(errorMessage);

        if (status == JobStatusEnum.RUNNING && context.getStartedAt() == null) {
            context.setStartedAt(LocalDateTime.now());
        }
        if (status == JobStatusEnum.COMPLETED || status == JobStatusEnum.FAILED || status == JobStatusEnum.CANCELLED) {
            context.setFinishedAt(LocalDateTime.now());
        }

        storageFacadeService.updateJobStatus(context.getJobId(), status.name(), errorMessage);
        storageFacadeService.updateVersionStatus(context.getVersionId(), toVersionStatus(status), errorMessage);
    }

    private VersionStatusEnum toVersionStatus(JobStatusEnum jobStatus) {
        return switch (jobStatus) {
            case QUEUED -> VersionStatusEnum.QUEUED;
            case RUNNING, CANCELLING -> VersionStatusEnum.RUNNING;
            case COMPLETED -> VersionStatusEnum.COMPLETED;
            case FAILED -> VersionStatusEnum.FAILED;
            case CANCELLED -> VersionStatusEnum.CANCELLED;
        };
    }

    private ScraperOptionsBO mergeRefreshOptions(Map<String, Object> storedOptions,
                                                 String library,
                                                 String version,
                                                 List<PageSnapshotBO> pages) {
        ScraperOptionsBO options = new ScraperOptionsBO();
        options.setLibrary(library);
        options.setVersion(version);

        Object sourceUrl = storedOptions.get("url");
        String fallbackUrl = pages.get(0).getUrl();
        options.setUrl(sourceUrl instanceof String s && !s.isBlank() ? s : fallbackUrl);

        options.setMaxPages(toInt(storedOptions.get("maxPages"), pages.size()));
        options.setMaxDepth(toInt(storedOptions.get("maxDepth"), 2));
        options.setFollowRedirects(toBoolean(storedOptions.get("followRedirects"), true));
        options.setIgnoreErrors(toBoolean(storedOptions.get("ignoreErrors"), true));
        options.setClean(false);
        options.setRefresh(true);

        Object scope = storedOptions.get("scope");
        if (scope instanceof String scopeStr) {
            options.setScope(com.company.docs.common.enums.CrawlScopeEnum.from(scopeStr));
        }

        Object include = storedOptions.get("includePatterns");
        if (include instanceof List<?> includeList) {
            options.setIncludePatterns(includeList.stream().map(String::valueOf).toList());
        }

        Object exclude = storedOptions.get("excludePatterns");
        if (exclude instanceof List<?> excludeList) {
            options.setExcludePatterns(excludeList.stream().map(String::valueOf).toList());
        }

        Object headers = storedOptions.get("headers");
        if (headers instanceof Map<?, ?> headerMap) {
            Map<String, String> casted = new ConcurrentHashMap<>();
            headerMap.forEach((k, v) -> casted.put(String.valueOf(k), String.valueOf(v)));
            options.setHeaders(casted);
        }

        List<QueueItemBO> initialQueue = new ArrayList<>();
        for (PageSnapshotBO page : pages) {
            QueueItemBO item = new QueueItemBO();
            item.setUrl(page.getUrl());
            item.setDepth(page.getDepth() == null ? 0 : page.getDepth());
            item.setPageId(page.getPageId());
            item.setEtag(page.getEtag());
            initialQueue.add(item);
        }
        options.setInitialQueue(initialQueue);
        return options;
    }

    private JobDO buildJobDO(PipelineJobContext context) {
        JobDO jobDO = new JobDO();
        jobDO.setId(context.getJobId());
        LibraryDO libraryDO = storageFacadeService.getLibrary(context.getLibrary());
        if (libraryDO != null) {
            jobDO.setLibraryId(libraryDO.getId());
        }
        jobDO.setVersionId(context.getVersionId());
        jobDO.setStatus(context.getStatus().name());
        jobDO.setProgressPages(0);
        jobDO.setProgressMaxPages(0);
        jobDO.setCreatedAt(context.getCreatedAt());
        jobDO.setUpdatedAt(context.getCreatedAt());
        return jobDO;
    }

    private String normalizeLibrary(String library) {
        return library == null ? "" : library.trim().toLowerCase();
    }

    private String normalizeVersion(String version) {
        return version == null ? "" : version.trim().toLowerCase();
    }

    private Integer toInt(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private Boolean toBoolean(Object value, Boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private boolean isFinishedStatus(String status) {
        return Objects.equals(status, JobStatusEnum.COMPLETED.name())
                || Objects.equals(status, JobStatusEnum.FAILED.name())
                || Objects.equals(status, JobStatusEnum.CANCELLED.name());
    }
}
