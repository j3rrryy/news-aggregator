package dev.j3rrryy.news_aggregator.controller.v1;

import dev.j3rrryy.news_aggregator.dto.response.*;
import dev.j3rrryy.news_aggregator.service.v1.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalyticsController.class)
public class AnalyticsControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyticsService analyticsService;

    @Test
    void getCategoryCounts() throws Exception {
        CategoryCounts response = new CategoryCounts(0, 1, 2, 3, 4);
        given(analyticsService.getCategoryCounts()).willReturn(response);

        mockMvc.perform(get("/v1/analytics/categories")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.politics").value(0))
                .andExpect(jsonPath("$.economics").value(1))
                .andExpect(jsonPath("$.society").value(2))
                .andExpect(jsonPath("$.sport").value(3))
                .andExpect(jsonPath("$.scienceTech").value(4));
    }

    @Test
    void getTopFrequentKeywords() throws Exception {
        List<KeywordFrequency> response = List.of(
                new KeywordFrequency("java", 6),
                new KeywordFrequency("spring", 12)
        );
        given(analyticsService.getTopFrequentKeywords(10)).willReturn(response);

        mockMvc.perform(get("/v1/analytics/keywords/top")
                        .param("limit", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].keyword").value("java"))
                .andExpect(jsonPath("$[0].frequency").value(6))
                .andExpect(jsonPath("$[1].keyword").value("spring"))
                .andExpect(jsonPath("$[1].frequency").value(12));
    }

    @Test
    void getTopFrequentKeywords_invalidLimit_zero() throws Exception {
        mockMvc.perform(get("/v1/analytics/keywords/top")
                        .param("limit", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.limit")
                        .value("Limit must be > 0"));
    }

    @Test
    void getTopFrequentKeywords_invalidLimit_negative() throws Exception {
        mockMvc.perform(get("/v1/analytics/keywords/top")
                        .param("limit", "-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.limit")
                        .value("Limit must be > 0"));
    }

    @Test
    void getKeywordTrend() throws Exception {
        List<KeywordDateCount> response = List.of(
                new KeywordDateCount(LocalDate.of(2025, 5, 1), 5),
                new KeywordDateCount(LocalDate.of(2025, 2, 1), 7)
        );
        given(analyticsService.getKeywordTrend("java")).willReturn(response);

        mockMvc.perform(get("/v1/analytics/keywords/trend/java")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].date").value("2025-05-01"))
                .andExpect(jsonPath("$[0].count").value(5))
                .andExpect(jsonPath("$[1].date").value("2025-02-01"))
                .andExpect(jsonPath("$[1].count").value(7));
    }

    @Test
    void getTrendingTopics() throws Exception {
        List<TrendingTopic> response = List.of(
                new TrendingTopic("java", 10, 3, 7),
                new TrendingTopic("spring", 8, 5, 3)
        );
        LocalDateTime from = LocalDateTime.of(2025, 5, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2025, 5, 7, 0, 0);
        given(analyticsService.getTrendingTopics(from, to, 10)).willReturn(response);

        mockMvc.perform(get("/v1/analytics/keywords/trending")
                        .param("fromDate", "2025-05-01T00:00:00")
                        .param("toDate", "2025-05-07T00:00:00")
                        .param("limit", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].keyword").value("java"))
                .andExpect(jsonPath("$[0].currentCount").value(10))
                .andExpect(jsonPath("$[0].previousCount").value(3))
                .andExpect(jsonPath("$[0].delta").value(7))
                .andExpect(jsonPath("$[1].keyword").value("spring"))
                .andExpect(jsonPath("$[1].currentCount").value(8))
                .andExpect(jsonPath("$[1].previousCount").value(5))
                .andExpect(jsonPath("$[1].delta").value(3));
    }

    @Test
    void getTrendingTopics_invalidFromDate_future() throws Exception {
        mockMvc.perform(get("/v1/analytics/keywords/trending")
                        .param("fromDate", LocalDateTime.now().plusDays(1).toString())
                        .param("toDate", "2025-05-07T00:00:00")
                        .param("limit", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.fromDate")
                        .value("'fromDate' timestamp must be in the past"));
    }

    @Test
    void getTrendingTopics_invalidToDate_future() throws Exception {
        mockMvc.perform(get("/v1/analytics/keywords/trending")
                        .param("fromDate", "2025-05-01T00:00:00")
                        .param("toDate", LocalDateTime.now().plusDays(1).toString())
                        .param("limit", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.toDate")
                        .value("'toDate' timestamp must be in the past or present"));
    }

    @Test
    void getTrendingTopics_invalidLimit_zero() throws Exception {
        mockMvc.perform(get("/v1/analytics/keywords/trending")
                        .param("fromDate", "2025-05-01T00:00:00")
                        .param("toDate", "2025-05-07T00:00:00")
                        .param("limit", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.limit")
                        .value("Limit must be > 0"));
    }

    @Test
    void getTrendingTopics_invalidLimit_negative() throws Exception {
        mockMvc.perform(get("/v1/analytics/keywords/trending")
                        .param("fromDate", "2025-05-01T00:00:00")
                        .param("toDate", "2025-05-07T00:00:00")
                        .param("limit", "-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.limit")
                        .value("Limit must be > 0"));
    }

}
