package dev.j3rrryy.news_aggregator.parser.impl;

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

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RtRuParserTest {

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

    private RtRuParser parser;

    @BeforeEach
    void setup() {
        ParserConfig mockConfig = mock(ParserConfig.class);
        when(mockConfig.getRateLimitPerSecond()).thenReturn(1.0);
        when(parserProperties.getConfigs()).thenReturn(Map.of(Source.RT_RU, mockConfig));

        parser = new RtRuParser(
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
    void getPageUrls_includesOnlyNewerThanLatest() {
        String html = """
                <ul>
                  <li class="listing__column">
                    <a class="link" href="https://example.com/test-url-2">
                      <time class="date" datetime="2025-05-07 00:00"></time>
                    </a>
                  </li>
                  <li class="listing__column">
                    <a class="link" href="https://example.com/test-url-1">
                      <time class="date" datetime="2025-05-01 00:00"></time>
                    </a>
                  </li>
                </ul>
                """;

        Document doc = Jsoup.parse(html);
        Set<String> urls = parser.getPageUrls(doc,
                LocalDateTime.of(2025, 5, 1, 0, 1));

        assertThat(urls).hasSize(1);
        assertThat(urls).containsExactly("https://example.com/test-url-2");
    }

    @Test
    void getPageUrls_noArticles_returnsEmptySet() {
        Document doc = Jsoup.parse("<ul></ul>");
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
                <ul>
                  <li class="listing__column">
                    <time class="date" datetime="2025-05-07 00:00"></time>
                  </li>
                </ul>
                """;

        Document doc = Jsoup.parse(html);
        Set<String> urls = parser.getPageUrls(doc,
                LocalDateTime.of(2025, 5, 1, 0, 0));

        assertThat(urls).isEmpty();
    }

    @Test
    void getPageUrls_latestPublishedAtNull() {
        String html = """
                <ul>
                  <li class="listing__column">
                    <a class="link" href="https://example.com/test-url-2">
                      <time class="date" datetime="2025-05-07 00:00"></time>
                    </a>
                  </li>
                </ul>
                """;

        Document doc = Jsoup.parse(html);
        Set<String> urls = parser.getPageUrls(doc, null);

        assertThat(urls).hasSize(1);
        assertThat(urls).containsExactly("https://example.com/test-url-2");
    }

    @Test
    void parseNewsArticle_parsesAllFieldsCorrectly() {
        String html = """
                <h1 class="article__heading">test title</h1>
                <div class="article__summary">test summary.</div>
                <div class="tags">
                  <a class="tags-trends__link">java</a>
                  <a class="tags-trends__link">spring</a>
                </div>
                <img class="article__cover-image" src="https://example.com/test-media-url-1"/>
                <img class="article__cover-image" src="https://example.com/test-media-url-2"/>
                <div class="article__text">
                  <p>test content 1.</p>
                  <h2>test content header.</h2>
                  <p>test content 2.</p>
                  <blockquote>test content quote.</blockquote>
                </div>
                <time class="date" datetime="2025-05-07 00:00"></time>
                """;

        Document doc = Jsoup.parse(html, "test url");
        Optional<NewsArticle> opt = parser.parseNewsArticle(doc, Category.ECONOMICS);

        assertThat(opt).isPresent();
        NewsArticle article = opt.orElseThrow();

        assertThat(article.getTitle()).isEqualTo("test title");
        assertThat(article.getSummary()).isEqualTo("test summary.");
        assertThat(article.getContent()).isEqualTo(
                "test summary.\n\ntest content 1.\n\ntest content header.\n\ntest content 2.\n\ntest content quote."
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
        assertThat(article.getSource()).isEqualTo(Source.RT_RU);
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
                <h1 class="article__heading">test title</h1>
                <div class="article__summary">test summary.</div>
                <div class="tags">
                  <a class="tags-trends__link">java</a>
                  <a class="tags-trends__link"></a>
                </div>
                <img class="article__cover-image" src="https://example.com/test-media-url-1"/>
                <img class="article__cover-image" src=""/>
                <div class="article__text">
                  <p>test content 1.</p>
                  <p></p>
                </div>
                <time class="date" datetime="2025-05-07 00:00"></time>
                """;

        Document doc = Jsoup.parse(html, "test url");
        Optional<NewsArticle> opt = parser.parseNewsArticle(doc, Category.ECONOMICS);

        assertThat(opt).isPresent();
        NewsArticle article = opt.orElseThrow();

        assertThat(article.getTitle()).isEqualTo("test title");
        assertThat(article.getSummary()).isEqualTo("test summary.");
        assertThat(article.getContent()).isEqualTo("test summary.\n\ntest content 1.");
        assertThat(article.getCategory()).isEqualTo(Category.ECONOMICS);
        assertThat(article.getKeywords()).containsExactlyInAnyOrder("Java");
        assertThat(article.getMediaUrls()).containsExactlyInAnyOrder("https://example.com/test-media-url-1");
        assertThat(article.getUrl()).isEqualTo("test url");
        assertThat(article.getStatus()).isEqualTo(Status.NEW);
        assertThat(article.getPublishedAt()).isEqualTo(
                LocalDateTime.of(2025, 5, 7, 0, 0)
        );
        assertThat(article.getSource()).isEqualTo(Source.RT_RU);
    }

    @Test
    void parseNewsArticle_malformedHtml_returnsEmpty() {
        Document doc = Jsoup.parse("", "test url");
        Optional<NewsArticle> opt = parser.parseNewsArticle(doc, Category.SOCIETY);

        assertThat(opt).isEmpty();
    }

}
