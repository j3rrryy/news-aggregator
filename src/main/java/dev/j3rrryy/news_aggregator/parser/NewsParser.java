package dev.j3rrryy.news_aggregator.parser;

import com.google.common.util.concurrent.RateLimiter;
import dev.j3rrryy.news_aggregator.entity.NewsArticle;
import dev.j3rrryy.news_aggregator.enums.Category;
import dev.j3rrryy.news_aggregator.enums.Source;
import dev.j3rrryy.news_aggregator.parser.config.ParserConfig;
import dev.j3rrryy.news_aggregator.parser.config.ParserProperties;
import dev.j3rrryy.news_aggregator.parser.service.PageFetcher;
import dev.j3rrryy.news_aggregator.parser.service.ParsingService;
import dev.j3rrryy.news_aggregator.parser.service.ParsingStatusManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;

@Slf4j
public abstract class NewsParser {

    @Getter
    protected final Source source;
    protected final int initialPage;
    protected final String urlTemplate;
    protected final RateLimiter rateLimiter;
    protected final PageFetcher pageFetcher;
    protected final ParserConfig parserConfig;
    protected final ExecutorService ioExecutor;
    protected final ExecutorService cpuExecutor;
    protected final ParsingService parsingService;
    protected final ParsingStatusManager parsingStatusManager;

    protected NewsParser(
            Source source,
            int initialPage,
            String urlTemplate,
            PageFetcher pageFetcher,
            ExecutorService ioExecutor,
            ExecutorService cpuExecutor,
            ParsingService parsingService,
            ParserProperties parserProperties,
            ParsingStatusManager parsingStatusManager
    ) {
        this.source = source;
        this.ioExecutor = ioExecutor;
        this.cpuExecutor = cpuExecutor;
        this.initialPage = initialPage;
        this.urlTemplate = urlTemplate;
        this.pageFetcher = pageFetcher;
        this.parsingService = parsingService;
        this.parsingStatusManager = parsingStatusManager;
        this.parserConfig = parserProperties.getConfigs().get(source);
        this.rateLimiter = RateLimiter.create(parserConfig.getRateLimitPerSecond());
    }

    protected abstract Set<String> getPageUrls(Document doc, LocalDateTime latestPublishedAt);

    protected abstract Optional<NewsArticle> parseNewsArticle(Document doc, Category category);

    public void parse(Map<Category, LocalDateTime> latestPublishedAtByCategory) {
        for (Map.Entry<Category, Set<String>> entry : parserConfig.getCategoryUrls().entrySet()) {
            Category category = entry.getKey();
            LocalDateTime latestPublishedAt = latestPublishedAtByCategory.get(category);
            for (String path : entry.getValue()) processPath(path, category, latestPublishedAt);
        }
    }

    private void processPath(String path, Category category, LocalDateTime latestPublishedAt) {
        int fetchedPages = 0;
        Integer firstPageHash = null;
        boolean endReached = false;

        while (!endReached) {
            Set<String> articleUrls = new HashSet<>();

            int startPage = fetchedPages + initialPage;
            int endPage = fetchedPages + initialPage + 19;

            List<Document> pages = fetchPages(path, startPage, endPage);
            if (pages.isEmpty()) break;

            for (int i = 0; i < pages.size(); i++) {
                int pageNum = startPage + i;
                Document doc = pages.get(i);
                int hash = doc.text().hashCode();

                if (pageNum == initialPage) {
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
            int saved = parsingService.saveArticles(articles);
            fetchedPages += pages.size();

            if (saved > 0) log.info("Saved {} new articles from {}", saved, category);
        }
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
        String url = urlTemplate.formatted(path, page);
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
