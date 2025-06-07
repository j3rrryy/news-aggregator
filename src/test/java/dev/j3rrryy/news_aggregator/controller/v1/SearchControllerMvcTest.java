package dev.j3rrryy.news_aggregator.controller.v1;

import dev.j3rrryy.news_aggregator.dto.response.CursorPage;
import dev.j3rrryy.news_aggregator.dto.response.NewsArticleFull;
import dev.j3rrryy.news_aggregator.dto.response.NewsArticleSummary;
import dev.j3rrryy.news_aggregator.enums.*;
import dev.j3rrryy.news_aggregator.service.v1.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SearchController.class)
public class SearchControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchService searchService;

    @Test
    void searchNews() throws Exception {
        CursorPage response = new CursorPage(
                List.of(
                        new NewsArticleSummary(
                                UUID.randomUUID(),
                                "test title 1",
                                "test summary 1",
                                Category.ECONOMICS,
                                List.of("java"),
                                List.of("test media url 1"),
                                "test url 1",
                                Status.NEW,
                                LocalDateTime.of(2025, 5, 7, 0, 0, 0),
                                Source.RT_RU
                        ),
                        new NewsArticleSummary(
                                UUID.randomUUID(),
                                "test title 2",
                                "test summary 2",
                                Category.SOCIETY,
                                List.of("spring"),
                                List.of("test media url 2"),
                                "test url 2",
                                Status.ACTIVE,
                                LocalDateTime.of(2025, 5, 1, 0, 0, 0),
                                Source.AIF_RU
                        )
                ),
                "2025-05-01T00:00:00"
        );

        given(searchService.searchNews(
                nullable(String.class),
                nullable(LocalDateTime.class),
                nullable(LocalDateTime.class),
                any(),
                any(),
                any(),
                any(),
                nullable(SortField.class),
                nullable(SortDirection.class),
                nullable(String.class),
                anyInt()
        )).willReturn(response);

        mockMvc.perform(get("/v1/search")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.articles[0].id").isNotEmpty())
                .andExpect(jsonPath("$.articles[0].title").value("test title 1"))
                .andExpect(jsonPath("$.articles[0].summary").value("test summary 1"))
                .andExpect(jsonPath("$.articles[0].category").value("ECONOMICS"))
                .andExpect(jsonPath("$.articles[0].keywords[0]").value("java"))
                .andExpect(jsonPath("$.articles[0].mediaUrls[0]").value("test media url 1"))
                .andExpect(jsonPath("$.articles[0].url").value("test url 1"))
                .andExpect(jsonPath("$.articles[0].status").value("NEW"))
                .andExpect(jsonPath("$.articles[0].publishedAt").value("2025-05-07T00:00:00"))
                .andExpect(jsonPath("$.articles[0].source").value("RT_RU"))
                .andExpect(jsonPath("$.articles[1].id").isNotEmpty())
                .andExpect(jsonPath("$.articles[1].title").value("test title 2"))
                .andExpect(jsonPath("$.articles[1].summary").value("test summary 2"))
                .andExpect(jsonPath("$.articles[1].category").value("SOCIETY"))
                .andExpect(jsonPath("$.articles[1].keywords[0]").value("spring"))
                .andExpect(jsonPath("$.articles[1].mediaUrls[0]").value("test media url 2"))
                .andExpect(jsonPath("$.articles[1].url").value("test url 2"))
                .andExpect(jsonPath("$.articles[1].status").value("ACTIVE"))
                .andExpect(jsonPath("$.articles[1].publishedAt").value("2025-05-01T00:00:00"))
                .andExpect(jsonPath("$.articles[1].source").value("AIF_RU"))
                .andExpect(jsonPath("$.nextCursor").value("2025-05-01T00:00:00"));
    }

    @Test
    void searchNews_invalidFromDate_future() throws Exception {
        mockMvc.perform(get("/v1/search")
                        .param("fromDate", LocalDateTime.now().plusDays(1).toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.fromDate")
                        .value("'fromDate' timestamp must be in the past"));
    }

    @Test
    void searchNews_invalidToDate_future() throws Exception {
        mockMvc.perform(get("/v1/search")
                        .param("toDate", LocalDateTime.now().plusDays(1).toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.toDate")
                        .value("'toDate' timestamp must be in the past or present"));
    }

    @Test
    void searchNews_invalidLimit_zero() throws Exception {
        mockMvc.perform(get("/v1/search")
                        .param("limit", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.limit")
                        .value("Limit must be > 0"));
    }

    @Test
    void searchNews_invalidLimit_negative() throws Exception {
        mockMvc.perform(get("/v1/search")
                        .param("limit", "-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.limit")
                        .value("Limit must be > 0"));
    }

    @Test
    void searchNews_invalidLimit_max() throws Exception {
        mockMvc.perform(get("/v1/search")
                        .param("limit", "101")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.limit")
                        .value("Limit must be ≤ 100"));
    }

    @Test
    void getNewsArticle() throws Exception {
        NewsArticleFull response = new NewsArticleFull(
                UUID.randomUUID(),
                "test title",
                "test summary",
                "test content",
                Category.SCIENCE_TECH,
                List.of("java", "spring"),
                List.of("test media url"),
                "test url",
                Status.DELETED,
                LocalDateTime.of(2025, 5, 1, 0, 0, 0),
                Source.SVPRESSA_RU
        );

        given(searchService.getNewsArticle(any(UUID.class))).willReturn(response);

        mockMvc.perform(get("/v1/search/" + UUID.randomUUID())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("test title"))
                .andExpect(jsonPath("$.summary").value("test summary"))
                .andExpect(jsonPath("$.content").value("test content"))
                .andExpect(jsonPath("$.category").value("SCIENCE_TECH"))
                .andExpect(jsonPath("$.keywords[0]").value("java"))
                .andExpect(jsonPath("$.keywords[1]").value("spring"))
                .andExpect(jsonPath("$.mediaUrls[0]").value("test media url"))
                .andExpect(jsonPath("$.url").value("test url"))
                .andExpect(jsonPath("$.status").value("DELETED"))
                .andExpect(jsonPath("$.publishedAt").value("2025-05-01T00:00:00"))
                .andExpect(jsonPath("$.source").value("SVPRESSA_RU"));
    }

}
