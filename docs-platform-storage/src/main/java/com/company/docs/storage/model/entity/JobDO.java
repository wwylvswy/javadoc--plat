package com.company.docs.storage.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务实体。
 * <p>
 * 任务是运行态对象的持久化投影，用于重启恢复和进度观测。
 * </p>
 */
@Data
@TableName("jobs")
public class JobDO {

    @TableId
    private String id;

    @TableField("library_id")
    private Long libraryId;

    @TableField("version_id")
    private Long versionId;

    private String status;

    @TableField("progress_pages")
    private Integer progressPages;

    @TableField("progress_max_pages")
    private Integer progressMaxPages;

    @TableField("error_message")
    private String errorMessage;

    @TableField("source_url")
    private String sourceUrl;

    @TableField("scraper_options")
    private String scraperOptions;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("finished_at")
    private LocalDateTime finishedAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
