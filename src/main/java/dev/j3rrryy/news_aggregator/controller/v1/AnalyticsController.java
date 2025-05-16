package dev.j3rrryy.news_aggregator.controller.v1;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/analytics")
@Tag(name = "Analytics", description = "Endpoints for retrieving statistics and trending topics from the news articles")
public class AnalyticsController {

}
