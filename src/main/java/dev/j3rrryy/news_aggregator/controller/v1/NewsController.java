package dev.j3rrryy.news_aggregator.controller.v1;

import dev.j3rrryy.news_aggregator.dto.request.MarkDeletedDto;
import dev.j3rrryy.news_aggregator.dto.response.ArticlesAffectedDto;
import dev.j3rrryy.news_aggregator.service.v1.NewsService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/news")
@RequiredArgsConstructor
@Tag(name = "News", description = "Manage news")
public class NewsController {

    private final NewsService newsService;

    @PostMapping("/start-parsing")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Parsing started successfully"),
            @ApiResponse(responseCode = "409", description = "Parsing is already in progress")
    })
    public void startParsing() {
        newsService.startParsingAsync();
    }

    @PutMapping("/mark-deleted")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "News articles marked as 'deleted' successfully"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid timestamp format",
                    content = @io.swagger.v3.oas.annotations.media.Content()
            )
    })
    public ArticlesAffectedDto markAsDeleted(@Valid @RequestBody MarkDeletedDto markDeletedDto) {
        return newsService.markAsDeleted(markDeletedDto);
    }

    @DeleteMapping("/delete-marked")
    @ApiResponse(responseCode = "200", description = "News articles marked as 'deleted' deleted successfully")
    public ArticlesAffectedDto deleteMarkedArticles() {
        return newsService.deleteMarkedArticles();
    }

    @DeleteMapping("/delete-all")
    @ApiResponse(responseCode = "200", description = "All news articles deleted successfully")
    public ArticlesAffectedDto deleteAllArticles() {
        return newsService.deleteAllArticles();
    }

}
