package com.configcat.override;

import com.configcat.models.Setting;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class OverrideDataSource implements Closeable {
    public Map<String, Setting> getLocalConfiguration() {
        return new HashMap<>();
    }

    @Override
    public void close() throws IOException { }
}

