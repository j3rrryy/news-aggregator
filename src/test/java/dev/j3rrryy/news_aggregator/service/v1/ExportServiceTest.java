package dev.j3rrryy.news_aggregator.service.v1;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.j3rrryy.news_aggregator.dto.response.NewsArticleFull;
import dev.j3rrryy.news_aggregator.dto.response.NewsArticleSummary;
import dev.j3rrryy.news_aggregator.entity.NewsArticle;
import dev.j3rrryy.news_aggregator.enums.*;
import dev.j3rrryy.news_aggregator.exceptions.ExportFailedException;
import dev.j3rrryy.news_aggregator.exceptions.FromDateAfterToDateException;
import dev.j3rrryy.news_aggregator.mapper.SearchMapper;
import dev.j3rrryy.news_aggregator.repository.NewsArticleRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ExportServiceTest {

    private NewsArticle article1;
    private NewsArticle article2;

    @Mock
    private SearchMapper searchMapper;

    @InjectMocks
    private ExportService exportService;

    @Mock
    private NewsArticleRepository repository;

    @BeforeEach
    void setUp() {
        article1 = new NewsArticle(
                UUID.randomUUID(),
                "test title 1",
                "test summary 1",
                "test content 1",
                Category.SPORT,
                Set.of("java"),
                Set.of("test media url 1"),
                "test url 1",
                Status.NEW,
                LocalDateTime.of(2025, 5, 1, 0, 0),
                Source.SVPRESSA_RU
        );
        article2 = new NewsArticle(
                UUID.randomUUID(),
                "test title 2",
                "test summary 2",
                "test content 2",
                Category.POLITICS,
                Set.of("spring"),
                Set.of("test media url 2"),
                "test url 2",
                Status.ACTIVE,
                LocalDateTime.of(2025, 5, 7, 0, 0),
                Source.AIF_RU
        );
    }

    @Test
    void export_toDateNull_noThrow() {
        Page<NewsArticle> page = new PageImpl<>(
                List.of(article1),
                PageRequest.of(0, 1000, Sort.unsorted()),
                1
        );
        Specification<NewsArticle> spec = any();
        when(repository.findAll(spec, any(Pageable.class))).thenReturn(page);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.export(
                null, LocalDateTime.of(2025, 5, 1, 0, 0),
                null, null, null, null, null,
                SortField.PUBLISHED_AT, SortDirection.ASC, out, false, FileFormat.CSV
        );

        String csv = out.toString(StandardCharsets.UTF_8);
        assertThat(csv).contains(article1.getId().toString());
    }

    @Test
    void export_withValidDateRange_noThrow() {
        Page<NewsArticle> page = new PageImpl<>(
                List.of(article1),
                PageRequest.of(0, 1000, Sort.unsorted()),
                1
        );
        Specification<NewsArticle> spec = any();
        when(repository.findAll(spec, any(Pageable.class))).thenReturn(page);

        NewsArticleSummary summary = new NewsArticleSummary(
                article1.getId(), article1.getTitle(), article1.getSummary(),
                article1.getCategory(), List.copyOf(article1.getKeywords()),
                List.copyOf(article1.getMediaUrls()), article1.getUrl(),
                article1.getStatus(), article1.getPublishedAt(), article1.getSource()
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.export(
                null, LocalDateTime.of(2025, 5, 1, 0, 0),
                LocalDateTime.of(2025, 5, 7, 0, 0), null,
                null, null, null, SortField.PUBLISHED_AT, SortDirection.ASC,
                out, false, FileFormat.CSV
        );

        assertThat(out.toString(StandardCharsets.UTF_8)).contains(summary.id().toString());
    }

    @Test
    void exportCsv_withoutContent_writesHeaderAndRow() throws Exception {
        Page<NewsArticle> page = new PageImpl<>(
                List.of(article1),
                PageRequest.of(0, 1000, Sort.unsorted()),
                1
        );
        Specification<NewsArticle> spec = any();
        when(repository.findAll(spec, any(Pageable.class))).thenReturn(page);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.export(
                null, null, null, null, null, null, null,
                SortField.PUBLISHED_AT, SortDirection.ASC,
                out, false, FileFormat.CSV
        );

        try (
                Reader reader = new InputStreamReader(
                        new ByteArrayInputStream(out.toByteArray()), StandardCharsets.UTF_8);
                CSVParser parser = CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setTrim(true)
                        .get()
                        .parse(reader)
        ) {
            List<String> headers = parser.getHeaderNames();
            assertThat(headers).containsExactly(
                    "id", "title", "summary",
                    "category", "keywords", "mediaUrls",
                    "url", "status", "publishedAt", "source"
            );

            List<CSVRecord> records = parser.getRecords();
            assertThat(records).hasSize(1);
            CSVRecord row = records.getFirst();

            assertThat(row.get("id")).isEqualTo(article1.getId().toString());
            assertThat(row.get("title")).isEqualTo(article1.getTitle());
            assertThat(row.get("summary")).isEqualTo(article1.getSummary());
            assertThat(row.get("category")).isEqualTo(article1.getCategory().name());
            assertThat(row.get("keywords")).isEqualTo(String.join(";", article1.getKeywords()));
            assertThat(row.get("mediaUrls")).isEqualTo(String.join(";", article1.getMediaUrls()));
            assertThat(row.get("url")).isEqualTo(article1.getUrl());
            assertThat(row.get("status")).isEqualTo(article1.getStatus().name());
            assertThat(row.get("publishedAt")).startsWith(article1.getPublishedAt().toString());
            assertThat(row.get("source")).isEqualTo(article1.getSource().name());
        }
    }

    @Test
    void exportCsv_withContent_includesContentColumn() {
        Page<NewsArticle> page = new PageImpl<>(
                List.of(article1),
                PageRequest.of(0, 1000, Sort.unsorted()),
                1
        );
        Specification<NewsArticle> spec = any();
        when(repository.findAll(spec, any(Pageable.class))).thenReturn(page);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.export(
                null, null, null, null, null, null, null,
                SortField.PUBLISHED_AT, SortDirection.ASC,
                out, true, FileFormat.CSV
        );

        String csv = out.toString(StandardCharsets.UTF_8);
        assertThat(csv).contains("content");
        assertThat(csv).contains("test content");
    }

    @Test
    void exportCsv_multiplePages_writesAllRecords() throws Exception {
        Page<NewsArticle> page1 = new PageImpl<>(
                List.of(article1),
                PageRequest.of(0, 1, Sort.unsorted()),
                2
        );
        Page<NewsArticle> page2 = new PageImpl<>(
                List.of(article2),
                PageRequest.of(1, 1, Sort.unsorted()),
                2
        );

        Specification<NewsArticle> spec = any();
        when(repository.findAll(spec, any(Pageable.class))).thenReturn(page1).thenReturn(page2);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.export(
                null, null, null, null, null, null, null,
                SortField.PUBLISHED_AT, SortDirection.ASC,
                out, false, FileFormat.CSV
        );

        try (
                Reader reader = new InputStreamReader(
                        new ByteArrayInputStream(out.toByteArray()), StandardCharsets.UTF_8);
                CSVParser parser = CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setTrim(true)
                        .get()
                        .parse(reader)
        ) {
            List<CSVRecord> records = parser.getRecords();
            assertThat(records).hasSize(2);

            CSVRecord r1 = records.get(0);
            assertThat(r1.get("id")).isEqualTo(article1.getId().toString());

            CSVRecord r2 = records.get(1);
            assertThat(r2.get("id")).isEqualTo(article2.getId().toString());
        }
    }

    @Test
    void exportJson_withoutContent_writesSummaries() throws Exception {
        NewsArticleSummary summary = new NewsArticleSummary(
                article1.getId(), article1.getTitle(), article1.getSummary(),
                article1.getCategory(), List.copyOf(article1.getKeywords()),
                List.copyOf(article1.getMediaUrls()), article1.getUrl(),
                article1.getStatus(), article1.getPublishedAt(), article1.getSource()
        );
        Page<NewsArticle> page = new PageImpl<>(
                List.of(article1),
                PageRequest.of(0, 1000, Sort.unsorted()),
                1
        );
        Specification<NewsArticle> spec = any();

        when(repository.findAll(spec, any(Pageable.class))).thenReturn(page);
        when(searchMapper.toSummary(article1)).thenReturn(summary);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.export(
                null, null, null, null, null, null, null,
                SortField.PUBLISHED_AT, SortDirection.ASC,
                out, false, FileFormat.JSON
        );

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        List<NewsArticleSummary> list = mapper.readValue(
                out.toByteArray(),
                new TypeReference<>() {
                }
        );
        assertThat(list).containsExactly(summary);
    }

    @Test
    void exportJson_withContent_writesFull() throws Exception {
        NewsArticleFull full = new NewsArticleFull(
                article1.getId(), article1.getTitle(), article1.getSummary(), article1.getContent(),
                article1.getCategory(), List.copyOf(article1.getKeywords()),
                List.copyOf(article1.getMediaUrls()), article1.getUrl(),
                article1.getStatus(), article1.getPublishedAt(), article1.getSource()
        );
        Page<NewsArticle> page = new PageImpl<>(
                List.of(article1),
                PageRequest.of(0, 1000, Sort.unsorted()),
                1
        );
        Specification<NewsArticle> spec = any();

        when(repository.findAll(spec, any(Pageable.class))).thenReturn(page);
        when(searchMapper.toFull(article1)).thenReturn(full);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.export(
                null, null, null, null, null, null, null,
                SortField.PUBLISHED_AT, SortDirection.ASC,
                out, true, FileFormat.JSON
        );

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        List<NewsArticleFull> list = mapper.readValue(
                out.toByteArray(),
                new TypeReference<>() {
                }
        );
        assertThat(list).containsExactly(full);
    }

    @Test
    void exportJson_multiplePages_writesAllRecords() throws Exception {
        Page<NewsArticle> page1 = new PageImpl<>(
                List.of(article1),
                PageRequest.of(0, 1, Sort.unsorted()),
                2
        );
        Page<NewsArticle> page2 = new PageImpl<>(
                List.of(article2),
                PageRequest.of(1, 1, Sort.unsorted()),
                2
        );

        Specification<NewsArticle> spec = any();
        when(repository.findAll(spec, any(Pageable.class))).thenReturn(page1).thenReturn(page2);

        NewsArticleSummary s1 = new NewsArticleSummary(
                article1.getId(), article1.getTitle(), article1.getSummary(),
                article1.getCategory(), List.copyOf(article1.getKeywords()),
                List.copyOf(article1.getMediaUrls()),
                article1.getUrl(), article1.getStatus(), article1.getPublishedAt(), article1.getSource()
        );
        NewsArticleSummary s2 = new NewsArticleSummary(
                article2.getId(), article2.getTitle(), article2.getSummary(),
                article2.getCategory(), List.copyOf(article2.getKeywords()),
                List.copyOf(article2.getMediaUrls()),
                article2.getUrl(), article2.getStatus(), article2.getPublishedAt(), article2.getSource()
        );

        when(searchMapper.toSummary(article1)).thenReturn(s1);
        when(searchMapper.toSummary(article2)).thenReturn(s2);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.export(
                null, null, null, null, null, null, null,
                SortField.PUBLISHED_AT, SortDirection.ASC,
                out, false, FileFormat.JSON
        );

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        List<NewsArticleSummary> list = mapper.readValue(
                out.toByteArray(),
                new TypeReference<>() {
                }
        );
        assertThat(list).containsExactly(s1, s2);
    }

    @Test
    void exportHtml_withoutContent_rendersCorrectly() {
        Page<NewsArticle> page = new PageImpl<>(
                List.of(article1),
                PageRequest.of(0, 1000, Sort.unsorted()),
                1
        );
        Specification<NewsArticle> spec = any();
        when(repository.findAll(spec, any(Pageable.class))).thenReturn(page);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.export(
                null, null, null, null, null, null, null,
                SortField.PUBLISHED_AT, SortDirection.ASC,
                out, false, FileFormat.HTML
        );

        String html = out.toString(StandardCharsets.UTF_8);
        assertThat(html).contains("<html>").contains("</html>");
        assertThat(html).contains("<h2><a href=\"" + article1.getUrl() + "\"");
        assertThat(html).doesNotContain(article1.getContent());
    }

    @Test
    void exportHtml_withContent_includesContentDiv() {
        Page<NewsArticle> page = new PageImpl<>(
                List.of(article1),
                PageRequest.of(0, 1000, Sort.unsorted()),
                1
        );
        Specification<NewsArticle> spec = any();
        when(repository.findAll(spec, any(Pageable.class))).thenReturn(page);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.export(
                null, null, null, null, null, null, null,
                SortField.PUBLISHED_AT, SortDirection.ASC,
                out, true, FileFormat.HTML
        );

        String html = out.toString(StandardCharsets.UTF_8);
        assertThat(html).contains("<div class='content'>").contains(article1.getContent());
    }

    @Test
    void exportHtml_multiplePages_writesAllArticles() {
        Page<NewsArticle> page1 = new PageImpl<>(
                List.of(article1),
                PageRequest.of(0, 1, Sort.unsorted()),
                2
        );
        Page<NewsArticle> page2 = new PageImpl<>(
                List.of(article2),
                PageRequest.of(1, 1, Sort.unsorted()),
                2
        );

        Specification<NewsArticle> spec = any();
        when(repository.findAll(spec, any(Pageable.class))).thenReturn(page1).thenReturn(page2);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exportService.export(
                null, null, null, null, null, null, null,
                SortField.PUBLISHED_AT, SortDirection.ASC,
                out, false, FileFormat.HTML
        );

        String html = out.toString(StandardCharsets.UTF_8);
        assertThat(html.split("<div class='article'>", -1)).hasSize(3);
        assertThat(html).contains("<a href=\"" + article1.getUrl() + "\"");
        assertThat(html).contains("<a href=\"" + article2.getUrl() + "\"");
    }

    @Test
    void export_throwsOnInvalidDateRange() {
        LocalDateTime from = LocalDateTime.of(2025, 5, 7, 0, 0);
        LocalDateTime to = LocalDateTime.of(2025, 5, 1, 0, 0);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        assertThatThrownBy(() ->
                exportService.export(null, from, to, null, null, null, null,
                        SortField.PUBLISHED_AT, SortDirection.ASC,
                        out, false, FileFormat.CSV)
        ).isInstanceOf(FromDateAfterToDateException.class);
    }

    @Test
    void exportCsv_onRepositoryError_throwsExportFailed() {
        Specification<NewsArticle> spec = any();
        when(repository.findAll(spec, any(Pageable.class)))
                .thenThrow(new RuntimeException("test message"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertThatThrownBy(() ->
                exportService.export(null, null, null, null,
                        null, null, null,
                        SortField.PUBLISHED_AT, SortDirection.ASC,
                        out, false, FileFormat.CSV)
        ).isInstanceOf(ExportFailedException.class)
                .hasMessageContaining("CSV");
    }

    @Test
    void exportJson_onRepositoryError_throwsExportFailed() {
        Specification<NewsArticle> spec = any();
        when(repository.findAll(spec, any(Pageable.class)))
                .thenThrow(new RuntimeException("test message"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertThatThrownBy(() ->
                exportService.export(null, null, null, null,
                        null, null, null,
                        SortField.PUBLISHED_AT, SortDirection.ASC,
                        out, false, FileFormat.JSON)
        ).isInstanceOf(ExportFailedException.class)
                .hasMessageContaining("JSON");
    }

    @Test
    void exportHtml_onRepositoryError_throwsExportFailed() {
        Specification<NewsArticle> spec = any();
        when(repository.findAll(spec, any(Pageable.class)))
                .thenThrow(new RuntimeException("test message"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertThatThrownBy(() ->
                exportService.export(null, null, null, null,
                        null, null, null,
                        SortField.PUBLISHED_AT, SortDirection.ASC,
                        out, false, FileFormat.HTML)
        ).isInstanceOf(ExportFailedException.class)
                .hasMessageContaining("HTML");
    }

}
