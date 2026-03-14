package com.company.docs.scraper.manager;

import com.company.docs.common.enums.ScrapeModeEnum;
import com.company.docs.scraper.model.ScraperOptionsBO;
import com.company.docs.scraper.pipeline.ScrapeProgressCallback;
import com.company.docs.scraper.strategy.FetchScrapeStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 抓取编排器。
 * <p>
 * 当前统一使用 Fetch 策略，预留 Playwright 扩展入口。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class ScraperManager {

    private final FetchScrapeStrategy fetchScrapeStrategy;

    /**
     * 执行抓取。
     */
    public void scrape(ScraperOptionsBO options, ScrapeProgressCallback callback, AtomicBoolean cancelled) {
        ScrapeModeEnum mode = options.getScrapeMode() == null ? ScrapeModeEnum.AUTO : options.getScrapeMode();
        // 目前 AUTO/FETCH 均走 Fetch 策略，PLAYWRIGHT 作为后续能力点。
        if (mode == ScrapeModeEnum.PLAYWRIGHT) {
            fetchScrapeStrategy.scrape(options, callback, cancelled);
            return;
        }
        fetchScrapeStrategy.scrape(options, callback, cancelled);
    }
}
