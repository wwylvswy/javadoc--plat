package com.company.docs.web.controller;

import com.company.docs.common.response.ApiResponse;
import com.company.docs.storage.model.entity.LibraryVersionStatDO;
import com.company.docs.storage.repository.sqlite.StorageFacadeService;
import com.company.docs.web.vo.LibraryVO;
import com.company.docs.web.vo.VersionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档库查询接口。
 */
@RestController
@RequestMapping("/api/libraries")
@RequiredArgsConstructor
public class LibraryController {

    private final StorageFacadeService storageFacadeService;

    /**
     * 列出全部库和版本统计。
     */
    @GetMapping
    public ApiResponse<List<LibraryVO>> listLibraries() {
        List<LibraryVersionStatDO> stats = storageFacadeService.listLibraryVersionStats();

        Map<String, List<LibraryVersionStatDO>> grouped = new LinkedHashMap<>();
        for (LibraryVersionStatDO stat : stats) {
            grouped.computeIfAbsent(stat.getLibrary(), key -> new ArrayList<>()).add(stat);
        }

        List<LibraryVO> list = new ArrayList<>();
        for (Map.Entry<String, List<LibraryVersionStatDO>> entry : grouped.entrySet()) {
            LibraryVO libraryVO = new LibraryVO();
            libraryVO.setLibrary(entry.getKey());
            libraryVO.setVersions(entry.getValue().stream().map(this::toVersionVO).toList());
            list.add(libraryVO);
        }
        return ApiResponse.success(list);
    }

    private VersionVO toVersionVO(LibraryVersionStatDO row) {
        VersionVO vo = new VersionVO();
        vo.setId(row.getVersionId());
        vo.setVersion(row.getVersion());
        vo.setStatus(row.getStatus());
        vo.setProgressPages(row.getProgressPages());
        vo.setProgressMaxPages(row.getProgressMaxPages());
        vo.setDocumentCount(row.getDocumentCount());
        vo.setUniqueUrlCount(row.getUniqueUrlCount());
        vo.setIndexedAt(row.getIndexedAt());
        vo.setSourceUrl(row.getSourceUrl());
        return vo;
    }
}
