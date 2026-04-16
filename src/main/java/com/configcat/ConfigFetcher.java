package com.configcat;

import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

class ConfigFetcher implements Closeable {

    private static final long RETRY_DELAY_MS = 50;

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
        return this.fetchWithRetryAsync(eTag).thenComposeAsync(fetchResponse -> {
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
                if (newUrl == null || newUrl.isEmpty() || this.url.equals(newUrl)) {
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
                this.logger.error(1103, ConfigCatLogMessages.getFetchFailedDueToUnexpectedError(fetchResponse.cfRayId()), exception);
                return CompletableFuture.completedFuture(fetchResponse);
            }

            this.logger.error(1104, ConfigCatLogMessages.getFetchFailedDueToRedirectLoop(fetchResponse.cfRayId()));
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
                Object message = ConfigCatLogMessages.getFetchFailedDueToUnexpectedError(null);
                if (!isClosed.get()) {
                    if (e instanceof SocketTimeoutException) {
                        logEventId = 1102;
                        message = ConfigCatLogMessages.getFetchFailedDueToRequestTimeout(httpClient.connectTimeoutMillis(), httpClient.readTimeoutMillis(), httpClient.writeTimeoutMillis(), null);
                    }
                    logger.error(logEventId, message, e);
                }
                future.complete(FetchResponse.failed(message, false, null, true));
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                String cfRayId = null;
                FetchResponse fetchResponse = null;
                try (ResponseBody body = response.body()) {
                    cfRayId = response.header("CF-RAY");
                    if (response.code() == 200) {
                        String content = body != null ? body.string() : null;
                        String eTag = response.header("ETag");
                        Result<Config> result = deserializeConfig(content, cfRayId);
                        if (result.error() != null) {
                            fetchResponse = FetchResponse.failed(result.error(), false, cfRayId, false);
                        } else {
                            fetchResponse = FetchResponse.fetched(new Entry(result.value(), eTag, content, System.currentTimeMillis()), cfRayId);
                            logger.debug("Fetch was successful: new config fetched.");
                        }
                    } else if (response.code() == 304) {
                        fetchResponse = FetchResponse.notModified(cfRayId);
                        if(cfRayId != null) {
                            logger.debug(String.format("Fetch was successful: config not modified. %s", ConfigCatLogMessages.getCFRayIdPostFix(cfRayId)));
                        } else {
                            logger.debug("Fetch was successful: config not modified.");
                        }
                    } else if (response.code() == 403 || response.code() == 404) {
                        FormattableLogMessage message = ConfigCatLogMessages.getFetchFailedDueToInvalidSDKKey(cfRayId);
                        fetchResponse = FetchResponse.failed(message, true, cfRayId, false);
                        logger.error(1100, message);
                    } else {
                        FormattableLogMessage formattableLogMessage = ConfigCatLogMessages.getFetchFailedDueToUnexpectedHttpResponse(response.code(), response.message(), cfRayId);
                        fetchResponse = FetchResponse.failed(formattableLogMessage, false, cfRayId, true);
                        logger.error(1101, formattableLogMessage);
                    }
                } catch (SocketTimeoutException e) {
                    FormattableLogMessage formattableLogMessage = ConfigCatLogMessages.getFetchFailedDueToRequestTimeout(httpClient.connectTimeoutMillis(), httpClient.readTimeoutMillis(), httpClient.writeTimeoutMillis(), cfRayId);
                    fetchResponse = FetchResponse.failed(formattableLogMessage, false, cfRayId, true);
                    logger.error(1102, formattableLogMessage, e);
                } catch (Exception e) {
                    FormattableLogMessage formattableLogMessage = ConfigCatLogMessages.getFetchFailedDueToUnexpectedError(cfRayId);
                    fetchResponse = FetchResponse.failed(formattableLogMessage, false, cfRayId, true);
                    logger.error(1103, formattableLogMessage, e);
                } finally {
                    if(fetchResponse == null) {
                        FormattableLogMessage formattableLogMessage = ConfigCatLogMessages.getFetchFailedDueToUnexpectedError(cfRayId);
                        fetchResponse = FetchResponse.failed(formattableLogMessage,false, cfRayId, false);
                    }
                    future.complete(fetchResponse);
                }
            }
        });

        return future;
    }

    private CompletableFuture<FetchResponse> fetchWithRetryAsync(final String eTag) {
        return this.getResponseAsync(eTag).thenComposeAsync(response -> {
            if (response.shouldRetry()) {
                try {
                    this.httpClient.connectionPool().evictAll();
                    Thread.sleep(RETRY_DELAY_MS);
                    return this.getResponseAsync(eTag);
                } catch (InterruptedException e) {
                    this.logger.error(0, "Thread interrupted.", e);
                    Thread.currentThread().interrupt();
                    return CompletableFuture.completedFuture(response);
                }
            }
            return CompletableFuture.completedFuture(response);
        });
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
        String url = this.url + "/configuration-files/" + this.sdkKey + "/" + Constants.CONFIG_JSON_NAME;
        Request.Builder builder = new Request.Builder()
                .addHeader("X-ConfigCat-UserAgent", "ConfigCat-Java/" + this.mode + "-" + Constants.VERSION);

        if (etag != null && !etag.isEmpty())
            builder.addHeader("If-None-Match", etag);

        return builder.url(url).build();
    }

    private Result<Config> deserializeConfig(String json, String cfRayId) {
        try {
            return Result.success(Utils.deserializeConfig(json));
        } catch (Exception e) {
            FormattableLogMessage message = ConfigCatLogMessages.getFetchReceived200WithInvalidBodyError(cfRayId);
            this.logger.error(1105, message, e);
            return Result.error(message, null);
        }
    }
}
