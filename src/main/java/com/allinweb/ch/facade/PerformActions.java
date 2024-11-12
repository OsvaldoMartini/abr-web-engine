package com.allinweb.ch.facade;

import com.allinweb.ch.cryptingAlgorithm.CryptationAlgorithm;
import com.allinweb.ch.driver.ABRWebDriver;
import com.allinweb.ch.dto.BlockLoadDTO;
import com.allinweb.ch.dto.BlockLoopInstructionDTO;
import com.allinweb.ch.dto.BlockLoopInstructionLoadDTO;
import com.allinweb.ch.dto.ComplexInstructionLoadDTO;
import com.allinweb.ch.dto.InstructionReferenceLoadDTO;
import com.allinweb.ch.readersAndWriters.ExcelWriter;
import com.allinweb.ch.util.ABRConstants;
import com.allinweb.ch.util.ABRLogger;
import com.allinweb.ch.util.ABRPriorities;
import com.allinweb.ch.util.ABRPropertyEnum;
import com.allinweb.ch.util.ABRPropertyManager;
import com.allinweb.ch.util.ABRWebUtil;
import com.allinweb.ch.util.Constants;
import com.allinweb.ch.util.ExcelReportStatusEnum;
import com.allinweb.ch.util.PriorityTypeEnum;
import com.allinweb.ch.util.UtilsMethods;
import com.google.common.base.Strings;
import java.awt.*;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javax.swing.*;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * PerformActions.
 *
 * @author Osvaldo Martini
 * @version 1.0
 */
public class PerformActions {
    public List<String> windowHandlesList = new ArrayList<>();

    private ABRPriorities abrPriorities;
    private ABRWebDriver abrWebDriver;
    private boolean periodicActivated;
    private static JavascriptExecutor jsExecutor;
    public static Wait<WebDriver> waitForPage;
    public static Wait<WebDriver> waitForAction;
    private boolean justCalledRefreshPage = false;

    // Static final variable to hold the singleton instance
    protected static final SingletonSupplier<PerformActions> instance = () -> new PerformActions();

    private static final DateTimeFormatter FORMAT_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    // Private constructor to prevent instantiation
    private PerformActions() {
        // Initialize if necessary
    }

    public void initializePerformActions(ABRPriorities abrPriorities, ABRWebDriver abrWebDriver) {
        this.abrPriorities = abrPriorities;
        this.abrWebDriver = abrWebDriver;
    }

    // Public method to access the singleton instance
    public static PerformActions getInstance() {
        return instance.get();
    }

    public String performWebActions(
            Map<String, String> data,
            BlockLoopInstructionLoadDTO instruction,
            int botJobId,
            String blockJobName,
            Map<String, String> mapOperators)
            throws Exception {
        WebElement instructionElement = null;
        String[] actions = instruction.getActions().split(Constants.ACTIONS_AND_PATHS_SPLITTER);

        if (!StringUtils.isBlank(instruction.getPath())) {
            instructionElement = locateElement(instruction, botJobId);
        }
        String result = null;
        if (instructionElement != null
                || actions[0].equals(Constants.HOLD)
                || actions[0].equals(Constants.QUIT)
                || actions[0].equals(Constants.SCREEN)) {

            for (String action : actions) {
                switch (String.valueOf(action.charAt(0))) {
                    case Constants.VISUALIZE:
                        scrollToElement(instructionElement);
                        break;
                    case Constants.OTHER:
                        result = "other Element --> " + instruction.getName() + " --> "
                                + clickElement(instructionElement);
                        break;
                    case Constants.OUTPUT:
                        String fieldName = instruction.getId() + "-" + instruction.getName();
                        result = "outPutElement --> " + instruction.getName() + " --> "
                                + getOutPutElement(
                                        instructionElement, fieldName, instruction.getActions(), mapOperators);
                        break;
                    case Constants.CLICK:
                        result = "clickElement --> " + instruction.getName() + " --> "
                                + clickElement(instructionElement);
                        break;
                    case Constants.INSERT:
                        if ("select".equalsIgnoreCase(instructionElement.getTagName())) {
                            result = "Select: -> "
                                    + insertDataInSelectElement(instructionElement, data, action, instruction);
                        } else {
                            result = insertInElement(instructionElement, data, action, instruction);
                        }
                        break;
                    case Constants.LIST_OPERATION:
                        listOperation(instruction, data);
                        break;
                    case Constants.HOLD:
                        //                        executeAlert(instruction);
                        result = onHoldForSeconds(instruction);
                        break;
                    case Constants.REFRESH:
                        refreshPage();
                        result = "refreshPage";
                        break;
                    case Constants.QUIT:
                        result = "Close Browser";
                        // quit(0);
                        break;
                    case Constants.SCREEN:
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
            BlockLoopInstructionLoadDTO instruction,
            String targetXPath,
            String action,
            String[] operations,
            String parentField,
            Map<String, String> mapOperators)
            throws Exception {

        WebElement instructionElement = null;

        if (!StringUtils.isBlank(targetXPath)) {
            instructionElement = locateTargetElement(targetXPath, instruction.getActionCustomMaxWaitSec());
        }
        if (instructionElement != null) {

            switch (action) {
                case "SET":
                    insertTargetElement(instructionElement, operations[0], operations[1]);
                    return "SET_VALUE to (Parent: " + parentField + ") Var:" + operations[0] + " <-- " + operations[1];
                case "GET":
                    String valueElem;
                    if (mapOperators.containsKey(parentField)) {
                        valueElem = mapOperators.get(parentField);
                    } else {
                        valueElem = getValueInElement(instructionElement);
                        mapOperators.put(parentField, valueElem);
                    }
                    return "GET_VALUE from (Parent: " + parentField + ") Var" + operations[1] + " <-- " + valueElem;
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

    private WebElement locateTargetElement(String targetXPath, Integer actionCustomMaxWaitSec) {

        String tagName = null;
        try {
            tagName = removeTrailingSlash(targetXPath);
            tagName = extractTagName(targetXPath);
        } catch (Exception e) {
            ABRLogger.getInstance(PerformActions.class)
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
                List<WebElement> foundElementList = abrWebDriver.getDriver().findElements(criteria);

                if (foundElementList != null && foundElementList.size() > 0) {
                    if (justCalledRefreshPage) {
                        justCalledRefreshPage = false;
                        try {
                            waitForPage.until(ExpectedConditions.visibilityOfElementLocated(criteria));
                        } catch (Exception e) {
                            ABRLogger.getInstance(PerformActions.class)
                                    .fine(String.format(
                                            "Could Not Find Elements %s   \nCriteria  %s\nCause: %s",
                                            targetXPath, criteria, e.getMessage()));
                        }
                    } else if (actionCustomMaxWaitSec != null) {
                        try {

                            new WebDriverWait(abrWebDriver.getDriver(), Duration.ofSeconds(actionCustomMaxWaitSec))
                                    .until(ExpectedConditions.presenceOfElementLocated(criteria));
                        } catch (Exception e) {
                            ABRLogger.getInstance(PerformActions.class)
                                    .fine(String.format(
                                            "Could Not Find Elements %s   \nCriteria  %s\nCause: %s",
                                            targetXPath, criteria, e.getMessage()));
                        }
                    } else {
                        try {

                            waitForAction.until(ExpectedConditions.visibilityOfElementLocated(criteria));
                        } catch (Exception e) {
                            ABRLogger.getInstance(PerformActions.class)
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

    private WebElement locateElement(BlockLoopInstructionLoadDTO instruction, int botJobId) {

        String instructionPath = instruction.getPath();
        String tagName = null;
        try {
            tagName = removeTrailingSlash(instructionPath);
            tagName = extractTagName(instructionPath);
        } catch (Exception e) {
            ABRLogger.getInstance(PerformActions.class)
                    .fine(String.format(
                            "Error RemoveTrailingSlash for %s   \nxPath  %s\nCause: %s",
                            tagName, instructionPath, e.getMessage()));
        }
        List<InstructionReferenceLoadDTO> instructionReferenceList = instruction.getInstructionReferenceLoadDTOList();

        if (instructionReferenceList.size() == 0) {
            ABRLogger.getInstance(PerformActions.class)
                    .severe("####    Access Database Error   ####"
                            + "\n####    It means there is not XPath to Be Located!   ####"
                            + "\n####    Remove and Re-Scan the Failed Field Again   ####");

            return null;
        }

        waitPage();

        // If Not Loaded get if the JobId Changed
        if (abrPriorities.getJobId() == null) {
            abrPriorities.setJobId(botJobId);
            if (instruction.getPriority() != null) {
                abrPriorities.loadPrioritiesFromString(instruction.getPriority());
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

        if (abrPriorities.getAllPriorityList().size() < 4) {}

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

                    ABRLogger.getInstance(PerformActions.class)
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

                    if (abrWebDriver.getDriver() == null) {
                        showAlert(
                                Alert.AlertType.ERROR,
                                "ABR Web Driver is NULL",
                                "Restart the APP",
                                "Close all Browser or Restart the APP");
                        return null;
                    }

                    ABRLogger.getInstance(PerformActions.class).fine("WebDriver Session ID: " + getSessionId());

                    // Actualy here is Calling the Actions
                    if (criterias != null) {

                        for (By criteria : criterias) {
                            List<WebElement> foundElementList =
                                    abrWebDriver.getDriver().findElements(criteria);

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
                                        ABRLogger.getInstance(PerformActions.class)
                                                .fine(String.format(
                                                        "Could Not Find Elements %s   \nCriteria  %s\nCause: %s",
                                                        instructionPath, criteria, e.getMessage()));
                                    }
                                } else if (instruction.getActionCustomMaxWaitSec() != null) {
                                    try {

                                        new WebDriverWait(
                                                        abrWebDriver.getDriver(),
                                                        Duration.ofSeconds(instruction.getActionCustomMaxWaitSec()))
                                                .until(ExpectedConditions.presenceOfElementLocated(criteria));
                                    } catch (Exception e) {
                                        ABRLogger.getInstance(PerformActions.class)
                                                .fine(String.format(
                                                        "Could Not Find Elements %s   \nCriteria  %s\nCause: %s",
                                                        instructionPath, criteria, e.getMessage()));
                                    }
                                } else {
                                    try {

                                        waitForAction.until(ExpectedConditions.visibilityOfElementLocated(criteria));
                                    } catch (Exception e) {
                                        ABRLogger.getInstance(PerformActions.class)
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
                                        String xpath = ABRWebUtil.extractXPath(
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

    public static String removeTrailingSlash(String xPath) {
        if (xPath != null && xPath.endsWith("/")) {
            return xPath.substring(0, xPath.length() - 1);
        }
        return xPath;
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

    private String getValueInElement(WebElement element) throws Exception {
        UtilsMethods.exceptionIfNullWebElement(element);
        waitForAction.until(ExpectedConditions.visibilityOf(element));

        // Assuming instructionElement is an input field
        return element.getAttribute("value");
    }

    public synchronized String onHoldForSeconds(BlockLoopInstructionLoadDTO instruction) throws Exception {
        if (instruction != null) {
            Integer instructionSeconds = instruction.getOnHoldSeconds();
            if (instructionSeconds != null && instructionSeconds > 0) {
                wait(fromSecondsToMilliseconds(TimeUnit.SECONDS, instructionSeconds));
                return "HOLD" + "->" + instructionSeconds + " seconds";
            } else {
                String stopSeconds =
                        ABRPropertyManager.getInstance().getProperty(ABRPropertyEnum.DEFAULT_INSTRUCTION_STOP_SECONDS);
                wait(fromSecondsToMilliseconds(TimeUnit.SECONDS, Integer.parseInt(stopSeconds)));
                return "HOLD" + "->" + stopSeconds + " seconds";
            }
        } else {
            wait(400);
            return "HOLD" + "->" + "400 milliseconds";
        }
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

    private void waitPage() {
        WebDriver driver = abrWebDriver.getDriver();
        if (driver != null) {
            try {

                waitForPage.until(d -> ((JavascriptExecutor) driver)
                        .executeScript("return document.readyState")
                        .equals("complete"));
            } catch (Exception ex) {
                ABRLogger.getInstance(PerformActions.class)
                        .warning(String.format(
                                "WaitForPage.until(d -> ((JavascriptExecutor) driver) error:\n%s", ex.getMessage()));
            }
        } else {
            // Handle the case when driver is null (e.g., throw an exception or initialize the driver)
            ABRLogger.getInstance(PerformActions.class)
                    .warning("WaitForPage.until(d -> ((JavascriptExecutor) driver) is returning nulls");
        }
    }

    public void scrollToElement(WebElement element) throws Exception {
        UtilsMethods.exceptionIfNullWebElement(element);
        ((JavascriptExecutor) abrWebDriver.getDriver()).executeScript("arguments[0].scrollIntoView(true);", element);
    }

    public String clickElement(WebElement element) throws Exception {
        UtilsMethods.exceptionIfNullWebElement(element);
        if (!element.isEnabled()) {
            // throw new TimeoutException();
        }
        waitForAction.until(ExpectedConditions.visibilityOf(element).andThen(e -> {
            ((JavascriptExecutor) abrWebDriver.getDriver())
                    .executeScript("arguments[0].scrollIntoView(true);", element);
            return waitForAction.until(ExpectedConditions.elementToBeClickable(element));
        }));
        try {
            element.click();
            try {
                return ABRWebUtil.extractXPath(element.toString());
            } catch (Exception e) {
                return "Extract XPath Problem: ON ABRWebUtil.extractXPath for " + element.getTagName();
            }
        } catch (ElementClickInterceptedException e) {
            JavascriptExecutor jse = (JavascriptExecutor) abrWebDriver.getDriver();
            jse.executeScript("arguments[0].click()", element);
            return "ElementClickIntercepted Exception: " + ABRWebUtil.extractXPath(element.toString());
        }
    }

    public void refreshPage() {
        abrWebDriver.getDriver().navigate().refresh();
        justCalledRefreshPage = true;
    }

    private String insertInElement(
            WebElement element,
            Map<String, String> data,
            String singleInstruction,
            BlockLoopInstructionLoadDTO instructionDTO)
            throws Exception {
        UtilsMethods.exceptionIfNullWebElement(element);
        waitForAction.until(ExpectedConditions.visibilityOf(element));
        String dataFieldName = "";
        String dataFieldValue = "";
        if (data != null) {
            String[] arr = UtilsMethods.splitIfContains(singleInstruction, Constants.ACTION_SPECIFICATIONS_SPLITTER);
            if (arr.length > 1) {
                dataFieldName = arr[1].split(Constants.PATH_FIELD_SUBSTITUTION)[0];

                dataFieldValue = data.get(dataFieldName);
                if (instructionDTO.isEncrypted()) {
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
            if (instructionDTO.isEncrypted()) {
                dataFieldValue = CryptationAlgorithm.decrypt(dataFieldValue);
            }
            element.sendKeys(dataFieldValue);
        }

        return dataFieldName + "->" + dataFieldValue;
    }

    private String insertDataInSelectElement(
            WebElement element,
            Map<String, String> data,
            String singleInstruction,
            BlockLoopInstructionLoadDTO instructionDTO)
            throws Exception {
        UtilsMethods.exceptionIfNullWebElement(element);
        waitForAction.until(ExpectedConditions.visibilityOf(element));
        String dataFieldName = "";
        String dataFieldValue = "";
        if (data != null) {
            String[] arr = UtilsMethods.splitIfContains(singleInstruction, Constants.ACTION_SPECIFICATIONS_SPLITTER);
            if (arr.length > 1) {
                dataFieldName = arr[1].split(Constants.PATH_FIELD_SUBSTITUTION)[0];

                dataFieldValue = data.get(dataFieldName);
                if (instructionDTO.isEncrypted()) {
                    dataFieldValue = CryptationAlgorithm.decrypt(dataFieldValue);
                }

                try {
                    // Create a Select instance to interact with the dropdown
                    Select selectCountry = new Select(element);
                    // Select "Switzerland" by visible text
                    selectCountry.selectByVisibleText(dataFieldValue);

                } catch (Exception ex) {
                    return "Error: -> Cannot find: " + dataFieldName + "-> \"" + dataFieldValue
                            + "\" - Attention to Case-Sentitives!";
                }
            }
        } else if (instructionDTO.getDefaultValue() != null) {
            dataFieldValue = instructionDTO.getDefaultValue();
            if (instructionDTO.isEncrypted()) {
                dataFieldValue = CryptationAlgorithm.decrypt(dataFieldValue);
            }
            element.sendKeys(dataFieldValue);
        }

        return "Select: -> " + dataFieldName + "->" + dataFieldValue;
    }

    private String getOutPutElement(
            WebElement element, String fieldName, String action, Map<String, String> mapOperators) throws Exception {

        UtilsMethods.exceptionIfNullWebElement(element);
        waitForAction.until(ExpectedConditions.visibilityOf(element));

        String textByhJS = "";
        String finalTextNested = "";
        String textAttribute = "";
        String textContext = "";

        try {
            JavascriptExecutor js = (JavascriptExecutor) abrWebDriver.getDriver();
            textByhJS = (String) js.executeScript("return arguments[0].textContent;", element);
        } catch (Exception ex) {
            ABRLogger.getInstance(PerformActions.class)
                    .warning(String.format(
                            "By JavascriptExecutor - Not succeeded to get a Text from Label for: %s", fieldName));
        }

        try {
            List<WebElement> children = element.findElements(By.xpath(".//*"));
            StringBuilder textByNested = new StringBuilder();
            for (WebElement child : children) {
                textByNested.append(child.getText()).append(" ");
            }
            finalTextNested = textByNested.toString().trim();
        } catch (Exception ex) {
            ABRLogger.getInstance(PerformActions.class)
                    .warning(String.format(
                            "By Text Nested - Not succeeded to get a Text from Label for: %s", fieldName));
        }

        try {
            textAttribute = element.getAttribute("value");
        } catch (Exception ex) {
            ABRLogger.getInstance(PerformActions.class)
                    .warning(String.format(
                            "By Text Attribute - Not succeeded to get a Text from Label for: %s Operation: %s",
                            fieldName, action));
        }

        try {
            textContext = element.getAttribute("textContent");
        } catch (Exception ex) {
            ABRLogger.getInstance(PerformActions.class)
                    .warning(String.format(
                            "By Text Content - Not succeeded to get a Text from Label for: %s Operation: %s",
                            fieldName, action));
        }

        // Check if the element is clickable
        boolean isClickable = false;
        try {
            waitForAction.until(ExpectedConditions.elementToBeClickable(element));
            isClickable = true;
        } catch (Exception ex) {
            ABRLogger.getInstance(PerformActions.class)
                    .warning(String.format("Element is not clickable: %s", fieldName));
        }

        // Set the final text value by priority and add to mapOperators
        String finalText = "";

        if (isClickable && finalTextNested != null && !finalTextNested.trim().isEmpty()) {
            finalText = finalTextNested; // Use nested text if the element is clickable
            mapOperators.put(fieldName, finalText);
        } else if (textByhJS != null && !textByhJS.trim().isEmpty()) {
            finalText = textByhJS;
            mapOperators.put(fieldName, finalText);
        } else if (finalTextNested != null && !finalTextNested.trim().isEmpty()) {
            finalText = finalTextNested;
            mapOperators.put(fieldName, finalText);
        } else if (textAttribute != null && !textAttribute.trim().isEmpty()) {
            finalText = textAttribute;
            mapOperators.put(fieldName, finalText);
        } else if (textContext != null && !textContext.trim().isEmpty()) {
            finalText = textContext;
            mapOperators.put(fieldName, finalText);
        } else {
            mapOperators.put(fieldName, "Failed");
            ABRLogger.getInstance(PerformActions.class)
                    .severe(String.format("Failed to retrieve text from element for: %s", fieldName));
        }

        return Strings.isNullOrEmpty(finalText) ? "$EMPTY" : finalText;
    }

    private void listOperation(BlockLoopInstructionLoadDTO instructionDTO, Map<String, String> data) {

        /*
        TODO: Da rivedere, attualmente non del tutto funzionante
        Complex instruction string interpretation:
        [       0       ||       1      ||       2         ||    3    ||        4       ||  5   ||            6                ]
        [backward_button||forward_button||list_elements_tag||condition||expected_results||action||sub_element_on_execute_action]
        */
        List<ComplexInstructionLoadDTO> complexInstructionDTOS = instructionDTO.getComplexInstructionLoadDTOList();
        String[] complexActionParts =
                complexInstructionDTOS.get(0).getInstruction().split(Constants.COMPLEX_INSTRUCTION_SEPARATOR);
        List<WebElement> webElementList;
        WebElement forwardButton;
        WebElement backwardButton;
        boolean shouldContinue = true;

        boolean existNextPage;
        do {
            waitForPage.until(ExpectedConditions.visibilityOfElementLocated(By.tagName(complexActionParts[2])));
            backwardButton = abrWebDriver.getDriver().findElement(By.xpath(complexActionParts[0]));
            forwardButton = abrWebDriver.getDriver().findElement(By.xpath(complexActionParts[1]));
            webElementList = abrWebDriver.getDriver().findElements(By.tagName(complexActionParts[2]));

            WebElement element;
            WebElement reasonWebElement;
            for (int i = 0; i < 5; i++) {

                if (i != 0) {
                    waitForPage.until(ExpectedConditions.visibilityOfElementLocated(By.tagName(complexActionParts[2])));
                    webElementList = abrWebDriver.getDriver().findElements(By.tagName(complexActionParts[2]));
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
                            abrWebDriver
                                    .getDriver()
                                    .findElement(
                                            By.xpath(
                                                    ".//button[@test-id='web-banking-payment-core.payment-ctx-action.payment-action-VIEW']")));

                    Thread.sleep(1000);
                    clickElement(abrWebDriver
                            .getDriver()
                            .findElement(By.xpath(
                                    ".//button[@test-id='web-banking-common.export-to-file.single-file-button']")));

                    Thread.sleep(1000);
                    clickElement(
                            abrWebDriver
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

    public void quit(int status) {
        abrWebDriver.getDriver().quit();
        if (status == 0) {
            System.exit(status);
        }
    }

    public short operationLog(
            boolean success, String mainMsg, String resultActions, String lastInstructionExecuted, long duration) {

        if (success) {

            ABRLogger.getInstance(PerformActions.class)
                    .info(String.format(
                            success
                                    ? "SUCCESS %s Previous: %s --> Current Cmd: %s - Duration: %s"
                                    : "FAILED %s Previous: %s --> Current Cmd: %s - Duration: %s",
                            mainMsg,
                            resultActions,
                            lastInstructionExecuted,
                            LocalTime.ofNanoOfDay(duration).format(FORMAT_TIME)));
        } else {

            ABRLogger.getInstance(PerformActions.class)
                    .severe(String.format(
                            success
                                    ? "SUCCESS %s Previous: %s --> Current Cmd: %s - Duration: %s"
                                    : "FAILED %s Previous: %s --> Current Cmd: %s - Duration: %s",
                            mainMsg,
                            resultActions,
                            lastInstructionExecuted,
                            LocalTime.ofNanoOfDay(duration).format(FORMAT_TIME)));
        }

        return (short) (success ? ExcelReportStatusEnum.SUCCESS.ordinal() : ExcelReportStatusEnum.ERROR.ordinal());
    }

    public String pauseEngine(String blockName) {

        //        JavascriptExecutor js = (JavascriptExecutor) abrWebDriver.getDriver();
        //        js.executeScript("alert('This is a custom alert modal!');");
        String message = "PAUSE REQUESTED "
                + "<br>----------------------------------------------<br>"
                + "BOT JOB in PAUSE MODE:: <b style='color:red;'><br>"
                + blockName
                + "</b>"
                + "<br>----------------------------------------------<br>";

        alertMessage(message);

        return "BOT JOG in PAUSE MODE: " + blockName;
    }

    public String getValueIsNotDefinedEngine(
            BlockLoopInstructionLoadDTO currentInstruction,
            String lastInstructionExecuted,
            boolean ifClause,
            boolean elseClause) {

        if (!ifClause && !elseClause) {
            String message = "There is NOT GET VALUE defined for: "
                    + "<br>----------------------------------------------<br>"
                    + "Validation Error: <b style='color:red;'>"
                    + currentInstruction.getName()
                    + "</b>"
                    + "<br>----------------------------------------------<br>"
                    + "Check the GET for <b style='color:red;'>"
                    + currentInstruction.getParentId() + "-"
                    + currentInstruction.getOperation()
                    + "</b>";
            alertMessage(message);
        }

        String conditionalBlock = ifClause
                ? "Closing Block { IF -> ELSE }  -> "
                : elseClause ? "Closing Block { ELSE -> ENDIF }  -> " : "";

        if (ifClause || elseClause) {
            return conditionalBlock + "Failed to Execute Cmd: " + lastInstructionExecuted;

        } else {
            return "Failed to Execute Cmd: " + lastInstructionExecuted;
        }
    }

    public String getValueIsNotDefined(
            BlockLoopInstructionLoadDTO currentInstruction,
            String lastInstructionExecuted,
            boolean ifClause,
            boolean elseClause) {

        if (!ifClause && !elseClause) {
            showAlert(
                    Alert.AlertType.ERROR,
                    "GET is Not Defined for \"+" + currentInstruction.getName() + "\"",
                    "\"" + currentInstruction.getName() + "\" - GET is Not Defined",
                    "There is NOT GET VALUE defined for: "
                            + currentInstruction.getName()
                            + "\n --------------------- "
                            + "\nCheck the GET for "
                            + currentInstruction.getParentId() + "-"
                            + currentInstruction.getOperation());
        }

        String conditionalBlock = ifClause
                ? "Closing Block { IF -> ELSE }  -> "
                : elseClause ? "Closing Block { ELSE -> ENDIF }  -> " : "";

        if (ifClause || elseClause) {
            return conditionalBlock + "Failed to Execute Cmd: " + lastInstructionExecuted;

        } else {
            return "Failed to Execute Cmd: " + lastInstructionExecuted;
        }
    }

    public String parentIdWrongBlockEngine(
            BlockLoopInstructionLoadDTO currentInstruction,
            BlockLoadDTO blockLoad,
            boolean ifClause,
            boolean elseClause) {
        if (!ifClause && !elseClause) {
            String message = "The Parent Id: <b style='color:red;'>"
                    + "The Parent Id: \"(" + currentInstruction.getParentId() + ")"
                    + currentInstruction
                            .getOperation()
                            .substring(0, currentInstruction.getOperation().indexOf(":")) + "\""
                    + "<br>----------------------------------------------<br>"
                    + "<b style='color:red;'>" + "Does not belong to this block: \"" + blockLoad.getBlockOrderNumber()
                    + "-\"" + blockLoad.getName() + "\"" + "</b>"
                    + "</br>"
                    + "<b style='color:red;'>"
                    + "Attempted Operation : \"" + currentInstruction.getActions() + "\" -> \""
                    + currentInstruction.getOperation() + "\"" + "</b>"
                    + "<br>----------------------------------------------<br>"
                    + "<b style='color:blue;'>"
                    + "Check the Web Field \" ( ID ) <NAME>\" per Block</b>";

            alertMessage(message);
        }

        String conditionalBlock = ifClause
                ? "Closing Block { IF -> ELSE }  -> "
                : elseClause ? "Closing Block { ELSE -> ENDIF }  -> " : "";

        if (ifClause || elseClause) {
            ABRLogger.getInstance(PerformActions.class)
                    .warning(String.format(
                            "%sParent Id Error Check Parent Id: %d "
                                    + "For the \"%s\" Does not belong to this block: "
                                    + blockLoad.getId() + "-" + blockLoad.getName(),
                            conditionalBlock,
                            currentInstruction.getParentId(),
                            currentInstruction.getOperation()));

        } else {
            ABRLogger.getInstance(PerformActions.class)
                    .severe(String.format(
                            "Parent Id Error Check Parent Id: %d "
                                    + "For the \"%s\" Does not belong to this block: "
                                    + blockLoad.getId() + "-" + blockLoad.getName(),
                            currentInstruction.getParentId(),
                            currentInstruction.getOperation()));
        }

        return String.format(
                "This ParentId: %d does not belong to this block: %d - %s. Check the Field Names and Fields Ids",
                currentInstruction.getParentId(), blockLoad.getId(), blockLoad.getName());
    }

    public String parentIdWrongBlock(
            BlockLoopInstructionLoadDTO currentInstruction,
            BlockLoadDTO blockLoad,
            boolean ifClause,
            boolean elseClause) {
        if (!ifClause && !elseClause) {
            showAlert(
                    Alert.AlertType.ERROR,
                    "Parent Id Error",
                    "Check Parent Id",
                    "The Parent Id: \"(" + currentInstruction.getParentId() + ")"
                            + currentInstruction
                                    .getOperation()
                                    .substring(
                                            0, currentInstruction.getOperation().indexOf(":"))
                            + "\""
                            + "\nDoes not belong to the block: \"" + blockLoad.getBlockOrderNumber() + "-"
                            + blockLoad.getName() + "\""
                            + "\nAttempted Operation : \"" + currentInstruction.getActions() + "\" -> \""
                            + currentInstruction.getOperation() + "\""
                            + "\nCheck the Web Field \" ( ID ) <NAME> \" per Block");
        }

        String conditionalBlock = ifClause
                ? "Closing Block { IF -> ELSE }  -> "
                : elseClause ? "Closing Block { ELSE -> ENDIF }  -> " : "";

        if (ifClause || elseClause) {
            ABRLogger.getInstance(PerformActions.class)
                    .warning(String.format(
                            "%sParent Id Error Check Parent Id: %d "
                                    + "For the \"%s\" Does not belong to this block: "
                                    + blockLoad.getId() + "-" + blockLoad.getName(),
                            conditionalBlock,
                            currentInstruction.getParentId(),
                            currentInstruction.getOperation()));

        } else {
            ABRLogger.getInstance(PerformActions.class)
                    .severe(String.format(
                            "Parent Id Error Check Parent Id: %d "
                                    + "For the \"%s\" Does not belong to this block: "
                                    + blockLoad.getId() + "-" + blockLoad.getName(),
                            currentInstruction.getParentId(),
                            currentInstruction.getOperation()));
        }

        return String.format(
                "This ParentId: %d does not belong to this block: %d - %s. Check the Field Names and Fields Ids",
                currentInstruction.getParentId(), blockLoad.getId(), blockLoad.getName());
    }

    public String checkValidationFailedEngine(
            String parent,
            String expected,
            String lastInstructionExecuted,
            String[] operations,
            boolean ifClause,
            boolean elseClause) {
        if (!ifClause && !elseClause) {
            String message = "The Value of: <b style='color:red;'>\"" + operations[2] + "\""
                    + "</b> is not " + "<b>" + operations[1] + " "
                    + " \"" + expected + "\"" + "</b> Length: (<b>" + expected.length() + "</b>)"
                    + "<br>----------------------------------------------<br>"
                    + "The Variable \"" + operations[0] + "\" holds value \"" + operations[2] + "\"</br>"
                    + "<br>Current Web Field: <b style='color:red;'> \"" + parent + "\" value: \"" + expected
                    + "\"</b> Length: (<b>\"" + expected.length() + ")</b>"
                    + "<br>Expected value: <b style='color:green;'>" + operations[2] + "</b> Length: (<b>"
                    + operations[2].length() + "</b>)";

            alertMessage(message);
        }

        String conditionalBlock = ifClause
                ? "Closing Block { IF -> ELSE }  -> "
                : elseClause ? "Closing Block { ELSE -> ENDIF }  -> " : "";

        if (ifClause || elseClause) {
            return conditionalBlock + "Failed to Execute Cmd: " + lastInstructionExecuted;

        } else {
            return "Failed to Execute Cmd: " + lastInstructionExecuted;
        }
    }

    public String checkValidationFailed(
            String parent,
            String expected,
            String lastInstructionExecuted,
            String[] operations,
            boolean ifClause,
            boolean elseClause) {
        if (!ifClause && !elseClause) {
            showAlert(
                    Alert.AlertType.ERROR,
                    "Validation Error",
                    "Check Validation Error",
                    "The Value of: \"" + operations[2] + "\" is not " + operations[1] + " \""
                            + expected + "\" Length: ("
                            + expected.length()
                            + ")" + "\n --------------------- "
                            + "\nThe Variable \""
                            + operations[0] + "\" holds value \"" + operations[2] + "\""
                            + "\nCurrent Web Field \"" + parent + "\" value: \""
                            + expected + "\" Length: (" + expected.length()
                            + ")" + "\nExpected value: "
                            + operations[2]
                            + " Length: ("
                            + operations[2].length()
                            + ")");
        }

        String conditionalBlock = ifClause
                ? "Closing Block { IF -> ELSE }  -> "
                : elseClause ? "Closing Block { ELSE -> ENDIF }  -> " : "";

        if (ifClause || elseClause) {
            return conditionalBlock + "Failed to Execute Cmd: " + lastInstructionExecuted;

        } else {
            return "Failed to Execute Cmd: " + lastInstructionExecuted;
        }
    }

    public void showAlert(Alert.AlertType alertType, String title, String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    public void showAlertCombinedHBox(
            Alert.AlertType alertType, String title, String header, String content, HBox combinedTextContainer) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.getDialogPane().setContent(combinedTextContainer);

            alert.showAndWait();
        });
    }

    public void showAlertDialog(Alert.AlertType alertType, String title, String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    public void excelReportWrite(
            boolean success,
            BlockLoopInstructionLoadDTO currentInstruction,
            long duration,
            Map<String, String> dataExcel,
            ExcelWriter.ExcelChain writerReport) {
        writerReport.insertInstructionResult(
                currentInstruction, dataExcel, LocalTime.ofNanoOfDay(duration), success ? "success" : "failed");
    }

    public long duration(long startTime) {
        long currentInstructionEndTime = System.nanoTime();
        return currentInstructionEndTime - startTime;
    }

    public String blockGotoFailed(String resultActions) {
        showAlert(
                Alert.AlertType.ERROR, "Block GO TO Error", "Check Correct Block Existence", "CMD: \n" + resultActions);

        ABRLogger.getInstance(PerformActions.class)
                .severe("Block GO TO Error.\n" + "Check Correct Block Existence!\n" + "CMD: \n" + resultActions);

        return resultActions;
    }

    public void alertExecutionTimes(int executionTimes, String lastInstructionExecuted) {
        showAlert(
                Alert.AlertType.ERROR,
                "Block Execution Time LIMIT",
                "Attention The Process Reached the LIMIT of Block Loop Executions",
                String.format(
                        "Attention the Process Reached the Block LOOP LIMIT of %d\nLast Instruction Executed : %s\nWe are Exiting All of processes Now!",
                        executionTimes, lastInstructionExecuted));
    }

    // Update the list of window handles (tabs)
    public void updateWindowHandlesList() {
        Set<String> windowHandles = abrWebDriver.getDriver().getWindowHandles();
        windowHandlesList = new ArrayList<>(windowHandles);
    }

    public String getSessionId() {
        if (abrWebDriver.getDriver() instanceof RemoteWebDriver) {
            return ((RemoteWebDriver) abrWebDriver.getDriver()).getSessionId().toString();
        } else {
            throw new IllegalStateException("Driver is not an instance of RemoteWebDriver");
        }
    }

    //    // Creating SAVED BLOCKS FORM BLOCKS DTO
    //    public static SavedBlocksDTO createSavedBlocksDTOFromBlocksDTO(BlockDTO blockDTO) {
    //        SavedBlocksDTO savedBlocksDTO = new SavedBlocksDTO();
    //        savedBlocksDTO.setName(blockDTO.getName());
    //        savedBlocksDTO.setDescription(blockDTO.getDescription());
    //        savedBlocksDTO.setTypeId(blockDTO.getTypeId());
    //
    //        return savedBlocksDTO;
    //    }

    // Creating COMPONENT SAVED INSTRUCTIONS FOR BLOCK INSTRUCTIONS
    //    public static List<SavedBlockLoopInstructionDTO> createSavedBlockLoopInstructionsFromBlocksDTO(
    //            BlockDTO blockDTO, SavedBlocksDTO savedBlocksDTO) {
    //        SavedBlockLoopInstructionDTO savedBlockLoopInstructionDTO;
    //        List<SavedBlockLoopInstructionDTO> savedBlockLoopInstructionDTOs = new ArrayList<>();
    //
    //        List<BlockLoopInstructionDTO> instructionList = ABRSharedResources.getInstance()
    //                .getEntityList(
    //                        BlockLoopInstructionDTO.class,
    //                        instruction -> instruction.getBlock().getId() == blockDTO.getId());
    //
    //        List<BlockLoopInstructionDTO> instructionFiltered = filterInstructions(instructionList);
    //
    //        for (BlockLoopInstructionDTO blockLoopInstructionDTO : instructionFiltered) {
    //            savedBlockLoopInstructionDTO = new SavedBlockLoopInstructionDTO();
    //
    //
    // savedBlockLoopInstructionDTO.setActionCustomMaxWaitSec(blockLoopInstructionDTO.getActionCustomMaxWaitSec());
    //            savedBlockLoopInstructionDTO.setActions(blockLoopInstructionDTO.getActions());
    //            savedBlockLoopInstructionDTO.setBlock(savedBlocksDTO);
    //
    //            savedBlockLoopInstructionDTO.setDefaultValue(blockLoopInstructionDTO.getDefaultValue());
    //            savedBlockLoopInstructionDTO.setDescription(blockLoopInstructionDTO.getDescription());
    //            savedBlockLoopInstructionDTO.setEncrypted(blockLoopInstructionDTO.isEncrypted());
    //            savedBlockLoopInstructionDTO.setExportToABR(blockLoopInstructionDTO.getExportToABR());
    //
    // savedBlockLoopInstructionDTO.setInstructionOrderNumber(blockLoopInstructionDTO.getInstructionOrderNumber());
    //            savedBlockLoopInstructionDTO.setName(blockLoopInstructionDTO.getName());
    //            savedBlockLoopInstructionDTO.setOnHoldSeconds(blockLoopInstructionDTO.getOnHoldSeconds());
    //            savedBlockLoopInstructionDTO.setOptional(blockLoopInstructionDTO.isOptional());
    //            savedBlockLoopInstructionDTO.setPath(blockLoopInstructionDTO.getPath());
    //
    //            List<SavedInstructionReferenceDTO> referenceDTOList = new ArrayList<>(
    //                    SavedInstructionReferenceDTO.createSavedReferencesFromInstructionForSavedInstruction(
    //                            blockLoopInstructionDTO, savedBlockLoopInstructionDTO));
    //            savedBlockLoopInstructionDTO.setSavedInstructionReferenceDTOList(referenceDTOList);
    //
    //            savedBlockLoopInstructionDTOs.add(savedBlockLoopInstructionDTO);
    //        }
    //
    //        return savedBlockLoopInstructionDTOs;
    //    }

    //    // Creating BLOCKS DTO FROM SAVED BLOCKS
    //    public static BlockDTO createBlocksDTOFromSavedBlocksDTO(SavedBlocksDTO savedBlocksDTO, BotJobDTO botJobDTO) {
    //        BlockDTO blocksDTO = new BlockDTO();
    //        blocksDTO.setName(savedBlocksDTO.getName());
    //        blocksDTO.setBotJob(botJobDTO);
    //        blocksDTO.setDescription(savedBlocksDTO.getDescription());
    //        blocksDTO.setTypeId(savedBlocksDTO.getTypeId());
    //        return blocksDTO;
    //    }
    //
    //    // Creating BLOCK INSTRUCTIONS FROM COMPONENT SAVED INSTRUCTIONS
    //    public static List<BlockLoopInstructionDTO> createBlockLoopInstructionsFromSavedBlocksDTO(
    //            SavedBlocksDTO savedBlocksDTO, BlockDTO blockDTO) {
    //        List<BlockLoopInstructionDTO> blockLoopInstructionDTOs = new ArrayList<>();
    //
    //        BlockLoopInstructionDTO blockLoopInstructionDTO;
    //
    //        List<SavedBlockLoopInstructionDTO> savedInstructions = ABRSharedResources.getInstance()
    //                .getEntityList(
    //                        SavedBlockLoopInstructionDTO.class,
    //                        saved -> saved.getBlock().getId() == savedBlocksDTO.getId());
    //
    //        for (SavedBlockLoopInstructionDTO savedBlockLoopInstructionDTO : savedInstructions) {
    //            blockLoopInstructionDTO = new BlockLoopInstructionDTO();
    //
    //
    // blockLoopInstructionDTO.setActionCustomMaxWaitSec(savedBlockLoopInstructionDTO.getActionCustomMaxWaitSec());
    //            blockLoopInstructionDTO.setActions(savedBlockLoopInstructionDTO.getActions());
    //
    //            blockLoopInstructionDTO.setBlock(blockDTO);
    //            blockLoopInstructionDTO.setDefaultValue(savedBlockLoopInstructionDTO.getDefaultValue());
    //            blockLoopInstructionDTO.setDescription(savedBlockLoopInstructionDTO.getDescription());
    //            blockLoopInstructionDTO.setEncrypted(savedBlockLoopInstructionDTO.isEncrypted());
    //            blockLoopInstructionDTO.setExportToABR(savedBlockLoopInstructionDTO.getExportToABR());
    //
    // blockLoopInstructionDTO.setInstructionOrderNumber(savedBlockLoopInstructionDTO.getInstructionOrderNumber());
    //            blockLoopInstructionDTO.setName(savedBlockLoopInstructionDTO.getName());
    //            blockLoopInstructionDTO.setOnHoldSeconds(savedBlockLoopInstructionDTO.getOnHoldSeconds());
    //            blockLoopInstructionDTO.setOptional(savedBlockLoopInstructionDTO.isOptional());
    //            blockLoopInstructionDTO.setPath(savedBlockLoopInstructionDTO.getPath());
    //
    //            List<InstructionReferenceDTO> referenceDTOList =
    //                    new ArrayList<>(InstructionReferenceDTO.createReferencesFromSavedInstructionForInstruction(
    //                            savedBlockLoopInstructionDTO, blockLoopInstructionDTO));
    //            blockLoopInstructionDTO.setInstructionReferenceDTOList(referenceDTOList);
    //
    //            blockLoopInstructionDTOs.add(blockLoopInstructionDTO);
    //        }
    //
    //        return blockLoopInstructionDTOs;
    //    }

    //    private int createSavedBlock(BlockDTO blockDTO) {
    //        // Generate a Unique-ID for the block
    //        Integer nextId = loadNextIdSavedBlockData() + 1;
    //        Integer nextBlockOrder =
    //                loadNextSavedBlockOrderNumber(blockDTO.getBotJobDTO().getId()) + 1;
    //
    //        // Build the SQL insert query
    //        String insertSQL =
    //                "INSERT INTO saved_blocks(id, block_order_number, description, name, type_id, bot_job_id) VALUES
    // ("
    //                        + nextId + ", "
    //                        + nextBlockOrder + ", " // block_order_number
    //                        + "'" + blockDTO.getDescription() + "', " // description
    //                        + "'" + blockDTO.getName() + "', " // name
    //                        + 1 + ", " // type_id
    //                        + blockDTO.getBotJobDTO().getId() + ")"; // bot_job_id, assuming BotJobDTO has an ID
    //
    //        try (Statement stmt = ABRSharedResources.getInstance().getConnection().createStatement()) {
    //            stmt.executeUpdate(insertSQL);
    //            ABRLogger.getInstance(ABRViewBotJobPane.class).info("Block data saved successfully id: " + nextId);
    //            return nextId;
    //        } catch (SQLException e) {
    //            ABRLogger.getInstance(ABRViewBotJobPane.class).severe("saveBlock  \nError: " + e.getMessage());
    //            return -1;
    //        }
    //    }
    //
    //    private Integer loadNextSavedBlockOrderNumber(int botJobId) {
    //        //        String selectSQL = "SELECT NEXT_VAL fROM homeBankingSeq";
    //        String selectSQL = "SELECT MAX(ID) AS max_id FROM saved_blocks where bot_job_id = " + botJobId;
    //        try (Statement stmt = ABRSharedResources.getInstance().getConnection().createStatement();
    //             ResultSet rs = stmt.executeQuery(selectSQL)) {
    //            while (rs.next()) {
    //                return rs.getInt("max_id");
    //            }
    //        } catch (SQLException e) {
    //            ABRLogger.getInstance(ABRViewBotJobPane.class).severe("loadNextIdBlockData  \nError: " +
    // e.getMessage());
    //        }
    //        return null;
    //    }
    //
    //    private Integer loadNextIdSavedInstructionData() {
    //        //        String selectSQL = "SELECT NEXT_VAL fROM homeBankingSeq";
    //        String selectSQL = "SELECT MAX(ID) AS max_id FROM saved_block_loop_instruction";
    //        try (Statement stmt = ABRSharedResources.getInstance().getConnection().createStatement();
    //             ResultSet rs = stmt.executeQuery(selectSQL)) {
    //            while (rs.next()) {
    //                return rs.getInt("max_id");
    //            }
    //        } catch (SQLException e) {
    //            ABRLogger.getInstance(ABRViewBotJobPane.class)
    //                    .severe("loadNextIdBReferenceData  \nError: " + e.getMessage());
    //        }
    //        return null;
    //    }
    //
    //    private Integer loadNextIdSavedBlockData() {
    //        //        String selectSQL = "SELECT NEXT_VAL fROM homeBankingSeq";
    //        String selectSQL = "SELECT MAX(ID) AS max_id FROM saved_blocks";
    //        try (Statement stmt = ABRSharedResources.getInstance().getConnection().createStatement();
    //             ResultSet rs = stmt.executeQuery(selectSQL)) {
    //            while (rs.next()) {
    //                return rs.getInt("max_id");
    //            }
    //        } catch (SQLException e) {
    //            ABRLogger.getInstance(ABRViewBotJobPane.class).severe("loadNextIdBlockData  \nError: " +
    // e.getMessage());
    //        }
    //        return null;
    //    }

    public static List<BlockLoopInstructionDTO> filterInstructions(List<BlockLoopInstructionDTO> instructionList) {
        return instructionList.stream()
                .filter(instruction -> !ABRConstants.EXTRACT_FIELD.equals(instruction.getActions())
                        && !ABRConstants.SET_VALUE.equals(instruction.getActions())
                        && !ABRConstants.GET_VALUE.equals(instruction.getActions())
                        && !ABRConstants.CHECK_VALUE.equals(instruction.getActions())
                        && !ABRConstants.GOTO.equals(instruction.getActions())
                        && !ABRConstants.IF.equals(instruction.getActions())
                        && !ABRConstants.ELSE.equals(instruction.getActions())
                        && !ABRConstants.ENDIF.equals(instruction.getActions()))
                .collect(Collectors.toList());
    }

    public boolean showCombinedConfirmation(String title, String header, String content, HBox combinedTextContainer) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.getDialogPane().setContent(combinedTextContainer);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    public boolean showAlertCombinedVBOX(
            Alert.AlertType alertType, String title, String header, String content, VBox combinedTextContainer) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.getDialogPane().setContent(combinedTextContainer);

        if (alertType.equals(Alert.AlertType.CONFIRMATION)) {
            alert.getButtonTypes().set(0, ButtonType.YES);
            alert.getButtonTypes().set(1, ButtonType.NO);
        }
        Optional<ButtonType> result = alert.showAndWait();

        if (alertType.equals(Alert.AlertType.CONFIRMATION)) {
            return result.isPresent() && result.get() == ButtonType.YES;
        } else {
            return result.isPresent() && result.get() == ButtonType.OK;
        }
    }

    public void alertMessage(String message) {
        JavascriptExecutor js = (JavascriptExecutor) abrWebDriver.getDriver();

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

        js.executeScript(script);

        // Optional: Handle the alert
        org.openqa.selenium.Alert alert = abrWebDriver.getDriver().switchTo().alert();

        // Optional: pause for a few seconds to view the alert
        try {
            Thread.sleep(5000); // 10 minutes in milliseconds
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Accept (close) the alert
        alert.accept();
    }

    public static void showCustomDialog(String title, String message) {
        // Create a JDialog as a custom message dialog
        JDialog dialog = new JDialog();
        dialog.setTitle(title);
        dialog.setSize(300, 150);
        dialog.setLocationRelativeTo(null); // Center on screen
        dialog.setUndecorated(true); // Remove the default border

        // Style the dialog's main panel
        JPanel panel = new JPanel();
        panel.setBackground(new Color(255, 218, 51)); // Light orange background
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setLayout(new BorderLayout());

        // Style the message
        JLabel messageLabel =
                new JLabel("<html><span style='color: blue;'>" + message + "</span></html>", SwingConstants.CENTER);
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(messageLabel, BorderLayout.CENTER);

        // OK button to close the dialog
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> dialog.dispose());
        panel.add(okButton, BorderLayout.SOUTH);

        // Add panel to dialog and set properties
        dialog.getContentPane().add(panel);
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true);
    }

    public static void showCustomModalDialog(String title, String message, String message2) {
        // Create a JDialog as a custom modal message dialog
        JDialog dialog = new JDialog((Frame) null, title, true); // true makes it modal
        dialog.setSize(300, 200);
        dialog.setLocationRelativeTo(null); // Center on screen
        dialog.setUndecorated(true); // Remove the default border

        // Style the dialog's main panel
        JPanel panel = new JPanel();
        panel.setBackground(new Color(255, 218, 51)); // Light orange background
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setLayout(new BorderLayout());

        // Style the message
        JLabel messageLabel = new JLabel(
                "<html><br><span style='color: blue;'>" + message
                        + "</span><<br>---------------------------<br><span style='color: blue;'>" + message2
                        + "</span></html>",
                SwingConstants.CENTER);
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(messageLabel, BorderLayout.CENTER);

        // OK button to close the dialog
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> dialog.dispose());
        panel.add(okButton, BorderLayout.SOUTH);

        // Add panel to dialog and set properties
        dialog.getContentPane().add(panel);
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true); // This will block other input until the dialog is closed
    }
}
