package dev.j3rrryy.news_aggregator.parser.service;

import dev.j3rrryy.news_aggregator.enums.Category;
import dev.j3rrryy.news_aggregator.enums.Source;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class ParsingStateStoreTest {

    private ParsingStateStore stateStore;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock();

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        stateStore = new ParsingStateStore(redisTemplate);
    }

    @Test
    void getCurrentPage_shouldReturnOptionalWithValue_whenKeyExists() {
        String path = "/news";
        String expectedKey = "state:RT_RU:SPORT:%2Fnews";
        when(valueOps.get(expectedKey)).thenReturn("42");

        Optional<Integer> result = stateStore.getCurrentPage(Source.RT_RU, Category.SPORT, path);

        assertTrue(result.isPresent());
        assertEquals(42, result.get());
    }

    @Test
    void getCurrentPage_shouldReturnEmpty_whenKeyDoesNotExist() {
        String path = "/news";
        String expectedKey = "state:AIF_RU:ECONOMICS:%2Fnews";
        when(valueOps.get(expectedKey)).thenReturn(null);

        Optional<Integer> result = stateStore.getCurrentPage(Source.AIF_RU, Category.ECONOMICS, path);

        assertTrue(result.isEmpty());
    }

    @Test
    void updateCurrentPage_shouldSetValueCorrectly() {
        String path = "/news";
        String expectedKey = "state:SVPRESSA_RU:POLITICS:%2Fnews";

        stateStore.updateCurrentPage(Source.SVPRESSA_RU, Category.POLITICS, path, 10);

        verify(valueOps).set(expectedKey, "10");
    }

    @Test
    void clearState_shouldDeleteCorrectKey() {
        String path = "/news";
        String expectedKey = "state:AIF_RU:SOCIETY:%2Fnews";

        stateStore.clearState(Source.AIF_RU, Category.SOCIETY, path);

        verify(redisTemplate).delete(expectedKey);
    }

}
