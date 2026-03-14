package com.company.docs.scraper.model;

import lombok.Data;

/**
 * 抓取队列元素。
 */
@Data
public class QueueItemBO {

    private String url;

    private Integer depth;

    /**
     * 刷新场景下的历史 pageId。
     */
    private Long pageId;

    /**
     * 刷新场景下的历史 ETag。
     */
    private String etag;
}
