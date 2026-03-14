package com.company.docs.scraper.model;

import com.company.docs.common.enums.CrawlScopeEnum;
import com.company.docs.common.enums.ScrapeModeEnum;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 抓取参数。
 */
@Data
public class ScraperOptionsBO {

    private String url;

    private String library;

    private String version;

    private Integer maxPages;

    private Integer maxDepth;

    private CrawlScopeEnum scope;

    private Boolean followRedirects;

    private Integer maxConcurrency;

    private Boolean ignoreErrors;

    private ScrapeModeEnum scrapeMode;

    private List<String> includePatterns;

    private List<String> excludePatterns;

    private Map<String, String> headers;

    /**
     * true 时抓取前清空旧数据。
     */
    private Boolean clean;

    /**
     * 刷新场景初始队列。
     */
    private List<QueueItemBO> initialQueue;

    /**
     * 是否刷新任务。
     */
    private Boolean refresh;
}
