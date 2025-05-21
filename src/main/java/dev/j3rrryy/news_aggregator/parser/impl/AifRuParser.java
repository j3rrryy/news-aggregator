package dev.j3rrryy.news_aggregator.parser.impl;

import dev.j3rrryy.news_aggregator.entity.NewsArticle;
import dev.j3rrryy.news_aggregator.enums.Category;
import dev.j3rrryy.news_aggregator.enums.Source;
import dev.j3rrryy.news_aggregator.enums.Status;
import dev.j3rrryy.news_aggregator.parser.NewsParser;
import dev.j3rrryy.news_aggregator.parser.config.ParserProperties;
import dev.j3rrryy.news_aggregator.parser.service.PageFetcher;
import dev.j3rrryy.news_aggregator.parser.service.ParsingService;
import dev.j3rrryy.news_aggregator.parser.service.ParsingStatusManager;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AifRuParser extends NewsParser {

    private static final int INITIAL_PAGE = 1;
    private static final String BODY_TEMPLATE = "page=%s";
    private static final String URL_TEMPLATE = "https://aif.ru/%s";
    private static final String SUMMARY_AND_CONTENT_SELECTOR = """
            div.article_text > p, div.article_text > h1, div.article_text > h2, div.article_text > h3,
            div.article_text > h4, div.article_text > h5, div.article_text > h6
            """;
    private static final DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
            .appendOptional(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
            .appendOptional(DateTimeFormatter.ofPattern("HH:mm"))
            .toFormatter();

    @Autowired
    public AifRuParser(
            ParserProperties parserProperties,
            PageFetcher pageFetcher,
            ParsingStatusManager parsingStatusManager,
            ExecutorService ioExecutor,
            ExecutorService cpuExecutor,
            ParsingService parsingService
    ) {
        super(
                Source.AIF_RU,
                INITIAL_PAGE,
                URL_TEMPLATE,
                pageFetcher,
                ioExecutor,
                cpuExecutor,
                parsingService,
                parserProperties,
                parsingStatusManager
        );
    }

    @Override
    protected Optional<Document> fetchPage(String path, int page) {
        String url = URL_TEMPLATE.formatted(path);
        return pageFetcher.downloadPage(pageFetcher.fetchPost(url, BODY_TEMPLATE.formatted(page)), url, rateLimiter);
    }

    @Override
    protected Set<String> getPageUrls(Document doc, LocalDateTime latestPublishedAt) {
        if (parsingStatusManager.isStopRequested()) return Set.of();

        Elements newsArticles = doc.select("div.list_item");
        Set<String> urls = new HashSet<>();

        for (Element article : newsArticles) {
            try {
                String url = Objects.requireNonNull(article.selectFirst("div.box_info a"))
                        .absUrl("href");
                String publishedAtText = Objects.requireNonNull(article.selectFirst("span.text_box__date"))
                        .text()
                        .trim();
                LocalDateTime publishedAt = parsePublishedAt(publishedAtText);

                if (!publishedAt.isSupported(ChronoField.YEAR))
                    publishedAt = LocalDate.now().atTime(publishedAt.toLocalTime());

                if (latestPublishedAt != null && publishedAt.isBefore(latestPublishedAt)) break;
                urls.add(url);
            } catch (Exception ignored) {
            }
        }
        return urls;
    }

    @Override
    protected Optional<NewsArticle> parseNewsArticle(Document doc, Category category) {
        if (parsingStatusManager.isStopRequested()) return Optional.empty();
        try {
            String title = Objects.requireNonNull(doc.selectFirst("h1[itemprop=headline]"))
                    .text()
                    .trim();
            String summary = Objects.requireNonNull(doc.selectFirst(SUMMARY_AND_CONTENT_SELECTOR))
                    .text()
                    .trim()
                    .replaceFirst("^(.*?[.!?]).*", "$1");
            String content = doc.select(SUMMARY_AND_CONTENT_SELECTOR).stream()
                    .map(Element::text)
                    .map(String::trim)
                    .filter(text -> !text.isEmpty())
                    .collect(Collectors.joining("\n\n"));
            Set<String> keywords = doc.select("span[itemprop=keywords]").stream()
                    .map(Element::text)
                    .map(String::trim)
                    .filter(text -> !text.isEmpty())
                    .map(kw -> Character.toUpperCase(kw.charAt(0)) + kw.substring(1))
                    .collect(Collectors.toSet());
            Set<String> mediaUrls = doc.select("img[itemprop=image]").stream()
                    .map(media -> media.absUrl("src"))
                    .filter(text -> !text.isEmpty())
                    .collect(Collectors.toSet());
            String url = doc.location();

            String publishedAtText = Objects.requireNonNull(doc.selectFirst("time[itemprop=datePublished]"))
                    .text()
                    .trim();
            LocalDateTime publishedAt = parsePublishedAt(publishedAtText);

            NewsArticle newsArticle = NewsArticle.builder()
                    .title(title)
                    .summary(summary)
                    .content(content)
                    .category(category)
                    .keywords(keywords)
                    .mediaUrls(mediaUrls)
                    .url(url)
                    .status(Status.NEW)
                    .publishedAt(publishedAt)
                    .source(Source.AIF_RU)
                    .build();
            return Optional.of(newsArticle);
        } catch (Exception e) {
            log.debug("Skipping invalid article {}", doc.location());
            return Optional.empty();
        }
    }

    private LocalDateTime parsePublishedAt(String text) {
        TemporalAccessor parsed = dateTimeFormatter.parse(text);
        if (parsed.isSupported(ChronoField.YEAR)) {
            return LocalDateTime.from(parsed);
        } else {
            LocalTime time = LocalTime.from(parsed);
            return LocalDate.now().atTime(time);
        }
    }

}
