package com.company.docs.common.enums;

/**
 * 版本索引状态枚举。
 * <p>
 * 与数据库 versions.status 字段保持一致，避免状态值在各层不一致。
 * </p>
 */
public enum VersionStatusEnum {
    NOT_INDEXED,
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED,
    UPDATING
}
