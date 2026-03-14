package com.company.docs.storage.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档分片实体。
 */
@Data
@TableName("documents")
public class DocumentDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("page_id")
    private Long pageId;

    private String content;

    /**
     * 分片元数据 JSON。
     */
    private String metadata;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
