package com.company.docs.common.model.bo;

import lombok.Data;

import java.util.List;

/**
 * 文档分片业务对象。
 */
@Data
public class ChunkBO {

    /** 分片正文。 */
    private String content;

    /** 层级深度。 */
    private Integer level;

    /** 分层路径。 */
    private List<String> path;

    /** 分片类型集合，例如 text/code/table。 */
    private List<String> types;
}
