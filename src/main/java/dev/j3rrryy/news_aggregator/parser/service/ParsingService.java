package dev.j3rrryy.news_aggregator.parser.service;

import dev.j3rrryy.news_aggregator.entity.NewsArticle;
import dev.j3rrryy.news_aggregator.enums.Category;
import dev.j3rrryy.news_aggregator.enums.Source;
import dev.j3rrryy.news_aggregator.repository.NewsArticleBatchRepository;
import dev.j3rrryy.news_aggregator.repository.NewsArticleRepository;
import dev.j3rrryy.news_aggregator.service.v1.CacheManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ParsingService {

    private final CacheManagerService cacheManagerService;
    private final NewsArticleRepository newsArticleRepository;
    private final NewsArticleBatchRepository newsArticleBatchRepository;

    @Transactional
    public void prepareForParsing() {
        cacheManagerService.clearAllCaches();
        newsArticleRepository.updateAllNewToActive();
    }

    @Transactional(readOnly = true)
    public Map<Source, Map<Category, LocalDateTime>> getLatestPublishedAtByCategoryAndSource() {
        List<Object[]> query = newsArticleRepository.findLatestPublishedAtByCategoryAndSource();
        Map<Source, Map<Category, LocalDateTime>> result = new EnumMap<>(Source.class);

        for (Source source : Source.values()) {
            Map<Category, LocalDateTime> categoryMap = new EnumMap<>(Category.class);
            for (Category category : Category.values()) {
                Optional<Object[]> matchingRow = query.stream()
                        .filter(res -> res[0] == source && res[1] == category)
                        .findFirst();

                LocalDateTime latestPublishedAt = matchingRow
                        .map(res -> (LocalDateTime) res[2])
                        .orElse(null);

                categoryMap.put(category, latestPublishedAt);
            }
            result.put(source, categoryMap);
        }
        return result;
    }

    @Transactional
    public int saveArticles(List<NewsArticle> articles) {
        return newsArticleBatchRepository.saveAllBatch(articles);
    }

}
