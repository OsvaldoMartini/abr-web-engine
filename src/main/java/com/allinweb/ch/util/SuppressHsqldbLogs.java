package com.allinweb.ch.util;

import java.util.logging.Level;
import java.util.logging.Logger;

public class SuppressHsqldbLogs {

    // Static final variable to hold the singleton instance
    protected static volatile SuppressHsqldbLogs instance;

    // Public method to access the singleton instance
    public static SuppressHsqldbLogs getInstance() {
        if (instance == null) {
            synchronized (SuppressHsqldbLogs.class) {
                if (instance == null) {
                    instance = new SuppressHsqldbLogs();
                }
            }
        }
        return instance;
    }

    static {
        Logger hsqldbLogger = Logger.getLogger("org.hsqldb.persist.Logger");
        hsqldbLogger.setLevel(Level.OFF);

        // Also disable parent handlers so JUL doesn't leak logs
        hsqldbLogger.setUseParentHandlers(false);
    }
}
