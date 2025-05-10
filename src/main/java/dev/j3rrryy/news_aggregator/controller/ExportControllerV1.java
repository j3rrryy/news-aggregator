package dev.j3rrryy.news_aggregator.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/export")
@Tag(name = "Export", description = "Export news")
public class ExportControllerV1 {

}
