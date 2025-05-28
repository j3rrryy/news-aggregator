package dev.j3rrryy.news_aggregator.controller.v1;

import dev.j3rrryy.news_aggregator.dto.response.*;
import dev.j3rrryy.news_aggregator.service.v1.AnalyticsService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/analytics")
@Tag(name = "Analytics", description = "Endpoints for retrieving statistics and trending topics from the news articles")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/categories")
    @ApiResponse(responseCode = "200", description = "Count of news articles by category")
    public CategoryCountsDto getCategoryCounts() {
        return analyticsService.getCategoryCounts();
    }

    @GetMapping("/keywords/top")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Top frequent keywords"),
            @ApiResponse(responseCode = "400", ref = "ValidationFailed")
    })
    public List<KeywordFrequencyDto> getTopFrequentKeywords(
            @RequestParam(defaultValue = "10") @Positive(message = "Limit must be > 0") int limit
    ) {
        return analyticsService.getTopFrequentKeywords(limit);
    }

    @GetMapping("/keywords/trend/{keyword}")
    @ApiResponse(responseCode = "200", description = "Frequency trend of the keyword over time")
    public List<KeywordDateCountDto> getKeywordTrend(@PathVariable String keyword) {
        return analyticsService.getKeywordTrend(keyword);
    }

    @GetMapping("/keywords/trending")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trending topics over the period"),
            @ApiResponse(responseCode = "400", ref = "ValidationFailed")
    })
    public List<TrendingTopicDto> getTrendingTopics(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Past(message = "'fromDate' timestamp must be in the past")
            LocalDateTime fromDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @PastOrPresent(message = "'toDate' timestamp must be in the past or present")
            LocalDateTime toDate,

            @RequestParam(defaultValue = "10")
            @Positive(message = "Limit must be > 0")
            int limit
    ) {
        return analyticsService.getTrendingTopics(fromDate, toDate, limit);
    }

}
