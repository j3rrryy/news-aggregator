package dev.j3rrryy.news_aggregator.utils;

import dev.j3rrryy.news_aggregator.enums.SortDirection;
import dev.j3rrryy.news_aggregator.enums.SortField;
import org.springframework.data.domain.Sort;

public class SortResolver {

    private SortResolver() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Sort resolveSort(SortField sortField, SortDirection sortDirection) {
        SortField safeField = (sortField == null) ? SortField.PUBLISHED_AT : sortField;
        SortDirection safeDirection = (sortDirection == null) ? SortDirection.DESC : sortDirection;

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
        return Sort.by(Sort.Direction.fromString(safeDirection.name()), property);
    }

}
