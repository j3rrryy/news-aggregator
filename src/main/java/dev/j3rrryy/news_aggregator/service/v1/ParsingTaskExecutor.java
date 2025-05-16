package dev.j3rrryy.news_aggregator.service.v1;

import dev.j3rrryy.news_aggregator.config.ParserProperties;
import dev.j3rrryy.news_aggregator.enums.Category;
import dev.j3rrryy.news_aggregator.enums.Source;
import dev.j3rrryy.news_aggregator.parser.AifRuParser;
import dev.j3rrryy.news_aggregator.parser.RtRuParser;
import dev.j3rrryy.news_aggregator.parser.SvpressaRuParser;
import dev.j3rrryy.news_aggregator.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class ParsingTaskExecutor {

    private final RtRuParser rtRuParser;
    private final AifRuParser aifRuParser;
    private final SvpressaRuParser svpressaRuParser;
    private final ParserProperties parserProperties;
    private final NewsArticleRepository newsArticleRepository;

    @Async
    public void asyncParsingTask(AtomicBoolean parsingInProgress) {
        try {
            newsArticleRepository.updateAllNewToActive();
            Map<Source, Map<Category, LocalDateTime>> publishedAt = getLatestPublishedAtByCategoryAndSource();
            Map<Source, Boolean> sourceStatuses = parserProperties.getSourceStatuses();

            if (sourceStatuses.get(Source.RT_RU)) {
                rtRuParser.parse(publishedAt.get(Source.RT_RU));
            }
            if (sourceStatuses.get(Source.AIF_RU)) {
                aifRuParser.parse(publishedAt.get(Source.AIF_RU));
            }
            if (sourceStatuses.get(Source.SVPRESSA_RU)) {
                svpressaRuParser.parse(publishedAt.get(Source.SVPRESSA_RU));
            }
        } finally {
            parsingInProgress.set(false);
        }
    }

    private Map<Source, Map<Category, LocalDateTime>> getLatestPublishedAtByCategoryAndSource() {
        List<Object[]> query = newsArticleRepository.findLatestPublishedAtByCategoryAndSource();
        Map<Source, Map<Category, LocalDateTime>> result = new HashMap<>();

        for (Source source : Source.values()) {
            Map<Category, LocalDateTime> categoryMap = new HashMap<>();
            for (Category category : Category.values()) {
                Optional<Object[]> matchingRow = query.stream()
                        .filter(res -> res[0].equals(source.name()) && res[1].equals(category.name()))
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
