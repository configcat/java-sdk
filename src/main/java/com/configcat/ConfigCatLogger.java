package com.configcat;

import org.slf4j.Logger;

import java.util.List;

class ConfigCatLogger {
    private final Logger logger;
    private final LogLevel logLevel;
    private final ConfigCatHooks configCatHooks;
    private final List<Integer> excludeEventIds;

    public ConfigCatLogger(Logger logger, LogLevel logLevel, ConfigCatHooks configCatHooks, List<Integer> excludeEventIds) {
        this.logger = logger;
        this.logLevel = logLevel;
        this.configCatHooks = configCatHooks;
        this.excludeEventIds = excludeEventIds;
    }

    public ConfigCatLogger(Logger logger, LogLevel logLevel) {
        this(logger, logLevel, null, null);
    }

    public ConfigCatLogger(Logger logger) {
        this(logger, LogLevel.WARNING);
    }

    public void warn(int eventId, String message) {
        if (this.logLevel.ordinal() <= LogLevel.WARNING.ordinal() && !checkExcludeEventId(eventId)) {
            this.logger.warn("[{}] {}", eventId, message);
        }
    }

    public void error(int eventId, String message, Exception exception) {
        if (this.configCatHooks != null) this.configCatHooks.invokeOnError(message);
        if (this.logLevel.ordinal() <= LogLevel.ERROR.ordinal() && !checkExcludeEventId(eventId)) {
            this.logger.error("[{}] {}", eventId, message, exception);
        }
    }

    public void error(int eventId, String message) {
        if (this.configCatHooks != null) this.configCatHooks.invokeOnError(message);
        if (this.logLevel.ordinal() <= LogLevel.ERROR.ordinal() && !checkExcludeEventId(eventId)) {
            this.logger.error("[{}] {}", eventId, message);
        }
    }

    public void info(int eventId, String message) {
        if (this.logLevel.ordinal() <= LogLevel.INFO.ordinal() && !checkExcludeEventId(eventId)) {
            this.logger.info("[{}] {}", eventId, message);
        }
    }

    public void debug(String message) {
        if (this.logLevel.ordinal() <= LogLevel.DEBUG.ordinal()) {
            this.logger.debug("[{}] {}", 0, message);
        }
    }

    private boolean checkExcludeEventId(int eventId) {
        return this.excludeEventIds != null && this.excludeEventIds.contains(eventId);
    }
}
