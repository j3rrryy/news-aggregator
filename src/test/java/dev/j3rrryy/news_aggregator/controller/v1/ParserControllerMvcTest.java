package dev.j3rrryy.news_aggregator.controller.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import dev.j3rrryy.news_aggregator.dto.request.AutoParsingInterval;
import dev.j3rrryy.news_aggregator.dto.request.NewsSourceStatusesRequest;
import dev.j3rrryy.news_aggregator.dto.response.AutoParsingStatus;
import dev.j3rrryy.news_aggregator.dto.response.NewsSourceStatusesResponse;
import dev.j3rrryy.news_aggregator.dto.response.ParsingStatus;
import dev.j3rrryy.news_aggregator.parser.scheduler.ParsingScheduler;
import dev.j3rrryy.news_aggregator.serializers.DurationSerializer;
import dev.j3rrryy.news_aggregator.service.v1.ParserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ParserController.class)
public class ParserControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ParserService parserService;

    @MockitoBean
    private ParsingScheduler parsingScheduler;

    @BeforeEach
    void setUp() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Duration.class, new DurationSerializer());
        objectMapper.registerModule(module);
    }

    @Test
    void startParsing() throws Exception {
        mockMvc.perform(post("/v1/parser/start")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());
        verify(parserService).startParsing();
    }

    @Test
    void stopParsing() throws Exception {
        mockMvc.perform(post("/v1/parser/stop")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());
        verify(parserService).stopParsing();
    }

    @Test
    void getParsingStatus() throws Exception {
        ParsingStatus response = new ParsingStatus(true);
        given(parserService.getParsingStatus()).willReturn(response);

        mockMvc.perform(get("/v1/parser/status")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.inProgress").value(true));
    }

    @Test
    void getSourceStatuses() throws Exception {
        NewsSourceStatusesResponse response = new NewsSourceStatusesResponse(false, false, true);
        given(parserService.getSourceStatuses()).willReturn(response);

        mockMvc.perform(get("/v1/parser/sources/statuses")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.rtRu").value(false))
                .andExpect(jsonPath("$.aifRu").value(false))
                .andExpect(jsonPath("$.svpressaRu").value(true));
    }

    @Test
    void patchSourceStatuses() throws Exception {
        NewsSourceStatusesRequest request = new NewsSourceStatusesRequest(true, false, null);
        mockMvc.perform(patch("/v1/parser/sources/statuses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
        verify(parserService).patchSourceStatuses(request);
    }

    @Test
    void getAutoParsingStatus() throws Exception {
        AutoParsingStatus response = new AutoParsingStatus(false, Duration.ofSeconds(150412));
        given(parserService.getAutoParsingStatus()).willReturn(response);

        mockMvc.perform(get("/v1/parser/auto-parsing/status")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.autoParsingEnabled").value(false))
                .andExpect(jsonPath("$.autoParsingInterval").value("1d17h46m"));
    }

    @Test
    void enableAutoParsing() throws Exception {
        mockMvc.perform(patch("/v1/parser/auto-parsing/enable")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
        verify(parsingScheduler).enableAutoParsing();
    }

    @Test
    void disableAutoParsing() throws Exception {
        mockMvc.perform(patch("/v1/parser/auto-parsing/disable")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
        verify(parsingScheduler).disableAutoParsing();
    }

    @Test
    void setAutoParsingInterval() throws Exception {
        AutoParsingInterval request = new AutoParsingInterval(Duration.ofSeconds(183960));
        mockMvc.perform(patch("/v1/parser/auto-parsing/interval")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autoParsingInterval\": \"2d3h6m\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
        verify(parserService).setAutoParsingInterval(request);
    }

    @Test
    void setAutoParsingInterval_invalidBody_invaildAutoParsingIntervalFormat() throws Exception {
        mockMvc.perform(patch("/v1/parser/auto-parsing/interval")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autoParsingInterval\": \"5dm\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.autoParsingInterval")
                        .value("Invalid duration format: 5dm. Expected a combination of numbers with " +
                                "units: 'd' (days), 'h' (hours), 'm' (minutes). Examples: 2d5h, 30m7h, 3d."));
    }

    @Test
    void setAutoParsingInterval_invalidBody_zeroAutoParsingInterval() throws Exception {
        mockMvc.perform(patch("/v1/parser/auto-parsing/interval")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autoParsingInterval\": \"0d0h0m\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.autoParsingInterval")
                        .value("Interval must not be zero"));
    }

    @Test
    void setAutoParsingInterval_invalidBody_missingAutoParsingInterval() throws Exception {
        mockMvc.perform(patch("/v1/parser/auto-parsing/interval")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.autoParsingInterval")
                        .value("Interval must be provided"));
    }

}
