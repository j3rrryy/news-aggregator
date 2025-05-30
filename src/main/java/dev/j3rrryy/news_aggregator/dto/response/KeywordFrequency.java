package dev.j3rrryy.news_aggregator.dto.response;

import java.io.Serializable;

public record KeywordFrequency(String keyword, int frequency) implements Serializable {

}
