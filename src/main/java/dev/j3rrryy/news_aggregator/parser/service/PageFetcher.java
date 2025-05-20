package dev.j3rrryy.news_aggregator.parser.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

@Slf4j
@Component
@RequiredArgsConstructor
public class PageFetcher {

    private static final Semaphore ioSemaphore = new Semaphore(50);
    private static final Map<String, String> headers = Map.of(
            "Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7",
            "Accept-Encoding", "gzip, deflate, br",
            "Referer", "https://www.google.com",
            "Connection", "keep-alive",
            "Cache-Control", "no-cache",
            "DNT", "1",
            "Upgrade-Insecure-Requests", "1",
            "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8"
    );

    private final UserAgentProvider userAgentProvider;
    private final ParsingStatusManager parsingStatusManager;

    public Optional<Document> downloadPage(Callable<Document> pageLoader, String urlForLog, RateLimiter rateLimiter) {
        boolean stopRequested = parsingStatusManager.isStopRequested();
        try {
            ioSemaphore.acquire();
            rateLimiter.acquire();

            if (stopRequested || Thread.interrupted()) return Optional.empty();

            Document doc = pageLoader.call();
            return Optional.of(doc);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Download from {} failed: {}", urlForLog.trim(), e.getMessage());
            return Optional.empty();
        } finally {
            ioSemaphore.release();
        }
    }

    public Callable<Document> fetchGet(String url) {
        return () -> Jsoup.connect(url)
                .userAgent(userAgentProvider.getNextUserAgent())
                .timeout(45_000)
                .headers(headers)
                .get();
    }

    public Callable<Document> fetchPost(String url, String body) {
        return () -> {
            String json = Jsoup.connect(url)
                    .userAgent(userAgentProvider.getNextUserAgent())
                    .timeout(45_000)
                    .headers(headers)
                    .header("X-Requested-With", "XMLHttpRequest")
                    .method(Connection.Method.POST)
                    .requestBody(body)
                    .ignoreContentType(true)
                    .execute()
                    .body();

            String html = new ObjectMapper().readValue(json, new TypeReference<Map<String, Object>>() {
                    })
                    .get("data")
                    .toString();
            return Jsoup.parse(html);
        };
    }

}
