package dev.j3rrryy.news_aggregator.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Duration;

public record AutoParsingStatus(
        boolean autoParsingEnabled,

        @Schema(example = "2d5h10m")
        Duration autoParsingInterval
) {

}
