package com.limelight;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Debug-only application logging boundary. */
public final class LimeLog {
    private static final Logger LOGGER = Logger.getLogger(LimeLog.class.getName());
    private static final boolean IS_LOGGABLE = BuildConfig.DEBUG;

    private LimeLog() {
    }

    public static void info(String msg) {
        if (IS_LOGGABLE) {
            LOGGER.info(msg);
        }
    }

    public static void warning(String msg) {
        if (IS_LOGGABLE) {
            LOGGER.warning(msg);
        }
    }

    public static void warning(String msg, Throwable error) {
        if (IS_LOGGABLE) {
            LOGGER.log(Level.WARNING, msg, error);
        }
    }

    public static void severe(String msg) {
        if (IS_LOGGABLE) {
            LOGGER.severe(msg);
        }
    }

    public static void severe(String msg, Throwable error) {
        if (IS_LOGGABLE) {
            LOGGER.log(Level.SEVERE, msg, error);
        }
    }
}
