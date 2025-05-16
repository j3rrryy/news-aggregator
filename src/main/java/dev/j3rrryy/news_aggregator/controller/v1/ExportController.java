package dev.j3rrryy.news_aggregator.controller.v1;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/export")
@Tag(name = "Export", description = "Endpoints for exporting news data in CSV, JSON or HTML formats")
public class ExportController {

}
