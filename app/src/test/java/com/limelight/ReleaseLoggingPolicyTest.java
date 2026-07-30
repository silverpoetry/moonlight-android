package com.limelight;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class ReleaseLoggingPolicyTest {

    @Test
    public void releaseBuildSuppressesTaggedAndroidLogging() {
        if (BuildConfig.DEBUG) {
            return;
        }

        assertFalse(DebugLog.isEnabled());
        RuntimeException error = new RuntimeException("not logged");
        assertEquals(0, DebugLog.debug("release-test", "debug"));
        assertEquals(0, DebugLog.info("release-test", "info"));
        assertEquals(0, DebugLog.warning("release-test", "warning"));
        assertEquals(0, DebugLog.warning("release-test", "warning", error));
        assertEquals(0, DebugLog.error("release-test", "error"));
        assertEquals(0, DebugLog.error("release-test", "error", error));
    }

    @Test
    public void releaseBuildSuppressesGeneralLogging() {
        if (BuildConfig.DEBUG) {
            return;
        }

        Logger logger = Logger.getLogger(LimeLog.class.getName());
        boolean previousParentHandlerState = logger.getUseParentHandlers();
        AtomicInteger publishedRecords = new AtomicInteger();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                publishedRecords.incrementAndGet();
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        handler.setLevel(Level.ALL);

        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        try {
            LimeLog.info("info");
            LimeLog.warning("warning");
            LimeLog.severe("severe");
            assertEquals(0, publishedRecords.get());
        }
        finally {
            logger.removeHandler(handler);
            logger.setUseParentHandlers(previousParentHandlerState);
        }
    }
}
