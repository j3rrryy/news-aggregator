package dev.j3rrryy.news_aggregator.controller.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.j3rrryy.news_aggregator.dto.request.MarkDeleted;
import dev.j3rrryy.news_aggregator.dto.response.ArticlesAffected;
import dev.j3rrryy.news_aggregator.dto.response.ArticlesSummary;
import dev.j3rrryy.news_aggregator.service.v1.ArticlesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ArticlesController.class)
public class ArticlesControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ArticlesService articlesService;

    @Test
    void getArticlesSummary() throws Exception {
        ArticlesSummary response = new ArticlesSummary(1, 2, 3, 6);
        given(articlesService.getArticlesSummary()).willReturn(response);

        mockMvc.perform(get("/v1/articles/summary")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.newArticlesCount").value(1))
                .andExpect(jsonPath("$.activeArticlesCount").value(2))
                .andExpect(jsonPath("$.deletedArticlesCount").value(3))
                .andExpect(jsonPath("$.totalArticles").value(6));
    }

    @Test
    void markAsDeleted() throws Exception {
        MarkDeleted request = new MarkDeleted(
                LocalDateTime.of(2025, 5, 1, 0, 0)
        );
        ArticlesAffected response = new ArticlesAffected(5);
        given(articlesService.markAsDeleted(any(MarkDeleted.class))).willReturn(response);

        mockMvc.perform(put("/v1/articles/mark-deleted")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.articlesAffected").value(5));
    }

    @Test
    void markAsDeleted_invalidBody_futureOlderThan() throws Exception {
        MarkDeleted request = new MarkDeleted(LocalDateTime.now().plusDays(1));
        mockMvc.perform(put("/v1/articles/mark-deleted")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.olderThan")
                        .value("Timestamp must be in the past or present"));
    }

    @Test
    void markAsDeleted_invalidBody_missingOlderThan() throws Exception {
        mockMvc.perform(put("/v1/articles/mark-deleted")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.olderThan")
                        .value("Timestamp must be provided"));
    }

    @Test
    void deleteMarkedArticles() throws Exception {
        ArticlesAffected response = new ArticlesAffected(7);
        given(articlesService.deleteMarkedArticles()).willReturn(response);

        mockMvc.perform(delete("/v1/articles/marked")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.articlesAffected").value(7));
    }

    @Test
    void deleteAllArticles() throws Exception {
        ArticlesAffected response = new ArticlesAffected(23);
        given(articlesService.deleteAllArticles()).willReturn(response);

        mockMvc.perform(delete("/v1/articles/all")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.articlesAffected").value(23));
    }

}
