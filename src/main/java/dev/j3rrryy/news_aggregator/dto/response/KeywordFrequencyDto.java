package dev.j3rrryy.news_aggregator.dto.response;

import java.io.Serializable;

public record KeywordFrequencyDto(String keyword, int frequency) implements Serializable {

}
