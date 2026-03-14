package com.company.docs.common.enums;

/**
 * 抓取范围枚举。
 * <p>
 * SUBPAGES：同主机且同起始路径下的子路径。
 * HOSTNAME：同主机。
 * DOMAIN：同顶级域名及其子域。
 * </p>
 */
public enum CrawlScopeEnum {
    SUBPAGES,
    HOSTNAME,
    DOMAIN;

    /**
     * 兼容外部字符串传参，非法值默认返回 SUBPAGES。
     */
    public static CrawlScopeEnum from(String value) {
        if (value == null || value.isBlank()) {
            return SUBPAGES;
        }
        for (CrawlScopeEnum scope : values()) {
            if (scope.name().equalsIgnoreCase(value)) {
                return scope;
            }
        }
        return SUBPAGES;
    }
}
