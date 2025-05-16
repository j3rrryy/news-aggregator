package dev.j3rrryy.news_aggregator.dto.response;

public record TrendingTopicDto(String keyword, int currentCount, int previousCount, int delta) {

}
