package dev.j3rrryy.news_aggregator.controller.v1;

import dev.j3rrryy.news_aggregator.dto.request.NewsSourceStatusRequestDto;
import dev.j3rrryy.news_aggregator.dto.response.NewsSourceStatusResponseDto;
import dev.j3rrryy.news_aggregator.service.v1.ParserService;
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

    @GetMapping("/source-status")
    public NewsSourceStatusResponseDto getAll() {
        return parserService.listAll();
    }

    @PatchMapping("/source-status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void patchAll(@RequestBody NewsSourceStatusRequestDto sourceStatusDto) {
        parserService.updateAll(sourceStatusDto);
    }

}
