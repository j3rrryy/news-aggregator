package dev.j3rrryy.news_aggregator.service.v1;

import dev.j3rrryy.news_aggregator.dto.response.*;
import dev.j3rrryy.news_aggregator.enums.Category;
import dev.j3rrryy.news_aggregator.exceptions.FromDateAfterToDateException;
import dev.j3rrryy.news_aggregator.repository.NewsArticleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AnalyticsServiceTest {

    @Mock
    private NewsArticleRepository repository;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    void getCategoryCounts_withSomeCategories_returnsCorrectCountsAndDefaults() {
        List<Object[]> rows = List.of(
                new Object[]{Category.POLITICS, 5},
                new Object[]{Category.SCIENCE_TECH, 12}
        );
        when(repository.countArticlesByCategory()).thenReturn(rows);

        CategoryCounts counts = analyticsService.getCategoryCounts();

        assertThat(counts.politics()).isEqualTo(5);
        assertThat(counts.economics()).isEqualTo(0);
        assertThat(counts.society()).isEqualTo(0);
        assertThat(counts.sport()).isEqualTo(0);
        assertThat(counts.scienceTech()).isEqualTo(12);
    }

    @Test
    void getTopFrequentKeywords_mapsRepositoryRowsToDto() {
        Pageable pageable = PageRequest.of(0, 3);
        when(repository.findMostFrequentKeywords(pageable)).thenReturn(List.of(
                new Object[]{"java", 7},
                new Object[]{"spring", 3}
        ));

        List<KeywordFrequency> frequencies = analyticsService.getTopFrequentKeywords(3);

        assertThat(frequencies).containsExactly(
                new KeywordFrequency("java", 7),
                new KeywordFrequency("spring", 3)
        );
    }

    @Test
    void getKeywordTrend_mapsDateTimeToDateAndCounts() {
        String kw = "java";
        LocalDateTime dt1 = LocalDateTime.of(2025, 5, 1, 0, 0);
        LocalDateTime dt2 = LocalDateTime.of(2025, 5, 7, 0, 0);
        when(repository.findKeywordFrequencyOverTime(kw)).thenReturn(List.of(
                new Object[]{dt1, 4},
                new Object[]{dt2, 2}
        ));

        List<KeywordDateCount> trend = analyticsService.getKeywordTrend(kw);

        assertThat(trend).containsExactly(
                new KeywordDateCount(LocalDate.of(2025, 5, 1), 4),
                new KeywordDateCount(LocalDate.of(2025, 5, 7), 2)
        );
    }

    @Test
    void getTrendingTopics_fromAfterTo_throws() {
        LocalDateTime from = LocalDateTime.of(2025, 5, 7, 0, 0);
        LocalDateTime to = LocalDateTime.of(2025, 5, 1, 0, 0);

        assertThatThrownBy(() ->
                analyticsService.getTrendingTopics(from, to, 5)
        ).isInstanceOf(FromDateAfterToDateException.class);
    }

    @Test
    void getTrendingTopics_validRange_mapsRepositoryRowsToDto() {
        LocalDateTime from = LocalDateTime.of(2025, 5, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2025, 5, 7, 0, 0);
        Duration duration = Duration.between(from, to);
        LocalDateTime prevStart = from.minus(duration);

        when(repository.findTopKeywordsInRange(from, to, prevStart, 2)).thenReturn(List.of(
                new Object[]{"spring", 10, 6, 4},
                new Object[]{"java", 5, 2, 3}
        ));

        List<TrendingTopic> topics = analyticsService.getTrendingTopics(from, to, 2);

        assertThat(topics).containsExactly(
                new TrendingTopic("spring", 10, 6, 4),
                new TrendingTopic("java", 5, 2, 3)
        );
    }

}
