package com.company.docs.storage.model.entity;

import lombok.Data;

/**
 * 库与版本聚合统计行。
 */
@Data
public class LibraryVersionStatDO {

    private String library;

    private String version;

    private Long versionId;

    private String status;

    private Integer progressPages;

    private Integer progressMaxPages;

    private String sourceUrl;

    private String indexedAt;

    private Long documentCount;

    private Long uniqueUrlCount;
}
