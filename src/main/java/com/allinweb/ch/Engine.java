package com.allinweb.ch;

import com.allinweb.ch.facade.PerformDBEngine;
import com.allinweb.ch.facade.PerformMessage;
import com.allinweb.ch.license.LicenceVal;
import com.allinweb.ch.license.LicenseManager;
import com.allinweb.ch.runner.EngineRunner;
import com.allinweb.ch.util.*;
import com.google.common.base.Strings;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Engine {

    private static final LogControl logControl = LogControl.getInstance();
    private static final ARPropertyManager arPropertyManager = ARPropertyManager.getInstance();
    private static final PerformMessage performMessage = PerformMessage.getInstance();
    private static final PerformDBEngine performDBEngine = PerformDBEngine.getInstance();

    private static String defaultConfigurationFileName =
            ARConstantsEngine.USER_PATH + ARConstantsEngine.FILE_NAME_CONFIGURATION;
    private static final String language = "en";

    private static boolean isEnabledLicence = true;

    // static only for entry point
    public static void main(String[] args) {

        Logger.getLogger("org.openqa.selenium").setLevel(Level.SEVERE);

        System.setProperty("org.eclipse.jetty.LEVEL", "OFF");
        Logger.getLogger("org.openqa.selenium").setLevel(Level.SEVERE);
        logControl.disableLogging();

        System.out.println("ENGINE STARTED");

        if (args.length == 0) {
            System.out.println("No parameters, please read documentation.");
            System.exit(0);
        }

        for (int i = 0; i < args.length; i++) {
            System.out.println("PARAM " + i + ">> " + args[i]);
        }

        // Redirect System.out and System.err
        System.setOut(new PrintStream(new LoggingOutputStream(log, false), true));
        System.setErr(new PrintStream(new LoggingOutputStream(log, true), true));

        // --- configuration file setup
        configureProperties(args);

        // Per-bot-job lock - prevents the same bot job from running twice
        String botJobLockId = extractBotJobId(args);
        if (botJobLockId != null) {
            String logPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_LOG);
            if (!Strings.isNullOrEmpty(logPath)) {
                if (!SingleInstance.acquire("ARWebEngine-botJob" + botJobLockId, logPath)) {
                    log.warn("Bot Job {} is already running. Exiting.", botJobLockId);
                    System.exit(0);
                }
                Runtime.getRuntime().addShutdownHook(new Thread(SingleInstance::release));
            }
        }

        Labels.initializeLabelsInSpecLang(language);

        List<String> missingProperties = checkProperties(arPropertyManager.getProperties());
        if (!missingProperties.isEmpty()) {
            reportMissingProperties(missingProperties);
            return;
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            log.error("LookAndFeel setting failed.");
        }
        EngineRunner runner = new EngineRunner();
        runner.run(args);
    }

    private static void reportMissingProperties(List<String> missingProperties) {
        int totalMissing = missingProperties.size();
        int partSize = (int) Math.ceil((double) totalMissing / 3);

        String part1 = String.join(", ", missingProperties.subList(0, Math.min(partSize, totalMissing)));
        String part2 = totalMissing > partSize
                ? String.join(", ", missingProperties.subList(partSize, Math.min(2 * partSize, totalMissing)))
                : "";
        String part3 = totalMissing > 2 * partSize
                ? String.join(", ", missingProperties.subList(2 * partSize, totalMissing))
                : "";

        performMessage.errorMessage("I cannot Execute Engine", "Missing required properties: ", part1, part2, part3, 0);
    }

    private static void configureProperties(String[] args) {
        List<String> arguments = Arrays.asList(args);
        String configurationValue;

        if (arguments.contains("-c")) {
            int configurationValueIndex = arguments.indexOf("-c") + 1;
            configurationValue = arguments.get(configurationValueIndex);
        } else {
            configurationValue = defaultConfigurationFileName;
        }

        try {
            System.setProperty("ARWebConfig", configurationValue);
        } catch (Exception ignore) {
        }

        arPropertyManager.setConfigurationFileName(configurationValue);
        File configurationFile = new File(configurationValue);

        try (FileInputStream conf = new FileInputStream(configurationFile)) {
            arPropertyManager.loadProperties(conf);
            setLogPath();
            logControl.enableLogging();
            licenseControl();
            initializeServers();
        } catch (Exception error) {
            arPropertyManager.createDefaultProperties(configurationFile);
            setLogPath();
            logControl.enableLogging();
            licenseControl();
            initializeServers();
        }

        log.info("Configuration file path: " + configurationValue);
    }

    private static void licenseControl() {
        if (isEnabledLicence) {
            if (!checkLicense()) {
                System.exit(0);
            }
        }
    }

    private static boolean checkLicense() {
        try {
            String licensePath = arPropertyManager.getProperty(ARPropertyEnum.PATH_LICENSE);
            if (Strings.isNullOrEmpty(licensePath)) {
                licensePath = System.getProperty("user.dir");
            }

            LicenceVal licenseStatus = LicenseManager.checkLicenseFile(licensePath);

            String msgValid = "The license file is valid and the application is authorized for use.";
            String msgNextStep = "You can now proceed with normal application usage.";

            String msgColor = "#0277BD";
            if (!licenseStatus.equals(LicenceVal.VALID)) {
                msgValid = "The license file is not valid and the application is not authorized for use.";
                msgNextStep = "Application access is restricted. Please obtain a valid license to continue.";
                msgColor = "#C62828"; // Soft, elegant red tone

                performMessage.showCustomModalDialogDragWin11(
                        "License Status Verification",
                        "<span style='color: #2E7D32; font-weight: bold; font-size: 1.1em;'>License status has been successfully verified.</span>",
                        "<span style='color: " + msgColor + "; font-weight: bold;'>" + msgValid + "</span>",
                        "<span style='font-style: italic;'>" + msgNextStep + "</span>",
                        "<span style='color: #E65100; font-weight: bold;'>Current license status:</span> <span style='font-weight: bold;'>"
                                + licenseStatus.getStaus() + "</span>",
                        false,
                        "OK",
                        null,
                        0);
                return false;
            }
            return true;
        } catch (Exception error) {
            log.error("Cannot read/validate the License path/file. Error: " + error.getMessage());
            return false;
        }
    }

    public static List<String> checkProperties(Properties properties) {
        String[] requiredProperties = {
            "data_base",
            //            "db_url",
            //            "db_user",
            //            "db_pwd",
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

    private static void setLogPath() {
        // Set log path system property BEFORE logback init
        String logPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_LOG);
        File logDir = new File(logPath);
        if (!logDir.exists() && !logDir.mkdirs()) {
            log.error("❌ Failed to create log directory: " + logDir.getAbsolutePath());
            System.exit(1);
        }
        System.setProperty("LOG_PATH", logDir.getAbsolutePath());

        // After main logic, initialize logging
        LogbackInitializer.loadLogbackFromResources();
        System.setProperty("org.eclipse.jetty.LEVEL", "ON");
        // Now logback initializes with correct LOG_PATH
        log.info("Using log path: {}", logDir.getAbsolutePath());
    }

    private static void initializeServers() {
        System.setProperty(
                "ARWebChosenPort", String.valueOf(arPropertyManager.getProperty(ARPropertyEnum.PORT_SOCKET)));
        performDBEngine.callSocketLists("engine-perform-bot-job");
    }

    /**
     * Extract the bot job ID from args: execute/j {homeBankId} {botJobId} ...
     */
    private static String extractBotJobId(String[] args) {
        try {
            List<String> list = Arrays.asList(args);
            int idx = list.indexOf("execute/j");
            if (idx >= 0 && idx + 2 < list.size()) {
                return list.get(idx + 2); // botJobId is the second param after execute/j
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
