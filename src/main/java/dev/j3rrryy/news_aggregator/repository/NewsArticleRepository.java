package dev.j3rrryy.news_aggregator.repository;

import dev.j3rrryy.news_aggregator.entity.NewsArticle;
import dev.j3rrryy.news_aggregator.enums.Status;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticle, UUID>, JpaSpecificationExecutor<NewsArticle> {

    @Modifying
    @Query("UPDATE NewsArticle SET status = 'ACTIVE' WHERE status = 'NEW'")
    void updateAllNewToActive();

    @Query("""
            SELECT source, category, MAX(publishedAt)
            FROM NewsArticle
            WHERE status != 'DELETED'
            GROUP BY source, category
            """)
    List<Object[]> findLatestPublishedAtByCategoryAndSource();

    int countByStatus(Status status);

    @Modifying
    @Query("UPDATE NewsArticle SET status = 'DELETED' WHERE publishedAt < :olderThan AND status != 'DELETED'")
    int markAsDeletedByPublishedAtBefore(@Param("olderThan") LocalDateTime olderThan);

    @Modifying
    @Query("DELETE FROM NewsArticle WHERE status = 'DELETED'")
    int deleteAllMarkedAsDeleted();

    @Modifying
    @Query("DELETE FROM NewsArticle")
    int deleteAllArticles();

    @Query("SELECT category, COUNT(*) FROM NewsArticle WHERE status != 'DELETED' GROUP BY category")
    List<Object[]> countArticlesByCategory();

    @Query("""
                SELECT k, COUNT(k) frequency
                FROM NewsArticle n
                JOIN n.keywords k
                WHERE n.status != 'DELETED'
                GROUP BY k
                ORDER BY frequency DESC
            """)
    List<Object[]> findMostFrequentKeywords(Pageable pageable);

    @Query("""
                SELECT FUNCTION('date_trunc', 'day', n.publishedAt) AS day, COUNT(n)
                FROM NewsArticle n
                JOIN n.keywords k
                WHERE LOWER(k) LIKE CONCAT('%', LOWER(:keyword), '%') AND n.status != 'DELETED'
                GROUP BY day
                ORDER BY day DESC
            """)
    List<Object[]> findKeywordFrequencyOverTime(@Param("keyword") String keyword);

    @Query(value = """
                WITH
                  curr AS (
                    SELECT k.keyword, COUNT(*) AS curr_count
                    FROM news_articles n
                    JOIN news_keywords k ON n.id = k.article_id
                    WHERE n.published_at BETWEEN :fromDate AND :toDate AND n.status <> 'DELETED'
                    GROUP BY k.keyword
                  ),
            
                  prev AS (
                    SELECT k.keyword, COUNT(*) AS prev_count
                    FROM news_articles n
                    JOIN news_keywords k ON n.id = k.article_id
                    WHERE n.published_at BETWEEN :prev_start AND :fromDate AND n.status <> 'DELETED'
                    GROUP BY k.keyword
                  )
            
                SELECT
                  c.keyword,
                  c.curr_count,
                  COALESCE(p.prev_count, 0) AS prev_count,
                  (c.curr_count - COALESCE(p.prev_count, 0)) AS delta
                FROM curr c
                LEFT JOIN prev p ON p.keyword = c.keyword
                ORDER BY delta DESC
                LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopKeywordsInRange(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("prev_start") LocalDateTime prevStart,
            int limit
    );

}
