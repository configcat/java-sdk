package com.configcat;

import com.configcat.models.Setting;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class OverrideDataSource implements Closeable {
    public Map<String, Setting> getLocalConfiguration() {
        return new HashMap<>();
    }

    @Override
    public void close() throws IOException {
        /* Default OverrideDataSource has nothing to close. */
    }
}

