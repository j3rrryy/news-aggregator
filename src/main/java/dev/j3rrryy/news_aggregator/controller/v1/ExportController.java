package dev.j3rrryy.news_aggregator.controller.v1;

import dev.j3rrryy.news_aggregator.enums.*;
import dev.j3rrryy.news_aggregator.service.v1.ExportService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/export")
@Tag(name = "Export", description = "Endpoints for exporting news articles in CSV, JSON and HTML formats")
public class ExportController {

    private static final Map<FileFormat, MediaType> contentTypeMap = Map.of(
            FileFormat.CSV, MediaType.valueOf("text/csv"),
            FileFormat.JSON, MediaType.APPLICATION_JSON,
            FileFormat.HTML, MediaType.TEXT_HTML
    );

    private final ExportService exportService;

    @GetMapping(produces = "*/*")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File with news articles"),
            @ApiResponse(responseCode = "400", ref = "ValidationFailed")
    })
    public ResponseEntity<StreamingResponseBody> export(
            @RequestParam(required = false)
            String query,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Past(message = "'fromDate' timestamp must be in the past")
            LocalDateTime fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @PastOrPresent(message = "'toDate' timestamp must be in the past or present")
            LocalDateTime toDate,

            @RequestParam(required = false)
            Set<Category> category,

            @RequestParam(required = false)
            Set<Source> source,

            @RequestParam(required = false)
            Set<Status> status,

            @RequestParam(required = false)
            Set<String> keywords,

            @RequestParam(required = false)
            SortField sortField,

            @RequestParam(required = false)
            SortDirection sortDirection,

            @RequestParam(defaultValue = "false")
            boolean includeContent,

            FileFormat fileFormat
    ) {
        StreamingResponseBody body = outputStream -> exportService.export(
                query,
                fromDate,
                toDate,
                category,
                source,
                status,
                keywords,
                sortField,
                sortDirection,
                outputStream,
                includeContent,
                fileFormat
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=news." + fileFormat.name().toLowerCase())
                .contentType(contentTypeMap.get(fileFormat))
                .body(body);
    }

}
