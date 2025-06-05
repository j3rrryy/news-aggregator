package dev.j3rrryy.news_aggregator.mapper;

import dev.j3rrryy.news_aggregator.dto.request.NewsSourceStatusesRequest;
import dev.j3rrryy.news_aggregator.dto.response.NewsSourceStatusesResponse;
import dev.j3rrryy.news_aggregator.enums.Source;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class NewsSourceStatusesMapperTest {

    private NewsSourceStatusesMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new NewsSourceStatusesMapper();
    }

    @Test
    void updateStatusMap_shouldUpdateGivenValuesOnly() {
        Map<Source, Boolean> statusMap = new EnumMap<>(Source.class);

        NewsSourceStatusesRequest request = new NewsSourceStatusesRequest(true, null, false);
        mapper.updateStatusMap(request, statusMap);

        assertEquals(Boolean.TRUE, statusMap.get(Source.RT_RU));
        assertEquals(Boolean.FALSE, statusMap.get(Source.SVPRESSA_RU));
        assertNull(statusMap.get(Source.AIF_RU));
    }

    @Test
    void updateStatusMap_shouldUpdateAllValues() {
        Map<Source, Boolean> statusMap = new EnumMap<>(Source.class);

        NewsSourceStatusesRequest request = new NewsSourceStatusesRequest(true, false, true);
        mapper.updateStatusMap(request, statusMap);

        assertEquals(Boolean.TRUE, statusMap.get(Source.RT_RU));
        assertEquals(Boolean.FALSE, statusMap.get(Source.AIF_RU));
        assertEquals(Boolean.TRUE, statusMap.get(Source.SVPRESSA_RU));
    }

    @Test
    void updateStatusMap_shouldSkipNullValues() {
        Map<Source, Boolean> statusMap = new EnumMap<>(Source.class);
        statusMap.put(Source.RT_RU, false);
        statusMap.put(Source.AIF_RU, true);
        statusMap.put(Source.SVPRESSA_RU, true);

        NewsSourceStatusesRequest request = new NewsSourceStatusesRequest(null, null, null);
        mapper.updateStatusMap(request, statusMap);

        assertEquals(Boolean.FALSE, statusMap.get(Source.RT_RU));
        assertEquals(Boolean.TRUE, statusMap.get(Source.AIF_RU));
        assertEquals(Boolean.TRUE, statusMap.get(Source.SVPRESSA_RU));
    }

    @Test
    void toResponseDto_shouldReturnResponseWithDefaults() {
        Map<Source, Boolean> statusMap = new EnumMap<>(Source.class);
        statusMap.put(Source.RT_RU, true);
        statusMap.put(Source.SVPRESSA_RU, false);

        NewsSourceStatusesResponse response = mapper.toResponseDto(statusMap);

        assertTrue(response.rtRu());
        assertFalse(response.aifRu());
        assertFalse(response.svpressaRu());
    }

    @Test
    void toResponseDto_shouldDefaultAllToFalseWhenEmpty() {
        Map<Source, Boolean> statusMap = new EnumMap<>(Source.class);
        NewsSourceStatusesResponse response = mapper.toResponseDto(statusMap);

        assertFalse(response.rtRu());
        assertFalse(response.aifRu());
        assertFalse(response.svpressaRu());
    }

}
