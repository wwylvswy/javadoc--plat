package com.company.docs.web.controller;

import com.company.docs.common.model.bo.SearchHitBO;
import com.company.docs.common.response.ApiResponse;
import com.company.docs.search.manager.SearchManager;
import com.company.docs.search.model.SearchResultEnvelopeBO;
import com.company.docs.web.dto.SearchQueryDTO;
import com.company.docs.web.vo.SearchHitVO;
import com.company.docs.web.vo.SearchResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 搜索接口。
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchManager searchManager;

    /**
     * 搜索文档。
     */
    @GetMapping
    public ApiResponse<SearchResultVO> search(@Valid @ModelAttribute SearchQueryDTO dto) {
        SearchResultEnvelopeBO envelope = searchManager.search(
                dto.getLibrary(),
                dto.getVersion(),
                dto.getQuery(),
                dto.getLimit(),
                dto.getExactMatch()
        );

        SearchResultVO vo = new SearchResultVO();
        vo.setResolvedVersion(envelope.getResolvedVersion());
        vo.setResults(toHitVOList(envelope.getResults()));
        return ApiResponse.success(vo);
    }

    private List<SearchHitVO> toHitVOList(List<SearchHitBO> hits) {
        return hits.stream().map(hit -> {
            SearchHitVO vo = new SearchHitVO();
            vo.setUrl(hit.getUrl());
            vo.setTitle(hit.getTitle());
            vo.setContent(hit.getContent());
            vo.setScore(hit.getScore());
            vo.setVersion(hit.getVersion());
            return vo;
        }).toList();
    }
}
