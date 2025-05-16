package dev.j3rrryy.news_aggregator.controller.v1;

import dev.j3rrryy.news_aggregator.dto.request.AutoParsingIntervalDto;
import dev.j3rrryy.news_aggregator.dto.request.NewsSourceStatusesRequestDto;
import dev.j3rrryy.news_aggregator.dto.response.AutoParsingStatusDto;
import dev.j3rrryy.news_aggregator.dto.response.NewsSourceStatusesResponseDto;
import dev.j3rrryy.news_aggregator.dto.response.ParsingStatusDto;
import dev.j3rrryy.news_aggregator.scheduler.ParsingScheduler;
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
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Parsing started successfully"),
            @ApiResponse(responseCode = "409", description = "Parsing is already in progress")
    })
    public void startParsing() {
        parserService.startParsing();
    }

    @GetMapping("/status")
    @ApiResponse(responseCode = "200", description = "Current status of parsing process")
    public ParsingStatusDto getParsingStatus() {
        return parserService.getParsingStatus();
    }

    @GetMapping("/sources/statuses")
    @ApiResponse(responseCode = "200", description = "Current statuses of news sources")
    public NewsSourceStatusesResponseDto getSourceStatuses() {
        return parserService.getSourceStatuses();
    }

    @PatchMapping("/sources/statuses")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Source statuses updated successfully"),
            @ApiResponse(responseCode = "400", ref = "ValidationFailed")
    })
    public void patchSourceStatuses(@RequestBody NewsSourceStatusesRequestDto newsSourceStatusesRequestDto) {
        parserService.patchSourceStatuses(newsSourceStatusesRequestDto);
    }

    @GetMapping("/auto-parsing/status")
    @ApiResponse(responseCode = "200", description = "Current status of auto-parsing")
    public AutoParsingStatusDto getAutoParsingStatus() {
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
    public void setAutoParsingInterval(@RequestBody @Valid AutoParsingIntervalDto autoParsingIntervalDto) {
        parserService.setAutoParsingInterval(autoParsingIntervalDto);
    }

}
