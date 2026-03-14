package com.company.docs.scraper.model;

import lombok.Data;

import java.util.List;

/**
 * 单页面抓取原始结果。
 */
@Data
public class FetchedPageBO {

    private String finalUrl;

    private Integer statusCode;

    private String contentType;

    private String body;

    private String title;

    private String etag;

    private String lastModified;

    private List<String> links;
}
