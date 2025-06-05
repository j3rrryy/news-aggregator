package dev.j3rrryy.news_aggregator.parser.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class UserAgentProviderTest {

    private UserAgentProvider userAgentProvider;

    @BeforeEach
    void setUp() {
        userAgentProvider = new UserAgentProvider();
    }

    @Test
    void getNextUserAgent_shouldReturnNonNullString() {
        String userAgent = userAgentProvider.getNextUserAgent();
        assertNotNull(userAgent);
        assertFalse(userAgent.isBlank());
    }

    @Test
    void getNextUserAgent_shouldCycleThroughUserAgents() {
        Set<String> seen = new HashSet<>();
        int total = 10;

        for (int i = 0; i < total; i++) {
            String agent = userAgentProvider.getNextUserAgent();
            assertNotNull(agent);
            assertTrue(UserAgentProvider.userAgents.contains(agent));
            seen.add(agent);
        }

        assertEquals(total, seen.size());
    }

    @Test
    void getNextUserAgent_shouldWrapAroundAfterEnd() {
        for (int i = 1; i < UserAgentProvider.userAgents.size(); i++) {
            userAgentProvider.getNextUserAgent();
        }

        String firstAgain = userAgentProvider.getNextUserAgent();
        assertEquals(UserAgentProvider.userAgents.getFirst(), firstAgain);
    }

}
