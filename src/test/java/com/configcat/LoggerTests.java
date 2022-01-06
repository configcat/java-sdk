package com.configcat;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.mockito.Mockito.*;

public class LoggerTests {
    @Test
    public void debug() {
        Logger mockLogger = mock(Logger.class);
        ConfigCatLogger logger = new ConfigCatLogger(mockLogger, LogLevel.DEBUG);

        logger.debug("debug");
        logger.info("info");
        logger.warn("warn");
        logger.error("error");

        verify(mockLogger, times(1)).debug("debug");
        verify(mockLogger, times(1)).error("error");
        verify(mockLogger, times(1)).warn("warn");
        verify(mockLogger, times(1)).info("info");
    }

    @Test
    public void info() {
        Logger mockLogger = mock(Logger.class);
        ConfigCatLogger logger = new ConfigCatLogger(mockLogger, LogLevel.INFO);

        logger.debug("debug");
        logger.info("info");
        logger.warn("warn");
        logger.error("error");

        verify(mockLogger, never()).debug("debug");
        verify(mockLogger, times(1)).error("error");
        verify(mockLogger, times(1)).warn("warn");
        verify(mockLogger, times(1)).info("info");
    }

    @Test
    public void warn() {
        Logger mockLogger = mock(Logger.class);
        ConfigCatLogger logger = new ConfigCatLogger(mockLogger, LogLevel.WARNING);

        logger.debug("debug");
        logger.info("info");
        logger.warn("warn");
        logger.error("error");

        verify(mockLogger, never()).debug("debug");
        verify(mockLogger, never()).info("info");
        verify(mockLogger, times(1)).warn("warn");
        verify(mockLogger, times(1)).error("error");
    }

    @Test
    public void error() {
        Logger mockLogger = mock(Logger.class);
        ConfigCatLogger logger = new ConfigCatLogger(mockLogger, LogLevel.ERROR);

        logger.debug("debug");
        logger.info("info");
        logger.warn("warn");
        logger.error("error");

        verify(mockLogger, never()).debug("debug");
        verify(mockLogger, never()).info("info");
        verify(mockLogger, never()).warn("warn");
        verify(mockLogger, times(1)).error("error");
    }

    @Test
    public void noLog() {
        Logger mockLogger = mock(Logger.class);
        ConfigCatLogger logger = new ConfigCatLogger(mockLogger, LogLevel.NO_LOG);

        logger.debug("debug");
        logger.info("info");
        logger.warn("warn");
        logger.error("error");

        verify(mockLogger, never()).debug("debug");
        verify(mockLogger, never()).info("info");
        verify(mockLogger, never()).warn("warn");
        verify(mockLogger, never()).error("error");
    }
}
