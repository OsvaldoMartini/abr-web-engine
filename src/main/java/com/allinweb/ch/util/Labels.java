package com.allinweb.ch.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Labels {
    public static Properties labelsValue;

    public static final String START = "START";
    public static final String END = "END";
    public static final String OK = "OK";
    public static final String KO = "KO";

    public static void initializeLabelsInSpecLang(String language) {
        labelsValue = new Properties();
        String labelsFileName =
                ABRConstants.LABELS_FILE_NAME_COMMON + language + ABRConstants.PROPERTIES_FILE_EXTENSION;

        // Read in the LCO Probe properties file
        try (InputStream inputStream =
                new FileInputStream(new File(".").getCanonicalPath() + File.separator + labelsFileName)) {
            labelsValue = new Properties();
            // load a properties file
            labelsValue.load(inputStream);
        } catch (IOException ex) {
            ABRLogger.getInstance(Labels.class).severe("Cannot Read Lang Labels: " + ex.getMessage());
        }
    }
}
