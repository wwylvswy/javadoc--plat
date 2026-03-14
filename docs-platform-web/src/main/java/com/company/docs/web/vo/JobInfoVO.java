package com.company.docs.web.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务信息 VO。
 */
@Data
public class JobInfoVO {

    private String id;

    private String library;

    private String version;

    private String status;

    private Integer progressPages;

    private Integer progressMaxPages;

    private String errorMessage;

    private String sourceUrl;

    private LocalDateTime createdAt;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;
}
