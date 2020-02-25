package com.configcat;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * This class is used by the internal {@link ConfigCache} implementation to fetch the latest configuration.
 */
class ConfigFetcher implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigFetcher.class);
    private final OkHttpClient httpClient;
    private String url;
    private String eTag;
    private String mode;
    private final String version;

    ConfigFetcher(OkHttpClient httpClient, String apiKey, PollingMode mode) {
        this(httpClient, apiKey, null, mode);
    }

    ConfigFetcher(OkHttpClient httpClient, String apiKey, String baseUrl, PollingMode mode) {
        baseUrl = baseUrl == null || baseUrl.isEmpty() ? "https://cdn.configcat.com" : baseUrl;
        this.httpClient = httpClient;
        this.url = baseUrl + "/configuration-files/" + apiKey + "/config_v4.json";
        this.version = this.getClass().getPackage().getImplementationVersion();
        this.mode = mode.getPollingIdentifier();
    }

    /**
     * Gets the latest configuration from the network asynchronously.
     *
     * @return a {@link FetchResponse} instance which holds the result of the fetch.
     */
    public CompletableFuture<FetchResponse> getConfigurationJsonStringAsync() {
        Request request = this.getRequest();

        CompletableFuture<FetchResponse> future = new CompletableFuture<>();
        this.httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                LOGGER.error("An error occurred during fetching the latest configuration.", e);
                future.complete(new FetchResponse(FetchResponse.Status.FAILED, null));
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (response.isSuccessful()) {
                        LOGGER.debug("Fetch was successful: new config fetched.");
                        eTag = response.header("ETag");
                        future.complete(new FetchResponse(FetchResponse.Status.FETCHED, response.body().string()));
                    } else if (response.code() == 304) {
                        LOGGER.debug("Fetch was successful: config not modified.");
                        future.complete(new FetchResponse(FetchResponse.Status.NOTMODIFIED, null));
                    } else {
                        LOGGER.error("Double-check your API KEY at https://app.configcat.com/apikey. Received unexpected response: " + response.code());
                        future.complete(new FetchResponse(FetchResponse.Status.FAILED, null));
                    }
                } catch (Exception e) {
                    LOGGER.error("Exception in ConfigFetcher.getConfigurationJsonStringAsync", e);
                    future.complete(new FetchResponse(FetchResponse.Status.FAILED, null));
                }
            }
        });

        return future;
    }

    @Override
    public void close() throws IOException {
        if (this.httpClient != null) {
            if (this.httpClient.dispatcher() != null && this.httpClient.dispatcher().executorService() != null)
                this.httpClient.dispatcher().executorService().shutdownNow();

            if (this.httpClient.connectionPool() != null)
                this.httpClient.connectionPool().evictAll();

            if (this.httpClient.cache() != null)
                this.httpClient.cache().close();

        }
    }

    Request getRequest() {
        Request.Builder builder =  new Request.Builder()
                .addHeader("X-ConfigCat-UserAgent", "ConfigCat-Java/"+ this.mode + "-" + this.version);

        if(this.eTag != null)
            builder.addHeader("If-None-Match", this.eTag);

        return builder.url(this.url).build();
    }
}

