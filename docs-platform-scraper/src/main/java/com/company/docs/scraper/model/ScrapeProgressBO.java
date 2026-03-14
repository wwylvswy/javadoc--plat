package com.company.docs.scraper.model;

import com.company.docs.common.model.bo.ScrapeResultBO;
import lombok.Data;

/**
 * 抓取进度事件。
 */
@Data
public class ScrapeProgressBO {

    private Integer pagesScraped;

    private Integer totalPages;

    private String currentUrl;

    private Integer depth;

    private Long pageId;

    private Boolean deleted;

    private ScrapeResultBO result;
}
