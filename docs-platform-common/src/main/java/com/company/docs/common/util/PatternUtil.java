package com.company.docs.common.util;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 包含/排除模式匹配工具。
 */
public final class PatternUtil {

    private PatternUtil() {
        // 工具类不允许实例化
    }

    /**
     * 判断 URL 是否可被抓取。
     * 规则：先匹配 include，再匹配 exclude，exclude 优先级更高。
     */
    public static boolean isAllowed(String url, List<String> includePatterns, List<String> excludePatterns) {
        boolean included = includePatterns == null || includePatterns.isEmpty() || matchAny(url, includePatterns);
        boolean excluded = excludePatterns != null && !excludePatterns.isEmpty() && matchAny(url, excludePatterns);
        return included && !excluded;
    }

    private static boolean matchAny(String text, List<String> patterns) {
        for (String raw : patterns) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            if (raw.startsWith("/") && raw.endsWith("/") && raw.length() > 2) {
                String regex = raw.substring(1, raw.length() - 1);
                if (Pattern.compile(regex).matcher(text).find()) {
                    return true;
                }
            } else if (text.contains(raw)) {
                return true;
            }
        }
        return false;
    }
}
