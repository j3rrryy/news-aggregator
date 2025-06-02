package dev.j3rrryy.news_aggregator.controller.v1;

import dev.j3rrryy.news_aggregator.enums.FileFormat;
import dev.j3rrryy.news_aggregator.enums.SortDirection;
import dev.j3rrryy.news_aggregator.enums.SortField;
import dev.j3rrryy.news_aggregator.service.v1.ExportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.OutputStream;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.willAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExportController.class)
public class ExportControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExportService exportService;

    @Test
    void exportCsv() throws Exception {
        willAnswer(invocation -> {
            OutputStream os = invocation.getArgument(9);
            os.write("test".getBytes());
            os.flush();
            return null;
        }).given(exportService).export(
                nullable(String.class),
                nullable(LocalDateTime.class),
                nullable(LocalDateTime.class),
                any(),
                any(),
                any(),
                any(),
                nullable(SortField.class),
                nullable(SortDirection.class),
                any(OutputStream.class),
                anyBoolean(),
                eq(FileFormat.CSV)
        );

        MvcResult mvcResult = mockMvc.perform(get("/v1/export")
                        .param("fileFormat", "CSV")
                        .accept(MediaType.ALL))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("attachment; filename=news.csv")))
                .andExpect(content().string(containsString("test")));
    }

    @Test
    void exportJson() throws Exception {
        willAnswer(invocation -> {
            OutputStream os = invocation.getArgument(9);
            os.write("test".getBytes());
            os.flush();
            return null;
        }).given(exportService).export(
                nullable(String.class),
                nullable(LocalDateTime.class),
                nullable(LocalDateTime.class),
                any(),
                any(),
                any(),
                any(),
                nullable(SortField.class),
                nullable(SortDirection.class),
                any(OutputStream.class),
                anyBoolean(),
                eq(FileFormat.JSON)
        );

        MvcResult mvcResult = mockMvc.perform(get("/v1/export")
                        .param("fileFormat", "JSON")
                        .accept(MediaType.ALL))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("attachment; filename=news.json")))
                .andExpect(content().string(containsString("test")));
    }

    @Test
    void exportHtml() throws Exception {
        willAnswer(invocation -> {
            OutputStream os = invocation.getArgument(9);
            os.write("test".getBytes());
            os.flush();
            return null;
        }).given(exportService).export(
                nullable(String.class),
                nullable(LocalDateTime.class),
                nullable(LocalDateTime.class),
                any(),
                any(),
                any(),
                any(),
                nullable(SortField.class),
                nullable(SortDirection.class),
                any(OutputStream.class),
                anyBoolean(),
                eq(FileFormat.HTML)
        );

        MvcResult mvcResult = mockMvc.perform(get("/v1/export")
                        .param("fileFormat", "HTML")
                        .accept(MediaType.ALL))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_HTML))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("attachment; filename=news.html")))
                .andExpect(content().string(containsString("test")));
    }

    @Test
    void export_invalidFromDate_Future() throws Exception {
        mockMvc.perform(get("/v1/export")
                        .param("dateFrom", LocalDateTime.now().plusDays(1).toString())
                        .param("fileFormat", "CSV")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.dateFrom")
                        .value("'dateFrom' timestamp must be in the past"));
    }

    @Test
    void export_invalidToDate_Future() throws Exception {
        mockMvc.perform(get("/v1/export")
                        .param("dateTo", LocalDateTime.now().plusDays(1).toString())
                        .param("fileFormat", "CSV")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.dateTo")
                        .value("'dateTo' timestamp must be in the past or present"));
    }

}
