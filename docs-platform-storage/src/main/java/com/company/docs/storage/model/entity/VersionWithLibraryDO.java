package com.company.docs.storage.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 版本与库名的联表结果。
 */
@Data
public class VersionWithLibraryDO {

    private Long id;

    private Long libraryId;

    private String name;

    private String status;

    private Integer progressPages;

    private Integer progressMaxPages;

    private String errorMessage;

    private String sourceUrl;

    private String scraperOptions;

    private LocalDateTime startedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String libraryName;
}
