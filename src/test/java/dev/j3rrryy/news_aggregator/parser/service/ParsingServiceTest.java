package dev.j3rrryy.news_aggregator.parser.service;

import dev.j3rrryy.news_aggregator.entity.NewsArticle;
import dev.j3rrryy.news_aggregator.enums.Category;
import dev.j3rrryy.news_aggregator.enums.Source;
import dev.j3rrryy.news_aggregator.repository.NewsArticleBatchRepository;
import dev.j3rrryy.news_aggregator.repository.NewsArticleRepository;
import dev.j3rrryy.news_aggregator.service.v1.CacheManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

public class ParsingServiceTest {

    private ParsingService parsingService;
    private NewsArticleRepository repository;
    private CacheManagerService cacheManagerService;
    private NewsArticleBatchRepository batchRepository;

    @BeforeEach
    void setUp() {
        cacheManagerService = mock(CacheManagerService.class);
        repository = mock(NewsArticleRepository.class);
        batchRepository = mock(NewsArticleBatchRepository.class);

        parsingService = new ParsingService(cacheManagerService, repository, batchRepository);
    }

    @Test
    void prepareForParsing_shouldClearCachesAndUpdateStatuses() {
        parsingService.prepareForParsing();

        verify(cacheManagerService).clearAllCaches();
        verify(repository).updateAllNewToActive();
    }

    @Test
    void getLatestPublishedAtByCategoryAndSource_shouldReturnCorrectMap() {
        Object[] row1 = new Object[]{Source.AIF_RU, Category.SOCIETY,
                LocalDateTime.of(2025, 5, 1, 0, 0)};
        Object[] row2 = new Object[]{Source.RT_RU, Category.POLITICS,
                LocalDateTime.of(2025, 5, 7, 0, 0)};
        when(repository.findLatestPublishedAtByCategoryAndSource()).thenReturn(List.of(row1, row2));

        Map<Source, Map<Category, LocalDateTime>> result = parsingService.getLatestPublishedAtByCategoryAndSource();

        assertEquals(LocalDateTime.of(2025, 5, 1, 0, 0),
                result.get(Source.AIF_RU).get(Category.SOCIETY));
        assertEquals(LocalDateTime.of(2025, 5, 7, 0, 0),
                result.get(Source.RT_RU).get(Category.POLITICS));

        for (Source source : Source.values()) {
            for (Category category : Category.values()) {
                if ((source != Source.AIF_RU || category != Category.SOCIETY) &&
                        (source != Source.RT_RU || category != Category.POLITICS)) {
                    assertNull(result.get(source).get(category));
                }
            }
        }
    }

    @Test
    void saveArticles_shouldDelegateToRepositoryAndReturnCount() {
        List<NewsArticle> articles = List.of(new NewsArticle(), new NewsArticle());
        when(batchRepository.saveAllBatch(articles)).thenReturn(2);

        int savedCount = parsingService.saveArticles(articles);

        assertEquals(2, savedCount);
        verify(batchRepository).saveAllBatch(articles);
    }

}
