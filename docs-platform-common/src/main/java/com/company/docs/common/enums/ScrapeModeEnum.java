package com.company.docs.common.enums;

/**
 * 抓取模式枚举。
 * <p>
 * 当前实现中 AUTO 与 FETCH 等价，预留 PLAYWRIGHT 扩展位。
 * </p>
 */
public enum ScrapeModeEnum {
    AUTO,
    FETCH,
    PLAYWRIGHT;

    public static ScrapeModeEnum from(String value) {
        if (value == null || value.isBlank()) {
            return AUTO;
        }
        for (ScrapeModeEnum mode : values()) {
            if (mode.name().equalsIgnoreCase(value)) {
                return mode;
            }
        }
        return AUTO;
    }
}
