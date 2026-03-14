package com.company.docs.scraper.parser;

import com.company.docs.common.model.bo.ScrapeResultBO;
import com.company.docs.scraper.model.FetchedPageBO;
import com.company.docs.scraper.splitter.SimpleChunkSplitter;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

/**
 * 页面解析器。
 */
@Component
@RequiredArgsConstructor
public class PageParser {

    private final SimpleChunkSplitter chunkSplitter;

    /**
     * 将抓取结果解析为可入库对象。
     */
    public ScrapeResultBO parse(FetchedPageBO fetchedPage, Integer depth) {
        String text = extractText(fetchedPage);
        ScrapeResultBO result = new ScrapeResultBO();
        result.setUrl(fetchedPage.getFinalUrl());
        result.setTitle(fetchedPage.getTitle());
        result.setContentType(fetchedPage.getContentType());
        result.setEtag(fetchedPage.getEtag());
        result.setLastModified(fetchedPage.getLastModified());
        result.setDepth(depth);
        result.setChunks(chunkSplitter.split(text));
        return result;
    }

    private String extractText(FetchedPageBO fetchedPage) {
        if (fetchedPage.getBody() == null) {
            return "";
        }
        String body = fetchedPage.getBody();
        if (isHtml(fetchedPage.getContentType(), body)) {
            org.jsoup.nodes.Document document = Jsoup.parse(body);
            return document.body() == null ? "" : document.body().text();
        }
        return body;
    }

    private boolean isHtml(String contentType, String body) {
        if (contentType != null && contentType.toLowerCase().contains("html")) {
            return true;
        }
        return body != null && body.contains("<html");
    }
}
