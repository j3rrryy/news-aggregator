package dev.j3rrryy.news_aggregator.parser.service;

import dev.j3rrryy.news_aggregator.enums.Category;
import dev.j3rrryy.news_aggregator.enums.Source;
import dev.j3rrryy.news_aggregator.parser.NewsParser;
import dev.j3rrryy.news_aggregator.parser.config.ParserProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

public class ParsingOrchestratorTest {

    @Mock
    private NewsParser parser1;

    @Mock
    private NewsParser parser2;

    @Mock
    private ParsingService parsingService;

    @InjectMocks
    private ParsingOrchestrator orchestrator;

    @Mock
    private ParserProperties parserProperties;

    @Mock
    private ParsingStatusManager parsingStatusManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void runAsyncParsing_shouldRunParsersWhenNotStoppedAndEnabled() {
        Map<Source, Map<Category, LocalDateTime>> latestPublishedAt = new EnumMap<>(Source.class);
        EnumMap<Category, LocalDateTime> categoryMap = new EnumMap<>(Category.class);
        categoryMap.put(Category.ECONOMICS, LocalDateTime.now());
        latestPublishedAt.put(Source.SVPRESSA_RU, categoryMap);

        when(parsingService.getLatestPublishedAtByCategoryAndSource()).thenReturn(latestPublishedAt);
        when(parserProperties.getSourceStatuses()).thenReturn(
                Map.of(Source.SVPRESSA_RU, true, Source.AIF_RU, false)
        );
        when(parsingStatusManager.isStopRequested()).thenReturn(false);
        when(parser1.getSource()).thenReturn(Source.SVPRESSA_RU);
        when(parser2.getSource()).thenReturn(Source.AIF_RU);

        List<NewsParser> parsers = List.of(parser1, parser2);

        orchestrator = new ParsingOrchestrator(parsers, parsingService, parserProperties, parsingStatusManager);
        orchestrator.runAsyncParsing();

        verify(parsingService).prepareForParsing();
        verify(parsingService).getLatestPublishedAtByCategoryAndSource();
        verify(parser1).parse(latestPublishedAt.get(Source.SVPRESSA_RU));
        verify(parser2, never()).parse(any());
        verify(parsingStatusManager).stopParsing();
        verify(parsingStatusManager).resetStopRequest();
    }

    @Test
    void runAsyncParsing_shouldNotRunParsersWhenStopRequested() {
        when(parsingStatusManager.isStopRequested()).thenReturn(true);
        when(parserProperties.getSourceStatuses()).thenReturn(Map.of(Source.SVPRESSA_RU, true));
        when(parser1.getSource()).thenReturn(Source.SVPRESSA_RU);

        orchestrator = new ParsingOrchestrator(List.of(parser1), parsingService, parserProperties, parsingStatusManager);
        orchestrator.runAsyncParsing();

        verify(parsingService).prepareForParsing();
        verify(parsingService).getLatestPublishedAtByCategoryAndSource();
        verify(parser1, never()).parse(any());
        verify(parsingStatusManager).stopParsing();
        verify(parsingStatusManager).resetStopRequest();
    }

}
