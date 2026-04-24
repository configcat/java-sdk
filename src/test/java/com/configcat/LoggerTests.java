package com.configcat;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class LoggerTests {
    @Test
    public void debug() {
        Logger mockLogger = mock(Logger.class);
        ConfigCatLogger logger = new ConfigCatLogger(mockLogger, LogLevel.DEBUG);

        logger.debug("debug");
        logger.info(5000, "info");
        logger.warn(3000, "warn");
        logger.error(1000, "error", new Exception());

        verify(mockLogger, times(1)).debug(anyString(), eq(0), eq("debug"));
        verify(mockLogger, times(1)).error(anyString(), eq(1000), eq("error"), any(Exception.class));
        verify(mockLogger, times(1)).warn(anyString(), eq(3000), eq("warn"));
        verify(mockLogger, times(1)).info(anyString(), eq(5000), eq("info"));
    }

    @Test
    public void info() {
        Logger mockLogger = mock(Logger.class);
        ConfigCatLogger logger = new ConfigCatLogger(mockLogger, LogLevel.INFO);

        logger.debug("debug");
        logger.info(5000, "info");
        logger.warn(3000, "warn");
        logger.error(1000, "error", new Exception());

        verify(mockLogger, never()).debug(anyString(), eq(0), eq("debug"));
        verify(mockLogger, times(1)).error(anyString(), eq(1000), eq("error"), any(Exception.class));
        verify(mockLogger, times(1)).warn(anyString(), eq(3000), eq("warn"));
        verify(mockLogger, times(1)).info(anyString(), eq(5000), eq("info"));
    }

    @Test
    public void warn() {
        Logger mockLogger = mock(Logger.class);
        ConfigCatLogger logger = new ConfigCatLogger(mockLogger, LogLevel.WARNING);

        logger.debug("debug");
        logger.info(5000, "info");
        logger.warn(3000, "warn");
        logger.error(1000, "error", new Exception());

        verify(mockLogger, never()).debug(anyString(), eq(0), eq("debug"));
        verify(mockLogger, never()).info(anyString(), eq(5000), eq("info"));
        verify(mockLogger, times(1)).warn(anyString(), eq(3000), eq("warn"));
        verify(mockLogger, times(1)).error(anyString(), eq(1000), eq("error"), any(Exception.class));
    }

    @Test
    public void error() {
        Logger mockLogger = mock(Logger.class);
        ConfigCatLogger logger = new ConfigCatLogger(mockLogger, LogLevel.ERROR);

        logger.debug("debug");
        logger.info(5000, "info");
        logger.warn(3000, "warn");
        logger.error(1000, "error", new Exception());

        verify(mockLogger, never()).debug(anyString(), eq(0), eq("debug"));
        verify(mockLogger, never()).info(anyString(), eq(5000), eq("info"));
        verify(mockLogger, never()).warn(anyString(), eq(3000), eq("warn"));
        verify(mockLogger, times(1)).error(anyString(), eq(1000), eq("error"), any(Exception.class));


    }

    @Test
    public void noLog() {
        Logger mockLogger = mock(Logger.class);
        ConfigCatLogger logger = new ConfigCatLogger(mockLogger, LogLevel.NO_LOG);

        logger.debug("[0] debug");
        logger.info(5000, "info");
        logger.warn(3000, "warn");
        logger.error(1000, "error", new Exception());

        verify(mockLogger, never()).debug(anyString(), eq(0), eq("debug"));
        verify(mockLogger, never()).info(anyString(), eq(5000), eq("info"));
        verify(mockLogger, never()).warn(anyString(), eq(3000), eq("warn"));
        verify(mockLogger, never()).error(anyString(), eq(1000), eq("error"), any(Exception.class));
    }

    @Test
    public void excludeLogEvents() {
        Logger mockLogger = mock(Logger.class);

        LogFilterFunction filterLogFunction = ( LogLevel logLevel, int eventId, Object message, Throwable exception) -> eventId != 1001 && eventId != 3001 && eventId != 5001 && !message.toString().contains("error");

        ConfigCatLogger logger = new ConfigCatLogger(mockLogger, LogLevel.INFO, null, filterLogFunction);

        logger.debug("[0] debug");
        logger.info(5000, "info");
        logger.warn(3000, "warn");
        logger.error(1000, "error", new Exception());
        logger.info(5001, "info");
        logger.warn(3001, "warn");
        logger.error(1001, "error", new Exception());

        verify(mockLogger, never()).debug(anyString(), eq(0), eq("debug"));
        verify(mockLogger, times(1)).info(anyString(), eq(5000), eq("info"));
        verify(mockLogger, times(1)).warn(anyString(), eq(3000), eq("warn"));
        verify(mockLogger, never()).error(anyString(), eq(1000), eq("error"), any(Exception.class));
        verify(mockLogger, never()).info(anyString(), eq(5001), eq("info"));
        verify(mockLogger, never()).warn(anyString(), eq(3001), eq("warn"));
        verify(mockLogger, never()).error(anyString(), eq(1001), eq("error"), any(Exception.class));
    }

    @Test
    public void isEnabledAtInfoThreshold() {
        ConfigCatLogger logger = new ConfigCatLogger(mock(Logger.class), LogLevel.INFO);

        assertFalse(logger.isEnabled(LogLevel.DEBUG));
        assertTrue(logger.isEnabled(LogLevel.INFO));
        assertTrue(logger.isEnabled(LogLevel.WARNING));
        assertTrue(logger.isEnabled(LogLevel.ERROR));
        assertTrue(logger.isEnabled(LogLevel.NO_LOG));
    }

    @Test
    public void isEnabledAtNoLogThreshold() {
        ConfigCatLogger logger = new ConfigCatLogger(mock(Logger.class), LogLevel.NO_LOG);

        assertFalse(logger.isEnabled(LogLevel.DEBUG));
        assertFalse(logger.isEnabled(LogLevel.INFO));
        assertFalse(logger.isEnabled(LogLevel.WARNING));
        assertFalse(logger.isEnabled(LogLevel.ERROR));
        assertTrue(logger.isEnabled(LogLevel.NO_LOG));
    }
}
