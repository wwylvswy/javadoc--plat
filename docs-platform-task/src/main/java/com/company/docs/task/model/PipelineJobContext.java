package com.company.docs.task.model;

import com.company.docs.common.enums.JobStatusEnum;
import com.company.docs.scraper.model.ScraperOptionsBO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 任务运行态上下文。
 */
@Data
public class PipelineJobContext {

    private String jobId;

    private String library;

    private String version;

    private Long versionId;

    private JobStatusEnum status;

    private ScraperOptionsBO options;

    private Integer pagesScraped;

    private Integer totalPages;

    private String errorMessage;

    private LocalDateTime createdAt;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private CompletableFuture<Void> completionFuture;

    private AtomicBoolean cancelled;
}
