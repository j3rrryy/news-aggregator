package dev.j3rrryy.news_aggregator.service.v1;

import dev.j3rrryy.news_aggregator.dto.response.*;
import dev.j3rrryy.news_aggregator.enums.Category;
import dev.j3rrryy.news_aggregator.exceptions.StartAfterEndException;
import dev.j3rrryy.news_aggregator.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final NewsArticleRepository newsArticleRepository;

    public CategoryCountsDto getCategoryCounts() {
        List<Object[]> counts = newsArticleRepository.countArticlesByCategory();
        Map<Category, Integer> categoryCounts = new HashMap<>();

        for (Object[] row : counts) {
            Category category = (Category) row[0];
            int count = ((Number) row[1]).intValue();
            categoryCounts.put(category, count);
        }
        return new CategoryCountsDto(
                categoryCounts.getOrDefault(Category.POLITICS, 0),
                categoryCounts.getOrDefault(Category.ECONOMICS, 0),
                categoryCounts.getOrDefault(Category.SOCIETY, 0),
                categoryCounts.getOrDefault(Category.SPORT, 0),
                categoryCounts.getOrDefault(Category.SCIENCE_TECH, 0)
        );
    }

    public List<KeywordFrequencyDto> getTopFrequentKeywords(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return newsArticleRepository.findMostFrequentKeywords(pageable).stream()
                .map(row -> new KeywordFrequencyDto((String) row[0], ((Number) row[1]).intValue()))
                .toList();
    }

    public List<KeywordDateCountDto> getKeywordTrend(String keyword) {
        return newsArticleRepository.findKeywordFrequencyOverTime(keyword).stream()
                .map(row -> {
                    LocalDate day = ((LocalDateTime) row[0]).toLocalDate();
                    int count = ((Number) row[1]).intValue();
                    return new KeywordDateCountDto(day, count);
                })
                .toList();
    }

    public List<TrendingTopicDto> getTrendingTopics(LocalDateTime start, LocalDateTime end, int limit) {
        if (start.isAfter(end)) throw new StartAfterEndException();
        LocalDateTime prevStart = start.minus(Duration.between(start, end));
        return newsArticleRepository.findTopKeywordsInRange(start, end, prevStart, limit).stream()
                .map(row -> new TrendingTopicDto(
                        (String) row[0],
                        ((Number) row[1]).intValue(),
                        ((Number) row[2]).intValue(),
                        ((Number) row[3]).intValue()
                ))
                .toList();
    }

}
