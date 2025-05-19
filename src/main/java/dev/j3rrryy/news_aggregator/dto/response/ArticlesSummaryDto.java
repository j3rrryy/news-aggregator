package dev.j3rrryy.news_aggregator.dto.response;

import java.io.Serializable;

public record ArticlesSummaryDto(
        int newArticlesCount, int activeArticlesCount, int deletedArticlesCount
) implements Serializable {

}
