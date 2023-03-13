package com.configcat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

class LocalFileDataSource extends OverrideDataSource {

    private final ConfigCatLogger logger;
    private final Gson gson = new GsonBuilder().create();
    private Map<String, Setting> loadedSettings = new HashMap<>();
    private final FileWatcher watcher;
    private final String filePath;
    private final boolean isResource;

    public LocalFileDataSource(String filePath, boolean isResource, ConfigCatLogger logger, boolean autoReload) {
        this.isResource = isResource;
        this.logger = logger;
        this.filePath = filePath;

        this.logger.debug("Reading " + this.filePath + " for local overrides.");

        this.reloadFileContent();

        FileWatcher fileWatcher = null;
        if (autoReload && !isResource) {
            Path path = Paths.get(filePath);
            try {
                fileWatcher = FileWatcher.create(Paths.get(filePath));
                fileWatcher.start(this::reloadFileContent);
            } catch (IOException e) {
                this.logger.error(1300, "Cannot find the local config file '" + path + "'. This is a path that your application provided to the ConfigCat SDK by passing it to the `OverrideDataSourceBuilder.localFile()` method. Read more: https://configcat.com/docs/sdk-reference/java/#json-file", e);
            }
        }
        this.watcher = fileWatcher;
    }

    @Override
    public Map<String, Setting> getLocalConfiguration() {
        return this.loadedSettings;
    }

    @Override
    public void close() throws IOException {
        if (this.watcher != null) {
            this.watcher.stop();
        }
    }

    private void reloadFileContent() {
        String content = null;
        try {
            content = this.readFile();
        } catch (IOException e) {
            this.logger.error(1302, "Failed to read the local config file '" + this.filePath + "'.", e);
        }

        if (content != null && !content.isEmpty()) {
            SimplifiedConfig simplifiedConfig = this.gson.fromJson(content, SimplifiedConfig.class);
            if (simplifiedConfig != null && simplifiedConfig.entries != null && simplifiedConfig.entries.size() > 0) {
                for (Map.Entry<String, JsonElement> entry : simplifiedConfig.entries.entrySet()) {
                    Setting setting = new Setting();
                    setting.setValue(entry.getValue());
                    this.loadedSettings.put(entry.getKey(), setting);
                }

                return;
            }

            Config config = this.gson.fromJson(content, Config.class);
            this.loadedSettings = config != null ? config.getEntries() : new HashMap<>();
        }
    }

    private String readFile() throws IOException {
        if (this.isResource) {
            try (InputStream stream = getClass().getClassLoader().getResourceAsStream(this.filePath)) {
                if (stream == null) {
                    throw new IOException();
                }
                byte[] buffer = new byte[4096];
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                int temp;
                while ((temp = stream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, temp);
                }
                return new String(outputStream.toByteArray(), Charset.defaultCharset());
            }
        } else {
            byte[] content = Files.readAllBytes(Paths.get(this.filePath));
            return new String(content, Charset.defaultCharset());
        }
    }

    private static final class FileWatcher implements Runnable {
        private final WatchService watchService;
        private final Thread watchThread;
        private volatile boolean interrupted;
        private Runnable onModifiedAction;
        private final Path filePath;

        public static FileWatcher create(Path filePath) throws IOException {
            WatchService watchService = FileSystems.getDefault().newWatchService();
            Path path = filePath.getParent();
            path.register(watchService, ENTRY_MODIFY);
            return new FileWatcher(watchService, filePath);
        }

        private FileWatcher(WatchService watchService, Path filePath) {
            this.watchService = watchService;
            this.filePath = filePath;

            this.watchThread = new Thread(this, FileWatcher.class.getName());
            this.watchThread.setDaemon(true);
        }

        @Override
        public void run() {
            while (!this.interrupted) {
                try {
                    WatchKey key = this.watchService.take();
                    for (WatchEvent<?> event : key.pollEvents()) {
                        Path changed = (Path) event.context();
                        if (changed.endsWith(this.filePath.getFileName())) {
                            this.onModifiedAction.run();
                        }
                    }
                    key.reset();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        public void start(Runnable onModifiedAction) {
            this.onModifiedAction = onModifiedAction;
            this.watchThread.start();
        }

        public void stop() {
            this.interrupted = true;
            this.watchThread.interrupt();
        }
    }

    private static class SimplifiedConfig {
        @SerializedName("flags")
        public Map<String, JsonElement> entries;
    }
}
