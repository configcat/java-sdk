package com.configcat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

class LocalPolicy implements RefreshPolicy {
    private final LocalPollingMode configuration;
    private final ConfigCatLogger logger;
    private final Gson gson = new GsonBuilder().create();
    private Config loadedConfig;
    private final FileWatcher watcher;

    public LocalPolicy(LocalPollingMode configuration, ConfigCatLogger logger) {
        this.configuration = configuration;
        this.logger = logger;
        this.reloadFileContent();

        FileWatcher fileWatcher = null;
        if (configuration.isAutoReload()) {
            try {
                fileWatcher = FileWatcher.create(configuration.getFilePath());
                fileWatcher.start(this::reloadFileContent);
            } catch (IOException e) {
                this.logger.error("Error during initializing file watcher on " + this.configuration.getFilePath().toString() + ".", e);
            }
        }
        this.watcher = fileWatcher;
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
            this.logger.error("Error during reading " + this.configuration.getFilePath().toString() + ".", e);
        }

        if (content != null && !content.isEmpty()) {
            SimplifiedConfig simplifiedConfig = this.gson.fromJson(content, SimplifiedConfig.class);
            if (simplifiedConfig != null && simplifiedConfig.Entries != null && simplifiedConfig.Entries.size() > 0) {
                this.loadedConfig = new Config();
                this.loadedConfig.Entries = new HashMap<>();
                for (Map.Entry<String, JsonElement> entry : simplifiedConfig.Entries.entrySet()) {
                    Setting setting = new Setting();
                    setting.Value = entry.getValue();
                    this.loadedConfig.Entries.put(entry.getKey(), setting);
                }

                return;
            }

            this.loadedConfig = this.gson.fromJson(content, Config.class);
        }
    }

    private String readFile() throws IOException {
        byte[] content = this.configuration.read();
        return new String(content, Charset.defaultCharset());
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
                } catch (InterruptedException ignored) {
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
        public Map<String, JsonElement> Entries;
    }
}
