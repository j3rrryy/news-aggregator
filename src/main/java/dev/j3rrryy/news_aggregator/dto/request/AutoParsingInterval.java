package dev.j3rrryy.news_aggregator.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;

public record AutoParsingInterval(
        @Schema(example = "2d5h10m")
        @NotNull(message = "Interval must be provided")
        Duration autoParsingInterval
) {

}
