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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RtRuParser extends NewsParser {

    private static final int INITIAL_PAGE = 0;
    private static final String URL_TEMPLATE = """
            https://russian.rt.com/listing/type.ArticleVideoGallery.trend.%s/prepare/all-trends-new/50/%s
            """;
    private static final String CONTENT_SELECTOR = """
            div.article__text > p, div.article__text > h1, div.article__text > h2, div.article__text > h3,
            div.article__text > h4, div.article__text > h5, div.article__text > h6, div.article__text > blockquote
            """;
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-d HH:mm");

    @Autowired
    public RtRuParser(
            ParserProperties parserProperties,
            PageFetcher pageFetcher,
            ParsingStatusManager parsingStatusManager,
            ExecutorService ioExecutor,
            ExecutorService cpuExecutor,
            ParsingService parsingService
    ) {
        super(
                Source.RT_RU,
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
    protected Set<String> getPageUrls(Document doc, LocalDateTime latestPublishedAt) {
        if (parsingStatusManager.isStopRequested()) return Set.of();

        Elements newsArticles = doc.select("li.listing__column");
        Set<String> urls = new HashSet<>();

        for (Element article : newsArticles) {
            try {
                String url = Objects.requireNonNull(article.selectFirst("a.link"))
                        .absUrl("href");
                String publishedAtAttr = Objects.requireNonNull(article.selectFirst("time.date"))
                        .attr("datetime");
                LocalDateTime publishedAt = LocalDateTime.parse(publishedAtAttr, dateTimeFormatter);

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
            String title = Objects.requireNonNull(doc.selectFirst("h1.article__heading"))
                    .text()
                    .trim();
            String summary = Objects.requireNonNull(doc.selectFirst("div.article__summary"))
                    .text()
                    .trim()
                    .replaceFirst("^(.*?[.!?]).*", "$1");
            String content = Objects.requireNonNull(doc.selectFirst("div.article__summary")).text().trim()
                    + "\n\n"
                    + doc.select(CONTENT_SELECTOR).stream()
                    .map(Element::text)
                    .map(String::trim)
                    .filter(text -> !text.isEmpty())
                    .collect(Collectors.joining("\n\n"));
            Set<String> keywords = doc.select("a.tags-trends__link").stream()
                    .map(Element::text)
                    .map(String::trim)
                    .filter(text -> !text.isEmpty())
                    .map(kw -> Character.toUpperCase(kw.charAt(0)) + kw.substring(1))
                    .collect(Collectors.toSet());
            Set<String> mediaUrls = doc.select("img.article__cover-image").stream()
                    .map(media -> media.absUrl("src"))
                    .filter(text -> !text.isEmpty())
                    .collect(Collectors.toSet());
            String url = doc.location();

            String publishedAtAttr = Objects.requireNonNull(doc.selectFirst("time.date"))
                    .attr("datetime");
            LocalDateTime publishedAt = LocalDateTime.parse(publishedAtAttr, dateTimeFormatter);

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
                    .source(source)
                    .build();
            return Optional.of(newsArticle);
        } catch (Exception e) {
            log.debug("Skipping invalid article {}", doc.location());
            return Optional.empty();
        }
    }

}
