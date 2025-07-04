package dev.j3rrryy.news_aggregator.dto.response;

import dev.j3rrryy.news_aggregator.enums.Category;
import dev.j3rrryy.news_aggregator.enums.Source;
import dev.j3rrryy.news_aggregator.enums.Status;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record NewsArticleFull(
        UUID id,
        String title,
        String summary,
        String content,
        Category category,
        List<String> keywords,
        List<String> mediaUrls,
        String url,
        Status status,
        LocalDateTime publishedAt,
        Source source
) implements Serializable {

}
