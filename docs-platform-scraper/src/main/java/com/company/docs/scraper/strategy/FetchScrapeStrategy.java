package com.company.docs.scraper.strategy;

import com.company.docs.common.constant.CommonConstants;
import com.company.docs.common.enums.CrawlScopeEnum;
import com.company.docs.common.model.bo.ScrapeResultBO;
import com.company.docs.common.util.PatternUtil;
import com.company.docs.scraper.fetcher.HttpPageFetcher;
import com.company.docs.scraper.model.FetchedPageBO;
import com.company.docs.scraper.model.QueueItemBO;
import com.company.docs.scraper.model.ScrapeProgressBO;
import com.company.docs.scraper.model.ScraperOptionsBO;
import com.company.docs.scraper.parser.PageParser;
import com.company.docs.scraper.pipeline.ScrapeProgressCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 默认抓取策略。
 * <p>
 * 使用 BFS 进行页面遍历，保证深度优先级可控，便于进度计算与任务取消。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FetchScrapeStrategy implements ScrapeStrategy {

    private final HttpPageFetcher pageFetcher;
    private final PageParser pageParser;

    @Override
    public void scrape(ScraperOptionsBO options, ScrapeProgressCallback callback, AtomicBoolean cancelled) {
        int maxPages = options.getMaxPages() == null ? CommonConstants.DEFAULT_MAX_PAGES : options.getMaxPages();
        int maxDepth = options.getMaxDepth() == null ? CommonConstants.DEFAULT_MAX_DEPTH : options.getMaxDepth();
        boolean followRedirects = options.getFollowRedirects() == null || options.getFollowRedirects();
        CrawlScopeEnum scope = options.getScope() == null ? CrawlScopeEnum.SUBPAGES : options.getScope();

        Queue<QueueItemBO> queue = new ArrayDeque<>();
        if (options.getInitialQueue() != null && !options.getInitialQueue().isEmpty()) {
            queue.addAll(options.getInitialQueue());
        } else {
            QueueItemBO seed = new QueueItemBO();
            seed.setUrl(options.getUrl());
            seed.setDepth(0);
            queue.offer(seed);
        }

        Set<String> visited = new HashSet<>();
        String rootUrl = options.getUrl();
        int processed = 0;

        while (!queue.isEmpty() && processed < maxPages) {
            if (cancelled.get()) {
                log.info("抓取任务被取消，停止继续处理。library={}, version={}", options.getLibrary(), options.getVersion());
                return;
            }

            QueueItemBO item = queue.poll();
            if (item == null || item.getUrl() == null || item.getUrl().isBlank()) {
                continue;
            }
            String currentUrl = item.getUrl();
            if (!visited.add(currentUrl)) {
                continue;
            }

            if (!PatternUtil.isAllowed(currentUrl, options.getIncludePatterns(), options.getExcludePatterns())) {
                continue;
            }

            try {
                FetchedPageBO fetched = pageFetcher.fetch(currentUrl, options.getHeaders(), followRedirects);

                if (Boolean.TRUE.equals(options.getRefresh()) && fetched.getStatusCode() == 404 && item.getPageId() != null) {
                    processed++;
                    callback.onProgress(buildProgress(processed, processed + queue.size(), currentUrl, item.getDepth(), item.getPageId(), true, null));
                    continue;
                }

                if (Boolean.TRUE.equals(options.getRefresh())
                        && item.getEtag() != null
                        && !item.getEtag().isBlank()
                        && item.getEtag().equals(fetched.getEtag())) {
                    processed++;
                    callback.onProgress(buildProgress(processed, processed + queue.size(), currentUrl, item.getDepth(), item.getPageId(), false, null));
                    continue;
                }

                if (fetched.getStatusCode() >= 400) {
                    if (Boolean.TRUE.equals(options.getIgnoreErrors())) {
                        log.warn("抓取失败但已忽略。url={}, status={}", currentUrl, fetched.getStatusCode());
                        continue;
                    }
                    throw new IllegalStateException("页面抓取失败: " + currentUrl + ", status=" + fetched.getStatusCode());
                }

                ScrapeResultBO result = pageParser.parse(fetched, item.getDepth());
                processed++;
                callback.onProgress(buildProgress(processed, processed + queue.size(), fetched.getFinalUrl(), item.getDepth(), item.getPageId(), false, result));

                Integer depth = item.getDepth() == null ? 0 : item.getDepth();
                if (depth < maxDepth) {
                    enqueueChildren(queue, fetched.getLinks(), rootUrl, fetched.getFinalUrl(), depth + 1, scope, visited);
                }
            } catch (Exception ex) {
                if (Boolean.TRUE.equals(options.getIgnoreErrors())) {
                    log.warn("抓取异常已忽略。url={}", currentUrl, ex);
                } else {
                    throw new RuntimeException("抓取任务失败: " + currentUrl, ex);
                }
            }
        }
    }

    private void enqueueChildren(Queue<QueueItemBO> queue,
                                 List<String> links,
                                 String rootUrl,
                                 String currentUrl,
                                 int nextDepth,
                                 CrawlScopeEnum scope,
                                 Set<String> visited) {
        if (links == null || links.isEmpty()) {
            return;
        }
        for (String link : links) {
            if (link == null || link.isBlank()) {
                continue;
            }
            if (!link.startsWith("http://") && !link.startsWith("https://")) {
                continue;
            }
            if (visited.contains(link)) {
                continue;
            }
            if (!inScope(rootUrl, currentUrl, link, scope)) {
                continue;
            }
            QueueItemBO child = new QueueItemBO();
            child.setUrl(link);
            child.setDepth(nextDepth);
            queue.offer(child);
        }
    }

    private boolean inScope(String rootUrl, String currentUrl, String targetUrl, CrawlScopeEnum scope) {
        try {
            URI root = URI.create(rootUrl);
            URI current = URI.create(currentUrl);
            URI target = URI.create(targetUrl);
            if (target.getHost() == null || root.getHost() == null) {
                return false;
            }
            return switch (scope) {
                case HOSTNAME -> target.getHost().equalsIgnoreCase(root.getHost());
                case DOMAIN -> topDomain(target.getHost()).equalsIgnoreCase(topDomain(root.getHost()));
                case SUBPAGES -> target.getHost().equalsIgnoreCase(root.getHost())
                        && target.getPath() != null
                        && target.getPath().startsWith(parentPath(current.getPath()));
            };
        } catch (Exception ex) {
            return false;
        }
    }

    private String parentPath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        int idx = path.lastIndexOf('/');
        if (idx <= 0) {
            return "/";
        }
        return path.substring(0, idx + 1);
    }

    private String topDomain(String host) {
        String lower = host.toLowerCase(Locale.ROOT);
        String[] arr = lower.split("\\.");
        if (arr.length < 2) {
            return lower;
        }
        return arr[arr.length - 2] + "." + arr[arr.length - 1];
    }

    private ScrapeProgressBO buildProgress(int pagesScraped,
                                           int totalPages,
                                           String currentUrl,
                                           Integer depth,
                                           Long pageId,
                                           Boolean deleted,
                                           ScrapeResultBO result) {
        ScrapeProgressBO progress = new ScrapeProgressBO();
        progress.setPagesScraped(pagesScraped);
        progress.setTotalPages(Math.max(totalPages, pagesScraped));
        progress.setCurrentUrl(currentUrl);
        progress.setDepth(depth == null ? 0 : depth);
        progress.setPageId(pageId);
        progress.setDeleted(deleted);
        progress.setResult(result);
        return progress;
    }
}
