package com.allinweb.ch.util;

import java.io.*;
import java.util.Properties;
import java.util.logging.Level;
import javax.swing.*;

public class ARPropertyManager {

    private static final String lock = "locked";
    private static String configurationFileName = ARConstants.CURRENT_PATH + ARConstants.FILE_NAME_CONFIGURATION;

    private static volatile ARPropertyManager instance;

    private Properties properties = new Properties();

    private ARPropertyManager() {
        loadProperties();
    }

    /***
     * This method both manages the retrieving of the ARPropertyManager instance and
     * the value of the instance static variable.
     * The block is synchronized in a way that the instance will be always of one instance value and that it never
     * changes during the execution of the application.
     * After the instance is set, we then load the property values into such instance.
     * In that way, all the property values are available in the application.
     * @return the current instance of the ARPropertyManager class
     */
    public static ARPropertyManager getInstance() {
        synchronized (lock) {
            if (instance == null) {
                instance = new ARPropertyManager();
            }
        }
        return instance;
    }

    /***
     * This method loads the file in memory and sets the properties read by the file into the
     * properties variable, making all the properties defined in the file available in the
     * application.
     */
    private void loadProperties() {
        File configurationFile = new File(configurationFileName);
        try (FileInputStream conf = new FileInputStream(configurationFile)) {
            this.properties.load(conf);
            String logLevel = this.properties.getProperty(ARPropertyEnum.LOG_LEVEL.getValue());
            String extReference = this.properties.getProperty(ARPropertyEnum.WEBDRIVER_EXT_REFERENCE.getValue());
            System.out.println("LOG_LEVEL = " + logLevel + "   ConfigFile=" + configurationFileName);
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Configuration file was not found. A new configuration file has been created at "
                            + configurationFileName + ". Please set the values for the configuration.\nError:\n"
                            + e.getMessage(),
                    "Configuration file not found",
                    JOptionPane.WARNING_MESSAGE);
            boolean dirSuccess = configurationFile.mkdirs();
            configurationFile.delete();
            try {
                configurationFile.createNewFile();
                loadProperties();
                setProperty(ARPropertyEnum.FOLDER_PATH_EXCEL.getValue(), "");
                setProperty(ARPropertyEnum.FOLDER_PATH_LOG.getValue(), "");
                setProperty(ARPropertyEnum.FOLDER_PATH_EXPORT.getValue(), "");
                //                setProperty(ARPropertyEnum.FILE_NAME_EXPORT.getValue(), "");
                setProperty(
                        ARPropertyEnum.FOLDER_PATH_JAVA.getValue(),
                        ARConstants.CURRENT_PATH + ARConstants.DEFAULT_PATH_JAVA);
                setProperty(
                        ARPropertyEnum.FOLDER_PATH_JAVA_FX.getValue(),
                        ARConstants.CURRENT_PATH + ARConstants.DEFAULT_PATH_JAVA_FX);
                setProperty(ARPropertyEnum.DATABASE_TYPE.getValue(), "Access");
                setProperty(ARPropertyEnum.PORT_SOCKET.getValue(), "8080");
                setProperty(ARPropertyEnum.BLOCK_EXEC_LIMIT.getValue(), "20");
                setProperty(ARPropertyEnum.FOLDER_PATH_DB.getValue(), "");
                setProperty(ARPropertyEnum.FOLDER_PATH_REPORT.getValue(), "");
                setProperty(ARPropertyEnum.PATH_ENGINE.getValue(), ARConstants.CURRENT_PATH);
                setProperty(ARPropertyEnum.PATH_WEBDRIVER.getValue(), "");
                setProperty(ARPropertyEnum.LOG_LEVEL.getValue(), Level.ALL.getName());
                setProperty(ARPropertyEnum.BROWSER.getValue(), ARConstants.CHROME);
                setProperty(ARPropertyEnum.WEBDRIVER_PAGE_UPDATE_TIMEOUT_SEC.getValue(), "60");
                setProperty(ARPropertyEnum.WEBDRIVER_INTERACTION_TIMEOUT_SEC.getValue(), "60");
                setProperty(ARPropertyEnum.DEFAULT_INSTRUCTION_STOP_SECONDS.getValue(), "15");

                setProperty(
                        ARPropertyEnum.WEBDRIVER_EXT_REFERENCE.getValue(),
                        "test-id='web-banking-payment-core.payment-details.external-reference'");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(
                        null,
                        "Could not create the file " + configurationFileName + ". Please check the permissions.\nError:"
                                + ex.getMessage(),
                        "Configuration file cannot be created",
                        JOptionPane.ERROR_MESSAGE);
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Could not read the file " + configurationFileName + ". Please check the permissions.\nError:"
                            + e.getMessage(),
                    "Configuration file cannot be read",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /***
     * This method gets the value associated to the property name specified.
     * Only properties define in the {@link ARPropertyEnum} class are supported.
     * In case more properties are added to the file, it needs to be mapped in the {@link ARPropertyEnum} as well.
     * @param property The property enum associated to the properties available in the file
     * @return Return the String value associated with the property name specified
     */
    public String getProperty(ARPropertyEnum property) {
        return this.properties.getProperty(property.getValue());
    }

    public void setProperty(String propertyName, String value) {
        this.properties.setProperty(propertyName, value);
        try (FileOutputStream output = new FileOutputStream(configurationFileName)) {
            this.properties.store(output, "added property: " + propertyName + " with value: " + value);
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Configuration file was not found. A new configuration file has been created at "
                            + configurationFileName + ". Please set the values for the configuration.",
                    "Configuration file not found",
                    JOptionPane.WARNING_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Could not read the file " + configurationFileName + ". Please check the permissions.",
                    "Configuration file cannot be read\nError:\n" + e.getMessage(),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void setConfigurationFileName(String configurationPath) {
        ARPropertyManager.configurationFileName = configurationPath;
    }

    public static String getConfigurationFileName() {
        return ARPropertyManager.configurationFileName;
    }
}
