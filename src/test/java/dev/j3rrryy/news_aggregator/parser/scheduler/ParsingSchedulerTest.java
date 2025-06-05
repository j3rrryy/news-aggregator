package dev.j3rrryy.news_aggregator.parser.scheduler;

import dev.j3rrryy.news_aggregator.exceptions.ParsingInProgressException;
import dev.j3rrryy.news_aggregator.parser.config.ParserProperties;
import dev.j3rrryy.news_aggregator.service.v1.ParserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParsingSchedulerTest {

    @Mock
    ParserService parserService;

    @Mock
    TaskScheduler taskScheduler;

    @Mock
    ScheduledFuture<?> scheduledFuture;

    @Spy
    ParserProperties parserProperties = new ParserProperties();

    @InjectMocks
    ParsingScheduler parsingScheduler;

    @Test
    void init_shouldEnableAutoParsing_whenEnabledInProperties() {
        parserProperties.setAutoParsingEnabled(true);
        parserProperties.setAutoParsingInterval(Duration.ofSeconds(10));

        doReturn(scheduledFuture)
                .when(taskScheduler).scheduleWithFixedDelay(any(Runnable.class), any(Duration.class));

        parsingScheduler.init();
        verify(taskScheduler).scheduleWithFixedDelay(any(Runnable.class), eq(Duration.ofSeconds(10)));
    }

    @Test
    void init_shouldNotEnableAutoParsing_whenDisabledInProperties() {
        parserProperties.setAutoParsingEnabled(false);
        parsingScheduler.init();
        verifyNoInteractions(taskScheduler);
    }

    @Test
    void shutdown_shouldDisableAutoParsing() {
        parsingScheduler.scheduledFuture = scheduledFuture;

        parsingScheduler.shutdown();
        verify(scheduledFuture).cancel(false);
        assertFalse(parserProperties.isAutoParsingEnabled());
    }

    @Test
    void enableAutoParsing_shouldDoNothing_whenAlreadyScheduled() {
        when(scheduledFuture.isCancelled()).thenReturn(false);
        parserProperties.setAutoParsingInterval(Duration.ofSeconds(10));
        parsingScheduler.scheduledFuture = scheduledFuture;

        parsingScheduler.enableAutoParsing();
        verify(taskScheduler, never()).scheduleWithFixedDelay(any(), any());
    }

    @Test
    void enableAutoParsing_shouldSchedule_whenScheduledFutureCancelled() {
        parserProperties.setAutoParsingInterval(Duration.ofSeconds(10));
        parserProperties.setAutoParsingEnabled(false);
        parsingScheduler.scheduledFuture = scheduledFuture;

        when(scheduledFuture.isCancelled()).thenReturn(true);
        doReturn(scheduledFuture)
                .when(taskScheduler).scheduleWithFixedDelay(any(Runnable.class), any(Duration.class));

        parsingScheduler.enableAutoParsing();
        verify(taskScheduler).scheduleWithFixedDelay(any(Runnable.class), eq(Duration.ofSeconds(10)));
        assertTrue(parserProperties.isAutoParsingEnabled());
    }

    @Test
    void enableAutoParsing_shouldNotSchedule_whenIntervalIsNull() {
        parserProperties.setAutoParsingEnabled(true);
        parserProperties.setAutoParsingInterval(null);

        parsingScheduler.enableAutoParsing();
        verify(taskScheduler, never()).scheduleWithFixedDelay(any(), any());
    }

    @Test
    void enableAutoParsing_shouldLogWarning_whenIntervalIsZero() {
        parserProperties.setAutoParsingEnabled(true);
        parserProperties.setAutoParsingInterval(Duration.ZERO);

        parsingScheduler.enableAutoParsing();
        verify(taskScheduler, never()).scheduleWithFixedDelay(any(), any());
    }

    @Test
    void enableAutoParsing_shouldNotSchedule_whenIntervalIsNegative() {
        parserProperties.setAutoParsingInterval(Duration.ofSeconds(-10));
        parserProperties.setAutoParsingEnabled(true);

        parsingScheduler.enableAutoParsing();
        verify(taskScheduler, never()).scheduleWithFixedDelay(any(), any());
    }

    @Test
    void disableAutoParsing_shouldCancelTask() {
        parsingScheduler.scheduledFuture = scheduledFuture;

        parsingScheduler.disableAutoParsing();
        verify(scheduledFuture).cancel(false);
        assertFalse(parserProperties.isAutoParsingEnabled());
    }

    @Test
    void disableAutoParsing_shouldDoNothing_whenScheduledFutureIsNull() {
        parsingScheduler.scheduledFuture = null;
        parsingScheduler.disableAutoParsing();
        verifyNoInteractions(taskScheduler);
    }

    @Test
    void autoParsing_shouldCallStartParsing() {
        parsingScheduler = new ParsingScheduler(parserService, taskScheduler, parserProperties);
        parsingScheduler.autoParsing();
        verify(parserService).startParsing();
    }

    @Test
    void autoParsing_shouldLogWarning_whenParsingInProgress() {
        doThrow(new ParsingInProgressException()).when(parserService).startParsing();
        parsingScheduler.autoParsing();
        verify(parserService).startParsing();
    }

    @Test
    void enableAutoParsing_shouldScheduleParsing_whenValid() {
        parserProperties.setAutoParsingEnabled(false);
        parserProperties.setAutoParsingInterval(Duration.ofMinutes(1));

        doReturn(scheduledFuture)
                .when(taskScheduler).scheduleWithFixedDelay(any(Runnable.class), any(Duration.class));

        parsingScheduler.enableAutoParsing();
        verify(taskScheduler).scheduleWithFixedDelay(any(Runnable.class), eq(Duration.ofMinutes(1)));
        assertTrue(parserProperties.isAutoParsingEnabled());
    }

}
