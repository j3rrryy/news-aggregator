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
import java.util.Collection;

public class NewsArticleSpecs {

    public static Specification<NewsArticle> filterAll(
            String query,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            Category category,
            Source source,
            Status status,
            Collection<String> keywords
    ) {
        return Specification.where(fullText(query))
                .and(dateBetween(dateFrom, dateTo))
                .and(byCategory(category))
                .and(bySource(source))
                .and(byStatus(status))
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

    public static Specification<NewsArticle> byCategory(Category category) {
        return (root, criteriaQuery, criteriaBuilder) -> category == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get("category"), category);
    }

    public static Specification<NewsArticle> bySource(Source source) {
        return (root, criteriaQuery, criteriaBuilder) -> source == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get("source"), source);
    }

    public static Specification<NewsArticle> byStatus(Status status) {
        return (root, criteriaQuery, criteriaBuilder) -> status == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<NewsArticle> keywordsIn(Collection<String> keywords) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (keywords == null || keywords.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            Join<NewsArticle, String> join = root.join("keywords");
            return join.in(keywords);
        };
    }

}
