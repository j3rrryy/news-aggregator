package dev.j3rrryy.news_aggregator.specification;

import dev.j3rrryy.news_aggregator.entity.NewsArticle;
import dev.j3rrryy.news_aggregator.enums.Category;
import dev.j3rrryy.news_aggregator.enums.Source;
import dev.j3rrryy.news_aggregator.enums.Status;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NewsArticleSpecsTest {

    @Mock
    private Root<NewsArticle> root;

    @Mock
    private CriteriaQuery<?> cq;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Path<LocalDateTime> pathDate;

    @Mock
    private Path<Category> pathCategory;

    @Mock
    private Path<Source> pathSource;

    @Mock
    private Path<Status> pathStatus;

    @Mock
    private Join<NewsArticle, String> joinKeywords;

    @Mock
    private Predicate dummyPredicate;

    @BeforeEach
    void setup() {
        lenient().when(root.<LocalDateTime>get("publishedAt")).thenReturn(pathDate);
        lenient().when(root.<Category>get("category")).thenReturn(pathCategory);
        lenient().when(root.<Source>get("source")).thenReturn(pathSource);
        lenient().when(root.<Status>get("status")).thenReturn(pathStatus);
        lenient().when(root.<NewsArticle, String>join("keywords")).thenReturn(joinKeywords);
        lenient().when(root.<UUID>get("id")).thenReturn(mock());

        lenient().when(cb.conjunction()).thenReturn(dummyPredicate);

        lenient().when(pathCategory.in(anyCollection())).thenReturn(dummyPredicate);
        lenient().when(pathSource.in(anyCollection())).thenReturn(dummyPredicate);
        lenient().when(pathStatus.in(anyCollection())).thenReturn(dummyPredicate);
        lenient().when(joinKeywords.in(anyCollection())).thenReturn(dummyPredicate);

        lenient().when(cb.greaterThanOrEqualTo(Mockito.<Expression<LocalDateTime>>any(), any(LocalDateTime.class)))
                .thenReturn(dummyPredicate);
        lenient().when(cb.lessThanOrEqualTo(Mockito.<Expression<LocalDateTime>>any(), any(LocalDateTime.class)))
                .thenReturn(dummyPredicate);
        lenient().when(cb.lessThan(Mockito.<Expression<LocalDateTime>>any(), any(LocalDateTime.class)))
                .thenReturn(dummyPredicate);
        lenient().when(cb.equal(Mockito.<Expression<LocalDateTime>>any(), any(LocalDateTime.class)))
                .thenReturn(dummyPredicate);
        lenient().when(cb.lessThan(Mockito.<Expression<UUID>>any(), any(UUID.class)))
                .thenReturn(dummyPredicate);

        lenient().when(cb.and(any(Predicate.class), any(Predicate.class))).thenReturn(dummyPredicate);
        lenient().when(cb.or(any(Predicate.class), any(Predicate.class))).thenReturn(dummyPredicate);

        lenient().when(cb.function(eq("to_tsvector"), eq(String.class), any(Expression[].class)))
                .thenReturn(mock());
        lenient().when(cb.function(eq("plainto_tsquery"), eq(String.class), any(Expression[].class)))
                .thenReturn(mock());
        lenient().when(cb.function(eq("fts"), eq(Boolean.class), any(Expression[].class)))
                .thenReturn(mock());
        lenient().when(cb.isTrue(any())).thenReturn(dummyPredicate);
    }

    @Test
    void constructor_shouldThrowException() {
        assertThatThrownBy(() -> {
            Constructor<NewsArticleSpecs> constructor = NewsArticleSpecs.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        }).hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    @Nested
    class FullTextTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void fullText_nullOrBlank(String query) {
            Specification<NewsArticle> spec = NewsArticleSpecs.filterAll(
                    query,
                    null,
                    null,
                    Collections.emptySet(),
                    Collections.emptySet(),
                    Collections.emptySet(),
                    Collections.emptySet(),
                    null,
                    null
            );

            Predicate result = spec.toPredicate(root, cq, cb);

            verify(cb, atLeastOnce()).conjunction();
            verify(cb, never()).function(anyString(), any(), any(Expression[].class));
            verify(cb, never()).isTrue(any());
            assertThat(result).isEqualTo(dummyPredicate);

            reset(cb);
            lenient().when(cb.conjunction()).thenReturn(dummyPredicate);
        }

        @ParameterizedTest
        @ValueSource(strings = {"test", "  spring  "})
        void fullText_nonBlank(String query) {
            Specification<NewsArticle> spec = NewsArticleSpecs.filterAll(
                    query,
                    null,
                    null,
                    Collections.emptySet(),
                    Collections.emptySet(),
                    Collections.emptySet(),
                    Collections.emptySet(),
                    null,
                    null
            );

            Predicate result = spec.toPredicate(root, cq, cb);

            verify(cb, times(1))
                    .function(eq("to_tsvector"), eq(String.class), any(Expression[].class));
            verify(cb, times(1))
                    .function(eq("plainto_tsquery"), eq(String.class), any(Expression[].class));
            verify(cb, times(1))
                    .function(eq("fts"), eq(Boolean.class), any(Expression[].class));
            verify(cb, times(1)).isTrue(any());
            assertThat(result).isEqualTo(dummyPredicate);
        }

    }

    @Nested
    class DateBetweenTests {

        static Stream<Arguments> dateRanges() {
            return Stream.of(
                    Arguments.of(null, null, false, false),
                    Arguments.of(
                            LocalDateTime.of(2025, 5, 1, 0, 0),
                            null, true, false
                    ),
                    Arguments.of(
                            null,
                            LocalDateTime.of(2025, 5, 7, 0, 0),
                            false, true
                    ),
                    Arguments.of(
                            LocalDateTime.of(2025, 5, 1, 0, 0),
                            LocalDateTime.of(2025, 5, 7, 0, 0),
                            true, true
                    )
            );
        }

        @ParameterizedTest
        @MethodSource("dateRanges")
        void dateBetween_various(LocalDateTime from,
                                 LocalDateTime to,
                                 boolean expectGreater,
                                 boolean expectLessOrEqual) {
            Specification<NewsArticle> spec = NewsArticleSpecs.filterAll(
                    null,
                    from,
                    to,
                    Collections.emptySet(),
                    Collections.emptySet(),
                    Collections.emptySet(),
                    Collections.emptySet(),
                    null,
                    null
            );

            Predicate result = spec.toPredicate(root, cq, cb);

            if (expectGreater) {
                verify(cb, times(1)).greaterThanOrEqualTo(any(), eq(from));
            } else {
                verify(cb, never()).greaterThanOrEqualTo(any(), any(LocalDateTime.class));
            }

            if (expectLessOrEqual) {
                verify(cb, times(1)).lessThanOrEqualTo(any(), eq(to));
            } else {
                verify(cb, never()).lessThanOrEqualTo(any(), any(LocalDateTime.class));
            }

            verify(cb, atLeastOnce()).and(any(Predicate.class), any(Predicate.class));
            assertThat(result).isEqualTo(dummyPredicate);
        }

    }

    @Nested
    class CategoriesTests {

        @NullSource
        @EmptySource
        @ParameterizedTest
        void categories_nullOrEmpty(Set<Category> categories) {
            Specification<NewsArticle> spec = NewsArticleSpecs.filterAll(
                    null,
                    null,
                    null,
                    categories,
                    Set.of(Source.SVPRESSA_RU),
                    Set.of(Status.NEW),
                    Set.of("java"),
                    null,
                    null
            );

            spec.toPredicate(root, cq, cb);
            verify(cb, atLeastOnce()).conjunction();
            verify(root, never()).<Category>get("category");

            reset(cb);
            lenient().when(cb.conjunction()).thenReturn(dummyPredicate);
        }

        @Test
        void categories_nonEmpty() {
            Set<Category> categories = Set.of(Category.POLITICS, Category.SPORT);
            Specification<NewsArticle> spec = NewsArticleSpecs.filterAll(
                    null,
                    null,
                    null,
                    categories,
                    Collections.emptySet(),
                    Collections.emptySet(),
                    Collections.emptySet(),
                    null,
                    null
            );

            Predicate result = spec.toPredicate(root, cq, cb);

            verify(root, times(1)).<Category>get("category");
            verify(pathCategory, times(1)).in(categories);
            verify(cb, atLeastOnce()).and(any(Predicate.class), any(Predicate.class));
            assertThat(result).isEqualTo(dummyPredicate);
        }

    }

    @Nested
    class SourcesTests {

        @NullSource
        @EmptySource
        @ParameterizedTest
        void sources_nullOrEmpty(Set<Source> sources) {
            Specification<NewsArticle> spec = NewsArticleSpecs.filterAll(
                    null,
                    null,
                    null,
                    Set.of(Category.SCIENCE_TECH),
                    sources,
                    Set.of(Status.NEW),
                    Set.of("java"),
                    null,
                    null
            );

            spec.toPredicate(root, cq, cb);
            verify(cb, atLeastOnce()).conjunction();
            verify(root, never()).<Source>get("source");

            reset(cb);
            lenient().when(cb.conjunction()).thenReturn(dummyPredicate);
        }

        @Test
        void sources_nonEmpty() {
            Set<Source> sources = Set.of(Source.RT_RU, Source.AIF_RU);
            Specification<NewsArticle> spec = NewsArticleSpecs.filterAll(
                    null,
                    null,
                    null,
                    Collections.emptySet(),
                    sources,
                    Collections.emptySet(),
                    Collections.emptySet(),
                    null,
                    null
            );

            Predicate result = spec.toPredicate(root, cq, cb);

            verify(root, times(1)).<Source>get("source");
            verify(pathSource, times(1)).in(sources);
            verify(cb, atLeastOnce()).and(any(Predicate.class), any(Predicate.class));
            assertThat(result).isEqualTo(dummyPredicate);
        }

    }

    @Nested
    class StatusesTests {

        @NullSource
        @EmptySource
        @ParameterizedTest
        void statuses_nullOrEmpty(Set<Status> statuses) {
            Specification<NewsArticle> spec = NewsArticleSpecs.filterAll(
                    null,
                    null,
                    null,
                    Set.of(Category.ECONOMICS),
                    Set.of(Source.AIF_RU),
                    statuses,
                    Set.of("news"),
                    null,
                    null
            );

            spec.toPredicate(root, cq, cb);
            verify(cb, atLeastOnce()).conjunction();
            verify(root, never()).<Status>get("status");

            reset(cb);
            lenient().when(cb.conjunction()).thenReturn(dummyPredicate);
        }

        @Test
        void statuses_nonEmpty() {
            Set<Status> statuses = Set.of(Status.NEW, Status.ACTIVE);
            Specification<NewsArticle> spec = NewsArticleSpecs.filterAll(
                    null,
                    null,
                    null,
                    Collections.emptySet(),
                    Collections.emptySet(),
                    statuses,
                    Collections.emptySet(),
                    null,
                    null
            );

            Predicate result = spec.toPredicate(root, cq, cb);

            verify(root, times(1)).<Status>get("status");
            verify(pathStatus, times(1)).in(statuses);
            verify(cb, atLeastOnce()).and(any(Predicate.class), any(Predicate.class));
            assertThat(result).isEqualTo(dummyPredicate);
        }

    }

    @Nested
    class KeywordsTests {

        @NullSource
        @EmptySource
        @ParameterizedTest
        void keywords_nullOrEmpty(Set<String> keywords) {
            Specification<NewsArticle> spec = NewsArticleSpecs.filterAll(
                    null,
                    null,
                    null,
                    Set.of(Category.SPORT),
                    Set.of(Source.RT_RU),
                    Set.of(Status.DELETED),
                    keywords,
                    null,
                    null
            );

            spec.toPredicate(root, cq, cb);
            verify(cb, atLeastOnce()).conjunction();
            verify(root, never()).join("keywords");

            reset(cb);
            lenient().when(cb.conjunction()).thenReturn(dummyPredicate);
        }

        @Test
        void keywords_nonEmpty() {
            Set<String> keywords = Set.of("java", "spring");
            Specification<NewsArticle> spec = NewsArticleSpecs.filterAll(
                    null,
                    null,
                    null,
                    Collections.emptySet(),
                    Collections.emptySet(),
                    Collections.emptySet(),
                    keywords,
                    null,
                    null
            );

            Predicate result = spec.toPredicate(root, cq, cb);

            verify(root, times(1)).join("keywords");
            verify(joinKeywords, times(1)).in(keywords);
            verify(cb, atLeastOnce()).and(any(Predicate.class), any(Predicate.class));
            assertThat(result).isEqualTo(dummyPredicate);
        }

    }

    @Nested
    class CursorTests {

        static Stream<Arguments> cursorArgs() {
            return Stream.of(
                    Arguments.of(null, null, true, false, false),
                    Arguments.of(null, UUID.randomUUID(), true, false, false),
                    Arguments.of(
                            LocalDateTime.of(2025, 5, 1, 0, 0),
                            null, true, false, false
                    ),
                    Arguments.of(
                            LocalDateTime.of(2025, 5, 1, 0, 0),
                            UUID.randomUUID(), false, true, true
                    )
            );
        }

        @ParameterizedTest
        @MethodSource("cursorArgs")
        void byCursor_various(LocalDateTime publishedAt, UUID lastId, boolean expectConjunction) {
            Specification<NewsArticle> spec = NewsArticleSpecs.filterAll(
                    null,
                    null,
                    null,
                    Collections.emptySet(),
                    Collections.emptySet(),
                    Collections.emptySet(),
                    Collections.emptySet(),
                    publishedAt,
                    lastId
            );

            Predicate result = spec.toPredicate(root, cq, cb);

            if (expectConjunction) {
                verify(cb, atLeastOnce()).conjunction();
                verify(cb, never()).lessThan(any(), any(LocalDateTime.class));
                verify(cb, never()).equal(any(Expression.class), any(LocalDateTime.class));
                verify(cb, never()).lessThan(any(), any(UUID.class));
                verify(cb, never()).or(any(Predicate.class), any(Predicate.class));
            } else {
                verify(cb, times(1))
                        .lessThan(Mockito.<Expression<LocalDateTime>>any(), eq(publishedAt));
                verify(cb, times(1))
                        .equal(Mockito.<Expression<LocalDateTime>>any(), eq(publishedAt));
                verify(cb, times(1))
                        .lessThan(Mockito.<Expression<UUID>>any(), eq(lastId));
                verify(cb, times(1))
                        .or(any(Predicate.class), any(Predicate.class));
            }

            assertThat(result).isEqualTo(dummyPredicate);

            reset(cb);
            lenient().when(cb.conjunction()).thenReturn(dummyPredicate);
        }

    }

}
