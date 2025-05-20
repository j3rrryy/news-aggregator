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

public class NewsArticleSpecs {

    public static Specification<NewsArticle> filterAll(
            String query,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            Set<Category> categories,
            Set<Source> sources,
            Set<Status> statuses,
            Set<String> keywords
    ) {
        return Specification.where(fullText(query))
                .and(dateBetween(dateFrom, dateTo))
                .and(byCategories(categories))
                .and(bySources(sources))
                .and(byStatuses(statuses))
                .and(keywordsIn(keywords));
    }

    public static Specification<NewsArticle> fullText(String query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (query == null || query.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            Expression<String> vector = criteriaBuilder.function(
                    "to_tsvector", String.class,
                    criteriaBuilder.literal("russian"),
                    criteriaBuilder.concat(
                            criteriaBuilder.concat(root.get("title"), criteriaBuilder.literal(" ")),
                            root.get("content")
                    )
            );
            Expression<String> tsQuery = criteriaBuilder.function(
                    "plainto_tsquery", String.class,
                    criteriaBuilder.literal("russian"),
                    criteriaBuilder.literal(query)
            );
            Expression<Boolean> fts = criteriaBuilder.function("fts", Boolean.class, vector, tsQuery);
            return criteriaBuilder.isTrue(fts);
        };
    }

    public static Specification<NewsArticle> dateBetween(LocalDateTime from, LocalDateTime to) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();
            if (from != null) {
                predicate = criteriaBuilder.and(
                        predicate, criteriaBuilder.greaterThanOrEqualTo(root.get("publishedAt"), from)
                );
            }
            if (to != null) {
                predicate = criteriaBuilder.and(
                        predicate, criteriaBuilder.lessThanOrEqualTo(root.get("publishedAt"), to)
                );
            }
            return predicate;
        };
    }

    public static Specification<NewsArticle> byCategories(Set<Category> categories) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (categories == null || categories.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("category").in(categories);
        };
    }

    public static Specification<NewsArticle> bySources(Set<Source> sources) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (sources == null || sources.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("source").in(sources);
        };
    }

    public static Specification<NewsArticle> byStatuses(Set<Status> statuses) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (statuses == null || statuses.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("status").in(statuses);
        };
    }

    public static Specification<NewsArticle> keywordsIn(Set<String> keywords) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (keywords == null || keywords.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            Join<NewsArticle, String> join = root.join("keywords");
            return join.in(keywords);
        };
    }

}
