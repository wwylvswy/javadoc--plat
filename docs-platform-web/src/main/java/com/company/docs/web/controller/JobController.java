package com.company.docs.web.controller;

import com.company.docs.common.response.ApiResponse;
import com.company.docs.storage.model.entity.JobDO;
import com.company.docs.storage.model.entity.LibraryDO;
import com.company.docs.storage.model.entity.VersionDO;
import com.company.docs.storage.repository.sqlite.StorageFacadeService;
import com.company.docs.task.manager.PipelineManager;
import com.company.docs.web.vo.JobInfoVO;
import com.company.docs.web.vo.SimpleMessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 任务接口。
 */
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final PipelineManager pipelineManager;
    private final StorageFacadeService storageFacadeService;

    /**
     * 获取任务列表。
     */
    @GetMapping
    public ApiResponse<List<JobInfoVO>> listJobs() {
        List<JobInfoVO> list = pipelineManager.getJobs().stream().map(this::toVO).toList();
        return ApiResponse.success(list);
    }

    /**
     * 获取单任务详情。
     */
    @GetMapping("/{jobId}")
    public ApiResponse<JobInfoVO> getJob(@PathVariable("jobId") String jobId) {
        JobDO job = pipelineManager.getJob(jobId);
        if (job == null) {
            return ApiResponse.success(null);
        }
        return ApiResponse.success(toVO(job));
    }

    /**
     * 取消任务。
     */
    @PostMapping("/{jobId}/cancel")
    public ApiResponse<SimpleMessageVO> cancelJob(@PathVariable("jobId") String jobId) {
        pipelineManager.cancelJob(jobId);
        return ApiResponse.success(new SimpleMessageVO("取消指令已下发"));
    }

    /**
     * 清理完成任务。
     */
    @PostMapping("/clear-completed")
    public ApiResponse<SimpleMessageVO> clearCompleted() {
        int count = pipelineManager.clearCompletedJobs();
        return ApiResponse.success(new SimpleMessageVO("已清理任务数量: " + count));
    }

    private JobInfoVO toVO(JobDO job) {
        JobInfoVO vo = new JobInfoVO();
        vo.setId(job.getId());
        vo.setStatus(job.getStatus());
        vo.setProgressPages(job.getProgressPages());
        vo.setProgressMaxPages(job.getProgressMaxPages());
        vo.setErrorMessage(job.getErrorMessage());
        vo.setSourceUrl(job.getSourceUrl());
        vo.setCreatedAt(job.getCreatedAt());
        vo.setStartedAt(job.getStartedAt());
        vo.setFinishedAt(job.getFinishedAt());

        VersionDO versionDO = job.getVersionId() == null ? null : storageFacadeService.getVersionById(job.getVersionId());
        if (versionDO != null) {
            vo.setVersion(versionDO.getName());
            LibraryDO libraryDO = storageFacadeService.getLibraryByVersionId(versionDO.getId());
            vo.setLibrary(libraryDO == null ? null : libraryDO.getName());
        }
        return vo;
    }
}
