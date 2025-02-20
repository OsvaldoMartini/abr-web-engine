package com.allinweb.ch.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.swing.JOptionPane;

public class ARLogger {
    private static final Object lock = new Object();
    private static volatile ARLogger instance;
    private static FileHandler handler;

    private Logger logger;

    private ARLogger() {
        String logPath = ARPropertyManager.getInstance().getProperty(ARPropertyEnum.FOLDER_PATH_LOG);

        if (logPath == null || logPath.isBlank()) {
            JOptionPane.showMessageDialog(
                    null,
                    "The configuration of the log path has not been set. Please set the configuration for the log path.",
                    "Log Configuration Not Set",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        File logDirectory = new File(logPath);
        if (!logDirectory.exists() && !logDirectory.mkdirs()) {
            JOptionPane.showMessageDialog(
                    null,
                    "Failed to create log directory at: " + logPath,
                    "Log Directory Creation Failed",
                    JOptionPane.ERROR_MESSAGE);
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

        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    null,
                    "An error occurred during the creation of the logger -> Cause: " + e.getMessage(),
                    "Error in Logger Creation",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

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
