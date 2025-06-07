package dev.j3rrryy.news_aggregator.service.v1;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.j3rrryy.news_aggregator.entity.NewsArticle;
import dev.j3rrryy.news_aggregator.enums.*;
import dev.j3rrryy.news_aggregator.exceptions.ExportFailedException;
import dev.j3rrryy.news_aggregator.exceptions.FromDateAfterToDateException;
import dev.j3rrryy.news_aggregator.mapper.SearchMapper;
import dev.j3rrryy.news_aggregator.repository.NewsArticleRepository;
import dev.j3rrryy.news_aggregator.specification.NewsArticleSpecs;
import dev.j3rrryy.news_aggregator.utils.Exporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static dev.j3rrryy.news_aggregator.utils.SortResolver.resolveSort;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static org.springframework.web.util.HtmlUtils.htmlEscape;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExportService {

    private static final DateTimeFormatter htmlDateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm");
    private static final String HTML_HEAD = """
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        padding: 20px;
                        background-color: #f8f8f8;
                        max-width: 992px;
                        margin: 0 auto;
                    }
            
                    .article {
                        background: #ffffff;
                        border: 1px solid #ddd;
                        border-radius: 10px;
                        padding: 20px;
                        margin-bottom: 24px;
                        box-shadow: 0 2px 8px rgba(0,0,0,0.04);
                        transition: box-shadow 0.3s ease, transform 0.3s ease;
                    }
            
                    .article:hover {
                        box-shadow: 0 4px 16px rgba(0,0,0,0.12);
                        transform: translateY(-2px);
                    }
            
                    h2 {
                        margin-bottom: 6px;
                    }
            
                    a {
                        font-weight: bold;
                        text-decoration: none;
                        color: #0066cc;
                    }
            
                    a:hover {
                        text-decoration: underline;
                    }
            
                    .summary {
                        margin-top: 8px;
                        margin-bottom: 16px;
                    }
            
                    .meta {
                        display: flex;
                        flex-wrap: wrap;
                        gap: 10px;
                        font-size: 0.85em;
                        color: #555;
                        margin-top: 8px;
                    }
            
                    .meta span {
                        background-color: #f0f0f0;
                        padding: 4px 8px;
                        border-radius: 6px;
                        align-content: center;
                    }
            
                    .meta-category {
                        background-color: #e0ecff;
                        color: #004a99;
                        font-weight: bold;
                        text-transform: capitalize;
                    }
            
                    .meta-date {
                        background-color: #ffe4cc;
                    }
            
                    .meta-date::before {
                        content: "🗓️ ";
                    }
            
                    .meta-source {
                        font-style: italic;
                        background-color: #e6f5d0;
                        color: #3b5d23;
                    }
            
                    .keywords {
                        display: flex;
                        flex-wrap: wrap;
                        gap: 6px;
                        margin-top: 8px;
                    }
            
                    .keywords span {
                        background-color: #f3f3f3;
                        color: #555;
                        padding: 4px 8px;
                        border-radius: 6px;
                        font-size: 0.8em;
                        white-space: nowrap;
                    }
            
                    .meta, .content, .keywords {
                        margin-top: 8px;
                    }
            
                    .media img {
                        width: 100%;
                        max-width: 300px;
                        border-radius: 8px;
                        margin: 5px;
                        height: auto;
                        object-fit: cover;
                        box-sizing: border-box;
                    }
            
                    .media {
                        display: flex;
                        flex-wrap: wrap;
                        gap: 10px;
                    }
            
                    .content {
                        margin-top: 25px;
                        white-space: pre-wrap;
                        border-left: 3px solid #eee;
                        padding-left: 12px;
                        color: #444;
                    }
                </style>
                <title>Exported News</title>
            </head>
            <body>
            """;
    private static final String HTML_FOOTER = "</body></html>";

    private final SearchMapper searchMapper;
    private final NewsArticleRepository repository;
    private final Map<FileFormat, Exporter> formatMap = Map.of(
            FileFormat.CSV, this::exportCsv,
            FileFormat.JSON, this::exportJson,
            FileFormat.HTML, this::exportHtml
    );

    public void export(
            String query,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Set<Category> categories,
            Set<Source> sources,
            Set<Status> statuses,
            Set<String> keywords,
            SortField sortField,
            SortDirection sortDirection,
            OutputStream outputStream,
            boolean includeContent,
            FileFormat fileFormat
    ) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new FromDateAfterToDateException();
        }

        Specification<NewsArticle> spec = NewsArticleSpecs.filterAll(
                query, fromDate, toDate, categories, sources, statuses,
                keywords, null, null
        );
        Sort sort = resolveSort(sortField, sortDirection);

        Exporter exporter = formatMap.get(fileFormat);
        exporter.accept(spec, sort, outputStream, includeContent);
    }

    private void exportCsv(
            Specification<NewsArticle> spec,
            Sort sort,
            OutputStream outputStream,
            boolean includeContent
    ) {
        List<String> headers = new ArrayList<>(List.of(
                "id", "title", "summary"
        ));
        if (includeContent) {
            headers.add("content");
        }
        headers.addAll(List.of(
                "category", "keywords", "mediaUrls",
                "url", "status", "publishedAt", "source"
        ));

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(headers.toArray(new String[0]))
                .get();

        try (
                Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
                CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)
        ) {
            int page = 0;
            boolean hasNext;

            do {
                Pageable pageable = PageRequest.of(page++, 1000, sort);
                Slice<NewsArticle> slice = repository.findAll(spec, pageable);
                hasNext = slice.hasNext();

                for (NewsArticle article : slice.getContent()) {
                    List<Object> row = new ArrayList<>(List.of(
                            article.getId(),
                            article.getTitle(),
                            article.getSummary()
                    ));
                    if (includeContent) {
                        row.add(article.getContent());
                    }
                    row.addAll(List.of(
                            article.getCategory(),
                            String.join(";", article.getKeywords()),
                            String.join(";", article.getMediaUrls()),
                            article.getUrl(),
                            article.getStatus(),
                            ISO_LOCAL_DATE_TIME.format(article.getPublishedAt()),
                            article.getSource()
                    ));

                    csvPrinter.printRecord(row);
                }
            } while (hasNext);

            csvPrinter.flush();
        } catch (Exception e) {
            log.error("Export to CSV failed: {}", e.getMessage());
            throw new ExportFailedException("CSV");
        }
    }

    private void exportJson(
            Specification<NewsArticle> spec,
            Sort sort,
            OutputStream outputStream,
            boolean includeContent
    ) {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        try (JsonGenerator jsonGenerator = objectMapper.getFactory().createGenerator(outputStream)) {
            int page = 0;
            boolean hasNext;
            jsonGenerator.writeStartArray();

            do {
                Pageable pageable = PageRequest.of(page++, 1000, sort);
                Slice<NewsArticle> slice = repository.findAll(spec, pageable);
                hasNext = slice.hasNext();

                for (NewsArticle article : slice.getContent()) {
                    Object serializedArticle = includeContent
                            ? searchMapper.toFull(article)
                            : searchMapper.toSummary(article);
                    objectMapper.writeValue(jsonGenerator, serializedArticle);
                }
            } while (hasNext);

            jsonGenerator.writeEndArray();
            jsonGenerator.flush();
        } catch (Exception e) {
            log.error("Export to JSON failed: {}", e.getMessage());
            throw new ExportFailedException("JSON");
        }
    }

    private void exportHtml(
            Specification<NewsArticle> spec,
            Sort sort,
            OutputStream outputStream,
            boolean includeContent
    ) {
        try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            writer.write(HTML_HEAD);

            int page = 0;
            boolean hasNext;

            do {
                Pageable pageable = PageRequest.of(page++, 1000, sort);
                Slice<NewsArticle> slice = repository.findAll(spec, pageable);
                hasNext = slice.hasNext();

                for (NewsArticle article : slice.getContent()) {
                    writer.write(renderArticle(article, includeContent));
                }
            } while (hasNext);

            writer.write(HTML_FOOTER);
            writer.flush();
        } catch (Exception e) {
            log.error("Export to HTML failed: {}", e.getMessage());
            throw new ExportFailedException("HTML");
        }
    }

    private String renderArticle(NewsArticle article, boolean includeContent) {
        StringBuilder html = new StringBuilder();
        html.append("<div class='article'>");

        html.append("<div class='media'>");
        for (String mediaUrl : article.getMediaUrls()) {
            html.append("<img src=\"")
                    .append(htmlEscape(mediaUrl))
                    .append("\" alt=\"media\" loading=\"lazy\"/>");
        }
        html.append("</div>");

        html.append("<h2><a href=\"")
                .append(article.getUrl())
                .append("\" target=\"_blank\" rel=\"noopener noreferrer\">")
                .append(htmlEscape(article.getTitle()))
                .append("</a></h2>");

        html.append("<div class='summary'>")
                .append(htmlEscape(article.getSummary().trim()))
                .append("</div>");

        html.append("<div class='meta'>")
                .append("<span class='meta-date'>")
                .append(htmlEscape(article.getPublishedAt().format(htmlDateTimeFormatter)))
                .append("</span>")
                .append("<span class='meta-category badge'>")
                .append(getCategory(article))
                .append("</span>")
                .append("<span class='meta-source'>")
                .append(getSource(article))
                .append("</span>")
                .append("</div>");

        html.append("<div class='keywords'>");
        for (String keyword : article.getKeywords()) {
            html.append("<span>")
                    .append(htmlEscape(keyword))
                    .append("</span>");
        }
        html.append("</div>");

        if (includeContent) {
            html.append("<div class='content'>")
                    .append(htmlEscape(article.getContent().trim()))
                    .append("</div>");
        }

        html.append("</div>");
        return html.toString();
    }

    private String getCategory(NewsArticle article) {
        return htmlEscape(article.getCategory().name().toLowerCase().replace('_', '-'));
    }

    private String getSource(NewsArticle article) {
        return htmlEscape(article.getSource().name().toLowerCase().replace('_', '.'));
    }

}
