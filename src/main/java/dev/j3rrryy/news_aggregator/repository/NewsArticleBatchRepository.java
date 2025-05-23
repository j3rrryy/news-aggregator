package dev.j3rrryy.news_aggregator.repository;

import dev.j3rrryy.news_aggregator.entity.NewsArticle;
import lombok.RequiredArgsConstructor;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Repository
@RequiredArgsConstructor
public class NewsArticleBatchRepository {

    private final JdbcTemplate jdbcTemplate;

    public int saveAllBatch(List<NewsArticle> articles) {
        articles.forEach(article -> {
            if (article.getId() == null) article.setId(UUID.randomUUID());
        });

        int[] inserts = insertArticlesIfNotExist(articles);
        List<NewsArticle> insertedArticles = IntStream.range(0, inserts.length)
                .filter(i -> inserts[i] > 0)
                .mapToObj(articles::get)
                .toList();

        insertKeywordsIfNotExist(insertedArticles);
        insertMediaUrlsIfNotExist(insertedArticles);
        return insertedArticles.size();
    }

    private int[] insertArticlesIfNotExist(List<NewsArticle> articles) {
        if (articles.isEmpty()) return new int[0];

        String sql = """
                INSERT INTO news_articles
                  (id, title, summary, content, category, url, status, published_at, source)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (url) DO NOTHING
                """;

        return jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(@NonNull PreparedStatement preparedStatement, int i) throws SQLException {
                NewsArticle article = articles.get(i);
                preparedStatement.setObject(1, article.getId(), Types.OTHER);
                preparedStatement.setString(2, article.getTitle());
                preparedStatement.setString(3, article.getSummary());
                preparedStatement.setString(4, article.getContent());

                PGobject categoryObj = new PGobject();
                categoryObj.setType("category");
                categoryObj.setValue(article.getCategory().name());
                preparedStatement.setObject(5, categoryObj);

                preparedStatement.setString(6, article.getUrl());

                PGobject statusObj = new PGobject();
                statusObj.setType("status");
                statusObj.setValue(article.getStatus().name());
                preparedStatement.setObject(7, statusObj);

                preparedStatement.setTimestamp(8, Timestamp.valueOf(article.getPublishedAt()));

                PGobject sourceObj = new PGobject();
                sourceObj.setType("source");
                sourceObj.setValue(article.getSource().name());
                preparedStatement.setObject(9, sourceObj);
            }

            @Override
            public int getBatchSize() {
                return articles.size();
            }
        });
    }

    private void insertKeywordsIfNotExist(List<NewsArticle> articles) {
        List<Object[]> batch = articles.stream()
                .flatMap(article -> article.getKeywords().stream()
                        .map(kw -> new Object[]{article.getId(), kw})
                )
                .toList();
        if (batch.isEmpty()) return;

        String sql = """
                INSERT INTO news_keywords (article_id, keyword)
                VALUES (?, ?)
                ON CONFLICT (article_id, keyword) DO NOTHING
                """;
        jdbcTemplate.batchUpdate(sql, batch);
    }

    private void insertMediaUrlsIfNotExist(List<NewsArticle> articles) {
        List<Object[]> batch = articles.stream()
                .flatMap(article -> article.getMediaUrls().stream()
                        .map(url -> new Object[]{article.getId(), url})
                )
                .toList();
        if (batch.isEmpty()) return;

        String sql = """
                INSERT INTO news_media_urls (article_id, media_url)
                VALUES (?, ?)
                ON CONFLICT (article_id, media_url) DO NOTHING
                """;
        jdbcTemplate.batchUpdate(sql, batch);
    }

}
