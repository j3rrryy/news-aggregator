package dev.j3rrryy.news_aggregator.dto.response;

import java.io.Serializable;

public record ArticlesSummary(
        int newArticlesCount, int activeArticlesCount, int deletedArticlesCount, int totalArticles
) implements Serializable {

}
