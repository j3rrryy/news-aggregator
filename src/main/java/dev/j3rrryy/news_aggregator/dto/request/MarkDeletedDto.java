package dev.j3rrryy.news_aggregator.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDateTime;

public record MarkDeletedDto(
        @NotNull(message = "Timestamp must be provided")
        @PastOrPresent(message = "Timestamp must be in the past or present")
        LocalDateTime olderThan
) {

}
