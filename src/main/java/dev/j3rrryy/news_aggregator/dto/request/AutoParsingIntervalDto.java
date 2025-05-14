package dev.j3rrryy.news_aggregator.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Duration;

public record AutoParsingIntervalDto(@Schema(example = "2d5h10m") Duration autoParsingInterval) {

}
