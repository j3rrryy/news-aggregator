package dev.j3rrryy.news_aggregator.repository;

import dev.j3rrryy.news_aggregator.entity.NewsArticle;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticle, UUID> {

    @Modifying
    @Transactional
    @Query("UPDATE NewsArticle SET status = 'ACTIVE' WHERE status = 'NEW'")
    void updateAllNewToActive();

    @Query("""
            SELECT source, category, MAX(publishedAt) FROM NewsArticle
            WHERE status != 'DELETED' GROUP BY source, category
            """)
    List<Object[]> findLatestPublishedAtByCategoryAndSource();

    @Query("SELECT url FROM NewsArticle WHERE status != 'DELETED' AND url IN :urls")
    Set<String> findExistingUrls(@Param("urls") Set<String> urls);

    @Modifying
    @Transactional
    @Query("UPDATE NewsArticle SET status = 'DELETED' WHERE publishedAt < :olderThan AND status <> 'DELETED'")
    int markAsDeletedByPublishedAtBefore(@Param("olderThan") LocalDateTime olderThan);

    @Modifying
    @Transactional
    @Query("DELETE FROM NewsArticle WHERE status = 'DELETED'")
    int deleteAllMarkedAsDeleted();

    @Modifying
    @Transactional
    @Query("DELETE FROM NewsArticle")
    int deleteAllArticles();

}
