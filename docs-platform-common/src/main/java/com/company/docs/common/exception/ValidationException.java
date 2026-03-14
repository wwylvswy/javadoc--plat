package com.company.docs.common.exception;

/**
 * 参数校验异常。
 */
public class ValidationException extends BizException {

    public ValidationException(String message) {
        super("VALIDATION_ERROR", message);
    }
}
