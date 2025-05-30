package dev.j3rrryy.news_aggregator.controller.v1;

import dev.j3rrryy.news_aggregator.dto.request.AutoParsingInterval;
import dev.j3rrryy.news_aggregator.dto.request.NewsSourceStatusesRequest;
import dev.j3rrryy.news_aggregator.dto.response.AutoParsingStatus;
import dev.j3rrryy.news_aggregator.dto.response.NewsSourceStatusesResponse;
import dev.j3rrryy.news_aggregator.dto.response.ParsingStatus;
import dev.j3rrryy.news_aggregator.parser.scheduler.ParsingScheduler;
import dev.j3rrryy.news_aggregator.service.v1.ParserService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/parser")
@Tag(name = "Parser", description = "Endpoints for parser configuration and monitoring")
public class ParserController {

    private final ParserService parserService;
    private final ParsingScheduler parsingScheduler;

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Parsing started successfully"),
            @ApiResponse(responseCode = "409", description = "Parsing is already in progress")
    })
    public void startParsing() {
        parserService.startParsing();
    }

    @PostMapping("/stop")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Parsing stopping..."),
            @ApiResponse(responseCode = "409", description = "Parsing is not running")
    })
    public void stopParsing() {
        parserService.stopParsing();
    }

    @GetMapping("/status")
    @ApiResponse(responseCode = "200", description = "Current status of parsing process")
    public ParsingStatus getParsingStatus() {
        return parserService.getParsingStatus();
    }

    @GetMapping("/sources/statuses")
    @ApiResponse(responseCode = "200", description = "Current statuses of news sources")
    public NewsSourceStatusesResponse getSourceStatuses() {
        return parserService.getSourceStatuses();
    }

    @PatchMapping("/sources/statuses")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Source statuses updated successfully"),
            @ApiResponse(responseCode = "400", ref = "ValidationFailed")
    })
    public void patchSourceStatuses(@RequestBody NewsSourceStatusesRequest dto) {
        parserService.patchSourceStatuses(dto);
    }

    @GetMapping("/auto-parsing/status")
    @ApiResponse(responseCode = "200", description = "Current status of auto-parsing")
    public AutoParsingStatus getAutoParsingStatus() {
        return parserService.getAutoParsingStatus();
    }

    @PatchMapping("/auto-parsing/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiResponse(responseCode = "204", description = "Auto-parsing enabled successfully")
    public void enableAutoParsing() {
        parsingScheduler.enableAutoParsing();
    }

    @PatchMapping("/auto-parsing/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiResponse(responseCode = "204", description = "Auto-parsing disabled successfully")
    public void disableAutoParsing() {
        parsingScheduler.disableAutoParsing();
    }

    @PatchMapping("/auto-parsing/interval")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Interval updated successfully"),
            @ApiResponse(responseCode = "400", ref = "ValidationFailed")
    })
    public void setAutoParsingInterval(@RequestBody @Valid AutoParsingInterval dto) {
        parserService.setAutoParsingInterval(dto);
    }

}
