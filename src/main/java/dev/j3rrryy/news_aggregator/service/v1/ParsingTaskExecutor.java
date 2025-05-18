package dev.j3rrryy.news_aggregator.service.v1;

import dev.j3rrryy.news_aggregator.config.ParserProperties;
import dev.j3rrryy.news_aggregator.enums.Category;
import dev.j3rrryy.news_aggregator.enums.Source;
import dev.j3rrryy.news_aggregator.parser.*;
import dev.j3rrryy.news_aggregator.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParsingTaskExecutor {

    private static final AtomicBoolean stopRequested = new AtomicBoolean(false);

    private final RtRuParser rtRuParser;
    private final AifRuParser aifRuParser;
    private final SvpressaRuParser svpressaRuParser;
    private final ParserProperties parserProperties;
    private final NewsArticleRepository newsArticleRepository;

    @Async
    @Transactional
    public void asyncParsingTask(AtomicBoolean parsingInProgress) {
        try {
            Map<Source, NewsParser> parserMap = Map.of(
                    Source.RT_RU, rtRuParser,
                    Source.AIF_RU, aifRuParser,
                    Source.SVPRESSA_RU, svpressaRuParser
            );

            newsArticleRepository.updateAllNewToActive();
            Map<Source, Map<Category, LocalDateTime>> publishedAt = getLatestPublishedAtByCategoryAndSource();
            Map<Source, Boolean> sourceStatuses = parserProperties.getSourceStatuses();

            for (Source source : Source.values()) {
                if (!stopRequested.get() && sourceStatuses.get(source)) {
                    log.info("Parsing news from {}...", source);
                    parserMap.get(source).parse(publishedAt.get(source), stopRequested);
                    log.info("Parsing from {} completed", source);
                }
            }
        } finally {
            stopRequested.set(false);
            parsingInProgress.set(false);
        }
    }

    public void requestStop() {
        stopRequested.set(true);
    }

    private Map<Source, Map<Category, LocalDateTime>> getLatestPublishedAtByCategoryAndSource() {
        List<Object[]> query = newsArticleRepository.findLatestPublishedAtByCategoryAndSource();
        Map<Source, Map<Category, LocalDateTime>> result = new HashMap<>();

        for (Source source : Source.values()) {
            Map<Category, LocalDateTime> categoryMap = new HashMap<>();
            for (Category category : Category.values()) {
                Optional<Object[]> matchingRow = query.stream()
                        .filter(res -> res[0] == source && res[1] == category)
                        .findFirst();

                LocalDateTime latestPublishedAt = matchingRow
                        .map(res -> (LocalDateTime) res[2])
                        .orElse(null);

                categoryMap.put(category, latestPublishedAt);
            }
            result.put(source, categoryMap);
        }
        return result;
    }

}
