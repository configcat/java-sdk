package com.configcat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.Map;

class LocalMapDataSource extends OverrideDataSource {
    private final Map<String, Setting> loadedSettings = new HashMap<>();

    public LocalMapDataSource(Map<String, Object> source) {
        if (source == null)
            throw new IllegalArgumentException("'source' cannot be null.");

        Gson gson = new GsonBuilder().create();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            Setting setting = new Setting();
            setting.value = gson.toJsonTree(entry.getValue());
            this.loadedSettings.put(entry.getKey(), setting);
        }
    }

    @Override
    public Map<String, Setting> getLocalConfiguration() {
        return this.loadedSettings;
    }
}
