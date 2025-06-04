package dev.j3rrryy.news_aggregator.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;

public record AutoParsingInterval(
        @Schema(examples = {"2d5h10m", "9m3h", "5d"})
        @NotNull(message = "Interval must be provided")
        Duration autoParsingInterval
) {

}
