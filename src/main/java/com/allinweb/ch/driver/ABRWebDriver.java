package com.allinweb.ch.driver;

import com.allinweb.ch.builder.WebElementAttributeEnum;
import com.allinweb.ch.builder.WebElementScriptFactory;
import com.allinweb.ch.component.scene.ABRAlertScene;
import com.allinweb.ch.facade.PerformActions;
import com.allinweb.ch.util.ABRConstants;
import com.allinweb.ch.util.ABRLogger;
import com.allinweb.ch.util.ABRPropertyEnum;
import com.allinweb.ch.util.ABRPropertyManager;
import com.google.common.base.Strings;
import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javax.swing.*;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.support.ui.WebDriverWait;

public class ABRWebDriver {

    private static WebDriver driver = null;
    private final WebElementScriptFactory scriptFactory = new WebElementScriptFactory();

    private static final PerformActions performAction;
    // Static block to initialize
    static {
        performAction = PerformActions.getInstance();
    }

    public static String identifyLineSeparator(String text) {
        if (text.contains("\r\n")) {
            return "\r\n"; // Windows style
        } else if (text.contains("\n")) {
            return "\n"; // Unix/Linux style
        } else if (text.contains("\r")) {
            return "\r"; // Old Mac style
        }
        return System.lineSeparator(); // Default line separator if none found
    }

    public WebDriver openDriver(String url, String optionsConfig) {

        String lineSeparator = identifyLineSeparator(optionsConfig);

        // Split the text into lines using the detected line separator
        String[] optionsConfigLines = new String[0];
        try {

            optionsConfigLines = optionsConfig.split(lineSeparator);
        } catch (Exception ex) {
            ABRLogger.getInstance(ABRWebDriver.class).severe("Error WebDriver config Options : \n" + ex.getMessage());
        }

        ABRLogger.getInstance(ABRWebDriver.class).fine("Going to call WebDriver for \n" + url);

        ABRPropertyManager managerProps = ABRPropertyManager.getInstance();
        String webDriverPath = managerProps.getProperty(ABRPropertyEnum.PATH_WEBDRIVER);

        if (Strings.isNullOrEmpty(webDriverPath)) {
            ABRLogger.getInstance(ABRWebDriver.class).fine("URL IS EMPTY");
            //            JOptionPane.showMessageDialog(
            //                    null,
            //                    "An error has occurred PATH_WEBDRIVER is NULL",
            //                    "Error in WebDriver PATH",
            //                    JOptionPane.ERROR_MESSAGE);
        }

        if (driver == null) {
            String browser = ABRPropertyManager.getInstance().getProperty(ABRPropertyEnum.BROWSER);
            String logFolder = ABRPropertyManager.getInstance().getProperty(ABRPropertyEnum.FOLDER_PATH_LOG);
            try {
                switch (browser) {
                    case ABRConstants.CHROME -> {
                        //                        String driverPath = webDriverPath + "\\chrome.exe";
                        if (!(new File(webDriverPath)).exists()) {
                            ABRLogger.getInstance(ABRWebDriver.class).fine("Web Driver NOT EXIST \n" + webDriverPath);
                        }

                        // "\\_chrome_browser.log");

                        System.setProperty("webdriver.chrome.driver", webDriverPath);

                        ChromeOptions optionsChrome = buildOptionsChrome(optionsConfigLines, logFolder);

                        if (optionsChrome != null) {
                            driver = new ChromeDriver(optionsChrome);
                        } else {
                            driver = new ChromeDriver();
                        }
                    }
                    case ABRConstants.EDGE -> {
                        //                        String driverPath = webDriverPath + "\\msedgedriver.exe";
                        if (!(new File(webDriverPath)).exists()) {
                            ABRLogger.getInstance(ABRWebDriver.class).fine("Web Driver NOT EXIST \n" + webDriverPath);
                            new ABRAlertScene(
                                    Alert.AlertType.WARNING,
                                    "Missing file excel",
                                    "Please generate and compile the data of the file excel first before launching the bot job",
                                    new ButtonType[] {ButtonType.OK});
                        }
                        // Set path to Edge WebDriver executable
                        System.setProperty("webdriver.edge.driver", webDriverPath);

                        // Configure Edge options
                        EdgeOptions options = buildOptionsEdge(optionsConfigLines, logFolder);

                        if (options != null) {
                            driver = new EdgeDriver(options);
                        } else {
                            driver = new EdgeDriver();
                        }
                    }
                    case ABRConstants.FIREFOX -> {
                        //                        String driverPath = webDriverPath + "\\geckodriver.exe";
                        if (!(new File(webDriverPath)).exists()) {
                            ABRLogger.getInstance(ABRWebDriver.class).fine("Web Driver NOT EXIST \n" + webDriverPath);
                        }
                        System.setProperty("webdriver.gecko.driver", webDriverPath);
                        FirefoxOptions options = new FirefoxOptions();
                        //                      options.setBinary(webDriverPath);
                        driver = new FirefoxDriver(options);
                    }
                }
            } catch (Exception e) {
                if (!e.getMessage().contains("Current browser version")) {
                    String[] lines = e.getMessage().split("\n");

                    String msg1 = "";
                    String msg2 = "";

                    for (String line : lines) {
                        int indexMessage = line.indexOf("Message: ");
                        if (indexMessage != -1) {
                            msg1 = line.substring(indexMessage + "Message: ".length());
                        }

                        int indexBrowserVersion = line.indexOf("Current browser version");
                        if (indexBrowserVersion != -1) {
                            msg2 = line.substring(indexBrowserVersion);
                        }
                    }

                    ABRLogger.getInstance(ABRWebDriver.class).severe("Error Open URL: \n" + msg1 + "\n" + msg2);

                    performAction.errorMessage("Error Open URL", msg1, msg2, null, null, 260);
                }

                //                String errorMessage = e.getMessage();
                //                ABRLogger.getInstance(ABRWebDriver.class)
                //                        .fine("An error has occurred during driver.get(url) Load " + errorMessage);
                //
                //                // Split the message into chunks of 100 characters
                //                int maxLength = 100;
                //                int messageLength = errorMessage.length();
                //                int parts = (int) Math.ceil((double) messageLength / maxLength);
                //                String[] messageChunks = new String[parts];
                //
                //                for (int i = 0; i < parts; i++) {
                //                    int startIndex = i * maxLength;
                //                    int endIndex = Math.min(startIndex + maxLength, messageLength);
                //                    messageChunks[i] = errorMessage.substring(startIndex, endIndex);
                //                }
                //
                //                // Pass a meaningful message for further actions
                //                performAction.errorMessage(
                //                        "Error Open URL", messageChunks[0], messageChunks[1], messageChunks[2],
                // messageChunks[3], 0);
                //
                //                // Example: print or log the chunks if needed
                //                for (String chunk : messageChunks) {
                //                    ABRLogger.getInstance(ABRWebDriver.class).fine("Error chunk: " + chunk);
                //                }

                //                ABRLogger.getInstance(ABRWebDriver.class)
                //                        .severe("An error has occurred during WebDriver Load " + e.getMessage());
                //                JOptionPane.showMessageDialog(
                //                        null,
                //                        "An error has occurred during WebDriver Load: \nError:" + e.getMessage() +
                // "\nCause: "
                //                                + e.getCause(),
                //                        "Error in WebDriver Load",
                //                        JOptionPane.ERROR_MESSAGE);

                throw new UnsupportedOperationException(e.getMessage());
            }
        }
        driver.manage().window().maximize();
        if (Strings.isNullOrEmpty(url)) {
            ABRLogger.getInstance(ABRWebDriver.class).fine("URL IS EMPTY");
            //            JOptionPane.showMessageDialog(
            //                    null,
            //                    "An error has occurred during WebDriver Load: \nError:  URL IE NULL",
            //                    "Error in WebDriver Load",
            //                    JOptionPane.ERROR_MESSAGE);
        }

        try {
            driver.get(url);

            // Wait for the page to finish loading
            Thread.sleep(3000);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            wait.until(webDriver -> ((JavascriptExecutor) webDriver)
                    .executeScript("return document.readyState")
                    .equals("complete"));

        } catch (Exception e) {
            String errorMessage = e.getMessage();
            ABRLogger.getInstance(ABRWebDriver.class)
                    .fine("An error has occurred during driver.get(url) Load " + errorMessage);

            // Split the message into chunks of 100 characters
            int maxLength = 100;
            int messageLength = errorMessage.length();
            int parts = (int) Math.ceil((double) messageLength / maxLength);
            String[] messageChunks = new String[parts];

            for (int i = 0; i < parts; i++) {
                int startIndex = i * maxLength;
                int endIndex = Math.min(startIndex + maxLength, messageLength);
                messageChunks[i] = errorMessage.substring(startIndex, endIndex);
            }

            // Pass a meaningful message for further actions
            performAction.errorMessage(
                    "Error Open URL", messageChunks[0], messageChunks[1], messageChunks[2], messageChunks[3], 0);

            // Example: print or log the chunks if needed
            for (String chunk : messageChunks) {
                ABRLogger.getInstance(ABRWebDriver.class).fine("Error chunk: " + chunk);
            }

            return null;

            //            JOptionPane.showMessageDialog(
            //                    null,
            //                    "An error has occurred during WebDriver Load: \nError:" + e.getMessage() + " Cause: "
            //                            + e.getCause(),
            //                    "Error in WebDriver Load",
            //                    JOptionPane.ERROR_MESSAGE);
        }
        return this.driver;
    }

    private EdgeOptions buildOptionsEdge(String[] optionsConfigLines, String logFolder) {
        EdgeOptions optionsEdge = new EdgeOptions();
        // Options Config
        for (String line : optionsConfigLines) {
            if (line.startsWith("#")) {
                ABRLogger.getInstance(ABRWebDriver.class).fine("COMMENTED OPTIONS: " + line);
                continue;
            }

            ABRLogger.getInstance(ABRWebDriver.class).fine("WebDriver config: \n" + line);
            String[] config = line.split(":");
            if (config.length > 1) {
                if (config[0].equalsIgnoreCase("proxy")) {

                    if (config.length > 2) {
                        // Proxy details
                        //  String proxyAddress = "proxy_address:proxy_port";
                        String proxyAddress = String.format("%s:%s", config[1], config[2]);

                        // Configure proxy settings
                        Proxy proxy = new Proxy();
                        proxy.setHttpProxy(proxyAddress)
                                .setFtpProxy(proxyAddress)
                                .setSslProxy(proxyAddress);

                        optionsEdge.setProxy(proxy);
                    } else {
                        ABRLogger.getInstance(ABRWebDriver.class)
                                .severe("Error Check Options Config for Proxy is wrong");
                    }
                } else if (config[0].equalsIgnoreCase("browser_log")) {

                    // Create LoggingPreferences object
                    LoggingPreferences logs = new LoggingPreferences();
                    logs.enable(LogType.BROWSER, Level.ALL); // Enable browser logs
                    // Set the path where you want to save the log file
                    String logFilePath = logFolder + "_edge_browser.log"; // Replace with your desired log file path
                    // Specify the logging preferences
                    logs.enable(LogType.BROWSER, Level.ALL);
                    optionsEdge.setCapability(
                            "ms:edgeOptions",
                            "{verbose: true, loggingPrefs: {" + "\"browser\": \"ALL\", \"driver\": \"ALL\"}}");
                } else if (config[0].equalsIgnoreCase("argument")) {
                    optionsEdge.addArguments(config[1]);
                    //                        options.addArguments("--disable-infobars");
                    //                        options.addArguments("--disable-dev-shm-usage");
                    //                        options.addArguments("--no-sandbox");
                    //                        options.addArguments("--remote-debugging-port=9222");
                    //                        optionsChrome.setExperimentalOption(
                    //                                "excludeSwitches",
                    // Collections.singletonList("enable-automation"));
                    //
                }
            }
        }
        return optionsEdge;
    }

    private ChromeOptions buildOptionsChrome(String[] optionsConfigLines, String logFolder) {
        ChromeOptions optionsChrome = new ChromeOptions();
        // Options Config
        for (String line : optionsConfigLines) {
            if (line.startsWith("#")) {
                ABRLogger.getInstance(ABRWebDriver.class).fine("COMMENTED OPTIONS: " + line);
                continue;
            }

            ABRLogger.getInstance(ABRWebDriver.class).fine("WebDriver config: \n" + line);
            String[] config = line.split(":");
            if (config.length > 1) {
                if (config[0].equalsIgnoreCase("proxy")) {

                    if (config.length > 2) {
                        // Proxy details
                        //  String proxyAddress = "proxy_address:proxy_port";
                        String proxyAddress = String.format("%s:%s", config[1], config[2]);

                        // Configure proxy settings
                        Proxy proxy = new Proxy();
                        proxy.setHttpProxy(proxyAddress)
                                .setFtpProxy(proxyAddress)
                                .setSslProxy(proxyAddress);

                        optionsChrome.setProxy(proxy);
                    } else {
                        ABRLogger.getInstance(ABRWebDriver.class)
                                .severe("Error Check Options Config for Proxy is wrong");
                    }
                } else if (config[0].equalsIgnoreCase("browser_log")) {

                    // Create LoggingPreferences object
                    System.setProperty("webdriver.chrome.verboseLogging", "true");
                    System.setProperty("webdriver.chrome.logfile", logFolder + "\\_chrome_browser.log");
                } else if (config[0].equalsIgnoreCase("argument")) {

                    optionsChrome.addArguments(config[1]);
                    //                        options.addArguments("--disable-infobars");
                    //                        options.addArguments("--disable-dev-shm-usage");
                    //                        options.addArguments("--no-sandbox");
                    //                        options.addArguments("--remote-debugging-port=9222");
                    //                        optionsChrome.setExperimentalOption(
                    //                                "excludeSwitches",
                    // Collections.singletonList("enable-automation"));
                    //
                }
            }
        }
        return optionsChrome;
    }

    public void highlightElement(WebElement element) {
        applyCssToElement(element, "background-color:red");
    }

    public void dehighlightElement(WebElement element) {
        applyCssToElement(element, "background-color:");
    }

    public void applyCssToElement(WebElement element, String cssToApply) {
        String script = scriptFactory.forElement(element).createSetStyleScript(cssToApply);
        runScript(element, script);
    }

    public List<String> extractAttributes(WebElement element, WebElementAttributeEnum... attributes) {
        return extractAttributes(element).stream()
                .filter(s -> Arrays.stream(attributes)
                        .anyMatch(attr -> attr.getValue().equals(s)))
                .collect(Collectors.toList());
    }

    public List<String> extractAttributes(WebElement element) {
        String script = scriptFactory.forElement(element).extractAttributesScript();
        return runScript(element, script);
    }

    private <T> T runScript(WebElement element, String script) {
        if (elementExists(element)) {
            return runScript(script);
        }
        return null;
    }

    private boolean elementExists(WebElement element) {
        String reference = scriptFactory.forElement(element).elementReferenceScript();
        return runScript(reference) != null;
    }

    private <T> T runScript(String script) {
        if (driver == null) {
            throw new ABRWebDriverNotStartedException();
        }
        JavascriptExecutor executor = (JavascriptExecutor) driver;
        return (T) executor.executeScript(script);
    }

    public void closeDriver() {
        this.driver.quit();
        this.driver = null;
    }

    public WebDriver getDriver() {
        return this.driver;
    }

    public void setDriver(WebDriver webDriver) {
        this.driver = webDriver;
    }
}
