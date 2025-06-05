package dev.j3rrryy.news_aggregator.parser.service;

import com.google.common.util.concurrent.RateLimiter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PageFetcherTest {

    private PageFetcher pageFetcher;

    @Mock
    private UserAgentProvider userAgentProvider;

    @Mock
    private ParsingStatusManager parsingStatusManager;

    @BeforeEach
    void setUp() {
        pageFetcher = spy(new PageFetcher(userAgentProvider, parsingStatusManager));
    }

    @Test
    void downloadPage_shouldReturnDocumentWhenSuccessful() throws Exception {
        RateLimiter limiter = RateLimiter.create(1000);
        String url = "https://example.com";

        Callable<Document> loader = mock();
        Document mockDoc = Jsoup.parse("<html><body>test</body></html>");

        when(parsingStatusManager.isStopRequested()).thenReturn(false);
        doReturn(false).when(pageFetcher).isThreadInterrupted();
        when(loader.call()).thenReturn(mockDoc);

        Optional<Document> result = pageFetcher.downloadPage(loader, url, limiter);

        assertTrue(result.isPresent());
        assertEquals("test", result.get().body().text());
    }

    @Test
    void downloadPage_shouldReturnEmpty_whenThreadIsInterrupted() throws Exception {
        RateLimiter limiter = RateLimiter.create(1000);
        String url = "https://example.com";

        Callable<Document> loader = mock();

        when(parsingStatusManager.isStopRequested()).thenReturn(false);
        doReturn(true).when(pageFetcher).isThreadInterrupted();

        Optional<Document> result = pageFetcher.downloadPage(loader, url, limiter);

        assertTrue(result.isEmpty());
        verify(loader, never()).call();
    }

    @Test
    void downloadPage_shouldReturnEmpty_whenStopRequestedAndThreadInterrupted() throws Exception {
        RateLimiter limiter = RateLimiter.create(1000);
        String url = "https://example.com";

        Callable<Document> loader = mock();

        when(parsingStatusManager.isStopRequested()).thenReturn(true);

        Optional<Document> result = pageFetcher.downloadPage(loader, url, limiter);

        assertTrue(result.isEmpty());
        verify(loader, never()).call();
    }

    @Test
    void downloadPage_shouldReturnEmptyOnInterruptedException() throws Exception {
        RateLimiter limiter = RateLimiter.create(1000);
        String url = "https://example.com";

        Callable<Document> loader = mock();

        when(parsingStatusManager.isStopRequested()).thenReturn(false);
        doReturn(false).when(pageFetcher).isThreadInterrupted();
        when(loader.call()).thenThrow(new InterruptedException());

        Optional<Document> result = pageFetcher.downloadPage(loader, url, limiter);

        assertTrue(result.isEmpty());
        assertTrue(Thread.currentThread().isInterrupted());
    }

    @Test
    void downloadPage_shouldReturnEmptyOnException() throws Exception {
        RateLimiter limiter = RateLimiter.create(1000);
        String url = "https://example.com";

        Callable<Document> loader = mock();

        when(parsingStatusManager.isStopRequested()).thenReturn(false);
        doReturn(false).when(pageFetcher).isThreadInterrupted();
        when(loader.call()).thenThrow(new IOException());

        Optional<Document> result = pageFetcher.downloadPage(loader, url, limiter);

        assertTrue(result.isEmpty());
    }

    @Test
    void fetchGet_shouldReturnCallableThatFetchesDocument() throws Exception {
        String url = "https://example.com";
        Connection connection = mock();
        Document mockDoc = Jsoup.parse("<html><body>test</body></html>");

        try (var mockedStatic = mockStatic(org.jsoup.Jsoup.class)) {
            mockedStatic.when(() -> org.jsoup.Jsoup.connect(url)).thenReturn(connection);
            when(userAgentProvider.getNextUserAgent()).thenReturn("test user agent");
            when(connection.userAgent("test user agent")).thenReturn(connection);
            when(connection.timeout(45_000)).thenReturn(connection);
            when(connection.headers(anyMap())).thenReturn(connection);
            when(connection.get()).thenReturn(mockDoc);

            Callable<Document> callable = pageFetcher.fetchGet(url);
            Document result = callable.call();

            assertNotNull(result);
            assertEquals("test", result.body().text());
            verify(connection).userAgent("test user agent");
            verify(connection).timeout(45_000);
            verify(connection).headers(anyMap());
            verify(connection).get();
        }
    }

    @Test
    void fetchPost_shouldReturnDocumentWhenSuccessful() throws Exception {
        String url = "https://example.com";
        String body = "{\"key\":\"value\"}";

        Connection connection = mock(Connection.class);
        Connection.Response response = mock(Connection.Response.class);

        String jsonResponse = "{\"data\":\"<html><body>test</body></html>\"}";

        try (var mockedStatic = mockStatic(org.jsoup.Jsoup.class)) {
            mockedStatic.when(() -> org.jsoup.Jsoup.connect(url)).thenReturn(connection);

            when(userAgentProvider.getNextUserAgent()).thenReturn("test user agent");
            when(connection.userAgent("test user agent")).thenReturn(connection);
            when(connection.timeout(45_000)).thenReturn(connection);
            when(connection.headers(anyMap())).thenReturn(connection);
            when(connection.header("X-Requested-With", "XMLHttpRequest")).thenReturn(connection);
            when(connection.method(Connection.Method.POST)).thenReturn(connection);
            when(connection.requestBody(body)).thenReturn(connection);
            when(connection.ignoreContentType(true)).thenReturn(connection);
            when(connection.execute()).thenReturn(response);
            when(response.body()).thenReturn(jsonResponse);
            mockedStatic.when(() -> Jsoup.parse(anyString())).thenCallRealMethod();

            Callable<Document> callable = pageFetcher.fetchPost(url, body);
            Document doc = callable.call();

            assertNotNull(doc);
            assertEquals("test", doc.body().text());

            verify(connection).userAgent("test user agent");
            verify(connection).timeout(45_000);
            verify(connection).headers(anyMap());
            verify(connection).header("X-Requested-With", "XMLHttpRequest");
            verify(connection).method(Connection.Method.POST);
            verify(connection).requestBody(body);
            verify(connection).ignoreContentType(true);
            verify(connection).execute();
            verify(response).body();
        }
    }

    @Test
    void isThreadInterrupted_shouldReturnTrueWhenThreadIsInterrupted() {
        Thread.currentThread().interrupt();
        boolean result = pageFetcher.isThreadInterrupted();

        assertTrue(result);
        assertFalse(Thread.interrupted());
    }

}
