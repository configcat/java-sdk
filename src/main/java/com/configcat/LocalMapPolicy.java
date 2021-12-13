package com.configcat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

class LocalMapPolicy implements RefreshPolicy  {
    private final Config loadedConfig;

    public LocalMapPolicy(LocalMapPollingMode configuration) {
        this.loadedConfig = new Config();
        Gson gson = new GsonBuilder().create();
        this.loadedConfig.entries = new HashMap<>();
        for (Map.Entry<String, Object> entry : configuration.getSource().entrySet()) {
            Setting setting = new Setting();
            setting.value = gson.toJsonTree(entry.getValue());
            this.loadedConfig.entries.put(entry.getKey(), setting);
        }
    }

    @Override
    public CompletableFuture<Config> getConfigurationAsync() {
        return CompletableFuture.completedFuture(this.loadedConfig);
    }

    @Override
    public CompletableFuture<Void> refreshAsync() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void close() throws IOException { }
}
