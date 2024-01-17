package com.configcat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
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
                fileWatcher = FileWatcher.create(path);
                fileWatcher.start(this::reloadFileContent);
            } catch (IOException e) {
                this.logger.error(1300, ConfigCatLogMessages.getLocalFileDataSourceDoesNotExist(path.toString()), e);
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
            this.logger.error(1302, ConfigCatLogMessages.getLocalFileDataSourceFailedToReadFile(this.filePath), e);
        }

        if (content != null && !content.isEmpty()) {
            SimplifiedConfig simplifiedConfig = this.gson.fromJson(content, SimplifiedConfig.class);
            if (simplifiedConfig != null && simplifiedConfig.entries != null && simplifiedConfig.entries.size() > 0) {
                for (Map.Entry<String, JsonElement> entry : simplifiedConfig.entries.entrySet()) {
                    Setting setting = convertJsonToSettingsValue(entry.getValue());
                    this.loadedSettings.put(entry.getKey(), setting);
                }

                return;
            }
            Config config = Utils.deserializeConfig(content);
            this.loadedSettings = config != null ? config.getEntries() : new HashMap<>();
        }
    }

    private Setting convertJsonToSettingsValue(JsonElement jsonElement) {
        Setting setting = new Setting();
        SettingsValue settingsValue = new SettingsValue();
        SettingType settingType;
        if (!jsonElement.isJsonPrimitive()) {
            throw new IllegalArgumentException("Invalid Config Json content: " + jsonElement);
        }
        JsonPrimitive primitive = jsonElement.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            settingsValue.setBooleanValue(primitive.getAsBoolean());
            settingType = SettingType.BOOLEAN;
        } else if (primitive.isString()) {
            settingsValue.setStringValue(primitive.getAsString());
            settingType = SettingType.STRING;
        } else {
            // primitive should be a number, try to cast int to see its not a double
            String numberAsSting = primitive.getAsString();
            try {
                settingsValue.setIntegerValue(Integer.parseInt(numberAsSting));
                settingType = SettingType.INT;
            } catch (NumberFormatException e) {
                // if int parse failed try double parse
                settingsValue.setDoubleValue(Double.parseDouble(numberAsSting));
                settingType = SettingType.DOUBLE;
            }
        }

        setting.setSettingsValue(settingsValue);
        setting.setType(settingType);
        return setting;
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
