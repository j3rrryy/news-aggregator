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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
public class AifRuParser extends NewsParser {

    private static final String URL_TEMPLATE = "https://aif.ru/%s";
    private static final String BODY_TEMPLATE = "page=%s";
    private static final String SUMMARY_AND_CONTENT_SELECTOR = """
            div.article_text > p, div.article_text > h1, div.article_text > h2, div.article_text > h3,
            div.article_text > h4, div.article_text > h5, div.article_text > h6
            """;
    private static final Map<Category, Set<String>> urlMap = Map.of(
            Category.POLITICS, Set.of("politics/russia", "politics/world"),
            Category.ECONOMICS, Set.of("money/economy", "money/business", "money/market"),
            Category.SOCIETY, Set.of("society/people"),
            Category.SPORT, Set.of(
                    "sport/football",
                    "sport/hockey",
                    "sport/winter",
                    "sport/summer",
                    "sport/other",
                    "sport/olymp",
                    "sport/structure",
                    "sport/person"
            ),
            Category.SCIENCE_TECH, Set.of("techno/industry", "techno/technology", "society/science")
    );
    private static final DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
            .appendOptional(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
            .appendOptional(DateTimeFormatter.ofPattern("HH:mm"))
            .toFormatter();

    @Autowired
    public AifRuParser(ExecutorService ioExecutor, ExecutorService cpuExecutor, NewsArticleRepository newsArticleRepository) {
        super(ioExecutor, RateLimiter.create(30), cpuExecutor, newsArticleRepository);
    }

    @Override
    public void parse(Map<Category, LocalDateTime> latestPublishedAtByCategory) {
        log.info("Parsing news from aif.ru...");
        for (Map.Entry<Category, Set<String>> entry : urlMap.entrySet()) {
            Category category = entry.getKey();
            LocalDateTime latestPublishedAt = latestPublishedAtByCategory.get(category);

            for (String path : entry.getValue()) {
                String baseUrl = URL_TEMPLATE.formatted(path);
                Set<String> articleUrls = new HashSet<>();

                int fetchedPages = 0;
                Integer firstPageHash = null;
                boolean endReached = false;

                while (!endReached) {
                    int startPage = fetchedPages + 1;
                    int endPage = fetchedPages + 10;

                    Set<CompletableFuture<Optional<Document>>> pageFutures = IntStream
                            .rangeClosed(startPage, endPage)
                            .mapToObj(page -> CompletableFuture.supplyAsync(
                                    () -> downloadPage(fetchPost(baseUrl, BODY_TEMPLATE.formatted(page)), baseUrl),
                                    ioExecutor
                            ))
                            .collect(Collectors.toSet());

                    CompletableFuture.allOf(pageFutures.toArray(CompletableFuture[]::new)).join();

                    List<Document> pages = pageFutures.stream()
                            .map(CompletableFuture::join)
                            .flatMap(Optional::stream)
                            .toList();

                    if (pages.isEmpty()) break;

                    for (int i = 0; i < pages.size(); i++) {
                        int pageNum = startPage + i;
                        Document doc = pages.get(i);
                        int hash = doc.text().hashCode();

                        if (pageNum == 1) {
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

                    fetchedPages += pages.size();
                }

                int saved = fetchAndSaveArticles(articleUrls, category, latestPublishedAt);
                log.info("Saved {} new articles from {}", saved, category);
            }
        }
        log.info("Parsing from aif.ru completed");
    }

    @Override
    protected Optional<NewsArticle> parseNewsArticle(Document doc, Category category) {
        try {
            String title = Objects.requireNonNull(doc.selectFirst("h1[itemprop=headline]"))
                    .text();
            String summary = Objects.requireNonNull(doc.selectFirst(SUMMARY_AND_CONTENT_SELECTOR))
                    .text()
                    .replaceFirst("^(.*?[.!?]).*", "$1")
                    .trim();
            String content = doc.select(SUMMARY_AND_CONTENT_SELECTOR).stream()
                    .map(Element::text)
                    .collect(Collectors.joining("\n\n"));
            Set<String> keywords = doc.select("span[itemprop=keywords]").stream()
                    .map(Element::text)
                    .collect(Collectors.toSet());
            Set<String> mediaUrls = doc.select("img[itemprop=image]").stream()
                    .map(media -> media.absUrl("src"))
                    .collect(Collectors.toSet());
            String url = doc.location();

            String publishedAtText = Objects.requireNonNull(doc.selectFirst("time[itemprop=datePublished]"))
                    .text();
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

    private Set<String> getPageUrls(Document doc, LocalDateTime latestPublishedAt) {
        Elements newsArticles = doc.select("div.list_item");
        Set<String> urls = new HashSet<>();

        for (Element article : newsArticles) {
            try {
                String url = Objects.requireNonNull(article.selectFirst("div.box_info a"))
                        .absUrl("href");
                String publishedAtText = Objects.requireNonNull(article.selectFirst("span.text_box__date"))
                        .text();
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
