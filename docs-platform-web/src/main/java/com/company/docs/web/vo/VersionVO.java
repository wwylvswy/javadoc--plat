package com.company.docs.web.vo;

import lombok.Data;

/**
 * 版本信息 VO。
 */
@Data
public class VersionVO {

    private Long id;

    private String version;

    private String status;

    private Integer progressPages;

    private Integer progressMaxPages;

    private Long documentCount;

    private Long uniqueUrlCount;

    private String indexedAt;

    private String sourceUrl;
}
