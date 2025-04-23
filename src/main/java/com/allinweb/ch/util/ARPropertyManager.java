package com.allinweb.ch.util;

import com.allinweb.ch.facade.PerformMessage;
import com.google.common.base.Strings;
import java.io.*;
import java.util.Properties;
import java.util.logging.Level;
import lombok.Getter;
import lombok.Setter;

public class ARPropertyManager {
    protected static ARPropertyManager instance;

    // Private constructor to prevent instantiation
    private ARPropertyManager() {
        // Initialize if necessary
    }

    public static ARPropertyManager getInstance() {
        if (instance == null) {
            synchronized (ARPropertyManager.class) {
                if (instance == null) {
                    instance = new ARPropertyManager();
                }
            }
        }
        return instance;
    }

    private static final PerformMessage performMessage;

    // Static block to initialize
    static {
        performMessage = PerformMessage.getInstance();
    }

    private static final String lock = "locked";

    @Getter
    @Setter
    private Properties properties = new Properties();

    @Getter
    @Setter
    private String configurationFileName;

    public void loadProperties() {
        configurationFileName = System.getProperty("ARWebConfig");

        if (!Strings.isNullOrEmpty(configurationFileName)) {
            File configurationFile = new File(configurationFileName);
            try (FileInputStream conf = new FileInputStream(configurationFile)) {
                this.properties.load(conf);
                String logLevel = this.properties.getProperty(ARPropertyEnum.LOG_LEVEL.getValue());
                String extReference = this.properties.getProperty(ARPropertyEnum.WEBDRIVER_EXT_REFERENCE.getValue());
                System.out.println("LOG_LEVEL = " + logLevel + "   ConfigFile=" + configurationFileName);

                String logPath = getProperty(ARPropertyEnum.FOLDER_PATH_LOG);
                if (logPath == null || logPath.isBlank()) {
                    performMessage.errorMessage(
                            "Configuration Warning: Log Path Missing",
                            "<span style='color: #FFA000; font-weight: bold; font-size: 1.1em;'>Warning: Log path configuration not found!</span> ⚠️",
                            "<span style='color: #F57C00; font-weight: bold;'>No custom log path set. Using default location:</span>",
                            "<span style='font-weight: bold;'>C:\\ARWeb\\Logs</span>.",
                            "<span style='font-style: italic;'>Consider configuring a specific log path for better organization and access to application logs.</span>",
                            0);
                }

                File logDirectory = new File(logPath);
                if (!logDirectory.exists() && !logDirectory.mkdirs()) {
                    performMessage.errorMessage(
                            "Error: Log Directory Creation Failed",
                            "<span style='color: #D32F2F; font-weight: bold; font-size: 1.1em;'>Failed to create log directory!</span>",
                            "<span style='color: #E65100; font-weight: bold;'>Attempted location:</span> <span style='font-weight: bold;'>"
                                    + logPath + "</span>",
                            "<span style='font-style: italic;'>Please ensure the application has the necessary permissions to create directories at the specified path. Check the path for validity.</span>",
                            null,
                            0);
                }

            } catch (FileNotFoundException e) {
                performMessage.errorMessage(
                        configurationFileName, // Using configurationFileName as the title
                        "<span style='color: #D32F2F; font-weight: bold; font-size: 1.1em;'>Critical: Configuration file not found!</span>",
                        "<span style='color: #2E7D32; font-weight: bold;'>A new configuration file has been created at:</span>",
                        "<span style='font-weight: bold;'>" + configurationFileName
                                + "</span>.", // Filename on a new line
                        "<span style='color: #E65100;'>Please set the necessary configuration values in this new file.</span><br><span style='font-style: italic;'>Details: "
                                + e.getMessage() + "</span>",
                        0);
                boolean dirSuccess = configurationFile.mkdirs();
                configurationFile.delete();
                try {
                    configurationFile.createNewFile();
                    loadProperties();
                    setProperty(ARPropertyEnum.FOLDER_PATH_EXCEL.getValue(), "C:/ARWeb/Excel");
                    setProperty(ARPropertyEnum.FOLDER_PATH_LOG.getValue(), "C:/ARWeb/Logs");
                    setProperty(ARPropertyEnum.FOLDER_PATH_EXPORT.getValue(), "C:/ARWeb/Export");
                    //                setProperty(ARPropertyEnum.FILE_NAME_EXPORT.getValue(), "");
                    setProperty(
                            ARPropertyEnum.FOLDER_PATH_JAVA.getValue(),
                            ARConstants.CURRENT_PATH + ARConstants.DEFAULT_PATH_JAVA);
                    setProperty(
                            ARPropertyEnum.FOLDER_PATH_JAVA_FX.getValue(),
                            ARConstants.CURRENT_PATH + ARConstants.DEFAULT_PATH_JAVA_FX);
                    setProperty(ARPropertyEnum.DATABASE_TYPE.getValue(), "Access");
                    setProperty(ARPropertyEnum.PORT_SOCKET.getValue(), "54525");
                    setProperty(ARPropertyEnum.FOLDER_PATH_DB.getValue(), "C:/ARWeb");
                    setProperty(ARPropertyEnum.FOLDER_PATH_REPORT.getValue(), "C:/ARWeb/Reports");
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
                    performMessage.errorMessage(
                            "File Creation Error",
                            "<span style='color: #D32F2F; font-weight: bold; font-size: 1.1em;'>Failed to create file:</span>",
                            "<span style='font-weight: bold;'>" + configurationFileName
                                    + "</span>.", // Filename on a new line
                            "<span style='color: #E65100; font-weight: bold;'>Please verify the application has the necessary write permissions for the directory.</span>",
                            "<span style='font-style: italic;'>Details: " + ex.getMessage() + "</span>",
                            0);
                }

            } catch (IOException error) {
                performMessage.errorMessage(
                        "File Creation Error",
                        "<span style='color: #D32F2F; font-weight: bold; font-size: 1.1em;'>Failed to create file:</span>",
                        "<span style='font-weight: bold;'>" + configurationFileName
                                + "</span>.", // Filename on a new line
                        "<span style='color: #E65100; font-weight: bold;'>Please verify the application has the necessary write permissions for the directory.</span>",
                        "<span style='font-style: italic;'>Details: " + error.getMessage() + "</span>",
                        0);
            }
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
            //            this.properties.store(output, "added property: " + propertyName + " with value: " + value);
            this.properties.store(output, null);
        } catch (FileNotFoundException e) {
            performMessage.errorMessage(
                    configurationFileName, // Using configurationFileName as the title
                    "<span style='color: #D32F2F; font-weight: bold; font-size: 1.1em;'>Critical: Configuration file not found!</span>",
                    "<span style='color: #2E7D32; font-weight: bold;'>A new configuration file has been created at:</span>",
                    "<span style='font-weight: bold;'>" + configurationFileName + "</span>.", // Filename on a new line
                    "<span style='color: #E65100;'>Please set the necessary configuration values in this new file.</span><br><span style='font-style: italic;'>Details: "
                            + e.getMessage() + "</span>",
                    0);
        } catch (IOException error) {
            performMessage.errorMessage(
                    "Error Reading File",
                    "<span style='color: #D32F2F; font-weight: bold; font-size: 1.1em;'>Failed to read file:</span>",
                    "<span style='font-weight: bold;'>" + configurationFileName + "</span>.",
                    "<span style='color: #E65100; font-weight: bold;'>Please ensure the application has the necessary read permissions for the file and that the file exists.</span>",
                    "<span style='font-style: italic;'>Details: " + error.getMessage() + "</span>",
                    0);
        }
    }
}
