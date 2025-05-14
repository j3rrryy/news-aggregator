package dev.j3rrryy.news_aggregator.scheduler;

import dev.j3rrryy.news_aggregator.config.ParserProperties;
import dev.j3rrryy.news_aggregator.exceptions.ParsingInProgressException;
import dev.j3rrryy.news_aggregator.service.v1.NewsService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParsingScheduler {

    private final NewsService newsService;
    private final TaskScheduler taskScheduler;
    private final ParserProperties parserProperties;
    private volatile ScheduledFuture<?> scheduledFuture;

    @PostConstruct
    public void init() {
        if (parserProperties.isAutoParsingEnabled()) enableAutoParsing();
    }

    @PreDestroy
    public void shutdown() {
        disableAutoParsing();
    }

    public void enableAutoParsing() {
        if (scheduledFuture != null && !scheduledFuture.isCancelled()) return;

        Duration interval = parserProperties.getAutoParsingInterval();
        if (interval == null || interval.isZero() || interval.isNegative()) {
            log.warn("Auto-parsing interval is not set or <= 0 ({}), could not enable the feature", interval);
            return;
        }

        scheduledFuture = taskScheduler.scheduleWithFixedDelay(this::autoParsing, interval);
        parserProperties.setAutoParsingEnabled(true);
        log.info("Auto-parsing every {} is enabled", interval);
    }

    public void disableAutoParsing() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            log.info("Auto-parsing is disabled");
        }
        parserProperties.setAutoParsingEnabled(false);
    }

    private void autoParsing() {
        try {
            newsService.startParsing();
        } catch (ParsingInProgressException e) {
            log.warn(e.getMessage());
        }
    }

}
