package dev.j3rrryy.news_aggregator.parser.service;

import dev.j3rrryy.news_aggregator.enums.Category;
import dev.j3rrryy.news_aggregator.enums.Source;
import dev.j3rrryy.news_aggregator.parser.NewsParser;
import dev.j3rrryy.news_aggregator.parser.config.ParserProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParsingOrchestrator {

    private final List<NewsParser> parsers;
    private final ParsingService parsingService;
    private final ParserProperties parserProperties;
    private final ParsingStatusManager parsingStatusManager;

    @Async
    public void runAsyncParsing() {
        try {
            parsingService.prepareForParsing();
            Map<Source, Map<Category, LocalDateTime>> latestPublishedAt =
                    parsingService.getLatestPublishedAtByCategoryAndSource();
            Map<Source, Boolean> sourceStatuses = parserProperties.getSourceStatuses();

            for (NewsParser parser : parsers) {
                Source source = parser.getSource();
                if (!parsingStatusManager.isStopRequested() && sourceStatuses.get(source)) {
                    log.info("Parsing news from {}...", source);
                    parser.parse(latestPublishedAt.get(source));
                    log.info("Parsing from {} completed", source);
                }
            }
        } finally {
            parsingStatusManager.stopParsing();
            parsingStatusManager.resetStopRequest();
        }
    }

}
