package dev.j3rrryy.news_aggregator.dto.response;

import java.io.Serializable;
import java.util.List;

public record CursorPage(List<NewsArticleSummary> articles, String nextCursor) implements Serializable {

}
