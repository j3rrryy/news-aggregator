package dev.j3rrryy.news_aggregator.dto.response;

import java.io.Serializable;
import java.time.LocalDate;

public record KeywordDateCount(LocalDate date, int count) implements Serializable {

}
