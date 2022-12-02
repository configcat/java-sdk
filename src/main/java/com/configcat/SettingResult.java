package com.configcat;

import java.util.Map;

/**
 * Represents the result of a getSettings() call.
 */
class SettingResult {
    private final Map<String, Setting> settings;
    private final long fetchTime;

    public SettingResult(Map<String, Setting> settings, long fetchTime) {
        this.settings = settings;
        this.fetchTime = fetchTime;
    }

    public Map<String, Setting> settings() {
        return settings;
    }

    public long fetchTime() {
        return fetchTime;
    }
}