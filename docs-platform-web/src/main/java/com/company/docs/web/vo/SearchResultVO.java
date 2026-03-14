package com.company.docs.web.vo;

import lombok.Data;

import java.util.List;

/**
 * 搜索响应 VO。
 */
@Data
public class SearchResultVO {

    private String resolvedVersion;

    private List<SearchHitVO> results;
}
