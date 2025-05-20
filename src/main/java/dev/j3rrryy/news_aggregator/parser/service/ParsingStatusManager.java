package dev.j3rrryy.news_aggregator.parser.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ParsingStatusManager {

    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private final AtomicBoolean parsingInProgress = new AtomicBoolean(false);

    public boolean isStopRequested() {
        return stopRequested.get();
    }

    public boolean isParsingInProgress() {
        return parsingInProgress.get();
    }

    public void requestStop() {
        stopRequested.set(true);
    }

    public void resetStopRequest() {
        stopRequested.set(false);
    }

    public boolean startParsing() {
        return parsingInProgress.compareAndSet(false, true);
    }

    public void stopParsing() {
        parsingInProgress.set(false);
    }

}
