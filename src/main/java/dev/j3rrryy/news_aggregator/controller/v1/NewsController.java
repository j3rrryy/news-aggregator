package dev.j3rrryy.news_aggregator.controller.v1;

import dev.j3rrryy.news_aggregator.service.v1.NewsService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Parsing started successfully"),
            @ApiResponse(responseCode = "409", description = "Parsing is already in progress")
    })
    public void startParsing() {
        newsService.startParsing();
    }

}
