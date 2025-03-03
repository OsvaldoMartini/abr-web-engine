package com.allinweb.ch.supportTypes;

import com.allinweb.ch.builder.WebElementAttributeEnum;
import com.allinweb.ch.component.model.BotJobLoadDTO;
import com.allinweb.ch.component.model.ComplexInstructionLoadDTO;
import com.allinweb.ch.component.model.InstructionLoadDTO;
import com.allinweb.ch.component.model.InstructionReferenceLoadDTO;
import com.allinweb.ch.cryptingAlgorithm.CryptationAlgorithm;
import com.allinweb.ch.driver.ARWebDriver;
import com.allinweb.ch.facade.PerformActions;
import com.allinweb.ch.readersAndWriters.ExcelWriter;
import com.allinweb.ch.util.*;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

public class WebPage {

    // Very important sequence on initiation
    private static ARPriorities abrPriorities;

    // Static block to initialize
    static {
        abrPriorities = ARPriorities.getInstance();
    }

    private List<BotJobLoadDTO> botLoadJobs = new ArrayList<>();

    private ARWebDriver arWebDriver;
    private WebDriver driver;
    private static Wait<WebDriver> waitForPage;
    private static Wait<WebDriver> waitForAction;
    private boolean justCalledRefreshPage = false;
    private String priority;

    private Map<String, String> mapOperators;

    public WebPage(
            String driverType, String url, String priority, String optionsConfig, Map<String, String> mapOperators) {

        this.mapOperators = mapOperators;

        this.priority = priority;

        arWebDriver = new ARWebDriver();

        this.driver = arWebDriver.openDriver(url, optionsConfig);

        //        this.driver = initDriver(driverType);
        if (waitForPage == null) {
            String updateTimeout =
                    ARPropertyManager.getInstance().getProperty(ARPropertyEnum.WEBDRIVER_PAGE_UPDATE_TIMEOUT_SEC);
            String interactionTimeout =
                    ARPropertyManager.getInstance().getProperty(ARPropertyEnum.WEBDRIVER_PAGE_UPDATE_TIMEOUT_SEC);
            waitForPage = new WebDriverWait(driver, Duration.ofSeconds(Integer.parseInt(updateTimeout)));
            waitForAction = new WebDriverWait(driver, Duration.ofSeconds(Integer.parseInt(interactionTimeout)));
        }
        openBrowser(url);
    }

    public WebDriver getDriver() {
        return driver;
    }

    public ARWebDriver getAbrWebDriver() {
        return arWebDriver;
    }

    public WebDriver initDriver(String driverType) {
        if (driverType.equalsIgnoreCase(ARConstants.FIREFOX)) {
            FirefoxOptions options = new FirefoxOptions();
            options.setBinary(ARConstants.CURRENT_PATH + "\\geckodriver.exe");
            driver = new FirefoxDriver(options);

        } else if (driverType.equalsIgnoreCase(ARConstants.EDGE)) {
            System.setProperty("webdriver.edge.driver", ARConstants.CURRENT_PATH + "\\msedgedriver.exe");
            EdgeOptions options = new EdgeOptions();
            options.setExperimentalOption("useAutomationExtension", false);
            options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
            driver = new EdgeDriver(options);

        } else if (driverType.equalsIgnoreCase(ARConstants.CHROME)) {

            ChromeOptions options = new ChromeOptions();
            options.setBinary(ARConstants.CURRENT_PATH + "\\chrome\\chrome.exe");
            options.setBinary("C:/Program Files (x86)/Google/Chrome/Application/chrome.exe");
            options.setExperimentalOption("useAutomationExtension", false);
            options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
            driver = new ChromeDriver(options);

        } else {
            WebDriverManager.firefoxdriver().setup();
            driver = new FirefoxDriver();
        }

        driver.manage().window().maximize();
        return driver;
    }

    public void openBrowser(String url) {
        try {
            driver.get(url);

            // Wait for the page to finish loading
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            wait.until(webDriver -> ((JavascriptExecutor) webDriver)
                    .executeScript("return document.readyState")
                    .equals("complete"));

        } catch (Exception e) {
            ARLogger.getInstance(ARWebDriver.class)
                    .fine("An error has occurred during driver.get(url) Load " + e.getMessage());
            JOptionPane.showMessageDialog(
                    null,
                    "An error has occurred during WebDriver Load: \nError:" + e.getMessage() + " Cause: "
                            + e.getCause(),
                    "Error in WebDriver Load",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static String extractTagName(String xPath) {
        // Find the position of the last '/'
        int lastSlashIndex = xPath.lastIndexOf("/");

        // Extract the substring after the last '/'
        String lastSegment = xPath.substring(lastSlashIndex + 1);

        // If the last segment contains '[', extract the tag name before it
        int bracketIndex = lastSegment.indexOf("[");
        if (bracketIndex != -1) {
            return lastSegment.substring(0, bracketIndex);
        }

        // Return the last segment as the tag name
        return lastSegment;
    }

    /**
     * Removes the trailing slash from an XPath if it ends with one.
     *
     * @param xPath the original XPath string
     * @return the cleaned XPath string without a trailing slash
     */
    public static String removeTrailingSlash(String xPath) {
        if (xPath != null && xPath.endsWith("/")) {
            return xPath.substring(0, xPath.length() - 1);
        }
        return xPath;
    }

    private WebElement locateElement(InstructionLoadDTO instruction, int botJobId) {

        String instructionPath = instruction.getXpath();
        String tagName = null;
        try {
            tagName = removeTrailingSlash(instructionPath);
            tagName = extractTagName(instructionPath);
        } catch (Exception e) {
            ARLogger.getInstance(WebPage.class)
                    .fine(String.format(
                            "Error RemoveTrailingSlash for %s   \nxPath  %s\nCause: %s",
                            tagName, instructionPath, e.getMessage()));
        }
        List<InstructionReferenceLoadDTO> instructionReferenceList = instruction.getInstructionReferenceLoadDTOList();

        if (instructionReferenceList.size() == 0) {
            ARLogger.getInstance(PerformActions.class)
                    .warning("####    Not XPath to Be Located!   ####"
                            + "\n####    Remove and Re-Scan the Failed Field Again   ####");
            return null;
        }

        waitPage();

        // If Not Loaded get if the JobId Changed
        if (abrPriorities.getJobId() == null) {
            abrPriorities.setJobId(botJobId);
            if (this.priority != null) {
                abrPriorities.loadPrioritiesFromString(this.priority);
            } else {
                abrPriorities.loadPriorities();
            }
        } else if (abrPriorities.getJobId() != botJobId) {
            abrPriorities.setJobId(botJobId);
            if (instruction.getPriority() != null) {
                abrPriorities.loadPrioritiesFromString(instruction.getPriority());
            } else {
                abrPriorities.loadPriorities();
            }
        }

        List<com.allinweb.ch.util.Priority> priorityList = abrPriorities.getAllPriorityList();
        if (abrPriorities.getAllPriorityList().size() > 0) {

            //            if (instruction.getActionCustomMaxWaitSec() > 5) {
            //                instruction.setActionCustomMaxWaitSec(5);
            //            }
            WebElement elementFound = null;
            //            for (int i = 0; i < priorityList.size() && elementFound == null; i++) {
            for (com.allinweb.ch.util.Priority priority : abrPriorities.getAllPriorityList()) {
                if (elementFound != null) {
                    break;
                }

                PriorityTypeEnum priorityTypeEnum = null;
                try {
                    priorityTypeEnum = PriorityTypeEnum.getPriorityType(
                            priority.getPriorityType().toString());
                } catch (Exception e) {
                    System.out.println(String.format("The ENUM: was not defined!"));
                    continue;
                }
                if (priorityTypeEnum == null) {
                    System.out.println("Define priorities!");
                    return null;
                }

                //            Optional<InstructionReferenceDTO> reference = instructionReferenceList.stream()
                //                    .filter(ref -> ref.getReferenceType().equals(priority.getName()))
                //                    .findFirst();

                // Find the first matching instruction reference
                Optional<InstructionReferenceLoadDTO> instructionReference = instructionReferenceList.stream()
                        .filter(reference -> priority.getName().stream()
                                .anyMatch(p -> p.equalsIgnoreCase(reference.getReferenceType())))
                        .findFirst();
                // Print or process the first matching instruction reference
                if (instructionReference.isPresent()) {

                    System.out.println(String.format(
                            "Search for %s   Type:  %s   Value: %s",
                            priority.getName(),
                            instructionReference.get().getReferenceType(),
                            instructionReference.get().getValue()));
                    ARLogger.getInstance(WebPage.class)
                            .fine(String.format(
                                    "Search for %s   Type:  %s   Value: %s",
                                    priority.getName(),
                                    instructionReference.get().getReferenceType(),
                                    instructionReference.get().getValue()));
                }
                if (instructionReference.isPresent()) {
                    List<By> criterias = null;
                    switch (priority.getPriorityType()) {
                        case xpath -> criterias = Arrays.asList(
                                new By[] {By.xpath(instructionReference.get().getValue())});
                        case attribute -> criterias = convertToCriteriaList(
                                tagName,
                                priority.getName(),
                                instructionReference.get().getValue());
                            //                                criteria = By.cssSelector(tagName + "[" +
                            // priority.getName() + "='" + instructionReference.get().getValue() + "']");
                        case coordinates -> {} // System.out.println("coordinates case");
                        case ById -> {} // System.out.println("ById case");
                        case ByClassName -> {} // System.out.println("Default case");
                        case ByName -> {} // System.out.println("Default case");
                        case ByTagName -> {} // System.out.println("Default case");
                        case ByLinkText -> {} // System.out.println("Default case");
                        case ByPartialLinkText -> {} // System.out.println("Default case");
                        case ByCssSelector -> {} // System.out.println("Default case"); //      ".nav-menu li";
                        case ExecuteScript -> {} // System.out.println("Default case"); //      "return
                            // document.getElementById('search-top')");
                        case createXPath -> {} // System.out.println("Default case"); //         Generates XPath
                            // Recursive tom the Elements Found
                        case dynamic -> {} // System.out.println("Default case"); //         Generates Dynamic Action ->
                            // Click, Hover, Etc.
                        case jsoup -> {} // System.out.println("Default case");
                    }

                    // Actualy here is Calling the Actions
                    if (criterias != null) {

                        for (By criteria : criterias) {
                            List<WebElement> foundElementList =
                                    arWebDriver.getDriver().findElements(criteria);

                            //                            try {
                            //                                elementFound = scroolUntilFindElement(criteria);
                            //                            } catch (Exception e) {
                            //                                e.printStackTrace();
                            //                            }
                            //                            if (elementFound != null) {
                            //                                break;
                            //                            }
                            if (foundElementList != null && foundElementList.size() > 0) {
                                if (justCalledRefreshPage) {
                                    justCalledRefreshPage = false;
                                    try {
                                        waitForPage.until(ExpectedConditions.visibilityOfElementLocated(criteria));
                                    } catch (Exception e) {
                                        ARLogger.getInstance(WebPage.class)
                                                .fine(String.format(
                                                        "Could Not Find Elements %s   \nCriteria  %s\nCause: %s",
                                                        instructionPath, criteria, e.getMessage()));
                                    }
                                } else if (instruction.getActionCustomMaxWaitSec() != null) {
                                    try {

                                        new WebDriverWait(
                                                        arWebDriver.getDriver(),
                                                        Duration.ofSeconds(instruction.getActionCustomMaxWaitSec()))
                                                .until(ExpectedConditions.presenceOfElementLocated(criteria));
                                    } catch (Exception e) {
                                        ARLogger.getInstance(WebPage.class)
                                                .fine(String.format(
                                                        "Could Not Find Elements %s   \nCriteria  %s\nCause: %s",
                                                        instructionPath, criteria, e.getMessage()));
                                    }
                                } else {
                                    try {

                                        waitForAction.until(ExpectedConditions.visibilityOfElementLocated(criteria));
                                    } catch (Exception e) {
                                        ARLogger.getInstance(WebPage.class)
                                                .fine(String.format(
                                                        "Could Not Find Elements %s   \nCriteria  %s\nCause: %s",
                                                        instructionPath, criteria, e.getMessage()));
                                    }
                                }
                                int k = 0;
                                //                            MAYBE THIS SHOUL BE NOT NECESSARY  USE UNIQUE ID   OR
                                // SESSION  SAVED TO GET THE SAME XPATHORELEMENT
                                if (foundElementList.size() > 1) {
                                    while (elementFound == null && k < foundElementList.size()) {
                                        String xpath = ARWebUtil.extractXPath(
                                                foundElementList.get(k).toString());

                                        // Second Verification for XPath Found
                                        if (instructionReference.isPresent()
                                                && xpath.equals(instructionReference
                                                        .get()
                                                        .getValue())) {
                                            elementFound = foundElementList.get(k);
                                            break;
                                        }
                                        k++;
                                    }
                                } else {
                                    elementFound = foundElementList.get(0);
                                }
                            }
                        }
                    }
                }
            }
            return elementFound;
        } else {
            return null;
        }
    }

    private WebElement scroolUntilFindElement(By criteria) {
        // Set the JavaScript executor
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Define the element locator
        //        By elementLocator = By.id("desiredElementId");

        int maxScrollAttempts = 2;
        int currentScrollAttempts = 0;

        // Loop to keep scrolling until the element is found
        while (currentScrollAttempts < maxScrollAttempts) {
            try {
                // Find the element
                WebElement element = driver.findElement(criteria);

                // Check if the element is displayed
                if (element.isDisplayed()) {
                    System.out.println("Element found!");
                    break;
                }
            } catch (Exception e) {
                // If element is not found, catch the exception and scroll down
                //                js.executeScript("window.scrollBy(0, window.innerHeight);");
                currentScrollAttempts++;
            }

            // Optionally, add a sleep to avoid excessive scrolling and hitting the server too frequently
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Interact with the element (example: click the element)
        return driver.findElement(criteria);
    }

    private void executeActionsAtInstructionCoordinates(InstructionLoadDTO instruction, Map<String, String> data)
            throws Exception {

        List<com.allinweb.ch.util.Priority> priorityList = ARPriorities.getAllPriorityList();
        Optional<com.allinweb.ch.util.Priority> priority = priorityList.stream()
                .filter(p -> p.getPriorityType() == PriorityTypeEnum.coordinates)
                .findFirst();
        if (priority.isPresent()) {
            List<InstructionReferenceLoadDTO> instructionReferenceList =
                    instruction.getInstructionReferenceLoadDTOList();
            Optional<InstructionReferenceLoadDTO> reference = instructionReferenceList.stream()
                    .filter(ref -> ref.getReferenceType().equals(priority.get().getName()))
                    .findFirst();
            int x = 0;
            int y = 0;
            int xCoord = 0;
            int yCoord = 0;
            if (reference.isPresent()) {
                String[] coordinates = reference.get().getValue().split(ARConstants.FIELDS_SEPARATOR);
                x = Integer.parseInt(coordinates[0]);
                y = Integer.parseInt(coordinates[1]);
                int maxHeight =
                        arWebDriver.getDriver().manage().window().getSize().getHeight();
                int maxWidth =
                        arWebDriver.getDriver().manage().window().getSize().getWidth();
                int offsetY = y - maxHeight;
                int offsetX = x - maxWidth;
                xCoord = x > maxWidth ? x - offsetX : x;
                yCoord = y > maxHeight ? y - offsetY : y;
            }
            String[] actions = instruction.getActions().split(ARConstants.ACTIONS_AND_PATHS_SPLITTER);
            for (String action : actions) {
                switch (String.valueOf(action.charAt(0))) {
                    case ARConstants.VISUALIZE:
                        scrollToCoordinates(x, y);
                        break;
                    case ARConstants.CLICK:
                        scrollToCoordinates(x, y);
                        onHoldForSeconds(null);
                        clickAtCoordinates(xCoord, yCoord);
                        break;
                    case ARConstants.INSERT:
                        scrollToCoordinates(x, y);
                        onHoldForSeconds(null);
                        clickAtCoordinates(xCoord, yCoord);
                        onHoldForSeconds(null);
                        typeCharacters(instruction, action, data);
                        break;
                    case ARConstants.HOLD:
                        onHoldForSeconds(instruction);
                        break;
                    case ARConstants.REFRESH_ONLY:
                        refreshPage();
                        break;
                    case ARConstants.QUIT:
                        quit(0);
                        break;
                    case ARConstants.SCREEN:
                        // screenshot();
                        break;
                    case ARConstants.EXTRACT_FIELD:
                        break;
                    case ARConstants.LIST_OPERATION:
                }
                onHoldForSeconds(null);
            }
        }
    }

    private void scrollToCoordinates(int x, int y) {
        int maxHeight = driver.manage().window().getSize().getHeight();
        int maxWidth = driver.manage().window().getSize().getWidth();
        int offsetY = y - maxHeight;
        int offsetX = x - maxWidth;
        if (offsetX > 0 || offsetY > 0) {
            String script = "function getScrollableParent(element){\n" + "    console.log(\"finding\");"
                    + "    let value = window.getComputedStyle(element).overflowY;\n"
                    + "    if(value !== \"scroll\" && value !== \"auto\"){\n"
                    + "        return getScrollableParent(element.parentNode);\n"
                    + "    }\n"
                    + "    return element;\n"
                    + "}\n"
                    + "getScrollableParent(document.elementFromPoint("
                    + (maxWidth / 2) + "," + (maxHeight / 2)
                    + ")).scrollTo(" + Math.max(offsetX, 0) + "," + Math.max(offsetY, 0) + ");" + "return true;";
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until((item) -> (Boolean) ((JavascriptExecutor) driver).executeScript(script));
        }
    }

    private void clickAtCoordinates(int x, int y) {
        /*
        String script = "function createCircle(x, y, diameter) {\n" +
                "    const randomColor = Math.floor(Math.random()*16777215).toString(16);\n" +
                "\n" +
                "    return `\n" +
                "    <svg style='height:100%;width:100%;position:absolute;top:0;z-index:9999'><circle\n" +
                "        cx=\"${x}\"\n" +
                "      cy=\"${y}\"\n" +
                "      r=\"${diameter/2}\"\n" +
                "      fill=\"#${randomColor}\"\n" +
                "    ></circle></svg>\n" +
                "  `;\n" +
                "}\n" +
                "\n" +
                "function pri(ev){\n" +
                "    console.log(ev);\n" +
                "    document.body.innerHTML += createCircle(ev.pageX,ev.pageY,10);\n" +
                "}\n" +
                "\n" +
                "window.addEventListener(\"click\", pri);";
        ((JavascriptExecutor)driver).executeScript(script);
         */
        new Actions(driver).moveToLocation(x, y).click().perform();
    }

    private void typeCharacters(InstructionLoadDTO instruction, String action, Map<String, String> data) {
        String value = null;
        if (data != null) {
            String[] arr = UtilsMethods.splitIfContains(action, ARConstants.ACTION_SPECIFICATIONS_SPLITTER);
            if (arr.length > 1) {
                String dataFieldName = arr[1].split(ARConstants.PATH_FIELD_SUBSTITUTION)[0];
                value = data.get(dataFieldName);
            }
        } else {
            value = instruction.getDefaultValue();
        }
        if (instruction.getCodified()) {
            value = CryptationAlgorithm.decrypt(value);
        }
        new Actions(arWebDriver.getDriver()).sendKeys(value).perform();
    }

    public String performActions(
            Map<String, String> data, InstructionLoadDTO instruction, int botJobId, String blockJobName)
            throws Exception {
        WebElement instructionElement = null;
        String[] actions = instruction.getActions().split(ARConstants.ACTIONS_AND_PATHS_SPLITTER);

        if (!StringUtils.isBlank(instruction.getXpath())) {
            instructionElement = locateElement(instruction, botJobId);
        }
        String result = null;
        if (instructionElement != null
                || actions[0].equals(ARConstants.HOLD)
                || actions[0].equals(ARConstants.QUIT)
                || actions[0].equals(ARConstants.SCREEN)) {

            for (String action : actions) {
                switch (String.valueOf(action.charAt(0))) {
                    case ARConstants.VISUALIZE:
                        scrollToElement(instructionElement);
                        break;
                    case ARConstants.CLICK:
                        result = "clickElement --> " + instruction.getName() + " --> "
                                + clickElement(instructionElement);
                        break;
                    case ARConstants.INSERT:
                        result = insertInElement(instructionElement, data, action, instruction);
                        break;
                    case ARConstants.LIST_OPERATION:
                        listOperation(instruction, data);
                        break;
                    case ARConstants.HOLD:
                        //                        executeAlert(instruction);
                        result = onHoldForSeconds(instruction);
                        break;
                    case ARConstants.REFRESH_ONLY:
                        refreshPage();
                        result = "refreshPage";
                        break;
                    case ARConstants.QUIT:
                        result = "Close Browser";
                        quit(0);
                        break;
                    case ARConstants.EXTRACT_FIELD:
                        result = "insertValueFieldNameInExcel-->"
                                + insertValueFieldNameInExcel(instructionElement, instruction, action, blockJobName);
                        break;
                    case ARConstants.SCREEN:
                        result = instruction.getName() + " --> " + blockJobName;
                        break;
                }
                onHoldForSeconds(null);
            }
        }
        //        } else {
        //            executeActionsAtInstructionCoordinates(instruction, data);
        //            onHoldForSeconds(null);
        //        }
        return result;
    }

    public String performActionOperator(
            InstructionLoadDTO instruction, String targetXPath, String action, String[] operations, String parentField)
            throws Exception {

        WebElement instructionElement = null;

        if (!StringUtils.isBlank(targetXPath)) {
            instructionElement = locateTargetElement(targetXPath, instruction.getActionCustomMaxWaitSec());
        }
        if (instructionElement != null) {

            switch (action) {
                case "SET":
                    insertTargetElement(instructionElement, operations[0], operations[1]);
                    return "SET_VALUE to (" + parentField + ") Var:" + operations[0] + " <-- " + operations[1];
                case "GET":
                    String valueElem = getValueInElement(instructionElement);
                    mapOperators.put(parentField, valueElem);
                    return "GET_VALUE from (" + parentField + ") Var" + operations[1] + " <-- " + valueElem;
                    //                    case "CK":
                    //                        if (operator.equalsIgnoreCase("=")) {
                    //                            result = "Equals -> "
                    //                                    + String.valueOf(getValueInElement(instructionElement)
                    //                                            .equalsIgnoreCase(valueOperator));
                    //                        } else if (operator.equalsIgnoreCase(">")) {
                    //                            result = "Greater -> "
                    //                                    + String.valueOf(getValueInElement(instructionElement)
                    //                                            .equalsIgnoreCase(valueOperator));
                    //                        }
                    //                        break;
            }
            onHoldForSeconds(null);
        }

        return null;
    }

    private String getValueInElement(WebElement element) throws Exception {
        UtilsMethods.exceptionIfNullWebElement(element);
        waitForAction.until(ExpectedConditions.visibilityOf(element));

        // Assuming instructionElement is an input field
        return element.getAttribute("value");
    }

    private String insertTargetElement(WebElement element, String fieldName, String dataFieldValue) throws Exception {
        UtilsMethods.exceptionIfNullWebElement(element);
        waitForAction.until(ExpectedConditions.visibilityOf(element));

        if (dataFieldValue != null) {
            element.clear();
            element.sendKeys(dataFieldValue);
            element.sendKeys(Keys.TAB);
        }

        return fieldName + "->" + dataFieldValue;
    }

    private WebElement locateTargetElement(String targetXPath, Integer actionCustomMaxWaitSec) {

        String tagName = null;
        try {
            tagName = removeTrailingSlash(targetXPath);
            tagName = extractTagName(targetXPath);
        } catch (Exception e) {
            ARLogger.getInstance(WebPage.class)
                    .fine(String.format(
                            "Error RemoveTrailingSlash for %s   \nxPath  %s\nCause: %s",
                            tagName, targetXPath, e.getMessage()));
        }

        waitPage();

        WebElement elementFound = null;
        List<By> criterias = Arrays.asList(new By[] {By.xpath(targetXPath)});

        // Actually here is Calling the Actions
        if (criterias != null) {

            for (By criteria : criterias) {
                List<WebElement> foundElementList = arWebDriver.getDriver().findElements(criteria);

                if (foundElementList != null && foundElementList.size() > 0) {
                    if (justCalledRefreshPage) {
                        justCalledRefreshPage = false;
                        try {
                            waitForPage.until(ExpectedConditions.visibilityOfElementLocated(criteria));
                        } catch (Exception e) {
                            ARLogger.getInstance(WebPage.class)
                                    .fine(String.format(
                                            "Could Not Find Elements %s   \nCriteria  %s\nCause: %s",
                                            targetXPath, criteria, e.getMessage()));
                        }
                    } else if (actionCustomMaxWaitSec != null) {
                        try {

                            new WebDriverWait(arWebDriver.getDriver(), Duration.ofSeconds(actionCustomMaxWaitSec))
                                    .until(ExpectedConditions.presenceOfElementLocated(criteria));
                        } catch (Exception e) {
                            ARLogger.getInstance(WebPage.class)
                                    .fine(String.format(
                                            "Could Not Find Elements %s   \nCriteria  %s\nCause: %s",
                                            targetXPath, criteria, e.getMessage()));
                        }
                    } else {
                        try {

                            waitForAction.until(ExpectedConditions.visibilityOfElementLocated(criteria));
                        } catch (Exception e) {
                            ARLogger.getInstance(WebPage.class)
                                    .fine(String.format(
                                            "Could Not Find Elements %s   \nCriteria  %s\nCause: %s",
                                            targetXPath, criteria, e.getMessage()));
                        }
                    }
                    if (foundElementList.size() > 0) {
                        elementFound = foundElementList.get(0);
                    }
                }
            }

            return elementFound;
        } else {
            return null;
        }
    }

    private String insertValueFieldNameInExcel(
            WebElement element, InstructionLoadDTO instruction, String action, String botJobName) {
        String innerHTMLValue = element.getAttribute(WebElementAttributeEnum.INNER_HTML.getValue());
        if (innerHTMLValue.contains("<div")) {
            int lastIndexOfDiv = innerHTMLValue.lastIndexOf("<div");
            innerHTMLValue = innerHTMLValue.substring(lastIndexOfDiv + 1);
            int firstIndexOfOpenTag = innerHTMLValue.indexOf("<");
            int firstIndexOfCloseTag = innerHTMLValue.indexOf(">");
            innerHTMLValue = innerHTMLValue.substring(firstIndexOfCloseTag + 1, firstIndexOfOpenTag);
        }
        String fieldName = null;
        String[] arr = UtilsMethods.splitIfContains(action, ARConstants.ACTION_SPECIFICATIONS_SPLITTER);
        if (arr.length > 1) {
            fieldName = arr[1].split(ARConstants.PATH_FIELD_SUBSTITUTION)[0];
        }

        new ExcelWriter(botJobName, null, false).withPurpose("excel").insertValueFieldName(fieldName, innerHTMLValue);
        return action + " fieldName " + fieldName;
    }

    private void listOperation(InstructionLoadDTO instructionDTO, Map<String, String> data) {

        /*
        TODO: Da rivedere, attualmente non del tutto funzionante
        Complex instruction string interpretation:
        [       0       ||       1      ||       2         ||    3    ||        4       ||  5   ||            6                ]
        [backward_button||forward_button||list_elements_tag||condition||expected_results||action||sub_element_on_execute_action]
        */
        List<ComplexInstructionLoadDTO> complexInstructionDTOS = instructionDTO.getComplexInstructionLoadDTOList();
        String[] complexActionParts =
                complexInstructionDTOS.get(0).getInstruction().split(ARConstants.COMPLEX_INSTRUCTION_SEPARATOR);
        List<WebElement> webElementList;
        WebElement forwardButton;
        WebElement backwardButton;
        boolean shouldContinue = true;

        boolean existNextPage;
        do {
            waitForPage.until(ExpectedConditions.visibilityOfElementLocated(By.tagName(complexActionParts[2])));
            backwardButton = arWebDriver.getDriver().findElement(By.xpath(complexActionParts[0]));
            forwardButton = arWebDriver.getDriver().findElement(By.xpath(complexActionParts[1]));
            webElementList = arWebDriver.getDriver().findElements(By.tagName(complexActionParts[2]));

            WebElement element;
            WebElement reasonWebElement;
            for (int i = 0; i < 5; i++) {

                if (i != 0) {
                    waitForPage.until(ExpectedConditions.visibilityOfElementLocated(By.tagName(complexActionParts[2])));
                    webElementList = arWebDriver.getDriver().findElements(By.tagName(complexActionParts[2]));
                }

                element = webElementList.get(i);
                try {
                    Thread.sleep(1000);

                    reasonWebElement = element.findElement(
                            By.xpath(".//div[@class='payments-table-field reason ng-star-inserted']"));
                    if (!UtilsMethods.testFixedCheck(reasonWebElement.getText())) {
                        continue;
                    }

                    clickElement(element.findElement(
                            By.xpath(".//button[@test-id='web-banking-payment-core.payment-ctx-action.button']")));
                    clickElement(
                            arWebDriver
                                    .getDriver()
                                    .findElement(
                                            By.xpath(
                                                    ".//button[@test-id='web-banking-payment-core.payment-ctx-action.payment-action-VIEW']")));

                    Thread.sleep(1000);
                    clickElement(arWebDriver
                            .getDriver()
                            .findElement(By.xpath(
                                    ".//button[@test-id='web-banking-common.export-to-file.single-file-button']")));

                    Thread.sleep(1000);
                    clickElement(
                            arWebDriver
                                    .getDriver()
                                    .findElement(
                                            By.xpath(
                                                    ".//avq-breadcrumb[@test-id='web-banking-portal.pages.payments-overview.breadcrumb']")));
                } catch (Exception e) {
                    System.out.println("Impossible execute operation on this element: " + element.toString());
                }
            }

            try {
                scrollToElement(forwardButton);
                clickElement(forwardButton);
                existNextPage = true;
            } catch (Exception e) {
                existNextPage = false;
            }

        } while (existNextPage && shouldContinue);
    }

    private String insertInElement(
            WebElement element, Map<String, String> data, String singleInstruction, InstructionLoadDTO instructionDTO)
            throws Exception {
        UtilsMethods.exceptionIfNullWebElement(element);
        waitForAction.until(ExpectedConditions.visibilityOf(element));
        String dataFieldName = "";
        String dataFieldValue = "";
        if (data != null) {
            String[] arr = UtilsMethods.splitIfContains(singleInstruction, ARConstants.ACTION_SPECIFICATIONS_SPLITTER);
            if (arr.length > 1) {
                dataFieldName = arr[1].split(ARConstants.PATH_FIELD_SUBSTITUTION)[0];

                dataFieldValue = data.get(dataFieldName);
                if (instructionDTO.getCodified()) {
                    dataFieldValue = CryptationAlgorithm.decrypt(dataFieldValue);
                }

                if (dataFieldValue != null) {
                    element.clear();
                    element.sendKeys(dataFieldValue);
                    element.sendKeys(Keys.TAB);

                } else {
                    element.sendKeys(UtilsMethods.generateRandomID(10));
                    element.sendKeys(Keys.TAB);
                }
            }
        } else if (instructionDTO.getDefaultValue() != null) {
            dataFieldValue = instructionDTO.getDefaultValue();
            if (instructionDTO.getCodified()) {
                dataFieldValue = CryptationAlgorithm.decrypt(dataFieldValue);
            }
            element.sendKeys(dataFieldValue);
        }

        return dataFieldName + "->" + dataFieldValue;
    }

    public void alertMessage(String message) {
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Escape the quotes in the JavaScript string
        String script = "let alertBox = document.createElement('div');" + "alertBox.style.position = 'fixed';"
                + "alertBox.style.top = '50%';"
                + "alertBox.style.left = '50%';"
                + "alertBox.style.transform = 'translate(-50%, -50%)';"
                + "alertBox.style.padding = '20px';"
                + "alertBox.style.backgroundColor = '#FFDA33';"
                + // Light orange background
                "alertBox.style.border = '2px solid #ff0000';"
                + // Red border
                "alertBox.style.borderRadius = '10px';"
                + "alertBox.style.boxShadow = '0 0 10px rgba(0, 0, 0, 0.5)';"
                + "alertBox.style.zIndex = '10000';"
                + "alertBox.innerHTML = \""
                + message.replace("\"", "\\\"") + "\";" + "document.body.appendChild(alertBox);";
        //                + "setTimeout(function() { document.body.removeChild(alertBox); }, 5000);"; // Auto-close
        // after 5
        // seconds

        js.executeScript(script);
    }

    private void scrollToElement(WebElement element) throws Exception {
        UtilsMethods.exceptionIfNullWebElement(element);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
    }

    private String clickElement(WebElement element) throws Exception {
        UtilsMethods.exceptionIfNullWebElement(element);
        if (!element.isEnabled()) {
            // throw new TimeoutException();
        }
        waitForAction.until(ExpectedConditions.visibilityOf(element).andThen(e -> {
            ((JavascriptExecutor) arWebDriver.getDriver()).executeScript("arguments[0].scrollIntoView(true);", element);
            return waitForAction.until(ExpectedConditions.elementToBeClickable(element));
        }));
        try {
            element.click();
            try {
                return ARWebUtil.extractXPath(element.toString());
            } catch (Exception e) {
                return "Error: ON ABRWebUtil.extractXPath for " + element.toString();
            }
        } catch (ElementClickInterceptedException e) {
            JavascriptExecutor jse = (JavascriptExecutor) arWebDriver.getDriver();
            jse.executeScript("arguments[0].click()", element);
            return "Error: " + ARWebUtil.extractXPath(element.toString());
        }
    }

    public synchronized String onHoldForSeconds(InstructionLoadDTO instruction) throws Exception {
        if (instruction != null) {
            Integer instructionSeconds = instruction.getOnHoldSeconds();
            if (instructionSeconds != null && instructionSeconds > 0) {
                wait(fromSecondsToMilliseconds(TimeUnit.SECONDS, instructionSeconds));
                return "HOLD" + "->" + instructionSeconds + " seconds";
            } else {
                String stopSeconds =
                        ARPropertyManager.getInstance().getProperty(ARPropertyEnum.DEFAULT_INSTRUCTION_STOP_SECONDS);
                wait(fromSecondsToMilliseconds(TimeUnit.SECONDS, Integer.parseInt(stopSeconds)));
                return "HOLD" + "->" + stopSeconds + " seconds";
            }
        } else {
            wait(400);
            return "HOLD" + "->" + "400 milliseconds";
        }
    }

    private void refreshPage() {
        driver.navigate().refresh();
        justCalledRefreshPage = true;
    }

    private void waitPage() {
        waitForPage.until(driver -> ((JavascriptExecutor) driver)
                .executeScript("return document.readyState")
                .equals("complete"));
    }

    private long fromSecondsToMilliseconds(TimeUnit timeUnit, int units) throws Exception {
        long milliseconds;

        switch (timeUnit) {
            case SECONDS:
                milliseconds = units * 1000L;
                break;

            case MINUTES:
                milliseconds = units * 1000L * 60L;
                break;

            default:
                throw new Exception("time unit: " + timeUnit.name() + " is not available for this operation");
        }
        return milliseconds;
    }

    public void quit(int status) {
        driver.quit();
        if (status > 0) {
            System.exit(status);
        }
    }

    public static List<By> convertToCriteriaList(String tagName, List<String> priorityToSearch, String someXPath) {
        // Split the string by commas and trim any leading/trailing whitespace from each element
        List<By> criteriaList = new ArrayList<>();

        for (String priority : priorityToSearch) {
            priority = priority.trim();
            // Create the By.cssSelector object and add it to the list
            By criteria = By.cssSelector(tagName + "[" + priority + "='" + someXPath + "']");
            criteriaList.add(criteria);
        }

        return criteriaList;
    }

    /* public void screenshot(){
        Function for screenshot, left in case a screen is needed
        outside of the report
    } */

}
