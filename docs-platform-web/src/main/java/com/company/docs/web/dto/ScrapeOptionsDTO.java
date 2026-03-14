package com.company.docs.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 抓取参数 DTO。
 */
@Data
public class ScrapeOptionsDTO {

    @Min(value = 1, message = "maxPages 最小为 1")
    @Max(value = 10000, message = "maxPages 最大为 10000")
    private Integer maxPages;

    @Min(value = 0, message = "maxDepth 最小为 0")
    @Max(value = 20, message = "maxDepth 最大为 20")
    private Integer maxDepth;

    /** subpages/hostname/domain。 */
    private String scope;

    private Boolean followRedirects;

    @Min(value = 1, message = "maxConcurrency 最小为 1")
    @Max(value = 128, message = "maxConcurrency 最大为 128")
    private Integer maxConcurrency;

    private Boolean ignoreErrors;

    /** auto/fetch/playwright。 */
    private String scrapeMode;

    private List<String> includePatterns;

    private List<String> excludePatterns;

    private Map<String, String> headers;

    private Boolean clean;
}
