package com.configcat;

import java.util.Map;

class LocalMapPollingMode extends PollingMode {
    private final Map<String, Object> source;

    public LocalMapPollingMode(Map<String, Object> source) {
        if (source == null)
            throw new IllegalArgumentException("'source' cannot be null.");

        this.source = source;
    }

    public Map<String, Object> getSource() {
        return source;
    }
}
