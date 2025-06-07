package dev.j3rrryy.news_aggregator.service.v1;

import dev.j3rrryy.news_aggregator.dto.request.AutoParsingInterval;
import dev.j3rrryy.news_aggregator.dto.request.NewsSourceStatusesRequest;
import dev.j3rrryy.news_aggregator.dto.response.AutoParsingStatus;
import dev.j3rrryy.news_aggregator.dto.response.NewsSourceStatusesResponse;
import dev.j3rrryy.news_aggregator.dto.response.ParsingStatus;
import dev.j3rrryy.news_aggregator.enums.Source;
import dev.j3rrryy.news_aggregator.exceptions.ParsingInProgressException;
import dev.j3rrryy.news_aggregator.exceptions.ParsingNotRunningException;
import dev.j3rrryy.news_aggregator.mapper.NewsSourceStatusesMapper;
import dev.j3rrryy.news_aggregator.parser.config.ParserProperties;
import dev.j3rrryy.news_aggregator.parser.service.ParsingOrchestrator;
import dev.j3rrryy.news_aggregator.parser.service.ParsingStatusManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParserServiceTest {

    @InjectMocks
    private ParserService parserService;

    @Mock
    private ParserProperties parserProperties;

    @Mock
    private ParsingOrchestrator parsingOrchestrator;

    @Mock
    private ParsingStatusManager parsingStatusManager;

    @Mock
    private NewsSourceStatusesMapper newsSourceStatusesMapper;

    @Test
    void startParsing_shouldRunParsingIfNotInProgress() {
        when(parsingStatusManager.startParsing()).thenReturn(true);
        parserService.startParsing();
        verify(parsingOrchestrator).runAsyncParsing();
    }

    @Test
    void startParsing_shouldThrowExceptionIfAlreadyInProgress() {
        when(parsingStatusManager.startParsing()).thenReturn(false);
        assertThrows(ParsingInProgressException.class, () -> parserService.startParsing());
        verify(parsingOrchestrator, never()).runAsyncParsing();
    }

    @Test
    void stopParsing_shouldRequestStopIfInProgress() {
        when(parsingStatusManager.isParsingInProgress()).thenReturn(true);
        parserService.stopParsing();
        verify(parsingStatusManager).requestStop();
    }

    @Test
    void stopParsing_shouldThrowExceptionIfNotInProgress() {
        when(parsingStatusManager.isParsingInProgress()).thenReturn(false);
        assertThrows(ParsingNotRunningException.class, () -> parserService.stopParsing());
        verify(parsingStatusManager, never()).requestStop();
    }

    @Test
    void getParsingStatus_shouldReturnCorrectStatus() {
        when(parsingStatusManager.isParsingInProgress()).thenReturn(true);
        ParsingStatus status = parserService.getParsingStatus();
        assertThat(status.inProgress()).isTrue();
    }

    @Test
    void getSourceStatuses_shouldReturnMappedStatuses() {
        Map<Source, Boolean> statuses = Map.of(Source.SVPRESSA_RU, true);
        NewsSourceStatusesResponse response = new NewsSourceStatusesResponse(false, true, true);

        when(parserProperties.getSourceStatuses()).thenReturn(statuses);
        when(newsSourceStatusesMapper.toResponseDto(statuses)).thenReturn(response);

        NewsSourceStatusesResponse result = parserService.getSourceStatuses();

        assertThat(result).isEqualTo(response);
    }

    @Test
    void patchSourceStatuses_shouldUpdateStatusesAndSetBack() {
        Map<Source, Boolean> currentStatuses = Map.of(Source.SVPRESSA_RU, true);
        NewsSourceStatusesRequest dto = new NewsSourceStatusesRequest(false, null, true);

        when(parserProperties.getSourceStatuses()).thenReturn(currentStatuses);

        parserService.patchSourceStatuses(dto);

        verify(newsSourceStatusesMapper).updateStatusMap(dto, currentStatuses);
        verify(parserProperties).setSourceStatuses(currentStatuses);
    }

    @Test
    void getAutoParsingStatus_shouldReturnCorrectDto() {
        Duration interval = Duration.of(5, ChronoUnit.HOURS);
        when(parserProperties.isAutoParsingEnabled()).thenReturn(true);
        when(parserProperties.getAutoParsingInterval()).thenReturn(interval);

        AutoParsingStatus result = parserService.getAutoParsingStatus();

        assertThat(result.autoParsingEnabled()).isTrue();
        assertThat(result.autoParsingInterval()).isEqualTo(interval);
    }

    @Test
    void setAutoParsingInterval_shouldUpdateConfig() {
        Duration interval = Duration.of(5, ChronoUnit.HOURS);
        AutoParsingInterval dto = new AutoParsingInterval(interval);

        parserService.setAutoParsingInterval(dto);

        verify(parserProperties).setAutoParsingInterval(interval);
    }

}
