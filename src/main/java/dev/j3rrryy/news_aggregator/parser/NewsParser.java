package dev.j3rrryy.news_aggregator.parser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import dev.j3rrryy.news_aggregator.entity.NewsArticle;
import dev.j3rrryy.news_aggregator.enums.Category;
import dev.j3rrryy.news_aggregator.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public abstract class NewsParser {

    private static final List<String> userAgents = List.of(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:124.0) Gecko/20100101 Firefox/124.0",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36 Edg/123.0.2420.81",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36 OPR/109.0.0.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 14.4; rv:124.0) Gecko/20100101 Firefox/124.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_4_1) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4.1 Safari/605.1.15",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_4_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36 OPR/109.0.0.0",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Linux i686; rv:124.0) Gecko/20100101 Firefox/124.0"
    );
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
    private static final AtomicInteger userAgentIndex = new AtomicInteger(0);
    private static final Semaphore ioSemaphore = new Semaphore(50);
    private static final int BATCH_SIZE = 500;

    protected final ExecutorService ioExecutor;
    private final RateLimiter rateLimiter;
    private final ExecutorService cpuExecutor;
    private final NewsArticleRepository newsArticleRepository;

    public abstract void parse(Map<Category, LocalDateTime> latestPublishedAtByCategory);

    protected abstract Optional<NewsArticle> parseNewsArticle(Document doc, Category category);

    protected Optional<Document> downloadPage(Callable<Document> pageLoader, String urlForLog) {
        if (Thread.interrupted()) return Optional.empty();
        try {
            ioSemaphore.acquire();
            rateLimiter.acquire();
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

    protected Callable<Document> fetchGet(String url) {
        return () -> Jsoup.connect(url)
                .userAgent(getNextUserAgent())
                .headers(headers)
                .timeout(120_000)
                .get();
    }

    protected Callable<Document> fetchPost(String url, String body) {
        return () -> {
            String json = Jsoup.connect(url)
                    .userAgent(getNextUserAgent())
                    .headers(headers)
                    .timeout(120_000)
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

    protected int fetchAndSaveArticles(Set<String> articleUrls, Category category, LocalDateTime latestPublishedAt) {
        Set<CompletableFuture<Optional<NewsArticle>>> articleFutures = articleUrls.stream()
                .map(articleUrl -> CompletableFuture
                        .supplyAsync(() -> downloadPage(fetchGet(articleUrl), articleUrl), ioExecutor)
                        .thenApplyAsync(optDoc -> optDoc
                                .flatMap(doc -> parseNewsArticle(doc, category)), cpuExecutor))
                .collect(Collectors.toSet());

        CompletableFuture.allOf(articleFutures.toArray(CompletableFuture[]::new)).join();
        return articleFutures.stream()
                .map(CompletableFuture::join)
                .flatMap(Optional::stream)
                .filter(n -> latestPublishedAt == null || n.getPublishedAt().isAfter(latestPublishedAt))
                .collect(Collectors.collectingAndThen(Collectors.toSet(), this::saveIgnoringDuplicates));
    }

    private String getNextUserAgent() {
        int index = userAgentIndex.getAndUpdate(i -> (i + 1) % userAgents.size());
        return userAgents.get(index);
    }

    private int saveIgnoringDuplicates(Set<NewsArticle> articles) {
        if (articles.isEmpty()) return 0;
        Set<String> inputUrls = articles.stream()
                .map(NewsArticle::getUrl)
                .collect(Collectors.toSet());

        if (inputUrls.isEmpty()) return 0;

        Set<String> existingUrls = newsArticleRepository.findExistingUrls(inputUrls);
        Map<String, NewsArticle> uniqueArticles = articles.stream()
                .collect(Collectors.toMap(
                        NewsArticle::getUrl,
                        Function.identity(),
                        (first, second) -> first
                ));
        List<NewsArticle> toSave = inputUrls.stream()
                .filter(url -> !existingUrls.contains(url))
                .map(uniqueArticles::get)
                .toList();

        int totalSaved = 0;
        for (int i = 0; i < toSave.size(); i += BATCH_SIZE) {
            List<NewsArticle> batch = toSave.subList(i, Math.min(i + BATCH_SIZE, toSave.size()));
            newsArticleRepository.saveAll(batch);
            totalSaved += batch.size();
        }
        return totalSaved;
    }

}
