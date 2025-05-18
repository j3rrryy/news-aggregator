package dev.j3rrryy.news_aggregator.controller.v1;

import dev.j3rrryy.news_aggregator.dto.response.NewsArticleFull;
import dev.j3rrryy.news_aggregator.dto.response.NewsArticleSummary;
import dev.j3rrryy.news_aggregator.enums.*;
import dev.j3rrryy.news_aggregator.service.v1.SearchService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/search")
@Tag(name = "Search", description = "Endpoints for searching, filtering and viewing news articles")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results"),
            @ApiResponse(responseCode = "400", ref = "ValidationFailed")
    })
    public Page<NewsArticleSummary> searchNews(
            @RequestParam(required = false)
            String query,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Past(message = "'dateFrom' timestamp must be in the past")
            LocalDateTime dateFrom,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @PastOrPresent(message = "'dateTo' timestamp must be in the past or present")
            LocalDateTime dateTo,

            @RequestParam(required = false)
            Category category,

            @RequestParam(required = false)
            Source source,

            @RequestParam(required = false)
            Status status,

            @RequestParam(required = false)
            List<String> keywords,

            @RequestParam(required = false)
            SortField sortField,

            @RequestParam(required = false)
            SortDirection sortDirection,

            @RequestParam(required = false)
            @PositiveOrZero(message = "Page must be >= 0")
            Integer page,

            @RequestParam(required = false)
            @Positive(message = "Size must be > 0")
            @Max(value = 100, message = "Size must be <= 100")
            Integer size
    ) {
        return searchService.searchNews(
                query,
                dateFrom,
                dateTo,
                category,
                source,
                status,
                keywords,
                sortField,
                sortDirection,
                page,
                size
        );
    }

    @GetMapping("/{id}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "News article"),
            @ApiResponse(responseCode = "400", ref = "ValidationFailed"),
            @ApiResponse(responseCode = "404", description = "Article not found")
    })
    public NewsArticleFull getNewsArticle(@PathVariable UUID id) {
        return searchService.getById(id);
    }

}
