package com.configcat;

import org.slf4j.Logger;

class ConfigCatLogger {
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

    public void warn(String message) {
        if (this.logLevel.ordinal() <= LogLevel.WARNING.ordinal()) {
            this.logger.warn(message);
        }
    }

    public void error(String message, Exception exception) {
        if (this.configCatHooks != null) this.configCatHooks.invokeOnError(message);
        if (this.logLevel.ordinal() <= LogLevel.ERROR.ordinal()) {
            this.logger.error(message, exception);
        }
    }

    public void error(String message) {
        if (this.configCatHooks != null) this.configCatHooks.invokeOnError(message);
        if (this.logLevel.ordinal() <= LogLevel.ERROR.ordinal()) {
            this.logger.error(message);
        }
    }

    public void info(String message) {
        if (this.logLevel.ordinal() <= LogLevel.INFO.ordinal()) {
            this.logger.info(message);
        }
    }

    public void debug(String message) {
        if (this.logLevel.ordinal() <= LogLevel.DEBUG.ordinal()) {
            this.logger.debug(message);
        }
    }
}
