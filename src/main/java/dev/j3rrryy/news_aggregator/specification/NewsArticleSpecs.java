package dev.j3rrryy.news_aggregator.specification;

import dev.j3rrryy.news_aggregator.entity.NewsArticle;
import dev.j3rrryy.news_aggregator.enums.Category;
import dev.j3rrryy.news_aggregator.enums.Source;
import dev.j3rrryy.news_aggregator.enums.Status;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public class NewsArticleSpecs {

    public static Specification<NewsArticle> filterAll(
            String query,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Set<Category> categories,
            Set<Source> sources,
            Set<Status> statuses,
            Set<String> keywords,
            LocalDateTime cursorPublishedAt,
            UUID cursorId
    ) {
        return Specification.where(fullText(query))
                .and(dateBetween(fromDate, toDate))
                .and(byCategories(categories))
                .and(bySources(sources))
                .and(byStatuses(statuses))
                .and(keywordsIn(keywords))
                .and(byCursor(cursorPublishedAt, cursorId));
    }

    private static Specification<NewsArticle> fullText(String query) {
        return (root, cq, cb) -> {
            if (query == null || query.isBlank()) {
                return cb.conjunction();
            }
            Expression<String> vector = cb.function(
                    "to_tsvector", String.class,
                    cb.literal("russian"),
                    cb.concat(cb.concat(root.get("title"), cb.literal(" ")),
                            root.get("content"))
            );
            Expression<String> tsQuery = cb.function(
                    "plainto_tsquery", String.class,
                    cb.literal("russian"),
                    cb.literal(query)
            );
            Expression<Boolean> fts = cb.function("fts", Boolean.class, vector, tsQuery);
            return cb.isTrue(fts);
        };
    }

    private static Specification<NewsArticle> dateBetween(LocalDateTime from, LocalDateTime to) {
        return (root, cq, cb) -> {
            Predicate predicate = cb.conjunction();
            if (from != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("publishedAt"), from));
            }
            if (to != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("publishedAt"), to));
            }
            return predicate;
        };
    }

    private static Specification<NewsArticle> byCategories(Set<Category> categories) {
        return (root, cq, cb) -> {
            if (categories == null || categories.isEmpty()) {
                return cb.conjunction();
            }
            return root.get("category").in(categories);
        };
    }

    private static Specification<NewsArticle> bySources(Set<Source> sources) {
        return (root, cq, cb) -> {
            if (sources == null || sources.isEmpty()) {
                return cb.conjunction();
            }
            return root.get("source").in(sources);
        };
    }

    private static Specification<NewsArticle> byStatuses(Set<Status> statuses) {
        return (root, cq, cb) -> {
            if (statuses == null || statuses.isEmpty()) {
                return cb.conjunction();
            }
            return root.get("status").in(statuses);
        };
    }

    private static Specification<NewsArticle> keywordsIn(Set<String> keywords) {
        return (root, cq, cb) -> {
            if (keywords == null || keywords.isEmpty()) {
                return cb.conjunction();
            }
            Join<NewsArticle, String> join = root.join("keywords");
            return join.in(keywords);
        };
    }

    private static Specification<NewsArticle> byCursor(LocalDateTime publishedAt, UUID lastId) {
        return (root, cq, cb) -> {
            if (publishedAt == null || lastId == null) {
                return cb.conjunction();
            }
            Predicate byDate = cb.lessThan(root.get("publishedAt"), publishedAt);
            Predicate sameDateEarlierId = cb.and(
                    cb.equal(root.get("publishedAt"), publishedAt),
                    cb.lessThan(root.get("id"), lastId)
            );
            return cb.or(byDate, sameDateEarlierId);
        };
    }

}
