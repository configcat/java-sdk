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
                        this.logger.warn(3002, ConfigCatLogMessages.DATA_GOVERNANCE_IS_OUT_OF_SYNC_WARN);
                    }

                    if (executionCount > 0) {
                        return this.executeFetchAsync(executionCount - 1, eTag);
                    }
                }

            } catch (Exception exception) {
                this.logger.error(1103, ConfigCatLogMessages.FETCH_FAILED_DUE_TO_UNEXPECTED_ERROR, exception);
                return CompletableFuture.completedFuture(fetchResponse);
            }

            this.logger.error(1104, ConfigCatLogMessages.FETCH_FAILED_DUE_TO_REDIRECT_LOOP_ERROR);
            return CompletableFuture.completedFuture(fetchResponse);
        });
    }

    private CompletableFuture<FetchResponse> getResponseAsync(final String eTag) {
        Request request = this.getRequest(eTag);
        CompletableFuture<FetchResponse> future = new CompletableFuture<>();
        this.httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                int logEventId = 1103;
                String message = ConfigCatLogMessages.FETCH_FAILED_DUE_TO_UNEXPECTED_ERROR;
                if (!isClosed.get()) {
                    if (e instanceof SocketTimeoutException) {
                        logEventId = 1102;
                        message = ConfigCatLogMessages.getFetchFailedDueToRequestTimeout(httpClient.connectTimeoutMillis() ,httpClient.readTimeoutMillis() ,httpClient.writeTimeoutMillis());
                    }
                    logger.error(logEventId, message, e);
                }
                future.complete(FetchResponse.failed(message, false, null));
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                try (ResponseBody body = response.body()) {
                    String fetchTime = response.headers().get("date");
                    if(fetchTime == null || fetchTime.isEmpty() || CacheUtils.DateTimeUtils.isValidDate(fetchTime)){
                        fetchTime = CacheUtils.DateTimeUtils.format(System.currentTimeMillis());
                    }
                    if (response.isSuccessful() && body != null) {
                        String content = body.string();
                        String eTag = response.header("ETag");
                        Result<Config> result = deserializeConfig(content);
                        if (result.error() != null) {
                            future.complete(FetchResponse.failed(result.error(), false, null));
                            return;
                        }
                        logger.debug("Fetch was successful: new config fetched.");
                        future.complete(FetchResponse.fetched(new Entry(result.value(), eTag, content, fetchTime), fetchTime));
                    } else if (response.code() == 304) {
                        logger.debug("Fetch was successful: config not modified.");
                        future.complete(FetchResponse.notModified(fetchTime));
                    } else if (response.code() == 403 || response.code() == 404) {
                        String message = ConfigCatLogMessages.FETCH_FAILED_DUE_TO_INVALID_SDK_KEY_ERROR;
                        logger.error(1100, message);
                        future.complete(FetchResponse.failed(message, true, fetchTime));
                    } else {
                        String message = ConfigCatLogMessages.getFetchFailedDueToUnexpectedHttpResponse(response.code(),response.message());
                        logger.error(1101, message);
                        future.complete(FetchResponse.failed(message, false, null));
                    }
                } catch (SocketTimeoutException e) {
                    String message = ConfigCatLogMessages.getFetchFailedDueToRequestTimeout(httpClient.connectTimeoutMillis() ,httpClient.readTimeoutMillis() ,httpClient.writeTimeoutMillis());
                    logger.error(1102, message, e);
                    future.complete(FetchResponse.failed(message, false, null));
                } catch (Exception e) {
                    String message = ConfigCatLogMessages.FETCH_FAILED_DUE_TO_UNEXPECTED_ERROR;
                    logger.error(1103, message, e);
                    future.complete(FetchResponse.failed(message, false, null));
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
            String message = ConfigCatLogMessages.FETCH_RECEIVED_200_WITH_INVALID_BODY_ERROR;
            this.logger.error(1105, message, e);
            return Result.error(message, null);
        }
    }
}

