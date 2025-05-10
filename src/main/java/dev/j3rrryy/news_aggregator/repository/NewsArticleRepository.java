package dev.j3rrryy.news_aggregator.repository;

import dev.j3rrryy.news_aggregator.entity.NewsArticle;
import dev.j3rrryy.news_aggregator.enums.Category;
import dev.j3rrryy.news_aggregator.enums.Source;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticle, UUID> {

    @Cacheable(value = "latestPublishedAt", key = "#category.name() + '_' + #source.name()")
    @Query("SELECT MAX(publishedAt) FROM NewsArticle WHERE category = :category AND source = :source")
    Optional<LocalDateTime> findLatestPublishedAtByCategorySource(@Param("category") Category category, @Param("source") Source source);

    @Query("SELECT url FROM NewsArticle WHERE url IN :urls")
    Set<String> findExistingUrls(@Param("urls") Set<String> urls);

}
