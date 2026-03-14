package com.company.docs.scraper.strategy;

import com.company.docs.scraper.model.ScraperOptionsBO;
import com.company.docs.scraper.pipeline.ScrapeProgressCallback;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 抓取策略接口。
 */
public interface ScrapeStrategy {

    /**
     * 执行抓取。
     */
    void scrape(ScraperOptionsBO options, ScrapeProgressCallback callback, AtomicBoolean cancelled);
}
