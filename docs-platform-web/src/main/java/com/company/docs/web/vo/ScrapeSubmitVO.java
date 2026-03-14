package com.company.docs.web.vo;

import lombok.Data;

/**
 * 抓取任务提交响应。
 */
@Data
public class ScrapeSubmitVO {

    private String jobId;

    private Boolean completed;

    private Integer pagesProcessed;
}
