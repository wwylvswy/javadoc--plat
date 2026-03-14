package com.company.docs.storage.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 版本实体。
 */
@Data
@TableName("versions")
public class VersionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("library_id")
    private Long libraryId;

    /**
     * 版本号。
     * 空字符串表示无版本文档。
     */
    private String name;

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

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
