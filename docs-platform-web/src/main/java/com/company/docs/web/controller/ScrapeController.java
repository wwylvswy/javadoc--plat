package com.company.docs.web.controller;

import com.company.docs.common.constant.CommonConstants;
import com.company.docs.common.enums.CrawlScopeEnum;
import com.company.docs.common.enums.ScrapeModeEnum;
import com.company.docs.common.response.ApiResponse;
import com.company.docs.scraper.model.ScraperOptionsBO;
import com.company.docs.storage.model.entity.JobDO;
import com.company.docs.storage.repository.sqlite.StorageFacadeService;
import com.company.docs.task.manager.PipelineManager;
import com.company.docs.web.dto.RefreshVersionDTO;
import com.company.docs.web.dto.RemoveVersionDTO;
import com.company.docs.web.dto.ScrapeCreateDTO;
import com.company.docs.web.dto.ScrapeOptionsDTO;
import com.company.docs.web.vo.ScrapeSubmitVO;
import com.company.docs.web.vo.SimpleMessageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 抓取相关接口。
 */
@RestController
@RequestMapping("/api/scrape")
@RequiredArgsConstructor
public class ScrapeController {

    private final PipelineManager pipelineManager;
    private final StorageFacadeService storageFacadeService;

    /**
     * 提交抓取任务。
     */
    @PostMapping("/queue")
    public ApiResponse<ScrapeSubmitVO> queueScrape(@Valid @RequestBody ScrapeCreateDTO dto) {
        ScraperOptionsBO options = buildOptions(dto.getLibrary(), dto.getVersion(), dto.getUrl(), dto.getOptions());
        String jobId = pipelineManager.enqueueScrapeJob(dto.getLibrary(), dto.getVersion(), options);

        boolean wait = dto.getWaitForCompletion() == null || dto.getWaitForCompletion();
        ScrapeSubmitVO vo = new ScrapeSubmitVO();
        vo.setJobId(jobId);
        vo.setCompleted(false);
        vo.setPagesProcessed(0);

        if (wait) {
            pipelineManager.waitForJobCompletion(jobId);
            JobDO job = pipelineManager.getJob(jobId);
            vo.setCompleted(true);
            vo.setPagesProcessed(job == null ? 0 : job.getProgressPages());
        }
        return ApiResponse.success(vo);
    }

    /**
     * 刷新版本。
     */
    @PostMapping("/refresh")
    public ApiResponse<ScrapeSubmitVO> refreshVersion(@Valid @RequestBody RefreshVersionDTO dto) {
        String jobId = pipelineManager.enqueueRefreshJob(dto.getLibrary(), dto.getVersion());
        boolean wait = dto.getWaitForCompletion() == null || dto.getWaitForCompletion();

        ScrapeSubmitVO vo = new ScrapeSubmitVO();
        vo.setJobId(jobId);
        vo.setCompleted(false);
        vo.setPagesProcessed(0);

        if (wait) {
            pipelineManager.waitForJobCompletion(jobId);
            JobDO job = pipelineManager.getJob(jobId);
            vo.setCompleted(true);
            vo.setPagesProcessed(job == null ? 0 : job.getProgressPages());
        }
        return ApiResponse.success(vo);
    }

    /**
     * 删除版本。
     * <p>
     * 先取消同库同版本运行中任务，避免“删除与写入并发”冲突。
     * </p>
     */
    @PostMapping("/remove")
    public ApiResponse<SimpleMessageVO> removeVersion(@Valid @RequestBody RemoveVersionDTO dto) {
        List<JobDO> activeJobs = storageFacadeService.listActiveJobs(dto.getLibrary(), dto.getVersion());
        for (JobDO job : activeJobs) {
            pipelineManager.cancelJob(job.getId());
            try {
                pipelineManager.waitForJobCompletion(job.getId());
            } catch (Exception ignored) {
                // 取消后等待可能抛完成异常，删除流程不需要中断
            }
        }

        storageFacadeService.removeVersion(dto.getLibrary(), dto.getVersion());
        return ApiResponse.success(new SimpleMessageVO("删除成功"));
    }

    private ScraperOptionsBO buildOptions(String library, String version, String url, ScrapeOptionsDTO dto) {
        ScrapeOptionsDTO optionsDTO = dto == null ? new ScrapeOptionsDTO() : dto;

        ScraperOptionsBO options = new ScraperOptionsBO();
        options.setLibrary(library);
        options.setVersion(version);
        options.setUrl(url);
        options.setMaxPages(optionsDTO.getMaxPages() == null ? CommonConstants.DEFAULT_MAX_PAGES : optionsDTO.getMaxPages());
        options.setMaxDepth(optionsDTO.getMaxDepth() == null ? CommonConstants.DEFAULT_MAX_DEPTH : optionsDTO.getMaxDepth());
        options.setScope(CrawlScopeEnum.from(optionsDTO.getScope()));
        options.setFollowRedirects(optionsDTO.getFollowRedirects() == null ? Boolean.TRUE : optionsDTO.getFollowRedirects());
        options.setMaxConcurrency(optionsDTO.getMaxConcurrency() == null ? CommonConstants.DEFAULT_MAX_CONCURRENCY : optionsDTO.getMaxConcurrency());
        options.setIgnoreErrors(optionsDTO.getIgnoreErrors() == null ? Boolean.TRUE : optionsDTO.getIgnoreErrors());
        options.setScrapeMode(ScrapeModeEnum.from(optionsDTO.getScrapeMode()));
        options.setIncludePatterns(optionsDTO.getIncludePatterns());
        options.setExcludePatterns(optionsDTO.getExcludePatterns());
        options.setHeaders(optionsDTO.getHeaders());
        options.setClean(optionsDTO.getClean() == null ? Boolean.TRUE : optionsDTO.getClean());
        options.setRefresh(Boolean.FALSE);
        return options;
    }
}
