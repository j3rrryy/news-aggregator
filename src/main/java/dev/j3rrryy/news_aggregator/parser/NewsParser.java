package dev.j3rrryy.news_aggregator.parser;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.RateLimiter;
import dev.j3rrryy.news_aggregator.entity.NewsArticle;
import dev.j3rrryy.news_aggregator.enums.Category;
import dev.j3rrryy.news_aggregator.enums.Source;
import dev.j3rrryy.news_aggregator.parser.config.ParserConfig;
import dev.j3rrryy.news_aggregator.parser.config.ParserProperties;
import dev.j3rrryy.news_aggregator.parser.service.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

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
    protected final ParsingStateStore parsingStateStore;
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
            ParsingStateStore parsingStateStore,
            ParsingStatusManager parsingStatusManager
    ) {
        this.source = source;
        this.ioExecutor = ioExecutor;
        this.cpuExecutor = cpuExecutor;
        this.initialPage = initialPage;
        this.urlTemplate = urlTemplate;
        this.pageFetcher = pageFetcher;
        this.parsingService = parsingService;
        this.parsingStateStore = parsingStateStore;
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

            for (String path : entry.getValue()) {
                parsingStateStore.getCurrentPage(source, category, path)
                        .ifPresent(startPage -> {
                            log.info("Found missing articles");
                            processTail(startPage, path, category);
                        });

                if (parsingStatusManager.isStopRequested()) return;
                processHead(path, category, latestPublishedAt);

                if (parsingStatusManager.isStopRequested()) return;
                parsingStateStore.clearState(source, category, path);
            }
        }
    }

    private void processTail(int startPage, String path, Category category) {
        int page = startPage;

        while (!parsingStatusManager.isStopRequested()) {
            Optional<Document> optDoc = fetchAsync(path, page);
            if (optDoc.isEmpty()) {
                parsingStateStore.updateCurrentPage(source, category, path, ++page);
                continue;
            }

            Set<String> urls = getPageUrls(optDoc.get(), null);
            if (urls.isEmpty()) break;

            int saved = parsingService.saveArticles(fetchAndParseArticles(urls, category, null));
            if (saved == 0) break;

            log.info("Saved {} missing articles from {}, page {}", saved, category, page);
            parsingStateStore.updateCurrentPage(source, category, path, ++page);
        }
    }

    private void processHead(String path, Category category, LocalDateTime latestPublishedAt) {
        int page = initialPage;

        while (!parsingStatusManager.isStopRequested()) {
            Optional<Document> optDoc = fetchAsync(path, page);
            if (optDoc.isEmpty()) {
                parsingStateStore.updateCurrentPage(source, category, path, page);
                break;
            }

            Set<String> urls = getPageUrls(optDoc.get(), latestPublishedAt);
            if (urls.isEmpty() && parsingStatusManager.isStopRequested()) {
                parsingStateStore.updateCurrentPage(source, category, path, page);
                break;
            }
            if (urls.isEmpty()) break;

            int saved = parsingService.saveArticles(fetchAndParseArticles(urls, category, latestPublishedAt));
            if (saved == 0) break;

            log.info("Saved {} new articles from {}, page {}", saved, category, page);
            parsingStateStore.updateCurrentPage(source, category, path, ++page);
        }
    }

    private Optional<Document> fetchAsync(String path, int page) {
        return CompletableFuture
                .supplyAsync(() -> fetchPage(path, page), ioExecutor)
                .join();
    }

    protected Optional<Document> fetchPage(String path, int page) {
        String url = urlTemplate.formatted(path, page);
        return pageFetcher.downloadPage(pageFetcher.fetchGet(url), url, rateLimiter);
    }

    @VisibleForTesting
    List<NewsArticle> fetchAndParseArticles(
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
