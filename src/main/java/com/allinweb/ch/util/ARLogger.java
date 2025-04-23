package com.allinweb.ch.util;

import com.allinweb.ch.facade.PerformMessage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ARLogger {
    private static volatile ARLogger instance;
    private static final Object lock = new Object();
    private static FileHandler handler;

    public static <T> ARLogger getInstance(Class<T> forClazz) {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new ARLogger();
                }
            }
        }
        instance.setLoggingClass(forClazz);
        return instance;
    }

    private void setLoggingClass(Class<?> clazz) {
        logger = Logger.getLogger(clazz.getName());

        // Check if the handler has already been added to avoid duplication
        if (logger.getHandlers().length == 0 && handler != null) {
            logger.addHandler(handler);
        }

        logger.setLevel(Level.ALL);
    }

    private static final PerformMessage performMessage;
    private static final ARPropertyManager arPropertyManager;

    static {
        performMessage = PerformMessage.getInstance();
        arPropertyManager = ARPropertyManager.getInstance();
    }

    private Logger logger;

    private ARLogger() {
        String logPath = arPropertyManager.getProperty(ARPropertyEnum.FOLDER_PATH_LOG);

        if (logPath == null || logPath.isBlank()) {
            performMessage.errorMessage(
                    "Warning: Log Path Configuration Missing",
                    "<span style='color: #FFA000; font-weight: bold; font-size: 1.1em;'>Log path configuration not set!</span> ⚠️",
                    "<span style='color: #E65100;'>Please specify the desired log path in the application settings.</span><br>",
                    "<span style='font-weight: bold;'>Current (invalid) configuration:</span> "
                            + "<span style='font-style: italic;'>" + logPath + "</span>",
                    "<span style='font-style: italic;'>Without a valid log path, the application might not be able to record important events.</span>",
                    0);

            return;
        }

        File logDirectory = new File(logPath);
        if (!logDirectory.exists() && !logDirectory.mkdirs()) {
            performMessage.errorMessage(
                    "Log Directory Creation Failed",
                    "<span style='color: #D32F2F; font-weight: bold; font-size: 1.1em;'>Unable to create log directory!</span> 📁❌",
                    "<span style='color: #E65100; font-weight: bold;'>Failed at this location:</span> ",
                    "<span style='font-weight: bold;'>" + "logDirectory" + "</span>",
                    "<span style='font-style: italic;'>Verify write permissions and the validity of the specified path. The application needs to create this directory to function correctly.</span>",
                    0);
            return;
        }

        try {
            handler = new FileHandler(logPath + ARConstants.FILE_NAME_ENGINE_RUN_LOG, true);
            handler.setFormatter(new SimpleFormatter());

            FileOutputStream fileOutputStream =
                    new FileOutputStream(logPath + ARConstants.FILE_NAME_ENGINE_OUTPUT_LOG, true);
            PrintStream printStream = new PrintStream(fileOutputStream);
            System.setOut(printStream);
            System.setErr(printStream);

        } catch (IOException error) {
            performMessage.errorMessage(
                    "Error Creating Log Directory",
                    "<span style='color: #D32F2F; font-weight: bold; font-size: 1.1em;'>Failed to create log directory!</span> 📁",
                    "<span style='color: #E65100; font-weight: bold;'>Attempted location:</span> <span style='font-weight: bold;'>"
                            + logPath + ARConstants.FILE_NAME_ENGINE_RUN_LOG + "</span>",
                    "<span style='font-style: italic;'>Please ensure the application has the necessary write permissions for the specified directory. Check the path for validity.</span>",
                    "<span style='font-style: italic;'>Details: " + error.getMessage() + "</span>",
                    0);
        }
    }

    public void info(String msg) {
        logger.info(msg);
    }

    public void warning(String msg) {
        logger.warning(msg);
    }

    public void severe(String msg) {
        logger.severe(msg);
    }

    public void config(String msg) {
        logger.config(msg);
    }

    public void fine(String msg) {
        logger.fine(msg);
    }

    public void finer(String msg) {
        logger.finer(msg);
    }

    public void finest(String msg) {
        logger.finest(msg);
    }
}
