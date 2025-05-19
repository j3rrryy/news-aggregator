package dev.j3rrryy.news_aggregator.service.v1;

import dev.j3rrryy.news_aggregator.dto.request.MarkDeletedDto;
import dev.j3rrryy.news_aggregator.dto.response.ArticlesAffectedDto;
import dev.j3rrryy.news_aggregator.dto.response.ArticlesSummaryDto;
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
    public ArticlesSummaryDto getArticlesSummary() {
        int newCount = newsArticleRepository.countByStatus(Status.NEW);
        int activeCount = newsArticleRepository.countByStatus(Status.ACTIVE);
        int deletedCount = newsArticleRepository.countByStatus(Status.DELETED);
        int total = newCount + activeCount + deletedCount;
        return new ArticlesSummaryDto(newCount, activeCount, deletedCount, total);
    }

    @Transactional
    public ArticlesAffectedDto markAsDeleted(MarkDeletedDto markDeletedDto) {
        cacheManagerService.clearAllCaches();
        return new ArticlesAffectedDto(
                newsArticleRepository.markAsDeletedByPublishedAtBefore(markDeletedDto.olderThan())
        );
    }

    @Transactional
    public ArticlesAffectedDto deleteMarkedArticles() {
        cacheManagerService.clearAllCaches();
        return new ArticlesAffectedDto(newsArticleRepository.deleteAllMarkedAsDeleted());
    }

    @Transactional
    public ArticlesAffectedDto deleteAllArticles() {
        cacheManagerService.clearAllCaches();
        return new ArticlesAffectedDto(newsArticleRepository.deleteAllArticles());
    }

}
