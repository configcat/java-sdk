package com.configcat;

import org.slf4j.Logger;

import java.util.function.Function;

class ConfigCatLogger {
    private final Logger logger;
    private final LogLevel logLevel;
    private final ConfigCatHooks configCatHooks;
    private final Function<FilterFunctionParameters, Boolean> filterOutFunction;

    public ConfigCatLogger(Logger logger, LogLevel logLevel, ConfigCatHooks configCatHooks, Function<FilterFunctionParameters, Boolean> filterOutFunction) {
        this.logger = logger;
        this.logLevel = logLevel;
        this.configCatHooks = configCatHooks;
        this.filterOutFunction = filterOutFunction;
    }

    public ConfigCatLogger(Logger logger, LogLevel logLevel) {
        this(logger, logLevel, null, null);
    }

    public ConfigCatLogger(Logger logger) {
        this(logger, LogLevel.WARNING);
    }

    public void warn(int eventId, String message) {
        if (this.logLevel.ordinal() <= LogLevel.WARNING.ordinal() && !checkFilterOut(new FilterFunctionParameters(eventId, message, LogLevel.WARNING))) {
            this.logger.warn("[{}] {}", eventId, message);
        }
    }

    public void error(int eventId, String message, Exception exception) {
        if (this.configCatHooks != null) this.configCatHooks.invokeOnError(message);
        if (this.logLevel.ordinal() <= LogLevel.ERROR.ordinal() && !checkFilterOut(new FilterFunctionParameters(eventId, message, LogLevel.ERROR, exception))) {
            this.logger.error("[{}] {}", eventId, message, exception);
        }
    }

    public void error(int eventId, String message) {
        if (this.configCatHooks != null) this.configCatHooks.invokeOnError(message);
        if (this.logLevel.ordinal() <= LogLevel.ERROR.ordinal() && !checkFilterOut(new FilterFunctionParameters(eventId, message, LogLevel.ERROR))) {
            this.logger.error("[{}] {}", eventId, message);
        }
    }

    public void info(int eventId, String message) {
        if (this.logLevel.ordinal() <= LogLevel.INFO.ordinal() && !checkFilterOut(new FilterFunctionParameters(eventId, message, LogLevel.INFO))) {
            this.logger.info("[{}] {}", eventId, message);
        }
    }

    public void debug(String message) {
        if (this.logLevel.ordinal() <= LogLevel.DEBUG.ordinal() && !checkFilterOut(new FilterFunctionParameters(0, message, LogLevel.DEBUG))) {
            this.logger.debug("[{}] {}", 0, message);
        }
    }

    private boolean checkFilterOut(FilterFunctionParameters parameters) {
        return this.filterOutFunction != null && this.filterOutFunction.apply(parameters);
    }
}
