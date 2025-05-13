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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RtRuParser extends NewsParser {

    private static final String URL_TEMPLATE = """
            https://russian.rt.com/listing/type.ArticleVideoGallery.trend.%s/prepare/all-trends-new/5000/0
            """;
    private static final String CONTENT_SELECTOR = """
            div.article__text > p, div.article__text > h1, div.article__text > h2, div.article__text > h3,
            div.article__text > h4, div.article__text > h5, div.article__text > h6, div.article__text > blockquote
            """;
    private static final Map<Category, Set<String>> urlMap = Map.of(
            Category.POLITICS, Set.of("5835d35ec46188a6798b493b", "58357206c46188dc658b45ad"),
            Category.ECONOMICS, Set.of(
                    "583573b2c46188a1658b45f7",
                    "58357264c461889e658b458e",
                    "58357896c461889f658b46a1",
                    "58358324c46188a2658b47c0",
                    "58358088c46188a1658b4704"
            ),
            Category.SOCIETY, Set.of("583585fac461889d658b484c"),
            Category.SPORT, Set.of(
                    "58356b4bc36188f34f8b48b6",
                    "58356befc46188991b8b47b1",
                    "58356e86c461884d4d8b4599",
                    "58357fd4c46188dc658b46fa",
                    "583571dcc461889e658b458a",
                    "57e94138c46188b8458b480c",
                    "58358911c4618866648b4589",
                    "58380405c46188046c8b46c7"
            ),
            Category.SCIENCE_TECH, Set.of(
                    "5d83432e02e8bd4e656e7f47",
                    "5835934dc4618894648b491a",
                    "58359464c461888a648b4835",
                    "584be210c36188d60d8b45b4",
                    "58359228c46188866a8b487f",
                    "5d834a6202e8bd51154aa624",
                    "5835a55bc4618845518b4785",
                    "5849bf37c361881a378b459b",
                    "58567a75c461888f758b45fe",
                    "5d8343dbae5ac977e066f422",
                    "58359402c4618893648b4a52"
            )
    );
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-d HH:mm");

    @Autowired
    public RtRuParser(ExecutorService ioExecutor, ExecutorService cpuExecutor, NewsArticleRepository newsArticleRepository) {
        super(ioExecutor, RateLimiter.create(30), cpuExecutor, newsArticleRepository);
    }

    @Override
    public void parse(Map<Category, LocalDateTime> latestPublishedAtByCategory) {
        log.info("Parsing news from rt.ru...");
        for (Map.Entry<Category, Set<String>> entry : urlMap.entrySet()) {
            Category category = entry.getKey();
            LocalDateTime latestPublishedAt = latestPublishedAtByCategory.get(category);

            Set<String> articleUrls = entry.getValue().stream()
                    .map(URL_TEMPLATE::formatted)
                    .map(pageUrl -> CompletableFuture
                            .supplyAsync(() -> downloadPage(fetchGet(pageUrl), pageUrl), ioExecutor))
                    .map(optPage -> optPage.thenApply(opt -> opt
                            .map(page -> getPageUrls(page, latestPublishedAt))
                            .orElseGet(Set::of)))
                    .flatMap(futSet -> futSet.thenApply(Set::stream).join())
                    .collect(Collectors.toSet());

            int saved = fetchAndSaveArticles(articleUrls, category, latestPublishedAt);
            log.info("Saved {} new articles from {}", saved, category.name());
        }
        log.info("Parsing from rt.ru completed");
    }

    @Override
    protected Optional<NewsArticle> parseNewsArticle(Document doc, Category category) {
        try {
            String title = Objects.requireNonNull(doc.selectFirst("h1.article__heading"))
                    .text();
            String summary = Objects.requireNonNull(doc.selectFirst("div.article__summary"))
                    .text()
                    .replaceFirst("^(.*?[.!?]).*", "$1")
                    .trim();
            String content = Objects.requireNonNull(doc.selectFirst("div.article__summary")).text()
                    + "\n\n" + doc.select(CONTENT_SELECTOR).text();
            Set<String> keywords = doc.select("a.tags-trends__link").stream()
                    .map(Element::text)
                    .collect(Collectors.toSet());
            Set<String> mediaUrls = doc.select("img.article__cover-image").stream()
                    .map(media -> media.absUrl("src"))
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
                    .source(Source.RT_RU)
                    .build();
            return Optional.of(newsArticle);
        } catch (Exception e) {
            log.debug("Skipping invalid article {}", doc.location());
            return Optional.empty();
        }
    }

    private Set<String> getPageUrls(Document doc, LocalDateTime latestPublishedAt) {
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

}
