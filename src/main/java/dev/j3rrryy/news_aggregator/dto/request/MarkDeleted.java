package dev.j3rrryy.news_aggregator.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public record MarkDeleted(
        @NotNull(message = "Timestamp must be provided")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @PastOrPresent(message = "Timestamp must be in the past or present")
        LocalDateTime olderThan
) {

}
