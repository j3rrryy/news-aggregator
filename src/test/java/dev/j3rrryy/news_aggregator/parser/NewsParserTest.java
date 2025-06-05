package dev.j3rrryy.news_aggregator.parser;

import dev.j3rrryy.news_aggregator.entity.NewsArticle;
import dev.j3rrryy.news_aggregator.enums.Category;
import dev.j3rrryy.news_aggregator.enums.Source;
import dev.j3rrryy.news_aggregator.enums.Status;
import dev.j3rrryy.news_aggregator.parser.config.ParserConfig;
import dev.j3rrryy.news_aggregator.parser.config.ParserProperties;
import dev.j3rrryy.news_aggregator.parser.service.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NewsParserTest {

    @Mock
    PageFetcher pageFetcher;

    @Mock
    ParsingService parsingService;

    @Mock
    ParserProperties parserProperties;

    @Mock
    ParsingStateStore parsingStateStore;

    @Mock
    ParsingStatusManager parsingStatusManager;

    private Document mockDocument;
    private NewsParser newsParser;
    private ParserConfig parserConfig;
    private ExecutorService ioExecutor;
    private ExecutorService cpuExecutor;

    @BeforeEach
    void setUp() {
        ioExecutor = Executors.newSingleThreadExecutor();
        cpuExecutor = Executors.newSingleThreadExecutor();
        parserConfig = new ParserConfig();
        parserConfig.setRateLimitPerSecond(10);
        parserConfig.getCategoryUrls().put(Category.SCIENCE_TECH, Set.of("test url 1", "test url 2"));
        when(parserProperties.getConfigs()).thenReturn(Map.of(Source.RT_RU, parserConfig));
        mockDocument = Jsoup.parse("<html><body>test</body></html>");

        NewsArticle article = new NewsArticle(
                UUID.randomUUID(), "test title", "test summary",
                "test content", Category.SCIENCE_TECH, Set.of("java"), Set.of("test media url"),
                "test url", Status.NEW, LocalDateTime.now(), Source.RT_RU);

        newsParser = buildParser(Set.of("test url"), article);
    }

    @AfterEach
    void tearDown() {
        ioExecutor.shutdownNow();
        cpuExecutor.shutdownNow();
    }

    @Test
    void parse_shouldSaveArticlesFromTailAndHead() {
        when(parsingStatusManager.isStopRequested()).thenReturn(false);
        when(parsingStateStore.getCurrentPage(any(), any(), any())).thenReturn(Optional.of(1));
        when(pageFetcher.fetchGet(anyString())).thenReturn(() -> mockDocument);
        when(pageFetcher.downloadPage(any(), any(), any())).thenReturn(Optional.of(mockDocument));
        when(parsingService.saveArticles(any())).thenReturn(1).thenReturn(0);

        newsParser.parse(Map.of(Category.SCIENCE_TECH, LocalDateTime.now().minusHours(1)));

        verify(parsingService, atLeastOnce()).saveArticles(any());
        verify(parsingStateStore, atLeastOnce()).clearState(
                eq(Source.RT_RU),
                eq(Category.SCIENCE_TECH),
                anyString()
        );
        verify(parsingStateStore, atLeastOnce()).updateCurrentPage(
                eq(Source.RT_RU),
                eq(Category.SCIENCE_TECH),
                anyString(),
                eq(2)
        );
    }

    @Test
    void parse_shouldStopBeforeAnyProcessing_whenStopRequestedInitially() {
        when(parsingStatusManager.isStopRequested()).thenReturn(true);

        newsParser.parse(Map.of(Category.SCIENCE_TECH, LocalDateTime.now()));

        verify(parsingService, never()).saveArticles(any());
        verify(parsingStateStore, never()).clearState(any(), any(), any());
        verify(parsingStateStore, never()).updateCurrentPage(any(), any(), any(), anyInt());
    }

    @Test
    void parse_shouldStopAfterTailBeforeHead_whenStopRequestedAfterTail() {
        when(parsingStateStore.getCurrentPage(any(), any(), any())).thenReturn(Optional.empty());
        when(parsingStatusManager.isStopRequested()).thenReturn(false, true);

        newsParser.parse(Map.of(Category.SCIENCE_TECH, LocalDateTime.now()));

        verify(parsingService, never()).saveArticles(any());
        verify(parsingStateStore, never()).clearState(any(), any(), any());
    }

    @Test
    void processTail_shouldNotInvokeDownload_whenStopImmediately() {
        when(parsingStatusManager.isStopRequested()).thenReturn(true);

        newsParser.parse(Map.of(Category.SCIENCE_TECH, LocalDateTime.now()));

        verify(pageFetcher, never()).downloadPage(any(), any(), any());
        verify(parsingService, never()).saveArticles(any());
    }

    @Test
    void processTail_optDocEmpty_shouldUpdateCurrentPageOnceAndStop() {
        when(parsingStateStore.getCurrentPage(any(), any(), any())).thenReturn(Optional.of(5));
        when(parsingStatusManager.isStopRequested()).thenReturn(false, true);
        when(pageFetcher.fetchGet(anyString())).thenReturn(() -> mockDocument);
        when(pageFetcher.downloadPage(any(), any(), any())).thenReturn(Optional.empty());

        Map<Category, LocalDateTime> latestPublishedAtByCategory = new EnumMap<>(Category.class);
        latestPublishedAtByCategory.put(Category.SCIENCE_TECH, null);
        newsParser.parse(latestPublishedAtByCategory);

        verify(parsingService, never()).saveArticles(any());
        verify(parsingStateStore).updateCurrentPage(
                eq(Source.RT_RU),
                eq(Category.SCIENCE_TECH),
                anyString(),
                eq(6)
        );
    }

    @Test
    void processTail_urlsEmpty_shouldBreakImmediately() {
        when(parsingStateStore.getCurrentPage(any(), any(), any())).thenReturn(Optional.of(3));
        when(parsingStatusManager.isStopRequested()).thenReturn(false);
        when(pageFetcher.fetchGet(anyString())).thenReturn(() -> mockDocument);
        when(pageFetcher.downloadPage(any(), any(), any())).thenReturn(Optional.of(mockDocument));

        NewsParser parser = buildParser(Set.of(), null);

        Map<Category, LocalDateTime> latestPublishedAtByCategory = new EnumMap<>(Category.class);
        latestPublishedAtByCategory.put(Category.SCIENCE_TECH, null);
        parser.parse(latestPublishedAtByCategory);

        verify(parsingService, never()).saveArticles(any());
        verify(parsingStateStore, never()).updateCurrentPage(any(), any(), any(), anyInt());
    }

    @Test
    void processTail_savedZero_shouldBreakWithoutUpdatingPage() {
        when(parsingStateStore.getCurrentPage(any(), any(), any())).thenReturn(Optional.of(2));
        when(parsingStatusManager.isStopRequested()).thenReturn(false);
        when(pageFetcher.fetchGet(anyString())).thenReturn(() -> mockDocument);
        when(pageFetcher.downloadPage(any(), any(), any())).thenReturn(Optional.of(mockDocument));
        when(parsingService.saveArticles(any())).thenReturn(0);

        Map<Category, LocalDateTime> latestPublishedAtByCategory = new EnumMap<>(Category.class);
        latestPublishedAtByCategory.put(Category.SCIENCE_TECH, null);
        newsParser.parse(latestPublishedAtByCategory);

        verify(parsingService, times(4)).saveArticles(any());
        verify(parsingStateStore, never()).updateCurrentPage(any(), any(), any(), anyInt());
    }

    @Test
    void processHead_optDocEmpty_shouldUpdatePageAndBreak() {
        when(parsingStateStore.getCurrentPage(any(), any(), any())).thenReturn(Optional.empty());
        when(parsingStatusManager.isStopRequested()).thenReturn(false);
        when(pageFetcher.fetchGet(anyString())).thenReturn(() -> mockDocument);
        when(pageFetcher.downloadPage(any(), any(), any())).thenReturn(Optional.empty());

        Map<Category, LocalDateTime> latestPublishedAtByCategory = new EnumMap<>(Category.class);
        latestPublishedAtByCategory.put(Category.SCIENCE_TECH, null);
        newsParser.parse(latestPublishedAtByCategory);

        verify(parsingService, never()).saveArticles(any());
        verify(parsingStateStore, times(2)).updateCurrentPage(
                eq(Source.RT_RU),
                eq(Category.SCIENCE_TECH),
                anyString(),
                eq(1)
        );
    }

    @Test
    void processHead_urlsEmptyAndNotStopped_shouldBreakWithoutUpdate() {
        when(parsingStateStore.getCurrentPage(any(), any(), any())).thenReturn(Optional.empty());
        when(parsingStatusManager.isStopRequested()).thenReturn(false);
        when(pageFetcher.fetchGet(anyString())).thenReturn(() -> mockDocument);
        when(pageFetcher.downloadPage(any(), any(), any())).thenReturn(Optional.of(mockDocument));

        NewsParser parser = buildParser(Set.of(), null);

        Map<Category, LocalDateTime> latestPublishedAtByCategory = new EnumMap<>(Category.class);
        latestPublishedAtByCategory.put(Category.SCIENCE_TECH, null);
        parser.parse(latestPublishedAtByCategory);

        verify(parsingService, never()).saveArticles(any());
        verify(parsingStateStore, never()).updateCurrentPage(any(), any(), any(), anyInt());
    }

    @Test
    void processHead_urlsEmptyAndStopRequested_shouldUpdatePageAndBreak() {
        when(pageFetcher.fetchGet(anyString())).thenReturn(() -> mockDocument);
        when(pageFetcher.downloadPage(any(), any(), any())).thenReturn(Optional.of(mockDocument));
        when(parsingStateStore.getCurrentPage(any(), any(), any())).thenReturn(Optional.empty());
        when(parsingStatusManager.isStopRequested()).thenReturn(false, false, true);

        NewsParser parser = buildParser(Set.of(), null);

        Map<Category, LocalDateTime> latestPublishedAtByCategory = new EnumMap<>(Category.class);
        latestPublishedAtByCategory.put(Category.SCIENCE_TECH, null);
        parser.parse(latestPublishedAtByCategory);

        verify(parsingService, never()).saveArticles(any());
        verify(parsingStateStore, times(1)).updateCurrentPage(
                eq(Source.RT_RU),
                eq(Category.SCIENCE_TECH),
                anyString(),
                eq(1)
        );
    }

    @Test
    void processHead_savedZero_shouldBreakWithoutUpdate() {
        when(parsingStateStore.getCurrentPage(any(), any(), any())).thenReturn(Optional.empty());
        when(parsingStatusManager.isStopRequested()).thenReturn(false);
        when(pageFetcher.fetchGet(anyString())).thenReturn(() -> mockDocument);
        when(pageFetcher.downloadPage(any(), any(), any())).thenReturn(Optional.of(mockDocument));
        when(parsingService.saveArticles(any())).thenReturn(0);

        Map<Category, LocalDateTime> latestPublishedAtByCategory = new EnumMap<>(Category.class);
        latestPublishedAtByCategory.put(Category.SCIENCE_TECH, null);
        newsParser.parse(latestPublishedAtByCategory);

        verify(parsingService, times(2)).saveArticles(any());
        verify(parsingStateStore, never()).updateCurrentPage(any(), any(), any(), anyInt());
    }

    @Test
    void processHead_savedPositive_shouldIncrementPageUpdate() {
        when(parsingStateStore.getCurrentPage(any(), any(), any())).thenReturn(Optional.empty());
        when(parsingStatusManager.isStopRequested()).thenReturn(false);
        when(pageFetcher.fetchGet(anyString())).thenReturn(() -> mockDocument);
        when(pageFetcher.downloadPage(any(), any(), any())).thenReturn(Optional.of(mockDocument));
        when(parsingService.saveArticles(any())).thenReturn(2).thenReturn(0);

        Map<Category, LocalDateTime> latestPublishedAtByCategory = new EnumMap<>(Category.class);
        latestPublishedAtByCategory.put(Category.SCIENCE_TECH, null);
        newsParser.parse(latestPublishedAtByCategory);

        verify(parsingStateStore).updateCurrentPage(
                eq(Source.RT_RU),
                eq(Category.SCIENCE_TECH),
                anyString(),
                eq(2)
        );
    }

    @Test
    void fetchPage_shouldReturnEmptyIfDownloadFails() {
        when(pageFetcher.fetchGet(any())).thenReturn(() -> mockDocument);
        when(pageFetcher.downloadPage(any(), any(), any())).thenReturn(Optional.empty());

        Optional<Document> result = newsParser.fetchPage("test path", 42);
        assertTrue(result.isEmpty());
    }

    @Test
    void fetchAndParseArticles_shouldNotFilterWhenLatestIsNull() {
        LocalDateTime published = LocalDateTime.now().minusDays(1);
        NewsArticle oldArticle = new NewsArticle(
                UUID.randomUUID(), "old article", "test summary", "test content",
                Category.SCIENCE_TECH, Set.of("spring"), Set.of("test media url"), "test url",
                Status.NEW, published, Source.RT_RU
        );

        String url = "test url";
        mockFetcherSuccess(url);

        NewsParser parser = buildParser(Set.of(), oldArticle);
        List<NewsArticle> result = parser.fetchAndParseArticles(
                Set.of(url),
                Category.SCIENCE_TECH,
                null);

        assertEquals(1, result.size());
        assertEquals("old article", result.getFirst().getTitle());
    }

    @Test
    void fetchAndParseArticles_shouldFilterBasedOnLatestPublishedAt() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime latestPublishedAt = now.minusMinutes(5);

        NewsArticle fresh = new NewsArticle(
                UUID.randomUUID(), "fresh title", "test summary", "test content",
                Category.SCIENCE_TECH, Set.of("spring"), Set.of("test media url"), "fresh url",
                Status.NEW, now, Source.RT_RU
        );
        NewsArticle old = new NewsArticle(
                UUID.randomUUID(), "old title", "test summary", "test content",
                Category.SCIENCE_TECH, Set.of("java"), Set.of("test media url"), "old url",
                Status.NEW, now.minusHours(1), Source.RT_RU
        );

        mockFetcherSuccess("fresh url");
        mockFetcherSuccess("old url");

        NewsParser parser = buildParser(Set.of(), fresh);
        List<NewsArticle> onlyFresh = parser.fetchAndParseArticles(
                Set.of("fresh url"),
                Category.SCIENCE_TECH,
                latestPublishedAt
        );

        assertEquals(1, onlyFresh.size());
        assertEquals("fresh title", onlyFresh.getFirst().getTitle());

        parser = buildParser(Set.of(), old);
        List<NewsArticle> onlyOld = parser.fetchAndParseArticles(
                Set.of("old url"),
                Category.SCIENCE_TECH,
                latestPublishedAt
        );

        assertTrue(onlyOld.isEmpty());
    }

    @Test
    void fetchAndParseArticles_shouldSkipIfDownloadOrParseFails() {
        String url1 = "test url 1";
        String url2 = "test url 2";

        when(pageFetcher.fetchGet(url1)).thenReturn(() -> mockDocument);
        when(pageFetcher.downloadPage(any(), eq(url1), any())).thenReturn(Optional.empty());
        when(pageFetcher.fetchGet(url2)).thenReturn(() -> mockDocument);
        when(pageFetcher.downloadPage(any(), eq(url2), any())).thenReturn(Optional.of(mockDocument));

        NewsParser parser = buildParser(Set.of(), null);
        List<NewsArticle> result = parser.fetchAndParseArticles(
                Set.of(url1, url2),
                Category.SCIENCE_TECH,
                null
        );

        assertTrue(result.isEmpty());
    }

    private NewsParser buildParser(Set<String> pageUrls, NewsArticle articleToReturn) {
        when(parserProperties.getConfigs()).thenReturn(Map.of(Source.RT_RU, parserConfig));

        return new NewsParser(
                Source.RT_RU,
                1,
                "test url template",
                pageFetcher,
                ioExecutor,
                cpuExecutor,
                parsingService,
                parserProperties,
                parsingStateStore,
                parsingStatusManager
        ) {
            @Override
            protected Set<String> getPageUrls(Document doc, LocalDateTime latestPublishedAt) {
                return pageUrls;
            }

            @Override
            protected Optional<NewsArticle> parseNewsArticle(Document doc, Category category) {
                if (articleToReturn == null) return Optional.empty();
                return Optional.of(articleToReturn);
            }
        };
    }

    private void mockFetcherSuccess(String url) {
        when(pageFetcher.fetchGet(url)).thenReturn(() -> mockDocument);
        when(pageFetcher.downloadPage(any(), eq(url), any())).thenReturn(Optional.of(mockDocument));
    }

}
