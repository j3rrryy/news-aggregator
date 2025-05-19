package dev.j3rrryy.news_aggregator.service.v1;

import dev.j3rrryy.news_aggregator.dto.response.NewsArticleFull;
import dev.j3rrryy.news_aggregator.dto.response.NewsArticleSummary;
import dev.j3rrryy.news_aggregator.entity.NewsArticle;
import dev.j3rrryy.news_aggregator.enums.*;
import dev.j3rrryy.news_aggregator.exceptions.ArticleNotFoundException;
import dev.j3rrryy.news_aggregator.exceptions.FromDateAfterToDateException;
import dev.j3rrryy.news_aggregator.mapper.SearchMapper;
import dev.j3rrryy.news_aggregator.repository.NewsArticleRepository;
import dev.j3rrryy.news_aggregator.specification.NewsArticleSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private final SearchMapper searchMapper;
    private final NewsArticleRepository newsArticleRepository;

    @Cacheable(
            value = "newsSearch",
            key = "#root.methodName + '_' + #query + '_' + #dateFrom + '_' + " +
                    "#dateTo + '_' + #category + '_' + #source + '_' + " +
                    "#status + '_' + (#keywords != null ? #keywords.hashCode() : null) + '_' + " +
                    "#sortField + '_' + #sortDirection + '_' + #page + '_' + #size",
            condition = "!@parsingStatusManager.isParsingInProgress()"
    )
    public Page<NewsArticleSummary> searchNews(
            String query,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            Category category,
            Source source,
            Status status,
            List<String> keywords,
            SortField sortField,
            SortDirection sortDirection,
            int page,
            int size
    ) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) throw new FromDateAfterToDateException();

        Specification<NewsArticle> spec = NewsArticleSpecs.filterAll(
                query,
                dateFrom,
                dateTo,
                category,
                source,
                status,
                keywords
        );

        SortField safeField = (sortField == null) ? SortField.PUBLISHED_AT : sortField;
        SortDirection safeDirection = (sortDirection == null) ? SortDirection.DESC : sortDirection;

        Sort.Direction direction = Sort.Direction.fromString(safeDirection.name());
        String property = switch (safeField) {
            case ID -> "id";
            case TITLE -> "title";
            case SUMMARY -> "summary";
            case CONTENT -> "content";
            case CATEGORY -> "category";
            case URL -> "url";
            case STATUS -> "status";
            case PUBLISHED_AT -> "publishedAt";
            case SOURCE -> "source";
        };

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, property));
        return newsArticleRepository.findAll(spec, pageable)
                .map(searchMapper::toSummary);
    }

    @Cacheable(
            value = "newsById",
            key = "#root.methodName + '_' + #id",
            condition = "!@parsingStatusManager.isParsingInProgress()"
    )
    public NewsArticleFull getById(UUID id) {
        return newsArticleRepository.findById(id)
                .map(searchMapper::toFull)
                .orElseThrow(() -> new ArticleNotFoundException(id));
    }

}
