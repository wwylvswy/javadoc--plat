package com.company.docs.web.vo;

import lombok.Data;

import java.util.List;

/**
 * 文档库 VO。
 */
@Data
public class LibraryVO {

    private String library;

    private List<VersionVO> versions;
}
