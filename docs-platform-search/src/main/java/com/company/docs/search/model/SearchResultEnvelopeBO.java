package com.company.docs.search.model;

import com.company.docs.common.model.bo.SearchHitBO;
import lombok.Data;

import java.util.List;

/**
 * 搜索结果包装对象。
 */
@Data
public class SearchResultEnvelopeBO {

    /**
     * 实际命中的版本。
     * null 表示命中无版本文档。
     */
    private String resolvedVersion;

    /**
     * 检索结果列表。
     */
    private List<SearchHitBO> results;
}
