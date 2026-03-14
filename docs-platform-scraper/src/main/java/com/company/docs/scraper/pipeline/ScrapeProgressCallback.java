package com.company.docs.scraper.pipeline;

import com.company.docs.scraper.model.ScrapeProgressBO;

/**
 * 抓取进度回调。
 */
@FunctionalInterface
public interface ScrapeProgressCallback {

    /**
     * 回调进度事件。
     */
    void onProgress(ScrapeProgressBO progress);
}
