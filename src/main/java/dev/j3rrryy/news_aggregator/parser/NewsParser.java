package dev.j3rrryy.news_aggregator.parser;

import com.google.common.util.concurrent.RateLimiter;
import dev.j3rrryy.news_aggregator.entity.NewsArticle;
import dev.j3rrryy.news_aggregator.enums.Category;
import dev.j3rrryy.news_aggregator.enums.Source;
import dev.j3rrryy.news_aggregator.parser.service.PageFetcher;
import dev.j3rrryy.news_aggregator.parser.service.ParsingService;
import dev.j3rrryy.news_aggregator.parser.service.ParsingStatusManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;

@Slf4j
@RequiredArgsConstructor
public abstract class NewsParser {

    protected final PageFetcher pageFetcher;
    protected final ParsingStatusManager parsingStatusManager;

    private final int INITIAL_PAGE;
    private final String URL_TEMPLATE;
    private final RateLimiter rateLimiter;
    private final ExecutorService ioExecutor;
    private final ExecutorService cpuExecutor;
    private final ParsingService parsingService;
    private final Map<Category, Set<String>> urlMap;

    public abstract Source getSource();

    protected abstract Set<String> getPageUrls(Document doc, LocalDateTime latestPublishedAt);

    protected abstract Optional<NewsArticle> parseNewsArticle(Document doc, Category category);

    public void parse(Map<Category, LocalDateTime> latestPublishedAtByCategory) {
        for (Map.Entry<Category, Set<String>> entry : urlMap.entrySet()) {
            Category category = entry.getKey();
            LocalDateTime latestPublishedAt = latestPublishedAtByCategory.get(category);

            for (String path : entry.getValue()) {
                int saved = processPath(path, category, latestPublishedAt);
                log.info("Saved {} new articles from {}", saved, category);
            }
        }
    }

    private int processPath(String path, Category category, LocalDateTime latestPublishedAt) {
        int saved = 0;
        int fetchedPages = 0;
        Integer firstPageHash = null;
        boolean endReached = false;

        while (!endReached) {
            Set<String> articleUrls = new HashSet<>();

            int startPage = fetchedPages + INITIAL_PAGE;
            int endPage = fetchedPages + INITIAL_PAGE + 19;

            List<Document> pages = fetchPages(path, startPage, endPage);
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

            List<NewsArticle> articles = fetchAndParseArticles(articleUrls, category, latestPublishedAt);
            saved += parsingService.saveArticles(articles);
            fetchedPages += pages.size();
        }
        return saved;
    }

    private List<Document> fetchPages(String path, int startPage, int endPage) {
        List<CompletableFuture<Optional<Document>>> pageFutures = IntStream
                .rangeClosed(startPage, endPage)
                .mapToObj(page -> CompletableFuture.supplyAsync(() -> fetchPage(path, page), ioExecutor))
                .toList();

        CompletableFuture.allOf(pageFutures.toArray(CompletableFuture[]::new)).join();
        return pageFutures.stream()
                .map(CompletableFuture::join)
                .flatMap(Optional::stream)
                .toList();
    }

    protected Optional<Document> fetchPage(String path, int page) {
        String url = URL_TEMPLATE.formatted(path, page);
        return pageFetcher.downloadPage(pageFetcher.fetchGet(url), url, rateLimiter);
    }

    private List<NewsArticle> fetchAndParseArticles(
            Set<String> articleUrls, Category category, LocalDateTime latestPublishedAt
    ) {
        List<CompletableFuture<Optional<NewsArticle>>> articleFutures = articleUrls.stream()
                .map(articleUrl -> CompletableFuture
                        .supplyAsync(() -> pageFetcher.downloadPage(
                                pageFetcher.fetchGet(articleUrl), articleUrl, rateLimiter
                        ), ioExecutor)
                        .thenApplyAsync(optDoc -> optDoc
                                .flatMap(doc -> parseNewsArticle(doc, category)), cpuExecutor))
                .toList();

        CompletableFuture.allOf(articleFutures.toArray(CompletableFuture[]::new)).join();
        return articleFutures.stream()
                .map(CompletableFuture::join)
                .flatMap(Optional::stream)
                .filter(n -> latestPublishedAt == null || n.getPublishedAt().isAfter(latestPublishedAt))
                .toList();
    }

}
