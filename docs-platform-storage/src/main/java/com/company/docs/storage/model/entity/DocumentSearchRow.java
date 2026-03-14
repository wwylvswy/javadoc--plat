package com.company.docs.storage.model.entity;

import lombok.Data;

/**
 * 文档检索结果行。
 */
@Data
public class DocumentSearchRow {

    private String url;

    private String title;

    private String content;

    private Double score;

    private String version;
}
