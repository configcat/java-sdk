package com.configcat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

class ConfigFetcher implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigFetcher.class);
    private static final String CONFIG_JSON_NAME = "config_v5.json";

    private final JsonParser parser = new JsonParser();
    private final OkHttpClient httpClient;
    private final String mode;
    private final String version;
    private final String apiKey;
    private final boolean urlIsCustom;

    private String url;
    private String eTag;

    ConfigFetcher(OkHttpClient httpClient,
                  String apiKey,
                  String url,
                  boolean urlIsCustom,
                  String pollingIdentifier) {
        this.apiKey = apiKey;
        this.urlIsCustom = urlIsCustom;
        this.url = url;
        this.httpClient = httpClient;
        this.version = this.getClass().getPackage().getImplementationVersion();
        this.mode = pollingIdentifier;
    }

    public CompletableFuture<FetchResponse> getConfigurationJsonStringAsync() {
        return this.executeFetchAsync(2);
    }

    private CompletableFuture<FetchResponse> executeFetchAsync(int executionCount) {
        return this.getResponseAsync().thenComposeAsync(fetchResponse -> {
            if(!fetchResponse.isFetched()) {
                return CompletableFuture.completedFuture(fetchResponse);
            }
            try {
                JsonObject json = parser.parse(fetchResponse.config()).getAsJsonObject();
                JsonObject preferences = json.getAsJsonObject(Config.Preferences);
                if(preferences == null) {
                    return CompletableFuture.completedFuture(fetchResponse);
                }

                String newUrl = preferences.get(Preferences.BaseUrl).getAsString();
                if(newUrl == null || newUrl.isEmpty() || newUrl.equals(this.url)) {
                    return CompletableFuture.completedFuture(fetchResponse);
                }

                int redirect = preferences.get(Preferences.Redirect).getAsInt();

                // we have a custom url set and we didn't get a forced redirect
                if(this.urlIsCustom && redirect != 2) {
                    return CompletableFuture.completedFuture(fetchResponse);
                }

                this.url = newUrl;

                if(redirect == 0) { // no redirect
                    return CompletableFuture.completedFuture(fetchResponse);
                } else { // redirect
                    if (redirect == 1) {
                        LOGGER.warn("Please check the data_governance parameter in the ConfigCatClient initialization. " +
                        "It should match the settings provided in " +
                        "https://app.configcat.com/organization/data-governance. " +
                        "If you are not allowed to view this page, ask your Organization's Admins " +
                        "for the correct setting.");
                    }

                    if(executionCount > 0) {
                        return this.executeFetchAsync(executionCount - 1);
                    }
                }

            } catch (Exception exception) {
                LOGGER.error("Exception in ConfigFetcher.executeFetchAsync", exception);
            }

            return CompletableFuture.completedFuture(fetchResponse);
        });
    }

    private CompletableFuture<FetchResponse> getResponseAsync() {
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
                    LOGGER.error("Exception in ConfigFetcher.getResponseAsync", e);
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
        String url = this.url + "/configuration-files/" + this.apiKey + "/" + CONFIG_JSON_NAME;
        Request.Builder builder =  new Request.Builder()
                .addHeader("X-ConfigCat-UserAgent", "ConfigCat-Java/"+ this.mode + "-" + this.version);

        if(this.eTag != null)
            builder.addHeader("If-None-Match", this.eTag);

        return builder.url(url).build();
    }
}

