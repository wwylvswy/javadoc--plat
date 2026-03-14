package com.company.docs.common.util;

import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 版本工具类。
 * <p>
 * 兼容 X / X.Y / X.Y.Z 输入，内部统一归一化到 X.Y.Z 小写语义。
 * </p>
 */
public final class VersionUtil {

    private VersionUtil() {
        // 工具类不允许实例化
    }

    private static final Pattern FULL = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)(?:[-+][0-9A-Za-z.-]+)?$");
    private static final Pattern PARTIAL = Pattern.compile("^(\\d+)(?:\\.(\\d+))?$");

    /**
     * 归一化版本。
     *
     * @param version 输入版本，允许 null/blank
     * @return null 表示无版本，其余返回规范化版本
     */
    public static String normalizeOrNull(String version) {
        if (version == null || version.isBlank()) {
            return null;
        }
        String trimmed = version.trim().toLowerCase(Locale.ROOT);
        Matcher full = FULL.matcher(trimmed);
        if (full.matches()) {
            return trimmed;
        }
        Matcher partial = PARTIAL.matcher(trimmed);
        if (partial.matches()) {
            String major = partial.group(1);
            String minor = partial.group(2);
            if (minor == null) {
                return major + ".0.0";
            }
            return major + "." + minor + ".0";
        }
        throw new IllegalArgumentException("版本格式非法: " + version);
    }

    /**
     * 版本比较器（降序）。
     */
    public static Comparator<String> descComparator() {
        return (a, b) -> compareSemantic(b, a);
    }

    /**
     * 语义版本比较，返回值含义同 Comparator。
     */
    public static int compareSemantic(String a, String b) {
        int[] va = parseCore(a);
        int[] vb = parseCore(b);
        for (int i = 0; i < 3; i++) {
            if (va[i] != vb[i]) {
                return Integer.compare(va[i], vb[i]);
            }
        }
        return 0;
    }

    private static int[] parseCore(String version) {
        String normalized = normalizeOrNull(version);
        if (normalized == null) {
            return new int[]{0, 0, 0};
        }
        String core = normalized.split("[-+]", 2)[0];
        String[] parts = core.split("\\.");
        return new int[]{
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2])
        };
    }

    /**
     * 判断目标版本是否命中请求版本模式。
     * 支持 latest、X、X.Y、X.Y.Z、X.x、X.Y.x。
     */
    public static boolean matches(String candidate, String requested) {
        if (requested == null || requested.isBlank() || "latest".equalsIgnoreCase(requested)) {
            return true;
        }
        String c = normalizeOrNull(candidate);
        String r = requested.trim().toLowerCase(Locale.ROOT);
        if (c == null) {
            return false;
        }
        String cCore = c.split("[-+]", 2)[0];
        String[] cParts = cCore.split("\\.");
        String[] rParts = r.split("\\.");

        if (r.endsWith(".x")) {
            if (rParts.length == 2) {
                return cParts[0].equals(rParts[0].replace("x", ""));
            }
            if (rParts.length == 3) {
                return cParts[0].equals(rParts[0]) && cParts[1].equals(rParts[1]);
            }
            return false;
        }

        try {
            String normalizedRequest = normalizeOrNull(r);
            if (normalizedRequest == null) {
                return false;
            }
            return c.equals(normalizedRequest);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
