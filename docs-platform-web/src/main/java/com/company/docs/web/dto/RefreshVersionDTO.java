package com.company.docs.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 刷新版本 DTO。
 */
@Data
public class RefreshVersionDTO {

    @NotBlank(message = "library 不能为空")
    private String library;

    private String version;

    private Boolean waitForCompletion;
}
