package dev.j3rrryy.news_aggregator.dto.request;

import java.time.LocalDateTime;
import java.util.UUID;

public record CursorData(LocalDateTime publishedAt, UUID id) {

}
