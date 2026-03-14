package com.company.docs.common.model.bo;

import lombok.Data;

/**
 * 检索命中结果业务对象。
 */
@Data
public class SearchHitBO {

    /** 文档链接。 */
    private String url;

    /** 文档标题。 */
    private String title;

    /** 命中文本片段。 */
    private String content;

    /** 相关性分数。 */
    private Double score;

    /** 版本号。 */
    private String version;
}
