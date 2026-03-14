package com.company.docs.common.exception;

/**
 * 业务异常基类。
 * <p>
 * 设计约束：Controller 只抛业务语义异常，统一由全局异常处理器映射为响应码。
 * </p>
 */
public class BizException extends RuntimeException {

    private final String code;

    public BizException(String code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
