package com.allinweb.ch.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

public class ABRLogger {
    private static final String lock = "locked";

    private static volatile ABRLogger instance;
    private static FileHandler handler;

    private Logger _log;

    private ABRLogger() {
        String logPath = ABRPropertyManager.getInstance().getProperty(ABRPropertyEnum.FOLDER_PATH_LOG);
        if (logPath.isBlank()) {
            JOptionPane.showMessageDialog(
                    null,
                    "The configuration of the log path has not been set. Please set the configuration for"
                            + "the log path",
                    "Log configuration not set",
                    JOptionPane.WARNING_MESSAGE);
        } else {
            File logDirectory = new File(logPath);
            if (!logDirectory.exists()) {
                logDirectory.mkdirs();
            }
            try {
                handler = new FileHandler(logPath + ABRConstants.FILE_NAME_SCANNER_LOG);
                FileOutputStream fileOutputStream =
                        new FileOutputStream(logPath + ABRConstants.FILE_NAME_SCANNER_OUTPUT_LOG);
                PrintStream printStream = new PrintStream(fileOutputStream);
                System.setOut(printStream);
                System.setErr(printStream);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(
                        null,
                        "An error has occurred during the creation of the logger: Message: " + e.getMessage()
                                + " Cause:" + e.getCause(),
                        "Error in the creation of the logger",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static <T> ABRLogger getInstance(Class<T> forClazz) {
        synchronized (lock) {
            if (instance == null) {
                instance = new ABRLogger();
            }
        }
        instance.setLoggingClass(forClazz);
        return instance;
    }

    private void setLoggingClass(Class clazz) {
        _log = Logger.getLogger(clazz.getName());
        _log.addHandler(handler);
        _log.setLevel(Level.ALL);
    }

    public void info(String msg) {
        _log.info(msg);
    }

    public void warning(String msg) {
        _log.warning(msg);
    }

    public void severe(String msg) {
        _log.severe(msg);
    }

    public void config(String msg) {
        _log.config(msg);
    }

    public void fine(String msg) {
        _log.fine(msg);
    }

    public void finer(String msg) {
        _log.finer(msg);
    }

    public void finest(String msg) {
        _log.finest(msg);
    }
}
