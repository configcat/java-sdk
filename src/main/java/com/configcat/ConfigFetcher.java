package com.configcat;

import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

class ConfigFetcher implements Closeable {
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final ConfigCatLogger logger;
    private final OkHttpClient httpClient;
    private final String mode;

    private final String sdkKey;
    private final boolean urlIsCustom;

    private String url;

    enum RedirectMode {
        NoRedirect,
        ShouldRedirect,
        ForceRedirect
    }

    ConfigFetcher(OkHttpClient httpClient,
                  ConfigCatLogger logger,
                  String sdkKey,
                  String url,
                  boolean urlIsCustom,
                  String pollingIdentifier) {
        this.logger = logger;
        this.sdkKey = sdkKey;
        this.urlIsCustom = urlIsCustom;
        this.url = url;
        this.httpClient = httpClient;
        this.mode = pollingIdentifier;
    }

    public CompletableFuture<FetchResponse> fetchAsync(String eTag) {
        return this.executeFetchAsync(2, eTag);
    }

    private CompletableFuture<FetchResponse> executeFetchAsync(int executionCount, String eTag) {
        return this.getResponseAsync(eTag).thenComposeAsync(fetchResponse -> {
            if (!fetchResponse.isFetched()) {
                return CompletableFuture.completedFuture(fetchResponse);
            }
            try {
                Entry entry = fetchResponse.entry();
                Config config = entry.getConfig();
                if (config.getPreferences() == null) {
                    return CompletableFuture.completedFuture(fetchResponse);
                }

                String newUrl = config.getPreferences().getBaseUrl();
                if (newUrl.equals(this.url)) {
                    return CompletableFuture.completedFuture(fetchResponse);
                }

                int redirect = config.getPreferences().getRedirect();

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
                        return this.executeFetchAsync(executionCount - 1, eTag);
                    }
                }

            } catch (Exception exception) {
                this.logger.error("Exception while trying to fetch the config.json.", exception);
                return CompletableFuture.completedFuture(fetchResponse);
            }

            this.logger.error("Redirect loop during config.json fetch. Please contact support@configcat.com.");
            return CompletableFuture.completedFuture(fetchResponse);
        });
    }

    private CompletableFuture<FetchResponse> getResponseAsync(final String eTag) {
        Request request = this.getRequest(eTag);
        CompletableFuture<FetchResponse> future = new CompletableFuture<>();
        this.httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                String message = "Exception while trying to fetch the config.json.";
                if (!isClosed.get()) {
                    if (e instanceof SocketTimeoutException) {
                        message = "Request timed out. Timeout values: [connect: " + httpClient.connectTimeoutMillis() + "ms, read: " + httpClient.readTimeoutMillis() + "ms, write: " + httpClient.writeTimeoutMillis() + "ms]";
                    }
                    logger.error(message, e);
                }
                future.complete(FetchResponse.failed(message, false));
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                try (ResponseBody body = response.body()) {
                    if (response.isSuccessful() && body != null) {
                        String content = body.string();
                        String eTag = response.header("ETag");
                        Result<Config> result = deserializeConfig(content);
                        if (result.error() != null) {
                            future.complete(FetchResponse.failed(result.error(), false));
                            return;
                        }
                        logger.debug("Fetch was successful: new config fetched.");
                        future.complete(FetchResponse.fetched(new Entry(result.value(), eTag, System.currentTimeMillis())));
                    } else if (response.code() == 304) {
                        logger.debug("Fetch was successful: config not modified.");
                        future.complete(FetchResponse.notModified());
                    } else if (response.code() == 403 || response.code() == 404) {
                        String message = "Double-check your API KEY at https://app.configcat.com/apikey.";
                        logger.error(message);
                        future.complete(FetchResponse.failed(message, true));
                    } else {
                        String message = "Unexpected HTTP response received: " + response.code() + " " + response.message();
                        logger.error(message);
                        future.complete(FetchResponse.failed(message, false));
                    }
                } catch (SocketTimeoutException e) {
                    String message = "Request timed out. Timeout values: [connect: " + httpClient.connectTimeoutMillis() + "ms, read: " + httpClient.readTimeoutMillis() + "ms, write: " + httpClient.writeTimeoutMillis() + "ms]";
                    logger.error(message, e);
                    future.complete(FetchResponse.failed(message, false));
                } catch (Exception e) {
                    String message = "Exception while trying to fetch the config.json.";
                    logger.error(message, e);
                    future.complete(FetchResponse.failed(message, false));
                }
            }
        });

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
        String url = this.url + "/configuration-files/" + this.sdkKey + "/" + Constants.CONFIG_JSON_NAME + ".json";
        Request.Builder builder = new Request.Builder()
                .addHeader("X-ConfigCat-UserAgent", "ConfigCat-Java/" + this.mode + "-" + Constants.VERSION);

        if (etag != null && !etag.isEmpty())
            builder.addHeader("If-None-Match", etag);

        return builder.url(url).build();
    }

    private Result<Config> deserializeConfig(String json) {
        try {
            return Result.success(Utils.gson.fromJson(json, Config.class));
        } catch (Exception e) {
            String message = "JSON parsing failed. " + e.getMessage();
            this.logger.error(message);
            return Result.error(message, null);
        }
    }
}

