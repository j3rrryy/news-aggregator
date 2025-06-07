package dev.j3rrryy.news_aggregator.service.v1;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SearchServiceTest {

    @Mock
    private SearchMapper searchMapper;

    @InjectMocks
    private SearchService searchService;

    @Mock
    private NewsArticleRepository repository;

    @Test
    void searchNews_toDateNull_noThrow() {
        Specification<NewsArticle> spec = any();
        LocalDateTime from = LocalDateTime.of(2025, 5, 1, 0, 0);
        when(repository.findAll(spec, any(Pageable.class))).thenReturn(new PageImpl<>(Collections.emptyList()));

        CursorPage result = searchService.searchNews(
                null, from, null, null, null, null, null,
                SortField.PUBLISHED_AT, SortDirection.ASC, null, 5
        );

        assertThat(result.articles()).isEmpty();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    void searchNews_fromDateAfterToDate_throws() {
        LocalDateTime from = LocalDateTime.of(2025, 5, 7, 0, 0);
        LocalDateTime to = LocalDateTime.of(2025, 5, 1, 0, 0);

        assertThatThrownBy(() ->
                searchService.searchNews(
                        null, from, to, null, null, null, null,
                        SortField.PUBLISHED_AT, SortDirection.DESC, null, 10
                )
        ).isInstanceOf(FromDateAfterToDateException.class);
    }

    @Test
    void searchNews_emptyResults_returnsEmptyPage() {
        Page<NewsArticle> page = new PageImpl<>(
                Collections.emptyList(), PageRequest.of(0, 5), 0);
        Specification<NewsArticle> spec = any();
        when(repository.findAll(spec, any(Pageable.class))).thenReturn(page);

        CursorPage result = searchService.searchNews(
                null, null, null, null, null, null, null,
                SortField.PUBLISHED_AT, SortDirection.ASC, null, 5
        );

        assertThat(result.articles()).isEmpty();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    void searchNews_withResults_andHasNext_generatesNextCursor() {
        NewsArticle a1 = new NewsArticle(
                UUID.randomUUID(),
                "test title 1",
                "test summary 1",
                "test content 1",
                Category.SCIENCE_TECH,
                Set.of(),
                Set.of(),
                "test url 1",
                Status.ACTIVE,
                LocalDateTime.of(2025, 5, 1, 0, 0),
                Source.AIF_RU
        );
        NewsArticle a2 = new NewsArticle(
                UUID.randomUUID(),
                "test title 2",
                "test summary 2",
                "test content 2",
                Category.POLITICS,
                Set.of(),
                Set.of(),
                "test url 2",
                Status.ACTIVE,
                LocalDateTime.of(2025, 5, 7, 0, 0),
                Source.SVPRESSA_RU
        );

        Page<NewsArticle> page = new PageImpl<>(List.of(a1, a2),
                PageRequest.of(0, 2, Sort.by("publishedAt")), 3);
        Specification<NewsArticle> spec = any();
        when(repository.findAll(spec, any(Pageable.class))).thenReturn(page);

        NewsArticleSummary s1 = new NewsArticleSummary(
                a1.getId(),
                a1.getTitle(),
                a1.getSummary(),
                a1.getCategory(),
                a1.getKeywords().stream().toList(),
                a1.getMediaUrls().stream().toList(),
                a1.getUrl(),
                a1.getStatus(),
                a1.getPublishedAt(),
                a1.getSource()
        );
        NewsArticleSummary s2 = new NewsArticleSummary(
                a2.getId(),
                a2.getTitle(),
                a2.getSummary(),
                a2.getCategory(),
                a2.getKeywords().stream().toList(),
                a2.getMediaUrls().stream().toList(),
                a2.getUrl(),
                a2.getStatus(),
                a2.getPublishedAt(),
                a2.getSource()
        );
        when(searchMapper.toSummary(a1)).thenReturn(s1);
        when(searchMapper.toSummary(a2)).thenReturn(s2);

        CursorPage result = searchService.searchNews(
                null, LocalDateTime.of(2025, 5, 1, 0, 0),
                LocalDateTime.of(2025, 5, 7, 0, 0), Set.of(Category.POLITICS),
                Set.of(Source.RT_RU), Set.of(Status.NEW), Set.of("kw"),
                SortField.PUBLISHED_AT, SortDirection.ASC, "", 2
        );

        assertThat(result.articles()).containsExactly(s1, s2);
        String expectedCursor = s2.publishedAt() + "|" + s2.id();
        assertThat(result.nextCursor()).isEqualTo(expectedCursor);
    }

    @Test
    void searchNews_invalidCursor_throws() {
        assertThatThrownBy(() ->
                searchService.searchNews(
                        null, null, null, null, null, null,
                        null, SortField.PUBLISHED_AT, SortDirection.DESC, "test|bad|cursor", 5
                )
        ).isInstanceOf(InvalidCursorFormatException.class);

        assertThatThrownBy(() ->
                searchService.searchNews(
                        null, null, null, null, null, null,
                        null, SortField.PUBLISHED_AT, SortDirection.DESC,
                        "test timestamp|test uuid", 5
                )
        ).isInstanceOf(InvalidCursorFormatException.class);
    }

    @Test
    void searchNews_validDateRange_andEmptyButHasNext_returnsNoCursor() {
        LocalDateTime from = LocalDateTime.of(2025, 5, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2025, 5, 7, 0, 0);

        Page<NewsArticle> page = new PageImpl<>(
                Collections.emptyList(),
                PageRequest.of(0, 1, Sort.by("publishedAt")),
                2
        );
        Specification<NewsArticle> spec = any();
        when(repository.findAll(spec, any(Pageable.class))).thenReturn(page);

        CursorPage result = searchService.searchNews(
                null, from, to, null, null, null, null,
                SortField.PUBLISHED_AT, SortDirection.ASC, null, 1
        );

        assertThat(result.articles()).isEmpty();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    void searchNews_withValidCursor_parsesAndReturnsResults() {
        NewsArticle article = new NewsArticle(
                UUID.randomUUID(),
                "test title",
                "test summary",
                "test content",
                Category.SCIENCE_TECH,
                Set.of(),
                Set.of(),
                "test url",
                Status.ACTIVE,
                LocalDateTime.of(2025, 5, 1, 0, 0),
                Source.AIF_RU
        );

        Page<NewsArticle> page = new PageImpl<>(List.of(article),
                PageRequest.of(0, 2, Sort.by("publishedAt")), 1);
        Specification<NewsArticle> spec = any();
        when(repository.findAll(spec, any(Pageable.class))).thenReturn(page);

        NewsArticleSummary summary = new NewsArticleSummary(
                article.getId(),
                article.getTitle(),
                article.getSummary(),
                article.getCategory(),
                article.getKeywords().stream().toList(),
                article.getMediaUrls().stream().toList(),
                article.getUrl(),
                article.getStatus(),
                article.getPublishedAt(),
                article.getSource()
        );
        when(searchMapper.toSummary(article)).thenReturn(summary);

        String timestamp = "2025-05-07T00:00";
        String uuid = article.getId().toString();
        String cursor = timestamp + "|" + uuid;

        CursorPage result = searchService.searchNews(
                null, null, null, null, null, null,
                null, SortField.PUBLISHED_AT, SortDirection.ASC, cursor, 1
        );

        assertThat(result.articles()).containsExactly(summary);
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    void getNewsArticle_found_returnsFull() {
        UUID id = UUID.randomUUID();
        NewsArticle article = new NewsArticle(
                id,
                "test title",
                "test summary",
                "test content",
                Category.SCIENCE_TECH,
                Set.of(),
                Set.of(),
                "test url",
                Status.ACTIVE,
                LocalDateTime.of(2025, 5, 1, 0, 0),
                Source.AIF_RU
        );
        when(repository.findById(id)).thenReturn(Optional.of(article));

        NewsArticleFull full = new NewsArticleFull(
                id,
                article.getTitle(),
                article.getSummary(),
                article.getContent(),
                article.getCategory(),
                article.getKeywords().stream().toList(),
                article.getMediaUrls().stream().toList(),
                article.getUrl(),
                article.getStatus(),
                article.getPublishedAt(),
                article.getSource()
        );
        when(searchMapper.toFull(article)).thenReturn(full);

        NewsArticleFull result = searchService.getNewsArticle(id);

        assertThat(result).isSameAs(full);
    }

    @Test
    void getNewsArticle_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> searchService.getNewsArticle(id))
                .isInstanceOf(ArticleNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

}
