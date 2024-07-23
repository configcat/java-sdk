package com.configcat;

/**
 * The Log Filter Functional Interface provides a custom filter option for the ConfigCat Logger.
 */
@FunctionalInterface
public interface LogFilterFunction {

    /**
     * Apply the custom filter option to the ConfigCatLogger.
     *
     * @param logLevel  Event severity level.
     * @param eventId  Event identifier.
     * @param message Message object. The formatted message String can be obtained by calling {@code toString}. (It is guaranteed that the message string is built only once even if {@code toString} called multiple times.)
     * @param exception The exception object related to the message (if any).
     * @return True to log the event, false will leave out the log.
     */
     boolean apply(LogLevel logLevel, int eventId, Object message, Throwable exception);
}
