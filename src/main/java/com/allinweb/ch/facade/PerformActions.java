package com.allinweb.ch.facade;

import com.allinweb.ch.component.model.BlockLoadDTO;
import com.allinweb.ch.component.model.BlockLoopInstructionLoadDTO;
import com.allinweb.ch.component.model.ComplexInstructionLoadDTO;
import com.allinweb.ch.component.model.InstructionReferenceLoadDTO;
import com.allinweb.ch.driver.ABRWebDriver;
import com.allinweb.ch.readersAndWriters.ExcelWriter;
import com.allinweb.ch.util.ABRConstants;
import com.allinweb.ch.util.ABRLogger;
import com.allinweb.ch.util.ABRPriorities;
import com.allinweb.ch.util.ABRPropertyEnum;
import com.allinweb.ch.util.ABRPropertyManager;
import com.allinweb.ch.util.ABRWebUtil;
import com.allinweb.ch.util.CryptationAlgorithm;
import com.allinweb.ch.util.ExcelReportStatusEnum;
import com.allinweb.ch.util.PriorityTypeEnum;
import com.allinweb.ch.util.UtilsMethods;
import com.google.common.base.Strings;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.layout.HBox;
import javafx.util.Pair;
import javax.swing.*;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
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

    private static final PerformMessage performMessage;
    private static final PerformDataBase performDataBase;

    static {
        performMessage = PerformMessage.getInstance();
        performDataBase = PerformDataBase.getInstance();
    }

    long totalExecutionTime = 0;

    public List<String> windowHandlesList = new ArrayList<>();

    private ABRPriorities abrPriorities;
    private ABRWebDriver abrWebDriver;
    public static Wait<WebDriver> waitForPage;
    public static Wait<WebDriver> waitForAction;
    private boolean justCalledRefreshPage = false;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 30;
    private static final Random RANDOM = new Random();

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

    public WebElement searchElement(BlockLoopInstructionLoadDTO instruction, int botJobId) {
        WebElement instructionElement = null;

        if (!StringUtils.isBlank(instruction.getPath())) {
            instructionElement = locateElement(instruction, botJobId);
        }
        return instructionElement;
    }

    public long getTotalExecutionTime() {
        return totalExecutionTime;
    }

    public void setTotalExecutionTime(long totalExecutionTime) {
        this.totalExecutionTime = totalExecutionTime;
    }

    public WebElement getElementAtCoordinates(int x, int y, WebDriver driver) {
        String script = "return document.elementFromPoint(arguments[0], arguments[1]);";

        // Execute the script and retrieve the element
        Object element = ((JavascriptExecutor) driver).executeScript(script, x, y);

        // Check if the returned element is not null and cast it to WebElement
        if (element instanceof WebElement) {
            return (WebElement) element;
        } else {
            throw new NoSuchElementException("No element found at the given coordinates: (" + x + ", " + y + ")");
        }
    }

    public boolean performWebActions(
            boolean byPassNotFound,
            String savedCoordinates,
            Pair<String, String> data,
            BlockLoopInstructionLoadDTO currentInstruction,
            Map<String, String> mapOperators,
            WebElement instructionElement,
            String actions[])
            throws Exception {

        WebDriver originalDriver = abrWebDriver.getDriver(); // Save the original WebDriver state
        boolean switchedToIframe = false;

        try {
            String xPath = currentInstruction.getPath().toLowerCase();
            if (currentInstruction.getPath() != null && xPath.contains("iframe")) {
                // Locate and switch to the iframe
                WebElement iframeElement = abrWebDriver.getDriver().findElement(By.xpath(xPath));
                WebDriver driver = abrWebDriver.getDriver().switchTo().frame(iframeElement);
                abrWebDriver.setDriver(driver);
                switchedToIframe = true;
            }

            if (instructionElement != null) {
                boolean passed = true;
                switch (actions[0]) {
                    case ABRConstants.VISUALIZE:
                        passed = scrollToElement(byPassNotFound, instructionElement);

                        if (!passed) {
                            // Try by coordinates
                            Pair<String, String> filedData = new Pair("&EMPTY", "&EMPTY");
                            passed = executeActionsAtCoordinates(savedCoordinates, filedData, ABRConstants.VISUALIZE);
                        }
                        return passed;
                    case ABRConstants.OUTPUT:
                        String fieldName = currentInstruction.getId() + "-" + currentInstruction.getName();
                        return getOutPutElement(
                                byPassNotFound,
                                instructionElement,
                                fieldName,
                                currentInstruction.getActions(),
                                mapOperators);
                    case ABRConstants.CLICK:
                    case ABRConstants.OTHER:
                        passed = clickElement(byPassNotFound, instructionElement);
                        if (!passed) {
                            // Try by coordinates
                            Pair<String, String> filedData = new Pair("&EMPTY", "&EMPTY");
                            passed = executeActionsAtCoordinates(savedCoordinates, filedData, ABRConstants.CLICK);
                        }
                        return passed;
                    case ABRConstants.INSERT:
                        if ("select".equalsIgnoreCase(instructionElement.getTagName())) {
                            passed = insertDataInSelectElement(
                                    byPassNotFound, instructionElement, savedCoordinates, data);

                            if (!passed) {
                                // Try by coordinates
                                passed = executeActionsAtCoordinates(savedCoordinates, data, ABRConstants.SELECT);
                            }
                            return passed;
                        } else {
                            passed = insertInElement(
                                    byPassNotFound,
                                    instructionElement,
                                    data.getValue(),
                                    currentInstruction.getDefaultValue(),
                                    currentInstruction.getCodified());

                            if (!passed) {
                                // Try by coordinates
                                passed = executeActionsAtCoordinates(savedCoordinates, data, ABRConstants.INSERT);
                            }
                            return passed;
                        }
                }

                onHoldForSeconds(null);
            }

            return true;
        } finally {
            // Restore the original WebDriver state
            if (switchedToIframe) {
                abrWebDriver.setDriver(originalDriver);
            }
        }
    }

    public void performOtherActions(boolean byPassNotFound, BlockLoopInstructionLoadDTO instruction, String actions[])
            throws Exception {

        switch (actions[0]) {
            case ABRConstants.LIST_OPERATION:
                listOperation(byPassNotFound, instruction);
                break;
            case ABRConstants.HOLD:
            case ABRConstants.REFRESH_HOLD:
                //                        executeAlert(instruction);
                onHoldForSeconds(instruction);
                break;
            case ABRConstants.REFRESH_ONLY:
            case ABRConstants.REFRESH_LOOP:
                refreshPage();
                break;
            case ABRConstants.QUIT:
                quit(1);
                break;
            case ABRConstants.SCREEN:
                break;
        }

        onHoldForSeconds(null);
    }

    public String performOperatorActions(
            boolean byPassNotFound,
            BlockLoopInstructionLoadDTO instruction,
            String targetXPath,
            String action,
            String[] operations,
            String parentField,
            Map<String, String> mapOperators)
            throws Exception {

        WebElement instructionElement = null;

        if (!StringUtils.isBlank(targetXPath)) {
            instructionElement =
                    locateTargetElement(byPassNotFound, targetXPath, instruction.getActionCustomMaxWaitSec());
        }
        if (instructionElement != null) {

            switch (action) {
                case "SET":
                    insertTargetElement(byPassNotFound, instructionElement, operations[0], operations[1]);
                    return "SET_VALUE to (Parent: " + parentField + ") Var:" + operations[0] + " <-- " + operations[1];
                case "GET":
                    String valueElem;
                    if (mapOperators.containsKey(parentField)) {
                        valueElem = mapOperators.get(parentField);
                    } else {
                        valueElem = getValueInElement(byPassNotFound, instructionElement);
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

    private WebElement locateTargetElement(boolean byPassNotFound, String targetXPath, Integer actionCustomMaxWaitSec) {

        String tagName = null;
        try {
            tagName = removeTrailingSlash(targetXPath);
            tagName = extractTagName(targetXPath);
        } catch (Exception e) {
            ABRLogger.getInstance(PerformActions.class)
                    .fine(String.format(
                            "Error RemoveTrailingSlash for %s -> xPath  %s -> Cause: %s",
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
                                            "Could Not Find xPath \"%s\" Criteria \"%s\" -> Cause: %s",
                                            targetXPath, criteria, e.getMessage()));

                            showNotFoundElement(targetXPath, criteria);

                            //                                SwingUtilities.invokeLater(() ->

                            if (!byPassNotFound) {
                                performMessage.couldNotFindElement(String.valueOf(criteria));
                            }
                        }
                    } else if (actionCustomMaxWaitSec != null) {
                        try {
                            new WebDriverWait(abrWebDriver.getDriver(), Duration.ofSeconds(actionCustomMaxWaitSec))
                                    .until(ExpectedConditions.presenceOfElementLocated(criteria));
                        } catch (Exception e) {
                            ABRLogger.getInstance(PerformActions.class)
                                    .fine(String.format(
                                            "Could Not Find xPath \"%s\" Criteria \"%s\" -> Cause: %s",
                                            targetXPath, criteria, e.getMessage()));
                            if (!byPassNotFound) {
                                performMessage.couldNotFindElement(String.valueOf(criteria));
                            }
                        }
                    } else {
                        try {
                            waitForAction.until(ExpectedConditions.visibilityOfElementLocated(criteria));
                        } catch (Exception e) {
                            ABRLogger.getInstance(PerformActions.class)
                                    .fine(String.format(
                                            "Could Not Find xPath \"%s\" Criteria \"%s\" -> Cause: %s",
                                            targetXPath, criteria, e.getMessage()));

                            if (!byPassNotFound) {
                                performMessage.couldNotFindElement(String.valueOf(criteria));
                            }
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

    private void callErrorMessageNotEnabled(String criteria) {
        performMessage.showCustomModalDialog(
                String.format("The Element \"%s\" is not Enabled", criteria),
                "1. Consider Fill Up all the Mandatory Fields",
                null,
                null,
                null,
                true,
                null,
                0);
    }

    private void showNotFoundElement(String targetXPath, By criteria) {}

    private WebElement locateElement(BlockLoopInstructionLoadDTO currentInstruction, int botJobId) {

        //        WebElement elementInsideIframe = null;
        //                if (xPath.toLowerCase().contains("iframe")){
        //                    // Switch to the iframe using ID or name
        //        //            abrWebDriver.getDriver().switchTo().frame("iframeID");
        //
        //                    // Alternatively, switch to the iframe using a WebElement
        //        //            WebElement iframeElement =
        //         abrWebDriver.getDriver().findElement(By.xpath("//iframe[@name='iframeName']"));
        //                    WebElement iframeElement = abrWebDriver.getDriver().findElement(By.xpath(xPath));
        //                    abrWebDriver.getDriver().switchTo().frame(iframeElement);
        //                    // Now, interact with elements inside the iframe
        //                    elementInsideIframe = abrWebDriver.getDriver().findElement(By.id("elementID"));
        //                }
        //
        //                if (elementInsideIframe != null) {
        //                    element = elementInsideIframe;
        //                }
        //
        //                if (elementInsideIframe != null) {
        //                    // Switch back to the main page
        //                    abrWebDriver.getDriver().switchTo().defaultContent();
        //                }

        String instructionPath = currentInstruction.getPath();
        String tagName = null;
        try {
            tagName = removeTrailingSlash(instructionPath);
            tagName = extractTagName(instructionPath);
        } catch (Exception e) {
            ABRLogger.getInstance(PerformActions.class)
                    .fine(String.format(
                            "Error RemoveTrailingSlash for %s -> xPath  %s -> Cause: %s",
                            tagName, instructionPath, e.getMessage()));
        }
        List<InstructionReferenceLoadDTO> instructionReferenceList =
                currentInstruction.getInstructionReferenceLoadDTOList();

        if (instructionReferenceList.size() == 0) {
            ABRLogger.getInstance(PerformActions.class)
                    .warning("####    Not XPath to Be Located!   ####"
                            + "\n####    Remove and Re-Scan the Failed Field Again   ####");

            return null;
        }

        waitPage();

        // If Not Loaded get if the JobId Changed
        if (abrPriorities.getJobId() == null) {
            abrPriorities.setJobId(botJobId);
            if (currentInstruction.getPriority() != null) {
                abrPriorities.loadPrioritiesFromString(currentInstruction.getPriority());
            } else {
                abrPriorities.loadPriorities();
            }
        } else if (abrPriorities.getJobId() != botJobId) {
            abrPriorities.setJobId(botJobId);
            if (currentInstruction.getPriority() != null) {
                abrPriorities.loadPrioritiesFromString(currentInstruction.getPriority());
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
                        case coordinates -> {
                            /// THIS MEANT TO BE USED JUST TO LOCATE THE ELEMENT NOT APPLYING ACTIONS TO IT

                            //                            Pair<String, String> filedData = new Pair("martini",
                            // "Martini");
                            //                            try {
                            //                                executeActionsAtInstructionCoordinates(currentInstruction,
                            // filedData);
                            //                            } catch (Exception e) {
                            //                                System.out.println(e.getMessage());
                            //
                        } // System.out.println("coordinates case");
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
                        //                        showAlert(
                        //                                Alert.AlertType.ERROR,
                        //                                "ABR Web Driver is NULL",
                        //                                "Restart the APP",
                        //                                "Close all Browser attached or Restart the APP");

                        String msg1 = "ABR Web Driver is NULL";
                        String msg2 = "Restart the APP";
                        String msg3 = "Close all Browser or Restart the APP";

                        performMessage.errorMessage("Parent Id Error", msg1, msg2, msg3, null, 0);

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
                            //                                System.out.println(e.getMessage());
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
                                                        "Could Not Find xPath \"%s\" Criteria \"%s\" -> Cause: %s",
                                                        instructionPath, criteria, e.getMessage()));

                                        //
                                        // performMessage.couldNotFindElement(String.valueOf(criteria));
                                    }
                                } else if (currentInstruction.getActionCustomMaxWaitSec() != null) {
                                    try {

                                        new WebDriverWait(
                                                        abrWebDriver.getDriver(),
                                                        Duration.ofSeconds(
                                                                currentInstruction.getActionCustomMaxWaitSec()))
                                                .until(ExpectedConditions.presenceOfElementLocated(criteria));
                                    } catch (Exception e) {
                                        ABRLogger.getInstance(PerformActions.class)
                                                .fine(String.format(
                                                        "Could Not Find xPath \"%s\" Criteria \"%s\" -> Cause: %s",
                                                        instructionPath, criteria, e.getMessage()));

                                        //
                                        // performMessage.couldNotFindElement(String.valueOf(criteria));
                                    }
                                } else {
                                    try {
                                        waitForAction.until(ExpectedConditions.visibilityOfElementLocated(criteria));
                                    } catch (Exception e) {
                                        ABRLogger.getInstance(PerformActions.class)
                                                .fine(String.format(
                                                        "Could Not Find xPath \"%s\" Criteria \"%s\" -> Cause: %s",
                                                        instructionPath, criteria, e.getMessage()));

                                        //
                                        // performMessage.couldNotFindElement(String.valueOf(criteria));
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

    private String insertTargetElement(
            boolean byPassNotFound, WebElement element, String fieldName, String dataFieldValue) throws Exception {
        UtilsMethods.exceptionIfNullWebElement(element);
        try {
            waitForAction.until(ExpectedConditions.visibilityOf(element));
        } catch (Exception e) {
            ABRLogger.getInstance(PerformActions.class)
                    .fine(String.format(
                            "Could Not Find Field Name \"%s\" Value \"%s\" -> Cause: %s",
                            fieldName, dataFieldValue, e.getMessage()));

            if (!byPassNotFound) {
                performMessage.couldNotFindElement(fieldName);
            }
        }

        if (dataFieldValue != null) {
            element.clear();
            element.sendKeys(dataFieldValue);
            element.sendKeys(Keys.TAB);
        }

        return fieldName + "->" + dataFieldValue;
    }

    private String getValueInElement(boolean byPassNotFound, WebElement element) throws Exception {
        UtilsMethods.exceptionIfNullWebElement(element);
        try {
            waitForAction.until(ExpectedConditions.visibilityOf(element));
        } catch (Exception e) {
            ABRLogger.getInstance(PerformActions.class)
                    .fine(String.format(
                            "Could Not Find TagName \"%s\" -> Cause: %s", element.getTagName(), e.getMessage()));

            if (!byPassNotFound) {
                performMessage.couldNotFindElement(element.getTagName());
            }
        }

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

    public synchronized String onHoldInSeconds(Integer seconds) throws Exception {
        wait(fromSecondsToMilliseconds(TimeUnit.SECONDS, seconds));
        return "HOLD" + "->" + seconds + " seconds";
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
                                "WaitForPage.until(d -> ((JavascriptExecutor) driver) error: %s", ex.getMessage()));

                performMessage.couldNotFindElement("WaitForPage.until");
            }
        } else {
            // Handle the case when driver is null (e.g., throw an exception or initialize the driver)
            ABRLogger.getInstance(PerformActions.class)
                    .warning("WaitForPage.until(d -> ((JavascriptExecutor) driver) is returning nulls");
        }
    }

    public boolean scrollToElement(boolean byPassNotFound, WebElement element) throws Exception {
        try {
            UtilsMethods.exceptionIfNullWebElement(element);
            ((JavascriptExecutor) abrWebDriver.getDriver())
                    .executeScript("arguments[0].scrollIntoView(true);", element);
            return true;
        } catch (Exception e) {
            ABRLogger.getInstance(PerformActions.class)
                    .severe(String.format(
                            "Failed to Scroll to Element \"%s\" -> Cause: %s", element.getTagName(), e.getMessage()));
            if (!byPassNotFound) {
                performMessage.couldNotFindElement("Failed to Scroll to Element " + element.getTagName());
            }
            return false;
        }
    }

    public boolean clickElement(boolean byPassNotFound, WebElement element) throws Exception {
        UtilsMethods.exceptionIfNullWebElement(element);
        if (!element.isEnabled()) {
            //        callErrorMessageNotEnabled(element.getTagName());
            performMessage.showCustomModalDialog(
                    "BOT JOB STOP",
                    String.format("The Element \"%s\" is not Enabled", element.getTagName()),
                    "Consider Fill Up all the Mandatory Fields!");
            // throw new TimeoutException();
            return false;
        }

        //        try {
        //            waitForAction.until(ExpectedConditions.visibilityOf(element).andThen(e -> {
        //                ((JavascriptExecutor) abrWebDriver.getDriver())
        //                        .executeScript("arguments[0].scrollIntoView(true);", element);
        //                return waitForAction.until(ExpectedConditions.elementToBeClickable(element));
        //            }));
        //        } catch (Exception e) {
        //            ABRLogger.getInstance(PerformActions.class)
        //                    .fine(String.format(
        //                            "Could Not Find TagName \"%s\" -> Cause: %s", element.getTagName(),
        // e.getMessage()));
        //
        //            if (!byPassNotFound) {
        //                performMessage.couldNotFindElement(element.getTagName());
        //            }
        //            return false;
        //        }

        try {
            element.click();
            return true;
        } catch (ElementClickInterceptedException e) {
            try {
                JavascriptExecutor jse = (JavascriptExecutor) abrWebDriver.getDriver();
                jse.executeScript("arguments[0].click()", element);
                return true;
            } catch (Exception ex) {

                ABRLogger.getInstance(PerformActions.class)
                        .fine(String.format(
                                "Could Not Click on  \"%s\" -> Cause: %s", element.getTagName(), e.getMessage()));
                return false;
            }
        }
    }

    public void refreshPage() {
        abrWebDriver.getDriver().navigate().refresh();
        justCalledRefreshPage = true;
    }

    private boolean insertInElement(
            boolean byPassNotFound, WebElement element, String dataFieldValue, String defaultValue, boolean isEncrypted)
            throws Exception {
        UtilsMethods.exceptionIfNullWebElement(element);

        try {
            waitForAction.until(ExpectedConditions.visibilityOf(element));
        } catch (Exception e) {
            ABRLogger.getInstance(PerformActions.class)
                    .fine(String.format(
                            "Could Not Find TagName \"%s\" -> Cause: %s", element.getTagName(), e.getMessage()));
            if (!byPassNotFound) {
                performMessage.couldNotFindElement(element.getTagName());
            }
            return false;
        }

        try {

            if (Strings.isNullOrEmpty(defaultValue)) {

                if (isEncrypted) {
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
            } else {
                dataFieldValue = defaultValue;

                if (isEncrypted) {
                    dataFieldValue = CryptationAlgorithm.decrypt(dataFieldValue);
                }
                element.sendKeys(dataFieldValue);
            }
        } catch (Exception e) {
            ABRLogger.getInstance(PerformActions.class)
                    .severe(String.format(
                            "Could Not Input Value to \"%s\" -> Cause: %s", element.getTagName(), e.getMessage()));

            performMessage.couldNotFindElement("Could Input Values to Element " + element.getTagName());
            return false;
        }

        return true;
    }

    /**
     * Extracts the dataFieldName and dataFieldValue based on the instruction and DTO.
     */
    public Pair<String, String> extractFieldData(
            Map<String, String> data, String[] actions, String defaultValue, boolean isEncrypted) throws Exception {

        String dataFieldName = "";
        String dataFieldValue = "";

        if (data != null) {
            if (actions.length > 1) {
                dataFieldName = actions[1].split(ABRConstants.PATH_FIELD_SUBSTITUTION)[0];
                dataFieldValue = data.get(dataFieldName);

                if (isEncrypted && dataFieldValue != null) {
                    dataFieldValue = CryptationAlgorithm.decrypt(dataFieldValue);
                }
            }
        } else if (!Strings.isNullOrEmpty(defaultValue)) {
            dataFieldValue = defaultValue;
            if (isEncrypted) {
                dataFieldValue = CryptationAlgorithm.decrypt(dataFieldValue);
            }
        }

        return new Pair<>(dataFieldName, dataFieldValue);
    }

    private boolean insertDataInSelectElement(
            boolean byPassNotFound, WebElement element, String coordinates, Pair<String, String> data)
            throws Exception {
        UtilsMethods.exceptionIfNullWebElement(element);
        try {
            waitForAction.until(ExpectedConditions.visibilityOf(element));
        } catch (Exception e) {
            ABRLogger.getInstance(PerformActions.class)
                    .fine(String.format(
                            "Could Not Find Select \"%s\" Value  \"%s\" -> Cause: %s",
                            data.getKey(), data.getValue(), e.getMessage()));
            if (!byPassNotFound) {
                performMessage.couldNotFindElement(data.getKey());
            }
        }

        try {
            // Create a Select instance to interact with the dropdown
            //            Select selectCountry = new Select(element);
            //            // Select "Switzerland" by visible text
            //            selectCountry.selectByVisibleText(data.getValue());

            String[] coordArray = new String[] {coordinates, "coordinates"};
            sequenceOfCommands(element, ABRConstants.SELECT, coordArray, data, abrWebDriver.getDriver());

        } catch (Exception e) {
            ABRLogger.getInstance(PerformActions.class)
                    .severe(String.format(
                            "Could Not Input Value to \"%s\" -> Cause: %s", element.getTagName(), e.getMessage()));

            performMessage.couldNotFindElement("Could Input Values to Element " + element.getTagName());

            return false;
        }
        return true;
    }

    private boolean getOutPutElement(
            boolean byPassNotFound,
            WebElement element,
            String fieldName,
            String action,
            Map<String, String> mapOperators)
            throws Exception {

        UtilsMethods.exceptionIfNullWebElement(element);

        try {
            waitForAction.until(ExpectedConditions.visibilityOf(element));
        } catch (Exception ex) {
            ABRLogger.getInstance(PerformActions.class)
                    .warning(
                            String.format("Could Not Find Field Name \"%s\" -> Cause: %s", fieldName, ex.getMessage()));

            if (!byPassNotFound) {
                performMessage.couldNotFindElement(fieldName);
            }
            return false;
        }

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
        } catch (Exception e) {
            ABRLogger.getInstance(PerformActions.class)
                    .warning(String.format("Element is not clickable: \"%s\"", fieldName));
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
            mapOperators.put(fieldName, "Failed to Load teh Text");
            ABRLogger.getInstance(PerformActions.class)
                    .severe(String.format("Failed to retrieve text from element for: %s", fieldName));
        }

        return true;
    }

    private void listOperation(boolean byPassNotFound, BlockLoopInstructionLoadDTO instructionDTO) {

        /*
        TODO: Da rivedere, attualmente non del tutto funzionante
        Complex instruction string interpretation:
        [       0       ||       1      ||       2         ||    3    ||        4       ||  5   ||            6                ]
        [backward_button||forward_button||list_elements_tag||condition||expected_results||action||sub_element_on_execute_action]
        */
        List<ComplexInstructionLoadDTO> complexInstructionDTOS = instructionDTO.getComplexInstructionLoadDTOList();
        String[] complexActionParts =
                complexInstructionDTOS.get(0).getInstruction().split(ABRConstants.COMPLEX_INSTRUCTION_SEPARATOR);
        List<WebElement> webElementList;
        WebElement forwardButton;
        WebElement backwardButton;
        boolean shouldContinue = true;

        boolean existNextPage;
        do {
            try {
                waitForPage.until(ExpectedConditions.visibilityOfElementLocated(By.tagName(complexActionParts[2])));
            } catch (Exception e) {
                ABRLogger.getInstance(PerformActions.class)
                        .fine(String.format(
                                "Could Not Find TagName \"%s\" Criteria \"%s\" -> Cause: %s",
                                complexActionParts[2], By.tagName(complexActionParts[2]), e.getMessage()));

                if (!byPassNotFound) {
                    performMessage.couldNotFindElement(complexActionParts[2]);
                }
            }

            backwardButton = abrWebDriver.getDriver().findElement(By.xpath(complexActionParts[0]));
            forwardButton = abrWebDriver.getDriver().findElement(By.xpath(complexActionParts[1]));
            webElementList = abrWebDriver.getDriver().findElements(By.tagName(complexActionParts[2]));

            WebElement element;
            WebElement reasonWebElement;
            for (int i = 0; i < 5; i++) {

                if (i != 0) {

                    try {
                        waitForPage.until(
                                ExpectedConditions.visibilityOfElementLocated(By.tagName(complexActionParts[2])));
                        webElementList = abrWebDriver.getDriver().findElements(By.tagName(complexActionParts[2]));
                    } catch (Exception e) {
                        ABRLogger.getInstance(PerformActions.class)
                                .fine(String.format(
                                        "Could Not Find TagName \"%s\" Criteria \"%s\" -> Cause: %s",
                                        complexActionParts[2], By.tagName(complexActionParts[2]), e.getMessage()));

                        if (!byPassNotFound) {
                            performMessage.couldNotFindElement(complexActionParts[2]);
                        }
                    }
                }

                element = webElementList.get(i);
                try {
                    Thread.sleep(1000);

                    reasonWebElement = element.findElement(
                            By.xpath(".//div[@class='payments-table-field reason ng-star-inserted']"));
                    if (!UtilsMethods.testFixedCheck(reasonWebElement.getText())) {
                        continue;
                    }

                    clickElement(
                            byPassNotFound,
                            element.findElement(By.xpath(
                                    ".//button[@test-id='web-banking-payment-core.payment-ctx-action.button']")));
                    clickElement(
                            byPassNotFound,
                            abrWebDriver
                                    .getDriver()
                                    .findElement(
                                            By.xpath(
                                                    ".//button[@test-id='web-banking-payment-core.payment-ctx-action.payment-action-VIEW']")));

                    Thread.sleep(1000);
                    clickElement(
                            byPassNotFound,
                            abrWebDriver
                                    .getDriver()
                                    .findElement(
                                            By.xpath(
                                                    ".//button[@test-id='web-banking-common.export-to-file.single-file-button']")));

                    Thread.sleep(1000);
                    clickElement(
                            byPassNotFound,
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
                scrollToElement(byPassNotFound, forwardButton);
                clickElement(byPassNotFound, forwardButton);
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

    public short operationLog(boolean success, String mainMsg, String currentExecution, long duration) {

        if (success) {

            ABRLogger.getInstance(PerformActions.class)
                    .info(String.format(
                            success
                                    ? "SUCCESS %s Current Cmd: %s - Duration: %s"
                                    : "FAILED %s Current Cmd: %s - Duration: %s",
                            mainMsg,
                            currentExecution,
                            LocalTime.ofNanoOfDay(duration).format(FORMAT_TIME)));
        } else {

            ABRLogger.getInstance(PerformActions.class)
                    .warning(String.format(
                            success
                                    ? "SUCCESS %s Current Cmd: %s - Duration: %s"
                                    : "FAILED %s Current Cmd: %s - Duration: %s",
                            mainMsg,
                            currentExecution,
                            LocalTime.ofNanoOfDay(duration).format(FORMAT_TIME)));
        }

        return (short) (success ? ExcelReportStatusEnum.SUCCESS.ordinal() : ExcelReportStatusEnum.ERROR.ordinal());
    }

    public String pauseEngine(String blockName) {

        //        JavascriptExecutor js = (JavascriptExecutor) abrWebDriver.getDriver();
        //        js.executeScript("alert('This is a custom alert modal!');");
        String message = "PAUSE REQUESTED "
                + "<br>-------------------------------------------------<br>"
                + "BOT JOB in PAUSE MODE:: <b style='color:red;'><br>"
                + blockName
                + "</b>"
                + "<br>-------------------------------------------------<br>";

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
                    + "<br>-------------------------------------------------<br>"
                    + "Validation Error: <b style='color:red;'>"
                    + currentInstruction.getName()
                    + "</b>"
                    + "<br>-------------------------------------------------<br>"
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
            ABRConstants.ConditionStatus conditionStatus) {

        if (conditionStatus.equals(ABRConstants.ConditionStatus.NONE)) {
            //            showAlert(
            //                    Alert.AlertType.ERROR,
            //                    "GET is Not Defined for \"" + currentInstruction.getName() + "\"",
            //                    "\"" + currentInstruction.getName() + "\" - GET is Not Defined",
            //                    "There is NOT GET VALUE defined for: "
            //                            + currentInstruction.getName()
            //                            + "\n --------------------- "
            //                            + "\nCheck the GET for "
            //                            + currentInstruction.getParentId() + "-"
            //                            + currentInstruction.getOperation());

            String msg1 = "There is NOT GET VALUE defined for: " + currentInstruction.getName();
            String msg2 =
                    "Check the GET for " + currentInstruction.getParentId() + "-" + currentInstruction.getOperation();

            performMessage.errorMessage(
                    "GET is Not Defined for \"" + currentInstruction.getName() + "\"", msg1, msg2, null, null, 0);
        }

        String conditionalBlock = conditionStatus.equals(ABRConstants.ConditionStatus.IF_PASSED)
                ? "Closing Block { IF -> ELSE }  -> "
                : conditionStatus.equals(ABRConstants.ConditionStatus.ELSEIF_PASSED)
                        ? "Closing Block { ELSEIF -> ELSE }  -> "
                        : conditionStatus.equals(ABRConstants.ConditionStatus.ELSE_PASSED)
                                ? "Closing Block { ELSE -> ENDIF }  -> "
                                : "";

        if (!conditionStatus.equals(ABRConstants.ConditionStatus.NONE)) {
            return "Failed to Execute Cmd: " + conditionalBlock + " -> " + lastInstructionExecuted;

        } else {
            return "Failed to Execute Cmd: " + lastInstructionExecuted;
        }
    }

    public String parentValueIsNotDefined(String instructionName, String parentField, String resultActions) {

        //        showAlert(
        //                Alert.AlertType.ERROR,
        //                "Parent is Not Defined for \"" + instructionName + "\"",
        //                "\"" + instructionName + "\" - Parent is Not Defined",
        //                "There is NOT PARENT VALUE defined for: "
        //                        + instructionName
        //                        + "\n --------------------- "
        //                        + "\nCheck the PARENT Web field for "
        //                        + parentId + "- Unknown");
        String msg1 = "Parent is Not Defined for \"" + instructionName + "\"";
        String msg2 = "There is NOT PARENT VALUE defined for: ";
        String msg3 = "Check the PARENT Web field for \"" + parentField + "\"";

        performMessage.errorMessage("Parent Id Error", msg1, msg2, msg3, null, 0);

        return "Failed to Execute Cmd: " + resultActions;
    }

    public String parentValueIsNotDefinedEngine(String instructionName, String parentField, String resultActions) {

        //        showAlert(
        //                Alert.AlertType.ERROR,
        //                "Parent is Not Defined for \"" + instructionName + "\"",
        //                "\"" + instructionName + "\" - Parent is Not Defined",
        //                "There is NOT PARENT VALUE defined for: "
        //                        + instructionName
        //                        + "\n --------------------- "
        //                        + "\nCheck the PARENT Web field for \"" + parentField+ "\"");

        String msg1 = "Parent is Not Defined for \"" + instructionName + "\"";
        String msg2 = "There is NOT PARENT VALUE defined for: \"" + instructionName + "\"";
        String msg3 = "Check the PARENT Web field for \"" + parentField + "\"";

        performMessage.errorMessage("Parent Id Error", msg1, msg2, msg3, null, 0);

        return "Failed to Execute Cmd: " + resultActions;
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
                    + "<br>-------------------------------------------------<br>"
                    + "<b style='color:red;'>" + "Does not belong to this block: \"" + blockLoad.getBlockOrderNumber()
                    + "-\"" + blockLoad.getName() + "\"" + "</b>"
                    + "</br>"
                    + "<b style='color:red;'>"
                    + "Attempted Operation : \"" + currentInstruction.getActions() + "\" -> \""
                    + currentInstruction.getOperation() + "\"" + "</b>"
                    + "<br>-------------------------------------------------<br>"
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
            String lastInstructionExecuted,
            ABRConstants.ConditionStatus conditionStatus) {

        if (conditionStatus.equals(ABRConstants.ConditionStatus.NONE)) {
            String operation = currentInstruction.getOperation();
            int colonIndex = operation.indexOf(":");
            String parentOperationPart = colonIndex != -1 ? operation.substring(0, colonIndex) : "Unknown Operation";

            String msg1 = "The Parent Id: \"(" + currentInstruction.getParentId() + ")" + parentOperationPart + "\"";
            String msg2 = "Does not belong to the block: \"" + blockLoad.getBlockOrderNumber() + "-"
                    + blockLoad.getName() + "\"";
            String msg3 = "Attempted Operation : \""
                    + (currentInstruction.getActions().equals(ABRConstants.EXTRACT_FIELD)
                            ? "Extract "
                            : currentInstruction.getActions())
                    + "\" -> \""
                    + operation + "\"";
            String msg4 = "Check the Web Field \" ( ID ) <NAME> \" per Block";

            performMessage.errorMessage("Parent Id Error", msg1, msg2, msg3, msg4, 0);
        }

        String conditionalBlock = conditionStatus.equals(ABRConstants.ConditionStatus.IF_PASSED)
                ? "Closing Block { IF -> ELSE }  -> "
                : conditionStatus.equals(ABRConstants.ConditionStatus.ELSEIF_PASSED)
                        ? "Closing Block { ELSEIF -> ELSE }  -> "
                        : conditionStatus.equals(ABRConstants.ConditionStatus.ELSE_PASSED)
                                ? "Closing Block { ELSE -> ENDIF }  -> "
                                : "";

        if (!conditionStatus.equals(ABRConstants.ConditionStatus.NONE)) {
            ABRLogger.getInstance(PerformActions.class)
                    .warning(String.format(
                            "%sParent Id Error Check Parent Id: %d For the \"%s\" Does not belong to this block: %d-%s",
                            conditionalBlock,
                            currentInstruction.getParentId(),
                            currentInstruction.getOperation(),
                            blockLoad.getId(),
                            blockLoad.getName()));
        } else {
            ABRLogger.getInstance(PerformActions.class)
                    .severe(String.format(
                            "Parent Id Error Check Parent Id: %d For the \"%s\" Does not belong to this block: %d-%s",
                            currentInstruction.getParentId(),
                            currentInstruction.getOperation(),
                            blockLoad.getId(),
                            blockLoad.getName()));
        }

        if (!conditionStatus.equals(ABRConstants.ConditionStatus.NONE)) {
            return "Failed to Execute Cmd: " + conditionalBlock + " -> " + lastInstructionExecuted;
        } else {
            return "Failed to Execute Cmd: " + lastInstructionExecuted;
        }
    }

    public String checkValidationFailedEngine(
            String parent,
            String expected,
            String lastInstructionExecuted,
            String[] operations,
            boolean ifClause,
            boolean elseClause,
            boolean byPassFlagLoop) {
        if (!ifClause && !elseClause && !byPassFlagLoop) {
            String message = "The Value of: <b style='color:red;'>\"" + operations[2] + "\""
                    + "</b> is not " + "<b>" + operations[1] + " "
                    + " \"" + expected + "\"" + "</b> Length: (<b>" + expected.length() + "</b>)"
                    + "<br>-------------------------------------------------<br>"
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
            ABRConstants.ConditionStatus conditionStatus,
            boolean byPassFlagLoop) {

        if (conditionStatus.equals(ABRConstants.ConditionStatus.NONE) && !byPassFlagLoop) {
            //            showAlert(
            //                    Alert.AlertType.ERROR,
            //                    "Validation Error",
            //                    "Check Validation Error",
            //                    "The Value of: \"" + operations[2] + "\" is not " + operations[1] + " \""
            //                            + expected + "\" Length: ("
            //                            + expected.length()
            //                            + ")" + "\n --------------------- "
            //                            + "\nThe Variable \""
            //                            + operations[0] + "\" holds value \"" + operations[2] + "\""
            //                            + "\nCurrent Web Field \"" + parent + "\" value: \""
            //                            + expected + "\" Length: (" + expected.length()
            //                            + ")" + "\nExpected value: "
            //                            + operations[2]
            //                            + " Length: ("
            //                            + operations[2].length()
            //                            + ")");

            String msg1 = "The Value of: \"" + operations[2] + "\" is not " + operations[1] + " \""
                    + expected + "\" Length: ("
                    + expected.length()
                    + ")";

            String msg2 = "The Variable \"" + operations[0] + "\" holds value \"" + operations[2] + "\"";

            String msg3 = "Current Web Field \"" + parent + "\" value: \""
                    + expected + "\" Length: (" + expected.length()
                    + ")";
            String msg4 = "\nExpected value: " + operations[2] + " Length: (" + operations[2].length() + ")";

            performMessage.errorMessage("Check Validation Error", msg1, msg2, msg3, msg4, 0);
        }

        String conditionalBlock = conditionStatus.equals(ABRConstants.ConditionStatus.IF_PASSED)
                ? "Closing Block { IF -> ELSE }  -> "
                : conditionStatus.equals(ABRConstants.ConditionStatus.ELSEIF_PASSED)
                        ? "Closing Block { ELSEIF -> ELSE }  -> "
                        : conditionStatus.equals(ABRConstants.ConditionStatus.ELSE_PASSED)
                                ? "Closing Block { ELSE -> ENDIF }  -> "
                                : "";

        if (!conditionStatus.equals(ABRConstants.ConditionStatus.NONE)) {
            return "Failed to Execute Cmd: " + conditionalBlock + " -> " + lastInstructionExecuted;

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

    public boolean excelReportWrite(
            ABRConstants.ConditionStatus currentCondition,
            String blockName,
            boolean success,
            String[] actions,
            Pair<String, String> msgLoop,
            long duration,
            Map<String, String> dataExcel,
            ExcelWriter.ExcelChain writerReport) {
        return writerReport.insertInstructionResult(
                currentCondition, blockName, actions, msgLoop, dataExcel, LocalTime.ofNanoOfDay(duration), success);
    }

    public long duration(long startTime) {
        long currentInstructionEndTime = System.nanoTime();
        return currentInstructionEndTime - startTime;
    }

    public String blockGotoFailed(String resultActions) {
        //        showAlert(
        //                Alert.AlertType.ERROR, "Block GO TO Error", "Check Correct Block Existence", "CMD: \n" +
        // resultActions);

        String msg1 = "Block GO TO Error";
        String msg2 = "Check Correct Block Existence";
        String msg3 = "CMD: " + resultActions;

        performMessage.errorMessage("Parent Id Error", msg1, msg2, msg3, null, 0);

        ABRLogger.getInstance(PerformActions.class)
                .severe("Block GO TO Error: -> Check Correct Block Existence! -> CMD: " + resultActions);

        return resultActions;
    }

    public void gotoLimitExecution(int executionTimes, String lastInstructionExecuted) {
        //        showAlert(
        //                Alert.AlertType.ERROR,
        //                "Block Execution Time LIMIT",
        //                "Attention The Process Reached the LIMIT of Block Loop Executions",
        //                String.format(
        //                        "Attention the Process Reached the Block LOOP LIMIT of %d\nLast Instruction Executed :
        // %s\nWe are Exiting All of processes Now!",
        //                        executionTimes, lastInstructionExecuted));

        performMessage.errorMessage(
                "Block Execution LIMIT Reached!",
                String.format("Process Reached BLOCK LIMIT of %d executions", executionTimes),
                "Exiting All processes Now!",
                "Last Execution",
                lastInstructionExecuted,
                0);
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
            System.out.println(e.getMessage());
        }

        // Accept (close) the alert
        alert.accept();
    }

    public String actionResultMessage(String blockJobName, String actions[], Pair<String, String> msgInstruction) {

        switch (actions[0]) {
            case ABRConstants.VISUALIZE:
                return "Visualize action executed for " + msgInstruction.getKey();
            case ABRConstants.OTHER:
                return "Other Element --> " + msgInstruction.getKey();
            case ABRConstants.OUTPUT:
                return "Output Element --> " + msgInstruction.getKey();
            case ABRConstants.CLICK:
                return "Click Element --> " + msgInstruction.getKey();
            case ABRConstants.INSERT:
                return "Insert action for  -> " + msgInstruction.getKey() + " = " + msgInstruction.getValue();
            case ABRConstants.LIST_OPERATION:
                return "List Operation performed for " + msgInstruction.getKey();
            case ABRConstants.HOLD:
                return "Hold executed ( " + msgInstruction.getKey() + " )";
            case ABRConstants.PAUSE:
                return "Pause action triggered";
            case ABRConstants.GOTO:
                if (msgInstruction.getValue().equals("Unknown")) {
                    return msgInstruction.getKey();
                } else {
                    String[] parts = msgInstruction.getKey().split(":");
                    return String.format(
                            "GO TO Block \"%s\" Limit %s times",
                            "(" + parts[0] + ")-#" + parts[2] + " " + parts[3], msgInstruction.getValue());
                }
            case ABRConstants.REFRESH_ONLY:
                return " Refresh Web Page";
            case ABRConstants.REFRESH_HOLD:
                String[] msgParent = msgInstruction.getKey().split(":");
                String[] msgValue = msgInstruction.getValue().split(":");
                return String.format(
                        "Wait for Parent \"%s\" Limit %s seconds",
                        "(" + msgParent[1] + ") " + msgParent[2], msgValue[0]);
            case ABRConstants.LOOP:
                if (msgInstruction.getValue().equals("Unknown")) {
                    return msgInstruction.getKey();
                } else {
                    msgParent = msgInstruction.getKey().split(":");
                    return String.format(
                            "Jump To Parent \"%s\" Limit %s times",
                            msgParent[0] + "-(" + msgParent[1] + ") " + msgParent[2], msgInstruction.getValue());
                }
            case ABRConstants.REFRESH_LOOP:
                if (msgInstruction.getValue().equals("Unknown")) {
                    return msgInstruction.getKey();
                } else {
                    msgParent = msgInstruction.getKey().split(":");
                    msgValue = msgInstruction.getValue().split(":");
                    return String.format(
                            "Refresh in %s seconds Loop %s times Jump To Parent \"%s\" ",
                            msgValue[0], msgValue[1], msgParent[0] + "-(" + msgParent[1] + ") " + msgParent[2]);
                }
            case ABRConstants.QUIT:
                return "Quit action processed";
            case ABRConstants.SCREEN:
                return "Screen action executed for " + msgInstruction.getKey() + " --> " + blockJobName;
            case ABRConstants.GET_VALUE:
            case ABRConstants.SET_VALUE:
                return actions[0]
                        + ABRConstants.BLANK_STRING
                        + msgInstruction.getKey()
                        + ABRConstants.BLANK_STRING
                        + msgInstruction.getValue();
            case ABRConstants.CHECK_VALUE:
                return actions[0]
                        + ABRConstants.BLANK_STRING
                        + msgInstruction.getValue()
                        + ABRConstants.BLANK_STRING
                        + msgInstruction.getKey();
            case ABRConstants.EXTRACT_FIELD:
                return ABRConstants.BLANK_STRING
                        + msgInstruction.getKey() + " Extract "
                        + ABRConstants.BLANK_STRING
                        + msgInstruction.getValue();

            default:
                return "No Action Detected for " + msgInstruction.getKey();
        }
    }

    public static Pair<String, String> insertRandomName(String key) {
        String randomName = generateRandomName();
        return new Pair<>(key, randomName);
    }

    public static String generateRandomName() {
        int length = RANDOM.nextInt(MAX_LENGTH - MIN_LENGTH + 1) + MIN_LENGTH;
        StringBuilder nameBuilder = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            char randomChar = CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length()));
            nameBuilder.append(randomChar);
        }

        return nameBuilder.toString();
    }

    public int[] addElementToArray(int[] refreshLoopArray, int newItem) {
        int[] extendedRefreshArray = new int[refreshLoopArray.length + 1];
        System.arraycopy(refreshLoopArray, 0, extendedRefreshArray, 0, refreshLoopArray.length);
        extendedRefreshArray[refreshLoopArray.length] = newItem;
        return extendedRefreshArray;
    }

    public String getXPathInstruction(BlockLoopInstructionLoadDTO currentInstruction, BlockLoadDTO blockLoad) {
        try {
            return blockLoad.getBlockLoopInstructionLoadDTOS().stream()
                    .filter(f -> f.getId().equals(currentInstruction.getParentId()))
                    .findFirst()
                    .get()
                    .getPath();
        } catch (Exception ex) {
            return null;
        }
    }

    public String getInstructionParentField(BlockLoopInstructionLoadDTO currentInstruction, BlockLoadDTO blockLoad) {
        try {
            return blockLoad.getBlockLoopInstructionLoadDTOS().stream()
                    .filter(f -> f.getId().equals(currentInstruction.getParentId()))
                    .findFirst()
                    .get()
                    .getName();
        } catch (Exception ex) {
            return null;
        }
    }

    // It Must be Greater than CurrentIndex
    // Ir Predicts if is going to have multiple ENSEIFs
    public int searchMapConditional(
            Map<String, List<Integer>> mapConditional,
            int parentBlockCondition,
            ABRConstants.ConditionStatus condition,
            int currentIndex,
            boolean showMessage) {

        // Construct the key pattern
        String keyPattern = parentBlockCondition + "-" + condition;

        // Iterate through the map entries
        for (Map.Entry<String, List<Integer>> entry : mapConditional.entrySet()) {
            String key = entry.getKey();
            List<Integer> indices = entry.getValue();

            // Check if the key matches the pattern
            if (key.startsWith(keyPattern)) {
                // Find the first index in the list that is greater than or equal to currentIndex
                for (int index : indices) {
                    if (index >= currentIndex) {
                        return index; // Return the matching index
                    }
                }
            }
        }

        if (showMessage) {
            // If no matching condition is found, show an error dialog
            performMessage.showCustomModalDialog(
                    "ERROR ON CONDITIONAL BLOCK",
                    String.format(
                            "Cannot find a matching condition for \"%s\" greater than the current index %d",
                            condition, currentIndex),
                    " Please click OK to continue!",
                    null,
                    null,
                    true,
                    null,
                    0);
        }

        return -1; // Return -1 if no valid index is found
    }

    public Map<String, List<Integer>> getConditionIndexMapByParentId(BlockLoadDTO blockLoad) {
        try {
            // Create a map where key is "parentId-actions" and value is a list of indices
            return IntStream.range(
                            0, blockLoad.getBlockLoopInstructionLoadDTOS().size())
                    .filter(index -> {
                        BlockLoopInstructionLoadDTO instruction =
                                blockLoad.getBlockLoopInstructionLoadDTOS().get(index);
                        String actions = instruction.getActions();
                        return actions != null
                                && (actions.equals("IF")
                                        || actions.equals("ELSEIF")
                                        || actions.equals("ELSE")
                                        || actions.equals("ENDIF"));
                    })
                    .boxed() // Convert IntStream to Stream<Integer>
                    .collect(Collectors.toMap(
                            index -> {
                                BlockLoopInstructionLoadDTO instruction = blockLoad
                                        .getBlockLoopInstructionLoadDTOS()
                                        .get(index);
                                return instruction.getParentId() + "-"
                                        + instruction.getActions(); // Key: parentId-actions
                            },
                            index -> {
                                List<Integer> indices = new ArrayList<>();
                                indices.add(index);
                                return indices;
                            }, // Value: list of indices
                            (existing, replacement) -> {
                                existing.addAll(replacement);
                                return existing;
                            } // Handle duplicates by merging lists
                            ));
        } catch (Exception ex) {
            // Return an empty map in case of an exception
            return Collections.emptyMap();
        }
    }

    public void createOutputHtml(String type, WebDriver driver) {
        // Save the HTML to a file
        List<WebElement> elements = driver.findElements(By.cssSelector(type));

        // Create a Set to store unique visible elements
        Set<String> uniqueElements = new HashSet<>();

        // Create a List to store the HTML content
        List<String> htmlArray = new ArrayList<>();

        // Iterate over all elements and add their outer HTML to the List
        for (WebElement element : elements) {
            if (isElementVisible(element, driver)) {
                String outerHTML = element.getAttribute("outerHTML");

                // Only add the element if it hasn't been added before
                if (uniqueElements.add(outerHTML)) {
                    htmlArray.add(outerHTML);
                }
            }
        }

        // Save the content as an array of strings to a new file
        String htmlPath = ABRPropertyManager.getInstance().getProperty(ABRPropertyEnum.FOLDER_PATH_EXPORT);
        try (FileWriter writer = new FileWriter(htmlPath + "/" + type + ".json")) {
            // Convert the list of strings to a JSON-like array format
            writer.write(htmlArray.stream()
                    .map(s -> "\"" + s.replace("\"", "\\\"") + "\"") // Escape double quotes
                    .collect(Collectors.joining(", ", "[", "]"))); // Format as JSON array
        } catch (IOException e) {
            System.out.println("Error writing to file: " + e.getMessage());
        } finally {
            // Close the browser if necessary
            // driver.quit();
        }
    }

    // Function to check if the element is visible
    private static boolean isElementVisible(WebElement element, WebDriver driver) {
        // Check if the element is displayed and within the viewport
        try {
            return element.isDisplayed() && isInViewport(element, driver);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    // Function to check if the element is within the viewport
    private static boolean isInViewport(WebElement element, WebDriver driver) {
        // Use JavaScript to check if the element is in the viewport
        // Use the WebDriver (which implements JavascriptExecutor) to execute JavaScript
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Execute the JavaScript to get the element's position and check if it's in the viewport
        return (boolean) js.executeScript(
                "var rect = arguments[0].getBoundingClientRect(); "
                        + "return (rect.top >= 0 && rect.left >= 0 && rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) && rect.right <= (window.innerWidth || document.documentElement.clientWidth));",
                element);
    }

    public void executeActionsAtInstructionCoordinates(
            BlockLoopInstructionLoadDTO currentInstruction, Pair<String, String> data) throws Exception {

        List<com.allinweb.ch.util.Priority> priorityList = ABRPriorities.getAllPriorityList();
        Optional<com.allinweb.ch.util.Priority> priority = priorityList.stream()
                .filter(p -> p.getPriorityType().equals(PriorityTypeEnum.coordinates))
                .findFirst();
        if (priority.isPresent()) {
            List<InstructionReferenceLoadDTO> instructionReferenceList =
                    currentInstruction.getInstructionReferenceLoadDTOList();
            Optional<InstructionReferenceLoadDTO> reference = instructionReferenceList.stream()
                    .filter(ref -> ref.getReferenceType().equals(priority.get().getName()))
                    .findFirst();
            int x = 0;
            int y = 0;
            int xCoord = 0;
            int yCoord = 0;
            if (reference.isPresent()) {
                String[] coordinates = reference.get().getValue().split(ABRConstants.FIELDS_SEPARATOR);

                double temp1 = Double.parseDouble(coordinates[0]);
                double temp2 = Double.parseDouble(coordinates[1]);
                x = (int) temp1;
                y = (int) temp2;
                int maxHeight =
                        abrWebDriver.getDriver().manage().window().getSize().getHeight();
                int maxWidth =
                        abrWebDriver.getDriver().manage().window().getSize().getWidth();
                int offsetY = y - maxHeight;
                int offsetX = x - maxWidth;
                xCoord = x > maxWidth ? x - offsetX : x;
                yCoord = y > maxHeight ? y - offsetY : y;
            }
            String[] actions = currentInstruction.getActions().split(ABRConstants.ACTIONS_AND_PATHS_SPLITTER);
            for (String action : actions) {
                switch (String.valueOf(action.charAt(0))) {
                    case ABRConstants.VISUALIZE:
                        scrollToCoordinates(x, y);
                        break;
                    case ABRConstants.CLICK:
                        scrollToCoordinates(x, y);
                        onHoldForSeconds(null);
                        clickAtCoordinates(xCoord, yCoord);
                        break;
                    case ABRConstants.INSERT:
                        scrollToCoordinates(x, y);
                        onHoldForSeconds(null);
                        clickAtCoordinates(xCoord, yCoord);
                        onHoldForSeconds(null);
                        typeCharacters(data);
                        break;
                }
                onHoldForSeconds(null);
            }
        }
    }

    public Map<String, String> calculateCoordinates(String savedCoordinates) {
        int x = 0;
        int y = 0;
        int xCoord = 0;
        int yCoord = 0;
        String[] coordinates = savedCoordinates.split(ABRConstants.FIELDS_SEPARATOR);
        double temp1 = Double.parseDouble(coordinates[0]);
        double temp2 = Double.parseDouble(coordinates[1]);
        x = (int) temp1;
        y = (int) temp2;
        int maxHeight = abrWebDriver.getDriver().manage().window().getSize().getHeight();
        int maxWidth = abrWebDriver.getDriver().manage().window().getSize().getWidth();
        int offsetY = y - maxHeight;
        int offsetX = x - maxWidth;
        xCoord = x > maxWidth ? x - offsetX : x;
        yCoord = y > maxHeight ? y - offsetY : y;

        Map<String, String> mapCoordinates = new HashMap<>();

        mapCoordinates.put("ScrollTo", x + ":" + y);
        mapCoordinates.put("ClickOn", xCoord + ":" + yCoord);
        return mapCoordinates;
    }

    public boolean executeActionsAtCoordinates(String savedCoordinates, Pair<String, String> data, String action) {

        int x = 0;
        int y = 0;
        int xCoord = 0;
        int yCoord = 0;
        try {
            String[] coordinates = savedCoordinates.split(ABRConstants.FIELDS_SEPARATOR);
            double temp1 = Double.parseDouble(coordinates[0]);
            double temp2 = Double.parseDouble(coordinates[1]);
            x = (int) temp1;
            y = (int) temp2;
            int maxHeight = abrWebDriver.getDriver().manage().window().getSize().getHeight();
            int maxWidth = abrWebDriver.getDriver().manage().window().getSize().getWidth();
            int offsetY = y - maxHeight;
            int offsetX = x - maxWidth;
            xCoord = x > maxWidth ? x - offsetX : x;
            yCoord = y > maxHeight ? y - offsetY : y;

            if (ABRConstants.VISUALIZE.equals(action)) {
                scrollToCoordinates(x, y);
            } else if (ABRConstants.CLICK.equals(action)) {
                scrollToCoordinates(x, y);
                //                circleAtCoordinates(x, y, abrWebDriver.getDriver());
                onHoldForSeconds(null);
                clickAtCoordinates(xCoord, yCoord);
            } else if (ABRConstants.INSERT.equals(action)) {
                scrollToCoordinates(x, y);
                //                sendInputJS(x, y, data.getValue(),abrWebDriver.getDriver());
                //                circleAtCoordinates(x, y, abrWebDriver.getDriver());
                onHoldForSeconds(null);
                clickAtCoordinates(xCoord, yCoord);
                onHoldForSeconds(null);
                typeCharacters(data);
            }
            onHoldForSeconds(null);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void scrollToCoordinates(int x, int y) {
        int maxHeight = abrWebDriver.getDriver().manage().window().getSize().getHeight();
        int maxWidth = abrWebDriver.getDriver().manage().window().getSize().getWidth();
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
            new WebDriverWait(abrWebDriver.getDriver(), Duration.ofSeconds(10))
                    .until((item) -> (Boolean) ((JavascriptExecutor) abrWebDriver.getDriver()).executeScript(script));
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
        new Actions(abrWebDriver.getDriver()).moveToLocation(x, y).click().perform();
    }

    private void circleAtCoordinates(int x, int y, WebDriver driver) {
        String script = "function createCircle(x, y, diameter) {\n"
                + "    const randomColor = Math.floor(Math.random()*16777215).toString(16);\n"
                + "\n"
                + "    return `\n"
                + "    <svg style='height:100%;width:100%;position:absolute;top:0;z-index:9999'><circle\n"
                + "        cx=\"${x}\"\n"
                + "      cy=\"${y}\"\n"
                + "      r=\"${diameter/2}\"\n"
                + "      fill=\"#${randomColor}\"\n"
                + "    ></circle></svg>\n"
                + "  `;\n"
                + "}\n"
                + "\n"
                + "function pri(ev){\n"
                + "    console.log(ev);\n"
                + "    document.body.innerHTML += createCircle(ev.pageX,ev.pageY,10);\n"
                + "}\n"
                + "\n"
                + "window.addEventListener(\"click\", pri);";
        ((JavascriptExecutor) driver).executeScript(script);
    }

    private void typeCharacters(Pair<String, String> fieldData) {
        new Actions(abrWebDriver.getDriver()).sendKeys(fieldData.getValue()).perform();
    }

    public String sequenceOfCommands(
            WebElement element,
            String typeCommand,
            String[] coordinates,
            Pair<String, String> fieldData,
            WebDriver driver) {

        String message = "Nothing to execute";
        try {
            if (typeCommand.equals(ABRConstants.SELECT)) {
                // Create a Select instance to interact with the dropdown
                message = "Select(element)";
                Select selectCountry = new Select(element);
                selectCountry.selectByVisibleText(fieldData.getValue());
            } else if (typeCommand.equals(ABRConstants.CLEAR)) {
                message = "clear()";
                element.clear();
            } else if (typeCommand.equals(ABRConstants.CLICK)) {
                message = "click()";
                element.click();
            } else if (typeCommand.equals(ABRConstants.INSERT)) {
                message = "sendKeys(\"" + fieldData.getValue() + "\")";
                element.sendKeys(fieldData.getValue());
            } else if (typeCommand.equals(ABRConstants.TAB)) {
                message = "(Keys.TAB)";
                element.sendKeys(Keys.TAB);
            } else if (typeCommand.equals(ABRConstants.GET_VALUE)) {
                message = "getText()";
                element.getText();
            } else if (typeCommand.equals(ABRConstants.FOCUS)) {
                message = "focusElement(element, driver)";
                focusElement(element, driver);
            } else if (typeCommand.equals(ABRConstants.COORD_VISUALIZA)) {
                message = "Coordinates COORD_VISUALIZA";
                executeActionsAtCoordinates(coordinates[1], fieldData, ABRConstants.VISUALIZE);
                executeActionsAtCoordinates(coordinates[0], fieldData, ABRConstants.VISUALIZE);
            } else if (typeCommand.equals(ABRConstants.COORD_CLICK)) {
                message = "Coordinates COORD_CLICK";
                executeActionsAtCoordinates(coordinates[1], fieldData, ABRConstants.CLICK);
                executeActionsAtCoordinates(coordinates[0], fieldData, ABRConstants.CLICK);
            } else if (typeCommand.equals(ABRConstants.COORD_INSERT)) {
                message = "Coordinates COORD_INSERT";
                executeActionsAtCoordinates(coordinates[1], fieldData, ABRConstants.INSERT);
                executeActionsAtCoordinates(coordinates[0], fieldData, ABRConstants.INSERT);
            }

            return "Success " + message;

        } catch (Exception ex) {
            return "Failed Attempt " + message;
        }
    }

    private void focusElement(WebElement element, WebDriver driver) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].focus();", element);

        Actions actions = new Actions(driver);
        actions.moveToElement(element).perform();
    }

    public void sendInputJS(int x, int y, String text, WebDriver driver) {
        String script = "function sendTextToElementAtCoordinates(x, y, text) {\n"
                + "    const element = document.elementFromPoint(x, y);\n"
                + "    if (element) {\n"
                + "        console.log('Found element:', element);\n"
                + "        element.click();\n"
                + "        if (element.tagName === 'INPUT' || element.tagName === 'TEXTAREA') {\n"
                + "            element.focus();\n"
                + "            element.value = text;\n"
                + "            const event = new Event('input', { bubbles: true });\n"
                + "            element.dispatchEvent(event);\n"
                + "        } else {\n"
                + "            console.warn('Element is not an input or textarea.');\n"
                + "        }\n"
                + "    } else {\n"
                + "        console.warn('No element found at the given coordinates.');\n"
                + "    }\n"
                + "}\n"
                + "\n"
                + "sendTextToElementAtCoordinates(arguments[0], arguments[1], arguments[2]);";

        // Execute the JavaScript with the provided x, y, and text arguments
        ((JavascriptExecutor) driver).executeScript(script, x, y, text);
    }

    public void moveAndClickAtCoordinates(String savedCoordinates, WebDriver driver) {
        String[] coordinates = savedCoordinates.split(ABRConstants.FIELDS_SEPARATOR);
        double temp1 = Double.parseDouble(coordinates[0]);
        double temp2 = Double.parseDouble(coordinates[1]);
        int x = (int) temp1;
        int y = (int) temp2;

        String script = "function moveAndClickMouse(x, y) {\n" + "    const mouseDiv = document.createElement('div');\n"
                + "    mouseDiv.style.position = 'absolute';\n"
                + "    mouseDiv.style.width = '10px';\n"
                + "    mouseDiv.style.height = '10px';\n"
                + "    mouseDiv.style.backgroundColor = 'red';\n"
                + "    mouseDiv.style.borderRadius = '50%';\n"
                + "    mouseDiv.style.zIndex = '10000';\n"
                + "    mouseDiv.style.pointerEvents = 'none';\n"
                + "    mouseDiv.id = 'virtualMouse';\n"
                + "    document.body.appendChild(mouseDiv);\n"
                + "\n"
                + "    function blinkMouse() {\n"
                + "        const mouse = document.getElementById('virtualMouse');\n"
                + "        if (mouse) {\n"
                + "            mouse.style.visibility = mouse.style.visibility === 'hidden' ? 'visible' : 'hidden';\n"
                + "        }\n"
                + "    }\n"
                + "\n"
                + "    const blinkInterval = setInterval(blinkMouse, 500);\n"
                + "\n"
                + "    mouseDiv.style.left = `${x}px`;\n"
                + "    mouseDiv.style.top = `${y}px`;\n"
                + "\n"
                + "    const element = document.elementFromPoint(x, y);\n"
                + "    if (element) {\n"
                + "        element.click();\n"
                + "    }\n"
                + "\n"
                + "    setTimeout(() => {\n"
                + "        clearInterval(blinkInterval);\n"
                + "        const mouse = document.getElementById('virtualMouse');\n"
                + "        if (mouse) {\n"
                + "            mouse.remove();\n"
                + "        }\n"
                + "    }, 3000);\n"
                + "}\n"
                + "\n"
                + "moveAndClickMouse(arguments[0], arguments[1]);";

        ((JavascriptExecutor) driver).executeScript(script, x, y);
    }

    public Pair<String, String> getBlockDetailsById(
            List<BlockLoadDTO> blocksLoaded, BlockLoopInstructionLoadDTO currentInstruction) {
        for (BlockLoadDTO block : blocksLoaded) {
            if (block.getId() != null && block.getId().equals(currentInstruction.getParentId())) {
                Pair<String, String> blockDetails = new Pair<>(
                        currentInstruction.getId() + ":" + block.getId() + ":" + block.getBlockOrderNumber() + ":"
                                + block.getName().trim(),
                        currentInstruction.getOperation());
                return blockDetails;
            }
        }
        return null; // or throw an exception if the block is not found
    }

    public Pair<String, String> getInstructionDetailsById(
            List<BlockLoopInstructionLoadDTO> blockLoopInstructionLoadDTOS,
            BlockLoopInstructionLoadDTO currentInstruction) {
        for (BlockLoopInstructionLoadDTO instParent : blockLoopInstructionLoadDTOS) {
            if (instParent.getId() != null && instParent.getId().equals(currentInstruction.getParentId())) {
                Pair<String, String> blockDetails = new Pair<>(
                        currentInstruction.getId() + ":" + instParent.getId() + ":"
                                + instParent.getName().trim(),
                        currentInstruction.getOperation());
                return blockDetails;
            }
        }
        return null; // or throw an exception if the block is not found
    }

    public Map<String, Integer[]> getLoopAndRefreshLoops(
            List<BlockLoopInstructionLoadDTO> blockLoopInstructionLoadDTOS) {
        // Step 2: Filter rows where actions = "REFRESH_LOOP" or "LOOP" and collect into the map
        Map<String, Integer[]> mapRefreshLoops = new HashMap<>();

        for (BlockLoopInstructionLoadDTO instruction : blockLoopInstructionLoadDTOS) {
            // Filter by actions
            String actions = instruction.getActions();
            if ("REFRESH_LOOP".equalsIgnoreCase(actions) || "LOOP".equalsIgnoreCase(actions)) {
                // Convert id to String for the key
                String key = String.valueOf(instruction.getId());

                // Parse the operation into Integer[]
                String operation = instruction.getOperation();
                Integer[] operationValues;
                if (operation == null || operation.isEmpty()) {
                    operationValues = new Integer[] {}; // Handle null/empty operation
                } else {
                    String[] parts = operation.split(":"); // Split by ':'
                    operationValues = new Integer[parts.length];
                    for (int i = 0; i < parts.length; i++) {
                        operationValues[i] = Integer.parseInt(parts[i]); // Convert each part to Integer
                    }
                }

                // Add to the map
                mapRefreshLoops.put(key, operationValues);
            }
        }

        // Traverse and print keys and values
        for (Map.Entry<String, Integer[]> entry : mapRefreshLoops.entrySet()) {
            String key = entry.getKey(); // The key
            Integer[] values = entry.getValue(); // The value as an array

            // Convert the Integer[] to a readable string
            String valuesAsString = Arrays.stream(values)
                    .map(String::valueOf) // Convert each Integer to String
                    .collect(Collectors.joining(":")); // Join with ':'

            // Print the key and value
            System.out.println("Key: " + key + ", Value: " + valuesAsString);
        }

        return mapRefreshLoops;
    }

    public Set<Integer> getParentIdsForLoop(List<BlockLoopInstructionLoadDTO> blockLoopInstructionLoadDTOS) {
        return blockLoopInstructionLoadDTOS.stream()
                .filter(instruction -> "REFRESH_LOOP".equalsIgnoreCase(instruction.getActions())
                        || "LOOP".equalsIgnoreCase(instruction.getActions()))
                .map(BlockLoopInstructionLoadDTO::getParentId)
                .collect(Collectors.toSet());
    }

    public void logAndReport(
            ABRConstants.ConditionStatus currentCondition,
            boolean excelReport,
            boolean logOperation,
            long blockStartTime,
            String blockReportName,
            boolean success,
            String[] action,
            Pair<String, String> msgBlock,
            Map<String, String> dataExcel,
            ExcelWriter.ExcelChain writerReport,
            String mainMsg,
            String bodyLog) {
        long duration = duration(blockStartTime);

        if (excelReport) {
            excelReportWrite(
                    currentCondition, blockReportName, success, action, msgBlock, duration, dataExcel, writerReport);
            totalExecutionTime += duration;
        }
        if (logOperation) {

            operationLog(success, mainMsg, bodyLog, duration);
        }

        totalExecutionTime += duration;
    }

    public ABRConstants.ConditionStatus updateProgressSuccess(
            boolean success, ABRConstants.ConditionStatus currentCondition) {
        // It Gets last Progress Status
        // Machine State
        if (currentCondition.equals(ABRConstants.ConditionStatus.IF)) {
            return success ? ABRConstants.ConditionStatus.IF_PASSED : ABRConstants.ConditionStatus.IF_FAILED;
        } else if (currentCondition.equals(ABRConstants.ConditionStatus.ELSEIF)) {
            return success ? ABRConstants.ConditionStatus.ELSEIF_PASSED : ABRConstants.ConditionStatus.ELSEIF_FAILED;
        } else if (currentCondition.equals(ABRConstants.ConditionStatus.ELSE)) {
            return success ? ABRConstants.ConditionStatus.ELSE_PASSED : ABRConstants.ConditionStatus.ELSE_FAILED;
        } else if (currentCondition.equals(ABRConstants.ConditionStatus.ENDIF)) {
            return ABRConstants.ConditionStatus.NONE;
        }
        return ABRConstants.ConditionStatus.NONE;
    }

    public int checkActionToJump(
            String action,
            ABRConstants.ConditionStatus progressCondition,
            Map<String, List<Integer>> mapConditional,
            int parentBlockCondition,
            int currentIndex) {
        if (action.equalsIgnoreCase(ABRConstants.ELSEIF)) {
            // Goes to the ENDIF (ENDIF index + 1);
            return searchMapConditional(
                    mapConditional, parentBlockCondition, ABRConstants.ConditionStatus.ENDIF, currentIndex, true);

        } else if (action.equalsIgnoreCase(ABRConstants.ELSE)) {
            // Goes to the ENDIF (ENDIF index + 1);
            return searchMapConditional(
                    mapConditional, parentBlockCondition, ABRConstants.ConditionStatus.ENDIF, currentIndex, true);

        } else if (action.equalsIgnoreCase(ABRConstants.ELSE)) {
            // Goes to the ENDIF (ENDIF index + 1);
            return searchMapConditional(
                    mapConditional, parentBlockCondition, ABRConstants.ConditionStatus.ENDIF, currentIndex, true);
        }
        return 0;
    }
}
