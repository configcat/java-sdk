package com.configcat;

/**
 * The Log Filter Functional Interface provides a custom filter option for the ConfigCat Logger.
 */
@FunctionalInterface
interface LogFilterFunction {

    /**
     * Apply the custom filter option to the ConfigCatLogger.
     *
     * @param logLevel  Event severity level.
     * @param eventId  Event identifier.
     * @param message Message.
     * @param exception The exception object related to the message (if any).
     * @return True to log the event, false will leave out the log.
     */
     boolean apply(LogLevel logLevel, int eventId, String message, Throwable exception);
}
