package com.allinweb.ch.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

public class LogControl {
    private static final Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    // Static final variable to hold the singleton instance
    protected static volatile LogControl instance;

    // Private constructor to prevent instantiation
    private LogControl() {
        System.setProperty("org.eclipse.jetty.LEVEL", "OFF");
        disableLogging();
    }

    // Public method to access the singleton instance
    public static LogControl getInstance() {
        if (instance == null) {
            synchronized (LogControl.class) {
                if (instance == null) {
                    instance = new LogControl();
                }
            }
        }
        return instance;
    }

    public void disableLogging() {
        rootLogger.setLevel(Level.OFF);
    }

    public void enableLogging() {
        rootLogger.setLevel(Level.INFO); // or DEBUG, WARN, etc.
    }
}
