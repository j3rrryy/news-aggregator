package dev.j3rrryy.news_aggregator.service.v1;

import dev.j3rrryy.news_aggregator.dto.request.MarkDeletedDto;
import dev.j3rrryy.news_aggregator.dto.response.ArticlesAffectedDto;
import dev.j3rrryy.news_aggregator.enums.Category;
import dev.j3rrryy.news_aggregator.enums.Source;
import dev.j3rrryy.news_aggregator.exceptions.ParsingInProgressException;
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
public class NewsService {

    private static final AtomicBoolean isParsing = new AtomicBoolean(false);

    private final RtRuParser rtRuParser;
    private final AifRuParser aifRuParser;
    private final ParserService parserService;
    private final SvpressaRuParser svpressaRuParser;
    private final NewsArticleRepository newsArticleRepository;

    @Async
    public void startParsingAsync() {
        if (!isParsing.compareAndSet(false, true)) {
            throw new ParsingInProgressException();
        }

        try {
            newsArticleRepository.updateAllNewToActive();
            Map<Source, Map<Category, LocalDateTime>> publishedAt = getLatestPublishedAtByCategoryAndSource();

            if (parserService.getSourceStatuses().rtRu()) {
                rtRuParser.parse(publishedAt.get(Source.RT_RU));
            }
            if (parserService.getSourceStatuses().aifRu()) {
                aifRuParser.parse(publishedAt.get(Source.AIF_RU));
            }
            if (parserService.getSourceStatuses().svpressaRu()) {
                svpressaRuParser.parse(publishedAt.get(Source.SVPRESSA_RU));
            }
        } finally {
            isParsing.set(false);
        }
    }

    public ArticlesAffectedDto markAsDeleted(MarkDeletedDto markDeletedDto) {
        return new ArticlesAffectedDto(
                newsArticleRepository.markAsDeletedByPublishedAtBefore(markDeletedDto.olderThan())
        );
    }

    public ArticlesAffectedDto deleteMarkedArticles() {
        return new ArticlesAffectedDto(newsArticleRepository.deleteAllMarkedAsDeleted());
    }

    public ArticlesAffectedDto deleteAllArticles() {
        return new ArticlesAffectedDto(newsArticleRepository.deleteAllArticles());
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
