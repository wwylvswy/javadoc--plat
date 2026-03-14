package com.company.docs.common.constant;

/**
 * 通用常量定义。
 * <p>
 * 说明：该类只放跨模块共享且稳定的默认值，避免魔法值散落在业务代码中。
 * </p>
 */
public final class CommonConstants {

    private CommonConstants() {
        // 工具类不允许实例化
    }

    /** 默认查询条数。 */
    public static final int DEFAULT_SEARCH_LIMIT = 5;

    /** 查询条数上限。 */
    public static final int MAX_SEARCH_LIMIT = 100;

    /** 默认抓取最大页面数。 */
    public static final int DEFAULT_MAX_PAGES = 200;

    /** 默认抓取最大深度。 */
    public static final int DEFAULT_MAX_DEPTH = 2;

    /** 默认抓取并发度。 */
    public static final int DEFAULT_MAX_CONCURRENCY = 4;
}
