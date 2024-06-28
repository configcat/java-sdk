package com.configcat;

@FunctionalInterface
interface LogFilterFunction {

    /**
     *
     * @param eventId
     * @param logLevel
     * @param message
     * @param exception
     * @return
     */
     boolean apply(int eventId, LogLevel logLevel , String message, Throwable exception);
}
