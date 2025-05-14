package dev.j3rrryy.news_aggregator.controller.v1;

import dev.j3rrryy.news_aggregator.dto.request.AutoParsingIntervalDto;
import dev.j3rrryy.news_aggregator.dto.request.NewsSourceStatusRequestDto;
import dev.j3rrryy.news_aggregator.dto.response.AutoParsingStatusDto;
import dev.j3rrryy.news_aggregator.dto.response.NewsSourceStatusResponseDto;
import dev.j3rrryy.news_aggregator.scheduler.ParsingScheduler;
import dev.j3rrryy.news_aggregator.service.v1.ParserService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/parser")
@Tag(name = "Parser", description = "Configure parser")
public class ParserController {

    private final ParserService parserService;
    private final ParsingScheduler parsingScheduler;

    @GetMapping("/source-status")
    public NewsSourceStatusResponseDto getSourceStatus() {
        return parserService.getSourceStatus();
    }

    @PatchMapping("/source-status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void patchSourceStatus(@RequestBody NewsSourceStatusRequestDto newsSourceStatusRequestDto) {
        parserService.patchSourceStatus(newsSourceStatusRequestDto);
    }

    @GetMapping("/auto-parsing/status")
    public AutoParsingStatusDto getAutoParsingStatus() {
        return parserService.getAutoParsingStatus();
    }

    @PatchMapping("/auto-parsing/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enableAutoParsing() {
        parsingScheduler.enableAutoParsing();
    }

    @PatchMapping("/auto-parsing/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disableAutoParsing() {
        parsingScheduler.disableAutoParsing();
    }

    @PatchMapping("/auto-parsing/interval")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Interval updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid duration format")
    })
    public void setAutoParsingInterval(@RequestBody AutoParsingIntervalDto autoParsingIntervalDto) {
        parserService.setAutoParsingInterval(autoParsingIntervalDto);
    }

}
