package com.company.docs.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 删除版本 DTO。
 */
@Data
public class RemoveVersionDTO {

    @NotBlank(message = "library 不能为空")
    private String library;

    private String version;
}
