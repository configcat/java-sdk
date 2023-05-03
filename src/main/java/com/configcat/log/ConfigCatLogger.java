package com.configcat.log;

import com.configcat.hooks.ConfigCatHooks;
import org.slf4j.Logger;

public class ConfigCatLogger {
    private final Logger logger;
    private final LogLevel logLevel;
    private final ConfigCatHooks configCatHooks;

    public ConfigCatLogger(Logger logger, LogLevel logLevel, ConfigCatHooks configCatHooks) {
        this.logger = logger;
        this.logLevel = logLevel;
        this.configCatHooks = configCatHooks;
    }

    public ConfigCatLogger(Logger logger, LogLevel logLevel) {
        this(logger, logLevel, null);
    }

    public ConfigCatLogger(Logger logger) {
        this(logger, LogLevel.WARNING);
    }

    public void warn(int eventId, String message) {
        if (this.logLevel.ordinal() <= LogLevel.WARNING.ordinal()) {
            this.logger.warn("[{}] {}", eventId, message);
        }
    }

    public void error(int eventId, String message, Exception exception) {
        if (this.configCatHooks != null) this.configCatHooks.invokeOnError(message);
        if (this.logLevel.ordinal() <= LogLevel.ERROR.ordinal()) {
            this.logger.error("[{}] {}", eventId, message, exception);
        }
    }

    public void error(int eventId, String message) {
        if (this.configCatHooks != null) this.configCatHooks.invokeOnError(message);
        if (this.logLevel.ordinal() <= LogLevel.ERROR.ordinal()) {
            this.logger.error("[{}] {}", eventId, message);
        }
    }

    public void info(int eventId, String message) {
        if (this.logLevel.ordinal() <= LogLevel.INFO.ordinal()) {
            this.logger.info("[{}] {}", eventId, message);
        }
    }

    public void debug(String message) {
        if (this.logLevel.ordinal() <= LogLevel.DEBUG.ordinal()) {
            this.logger.debug("[{}] {}", 0, message);
        }
    }
}
