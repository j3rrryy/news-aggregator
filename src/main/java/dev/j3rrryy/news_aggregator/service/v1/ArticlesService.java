package dev.j3rrryy.news_aggregator.service.v1;

import dev.j3rrryy.news_aggregator.dto.request.MarkDeleted;
import dev.j3rrryy.news_aggregator.dto.response.ArticlesAffected;
import dev.j3rrryy.news_aggregator.dto.response.ArticlesSummary;
import dev.j3rrryy.news_aggregator.enums.Status;
import dev.j3rrryy.news_aggregator.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArticlesService {

    private final CacheManagerService cacheManagerService;
    private final NewsArticleRepository newsArticleRepository;

    @Transactional(readOnly = true)
    @Cacheable(
            value = "newsArticlesSummary",
            key = "#root.methodName",
            condition = "!@parsingStatusManager.isParsingInProgress()"
    )
    public ArticlesSummary getArticlesSummary() {
        int newCount = newsArticleRepository.countByStatus(Status.NEW);
        int activeCount = newsArticleRepository.countByStatus(Status.ACTIVE);
        int deletedCount = newsArticleRepository.countByStatus(Status.DELETED);
        int total = newCount + activeCount + deletedCount;
        return new ArticlesSummary(newCount, activeCount, deletedCount, total);
    }

    @Transactional
    public ArticlesAffected markAsDeleted(MarkDeleted dto) {
        cacheManagerService.clearAllCaches();
        return new ArticlesAffected(
                newsArticleRepository.markAsDeletedByPublishedAtBefore(dto.olderThan())
        );
    }

    @Transactional
    public ArticlesAffected deleteMarkedArticles() {
        cacheManagerService.clearAllCaches();
        return new ArticlesAffected(newsArticleRepository.deleteAllMarkedAsDeleted());
    }

    @Transactional
    public ArticlesAffected deleteAllArticles() {
        cacheManagerService.clearAllCaches();
        return new ArticlesAffected(newsArticleRepository.deleteAllArticles());
    }

}
