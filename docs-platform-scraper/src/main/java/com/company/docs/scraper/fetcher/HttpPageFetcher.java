package com.company.docs.scraper.fetcher;

import com.company.docs.scraper.model.FetchedPageBO;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于 OkHttp 的页面抓取器。
 */
@Slf4j
@Component
public class HttpPageFetcher {

    /**
     * 抓取页面。
     */
    public FetchedPageBO fetch(String url, Map<String, String> headers, boolean followRedirects) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .followRedirects(followRedirects)
                .followSslRedirects(followRedirects)
                .connectTimeout(Duration.ofSeconds(15))
                .readTimeout(Duration.ofSeconds(30))
                .build();

        Request.Builder requestBuilder = new Request.Builder().url(url).get();
        if (headers != null) {
            headers.forEach(requestBuilder::addHeader);
        }

        try (Response response = client.newCall(requestBuilder.build()).execute()) {
            FetchedPageBO fetched = new FetchedPageBO();
            fetched.setFinalUrl(response.request().url().toString());
            fetched.setStatusCode(response.code());
            fetched.setContentType(response.header("Content-Type"));
            fetched.setEtag(response.header("ETag"));
            fetched.setLastModified(response.header("Last-Modified"));

            String body = response.body() == null ? "" : response.body().string();
            fetched.setBody(body);

            if (isHtml(fetched.getContentType(), body)) {
                org.jsoup.nodes.Document doc = Jsoup.parse(body, fetched.getFinalUrl());
                fetched.setTitle(doc.title());
                List<String> links = new ArrayList<>(doc.select("a[href]").eachAttr("abs:href"));
                fetched.setLinks(links);
            } else {
                fetched.setTitle(fetched.getFinalUrl());
                fetched.setLinks(List.of());
            }

            return fetched;
        }
    }

    private boolean isHtml(String contentType, String body) {
        if (contentType != null && contentType.toLowerCase().contains("html")) {
            return true;
        }
        return body != null && body.contains("<html");
    }
}
