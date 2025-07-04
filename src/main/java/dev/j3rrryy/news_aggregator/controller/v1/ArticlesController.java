package dev.j3rrryy.news_aggregator.controller.v1;

import dev.j3rrryy.news_aggregator.dto.request.MarkDeleted;
import dev.j3rrryy.news_aggregator.dto.response.ArticlesAffected;
import dev.j3rrryy.news_aggregator.dto.response.ArticlesSummary;
import dev.j3rrryy.news_aggregator.service.v1.ArticlesService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/articles")
@Tag(name = "Articles", description = "Endpoints for managing news articles")
public class ArticlesController {

    private final ArticlesService articlesService;

    @GetMapping("/summary")
    @ApiResponse(responseCode = "200", description = "Count of new, active, deleted articles")
    public ArticlesSummary getArticlesSummary() {
        return articlesService.getArticlesSummary();
    }

    @PutMapping("/mark-deleted")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "News articles marked as 'deleted' successfully"),
            @ApiResponse(responseCode = "400", ref = "ValidationFailed")
    })
    public ArticlesAffected markAsDeleted(@RequestBody @Valid MarkDeleted dto) {
        return articlesService.markAsDeleted(dto);
    }

    @DeleteMapping("/marked")
    @ApiResponse(responseCode = "200", description = "News articles marked as 'deleted' deleted successfully")
    public ArticlesAffected deleteMarkedArticles() {
        return articlesService.deleteMarkedArticles();
    }

    @DeleteMapping("/all")
    @ApiResponse(responseCode = "200", description = "All news articles deleted successfully")
    public ArticlesAffected deleteAllArticles() {
        return articlesService.deleteAllArticles();
    }

}
