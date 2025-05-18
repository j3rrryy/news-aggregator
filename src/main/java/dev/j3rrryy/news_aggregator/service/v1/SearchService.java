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
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final SearchMapper searchMapper;
    private final NewsArticleRepository newsArticleRepository;

    @Transactional(readOnly = true)
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
            Integer page,
            Integer size
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
        int safePage = (page != null) ? page : 0;
        int safeSize = (size != null) ? size : 10;

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

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(direction, property));
        return newsArticleRepository.findAll(spec, pageable)
                .map(searchMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public NewsArticleFull getById(UUID id) {
        return newsArticleRepository.findById(id)
                .map(searchMapper::toFull)
                .orElseThrow(() -> new ArticleNotFoundException(id));
    }

}
