package dev.j3rrryy.news_aggregator.parser.impl;

import com.google.common.util.concurrent.RateLimiter;
import dev.j3rrryy.news_aggregator.entity.NewsArticle;
import dev.j3rrryy.news_aggregator.enums.Category;
import dev.j3rrryy.news_aggregator.enums.Source;
import dev.j3rrryy.news_aggregator.enums.Status;
import dev.j3rrryy.news_aggregator.parser.config.ParserConfig;
import dev.j3rrryy.news_aggregator.parser.config.ParserProperties;
import dev.j3rrryy.news_aggregator.parser.service.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AifRuParserTest {

    @Mock
    PageFetcher pageFetcher;

    @Mock
    ExecutorService ioExecutor;

    @Mock
    ExecutorService cpuExecutor;

    @Mock
    ParsingService parsingService;

    @Mock
    ParserProperties parserProperties;

    @Mock
    ParsingStateStore parsingStateStore;

    @Mock
    ParsingStatusManager parsingStatusManager;

    private AifRuParser parser;

    @BeforeEach
    void setup() {
        ParserConfig mockConfig = mock(ParserConfig.class);
        when(mockConfig.getRateLimitPerSecond()).thenReturn(1.0);
        when(parserProperties.getConfigs()).thenReturn(Map.of(Source.AIF_RU, mockConfig));

        parser = new AifRuParser(
                pageFetcher,
                ioExecutor,
                cpuExecutor,
                parsingService,
                parserProperties,
                parsingStateStore,
                parsingStatusManager
        );
    }

    @Test
    void fetchPage() {
        String expectedUrl = "https://aif.ru/test-path";
        String expectedBody = "page=52";

        Callable<Document> callable = () -> new Document("test url");
        Document expectedDoc = Jsoup.parse("", expectedUrl);

        when(pageFetcher.fetchPost(expectedUrl, expectedBody)).thenReturn(callable);
        when(pageFetcher.downloadPage(any(), anyString(), any(RateLimiter.class)))
                .thenReturn(Optional.of(expectedDoc));

        Optional<Document> result = parser.fetchPage("test-path", 52);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow()).isSameAs(expectedDoc);
    }

    @Test
    void getPageUrls_includesOnlyNewerThanLatest() {
        String html = """
                <div class="list_item">
                  <div class="box_info">
                    <a href="https://example.com/test-url-2"></a>
                    <span class="text_box__date">00:00</span>
                  </div>
                </div>
                <div class="list_item">
                  <div class="box_info">
                    <a href="https://example.com/test-url-1"></a>
                    <span class="text_box__date">01.05.2025 00:00</span>
                  </div>
                </div>
                """;

        Document doc = Jsoup.parse(html);
        Set<String> urls = parser.getPageUrls(doc,
                LocalDateTime.of(LocalDate.now(), LocalTime.of(0, 0)));

        assertThat(urls).hasSize(1);
        assertThat(urls).containsExactly("https://example.com/test-url-2");
    }

    @Test
    void getPageUrls_noArticles_returnsEmptySet() {
        Document doc = Jsoup.parse("<div></div>");
        Set<String> urls = parser.getPageUrls(doc,
                LocalDateTime.of(2025, 5, 7, 0, 0));

        assertThat(urls).isEmpty();
    }

    @Test
    void getPageUrls_stopRequested_returnsEmptySet() {
        when(parsingStatusManager.isStopRequested()).thenReturn(true);

        Document doc = Jsoup.parse("");
        Set<String> urls = parser.getPageUrls(doc,
                LocalDateTime.of(2025, 5, 7, 0, 0));

        assertThat(urls).isEmpty();
    }

    @Test
    void getPageUrls_noUrl_skipsArticle() {
        String html = """
                <div class="list_item">
                  <div class="box_info">
                    <span class="text_box__date">01.05.2025 00:00</span>
                  </div>
                </div>
                """;

        Document doc = Jsoup.parse(html);
        Set<String> urls = parser.getPageUrls(doc,
                LocalDateTime.of(2025, 5, 1, 0, 0));

        assertThat(urls).isEmpty();
    }

    @Test
    void getPageUrls_latestPublishedAtNull() {
        String html = """
                <div class="list_item">
                  <div class="box_info">
                    <a href="https://example.com/test-url-2"></a>
                    <span class="text_box__date">07.05.2025 00:00</span>
                  </div>
                </div>
                """;

        Document doc = Jsoup.parse(html);
        Set<String> urls = parser.getPageUrls(doc, null);

        assertThat(urls).hasSize(1);
        assertThat(urls).containsExactly("https://example.com/test-url-2");
    }

    @Test
    void parseNewsArticle_parsesAllFieldsCorrectly() {
        String html = """
                <h1 itemprop="headline">test title</h1>
                <div class="article_text">
                  <p>test content 1.</p>
                  <h2>test content header.</h2>
                  <p>test content 2.</p>
                </div>
                <span itemprop="keywords">java</span>
                <span itemprop="keywords">spring</span>
                <img itemprop="image" src="https://example.com/test-media-url-1"/>
                <img itemprop="image" src="https://example.com/test-media-url-2"/>
                <time itemprop="datePublished">07.05.2025 00:00</time>
                """;

        Document doc = Jsoup.parse(html, "test url");
        Optional<NewsArticle> opt = parser.parseNewsArticle(doc, Category.ECONOMICS);

        assertThat(opt).isPresent();
        NewsArticle article = opt.orElseThrow();

        assertThat(article.getTitle()).isEqualTo("test title");
        assertThat(article.getSummary()).isEqualTo("test content 1.");
        assertThat(article.getContent()).isEqualTo(
                "test content 1.\n\ntest content header.\n\ntest content 2."
        );
        assertThat(article.getCategory()).isEqualTo(Category.ECONOMICS);
        assertThat(article.getKeywords()).containsExactlyInAnyOrder("Java", "Spring");
        assertThat(article.getMediaUrls()).containsExactlyInAnyOrder(
                "https://example.com/test-media-url-1", "https://example.com/test-media-url-2"
        );
        assertThat(article.getUrl()).isEqualTo("test url");
        assertThat(article.getStatus()).isEqualTo(Status.NEW);
        assertThat(article.getPublishedAt()).isEqualTo(
                LocalDateTime.of(2025, 5, 7, 0, 0)
        );
        assertThat(article.getSource()).isEqualTo(Source.AIF_RU);
    }

    @Test
    void parseNewsArticle_stopRequested_returnsEmpty() {
        when(parsingStatusManager.isStopRequested()).thenReturn(true);

        Document doc = Jsoup.parse("", "test url");
        Optional<NewsArticle> opt = parser.parseNewsArticle(doc, Category.POLITICS);

        assertThat(opt).isEmpty();
    }

    @Test
    void parseNewsArticle_filterEmptyContentKeywordsMediaUrls() {
        String html = """
                <h1 itemprop="headline">test title</h1>
                <div class="article_text">
                  <p>test content 1.</p>
                  <p></p>
                </div>
                <span itemprop="keywords">java</span>
                <span itemprop="keywords"></span>
                <img itemprop="image" src="https://example.com/test-media-url-1"/>
                <img itemprop="image" src=""/>
                <time itemprop="datePublished">01:23</time>
                """;

        Document doc = Jsoup.parse(html, "test url");
        Optional<NewsArticle> opt = parser.parseNewsArticle(doc, Category.ECONOMICS);

        assertThat(opt).isPresent();
        NewsArticle article = opt.orElseThrow();

        assertThat(article.getTitle()).isEqualTo("test title");
        assertThat(article.getSummary()).isEqualTo("test content 1.");
        assertThat(article.getContent()).isEqualTo("test content 1.");
        assertThat(article.getCategory()).isEqualTo(Category.ECONOMICS);
        assertThat(article.getKeywords()).containsExactlyInAnyOrder("Java");
        assertThat(article.getMediaUrls()).containsExactlyInAnyOrder("https://example.com/test-media-url-1");
        assertThat(article.getUrl()).isEqualTo("test url");
        assertThat(article.getStatus()).isEqualTo(Status.NEW);
        assertThat(article.getPublishedAt()).isEqualTo(
                LocalDateTime.of(LocalDate.now(), LocalTime.of(1, 23))
        );
        assertThat(article.getSource()).isEqualTo(Source.AIF_RU);
    }

    @Test
    void parseNewsArticle_malformedHtml_returnsEmpty() {
        Document doc = Jsoup.parse("", "test url");
        Optional<NewsArticle> opt = parser.parseNewsArticle(doc, Category.SOCIETY);

        assertThat(opt).isEmpty();
    }

}
