package com.configcat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

// Test cases based on EvaluationTest 1_rule_no_user test case.
public class EvaluationLoggerTurnOffTest {
    @Test
    public void testEvaluationLogLevelInfo() throws IOException {

        ConfigCatClient client = ConfigCatClient.get("PKDVCLf-Hq-h-kCzMp-L7Q/psuH7BGHoUmdONrzzUOY7A", options -> {
            options.pollingMode(PollingModes.manualPoll());
            options.logLevel(LogLevel.INFO);
        });

        client.forceRefresh();

        Logger clientLogger = (Logger) LoggerFactory.getLogger(ConfigCatClient.class);
        // create and start a ListAppender
        clientLogger.setLevel(Level.INFO);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();

        clientLogger.addAppender(listAppender);

        String returnValue = client.getValue(String.class, "stringContainsDogDefaultCat", null, "default");

        assertEquals("Return value not match.", "Cat", returnValue);

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("Logged event size not match.", 2, logsList.size());
        assertEquals("LogLevel mismatch.", Level.WARN, logsList.get(0).getLevel());
        assertEquals("LogLevel mismatch.", Level.INFO, logsList.get(1).getLevel());

        client.close();
    }

    @Test
    public void testEvaluationLogLevelWarning() throws IOException {
        //based on 1_rule_no_user test case.
        ConfigCatClient client = ConfigCatClient.get("PKDVCLf-Hq-h-kCzMp-L7Q/psuH7BGHoUmdONrzzUOY7A", options -> {
            options.pollingMode(PollingModes.manualPoll());
            options.logLevel(LogLevel.WARNING);
        });

        client.forceRefresh();

        Logger clientLogger = (Logger) LoggerFactory.getLogger(ConfigCatClient.class);
        // create and start a ListAppender
        clientLogger.setLevel(Level.INFO);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();

        clientLogger.addAppender(listAppender);

        String returnValue = client.getValue(String.class, "stringContainsDogDefaultCat", null, "default");

        assertEquals("Return value not match.", "Cat", returnValue);

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("Logged event size not match.", 1, logsList.size());
        assertEquals("LogLevel mismatch.", Level.WARN, logsList.get(0).getLevel());

        client.close();
    }

}
