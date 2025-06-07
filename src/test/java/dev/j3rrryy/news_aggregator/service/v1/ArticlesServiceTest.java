package dev.j3rrryy.news_aggregator.service.v1;

import dev.j3rrryy.news_aggregator.dto.request.MarkDeleted;
import dev.j3rrryy.news_aggregator.dto.response.ArticlesAffected;
import dev.j3rrryy.news_aggregator.dto.response.ArticlesSummary;
import dev.j3rrryy.news_aggregator.enums.Status;
import dev.j3rrryy.news_aggregator.repository.NewsArticleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ArticlesServiceTest {

    @InjectMocks
    private ArticlesService articlesService;

    @Mock
    private NewsArticleRepository repository;

    @Mock
    private CacheManagerService cacheManagerService;

    @Test
    void getArticlesSummary_shouldReturnCorrectCounts() {
        when(repository.countByStatus(Status.NEW)).thenReturn(5);
        when(repository.countByStatus(Status.ACTIVE)).thenReturn(10);
        when(repository.countByStatus(Status.DELETED)).thenReturn(3);

        ArticlesSummary summary = articlesService.getArticlesSummary();

        assertThat(summary.newArticlesCount()).isEqualTo(5);
        assertThat(summary.activeArticlesCount()).isEqualTo(10);
        assertThat(summary.deletedArticlesCount()).isEqualTo(3);
        assertThat(summary.totalArticles()).isEqualTo(18);
    }

    @Test
    void markAsDeleted_shouldClearCacheAndReturnAffectedCount() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        MarkDeleted dto = new MarkDeleted(threshold);
        when(repository.markAsDeletedByPublishedAtBefore(threshold)).thenReturn(7);

        ArticlesAffected result = articlesService.markAsDeleted(dto);

        verify(cacheManagerService).clearAllCaches();
        assertThat(result.articlesAffected()).isEqualTo(7);
    }

    @Test
    void deleteMarkedArticles_shouldClearCacheAndReturnAffectedCount() {
        when(repository.deleteAllMarkedAsDeleted()).thenReturn(4);

        ArticlesAffected result = articlesService.deleteMarkedArticles();

        verify(cacheManagerService).clearAllCaches();
        assertThat(result.articlesAffected()).isEqualTo(4);
    }

    @Test
    void deleteAllArticles_shouldClearCacheAndReturnAffectedCount() {
        when(repository.deleteAllArticles()).thenReturn(12);

        ArticlesAffected result = articlesService.deleteAllArticles();

        verify(cacheManagerService).clearAllCaches();
        assertThat(result.articlesAffected()).isEqualTo(12);
    }

}
