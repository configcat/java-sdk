package com.configcat;

import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

class ConfigFetcher implements Closeable {

    private static final long RETRY_DELAY_MS = 50;

    private static final long EVICT_ALL_THRESHOLD_NS = 30_000_000_000L; // 30 seconds in nanoseconds

    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final ConfigCatLogger logger;
    private final OkHttpClient httpClient;
    private final String mode;

    private long lastEvictAllTimestamp = Long.MIN_VALUE;

    private final String sdkKey;
    private final boolean urlIsCustom;
    private final boolean isDebugLoggingEnabled;

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
        this.isDebugLoggingEnabled = logger.isEnabled(LogLevel.DEBUG);
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

    private CompletableFuture<FetchResponse> getResponseAsync(final String eTag, final UUID requestId) {
        Request request = this.getRequest(eTag);
        CompletableFuture<FetchResponse> future = new CompletableFuture<>();
        if(isDebugLoggingEnabled) {
            String proxyUri = getProxyAddress();
            if (proxyUri == null) {
                this.logger.debug(ConfigCatLogMessages.getDebugEnabledRequestWillBeSent(requestId,request.url().toString(),request.header("If-None-Match")));
            } else {
                this.logger.debug(ConfigCatLogMessages.getDebugEnabledRequestWillBeSentViaProxy(requestId, proxyUri, request.url().toString(),request.header("If-None-Match")));
            }
        }

        this.httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                FetchResponse fetchResponse = null;
                try{
                    if (isDebugLoggingEnabled){
                        logger.debug(ConfigCatLogMessages.getDebugEnabledRequestFailed(requestId));
                    }
                    int logEventId = 1103;
                    Object message = ConfigCatLogMessages.getFetchFailedDueToUnexpectedError(null);
                    if (!isClosed.get()) {
                        if (e instanceof SocketTimeoutException) {
                            logEventId = 1102;
                            message = ConfigCatLogMessages.getFetchFailedDueToRequestTimeout(httpClient.connectTimeoutMillis(), httpClient.readTimeoutMillis(), httpClient.writeTimeoutMillis(), null);
                        }
                        logger.error(logEventId, message, e);
                    }
                    fetchResponse =  FetchResponse.failed(message, false, null, true);
                } finally {
                    if(fetchResponse == null) {
                        FormattableLogMessage formattableLogMessage = ConfigCatLogMessages.getFetchFailedDueToUnexpectedError(null);
                        fetchResponse = FetchResponse.failed(formattableLogMessage,false, null, false);
                    }
                    future.complete(fetchResponse);
                }
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                String cfRayId = null;
                FetchResponse fetchResponse = null;
                try (ResponseBody body = response.body()) {
                    cfRayId = response.header("CF-RAY");
                    int responseCode = response.code();
                    String eTag = response.header("ETag");
                    if (isDebugLoggingEnabled) {
                        logger.debug(ConfigCatLogMessages.getDebugEnabledReceivedHeaders(requestId, String.valueOf(responseCode), response.message(), eTag));
                    }
                    if (responseCode == 200) {
                        String content = body != null ? body.string() : null;
                        if (isDebugLoggingEnabled && content != null) {

                            logger.debug(ConfigCatLogMessages.getDebugEnabledReceivedBody(requestId, content.length()));
                        }
                        Result<Config> result = deserializeConfig(content, cfRayId);
                        if (result.error() != null) {
                            fetchResponse = FetchResponse.failed(result.error(), false, cfRayId, false);
                        } else {
                            fetchResponse = FetchResponse.fetched(new Entry(result.value(), eTag, content, System.currentTimeMillis()), cfRayId);
                            logger.debug("Fetch was successful: new config fetched.");
                        }
                    } else if (responseCode == 304) {
                        fetchResponse = FetchResponse.notModified(cfRayId);
                        if(cfRayId != null && isDebugLoggingEnabled) {
                            logger.debug(String.format("Fetch was successful: config not modified. %s", ConfigCatLogMessages.getCFRayIdPostFix(cfRayId)));
                        } else {
                            logger.debug("Fetch was successful: config not modified.");
                        }
                    } else if (responseCode == 403 || responseCode == 404) {
                        FormattableLogMessage message = ConfigCatLogMessages.getFetchFailedDueToInvalidSDKKey(cfRayId);
                        fetchResponse = FetchResponse.failed(message, true, cfRayId, false);
                        logger.error(1100, message);
                    } else {
                        if (isDebugLoggingEnabled){
                          logger.debug(ConfigCatLogMessages.getDebugEnabledReceivedUnexpectedStatusCode(requestId));
                        }
                        FormattableLogMessage formattableLogMessage = ConfigCatLogMessages.getFetchFailedDueToUnexpectedHttpResponse(responseCode, response.message(), cfRayId);
                        fetchResponse = FetchResponse.failed(formattableLogMessage, false, cfRayId, true);
                        logger.error(1101, formattableLogMessage);
                    }
                } catch (SocketTimeoutException e) {
                    if (isDebugLoggingEnabled) {
                        logger.debug(ConfigCatLogMessages.getDebugEnabledRequestTimedOut(requestId));
                    }
                    FormattableLogMessage formattableLogMessage = ConfigCatLogMessages.getFetchFailedDueToRequestTimeout(httpClient.connectTimeoutMillis(), httpClient.readTimeoutMillis(), httpClient.writeTimeoutMillis(), cfRayId);
                    fetchResponse = FetchResponse.failed(formattableLogMessage, false, cfRayId, true);
                    logger.error(1102, formattableLogMessage, e);
                } catch (Exception e) {
                    if (isDebugLoggingEnabled) {
                        logger.debug(ConfigCatLogMessages.getDebugEnabledRequestFailed(requestId));
                    }
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
        UUID requestId;
        if(isDebugLoggingEnabled){
            requestId = UUID.randomUUID();
            this.logger.debug(ConfigCatLogMessages.getDebugEnabledPreparingRequest(requestId));
        } else {
            requestId = null;
        }

        return this.getResponseAsync(eTag, requestId).thenComposeAsync(response -> {
            if (response.shouldRetry()) {
                try {
                    long now = System.nanoTime();
                    if (lastEvictAllTimestamp == Long.MIN_VALUE || (now - lastEvictAllTimestamp) >= EVICT_ALL_THRESHOLD_NS) {
                        this.httpClient.connectionPool().evictAll();
                        lastEvictAllTimestamp = now;
                        if (isDebugLoggingEnabled){
                            this.logger.debug(ConfigCatLogMessages.getDebugEnabledResetConnectionPool(requestId));
                        }
                    }
                    Thread.sleep(RETRY_DELAY_MS);
                    if (isDebugLoggingEnabled) {
                        this.logger.debug(ConfigCatLogMessages.getDebugEnabledReTryRequest(requestId));
                    }
                    return this.getResponseAsync(eTag, requestId);
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

    /**
     * Returns the proxy address if a proxy is configured for the OkHttpClient, otherwise returns null.
     *
     * @return the proxy address or null if no proxy is configured or bypassed.
     */
    private String getProxyAddress() {
        Proxy proxy = this.httpClient.proxy();
        if (proxy == null || proxy.type() == Proxy.Type.DIRECT) {
            return null;
        }

        return proxy.toString();
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
