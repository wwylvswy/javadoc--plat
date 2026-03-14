package com.company.docs.storage.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 页面实体。
 */
@Data
@TableName("pages")
public class PageDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("version_id")
    private Long versionId;

    private String url;

    private String title;

    private String etag;

    @TableField("last_modified")
    private String lastModified;

    @TableField("content_type")
    private String contentType;

    private Integer depth;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
