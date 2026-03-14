package com.company.docs.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建抓取任务 DTO。
 */
@Data
public class ScrapeCreateDTO {

    @NotBlank(message = "library 不能为空")
    private String library;

    /**
     * 版本可为空，表示无版本文档。
     */
    private String version;

    @NotBlank(message = "url 不能为空")
    private String url;

    @Valid
    private ScrapeOptionsDTO options;

    /**
     * true: 同步等待任务结束；false: 立即返回 jobId。
     */
    private Boolean waitForCompletion;
}
