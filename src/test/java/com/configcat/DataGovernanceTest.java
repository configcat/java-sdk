package com.configcat;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

public class DataGovernanceTest {
    private static final String JsonTemplate = "{ p: { u: \"%s\", r: %d }, f: {} }";
    private final ConfigCatLogger logger = new ConfigCatLogger(LoggerFactory.getLogger(DataGovernanceTest.class));

    @Test
    public void shouldStayOnGivenUrl() throws IOException, ExecutionException, InterruptedException {
        MockWebServer server = this.createServer();
        ConfigFetcher fetcher = this.createFetcher(server.url("/").toString(), false);

        // Arrange
        String body = String.format(JsonTemplate, server.url("/"), 0);
        server.enqueue(new MockResponse().setResponseCode(200).setBody(body));

        // Act
        FetchResponse response = fetcher.fetchAsync().get();

        // Assert
        assertEquals(body, response.config().jsonString);
        assertEquals(1, server.getRequestCount());

        // Cleanup
        fetcher.close();
        server.shutdown();
    }

    @Test
    public void shouldStayOnSameUrl() throws IOException, ExecutionException, InterruptedException {
        MockWebServer server = this.createServer();
        ConfigFetcher fetcher = this.createFetcher(server.url("/").toString(), false);

        // Arrange
        String body = String.format(JsonTemplate, server.url("/"), 1);
        server.enqueue(new MockResponse().setResponseCode(200).setBody(body));

        // Act
        FetchResponse response = fetcher.fetchAsync().get();

        // Assert
        assertEquals(body, response.config().jsonString);
        assertEquals(1, server.getRequestCount());

        // Cleanup
        fetcher.close();
        server.shutdown();
    }

    @Test
    public void shouldStayOnSameUrlEvenWithForce() throws IOException, ExecutionException, InterruptedException {
        MockWebServer server = this.createServer();
        ConfigFetcher fetcher = this.createFetcher(server.url("/").toString(), false);

        // Arrange
        String body = String.format(JsonTemplate, server.url("/"), 2);
        server.enqueue(new MockResponse().setResponseCode(200).setBody(body));

        // Act
        FetchResponse response = fetcher.fetchAsync().get();

        // Assert
        assertEquals(body, response.config().jsonString);
        assertEquals(1, server.getRequestCount());

        // Cleanup
        fetcher.close();
        server.shutdown();
    }

    @Test
    public void shouldRedirectToAnotherServer() throws IOException, ExecutionException, InterruptedException {
        MockWebServer firstServer = this.createServer();
        MockWebServer secondServer = this.createServer();
        ConfigFetcher fetcher = this.createFetcher(firstServer.url("/").toString(), false);

        // Arrange
        String firstBody = String.format(JsonTemplate, secondServer.url("/"), 1);
        firstServer.enqueue(new MockResponse().setResponseCode(200).setBody(firstBody));

        String secondBody = String.format(JsonTemplate, secondServer.url("/"), 0);
        secondServer.enqueue(new MockResponse().setResponseCode(200).setBody(secondBody));

        // Act
        FetchResponse response = fetcher.fetchAsync().get();

        // Assert
        assertEquals(secondBody, response.config().jsonString);
        assertEquals(1, firstServer.getRequestCount());
        assertEquals(1, secondServer.getRequestCount());

        // Cleanup
        fetcher.close();
        firstServer.shutdown();
        secondServer.shutdown();
    }

    @Test
    public void shouldRedirectToAnotherServerWhenForced() throws IOException, ExecutionException, InterruptedException {
        MockWebServer firstServer = this.createServer();
        MockWebServer secondServer = this.createServer();
        ConfigFetcher fetcher = this.createFetcher(firstServer.url("/").toString(), false);

        // Arrange
        String firstBody = String.format(JsonTemplate, secondServer.url("/"), 2);
        firstServer.enqueue(new MockResponse().setResponseCode(200).setBody(firstBody));

        String secondBody = String.format(JsonTemplate, secondServer.url("/"), 0);
        secondServer.enqueue(new MockResponse().setResponseCode(200).setBody(secondBody));

        // Act
        FetchResponse response = fetcher.fetchAsync().get();

        // Assert
        assertEquals(secondBody, response.config().jsonString);
        assertEquals(1, firstServer.getRequestCount());
        assertEquals(1, secondServer.getRequestCount());

        // Cleanup
        fetcher.close();
        firstServer.shutdown();
        secondServer.shutdown();
    }

    @Test
    public void shouldBreakTheRedirectLoop() throws IOException, ExecutionException, InterruptedException {
        MockWebServer firstServer = this.createServer();
        MockWebServer secondServer = this.createServer();
        ConfigFetcher fetcher = this.createFetcher(firstServer.url("/").toString(), false);

        // Arrange
        String firstBody = String.format(JsonTemplate, secondServer.url("/"), 1);
        firstServer.enqueue(new MockResponse().setResponseCode(200).setBody(firstBody));
        firstServer.enqueue(new MockResponse().setResponseCode(200).setBody(firstBody));

        String secondBody = String.format(JsonTemplate, firstServer.url("/"), 1);
        secondServer.enqueue(new MockResponse().setResponseCode(200).setBody(secondBody));

        // Act
        FetchResponse response = fetcher.fetchAsync().get();

        // Assert
        assertEquals(firstBody, response.config().jsonString);
        assertEquals(2, firstServer.getRequestCount());
        assertEquals(1, secondServer.getRequestCount());

        // Cleanup
        fetcher.close();
        firstServer.shutdown();
        secondServer.shutdown();
    }

    @Test
    public void shouldRespectCustomUrlWhenNotForced() throws IOException, ExecutionException, InterruptedException {
        MockWebServer firstServer = this.createServer();
        MockWebServer secondServer = this.createServer();
        ConfigFetcher fetcher = this.createFetcher(firstServer.url("/").toString(), true);

        // Arrange
        String firstBody = String.format(JsonTemplate, secondServer.url("/"), 1);
        firstServer.enqueue(new MockResponse().setResponseCode(200).setBody(firstBody));

        // Act
        FetchResponse response = fetcher.fetchAsync().get();

        // Assert
        assertEquals(firstBody, response.config().jsonString);
        assertEquals(1, firstServer.getRequestCount());
        assertEquals(0, secondServer.getRequestCount());

        // Cleanup
        fetcher.close();
        firstServer.shutdown();
        secondServer.shutdown();
    }

    @Test
    public void shouldNotRespectCustomUrlWhenForced() throws IOException, ExecutionException, InterruptedException {
        MockWebServer firstServer = this.createServer();
        MockWebServer secondServer = this.createServer();
        ConfigFetcher fetcher = this.createFetcher(firstServer.url("/").toString(), true);

        // Arrange
        String firstBody = String.format(JsonTemplate, secondServer.url("/"), 2);
        firstServer.enqueue(new MockResponse().setResponseCode(200).setBody(firstBody));

        String secondBody = String.format(JsonTemplate, secondServer.url("/"), 0);
        secondServer.enqueue(new MockResponse().setResponseCode(200).setBody(secondBody));

        // Act
        FetchResponse response = fetcher.fetchAsync().get();

        // Assert
        assertEquals(secondBody, response.config().jsonString);
        assertEquals(1, firstServer.getRequestCount());
        assertEquals(1, secondServer.getRequestCount());

        // Cleanup
        fetcher.close();
        firstServer.shutdown();
        secondServer.shutdown();
    }

    private MockWebServer createServer() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();

        return server;
    }

    private ConfigFetcher createFetcher(String url, boolean isCustomUrl) {
        return new ConfigFetcher(new OkHttpClient.Builder().build(), logger, new ConfigMemoryCache(logger), "", url, isCustomUrl, "m");
    }
}
