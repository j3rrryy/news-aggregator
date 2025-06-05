package dev.j3rrryy.news_aggregator.parser.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ParsingStatusManagerTest {

    private ParsingStatusManager manager;

    @BeforeEach
    void setUp() {
        manager = new ParsingStatusManager();
    }

    @Test
    void requestStop_shouldSetStopRequestedTrue() {
        assertFalse(manager.isStopRequested());

        manager.requestStop();
        assertTrue(manager.isStopRequested());
    }

    @Test
    void resetStopRequest_shouldSetStopRequestedFalse() {
        manager.requestStop();
        assertTrue(manager.isStopRequested());

        manager.resetStopRequest();
        assertFalse(manager.isStopRequested());
    }

    @Test
    void startParsing_shouldReturnTrueOnFirstCall() {
        assertFalse(manager.isParsingInProgress());
        boolean started = manager.startParsing();

        assertTrue(started);
        assertTrue(manager.isParsingInProgress());
    }

    @Test
    void startParsing_shouldReturnFalseIfAlreadyInProgress() {
        assertTrue(manager.startParsing());
        boolean secondStart = manager.startParsing();

        assertFalse(secondStart);
        assertTrue(manager.isParsingInProgress());
    }

    @Test
    void stopParsing_shouldSetParsingInProgressFalse() {
        manager.startParsing();
        assertTrue(manager.isParsingInProgress());

        manager.stopParsing();
        assertFalse(manager.isParsingInProgress());
    }

}
