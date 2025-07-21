package com.allinweb.ch.util;

import com.allinweb.ch.facade.PerformMessage;
import com.google.common.base.Strings;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import lombok.Getter;
import lombok.Setter;

public class ARPropertyManager {
    protected static volatile ARPropertyManager instance;

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

    public void loadProperties(FileInputStream configFile) {
        configurationFileName = System.getProperty("ARWebConfig");

        //        if (!Strings.isNullOrEmpty(configurationFileName)) {
        //            File configurationFile = new File(configurationFileName);
        //            try (FileInputStream conf = new FileInputStream(configurationFile)) {
        try {
            this.properties.load(configFile);

            String logLevel = this.properties.getProperty(ARPropertyEnum.LOG_LEVEL.getValue());
            System.out.println("LOG_LEVEL = " + logLevel + "   ConfigFile=" + configurationFileName);

            String logPath = getProperty(ARPropertyEnum.PATH_LOG);
            if (logPath == null || logPath.isBlank()) {
                performMessage.errorMessage(
                        "Configuration Warning: Log Path is Missing",
                        "<span style='color: #FFA000; font-weight: bold; font-size: 1.1em;'>Warning: Log path configuration not found!</span> ⚠️",
                        "<span style='color: #F57C00; font-weight: bold;'>Using default location:</span>",
                        "<span style='font-weight: bold;'>C:\\ARWeb\\ARWeb\\Logs</span>.",
                        "<span style='font-style: italic;'>Consider configuring a specific log path for better organization and access to application logs.</span>",
                        0);
            }

            if (logPath == null || logPath.isBlank()) {
                logPath = "C:\\ARWeb\\ARWeb\\Logs";
                setProperty(ARPropertyEnum.PATH_LOG.getValue(), logPath);
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
            }

            String dataBasePath = getProperty(ARPropertyEnum.PATH_DB);
            if (dataBasePath == null || dataBasePath.isBlank()) {
                performMessage.errorMessage(
                        "Configuration Warning: Database Path is Missing",
                        "<span style='color: #FFA000; font-weight: bold; font-size: 1.1em;'>Warning: Database path configuration not found!</span> ⚠️",
                        "<span style='color: #F57C00; font-weight: bold;'>Using default location:</span>",
                        "<span style='font-weight: bold;'>C:\\ARWeb\\ARWeb</span>.",
                        "<span style='font-style: italic;'>Consider configuring a specific log path for better organization and access to application logs.</span>",
                        0);
            }

            if (dataBasePath == null || dataBasePath.isBlank()) {
                dataBasePath = "C:\\ARWeb\\ARWeb";
                setProperty(ARPropertyEnum.PATH_DB.getValue(), dataBasePath);
                File dbDirectory = new File(dataBasePath);
                if (!dbDirectory.exists() && !dbDirectory.mkdirs()) {
                    performMessage.errorMessage(
                            "Error: Database Directory Creation Failed",
                            "<span style='color: #D32F2F; font-weight: bold; font-size: 1.1em;'>Failed to create database directory!</span>",
                            "<span style='color: #E65100; font-weight: bold;'>Attempted location:</span> <span style='font-weight: bold;'>"
                                    + dataBasePath + "</span>",
                            "<span style='font-style: italic;'>Please ensure the application has the necessary permissions to create directories at the specified path. Check the path for validity.</span>",
                            null,
                            0);
                }
            }

            if (!properties.containsKey("db_url")) {
                properties.setProperty("db_url", "jdbc:postgresql://localhost:5432/ar_web");
            }

            //            if (!properties.containsKey("db_url")) {
            //                properties.setProperty("db_url", "jdbc:postgresql://localhost:5432/ar_web");
            //            }

            if (!properties.containsKey("db_user")) {
                properties.setProperty("db_user", "postgres");
            }
            if (!properties.containsKey("db_pwd")) {
                properties.setProperty("db_pwd", "martini");
            }

            missingMandatoryPats();

            setProperty(ARPropertyEnum.VERSION.getValue(), "AR Web v4.6f Beta Test");
            setProperty(ARPropertyEnum.BUILD.getValue(), "Build: " + getTodaysDate(0));

            //        } catch (Exception e) {
            //
            //            createDefaultProperties(configurationFile);
            //
            //            //                loadProperties();
            //            Platform.runLater(() -> {
            //                arConfigurationScene.showModal();
            //            });

        } catch (IOException error) {
            performMessage.errorMessage(
                    "File Creation Error",
                    "<span style='color: #D32F2F; font-weight: bold; font-size: 1.1em;'>Failed to create file:</span>",
                    "<span style='font-weight: bold;'>" + configurationFileName + "</span>.", // Filename on a new line
                    "<span style='color: #E65100; font-weight: bold;'>Please verify the application has the necessary write permissions for the directory.</span>",
                    "<span style='font-style: italic;'>Details: " + error.getMessage() + "</span>",
                    0);
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

    public void createDefaultProperties(File configurationFile) {
        performMessage.errorMessage(
                "Creation of new \"ARWeb.config\" file", // Using configurationFileName as the title
                "<span style='color: #D32F2F; font-weight: bold; font-size: 1.1em;'>Critical: Configuration file not found!</span>",
                "<span style='color: #2E7D32; font-weight: bold;'>A new configuration file has been created at:</span>",
                "<span style='font-weight: bold;'>" + configurationFileName + "</span>.", // Filename on a new line
                "<span style='color: #E65100;'>Please set the necessary configuration values in this new file.</span><br><span style='font-style: italic;'>Details: "
                        + "ARWeb.config" + "</span>",
                0);

        boolean dirSuccess = configurationFile.mkdirs();
        configurationFile.delete();
        try {
            configurationFile.createNewFile();
            setProperty(ARPropertyEnum.PATH_LICENSE.getValue(), ARConstants.USER_PATH);

            setProperty(ARPropertyEnum.PATH_EXCEL.getValue(), "C:\\ARWeb\\ARWeb\\Excel");
            setProperty(ARPropertyEnum.PATH_LOG.getValue(), "C:\\ARWeb\\ARWeb\\Logs");
            setProperty(ARPropertyEnum.PATH_EXPORT.getValue(), "C:\\ARWeb\\ARWeb\\Export");
            setProperty(ARPropertyEnum.PATH_REPORT.getValue(), "C:\\ARWeb\\ARWeb\\Reports");
            setProperty(ARPropertyEnum.PATH_DB.getValue(), "C:\\ARWeb\\ARWeb");
            setProperty(ARPropertyEnum.PATH_PRIORITY.getValue(), "C:\\ARWeb\\ARWeb");

            setProperty(ARPropertyEnum.DB_URL.getValue(), "jdbc:postgresql://localhost:5432/ar_web");
            setProperty(ARPropertyEnum.DB_USER.getValue(), "XXXXXX");
            setProperty(ARPropertyEnum.DB_PWD.getValue(), "XXXXXX");

            setProperty(ARPropertyEnum.DATABASE_TYPE.getValue(), "Access");

            setProperty(ARPropertyEnum.PORT_SOCKET.getValue(), "54525");
            setProperty(ARPropertyEnum.PATH_ENGINE.getValue(), ARConstants.USER_PATH);
            setProperty(ARPropertyEnum.PATH_WEBDRIVER.getValue(), ARConstants.USER_PATH + "\\driver");
            setProperty(ARPropertyEnum.LOG_LEVEL.getValue(), Level.INFO.getName());
            setProperty(ARPropertyEnum.BROWSER.getValue(), ARConstants.EDGE);
            setProperty(ARPropertyEnum.WEBDRIVER_PAGE_UPDATE_TIMEOUT_SEC.getValue(), "60");
            setProperty(ARPropertyEnum.WEBDRIVER_INTERACTION_TIMEOUT_SEC.getValue(), "60");
            setProperty(ARPropertyEnum.INSTRUCTION_STOP_SECONDS.getValue(), "15");

        } catch (IOException ex) {
            performMessage.errorMessage(
                    "File Creation Error",
                    "<span style='color: #D32F2F; font-weight: bold; font-size: 1.1em;'>Failed to create file:</span>",
                    "<span style='font-weight: bold;'>" + configurationFileName + "</span>.", // Filename on a new line
                    "<span style='color: #E65100; font-weight: bold;'>Please verify the application has the necessary write permissions for the directory.</span>",
                    "<span style='font-style: italic;'>Details: " + ex.getMessage() + "</span>",
                    0);
        }
    }

    public List<String> checkProperties(Properties properties) {
        String[] requiredProperties = {
            "data_base",
            "db_url",
            "db_user",
            "db_pwd",
            "path_excel",
            "path_log",
            "path_db",
            "path_report",
            "path_priority",
            "path_engine",
            "path_web_driver",
            "log_level",
            "browser"
        };

        List<String> missingPropertiesList = new ArrayList<>();

        for (String propertyName : requiredProperties) {
            if (!properties.containsKey(propertyName)) {
                missingPropertiesList.add(propertyName);
            }
        }

        return missingPropertiesList;
    }

    public boolean missingMandatoryPats() {
        List<String> missingProperties = checkProperties(getProperties());

        if (!missingProperties.isEmpty()) {

            int totalMissing = missingProperties.size();
            int partSize = (int) Math.ceil((double) totalMissing / 3); // Divide into 3 parts

            String part1 = String.join(", ", missingProperties.subList(0, Math.min(partSize, totalMissing)));
            String part2 = totalMissing > partSize
                    ? String.join(", ", missingProperties.subList(partSize, Math.min(2 * partSize, totalMissing)))
                    : "";
            String part3 = totalMissing > 2 * partSize
                    ? String.join(", ", missingProperties.subList(2 * partSize, totalMissing))
                    : "";

            if (!Strings.isNullOrEmpty(part1)) {
                part1 = "<span style='color: #c0392b; font-weight: bold;'>" + part1 + "</span>";
            }
            if (!Strings.isNullOrEmpty(part1)) {
                part2 = "<span style='color: #c0392b; font-weight: bold;'>" + part2 + "</span>";
            }
            if (!Strings.isNullOrEmpty(part1)) {
                part3 = "<span style='color: #c0392b; font-weight: bold;'>" + part3 + "</span>";
            }

            performMessage.errorMessage(
                    "Configuration Warning: Missing Mandatory Paths",
                    "<span style='color: #FFA000; font-weight: bold; font-size: 1.1em;'>Consider configuring all paths that are missing!</span> ⚠️",
                    part1,
                    Strings.isNullOrEmpty(part2) ? null : part2,
                    Strings.isNullOrEmpty(part3) ? null : part3,
                    0);
            return true;
        }
        return false;
    }

    public static String getTodaysDate(int day) {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(day);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        return yesterday.format(formatter);
    }
}
