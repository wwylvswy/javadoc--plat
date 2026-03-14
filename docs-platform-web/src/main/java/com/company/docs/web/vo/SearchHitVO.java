package com.company.docs.web.vo;

import lombok.Data;

/**
 * 搜索命中 VO。
 */
@Data
public class SearchHitVO {

    private String url;

    private String title;

    private String content;

    private Double score;

    private String version;
}
