package com.configcat;

public class FilterFunctionParameters {

    private final int eventId;

    private final String message;

    private final LogLevel logLevel;

    private final Exception exception;

    public FilterFunctionParameters(int eventId, String message, LogLevel logLevel, Exception exception) {
        this.eventId = eventId;
        this.message = message;
        this.logLevel = logLevel;
        this.exception = exception;
    }

    public FilterFunctionParameters(int eventId, String message, LogLevel logLevel) {
        this(eventId, message, logLevel, null);
    }

    public int getEventId() {
        return eventId;
    }

    public Exception getException() {
        return exception;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

    public String getMessage() {
        return message;
    }
}
