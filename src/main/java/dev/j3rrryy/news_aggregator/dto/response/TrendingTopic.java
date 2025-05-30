package dev.j3rrryy.news_aggregator.dto.response;

import java.io.Serializable;

public record TrendingTopic(String keyword, int currentCount, int previousCount, int delta) implements Serializable {

}
