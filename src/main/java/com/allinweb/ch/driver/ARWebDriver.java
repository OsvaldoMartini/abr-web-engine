package com.allinweb.ch.driver;

import com.allinweb.ch.builder.WebElementAttributeEnum;
import com.allinweb.ch.builder.WebElementScriptFactory;
import com.allinweb.ch.facade.PerformMessage;
import com.allinweb.ch.facade.PerformPreLoad;
import com.allinweb.ch.facade.SingletonSupplier;
import com.allinweb.ch.util.ARConstants;
import com.allinweb.ch.util.ARLogger;
import com.allinweb.ch.util.ARPropertyEnum;
import com.allinweb.ch.util.ARPropertyManager;
import com.google.common.base.Strings;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javafx.collections.ObservableList;
import javax.swing.*;
import lombok.Data;
import org.openqa.selenium.Capabilities;
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
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

@Data
public class ARWebDriver {

    // Static final variable to hold the singleton instance
    protected static final SingletonSupplier<ARWebDriver> instance = () -> new ARWebDriver();

    // Public method to access the singleton instance
    public static ARWebDriver getInstance() {
        return instance.get();
    }

    // Private constructor to prevent instantiation
    public ARWebDriver() {}

    private List<WebDriver> webDriverList = new ArrayList<>();
    private PerformMessage performMessage;
    private PerformPreLoad performPreLoad;
    private WebDriver currentDriver;
    private String edgeVersion;
    private String webDriverEdgeVersion;
    private String webDriverPath;

    public void initialize(
            ObservableList<WebDriver> webDriverList, PerformMessage performMessage, PerformPreLoad performPreLoad) {
        this.webDriverList = webDriverList;
        this.performMessage = performMessage;
        this.performPreLoad = performPreLoad;
    }

    private final WebElementScriptFactory scriptFactory = new WebElementScriptFactory();

    // Method to add WebDriver instances
    public void addWebDriver(WebDriver driver) {
        webDriverList.add(driver);
    }

    private String getEdgeWebDriverVersion(String webDriverPath) {
        try {
            ProcessBuilder builder = new ProcessBuilder(webDriverPath, "--version");
            builder.redirectErrorStream(true);
            Process process = builder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null && line.contains("MSEdgeDriver")) {
                    // Expected output: "MSEdgeDriver 122.0.2365.66 ..."
                    return line.split(" ")[1];
                }
            }
        } catch (IOException e) {
            ARLogger.getInstance(ARWebDriver.class).info("Error getting Edge WebDriver version: " + e.getMessage());
        }
        return "unknown";
    }

    private String getEdgeBrowserVersion(WebDriver driver) {
        try {
            Capabilities capabilities = ((RemoteWebDriver) driver).getCapabilities();
            return capabilities.getBrowserVersion();
        } catch (Exception e) {
            ARLogger.getInstance(ARWebDriver.class).info("Error getting Edge browser version: " + e.getMessage());
            return "unknown";
        }
    }

    public WebDriver getDriverEdge(EdgeOptions options) {
        if (this.currentDriver == null) {
            synchronized (ARWebDriver.class) {
                if (this.currentDriver == null) {
                    if (options != null) {
                        this.currentDriver = new EdgeDriver(options);
                        addWebDriver(this.currentDriver);
                        this.edgeVersion =
                                getEdgeBrowserVersion(this.currentDriver); // Detect version on driver creation
                        ARLogger.getInstance(ARWebDriver.class).fine("Detected Edge Version: " + this.edgeVersion);
                    } else {
                        this.currentDriver = new EdgeDriver();
                        this.edgeVersion =
                                getEdgeBrowserVersion(this.currentDriver); // Detect version on driver creation
                        ARLogger.getInstance(ARWebDriver.class).fine("Detected Edge Version: " + this.edgeVersion);
                    }
                }
            }
        }
        return this.currentDriver;
    }

    public WebDriver getDriverFireFox(FirefoxOptions options) {
        if (this.currentDriver == null) {
            synchronized (ARWebDriver.class) {
                if (this.currentDriver == null) {
                    if (options != null) {
                        this.currentDriver = new FirefoxDriver(options);
                        addWebDriver(this.currentDriver);
                    } else {
                        this.currentDriver = new FirefoxDriver();
                    }
                }
            }
        }
        return this.currentDriver;
    }

    public WebDriver getDriverChrome(ChromeOptions options) {
        if (this.currentDriver == null) {
            synchronized (ARWebDriver.class) {
                if (this.currentDriver == null) {
                    if (options != null) {
                        this.currentDriver = new ChromeDriver(options);
                        addWebDriver(this.currentDriver);
                    } else {
                        this.currentDriver = new ChromeDriver();
                    }
                }
            }
        }
        return currentDriver;
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

    public WebDriver openDriver(
            String browserType,
            String webDriverPath,
            String url,
            String optionsConfig,
            String[] dataArray,
            boolean searchHiddenFields,
            int port) {

        this.webDriverPath = webDriverPath;

        this.webDriverEdgeVersion = getEdgeWebDriverVersion(webDriverPath);

        if (Strings.isNullOrEmpty(url.trim())) {
            ARLogger.getInstance(ARWebDriver.class).fine("URL IS EMPTY");

            performMessage.errorMessage("URL IS EMPTY", "URL Web Browser is Empty", null, null, null, 0);

            return null;
        }

        String lineSeparator = identifyLineSeparator(optionsConfig);

        // Split the text into lines using the detected line separator
        String[] optionsConfigLines = new String[0];
        try {

            optionsConfigLines = optionsConfig.split(lineSeparator);
        } catch (Exception ex) {
            ARLogger.getInstance(ARWebDriver.class).severe("Error WebDriver config Options : \n" + ex.getMessage());
        }

        ARLogger.getInstance(ARWebDriver.class).fine("Going to call WebDriver for \n" + url);

        ARPropertyManager managerProps = ARPropertyManager.getInstance();

        if (Strings.isNullOrEmpty(webDriverPath)) {
            ARLogger.getInstance(ARWebDriver.class).fine("URL IS EMPTY");
        }

        //        if (driver == null) {
        String logFolder = ARPropertyManager.getInstance().getProperty(ARPropertyEnum.FOLDER_PATH_LOG);
        try {
            switch (browserType) {
                case ARConstants.CHROME -> {
                    //                        String driverPath = webDriverPath + "\\chrome.exe";
                    if (!(new File(webDriverPath)).exists()) {
                        ARLogger.getInstance(ARWebDriver.class).fine("Web Driver NOT EXIST \n" + webDriverPath);
                    }

                    // "\\_chrome_browser.log");

                    System.setProperty("webdriver.chrome.driver", webDriverPath);

                    ChromeOptions optionsChrome = buildOptionsChrome(optionsConfigLines, logFolder);
                    optionsChrome.addArguments("data:,"); // This is the key to opening a blank page

                    if (optionsChrome != null) {
                        this.currentDriver = getDriverChrome(optionsChrome);
                        this.currentDriver.get("about:blank");
                    } else {
                        this.currentDriver = getDriverChrome(null);
                        this.currentDriver.get("about:blank");
                    }
                }
                case ARConstants.EDGE -> {
                    //                        String driverPath = webDriverPath + "\\msedgedriver.exe";
                    if (!(new File(webDriverPath)).exists()) {
                        ARLogger.getInstance(ARWebDriver.class).fine("Web Driver NOT EXIST \n" + webDriverPath);
                    }
                    // Set path to Edge WebDriver executable
                    System.setProperty("webdriver.edge.driver", webDriverPath);

                    String userDataDir = System.getProperty("java.io.tmpdir") + File.separator + "edge-user-data-"
                            + UUID.randomUUID();

                    // Configure EdgeOptions
                    EdgeOptions optionsEdge = new EdgeOptions();
                    optionsEdge.addArguments("--user-data-dir=" + userDataDir);
                    optionsEdge.addArguments("data:,"); // This is the key to opening a blank page

                    optionsEdge.addArguments("--remote-allow-origins=*"); // Required for some Edge versions
                    optionsEdge.addArguments("--start-maximized"); // Opens browser in full-screen
                    optionsEdge.addArguments("--disable-gpu"); // Fixes potential rendering issues
                    optionsEdge.addArguments("--disable-infobars"); // Disable DevTools
                    optionsEdge.addArguments("--no-sandbox"); // Bypass OS security model
                    optionsEdge.addArguments("--disable-dev-shm-usage"); // Prevents resource exhaustion
                    // options.addArguments("--headless"); // if you want run it in headless mode.

                    // Configure Edge options
                    optionsEdge = buildOptionsEdge(optionsConfigLines, logFolder);

                    if (optionsEdge != null) {
                        this.currentDriver = getDriverEdge(optionsEdge);
                        this.currentDriver.get("about:blank");
                    } else {
                        this.currentDriver = getDriverEdge(null); // or pass options
                        this.currentDriver.get("about:blank");
                    }
                }
                case ARConstants.FIREFOX -> {
                    //                        String driverPath = webDriverPath + "\\geckodriver.exe";
                    if (!(new File(webDriverPath)).exists()) {
                        ARLogger.getInstance(ARWebDriver.class).fine("Web Driver NOT EXIST \n" + webDriverPath);
                    }
                    System.setProperty("webdriver.gecko.driver", webDriverPath);
                    FirefoxOptions options = new FirefoxOptions();
                    //                      options.setBinary(webDriverPath);
                    //                    driver = new FirefoxDriver(options);
                    if (options != null) {
                        this.currentDriver = getDriverFireFox(options);
                        this.currentDriver.get("about:blank");
                    } else {
                        this.currentDriver = getDriverFireFox(null); // or pass options
                        this.currentDriver.get("about:blank");
                    }
                }
            }
        } catch (Exception error) {
            throw new UnsupportedOperationException(error.getMessage());
        }
        //        }

        this.currentDriver.manage().window().maximize();

        try {
            //            performPreLoad.dynamicLoadAlerts(driver, url, dataArray, searchHiddenFields, port);
            //            performPreLoad.dynamicLoadElementsDTO(driver, url, dataArray, searchHiddenFields, port);

            this.currentDriver.get(url);
            //            performPreLoad.dynamicLoadAlerts(driver, url, dataArray, searchHiddenFields, port);

            //            performPreLoad.dynamicLoadElementsDTO(driver, url, dataArray, searchHiddenFields, port);

            // Wait for the page to finish loading
            Thread.sleep(3000);
            WebDriverWait wait = new WebDriverWait(this.currentDriver, Duration.ofSeconds(5));
            wait.until(webDriver -> ((JavascriptExecutor) webDriver)
                    .executeScript("return document.readyState")
                    .equals("complete"));

        } catch (Exception e) {

            String errorMessage = e.getMessage();
            ARLogger.getInstance(ARWebDriver.class)
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
            performMessage.errorMessage(
                    "Error Open URL", messageChunks[0], messageChunks[1], messageChunks[2], messageChunks[3], 0);

            // Example: print or log the chunks if needed
            for (String chunk : messageChunks) {
                ARLogger.getInstance(ARWebDriver.class).fine("Error chunk: " + chunk);
            }
            return null;
        }
        return this.currentDriver;
    }

    private EdgeOptions buildOptionsEdge(String[] optionsConfigLines, String logFolder) {
        EdgeOptions optionsEdge = new EdgeOptions();
        // Options Config
        optionsEdge.addArguments("--user-data-dir=" + System.getProperty("java.io.tmpdir") + "/edge-profile-"
                + System.currentTimeMillis());
        for (String line : optionsConfigLines) {
            if (line.startsWith("#")) {
                ARLogger.getInstance(ARWebDriver.class).fine("COMMENTED OPTIONS: " + line);
                continue;
            }

            ARLogger.getInstance(ARWebDriver.class).fine("WebDriver config: \n" + line);
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
                        ARLogger.getInstance(ARWebDriver.class).severe("Error Check Options Config for Proxy is wrong");
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
                } else if (config[0].startsWith("arg")) {
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
        optionsChrome.addArguments("--user-data-dir=" + System.getProperty("java.io.tmpdir") + "/edge-profile-"
                + System.currentTimeMillis());

        // Options Config
        for (String line : optionsConfigLines) {
            if (line.startsWith("#")) {
                ARLogger.getInstance(ARWebDriver.class).fine("COMMENTED OPTIONS: " + line);
                continue;
            }

            ARLogger.getInstance(ARWebDriver.class).fine("WebDriver config: \n" + line);
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
                        ARLogger.getInstance(ARWebDriver.class).severe("Error Check Options Config for Proxy is wrong");
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
        if (this.currentDriver == null) {
            throw new ARWebDriverNotStartedException();
        }
        JavascriptExecutor executor = (JavascriptExecutor) this.currentDriver;
        return (T) executor.executeScript(script);
    }

    public boolean isBrowserClosed(ARWebDriver arWebDriver) {
        try {
            this.currentDriver.getTitle(); // Try accessing a property
            return false; // If no exception, browser is open
        } catch (Exception e) {
            return true; // If exception occurs, browser is closed
        }
    }
}
