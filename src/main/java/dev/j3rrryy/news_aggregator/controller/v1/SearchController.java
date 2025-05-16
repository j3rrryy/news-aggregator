package dev.j3rrryy.news_aggregator.controller.v1;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Endpoints for searching, filtering and viewing news articles")
public class SearchController {

}
