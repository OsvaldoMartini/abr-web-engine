package com.allinweb.ch.util;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SeleniumLoggerSuppressor {

    public static void suppressInMemory() {
        // Get the root logger
        Logger rootLogger = Logger.getLogger("");

        // Remove all existing handlers
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }

        // Create a single console handler with SEVERE level
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.SEVERE);
        rootLogger.addHandler(consoleHandler);

        // Suppress Selenium loggers
        Logger.getLogger("org.openqa.selenium").setLevel(Level.SEVERE);
        Logger.getLogger("org.openqa.selenium.devtools").setLevel(Level.SEVERE);
    }
}
