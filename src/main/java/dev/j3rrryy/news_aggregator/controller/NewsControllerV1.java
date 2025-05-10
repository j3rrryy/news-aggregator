package dev.j3rrryy.news_aggregator.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/news")
@Tag(name = "News", description = "Manage news")
public class NewsControllerV1 {

}
