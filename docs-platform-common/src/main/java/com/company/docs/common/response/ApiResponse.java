package com.company.docs.common.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口响应体。
 *
 * @param <T> 业务数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    /** 是否成功。 */
    private boolean success;

    /** 业务错误码。 */
    private String code;

    /** 响应消息。 */
    private String message;

    /** 业务数据。 */
    private T data;

    /**
     * 成功响应构造。
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "OK", "success", data);
    }

    /**
     * 失败响应构造。
     */
    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }
}
