package com.company.docs.common.model.bo;

import lombok.Data;

import java.util.List;

/**
 * 单页面抓取并解析后的结果。
 */
@Data
public class ScrapeResultBO {

    /** 页面 URL。 */
    private String url;

    /** 页面标题。 */
    private String title;

    /** 内容类型。 */
    private String contentType;

    /** ETag。 */
    private String etag;

    /** Last-Modified。 */
    private String lastModified;

    /** 抓取深度。 */
    private Integer depth;

    /** 分片列表。 */
    private List<ChunkBO> chunks;
}
