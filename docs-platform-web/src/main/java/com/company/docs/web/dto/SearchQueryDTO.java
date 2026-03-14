package com.company.docs.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 搜索请求 DTO。
 */
@Data
public class SearchQueryDTO {

    @NotBlank(message = "library 不能为空")
    private String library;

    private String version;

    @NotBlank(message = "query 不能为空")
    private String query;

    @Min(value = 1, message = "limit 最小为 1")
    @Max(value = 100, message = "limit 最大为 100")
    private Integer limit;

    private Boolean exactMatch;
}
