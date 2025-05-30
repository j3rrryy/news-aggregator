package dev.j3rrryy.news_aggregator.service.v1;

import dev.j3rrryy.news_aggregator.dto.request.CursorData;
import dev.j3rrryy.news_aggregator.dto.response.CursorPage;
import dev.j3rrryy.news_aggregator.dto.response.NewsArticleFull;
import dev.j3rrryy.news_aggregator.dto.response.NewsArticleSummary;
import dev.j3rrryy.news_aggregator.entity.NewsArticle;
import dev.j3rrryy.news_aggregator.enums.*;
import dev.j3rrryy.news_aggregator.exceptions.ArticleNotFoundException;
import dev.j3rrryy.news_aggregator.exceptions.FromDateAfterToDateException;
import dev.j3rrryy.news_aggregator.exceptions.InvalidCursorFormatException;
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
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static dev.j3rrryy.news_aggregator.utils.SortResolver.resolveSort;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private final SearchMapper searchMapper;
    private final NewsArticleRepository newsArticleRepository;

    @Cacheable(
            value = "newsSearch",
            key = "#root.methodName + ':' + #query + '_' + #dateFrom + '_' + #dateTo + '_' + " +
                    "T(java.util.Objects).hash(#categories != null ? new java.util.TreeSet(#categories) : null) + '_' + " +
                    "T(java.util.Objects).hash(#sources != null ? new java.util.TreeSet(#sources) : null) + '_' + " +
                    "T(java.util.Objects).hash(#statuses != null ? new java.util.TreeSet(#statuses) : null) + '_' + " +
                    "T(java.util.Objects).hash(#keywords != null ? new java.util.TreeSet(#keywords) : null) + '_' + " +
                    "#sortField + '_' + #sortDirection + '_' + #cursor + '_' + #size",
            condition = "!@parsingStatusManager.isParsingInProgress()"
    )
    public CursorPage<NewsArticleSummary> searchNews(
            String query,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            Set<Category> categories,
            Set<Source> sources,
            Set<Status> statuses,
            Set<String> keywords,
            SortField sortField,
            SortDirection sortDirection,
            String cursor,
            int size
    ) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new FromDateAfterToDateException();
        }

        CursorData cursorData = parseCursor(cursor);
        Specification<NewsArticle> spec = NewsArticleSpecs.filterAll(
                query, dateFrom, dateTo, categories, sources, statuses,
                keywords, cursorData.publishedAt(), cursorData.id()
        );

        Sort sort = resolveSort(sortField, sortDirection);
        Pageable pageable = PageRequest.of(0, size, sort);
        Slice<NewsArticle> slice = newsArticleRepository.findAll(spec, pageable);

        List<NewsArticleSummary> articleSummaries = slice.getContent().stream()
                .map(searchMapper::toSummary)
                .toList();

        String nextCursor = null;
        if (slice.hasNext() && !articleSummaries.isEmpty()) {
            NewsArticleSummary last = articleSummaries.getLast();
            nextCursor = last.publishedAt() + "|" + last.id();
        }
        return new CursorPage<>(articleSummaries, nextCursor);
    }

    @Cacheable(
            value = "newsById",
            key = "#root.methodName + ':' + #id",
            condition = "!@parsingStatusManager.isParsingInProgress()"
    )
    public NewsArticleFull getById(UUID id) {
        return newsArticleRepository.findById(id)
                .map(searchMapper::toFull)
                .orElseThrow(() -> new ArticleNotFoundException(id));
    }

    private CursorData parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return new CursorData(null, null);
        }
        try {
            String[] parts = cursor.split("\\|");
            if (parts.length != 2) throw new InvalidCursorFormatException(cursor);
            return new CursorData(LocalDateTime.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (DateTimeParseException | IllegalArgumentException e) {
            throw new InvalidCursorFormatException(cursor);
        }
    }

}
