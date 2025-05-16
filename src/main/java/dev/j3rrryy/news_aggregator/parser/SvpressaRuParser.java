package dev.j3rrryy.news_aggregator.parser;

import com.google.common.util.concurrent.RateLimiter;
import dev.j3rrryy.news_aggregator.entity.NewsArticle;
import dev.j3rrryy.news_aggregator.enums.Category;
import dev.j3rrryy.news_aggregator.enums.Source;
import dev.j3rrryy.news_aggregator.enums.Status;
import dev.j3rrryy.news_aggregator.repository.NewsArticleRepository;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Map.entry;

@Slf4j
@Component
public class SvpressaRuParser extends NewsParser {

    private static final String URL_TEMPLATE = "https://svpressa.ru/%s/?page=%s";
    private static final Map<Category, Set<String>> urlMap = Map.of(
            Category.POLITICS, Set.of("politic"),
            Category.ECONOMICS, Set.of("economy"),
            Category.SOCIETY, Set.of("society"),
            Category.SPORT, Set.of("sport"),
            Category.SCIENCE_TECH, Set.of("science")
    );
    private static final Map<String, Integer> monthMap = Map.ofEntries(
            entry("января", 1), entry("февраля", 2), entry("марта", 3),
            entry("апреля", 4), entry("мая", 5), entry("июня", 6),
            entry("июля", 7), entry("августа", 8), entry("сентября", 9),
            entry("октября", 10), entry("ноября", 11), entry("декабря", 12)
    );

    @Autowired
    public SvpressaRuParser(ExecutorService ioExecutor, ExecutorService cpuExecutor, NewsArticleRepository newsArticleRepository) {
        super(ioExecutor, RateLimiter.create(10), cpuExecutor, newsArticleRepository);
    }

    @Override
    public void parse(Map<Category, LocalDateTime> latestPublishedAtByCategory) {
        log.info("Parsing news from svpressa.ru...");
        for (Map.Entry<Category, Set<String>> entry : urlMap.entrySet()) {
            Category category = entry.getKey();
            LocalDateTime latestPublishedAt = latestPublishedAtByCategory.get(category);

            for (String path : entry.getValue()) {
                Set<String> articleUrls = new HashSet<>();

                int fetchedPages = 0;
                boolean endReached = false;

                while (!endReached) {
                    int startPage = fetchedPages + 1;
                    int endPage = fetchedPages + 10;

                    Set<CompletableFuture<Optional<Document>>> pageFutures = IntStream
                            .rangeClosed(startPage, endPage)
                            .mapToObj(page -> CompletableFuture.supplyAsync(
                                    () -> {
                                        String url = URL_TEMPLATE.formatted(path, page);
                                        return downloadPage(fetchGet(url), url);
                                    },
                                    ioExecutor
                            ))
                            .collect(Collectors.toSet());

                    CompletableFuture.allOf(pageFutures.toArray(CompletableFuture[]::new)).join();

                    List<Document> pages = pageFutures.stream()
                            .map(CompletableFuture::join)
                            .flatMap(Optional::stream)
                            .toList();

                    if (pages.isEmpty()) break;

                    for (Document doc : pages) {
                        Set<String> urls = getPageUrls(doc, latestPublishedAt);
                        if (urls.isEmpty()) {
                            endReached = true;
                            break;
                        }
                        articleUrls.addAll(urls);
                    }

                    fetchedPages += pages.size();
                }

                int saved = fetchAndSaveArticles(articleUrls, category, latestPublishedAt);
                log.info("Saved {} new articles from {}", saved, category);
            }
        }
        log.info("Parsing from svpressa.ru completed");
    }

    @Override
    protected Optional<NewsArticle> parseNewsArticle(Document doc, Category category) {
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

    private Set<String> getPageUrls(Document doc, LocalDateTime latestPublishedAt) {
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
