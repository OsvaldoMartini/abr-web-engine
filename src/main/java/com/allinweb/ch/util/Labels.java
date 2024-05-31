package com.allinweb.ch.util;

import java.util.Properties;

public class Labels {
    public static Properties labelsValue;

    public static final String START = "START";
    public static final String END = "END";
    public static final String OK = "OK";
    public static final String KO = "KO";

    public static void initializeLabelsInSpecLang(String language) {
        labelsValue = new Properties();
        String labelsFileName = Constants.LABELS_FILE_NAME_COMMON + language + Constants.PROPERTIES_FILE_EXTENSION;
        try {
            labelsValue.load(ClassLoader.getSystemResourceAsStream(labelsFileName));
        } catch (Exception e) {
            e.printStackTrace();
            //            System.exit(1);
        }
    }
}
