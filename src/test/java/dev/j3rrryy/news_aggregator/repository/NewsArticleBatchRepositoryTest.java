package dev.j3rrryy.news_aggregator.repository;

import dev.j3rrryy.news_aggregator.entity.NewsArticle;
import dev.j3rrryy.news_aggregator.enums.Category;
import dev.j3rrryy.news_aggregator.enums.Source;
import dev.j3rrryy.news_aggregator.enums.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NewsArticleBatchRepositoryTest {

    @Mock
    JdbcTemplate jdbcTemplate;

    @InjectMocks
    NewsArticleBatchRepository repository;

    @Test
    void saveAllBatch_shouldReturnZero_whenArticlesEmpty() {
        int result = repository.saveAllBatch(List.of());
        assertEquals(0, result);
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void saveAllBatch_shouldAssignUUID_whenArticleIdIsNull() {
        NewsArticle article = dummyArticle(null, Set.of("java"), Set.of("test media url"));

        when(jdbcTemplate.batchUpdate(anyString(), any(BatchPreparedStatementSetter.class)))
                .thenReturn(new int[]{1});
        when(jdbcTemplate.batchUpdate(contains("news_keywords"), anyList()))
                .thenReturn(new int[]{1});
        when(jdbcTemplate.batchUpdate(contains("news_media_urls"), anyList()))
                .thenReturn(new int[]{1});

        repository.saveAllBatch(List.of(article));
        assertNotNull(article.getId());
    }

    @Test
    void saveAllBatch_shouldSkipKeywordsAndMediaUrls_whenInsertConflict() {
        NewsArticle article = dummyArticle(UUID.randomUUID(), Set.of("spring"), Set.of("test media url"));

        when(jdbcTemplate.batchUpdate(anyString(), any(BatchPreparedStatementSetter.class)))
                .thenReturn(new int[]{0});

        int result = repository.saveAllBatch(List.of(article));

        assertEquals(0, result);
        verify(jdbcTemplate, never()).batchUpdate(contains("news_keywords"), anyList());
        verify(jdbcTemplate, never()).batchUpdate(contains("news_media_urls"), anyList());
    }

    @Test
    void saveAllBatch_shouldFilterInsertedArticlesCorrectly() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        NewsArticle article1 = dummyArticle(id1, Set.of("java"), Set.of("test media url 1"));
        NewsArticle article2 = dummyArticle(id2, Set.of("spring"), Set.of("test media url 2"));

        when(jdbcTemplate.batchUpdate(anyString(), any(BatchPreparedStatementSetter.class)))
                .thenReturn(new int[]{0, 1});
        when(jdbcTemplate.batchUpdate(contains("news_keywords"), anyList()))
                .thenReturn(new int[]{1});
        when(jdbcTemplate.batchUpdate(contains("news_media_urls"), anyList()))
                .thenReturn(new int[]{1});

        int result = repository.saveAllBatch(List.of(article1, article2));

        assertEquals(1, result);
        verify(jdbcTemplate).batchUpdate(contains("news_keywords"),
                argThat((List<Object[]> list) -> list.stream().allMatch(arr -> id2.equals(arr[0])))
        );
    }

    @Test
    void insertKeywordsAndMedia_shouldNotCallJdbc_whenEmpty() {
        NewsArticle article = dummyArticle(UUID.randomUUID(), Set.of(), Set.of());
        when(jdbcTemplate.batchUpdate(anyString(), any(BatchPreparedStatementSetter.class)))
                .thenReturn(new int[]{1});

        int result = repository.saveAllBatch(List.of(article));

        assertEquals(1, result);
        verify(jdbcTemplate, never()).batchUpdate(contains("news_keywords"), anyList());
        verify(jdbcTemplate, never()).batchUpdate(contains("news_media_urls"), anyList());
    }

    @Test
    void insertArticlesIfNotExist_shouldSetPreparedStatementValues_andReturnCorrectBatchSize() {
        NewsArticle article = dummyArticle(UUID.randomUUID(), Set.of("java"), Set.of("test media url"));

        when(jdbcTemplate.batchUpdate(anyString(), any(BatchPreparedStatementSetter.class)))
                .thenAnswer(invocation -> {
                    BatchPreparedStatementSetter setter = invocation.getArgument(1);
                    PreparedStatement ps = mock(PreparedStatement.class);
                    setter.setValues(ps, 0);

                    verify(ps).setObject(eq(1), eq(article.getId()), eq(Types.OTHER));
                    verify(ps).setString(eq(2), eq(article.getTitle()));
                    verify(ps).setString(eq(3), eq(article.getSummary()));
                    verify(ps).setString(eq(4), eq(article.getContent()));
                    verify(ps).setObject(eq(5), any(PGobject.class));
                    verify(ps).setString(eq(6), eq(article.getUrl()));
                    verify(ps).setObject(eq(7), any(PGobject.class));
                    verify(ps).setTimestamp(eq(8), any());
                    verify(ps).setObject(eq(9), any(PGobject.class));

                    assertEquals(1, setter.getBatchSize());
                    return new int[]{1};
                });

        int result = repository.saveAllBatch(List.of(article));
        assertEquals(1, result);
    }

    private NewsArticle dummyArticle(UUID id, Set<String> keywords, Set<String> mediaUrls) {
        return NewsArticle.builder()
                .id(id)
                .title("test title")
                .summary("test summary")
                .content("test content")
                .category(Category.POLITICS)
                .keywords(keywords)
                .mediaUrls(mediaUrls)
                .url("test url " + id)
                .status(Status.NEW)
                .publishedAt(LocalDateTime.now())
                .source(Source.AIF_RU)
                .build();
    }

}
