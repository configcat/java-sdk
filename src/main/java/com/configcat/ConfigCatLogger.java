package com.configcat;

import org.slf4j.Logger;

class ConfigCatLogger {
    private final Logger logger;
    private final LogLevel logLevel;
    private final ConfigCatHooks configCatHooks;
    private final LogFilterFunction filterFunction ;

    public ConfigCatLogger(Logger logger, LogLevel logLevel, ConfigCatHooks configCatHooks, LogFilterFunction filterFunction ) {
        this.logger = logger;
        this.logLevel = logLevel;
        this.configCatHooks = configCatHooks;
        this.filterFunction  = filterFunction ;
    }

    public ConfigCatLogger(Logger logger, LogLevel logLevel) {
        this(logger, logLevel, null, null);
    }

    public ConfigCatLogger(Logger logger) {
        this(logger, LogLevel.WARNING);
    }

    public void warn(int eventId, Object message) {
        if (filter(eventId,  LogLevel.WARNING, message, null)) {
            this.logger.warn("[{}] {}", eventId, message);
        }
    }

    public void error(int eventId, Object message, Exception exception) {
        if (this.configCatHooks != null) this.configCatHooks.invokeOnError(message);
        if (filter(eventId,  LogLevel.ERROR, message, exception)) {
            this.logger.error("[{}] {}", eventId, message, exception);
        }
    }

    public void error(int eventId, Object message) {
        if (this.configCatHooks != null) this.configCatHooks.invokeOnError(message);
        if (filter(eventId,  LogLevel.ERROR, message, null)) {
            this.logger.error("[{}] {}", eventId, message);
        }
    }

    public void info(int eventId, Object message) {
        if (filter(eventId,  LogLevel.INFO, message, null)) {
            this.logger.info("[{}] {}", eventId, message);
        }
    }

    public void debug(Object message) {
        if (filter(0,  LogLevel.DEBUG, message, null)) {
            this.logger.debug("[{}] {}", 0, message);
        }
    }

    private boolean filter(int eventId, LogLevel logLevel, Object message, Exception exception) {
        return this.logLevel.ordinal() <= logLevel.ordinal() && (this.filterFunction == null || this.filterFunction.apply(logLevel, eventId, message, exception));
    }
}
