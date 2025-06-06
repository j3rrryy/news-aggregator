package dev.j3rrryy.news_aggregator.parser.impl;

import dev.j3rrryy.news_aggregator.entity.NewsArticle;
import dev.j3rrryy.news_aggregator.enums.Category;
import dev.j3rrryy.news_aggregator.enums.Source;
import dev.j3rrryy.news_aggregator.enums.Status;
import dev.j3rrryy.news_aggregator.parser.config.ParserConfig;
import dev.j3rrryy.news_aggregator.parser.config.ParserProperties;
import dev.j3rrryy.news_aggregator.parser.service.*;
import org.assertj.core.api.AssertionsForClassTypes;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SvpressaRuParserTest {

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

    private SvpressaRuParser parser;

    @BeforeEach
    void setup() {
        ParserConfig mockConfig = mock(ParserConfig.class);
        when(mockConfig.getRateLimitPerSecond()).thenReturn(1.0);
        when(parserProperties.getConfigs()).thenReturn(Map.of(Source.SVPRESSA_RU, mockConfig));

        parser = new SvpressaRuParser(
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
                <article class="b-article_item">
                  <a class="b-article__title" href="https://example.com/test-url-2"></a>
                  <div class="b-article__date">7 мая 2025</div>
                </article>
                <article class="b-article_item">
                  <a class="b-article__title" href="https://example.com/test-url-1"></a>
                  <div class="b-article__date">1 мая 2025</div>
                </article>
                """;

        Document doc = Jsoup.parse(html);
        Set<String> urls = parser.getPageUrls(doc,
                LocalDateTime.of(2025, 5, 2, 0, 0));

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
                <article class="b-article_item">
                  <div class="b-article__date">7 мая 2025</div>
                </article>
                """;

        Document doc = Jsoup.parse(html);
        Set<String> urls = parser.getPageUrls(doc,
                LocalDateTime.of(2025, 5, 1, 0, 0));

        assertThat(urls).isEmpty();
    }

    @Test
    void getPageUrls_latestPublishedAtNull() {
        String html = """
                <article class="b-article_item">
                  <a class="b-article__title" href="https://example.com/test-url-2"></a>
                  <div class="b-article__date">7 мая</div>
                </article>
                """;

        Document doc = Jsoup.parse(html);
        Set<String> urls = parser.getPageUrls(doc, null);

        assertThat(urls).hasSize(1);
        assertThat(urls).containsExactly("https://example.com/test-url-2");
    }

    @Test
    void parseNewsArticle_parsesAllFieldsCorrectly() {
        String html = """
                <h1 class="b-text__title">test title</h1>
                <div class="b-text__block">
                  <p>test content 1.</p>
                  <p>test content 2.</p>
                </div>
                <a class="b-tag__link">#java</a>
                <a class="b-tag__link">#spring</a>
                <div class="b-text__img">
                  <img src="https://example.com/test-media-url-1"/>
                  <img src="https://example.com/test-media-url-2"/>
                </div>
                <div class="b-text__date">07 мая 2025 00:00</div>
                """;

        Document doc = Jsoup.parse(html, "test url");
        Optional<NewsArticle> opt = parser.parseNewsArticle(doc, Category.ECONOMICS);

        AssertionsForClassTypes.assertThat(opt).isPresent();
        NewsArticle article = opt.orElseThrow();

        AssertionsForClassTypes.assertThat(article.getTitle()).isEqualTo("test title");
        AssertionsForClassTypes.assertThat(article.getSummary()).isEqualTo("test content 1.");
        AssertionsForClassTypes.assertThat(article.getContent()).isEqualTo(
                "test content 1.\n\ntest content 2."
        );
        assertThat(article.getCategory()).isEqualTo(Category.ECONOMICS);
        assertThat(article.getKeywords()).containsExactlyInAnyOrder("Java", "Spring");
        assertThat(article.getMediaUrls()).containsExactlyInAnyOrder(
                "https://example.com/test-media-url-1", "https://example.com/test-media-url-2"
        );
        AssertionsForClassTypes.assertThat(article.getUrl()).isEqualTo("test url");
        assertThat(article.getStatus()).isEqualTo(Status.NEW);
        AssertionsForClassTypes.assertThat(article.getPublishedAt()).isEqualTo(
                LocalDateTime.of(2025, 5, 7, 0, 0)
        );
        assertThat(article.getSource()).isEqualTo(Source.SVPRESSA_RU);
    }

    @Test
    void parseNewsArticle_stopRequested_returnsEmpty() {
        when(parsingStatusManager.isStopRequested()).thenReturn(true);

        Document doc = Jsoup.parse("", "test url");
        Optional<NewsArticle> opt = parser.parseNewsArticle(doc, Category.POLITICS);

        AssertionsForClassTypes.assertThat(opt).isEmpty();
    }

    @Test
    void parseNewsArticle_filterEmptyContentKeywordsMediaUrls() {
        String html = """
                <h1 class="b-text__title">test title</h1>
                <div class="b-text__block">
                  <p>test content 1.</p>
                  <p></p>
                </div>
                <a class="b-tag__link">#java</a>
                <a class="b-tag__link"></a>
                <div class="b-text__img">
                  <img src="https://example.com/test-media-url-1"/>
                  <img src=""/>
                </div>
                <div class="b-text__date">07 мая 00:00</div>
                """;

        Document doc = Jsoup.parse(html, "test url");
        Optional<NewsArticle> opt = parser.parseNewsArticle(doc, Category.ECONOMICS);

        AssertionsForClassTypes.assertThat(opt).isPresent();
        NewsArticle article = opt.orElseThrow();

        AssertionsForClassTypes.assertThat(article.getTitle()).isEqualTo("test title");
        AssertionsForClassTypes.assertThat(article.getSummary()).isEqualTo("test content 1.");
        AssertionsForClassTypes.assertThat(article.getContent()).isEqualTo("test content 1.");
        assertThat(article.getCategory()).isEqualTo(Category.ECONOMICS);
        assertThat(article.getKeywords()).containsExactlyInAnyOrder("Java");
        assertThat(article.getMediaUrls()).containsExactlyInAnyOrder("https://example.com/test-media-url-1");
        AssertionsForClassTypes.assertThat(article.getUrl()).isEqualTo("test url");
        assertThat(article.getStatus()).isEqualTo(Status.NEW);
        AssertionsForClassTypes.assertThat(article.getPublishedAt()).isEqualTo(
                LocalDateTime.of(LocalDate.now().getYear(), 5, 7, 0, 0)
        );
        assertThat(article.getSource()).isEqualTo(Source.SVPRESSA_RU);
    }

    @Test
    void parseNewsArticle_malformedHtml_returnsEmpty() {
        Document doc = Jsoup.parse("", "test url");
        Optional<NewsArticle> opt = parser.parseNewsArticle(doc, Category.SOCIETY);

        AssertionsForClassTypes.assertThat(opt).isEmpty();
    }

}
