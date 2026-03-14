package com.company.docs.common.exception;

/**
 * 资源不存在异常。
 */
public class NotFoundException extends BizException {

    public NotFoundException(String message) {
        super("NOT_FOUND", message);
    }
}
