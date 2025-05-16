package dev.j3rrryy.news_aggregator.service.v1;

import dev.j3rrryy.news_aggregator.dto.request.MarkDeletedDto;
import dev.j3rrryy.news_aggregator.dto.response.ArticlesAffectedDto;
import dev.j3rrryy.news_aggregator.dto.response.ArticlesSummaryDto;
import dev.j3rrryy.news_aggregator.enums.Status;
import dev.j3rrryy.news_aggregator.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArticlesService {

    private final NewsArticleRepository newsArticleRepository;

    public ArticlesSummaryDto getArticlesSummary() {
        int newCount = newsArticleRepository.countByStatus(Status.NEW);
        int activeCount = newsArticleRepository.countByStatus(Status.ACTIVE);
        int deletedCount = newsArticleRepository.countByStatus(Status.DELETED);
        return new ArticlesSummaryDto(newCount, activeCount, deletedCount);
    }

    public ArticlesAffectedDto markAsDeleted(MarkDeletedDto markDeletedDto) {
        return new ArticlesAffectedDto(
                newsArticleRepository.markAsDeletedByPublishedAtBefore(markDeletedDto.olderThan())
        );
    }

    public ArticlesAffectedDto deleteMarkedArticles() {
        return new ArticlesAffectedDto(newsArticleRepository.deleteAllMarkedAsDeleted());
    }

    public ArticlesAffectedDto deleteAllArticles() {
        return new ArticlesAffectedDto(newsArticleRepository.deleteAllArticles());
    }

}
