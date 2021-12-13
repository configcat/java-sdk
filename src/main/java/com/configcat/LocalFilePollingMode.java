package com.configcat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class LocalFilePollingMode extends PollingMode {
    private final File file;
    private final boolean autoReload;

    public LocalFilePollingMode(String filePath, boolean autoReload, boolean isResource) {
        this.file = isResource
                ? new File(getClass().getClassLoader().getResource(filePath).getFile())
                : new File(filePath);
        this.autoReload = autoReload;
    }

    public byte[] read() throws IOException {
        return Files.readAllBytes(this.file.toPath());
    }

    public Path getFilePath() {
        return this.file.toPath();
    }

    public boolean isAutoReload() {
        return autoReload;
    }
}
