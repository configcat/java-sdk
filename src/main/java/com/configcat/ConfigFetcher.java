package com.configcat;

import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

class ConfigFetcher implements Closeable {
    public static final String CONFIG_JSON_NAME = "config_v5";
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final ConfigCatLogger logger;
    private final OkHttpClient httpClient;
    private final String mode;
    private static final String version = "7.1.3";
    private final ConfigJsonCache configJsonCache;
    private final String sdkKey;
    private final boolean urlIsCustom;
    private CompletableFuture<FetchResponse> currentFuture;

    private String url;

    enum RedirectMode {
        NoRedirect,
        ShouldRedirect,
        ForceRedirect
    }

    ConfigFetcher(OkHttpClient httpClient,
                  ConfigCatLogger logger,
                  ConfigJsonCache configJsonCache,
                  String sdkKey,
                  String url,
                  boolean urlIsCustom,
                  String pollingIdentifier) {
        this.logger = logger;
        this.configJsonCache = configJsonCache;
        this.sdkKey = sdkKey;
        this.urlIsCustom = urlIsCustom;
        this.url = url;
        this.httpClient = httpClient;
        this.mode = pollingIdentifier;
    }

    public CompletableFuture<FetchResponse> fetchAsync() {
        return this.executeFetchAsync(2);
    }

    private CompletableFuture<FetchResponse> executeFetchAsync(int executionCount) {
        return this.getResponseAsync().thenComposeAsync(fetchResponse -> {
            if (!fetchResponse.isFetched()) {
                return CompletableFuture.completedFuture(fetchResponse);
            }
            try {
                Config config = fetchResponse.config();
                if (config.preferences == null) {
                    return CompletableFuture.completedFuture(fetchResponse);
                }

                String newUrl = config.preferences.baseUrl;
                if (newUrl.equals(this.url)) {
                    return CompletableFuture.completedFuture(fetchResponse);
                }

                int redirect = config.preferences.redirect;

                // we have a custom url set, and we didn't get a forced redirect
                if (this.urlIsCustom && redirect != RedirectMode.ForceRedirect.ordinal()) {
                    return CompletableFuture.completedFuture(fetchResponse);
                }

                this.url = newUrl;

                if (redirect == RedirectMode.NoRedirect.ordinal()) { // no redirect
                    return CompletableFuture.completedFuture(fetchResponse);
                } else { // redirect
                    if (redirect == RedirectMode.ShouldRedirect.ordinal()) {
                        this.logger.warn("Your builder.dataGovernance() parameter at ConfigCatClient " +
                                "initialization is not in sync with your preferences on the ConfigCat " +
                                "Dashboard: https://app.configcat.com/organization/data-governance. " +
                                "Only Organization Admins can access this preference.");
                    }

                    if (executionCount > 0) {
                        return this.executeFetchAsync(executionCount - 1);
                    }
                }

            } catch (Exception exception) {
                this.logger.error("Exception in ConfigFetcher.executeFetchAsync", exception);
                return CompletableFuture.completedFuture(fetchResponse);
            }

            this.logger.error("Redirect loop during config.json fetch. Please contact support@configcat.com.");
            return CompletableFuture.completedFuture(fetchResponse);
        });
    }

    private CompletableFuture<FetchResponse> getResponseAsync() {
        if (this.currentFuture != null && !this.currentFuture.isDone()) {
            this.logger.debug("Config fetching is skipped because there is an ongoing fetch request");
            return this.currentFuture;
        }

        Config cachedConfig = this.configJsonCache.readFromCache();
        Request request = this.getRequest(cachedConfig.eTag);
        CompletableFuture<FetchResponse> future = new CompletableFuture<>();
        this.httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                if (!isClosed.get()) {
                    logger.error("An error occurred during fetching the latest configuration.", e);
                }
                future.complete(FetchResponse.failed());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                try (ResponseBody body = response.body()) {
                    if (response.isSuccessful() && body != null) {
                        String content = body.string();
                        String eTag = response.header("ETag");
                        Config config = configJsonCache.readFromJson(content, eTag);
                        if (config == Config.empty) {
                            future.complete(FetchResponse.failed());
                            return;
                        }
                        logger.debug("Fetch was successful: new config fetched.");
                        future.complete(FetchResponse.fetched(config));
                    } else if (response.code() == 304) {
                        logger.debug("Fetch was successful: config not modified.");
                        future.complete(FetchResponse.notModified());
                    } else {
                        logger.error("Double-check your API KEY at https://app.configcat.com/apikey. Received unexpected response: " + response.code());
                        future.complete(FetchResponse.failed());
                    }
                } catch (SocketTimeoutException e) {
                    logger.error("Request timed out. Timeout values: [connect: " + httpClient.connectTimeoutMillis() + "ms, read: " + httpClient.readTimeoutMillis() + "ms, write: " + httpClient.writeTimeoutMillis() + "ms]", e);
                    future.complete(FetchResponse.failed());
                } catch (Exception e) {
                    logger.error("Exception in ConfigFetcher.getResponseAsync", e);
                    future.complete(FetchResponse.failed());
                }
            }
        });

        this.currentFuture = future;
        return future;
    }

    @Override
    public void close() throws IOException {
        if (!this.isClosed.compareAndSet(false, true)) {
            return;
        }

        if (this.httpClient != null) {
            this.httpClient.dispatcher().executorService().shutdownNow();
            this.httpClient.connectionPool().evictAll();
            Cache cache = this.httpClient.cache();
            if (cache != null)
                cache.close();
        }
    }

    Request getRequest(String etag) {
        String url = this.url + "/configuration-files/" + this.sdkKey + "/" + CONFIG_JSON_NAME + ".json";
        Request.Builder builder = new Request.Builder()
                .addHeader("X-ConfigCat-UserAgent", "ConfigCat-Java/" + this.mode + "-" + version);

        if (etag != null && !etag.isEmpty())
            builder.addHeader("If-None-Match", etag);

        return builder.url(url).build();
    }
}

