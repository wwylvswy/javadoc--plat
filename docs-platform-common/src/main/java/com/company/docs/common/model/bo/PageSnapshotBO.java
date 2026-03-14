package com.company.docs.common.model.bo;

import lombok.Data;

/**
 * 页面快照对象。
 * <p>
 * 刷新任务通过该对象携带历史页面的 URL、ETag 和深度信息。
 * </p>
 */
@Data
public class PageSnapshotBO {

    private Long pageId;

    private String url;

    private String etag;

    private Integer depth;
}
