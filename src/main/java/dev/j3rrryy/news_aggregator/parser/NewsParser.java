package dev.j3rrryy.news_aggregator.parser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import dev.j3rrryy.news_aggregator.entity.NewsArticle;
import dev.j3rrryy.news_aggregator.enums.Category;
import dev.j3rrryy.news_aggregator.repository.NewsArticleRepository;
import dev.j3rrryy.news_aggregator.service.v1.ParsingStatusManager;
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
import java.util.stream.IntStream;

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

    private final int INITIAL_PAGE;
    private final String URL_TEMPLATE;
    private final RateLimiter rateLimiter;
    private final ExecutorService ioExecutor;
    private final ExecutorService cpuExecutor;
    private final Map<Category, Set<String>> urlMap;
    private final ParsingStatusManager parsingStatusManager;
    private final NewsArticleRepository newsArticleRepository;

    protected abstract Set<String> getPageUrls(Document doc, LocalDateTime latestPublishedAt);

    protected abstract Optional<NewsArticle> parseNewsArticle(Document doc, Category category);

    public void parse(Map<Category, LocalDateTime> latestPublishedAtByCategory) {
        for (Map.Entry<Category, Set<String>> entry : urlMap.entrySet()) {
            Category category = entry.getKey();
            LocalDateTime latestPublishedAt = latestPublishedAtByCategory.get(category);

            for (String path : entry.getValue()) {
                Set<String> articleUrls = new HashSet<>();

                int saved = 0;
                int fetchedPages = 0;
                Integer firstPageHash = null;
                boolean endReached = false;

                while (!endReached) {
                    int startPage = fetchedPages + INITIAL_PAGE;
                    int endPage = fetchedPages + INITIAL_PAGE + 19;

                    List<CompletableFuture<Optional<Document>>> pageFutures = IntStream
                            .rangeClosed(startPage, endPage)
                            .mapToObj(page ->
                                    CompletableFuture.supplyAsync(() -> fetchPage(
                                                    path, page, parsingStatusManager.isStopRequested()
                                            ),
                                            ioExecutor
                                    )
                            )
                            .toList();
                    CompletableFuture.allOf(pageFutures.toArray(CompletableFuture[]::new)).join();

                    List<Document> pages = pageFutures.stream()
                            .map(CompletableFuture::join)
                            .flatMap(Optional::stream)
                            .toList();
                    if (pages.isEmpty()) break;

                    for (int i = 0; i < pages.size(); i++) {
                        int pageNum = startPage + i;
                        Document doc = pages.get(i);
                        int hash = doc.text().hashCode();

                        if (pageNum == INITIAL_PAGE) {
                            firstPageHash = hash;
                        } else if (hash == firstPageHash) {
                            endReached = true;
                            break;
                        }

                        Set<String> urls = getPageUrls(doc, latestPublishedAt);
                        if (urls.isEmpty()) {
                            endReached = true;
                            break;
                        }
                        articleUrls.addAll(urls);
                    }

                    fetchedPages += pages.size();
                    saved += fetchAndSaveArticles(
                            articleUrls,
                            category,
                            latestPublishedAt,
                            parsingStatusManager.isStopRequested()
                    );
                    log.info("Saved {} new articles from {} pages, category: {}", saved, fetchedPages, category);
                }
            }
        }
    }

    protected Optional<Document> fetchPage(String path, int page, boolean stopRequested) {
        String url = URL_TEMPLATE.formatted(path, page);
        return downloadPage(fetchGet(url), url, stopRequested);
    }

    protected Optional<Document> downloadPage(Callable<Document> pageLoader, String urlForLog, boolean stopRequested) {
        if (stopRequested || Thread.interrupted()) return Optional.empty();
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

    protected Callable<Document> fetchGet(String url) {
        return () -> Jsoup.connect(url)
                .userAgent(getNextUserAgent())
                .headers(headers)
                .get();
    }

    protected Callable<Document> fetchPost(String url, String body) {
        return () -> {
            String json = Jsoup.connect(url)
                    .userAgent(getNextUserAgent())
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

    protected int fetchAndSaveArticles(
            Set<String> articleUrls,
            Category category,
            LocalDateTime latestPublishedAt,
            boolean stopRequested
    ) {
        Set<CompletableFuture<Optional<NewsArticle>>> articleFutures = articleUrls.stream()
                .map(articleUrl -> CompletableFuture
                        .supplyAsync(() -> downloadPage(fetchGet(articleUrl), articleUrl, stopRequested), ioExecutor)
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

        newsArticleRepository.saveAll(toSave);
        return toSave.size();
    }

}
