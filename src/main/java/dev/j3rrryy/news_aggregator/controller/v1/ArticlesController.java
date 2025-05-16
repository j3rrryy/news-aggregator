package dev.j3rrryy.news_aggregator.controller.v1;

import dev.j3rrryy.news_aggregator.dto.request.MarkDeletedDto;
import dev.j3rrryy.news_aggregator.dto.response.ArticlesAffectedDto;
import dev.j3rrryy.news_aggregator.dto.response.ArticlesSummaryDto;
import dev.j3rrryy.news_aggregator.service.v1.ArticlesService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/articles")
@RequiredArgsConstructor
@Tag(name = "Articles", description = "Endpoints for managing news articles")
public class ArticlesController {

    private final ArticlesService articlesService;

    @GetMapping("/summary")
    @ApiResponse(responseCode = "200", description = "Count of new, active, deleted articles")
    public ArticlesSummaryDto getArticlesSummary() {
        return articlesService.getArticlesSummary();
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
        return articlesService.markAsDeleted(markDeletedDto);
    }

    @DeleteMapping("/marked")
    @ApiResponse(responseCode = "200", description = "News articles marked as 'deleted' deleted successfully")
    public ArticlesAffectedDto deleteMarkedArticles() {
        return articlesService.deleteMarkedArticles();
    }

    @DeleteMapping("/all")
    @ApiResponse(responseCode = "200", description = "All news articles deleted successfully")
    public ArticlesAffectedDto deleteAllArticles() {
        return articlesService.deleteAllArticles();
    }

}
