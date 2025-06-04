package dev.j3rrryy.news_aggregator.utils;

import dev.j3rrryy.news_aggregator.enums.SortDirection;
import dev.j3rrryy.news_aggregator.enums.SortField;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class SortResolverTest {

    @Test
    void resolveSort_defaultsToPublishedAtDesc() {
        Sort sort = SortResolver.resolveSort(null, null);
        Sort.Order order = sort.getOrderFor("publishedAt");

        assertThat(order).isNotNull();
        Assertions.assertNotNull(order);
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void resolveSort_defaultDirection() {
        Sort sort = SortResolver.resolveSort(SortField.TITLE, null);
        Sort.Order order = sort.getOrderFor("title");

        assertThat(order).isNotNull();
        Assertions.assertNotNull(order);
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void resolveSort_defaultField() {
        Sort sort = SortResolver.resolveSort(null, SortDirection.ASC);
        Sort.Order order = sort.getOrderFor("publishedAt");

        assertThat(order).isNotNull();
        Assertions.assertNotNull(order);
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void resolveSort_allFields() {
        assertFieldSort(SortField.ID, "id");
        assertFieldSort(SortField.TITLE, "title");
        assertFieldSort(SortField.SUMMARY, "summary");
        assertFieldSort(SortField.CONTENT, "content");
        assertFieldSort(SortField.CATEGORY, "category");
        assertFieldSort(SortField.URL, "url");
        assertFieldSort(SortField.STATUS, "status");
        assertFieldSort(SortField.PUBLISHED_AT, "publishedAt");
        assertFieldSort(SortField.SOURCE, "source");
    }

    private void assertFieldSort(SortField field, String expectedProperty) {
        Sort sort = SortResolver.resolveSort(field, SortDirection.ASC);
        Sort.Order order = sort.getOrderFor(expectedProperty);

        assertThat(order).isNotNull();
        Assertions.assertNotNull(order);
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

}
