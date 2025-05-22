package dev.j3rrryy.news_aggregator.parser.impl;

import dev.j3rrryy.news_aggregator.entity.NewsArticle;
import dev.j3rrryy.news_aggregator.enums.Category;
import dev.j3rrryy.news_aggregator.enums.Source;
import dev.j3rrryy.news_aggregator.enums.Status;
import dev.j3rrryy.news_aggregator.parser.NewsParser;
import dev.j3rrryy.news_aggregator.parser.config.ParserProperties;
import dev.j3rrryy.news_aggregator.parser.service.*;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static java.util.Map.entry;

@Slf4j
@Component
public class SvpressaRuParser extends NewsParser {

    private static final int INITIAL_PAGE = 1;
    private static final String URL_TEMPLATE = "https://svpressa.ru/%s/?page=%s";
    private static final Map<String, Integer> monthMap = Map.ofEntries(
            entry("января", 1), entry("февраля", 2), entry("марта", 3),
            entry("апреля", 4), entry("мая", 5), entry("июня", 6),
            entry("июля", 7), entry("августа", 8), entry("сентября", 9),
            entry("октября", 10), entry("ноября", 11), entry("декабря", 12)
    );

    @Autowired
    public SvpressaRuParser(
            PageFetcher pageFetcher,
            ExecutorService ioExecutor,
            ExecutorService cpuExecutor,
            ParsingService parsingService,
            ParserProperties parserProperties,
            ParsingStateStore parsingStateStore,
            ParsingStatusManager parsingStatusManager
    ) {
        super(
                Source.SVPRESSA_RU,
                INITIAL_PAGE,
                URL_TEMPLATE,
                pageFetcher,
                ioExecutor,
                cpuExecutor,
                parsingService,
                parserProperties,
                parsingStateStore,
                parsingStatusManager
        );
    }

    @Override
    protected Set<String> getPageUrls(Document doc, LocalDateTime latestPublishedAt) {
        if (parsingStatusManager.isStopRequested()) return Set.of();

        Elements newsArticles = doc.select("article.b-article_item");
        Set<String> urls = new HashSet<>();

        for (Element article : newsArticles) {
            try {
                String url = Objects.requireNonNull(article.selectFirst("a.b-article__title"))
                        .absUrl("href");
                String publishedAtText = Objects.requireNonNull(article.selectFirst("div.b-article__date"))
                        .text()
                        .trim();
                LocalDate publishedAt = parsePublishedAtPage(publishedAtText);

                if (latestPublishedAt != null && publishedAt.isBefore(latestPublishedAt.toLocalDate())) break;
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
            String title = Objects.requireNonNull(doc.selectFirst("h1.b-text__title"))
                    .text()
                    .trim();
            String summary = Objects.requireNonNull(doc.selectFirst("div.b-text__block > p"))
                    .text()
                    .trim()
                    .replaceFirst("^(.*?[.!?]).*", "$1");
            String content = doc.select("div.b-text__block > p").stream()
                    .map(Element::text)
                    .map(String::trim)
                    .filter(text -> !text.isEmpty())
                    .collect(Collectors.joining("\n\n"));
            Set<String> keywords = doc.select("a.b-tag__link").stream()
                    .map(Element::text)
                    .map(String::trim)
                    .filter(text -> !text.isEmpty())
                    .map(kw -> kw.substring(1))
                    .map(kw -> Character.toUpperCase(kw.charAt(0)) + kw.substring(1))
                    .collect(Collectors.toSet());
            Set<String> mediaUrls = doc.select("div.b-text__img img").stream()
                    .map(media -> media.absUrl("src"))
                    .filter(text -> !text.isEmpty())
                    .collect(Collectors.toSet());
            String url = doc.location();

            String publishedAtText = Objects.requireNonNull(doc.selectFirst("div.b-text__date"))
                    .text()
                    .trim();
            LocalDateTime publishedAt = parsePublishedAtArticle(publishedAtText);

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
                    .source(Source.SVPRESSA_RU)
                    .build();
            return Optional.of(newsArticle);
        } catch (Exception e) {
            log.debug("Skipping invalid article {}", doc.location());
            return Optional.empty();
        }
    }

    private LocalDate parsePublishedAtPage(String text) {
        String[] parts = text.split("\\s+");

        int day = Integer.parseInt(parts[0]);
        Integer month = monthMap.get(parts[1].toLowerCase());

        int year;
        if (parts.length == 3) {
            year = Integer.parseInt(parts[2]);
        } else {
            year = LocalDate.now().getYear();
        }
        return LocalDate.of(year, month, day);
    }

    private LocalDateTime parsePublishedAtArticle(String text) {
        String[] parts = text.split("\\s+");

        int day = Integer.parseInt(parts[0]);
        Integer month = monthMap.get(parts[1].toLowerCase());

        int year;
        String[] timeParts;

        if (parts.length == 3) {
            year = LocalDate.now().getYear();
            timeParts = parts[2].split(":");
        } else {
            year = Integer.parseInt(parts[2]);
            timeParts = parts[3].split(":");
        }
        return LocalDateTime.of(year, month, day, Integer.parseInt(timeParts[0]), Integer.parseInt(timeParts[1]));
    }

}
