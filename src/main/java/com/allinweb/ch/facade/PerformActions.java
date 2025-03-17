package com.allinweb.ch.facade;

import com.allinweb.ch.builder.WebElementAttributeEnum;
import com.allinweb.ch.builder.WebElementAttributeTypeValueEnum;
import com.allinweb.ch.builder.WebElementIcon;
import com.allinweb.ch.builder.WebElementTagNameEnum;
import com.allinweb.ch.component.model.BlockLoadDTO;
import com.allinweb.ch.component.model.ComplexInstructionLoadDTO;
import com.allinweb.ch.component.model.ElementDTO;
import com.allinweb.ch.component.model.InstructionLoadDTO;
import com.allinweb.ch.component.model.InstructionReferenceLoadDTO;
import com.allinweb.ch.driver.ARWebDriver;
import com.allinweb.ch.persistence.TargetElement;
import com.allinweb.ch.readersAndWriters.ExcelWriter;
import com.allinweb.ch.util.ARConstants;
import com.allinweb.ch.util.ARLogger;
import com.allinweb.ch.util.ARPriorities;
import com.allinweb.ch.util.ARPropertyEnum;
import com.allinweb.ch.util.ARPropertyManager;
import com.allinweb.ch.util.ARWebUtil;
import com.allinweb.ch.util.CryptationAlgorithm;
import com.allinweb.ch.util.ExcelReportStatusEnum;
import com.allinweb.ch.util.PriorityTypeEnum;
import com.allinweb.ch.util.UtilsMethods;
import com.google.common.base.Strings;
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
    private static final IframeInputLocator iframeInputLocator;

    static {
        performMessage = PerformMessage.getInstance();
        performDataBase = PerformDataBase.getInstance();
        iframeInputLocator = IframeInputLocator.getInstance();
    }

    long totalExecutionTime = 0;

    public List<String> windowHandlesList = new ArrayList<>();

    private ARPriorities arPriorities;
    private ARWebDriver arWebDriver;
    private Map<WebElement, List<WebElement>> iframeElementsMap;
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

    public void initializePerformActions(ARPriorities arPriorities, ARWebDriver arWebDriver) {
        this.arPriorities = arPriorities;
        this.arWebDriver = arWebDriver;
    }

    // Public method to access the singleton instance
    public static PerformActions getInstance() {
        return instance.get();
    }

    public WebElement searchElement(InstructionLoadDTO instruction, int botJobId) {
        WebElement instructionElement = null;

        if (!StringUtils.isBlank(instruction.getXpath())) {
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
            InstructionLoadDTO currentInstruction,
            Map<String, String> mapOperators,
            WebElement instructionElement,
            String actions[])
            throws Exception {

        WebDriver originalDriver = arWebDriver.getDriver(); // Save the original WebDriver state
        boolean switchedToIframe = false;

        try {
            String xPath = currentInstruction.getXpath().toLowerCase();
            if (currentInstruction.getXpath() != null && xPath.contains("iframe")) {
                // Locate and switch to the iframe
                WebElement iframeElement = arWebDriver.getDriver().findElement(By.xpath(xPath));
                WebDriver driver = arWebDriver.getDriver().switchTo().frame(iframeElement);
                arWebDriver.setDriver(driver);
                switchedToIframe = true;
            }

            Boolean pressEnterAfter = false;
            if (actions[0].equals(ARConstants.INSERT) && actions[1].equals(ARConstants.ENTER)) {
                pressEnterAfter = true;
            }

            if (instructionElement != null) {
                boolean passed = true;
                switch (actions[0]) {
                    case ARConstants.VISUALIZE:
                        scrollIntoView(instructionElement);
                        passed = scrollToElement(byPassNotFound, instructionElement);

                        if (!passed) {
                            // Try by coordinates
                            Pair<String, String> filedData = new Pair("&EMPTY", "&EMPTY");
                            passed = executeActionsAtCoordinates(
                                    savedCoordinates, filedData, ARConstants.VISUALIZE, pressEnterAfter);
                        }
                        return passed;
                    case ARConstants.OUTPUT:
                        String fieldName = currentInstruction.getId() + "-" + currentInstruction.getName();
                        return getOutPutElement(
                                byPassNotFound,
                                instructionElement,
                                fieldName,
                                currentInstruction.getActions(),
                                mapOperators);
                    case ARConstants.CLICK:
                    case ARConstants.OTHER:
                        passed = clickElement(byPassNotFound, instructionElement);
                        if (!passed) {
                            // Try by coordinates
                            Pair<String, String> filedData = new Pair("&EMPTY", "&EMPTY");
                            passed = executeActionsAtCoordinates(
                                    savedCoordinates, filedData, ARConstants.CLICK, pressEnterAfter);
                        }
                        return passed;
                    case ARConstants.INSERT:
                        if ("select".equalsIgnoreCase(instructionElement.getTagName())) {
                            passed = insertDataInSelectElement(
                                    byPassNotFound, instructionElement, savedCoordinates, data, pressEnterAfter);

                            if (!passed) {
                                // Try by coordinates
                                passed = executeActionsAtCoordinates(
                                        savedCoordinates, data, ARConstants.SELECT, pressEnterAfter);
                            }
                            return passed;
                        } else {

                            passed = insertInElement(
                                    byPassNotFound,
                                    instructionElement,
                                    data.getValue(),
                                    currentInstruction.getDefaultValue(),
                                    currentInstruction.getCodified(),
                                    pressEnterAfter);

                            if (!passed) {
                                // Try by coordinates
                                passed = executeActionsAtCoordinates(
                                        savedCoordinates, data, ARConstants.INSERT, pressEnterAfter);
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
                arWebDriver.setDriver(originalDriver);
            }
        }
    }

    public void performOtherActions(boolean byPassNotFound, InstructionLoadDTO instruction, String actions[])
            throws Exception {

        switch (actions[0]) {
            case ARConstants.LIST_OPERATION:
                listOperation(byPassNotFound, instruction);
                break;
            case ARConstants.HOLD:
            case ARConstants.REFRESH_HOLD:
                //                        executeAlert(instruction);
                onHoldForSeconds(instruction);
                break;
            case ARConstants.REFRESH_ONLY:
            case ARConstants.REFRESH_LOOP:
                refreshPage();
                break;
            case ARConstants.QUIT:
                quit(1);
                break;
            case ARConstants.SCREEN:
                break;
        }

        onHoldForSeconds(null);
    }

    public String performOperatorActions(
            boolean byPassNotFound,
            InstructionLoadDTO instruction,
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
            ARLogger.getInstance(PerformActions.class)
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
                List<WebElement> foundElementList = arWebDriver.getDriver().findElements(criteria);

                if (foundElementList != null && foundElementList.size() > 0) {
                    if (justCalledRefreshPage) {
                        justCalledRefreshPage = false;
                        try {
                            waitForPage.until(ExpectedConditions.visibilityOfElementLocated(criteria));
                        } catch (Exception e) {
                            ARLogger.getInstance(PerformActions.class)
                                    .fine(String.format(
                                            "Could Not Find the element by xPath \"%s\" Criteria \"%s\" -> Cause: %s",
                                            targetXPath, criteria, e.getMessage()));

                            showNotFoundElement(targetXPath, criteria);

                            //                                SwingUtilities.invokeLater(() ->

                            if (!byPassNotFound) {
                                //
                                // performMessage.couldNotFindElement(String.valueOf(criteria));
                                performMessage.couldNotInputBotJobVeryFast("Could Not Find the element by xPath");
                            }
                        }
                    } else if (actionCustomMaxWaitSec != null) {
                        try {
                            new WebDriverWait(arWebDriver.getDriver(), Duration.ofSeconds(actionCustomMaxWaitSec))
                                    .until(ExpectedConditions.presenceOfElementLocated(criteria));
                        } catch (Exception e) {
                            ARLogger.getInstance(PerformActions.class)
                                    .fine(String.format(
                                            "Could Not Find the element by xPath \"%s\" Criteria \"%s\" -> Cause: %s",
                                            targetXPath, criteria, e.getMessage()));
                            if (!byPassNotFound) {
                                //
                                // performMessage.couldNotFindElement(String.valueOf(criteria));
                                performMessage.couldNotInputBotJobVeryFast("Could Not Find the element by xPath");
                            }
                        }
                    } else {
                        try {
                            waitForAction.until(ExpectedConditions.visibilityOfElementLocated(criteria));
                        } catch (Exception e) {
                            ARLogger.getInstance(PerformActions.class)
                                    .fine(String.format(
                                            "Could Not Find hte element by xPath \"%s\" Criteria \"%s\" -> Cause: %s",
                                            targetXPath, criteria, e.getMessage()));

                            if (!byPassNotFound) {
                                //
                                // performMessage.couldNotFindElement(String.valueOf(criteria));
                                performMessage.couldNotInputBotJobVeryFast("Could Not Find the element by xPath");
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

    //    private void callErrorMessageNotEnabled(String criteria) {
    //        performMessage.showCustomModalDialogDragWin11(
    //                String.format("The Element \"%s\" is not Enabled", criteria),
    //                "1. Consider Fill Up all the Mandatory Fields",
    //                null,
    //                null,
    //                null,
    //                true,
    //                "Continue",
    //                "Stop All",
    //                0);
    //    }

    private void showNotFoundElement(String targetXPath, By criteria) {}

    private WebElement locateElementOLD(InstructionLoadDTO currentInstruction, int botJobId) {

        //        WebElement elementInsideIframe = null;
        //                if (xPath.toLowerCase().contains("iframe")){
        //                    // Switch to the iframe using ID or name
        //        //            arWebDriver.getDriver().switchTo().frame("iframeID");
        //
        //                    // Alternatively, switch to the iframe using a WebElement
        //        //            WebElement iframeElement =
        //         arWebDriver.getDriver().findElement(By.xpath("//iframe[@name='iframeName']"));
        //                    WebElement iframeElement = arWebDriver.getDriver().findElement(By.xpath(xPath));
        //                    arWebDriver.getDriver().switchTo().frame(iframeElement);
        //                    // Now, interact with elements inside the iframe
        //                    elementInsideIframe = arWebDriver.getDriver().findElement(By.id("elementID"));
        //                }
        //
        //                if (elementInsideIframe != null) {
        //                    element = elementInsideIframe;
        //                }
        //
        //                if (elementInsideIframe != null) {
        //                    // Switch back to the main page
        //                    arWebDriver.getDriver().switchTo().defaultContent();
        //                }

        String instructionPath = currentInstruction.getXpath();
        String tagName = null;
        try {
            tagName = removeTrailingSlash(instructionPath);
            tagName = extractTagName(instructionPath);
        } catch (Exception e) {
            ARLogger.getInstance(PerformActions.class)
                    .fine(String.format(
                            "Error RemoveTrailingSlash for %s -> xPath  %s -> Cause: %s",
                            tagName, instructionPath, e.getMessage()));
        }
        List<InstructionReferenceLoadDTO> instructionReferenceList =
                currentInstruction.getInstructionReferenceLoadDTOList();

        if (instructionReferenceList.size() == 0) {
            ARLogger.getInstance(PerformActions.class)
                    .warning("####    Not XPath to Be Located!   ####"
                            + "\n####    Remove and Re-Scan the Failed Field Again   ####");

            return null;
        }

        waitPage();

        // If Not Loaded get if the JobId Changed
        if (arPriorities.getJobId() == null) {
            arPriorities.setJobId(botJobId);
            if (currentInstruction.getPriority() != null) {
                arPriorities.loadPrioritiesFromString(currentInstruction.getPriority());
            } else {
                arPriorities.loadPriorities();
            }
        } else if (arPriorities.getJobId() != botJobId) {
            arPriorities.setJobId(botJobId);
            if (currentInstruction.getPriority() != null) {
                arPriorities.loadPrioritiesFromString(currentInstruction.getPriority());
            } else {
                arPriorities.loadPriorities();
            }
        }

        if (arPriorities.getAllPriorityList().size() < 4) {}

        List<com.allinweb.ch.util.Priority> priorityList = arPriorities.getAllPriorityList();
        if (arPriorities.getAllPriorityList().size() > 0) {

            //            if (instruction.getActionCustomMaxWaitSec() > 5) {
            //                instruction.setActionCustomMaxWaitSec(5);
            //            }
            WebElement elementFound = null;
            //            for (int i = 0; i < priorityList.size() && elementFound == null; i++) {
            for (com.allinweb.ch.util.Priority priority : arPriorities.getAllPriorityList()) {
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

                    ARLogger.getInstance(PerformActions.class)
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

                    if (arWebDriver.getDriver() == null) {
                        //                        showAlert(
                        //                                Alert.AlertType.ERROR,
                        //                                "AR Web Driver is NULL",
                        //                                "Restart the APP",
                        //                                "Close all Browser attached or Restart the APP");

                        String msg1 = "AR Web Driver is NULL";
                        String msg2 = "Restart the APP";
                        String msg3 = "Close all Browser or Restart the APP";

                        performMessage.errorMessage("Parent Id Error", msg1, msg2, msg3, null, 0);

                        return null;
                    }

                    ARLogger.getInstance(PerformActions.class).fine("WebDriver Session ID: " + getSessionId());

                    // Actualy here is Calling the Actions
                    if (criterias != null) {

                        for (By criteria : criterias) {
                            List<WebElement> foundElementList =
                                    arWebDriver.getDriver().findElements(criteria);

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
                                        ARLogger.getInstance(PerformActions.class)
                                                .fine(String.format(
                                                        "Could Not Find xPath \"%s\" Criteria \"%s\" -> Cause: %s",
                                                        instructionPath, criteria, e.getMessage()));

                                        //
                                        // performMessage.couldNotFindElement(String.valueOf(criteria));
                                    }
                                } else if (currentInstruction.getActionCustomMaxWaitSec() != null) {
                                    try {

                                        new WebDriverWait(
                                                        arWebDriver.getDriver(),
                                                        Duration.ofSeconds(
                                                                currentInstruction.getActionCustomMaxWaitSec()))
                                                .until(ExpectedConditions.presenceOfElementLocated(criteria));
                                    } catch (Exception e) {
                                        ARLogger.getInstance(PerformActions.class)
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
                                        ARLogger.getInstance(PerformActions.class)
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

    private WebElement locateElement(InstructionLoadDTO currentInstruction, int botJobId) {
        String instructionPath = currentInstruction.getXpath();
        String tagName = null;

        arWebDriver.getDriver().switchTo().defaultContent();

        try {
            tagName = removeTrailingSlash(instructionPath);
            tagName = extractTagName(instructionPath);
        } catch (Exception e) {
            ARLogger.getInstance(PerformActions.class)
                    .fine(String.format(
                            "Error RemoveTrailingSlash for %s -> xPath  %s -> Cause: %s",
                            tagName, instructionPath, e.getMessage()));
        }

        List<InstructionReferenceLoadDTO> instructionReferenceList =
                currentInstruction.getInstructionReferenceLoadDTOList();

        if (instructionReferenceList.size() == 0) {
            ARLogger.getInstance(PerformActions.class)
                    .warning("####    Not XPath to Be Located!   ####"
                            + "\n####    Remove and Re-Scan the Failed Field Again   ####");

            return null;
        }

        waitPage();

        if (arPriorities.getJobId() == null) {
            arPriorities.setJobId(botJobId);
            if (currentInstruction.getPriority() != null) {
                arPriorities.loadPrioritiesFromString(currentInstruction.getPriority());
            } else {
                arPriorities.loadPriorities();
            }
        } else if (arPriorities.getJobId() != botJobId) {
            arPriorities.setJobId(botJobId);
            if (currentInstruction.getPriority() != null) {
                arPriorities.loadPrioritiesFromString(currentInstruction.getPriority());
            } else {
                arPriorities.loadPriorities();
            }
        }

        if (arPriorities.getAllPriorityList().size() == 0
                || arPriorities.getAllPriorityList().size() < 4) {
            StringBuilder priorMissing = new StringBuilder();
            priorMissing.append("1,xpath,currentXPath" + System.lineSeparator());
            priorMissing.append("2,attributeID,attributeID" + System.lineSeparator());
            priorMissing.append("3,attributeName,attributeName" + System.lineSeparator());
            priorMissing.append("4,searchAttribute,searchAttribute" + System.lineSeparator());
            priorMissing.append("5,coordinates,coordinates" + System.lineSeparator());
            priorMissing.append("6,attribute,test-id" + System.lineSeparator());
            //            priorMissing.append("7,attributes,allAttributes" + System.lineSeparator());
            arPriorities.loadPrioritiesFromString(priorMissing.toString());
        }

        WebElement elementFound = null;
        WebElement iframeElement = null;

        if (!Strings.isNullOrEmpty(currentInstruction.getIFrameXPath())) {
            try {
                // Locate and switch to the iframe first
                WebElement iframe = arWebDriver.getDriver().findElement(By.xpath(currentInstruction.getIFrameXPath()));
                arWebDriver.getDriver().switchTo().frame(iframe);

                System.out.println("Found iFrame XPath: " + currentInstruction.getIFrameXPath());
            } catch (Exception e) {
                System.out.println("iFrame Not Found with XPath: " + currentInstruction.getIFrameXPath());
                //                performMessage.generalErrorIFrame(currentInstruction.getName());
                return null;
            }
        }

        int attempts = 0;
        while (elementFound == null && attempts < 5) {

            for (com.allinweb.ch.util.Priority priority : arPriorities.getAllPriorityList()) {
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

                Optional<InstructionReferenceLoadDTO> instructionReference = instructionReferenceList.stream()
                        .filter(reference -> priority.getName().stream()
                                .anyMatch(p -> p.equalsIgnoreCase(reference.getReferenceType())))
                        .findFirst();

                if (instructionReference.isPresent()) {
                    ARLogger.getInstance(PerformActions.class)
                            .fine(String.format(
                                    "Search for %s   Type:  %s   Value: %s",
                                    priority.getName(),
                                    instructionReference.get().getReferenceType(),
                                    instructionReference.get().getValue()));

                    List<By> criterias = null;

                    boolean isAttributeID = false;
                    boolean isAttributeName = false;
                    boolean isSearchAttribute = false;
                    String searchAttributeValue = "";

                    // Handle different priority types (like XPath, attribute, etc.)
                    switch (priority.getPriorityType()) {
                        case xpath -> criterias = Arrays.asList(
                                By.xpath(instructionReference.get().getValue()));

                        case attributeID -> {
                            isAttributeID = true;
                            searchAttributeValue = instructionReference.get().getValue();
                            criterias = convertToCriteriaList(
                                    tagName,
                                    priority.getName(),
                                    instructionReference.get().getValue());
                        }
                        case attributeName -> {
                            isAttributeName = true;
                            searchAttributeValue = instructionReference.get().getValue();
                            criterias = convertToCriteriaList(
                                    tagName,
                                    priority.getName(),
                                    instructionReference.get().getValue());
                        }
                        case searchAttribute -> {
                            isSearchAttribute = true;
                            searchAttributeValue = instructionReference.get().getValue();
                            String[] parts = searchAttributeValue.split("=");
                            criterias = convertToCriteriaList(tagName, List.of(parts[0]), parts[1]);
                        }
                        case attribute -> criterias = convertToCriteriaList(
                                tagName,
                                priority.getName(),
                                instructionReference.get().getValue());
                        case coordinates, allAttributes -> {
                            // These cases are placeholders and do not need additional handling
                        }

                        case ExecuteScript, createXPath, dynamic, jsoup -> {
                            // Handle the special cases (implement if needed)
                        }

                        case ById, ByClassName, ByName, ByTagName, ByLinkText, ByPartialLinkText, ByCssSelector -> {
                            // These cases can be handled if needed, otherwise leave them empty
                        }
                    }

                    if (criterias != null) {
                        for (By criteria : criterias) {

                            List<WebElement> foundElementList = new ArrayList<>();
                            try {
                                foundElementList = arWebDriver.getDriver().findElements(criteria);
                            } catch (Exception ignore) {

                            }

                            if (foundElementList.size() == 0) {
                                if (isAttributeID) {
                                    WebElement element = findElementByID(arWebDriver.getDriver(), searchAttributeValue);
                                    if (element != null) {
                                        foundElementList.add(element);
                                    }
                                } else if (isAttributeName) {
                                    WebElement element =
                                            findElementsByName(arWebDriver.getDriver(), searchAttributeValue);
                                    if (element != null) {
                                        foundElementList.add(element);
                                    }
                                } else if (isSearchAttribute) {
                                    String[] parts = searchAttributeValue.split("=");
                                    WebElement element =
                                            findElementByAttributeParams(arWebDriver.getDriver(), parts[0], parts[1]);
                                    if (element != null) {
                                        foundElementList.add(element);
                                    }
                                }
                            }

                            if (foundElementList != null && foundElementList.size() > 0 && iframeElement == null) {
                                // Wait for element visibility and process
                                //                                try {
                                //
                                // waitForAction.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(criteria));
                                //
                                // waitForPage.until(ExpectedConditions.visibilityOfElementLocated(criteria));
                                //                                    scrollToElement(currentInstruction.getXpath());
                                //                                } catch (Exception e) {
                                //                                    ARLogger.getInstance(PerformActions.class)
                                //                                            .fine(String.format(
                                //                                                    "Could Not Find xPath \"%s\"
                                // Criteria \"%s\" -> Cause: %s",
                                //                                                    instructionPath, criteria,
                                // e.getMessage()));
                                //                                }

                                // If multiple elements found, verify each
                                if (foundElementList.size() > 1) {
                                    int k = 0;
                                    while (elementFound == null && k < foundElementList.size()) {
                                        String xpath = ARWebUtil.extractXPath(
                                                foundElementList.get(k).toString());

                                        // Second verification for XPath found
                                        if (xpath.equals(
                                                instructionReference.get().getValue())) {
                                            elementFound = foundElementList.get(k);
                                            break;
                                        }
                                        k++;
                                    }
                                } else {
                                    elementFound = foundElementList.get(0);
                                }
                            } else {
                                elementFound = iframeElement;
                            }

                            // Switch back to main content after interacting with iframe (if applicable)
                            if (instructionPath.contains("iframe")) {
                                arWebDriver.getDriver().switchTo().defaultContent();
                            }
                        }
                    }
                }
            }
            attempts++;
            if (elementFound == null) {
                try {
                    onHoldInSeconds(5);
                    ARLogger.getInstance(PerformActions.class)
                            .fine(String.format(
                                    "Re-try %d Locate Web Element TagName \"%s\"",
                                    attempts, currentInstruction.getName()));

                } catch (Exception e) {
                }
            }
        }

        return elementFound;
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

            if (priority.equalsIgnoreCase("attributeID")) {
                priority = "id";
            } else if (priority.equalsIgnoreCase("attributeName")) {
                priority = "name";
            }
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
            scrollIntoView(element);
            waitForAction.until(ExpectedConditions.visibilityOf(element));
        } catch (Exception e) {
            ARLogger.getInstance(PerformActions.class)
                    .fine(String.format(
                            "Could Not Find Field Name \"%s\" Value \"%s\" -> Cause: %s",
                            fieldName, dataFieldValue, e.getMessage()));

            if (!byPassNotFound) {
                //                performMessage.couldNotFindElement(fieldName);
                performMessage.couldNotInputBotJobVeryFast("Could Not Find TagName " + element.getTagName());
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
            scrollIntoView(element);
            waitForAction.until(ExpectedConditions.visibilityOf(element));
        } catch (Exception e) {
            ARLogger.getInstance(PerformActions.class)
                    .fine(String.format(
                            "Could Not Find TagName \"%s\" -> Cause: %s", element.getTagName(), e.getMessage()));

            if (!byPassNotFound) {
                //                performMessage.couldNotFindElement(element.getTagName());
                performMessage.couldNotInputBotJobVeryFast("Could Not Find TagName " + element.getTagName());
            }
        }

        // Assuming instructionElement is an input field
        return element.getAttribute("value");
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
        WebDriver driver = arWebDriver.getDriver();
        if (driver != null) {
            try {

                waitForPage.until(d -> ((JavascriptExecutor) driver)
                        .executeScript("return document.readyState")
                        .equals("complete"));
            } catch (Exception ex) {
                ARLogger.getInstance(PerformActions.class)
                        .warning(String.format(
                                "WaitForPage.until(d -> ((JavascriptExecutor) driver) error: %s", ex.getMessage()));

                //                performMessage.couldNotFindElement("WaitForPage.until");
                performMessage.couldNotInputBotJobVeryFast("WaitForPage.until ");
            }
        } else {
            // Handle the case when driver is null (e.g., throw an exception or initialize the driver)
            ARLogger.getInstance(PerformActions.class)
                    .warning("WaitForPage.until(d -> ((JavascriptExecutor) driver) is returning nulls");
        }
    }

    public boolean scrollToElement(boolean byPassNotFound, WebElement element) throws Exception {
        try {
            UtilsMethods.exceptionIfNullWebElement(element);
            ((JavascriptExecutor) arWebDriver.getDriver()).executeScript("arguments[0].scrollIntoView(true);", element);
            return true;
        } catch (Exception e) {
            ARLogger.getInstance(PerformActions.class)
                    .severe(String.format(
                            "Failed to Scroll to Element \"%s\" -> Cause: %s", element.getTagName(), e.getMessage()));
            //            if (!byPassNotFound) {
            //                //                performMessage.couldNotFindElement("Failed to Scroll to Element " +
            //                // element.getTagName());
            //                performMessage.couldNotInputBotJobVeryFast("Failed to Scroll to Element " +
            // element.getTagName());
            //            }
            return false;
        }
    }

    public boolean clickElement(boolean byPassNotFound, WebElement element) throws Exception {
        UtilsMethods.exceptionIfNullWebElement(element);
        if (!element.isEnabled()) {
            //        callErrorMessageNotEnabled(element.getTagName());
            performMessage.showCustomModalDialogDragWin11(
                    "BOT JOB STOP",
                    String.format("The Element \"%s\" is not Enabled", element.getTagName()),
                    "Consider Fill Up all the Mandatory Fields!",
                    null,
                    null,
                    true,
                    "OK",
                    null,
                    0);

            // throw new TimeoutException();
            return false;
        }

        try {
            //            scrollIntoView(element);
            //            waitForAction.until(ExpectedConditions.visibilityOf(element));
            waitForAction.until(ExpectedConditions.visibilityOf(element).andThen(e -> {
                ((JavascriptExecutor) arWebDriver.getDriver())
                        .executeScript("arguments[0].scrollIntoView(true);", element);
                return waitForAction.until(ExpectedConditions.elementToBeClickable(element));
            }));
        } catch (Exception e) {
            ARLogger.getInstance(PerformActions.class)
                    .fine(String.format(
                            "Could Not Find TagName \"%s\" -> Cause: %s", element.getTagName(), e.getMessage()));

            //                    if (!byPassNotFound) {
            //                        performMessage.couldNotFindElement(element.getTagName());
            //                    }
            //                    return false;
        }

        try {
            element.click();
            return true;
        } catch (ElementClickInterceptedException e) {
            try {
                JavascriptExecutor jse = (JavascriptExecutor) arWebDriver.getDriver();
                jse.executeScript("arguments[0].click()", element);
                return true;
            } catch (Exception ex) {

                ARLogger.getInstance(PerformActions.class)
                        .fine(String.format(
                                "Could Not Click on  \"%s\" -> Cause: %s", element.getTagName(), e.getMessage()));
                return false;
            }
        }
    }

    public void refreshPage() {
        arWebDriver.getDriver().navigate().refresh();
        justCalledRefreshPage = true;
    }

    private boolean insertInElement(
            boolean byPassNotFound,
            WebElement element,
            String dataFieldValue,
            String defaultValue,
            boolean isEncrypted,
            boolean pressEnterAfter)
            throws Exception {
        UtilsMethods.exceptionIfNullWebElement(element);

        try {
            scrollIntoView(element);
            waitForAction.until(ExpectedConditions.visibilityOf(element));
        } catch (Exception e) {
            ARLogger.getInstance(PerformActions.class)
                    .fine(String.format(
                            "Could Not Find TagName \"%s\" -> Cause: %s", element.getTagName(), e.getMessage()));
            //                    if (!byPassNotFound) {
            //                        performMessage.couldNotFindElement(element.getTagName());
            //                    }
            //                    return false;
        }

        try {

            if (Strings.isNullOrEmpty(defaultValue)) {

                if (isEncrypted) {
                    dataFieldValue = CryptationAlgorithm.decrypt(dataFieldValue);
                }

                if (dataFieldValue != null) {
                    element.clear();
                    element.sendKeys(dataFieldValue);
                    // Waits component reaction
                    onHoldInSeconds(1);
                    if (!pressEnterAfter) {
                        element.sendKeys(Keys.TAB);
                    } else {
                        element.sendKeys(Keys.ENTER);
                    }
                } else {
                    element.sendKeys(UtilsMethods.generateRandomID(10));
                    // Waits component reaction
                    onHoldInSeconds(2);
                    if (!pressEnterAfter) {
                        element.sendKeys(Keys.TAB);
                    } else {
                        element.sendKeys(Keys.ENTER);
                    }
                }
            } else {
                dataFieldValue = defaultValue;

                if (isEncrypted) {
                    dataFieldValue = CryptationAlgorithm.decrypt(dataFieldValue);
                }
                element.sendKeys(dataFieldValue);
                // Waits component reaction
                onHoldInSeconds(2);
                if (!pressEnterAfter) {
                    element.sendKeys(Keys.TAB);
                } else {
                    element.sendKeys(Keys.ENTER);
                }
            }
        } catch (Exception e) {
            ARLogger.getInstance(PerformActions.class)
                    .severe(String.format(
                            "Could Not Input Value to \"%s\" -> Cause: %s", element.getTagName(), e.getMessage()));

            //                        performMessage.couldNotInputBotJobVeryFast("Could Input Values to Element " +
            // element.getTagName());
            return false;
        }

        return true;
    }

    private void scrollIntoView(WebElement element) {
        try {
            ((JavascriptExecutor) arWebDriver.getDriver()).executeScript("arguments[0].scrollIntoView(true);", element);
        } catch (Exception ignore) {

        }
    }

    /**
     * Extracts the dataFieldName and dataFieldValue based on the instruction and DTO.
     */
    public Pair<String, String> extractFieldData(
            Map<String, String> data, String[] actions, String defaultValue, boolean isEncrypted) throws Exception {

        String dataFieldName = "";
        String dataFieldValue = "";

        if (data != null) {
            if (actions.length >= 3 && actions[0].equals(ARConstants.INSERT) && actions[1].equals(ARConstants.ENTER)) {
                dataFieldName = actions[2].split(ARConstants.PATH_FIELD_SUBSTITUTION)[0];
                dataFieldValue = data.get(dataFieldName);

                if (isEncrypted && dataFieldValue != null) {
                    dataFieldValue = CryptationAlgorithm.decrypt(dataFieldValue);
                }
            } else if (actions.length == 2 && actions[0].equals(ARConstants.INSERT)) {
                dataFieldName = actions[1].split(ARConstants.PATH_FIELD_SUBSTITUTION)[0];
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
            boolean byPassNotFound,
            WebElement element,
            String coordinates,
            Pair<String, String> data,
            boolean pressEnterAfter)
            throws Exception {
        UtilsMethods.exceptionIfNullWebElement(element);
        try {
            scrollIntoView(element);
            waitForAction.until(ExpectedConditions.visibilityOf(element));
        } catch (Exception e) {
            ARLogger.getInstance(PerformActions.class)
                    .fine(String.format(
                            "Could Not Find Select \"%s\" Value  \"%s\" -> Cause: %s",
                            data.getKey(), data.getValue(), e.getMessage()));
            //            if (!byPassNotFound) {
            //                performMessage.couldNotFindElement(data.getKey());
            //            }
        }

        try {
            // Create a Select instance to interact with the dropdown
            //            Select selectCountry = new Select(element);
            //            // Select "Switzerland" by visible text
            //            selectCountry.selectByVisibleText(data.getValue());

            String[] coordArray = new String[] {coordinates, "coordinates"};
            sequenceOfCommands(element, ARConstants.SELECT, coordArray, data, arWebDriver.getDriver(), pressEnterAfter);

        } catch (Exception e) {
            ARLogger.getInstance(PerformActions.class)
                    .severe(String.format(
                            "Could Not Input Value to \"%s\" -> Cause: %s", element.getTagName(), e.getMessage()));

            //            performMessage.couldNotFindElement("Could Input Values to Element " + element.getTagName());

            //            performMessage.couldNotInputBotJobVeryFast("Could Input Values to Element " +
            // element.getTagName());

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
            scrollIntoView(element);
            waitForAction.until(ExpectedConditions.visibilityOf(element));
        } catch (Exception ex) {
            ARLogger.getInstance(PerformActions.class)
                    .warning(
                            String.format("Could Not Find Field Name \"%s\" -> Cause: %s", fieldName, ex.getMessage()));

            if (!byPassNotFound) {
                //                performMessage.couldNotFindElement(fieldName);
                performMessage.couldNotInputBotJobVeryFast("Could Not Find Field Name " + fieldName);
            }
            return false;
        }

        String textByhJS = "";
        String finalTextNested = "";
        String textAttribute = "";
        String textContext = "";

        try {
            JavascriptExecutor js = (JavascriptExecutor) arWebDriver.getDriver();
            textByhJS = (String) js.executeScript("return arguments[0].textContent;", element);
        } catch (Exception ex) {
            ARLogger.getInstance(PerformActions.class)
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
            ARLogger.getInstance(PerformActions.class)
                    .warning(String.format(
                            "By Text Nested - Not succeeded to get a Text from Label for: %s", fieldName));
        }

        try {
            textAttribute = element.getAttribute("value");
        } catch (Exception ex) {
            ARLogger.getInstance(PerformActions.class)
                    .warning(String.format(
                            "By Text Attribute - Not succeeded to get a Text from Label for: %s Operation: %s",
                            fieldName, action));
        }

        try {
            textContext = element.getAttribute("textContent");
        } catch (Exception ex) {
            ARLogger.getInstance(PerformActions.class)
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
            ARLogger.getInstance(PerformActions.class)
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
            ARLogger.getInstance(PerformActions.class)
                    .severe(String.format("Failed to retrieve text from element for: %s", fieldName));
        }

        return true;
    }

    private void listOperation(boolean byPassNotFound, InstructionLoadDTO InstructionLoadDTO) {

        /*
        TODO: Da rivedere, attualmente non del tutto funzionante
        Complex instruction string interpretation:
        [       0       ||       1      ||       2         ||    3    ||        4       ||  5   ||            6                ]
        [backward_button||forward_button||list_elements_tag||condition||expected_results||action||sub_element_on_execute_action]
        */
        List<ComplexInstructionLoadDTO> complexInstructionLoadDTOS =
                InstructionLoadDTO.getComplexInstructionLoadDTOList();
        String[] complexActionParts =
                complexInstructionLoadDTOS.get(0).getInstruction().split(ARConstants.COMPLEX_INSTRUCTION_SEPARATOR);
        List<WebElement> webElementList;
        WebElement forwardButton;
        WebElement backwardButton;
        boolean shouldContinue = true;

        boolean existNextPage;
        do {
            try {
                waitForPage.until(ExpectedConditions.visibilityOfElementLocated(By.tagName(complexActionParts[2])));
            } catch (Exception e) {
                ARLogger.getInstance(PerformActions.class)
                        .fine(String.format(
                                "Could Not Find TagName \"%s\" Criteria \"%s\" -> Cause: %s",
                                complexActionParts[2], By.tagName(complexActionParts[2]), e.getMessage()));

                if (!byPassNotFound) {
                    //                    performMessage.couldNotFindElement(complexActionParts[2]);
                    performMessage.couldNotInputBotJobVeryFast("Could Not Find TagName " + complexActionParts[2]);
                }
            }

            backwardButton = arWebDriver.getDriver().findElement(By.xpath(complexActionParts[0]));
            forwardButton = arWebDriver.getDriver().findElement(By.xpath(complexActionParts[1]));
            webElementList = arWebDriver.getDriver().findElements(By.tagName(complexActionParts[2]));

            WebElement element;
            WebElement reasonWebElement;
            for (int i = 0; i < 5; i++) {

                if (i != 0) {

                    try {
                        waitForPage.until(
                                ExpectedConditions.visibilityOfElementLocated(By.tagName(complexActionParts[2])));
                        webElementList = arWebDriver.getDriver().findElements(By.tagName(complexActionParts[2]));
                    } catch (Exception e) {
                        ARLogger.getInstance(PerformActions.class)
                                .fine(String.format(
                                        "Could Not Find TagName \"%s\" Criteria \"%s\" -> Cause: %s",
                                        complexActionParts[2], By.tagName(complexActionParts[2]), e.getMessage()));

                        if (!byPassNotFound) {
                            //                            performMessage.couldNotFindElement(complexActionParts[2]);
                            performMessage.couldNotInputBotJobVeryFast(
                                    "Could Not Find TagName " + complexActionParts[2]);
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
                            arWebDriver
                                    .getDriver()
                                    .findElement(
                                            By.xpath(
                                                    ".//button[@test-id='web-banking-payment-core.payment-ctx-action.payment-action-VIEW']")));

                    Thread.sleep(1000);
                    clickElement(
                            byPassNotFound,
                            arWebDriver
                                    .getDriver()
                                    .findElement(
                                            By.xpath(
                                                    ".//button[@test-id='web-banking-common.export-to-file.single-file-button']")));

                    Thread.sleep(1000);
                    clickElement(
                            byPassNotFound,
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
                scrollToElement(byPassNotFound, forwardButton);
                clickElement(byPassNotFound, forwardButton);
                existNextPage = true;
            } catch (Exception e) {
                existNextPage = false;
            }

        } while (existNextPage && shouldContinue);
    }

    public void quit(int status) {
        arWebDriver.getDriver().quit();
        if (status == 0) {
            System.exit(status);
        }
    }

    public short operationLog(boolean success, String mainMsg, String currentExecution, long duration) {

        if (success) {

            ARLogger.getInstance(PerformActions.class)
                    .info(String.format(
                            success
                                    ? "SUCCESS %s Current Cmd: %s - Duration: %s"
                                    : "FAILED %s Current Cmd: %s - Duration: %s",
                            mainMsg,
                            currentExecution,
                            LocalTime.ofNanoOfDay(duration).format(FORMAT_TIME)));
        } else {

            ARLogger.getInstance(PerformActions.class)
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

        //        JavascriptExecutor js = (JavascriptExecutor) arWebDriver.getDriver();
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
            InstructionLoadDTO currentInstruction,
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
            InstructionLoadDTO currentInstruction,
            String lastInstructionExecuted,
            ARConstants.ConditionStatus conditionStatus) {

        if (conditionStatus.equals(ARConstants.ConditionStatus.NONE)) {
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

        String conditionalBlock = conditionStatus.equals(ARConstants.ConditionStatus.IF_PASSED)
                ? "Closing Block { IF -> ELSE }  -> "
                : conditionStatus.equals(ARConstants.ConditionStatus.ELSEIF_PASSED)
                        ? "Closing Block { ELSEIF -> ELSE }  -> "
                        : conditionStatus.equals(ARConstants.ConditionStatus.ELSE_PASSED)
                                ? "Closing Block { ELSE -> ENDIF }  -> "
                                : "";

        if (!conditionStatus.equals(ARConstants.ConditionStatus.NONE)) {
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
            InstructionLoadDTO currentInstruction, BlockLoadDTO blockLoad, boolean ifClause, boolean elseClause) {
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
            ARLogger.getInstance(PerformActions.class)
                    .warning(String.format(
                            "%sParent Id Error Check Parent Id: %d "
                                    + "For the \"%s\" Does not belong to this block: "
                                    + blockLoad.getId() + "-" + blockLoad.getName(),
                            conditionalBlock,
                            currentInstruction.getParentId(),
                            currentInstruction.getOperation()));

        } else {
            ARLogger.getInstance(PerformActions.class)
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
            InstructionLoadDTO currentInstruction,
            BlockLoadDTO blockLoad,
            String lastInstructionExecuted,
            ARConstants.ConditionStatus conditionStatus) {

        if (conditionStatus.equals(ARConstants.ConditionStatus.NONE)) {
            String operation = currentInstruction.getOperation();
            int colonIndex = operation.indexOf(":");
            String parentOperationPart = colonIndex != -1 ? operation.substring(0, colonIndex) : "Unknown Operation";

            String msg1 = "The Parent Id: \"(" + currentInstruction.getParentId() + ")" + parentOperationPart + "\"";
            String msg2 = "Does not belong to the block: \"" + blockLoad.getBlockOrderNumber() + "-"
                    + blockLoad.getName() + "\"";
            String msg3 = "Attempted Operation : \""
                    + (currentInstruction.getActions().equals(ARConstants.EXTRACT_FIELD)
                            ? "Extract "
                            : currentInstruction.getActions())
                    + "\" -> \""
                    + operation + "\"";
            String msg4 = "Check the Web Field \" ( ID ) <NAME> \" per Block";

            performMessage.errorMessage("Parent Id Error", msg1, msg2, msg3, msg4, 0);
        }

        String conditionalBlock = conditionStatus.equals(ARConstants.ConditionStatus.IF_PASSED)
                ? "Closing Block { IF -> ELSE }  -> "
                : conditionStatus.equals(ARConstants.ConditionStatus.ELSEIF_PASSED)
                        ? "Closing Block { ELSEIF -> ELSE }  -> "
                        : conditionStatus.equals(ARConstants.ConditionStatus.ELSE_PASSED)
                                ? "Closing Block { ELSE -> ENDIF }  -> "
                                : "";

        if (!conditionStatus.equals(ARConstants.ConditionStatus.NONE)) {
            ARLogger.getInstance(PerformActions.class)
                    .warning(String.format(
                            "%sParent Id Error Check Parent Id: %d For the \"%s\" Does not belong to this block: %d-%s",
                            conditionalBlock,
                            currentInstruction.getParentId(),
                            currentInstruction.getOperation(),
                            blockLoad.getId(),
                            blockLoad.getName()));
        } else {
            ARLogger.getInstance(PerformActions.class)
                    .severe(String.format(
                            "Parent Id Error Check Parent Id: %d For the \"%s\" Does not belong to this block: %d-%s",
                            currentInstruction.getParentId(),
                            currentInstruction.getOperation(),
                            blockLoad.getId(),
                            blockLoad.getName()));
        }

        if (!conditionStatus.equals(ARConstants.ConditionStatus.NONE)) {
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
            ARConstants.ConditionStatus conditionStatus,
            boolean byPassFlagLoop) {

        if (conditionStatus.equals(ARConstants.ConditionStatus.NONE) && !byPassFlagLoop) {
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

        String conditionalBlock = conditionStatus.equals(ARConstants.ConditionStatus.IF_PASSED)
                ? "Closing Block { IF -> ELSE }  -> "
                : conditionStatus.equals(ARConstants.ConditionStatus.ELSEIF_PASSED)
                        ? "Closing Block { ELSEIF -> ELSE }  -> "
                        : conditionStatus.equals(ARConstants.ConditionStatus.ELSE_PASSED)
                                ? "Closing Block { ELSE -> ENDIF }  -> "
                                : "";

        if (!conditionStatus.equals(ARConstants.ConditionStatus.NONE)) {
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
            ARConstants.ConditionStatus currentCondition,
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

        ARLogger.getInstance(PerformActions.class)
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
        Set<String> windowHandles = arWebDriver.getDriver().getWindowHandles();
        windowHandlesList = new ArrayList<>(windowHandles);
    }

    public String getSessionId() {
        if (arWebDriver.getDriver() instanceof RemoteWebDriver) {
            return ((RemoteWebDriver) arWebDriver.getDriver()).getSessionId().toString();
        } else {
            throw new IllegalStateException("Driver is not an instance of RemoteWebDriver");
        }
    }

    public void alertMessage(String message) {
        JavascriptExecutor js = (JavascriptExecutor) arWebDriver.getDriver();

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
        org.openqa.selenium.Alert alert = arWebDriver.getDriver().switchTo().alert();

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
            case ARConstants.VISUALIZE:
                return "Visualize action executed for " + msgInstruction.getKey();
            case ARConstants.OTHER:
                return "Other Element --> " + msgInstruction.getKey();
            case ARConstants.OUTPUT:
                return "Output Element --> " + msgInstruction.getKey();
            case ARConstants.CLICK:
                return "Click Element --> " + msgInstruction.getKey();
            case ARConstants.INSERT:
                if (actions[0].equals(ARConstants.INSERT) && actions[1].equals(ARConstants.ENTER)) {
                    return "Insert/<Enter> action for  -> " + msgInstruction.getKey() + " = "
                            + msgInstruction.getValue();
                } else {
                    return "Insert action for  -> " + msgInstruction.getKey() + " = " + msgInstruction.getValue();
                }
            case ARConstants.LIST_OPERATION:
                return "List Operation performed for " + msgInstruction.getKey();
            case ARConstants.HOLD:
                return "Hold executed: " + msgInstruction.getKey();
            case ARConstants.PAUSE:
                return "Pause action triggered";
            case ARConstants.GOTO:
                if (msgInstruction.getValue().equals("Unknown")) {
                    return msgInstruction.getKey();
                } else {
                    String[] parts = msgInstruction.getKey().split(":");
                    return String.format(
                            "GO TO Block \"%s\" Limit %s times",
                            "(" + parts[0] + ")-#" + parts[2] + " " + parts[3], msgInstruction.getValue());
                }
            case ARConstants.REFRESH_ONLY:
                return " Refresh Web Page";
            case ARConstants.REFRESH_HOLD:
                String[] msgParent = msgInstruction.getKey().split(":");
                String[] msgValue = msgInstruction.getValue().split(":");
                return String.format(
                        "Wait for Parent \"%s\" Limit %s seconds",
                        "(" + msgParent[1] + ") " + msgParent[2], msgValue[0]);
            case ARConstants.LOOP:
                if (msgInstruction.getValue().equals("Unknown")) {
                    return msgInstruction.getKey();
                } else {
                    msgParent = msgInstruction.getKey().split(":");
                    return String.format(
                            "Jump To Parent \"%s\" Limit %s times",
                            msgParent[0] + "-(" + msgParent[1] + ") " + msgParent[2], msgInstruction.getValue());
                }
            case ARConstants.REFRESH_LOOP:
                if (msgInstruction.getValue().equals("Unknown")) {
                    return msgInstruction.getKey();
                } else {
                    msgParent = msgInstruction.getKey().split(":");
                    msgValue = msgInstruction.getValue().split(":");
                    return String.format(
                            "Refresh in %s seconds Loop %s times Jump To Parent \"%s\" ",
                            msgValue[0], msgValue[1], msgParent[0] + "-(" + msgParent[1] + ") " + msgParent[2]);
                }
            case ARConstants.QUIT:
                return "Quit action processed";
            case ARConstants.SCREEN:
                return "Screen action executed for " + msgInstruction.getKey() + " --> " + blockJobName;
            case ARConstants.GET_VALUE:
            case ARConstants.SET_VALUE:
                return actions[0]
                        + ARConstants.BLANK_STRING
                        + msgInstruction.getKey()
                        + ARConstants.BLANK_STRING
                        + msgInstruction.getValue();
            case ARConstants.CHECK_VALUE:
                return actions[0]
                        + ARConstants.BLANK_STRING
                        + msgInstruction.getValue()
                        + ARConstants.BLANK_STRING
                        + msgInstruction.getKey();
            case ARConstants.EXTRACT_FIELD:
                return ARConstants.BLANK_STRING
                        + msgInstruction.getKey() + " Extract "
                        + ARConstants.BLANK_STRING
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

    public String getXPathInstruction(InstructionLoadDTO currentInstruction, BlockLoadDTO blockLoad) {
        try {
            return blockLoad.getInstructionLoadDTOS().stream()
                    .filter(f -> f.getId().equals(currentInstruction.getParentId()))
                    .findFirst()
                    .get()
                    .getXpath();
        } catch (Exception ex) {
            return null;
        }
    }

    public String getInstructionParentField(InstructionLoadDTO currentInstruction, BlockLoadDTO blockLoad) {
        try {
            return blockLoad.getInstructionLoadDTOS().stream()
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
            ARConstants.ConditionStatus condition,
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
            performMessage.showCustomModalDialogDragWin11(
                    "ERROR ON CONDITIONAL BLOCK",
                    String.format(
                            "Cannot find a matching condition for \"%s\" greater than the current index %d",
                            condition, currentIndex),
                    " Please click OK to continue!",
                    null,
                    null,
                    true,
                    "OK",
                    null,
                    0);
        }

        return -1; // Return -1 if no valid index is found
    }

    public Map<String, List<Integer>> getConditionIndexMapByParentId(BlockLoadDTO blockLoad) {
        try {
            // Create a map where key is "parentId-actions" and value is a list of indices
            return IntStream.range(0, blockLoad.getInstructionLoadDTOS().size())
                    .filter(index -> {
                        InstructionLoadDTO instruction =
                                blockLoad.getInstructionLoadDTOS().get(index);
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
                                InstructionLoadDTO instruction =
                                        blockLoad.getInstructionLoadDTOS().get(index);
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
        String htmlPath = ARPropertyManager.getInstance().getProperty(ARPropertyEnum.FOLDER_PATH_EXPORT);
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

    public void executeActionsAtInstructionCoordinates(InstructionLoadDTO currentInstruction, Pair<String, String> data)
            throws Exception {

        List<com.allinweb.ch.util.Priority> priorityList = ARPriorities.getAllPriorityList();
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
                String[] coordinates = reference.get().getValue().split(ARConstants.FIELDS_SEPARATOR);

                double temp1 = Double.parseDouble(coordinates[0]);
                double temp2 = Double.parseDouble(coordinates[1]);
                x = (int) temp1;
                y = (int) temp2;
                int maxHeight =
                        arWebDriver.getDriver().manage().window().getSize().getHeight();
                int maxWidth =
                        arWebDriver.getDriver().manage().window().getSize().getWidth();
                int offsetY = y - maxHeight;
                int offsetX = x - maxWidth;
                xCoord = x > maxWidth ? x - offsetX : x;
                yCoord = y > maxHeight ? y - offsetY : y;
            }
            String[] actions = currentInstruction.getActions().split(ARConstants.ACTIONS_AND_PATHS_SPLITTER);
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
        String[] coordinates = savedCoordinates.split(ARConstants.FIELDS_SEPARATOR);
        double temp1 = Double.parseDouble(coordinates[0]);
        double temp2 = Double.parseDouble(coordinates[1]);
        x = (int) temp1;
        y = (int) temp2;
        int maxHeight = arWebDriver.getDriver().manage().window().getSize().getHeight();
        int maxWidth = arWebDriver.getDriver().manage().window().getSize().getWidth();
        int offsetY = y - maxHeight;
        int offsetX = x - maxWidth;
        xCoord = x > maxWidth ? x - offsetX : x;
        yCoord = y > maxHeight ? y - offsetY : y;

        Map<String, String> mapCoordinates = new HashMap<>();

        mapCoordinates.put("ScrollTo", x + ":" + y);
        mapCoordinates.put("ClickOn", xCoord + ":" + yCoord);
        return mapCoordinates;
    }

    public boolean executeActionsAtCoordinates(
            String savedCoordinates, Pair<String, String> data, String action, boolean pressEnterAfter) {

        boolean forceCLick = false;

        int x = 0;
        int y = 0;
        int xCoord = 0;
        int yCoord = 0;
        try {
            String[] coordinates = savedCoordinates.split(ARConstants.FIELDS_SEPARATOR);
            double temp1 = Double.parseDouble(coordinates[0]);
            double temp2 = Double.parseDouble(coordinates[1]);
            x = (int) temp1;
            y = (int) temp2;
            int maxHeight = arWebDriver.getDriver().manage().window().getSize().getHeight();
            int maxWidth = arWebDriver.getDriver().manage().window().getSize().getWidth();
            int offsetY = y - maxHeight;
            int offsetX = x - maxWidth;
            xCoord = x > maxWidth ? x - offsetX : x;
            yCoord = y > maxHeight ? y - offsetY : y;

            if (ARConstants.VISUALIZE.equals(action)) {
                scrollToCoordinates(x, y);
            } else if (ARConstants.CLICK.equals(action)) {
                scrollToCoordinates(x, y);
                //                circleAtCoordinates(x, y, arWebDriver.getDriver());
                onHoldForSeconds(null);
                clickAtCoordinates(xCoord, yCoord);
            } else if (ARConstants.INSERT.equals(action)) {
                scrollToCoordinates(x, y);
                //                sendInputJS(x, y, data.getValue(),arWebDriver.getDriver());
                //                circleAtCoordinates(x, y, arWebDriver.getDriver());
                onHoldForSeconds(null);
                //                clickAtCoordinates(xCoord, yCoord);
                //                onHoldForSeconds(null);
                typeCharacters(data);

                if (pressEnterAfter) {
                    sendEnter(xCoord, yCoord);
                }
            } else if (ARConstants.INSERT.equals(action) && forceCLick) {
                scrollToCoordinates(x, y);
                //                sendInputJS(x, y, data.getValue(),arWebDriver.getDriver());
                //                circleAtCoordinates(x, y, arWebDriver.getDriver());
                onHoldForSeconds(null);
                clickAtCoordinates(xCoord, yCoord);
                onHoldForSeconds(null);
                typeCharacters(data);

                if (pressEnterAfter) {
                    sendEnter(xCoord, yCoord);
                }
            }
            onHoldForSeconds(null);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void scrollToElement(String spath) {
        WebDriver driver = arWebDriver.getDriver();
        WebElement element = new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.presenceOfElementLocated(By.xpath(spath)));

        String script = "function getScrollableParent(element) {"
                + "    let value = window.getComputedStyle(element).overflowY;"
                + "    if(value !== 'scroll' && value !== 'auto') {"
                + "        return getScrollableParent(element.parentNode);"
                + "    }"
                + "    return element;"
                + "}"
                + "let el = arguments[0];"
                + "getScrollableParent(el).scrollTo({ top: el.offsetTop, behavior: 'smooth' });"
                + "return true;";

        ((JavascriptExecutor) driver).executeScript(script, element);
    }

    private void scrollToCoordinates(int x, int y) {
        int maxHeight = arWebDriver.getDriver().manage().window().getSize().getHeight();
        int maxWidth = arWebDriver.getDriver().manage().window().getSize().getWidth();
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
            new WebDriverWait(arWebDriver.getDriver(), Duration.ofSeconds(10))
                    .until((item) -> (Boolean) ((JavascriptExecutor) arWebDriver.getDriver()).executeScript(script));
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
        new Actions(arWebDriver.getDriver()).moveToLocation(x, y).click().perform();
    }

    public WebElement getElementFromCoordinates(String savedCoordinates) {
        int x = 0;
        int y = 0;
        int xCoord = 0;
        int yCoord = 0;
        try {
            String[] coordinates = savedCoordinates.split(ARConstants.FIELDS_SEPARATOR);
            double temp1 = Double.parseDouble(coordinates[0]);
            double temp2 = Double.parseDouble(coordinates[1]);
            x = (int) temp1;
            y = (int) temp2;
            int maxHeight = arWebDriver.getDriver().manage().window().getSize().getHeight();
            int maxWidth = arWebDriver.getDriver().manage().window().getSize().getWidth();
            int offsetY = y - maxHeight;
            int offsetX = x - maxWidth;
            xCoord = x > maxWidth ? x - offsetX : x;
            yCoord = y > maxHeight ? y - offsetY : y;

            JavascriptExecutor js = (JavascriptExecutor) arWebDriver.getDriver();

            WebElement elementFound = (WebElement)
                    js.executeScript("return document.elementFromPoint(arguments[0], arguments[1]);", xCoord, yCoord);

            return elementFound;

        } catch (Exception e) {
            return null;
        }
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
        new Actions(arWebDriver.getDriver()).sendKeys(fieldData.getValue()).perform();
    }

    private void sendEnter(int x, int y) {
        new Actions(arWebDriver.getDriver())
                .moveByOffset(x, y)
                .sendKeys(Keys.ENTER)
                .perform();
    }

    public String sequenceOfCommands(
            WebElement element,
            String typeCommand,
            String[] coordinates,
            Pair<String, String> fieldData,
            WebDriver driver,
            boolean pressEnterAfter) {

        String message = "Nothing to execute";
        try {
            if (typeCommand.equals(ARConstants.SELECT)) {
                // Create a Select instance to interact with the dropdown
                message = "Select(element)";
                Select selectCountry = new Select(element);
                selectCountry.selectByVisibleText(fieldData.getValue());
                if (!pressEnterAfter) {
                    element.sendKeys(Keys.TAB);
                } else {
                    element.sendKeys(Keys.ENTER);
                }
            } else if (typeCommand.equals(ARConstants.CLEAR)) {
                message = "clear()";
                element.clear();
            } else if (typeCommand.equals(ARConstants.CLICK)) {
                message = "click()";
                element.click();
            } else if (typeCommand.equals(ARConstants.INSERT)) {
                message = "sendKeys(\"" + fieldData.getValue() + "\")";
                element.sendKeys(fieldData.getValue());
                if (!pressEnterAfter) {
                    element.sendKeys(Keys.TAB);
                } else {
                    element.sendKeys(Keys.ENTER);
                }
            } else if (typeCommand.equals(ARConstants.TAB)) {
                message = "(Keys.TAB)";
                element.sendKeys(Keys.TAB);
            } else if (typeCommand.equals(ARConstants.GET_VALUE)) {
                message = "getText()";
                element.getText();
            } else if (typeCommand.equals(ARConstants.FOCUS)) {
                message = "focusElement(element, driver)";
                focusElement(element, driver);
            } else if (typeCommand.equals(ARConstants.COORD_VISUALIZA)) {
                message = "Coordinates Visualiza";
                for (String coords : coordinates) {
                    executeActionsAtCoordinates(coords, fieldData, ARConstants.VISUALIZE, pressEnterAfter);
                }
            } else if (typeCommand.equals(ARConstants.COORD_CLICK)) {
                message = "Coordinates Click";
                for (String coords : coordinates) {
                    executeActionsAtCoordinates(coords, fieldData, ARConstants.CLICK, pressEnterAfter);
                }
            } else if (typeCommand.equals(ARConstants.COORD_INSERT)) {
                message = "Coordinates Insert";
                if (pressEnterAfter) {
                    message = "Coordinates Insert with <ENTER>";
                }
                for (String coords : coordinates) {
                    executeActionsAtCoordinates(coords, fieldData, ARConstants.INSERT, pressEnterAfter);
                }
            } else if (typeCommand.equals(ARConstants.COORD_MOVE_CLICK_RED)) {
                message = "Coordinates Move Insert Red Circle";
                for (String coords : coordinates) {
                    moveAndClickAtCoordinates(coords, pressEnterAfter);
                }
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

    public String moveAndClickAtCoordinates(String savedCoordinates, boolean pressEnterAfter) {
        String[] coordinates = savedCoordinates.split(ARConstants.FIELDS_SEPARATOR);
        double temp1 = Double.parseDouble(coordinates[0]);
        double temp2 = Double.parseDouble(coordinates[1]);
        int xCoord = (int) temp1;
        int yCoord = (int) temp2;
        try {
            String script =
                    "function moveAndClickMouse(x, y) {\n" + "    const mouseDiv = document.createElement('div');\n"
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

            ((JavascriptExecutor) arWebDriver.getDriver()).executeScript(script, xCoord, yCoord);

            if (pressEnterAfter) {
                sendEnter(xCoord, yCoord);
            }

            return "Success Move And Click -> Red Circle";

        } catch (Exception error) {
            return "Failed Move And Click -> Red Circle";
        }
    }

    public Pair<String, String> getBlockDetailsById(
            List<BlockLoadDTO> blocksLoaded, InstructionLoadDTO currentInstruction) {
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
            List<InstructionLoadDTO> instructionLoadDTOS, InstructionLoadDTO currentInstruction) {
        for (InstructionLoadDTO instParent : instructionLoadDTOS) {
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

    public Map<String, Integer[]> getLoopAndRefreshLoops(List<InstructionLoadDTO> instructionLoadDTOS) {
        // Step 2: Filter rows where actions = "REFRESH_LOOP" or "LOOP" and collect into the map
        Map<String, Integer[]> mapRefreshLoops = new HashMap<>();

        for (InstructionLoadDTO instruction : instructionLoadDTOS) {
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

    public Set<Integer> getParentIdsForLoop(List<InstructionLoadDTO> instructionLoadDTOS) {
        return instructionLoadDTOS.stream()
                .filter(instruction -> "REFRESH_LOOP".equalsIgnoreCase(instruction.getActions())
                        || "LOOP".equalsIgnoreCase(instruction.getActions()))
                .map(InstructionLoadDTO::getParentId)
                .collect(Collectors.toSet());
    }

    public void logAndReport(
            ARConstants.ConditionStatus currentCondition,
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

    public ARConstants.ConditionStatus updateProgressSuccess(
            boolean success, ARConstants.ConditionStatus currentCondition) {
        // It Gets last Progress Status
        // Machine State
        if (currentCondition.equals(ARConstants.ConditionStatus.IF)) {
            return success ? ARConstants.ConditionStatus.IF_PASSED : ARConstants.ConditionStatus.IF_FAILED;
        } else if (currentCondition.equals(ARConstants.ConditionStatus.ELSEIF)) {
            return success ? ARConstants.ConditionStatus.ELSEIF_PASSED : ARConstants.ConditionStatus.ELSEIF_FAILED;
        } else if (currentCondition.equals(ARConstants.ConditionStatus.ELSE)) {
            return success ? ARConstants.ConditionStatus.ELSE_PASSED : ARConstants.ConditionStatus.ELSE_FAILED;
        } else if (currentCondition.equals(ARConstants.ConditionStatus.ENDIF)) {
            return ARConstants.ConditionStatus.NONE;
        }
        return ARConstants.ConditionStatus.NONE;
    }

    public int checkActionToJump(
            String action,
            ARConstants.ConditionStatus progressCondition,
            Map<String, List<Integer>> mapConditional,
            int parentBlockCondition,
            int currentIndex) {
        if (action.equalsIgnoreCase(ARConstants.ELSEIF)) {
            // Goes to the ENDIF (ENDIF index + 1);
            return searchMapConditional(
                    mapConditional, parentBlockCondition, ARConstants.ConditionStatus.ENDIF, currentIndex, true);

        } else if (action.equalsIgnoreCase(ARConstants.ELSE)) {
            // Goes to the ENDIF (ENDIF index + 1);
            return searchMapConditional(
                    mapConditional, parentBlockCondition, ARConstants.ConditionStatus.ENDIF, currentIndex, true);

        } else if (action.equalsIgnoreCase(ARConstants.ELSE)) {
            // Goes to the ENDIF (ENDIF index + 1);
            return searchMapConditional(
                    mapConditional, parentBlockCondition, ARConstants.ConditionStatus.ENDIF, currentIndex, true);
        }
        return 0;
    }

    public Map<WebElement, List<WebElement>> getIframeElementsMap() {
        iframeElementsMap = new HashMap<>();

        if (arWebDriver.getDriver() != null) {
            // Get all iframe elements on the page
            List<WebElement> iframeList = arWebDriver.getDriver().findElements(By.tagName("iframe"));
            System.out.println("Number of iframes found: " + iframeList.size());

            for (WebElement iframe : iframeList) {
                try {
                    // Switch to the iframe
                    arWebDriver.getDriver().switchTo().frame(iframe);

                    // Get all elements inside the iframe
                    List<WebElement> elementsInsideIframe =
                            arWebDriver.getDriver().findElements(By.xpath("//*"));
                    iframeElementsMap.put(iframe, elementsInsideIframe);

                    System.out.println("Iframe contains " + elementsInsideIframe.size() + " elements");
                } catch (Exception e) {
                    System.out.println("Could not access iframe: " + e.getMessage());
                } finally {
                    // Switch back to the main page
                    arWebDriver.getDriver().switchTo().defaultContent();
                }
            }

            iframeInputLocator.initializeIframeInputLocator(iframeElementsMap, arWebDriver.getDriver());
        }
        return iframeElementsMap;
    }

    public static String insertValueIFrameElement(
            WebDriver driver, String iframeXPath, String inputXPath, String inputValue) {
        JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;

        String script = "(function(iframeXPath, inputXPath, inputValue) {" + "    let logs = [];"
                + "    let iframe = document.evaluate(iframeXPath, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;"
                + "    if (iframe) {"
                + "        let iframeDocument = iframe.contentDocument || iframe.contentWindow.document;"
                + "        let inputElement = document.evaluate(inputXPath, iframeDocument, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;"
                + "        if (inputElement) {"
                + "            inputElement.value = inputValue;"
                + "            inputElement.dispatchEvent(new Event('input', { bubbles: true }));"
                + "            logs.push('Text entered successfully.');"
                + "        } else {"
                + "            logs.push('Input field not found inside the iframe.');"
                + "        }"
                + "    } else {"
                + "        logs.push('Iframe not found.');"
                + "    }"
                + "    return logs.join('\n');"
                + "})(arguments[0], arguments[1], arguments[2]);";

        return (String) jsExecutor.executeScript(script, iframeXPath, inputXPath, inputValue);
    }

    public static String insertValueIFrameElement(
            WebDriver driver,
            String iframeXPath,
            String inputXPath,
            String inputValue,
            String targetOriginURL,
            String trustedOriginURL) {

        JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;

        String script = "(function(iframeXPath, inputXPath, inputValue, targetOriginURL, trustedOriginURL) {"
                + "    let logs = [];"
                + "    let iframe = document.evaluate(iframeXPath, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;"
                + "    if (iframe) {"
                + "        let iframeDocument = iframe.contentDocument || iframe.contentWindow.document;"
                + "        let inputElement = document.evaluate(inputXPath, iframeDocument, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;"
                + "        if (inputElement) {"
                + "            inputElement.value = inputValue;"
                + "            inputElement.dispatchEvent(new Event('input', { bubbles: true }));"
                + "            logs.push('Text entered successfully.');"
                + "            "
                + "            // Send a message to the targetOriginURL (globally, once input is set)"
                + "            window.postMessage({ type: 'myMessage', data: 'some data' }, targetOriginURL);"
                + "        } else {"
                + "            logs.push('Input field not found inside the iframe.');"
                + "        }"
                + "    } else {"
                + "        logs.push('Iframe not found.');"
                + "    }"
                + "    return logs.join('\\n');"
                + "} )(arguments[0], arguments[1], arguments[2], arguments[3], arguments[4]);"
                + " // Listen for messages from the trusted origin (this needs to be in the global scope)"
                + "window.addEventListener('message', function (event) {"
                + "    if (event.origin !== trustedOriginURL) return;" // Validate message source
                + "    console.log('Received message:', event.data);"
                + "});";

        return (String) jsExecutor.executeScript(
                script, iframeXPath, inputXPath, inputValue, targetOriginURL, trustedOriginURL);
    }

    public List<ElementDTO> extractElementData(String[] iFrameElements) {
        List<ElementDTO> elements = new ArrayList<>();

        for (String line : iFrameElements) {
            String typeElement = "";
            String tagName = "";
            String xPath = "";
            String text = "";
            String attribId = "";
            String attribName = "";
            String coords = "";
            String allAttributes = "";
            String customXPath = "";

            // Split into key-value pairs based on ";"
            String[] parts = line.split(";");
            for (String part : parts) {
                int colonIndex = part.indexOf(":");
                if (colonIndex == -1) continue; // Skip invalid parts

                String key = part.substring(0, colonIndex).trim();
                String value = part.substring(colonIndex + 1).trim();

                // Map key-value pairs to variables
                switch (key) {
                    case "clicked":
                    case "tagName-found":
                    case "clicked-iFrame":
                    case "iFrame-Found":
                    case "iFrame-Child":
                        typeElement = key;
                        tagName = value;
                        break;
                    case "xpath":
                        xPath = value;
                        break;
                    case "text":
                        text = value;
                        break;
                    case "attribId":
                        attribId = value;
                        break;
                    case "attribName":
                        attribName = value;
                        break;
                    case "coords":
                        coords = value;
                        break;
                    case "allAttributes":
                        allAttributes = value;
                        break;
                    case "customXPath":
                        customXPath = value;
                        break;
                }
            }

            // Only add valid elements
            if (!typeElement.isEmpty() && !tagName.isEmpty()) {
                elements.add(new ElementDTO(
                        typeElement,
                        tagName,
                        xPath,
                        text,
                        attribId,
                        attribName,
                        coords,
                        allAttributes,
                        customXPath,
                        null,
                        null,
                        null,
                        null));
            }
        }
        return elements;
    }

    public TargetElement defineSearchReturn(ElementDTO elemenDTO, WebElement element, TargetElement targetDefine) {
        if (targetDefine == null || targetDefine.getElement() == null) {
            if (targetDefine == null) {
                targetDefine = new TargetElement();
            }

            targetDefine.setElement(element);

            // Reset Previous Values
            targetDefine.setAttribId(elemenDTO.getAttribId());
            targetDefine.setAttribName(elemenDTO.getAttribName());
            targetDefine.setOriginalTagName(elemenDTO.getTagName());
            targetDefine.setSomeText(elemenDTO.getSomeText());
            targetDefine.setCoords(elemenDTO.getCoords());

            targetDefine.setMainXPath(elemenDTO.getXPath());
            targetDefine.setCurrentXPath(elemenDTO.getXPath());

            targetDefine.setIFrameXPath(elemenDTO.getIFrameXPath());
            targetDefine.setAllAttributes(elemenDTO.getAllAttributes());
            targetDefine.setCustomXPath(elemenDTO.getCustomXPath());

            targetDefine.setDefinedName(null);
            if (!Strings.isNullOrEmpty(elemenDTO.getAttribId())) {
                targetDefine.setAttributeType("id");
                targetDefine.setAttributeValue(elemenDTO.getAttribId());
            } else if (!Strings.isNullOrEmpty(elemenDTO.getAttribName())) {
                targetDefine.setAttributeType("name");
                targetDefine.setAttributeValue(elemenDTO.getAttribName());
            } else {
                targetDefine.setAttributeType("");
                targetDefine.setAttributeValue("");
            }
            targetDefine.setIFrameElements(null);

            targetDefine.setXPathWorkedFirst(ARConstants.REGULAR_XPATH);

            // W3C 6 Headers
            String[] validHeaders = {"h1", "h2", "h3", "h4", "h5", "h6"};

            if (elemenDTO.getTagName().equalsIgnoreCase(WebElementTagNameEnum.BUTTON.getValue())
                    || elemenDTO.getTagName().equalsIgnoreCase(WebElementTagNameEnum.ANCHOR.getValue())
                    || elemenDTO.getTagName().equalsIgnoreCase(WebElementTagNameEnum.OPTION.getValue())
                    || elemenDTO.getTagName().equalsIgnoreCase(WebElementTagNameEnum.MAT_SELECT.getValue())) {
                targetDefine.setTagType(WebElementTagNameEnum.BUTTON);
                targetDefine.setIconType(WebElementIcon.CLICK);
            } else if (elemenDTO.getTagName().equalsIgnoreCase(WebElementTagNameEnum.INPUT.getValue())
                    || elemenDTO.getTagName().equalsIgnoreCase(WebElementTagNameEnum.TEXT_AREA.getValue())) {
                targetDefine.setTagType(WebElementTagNameEnum.INPUT);
                targetDefine.setIconType(WebElementIcon.INSERT);
            } else if (elemenDTO.getTagName().equalsIgnoreCase(WebElementTagNameEnum.PARAGRAPH.getValue())
                    || elemenDTO.getTagName().equalsIgnoreCase(WebElementTagNameEnum.HEADER.getValue())
                    || elemenDTO.getTagName().equalsIgnoreCase(WebElementTagNameEnum.LABEL.getValue())
                    || elemenDTO.getTagName().equalsIgnoreCase(WebElementTagNameEnum.FOR_LABEL.getValue())
                    || elemenDTO.getTagName().equalsIgnoreCase(WebElementTagNameEnum.DIV.getValue())
                    || elemenDTO.getTagName().equalsIgnoreCase(WebElementTagNameEnum.STRONG.getValue())
                    || elemenDTO.getTagName().equalsIgnoreCase(WebElementTagNameEnum.SPAN.getValue())
                    || Arrays.asList(validHeaders)
                            .contains(elemenDTO.getTagName().toLowerCase())) {
                targetDefine.setTagType(WebElementTagNameEnum.OUTPUT);
                targetDefine.setIconType(WebElementIcon.OUTPUT);
            } else if (elemenDTO.getTagName().equalsIgnoreCase(WebElementTagNameEnum.IFRAME.getValue())) {
                targetDefine.setTagType(WebElementTagNameEnum.IFRAME);
                targetDefine.setIconType(WebElementIcon.IFRAME);
            } else {
                targetDefine.setTagType(WebElementTagNameEnum.ALL);
                targetDefine.setIconType(WebElementIcon.TEXT);
            }
        }
        return targetDefine;
    }

    public static String truncateAndNormalize(String someText, int limit) {
        if (someText == null || someText.isEmpty()) {
            return someText;
        }

        // Remove extra spaces and trim
        String normalizedText = someText.trim().replaceAll("\\s+", " ");

        if (normalizedText.length() <= limit) {
            return normalizedText;
        }

        return normalizedText.substring(0, limit) + "...";
    }

    // TODO MORE INTELLIGENT  LOGIC MADE BY ME
    public TargetElement defineTargetNameTitles(TargetElement target) {

        try {
            String tagNameDefined =
                    target.getDefinedName() != null ? target.getDefinedName() : target.getOriginalTagName();
            WebElement targetElem = target.getElement();

            // Check element tag names
            boolean isAnchor = target.getOriginalTagName().equalsIgnoreCase(WebElementTagNameEnum.ANCHOR.getValue());
            boolean isOption = target.getOriginalTagName().equalsIgnoreCase(WebElementTagNameEnum.OPTION.getValue());

            // Extract various attributes

            String labelAttributeValue = extractAttribute(targetElem, WebElementAttributeEnum.LABEL);
            String forLabelAttributeValue = extractAttribute(targetElem, WebElementAttributeEnum.FOR_LABEL);
            String classAttributeValue = extractAttribute(targetElem, WebElementAttributeEnum.CLASS);
            String typeAttributeValue = extractAttribute(targetElem, WebElementAttributeEnum.TYPE);
            String idAttributeValue = extractAttribute(targetElem, WebElementAttributeEnum.ID);
            String titleAttributeValue = extractAttribute(targetElem, WebElementAttributeEnum.TITLE);
            String disabledAttributeValue = extractAttribute(targetElem, WebElementAttributeEnum.DISABLED);
            String styleAttributeValue = extractAttribute(targetElem, WebElementAttributeEnum.STYLE);
            String dataTestIdAttributeValue = extractAttribute(targetElem, WebElementAttributeEnum.DATA_TEST_ID);

            String ariaLabelValue = extractAttribute(targetElem, WebElementAttributeEnum.ARIA_LABEL);
            String innerHTMLValue = extractAttribute(targetElem, WebElementAttributeEnum.INNER_HTML);
            String formControlNameAttributeValue =
                    extractAttribute(targetElem, WebElementAttributeEnum.FORM_CONTROL_NAME);
            String testIdAttributeValue = extractAttribute(targetElem, WebElementAttributeEnum.TEST_ID);
            String nameAttributeValue = extractAttribute(targetElem, WebElementAttributeEnum.NAME);
            String valueAttributeValue = extractAttribute(targetElem, WebElementAttributeEnum.VALUE);

            String hasMatLabelValue = extractAttribute(targetElem, WebElementAttributeEnum.MAT_LABEL);
            String hasMatInputValue = extractAttribute(targetElem, WebElementAttributeEnum.MAT_INPUT);
            String hasInputValue = extractAttribute(targetElem, WebElementAttributeEnum.MAT_INPUT);

            String valueHRefFile = extractFileExtension(extractAttribute(targetElem, WebElementAttributeEnum.HREF));

            String textLabel = targetElem.getText();

            // Determine boolean conditions
            boolean hasButton = target.getOriginalTagName().equalsIgnoreCase("button")
                    && isClickable(targetElem, tagNameDefined)
                    && !textLabel.isBlank();
            boolean hasAriaLabel = isValidString(ariaLabelValue);
            boolean hasInnerHTML = isValidString(innerHTMLValue) && !hasButton;
            boolean hasInnerHTMLTag = hasInnerHTML && (innerHTMLValue.contains("<") || innerHTMLValue.contains(">"));
            boolean hasFormControlName = isValidString(formControlNameAttributeValue);
            boolean hasTestId = isValidString(testIdAttributeValue);
            boolean hasName = isValidString(nameAttributeValue);
            boolean hasId = isValidString(idAttributeValue) && !hasButton;
            boolean hasValue = isValidString(valueAttributeValue);
            boolean hasHRefFile = isValidString(valueHRefFile);
            boolean hasParagraph = !Strings.isNullOrEmpty(textLabel)
                    && target.getOriginalTagName().equalsIgnoreCase("p");
            boolean hasSpan = !Strings.isNullOrEmpty(textLabel)
                    && target.getOriginalTagName().equalsIgnoreCase("span");
            boolean hasDiv = !Strings.isNullOrEmpty(textLabel)
                    && target.getOriginalTagName().equalsIgnoreCase("div");

            boolean isLabel = !Strings.isNullOrEmpty(labelAttributeValue);
            boolean isForLabel = !Strings.isNullOrEmpty(forLabelAttributeValue);

            boolean isElementHidden;
            try {
                isElementHidden = extractAttribute(targetElem, WebElementAttributeEnum.TYPE) != null
                        && extractAttribute(targetElem, WebElementAttributeEnum.TYPE)
                                .equalsIgnoreCase("hidden");

            } catch (Exception ignored) {
                isElementHidden = false;
            }

            target.setIsElementHidden(isElementHidden);

            // Set nameLabel and nameField based on conditions
            if (!Strings.isNullOrEmpty(target.getSomeText())) {
                String truncatedSomeText = truncateAndNormalize(target.getSomeText(), 30);
                target = setElementText(target, truncatedSomeText, truncatedSomeText);
            } else if (isLabel) {
                target = setElementText(target, labelAttributeValue, labelAttributeValue);
            } else if (isForLabel) {
                target = setElementText(target, forLabelAttributeValue, forLabelAttributeValue);
            } else if (isOption && hasValue) {
                target = setElementText(target, valueAttributeValue, valueAttributeValue);
            } else if (hasFormControlName) {
                target = setElementText(target, formControlNameAttributeValue, formControlNameAttributeValue);
            } else if (hasTestId) {
                target = setElementText(target, testIdAttributeValue, testIdAttributeValue);
            } else if (hasName) {
                target = setElementText(target, nameAttributeValue, nameAttributeValue);
            } else if (hasAriaLabel) {
                target = setElementText(target, ariaLabelValue, ariaLabelValue);
            } else if (isAnchor && hasInnerHTML && !hasInnerHTMLTag) {
                target = setElementText(target, innerHTMLValue, innerHTMLValue);
            } else if (hasId) {
                target = setElementText(target, idAttributeValue, idAttributeValue);
            } else if (hasHRefFile) {
                target = setElementText(target, valueHRefFile + " File", valueHRefFile + " File");
            } else if (hasParagraph) {
                target = setElementText(target, textLabel, tagNameDefined);
            } else if (hasButton) {
                target = setElementText(target, textLabel, tagNameDefined);
            } else if (hasSpan) {
                target = setElementText(target, textLabel, tagNameDefined);
            } else if (hasDiv) {
                target = setElementText(target, textLabel, tagNameDefined);
            } else if (!Strings.isNullOrEmpty(textLabel) && !Strings.isNullOrEmpty(tagNameDefined)) {
                target = setElementText(target, textLabel, tagNameDefined);
            } else if (!Strings.isNullOrEmpty(hasMatLabelValue)) {
                target = setElementText(target, hasMatLabelValue, hasMatLabelValue);
            } else if (!Strings.isNullOrEmpty(hasMatInputValue)) {
                target = setElementText(target, hasMatInputValue, hasMatInputValue);
            } else if (!Strings.isNullOrEmpty(hasInputValue)) {
                target = setElementText(target, hasInputValue, hasInputValue);
            } else if (!Strings.isNullOrEmpty(dataTestIdAttributeValue)) {
                target = setElementText(target, dataTestIdAttributeValue, dataTestIdAttributeValue);
            } else if (!Strings.isNullOrEmpty(titleAttributeValue)) {
                target = setElementText(target, titleAttributeValue, titleAttributeValue);
            } else if (!Strings.isNullOrEmpty(tagNameDefined) && tagNameDefined.equalsIgnoreCase("iFrame")) {
                target = setElementText(target, target.getOriginalTagName(), tagNameDefined);
            } else {
                //                if (tagNameDefined.equalsIgnoreCase("input")) {
                //                    target.setTagType(WebElementTagNameEnum.INPUT);
                //                }else  if (tagNameDefined.equalsIgnoreCase("input")) {
                //                    target.setTagType(WebElementTagNameEnum.OUTPUT);
                //                } else  if (tagNameDefined.equalsIgnoreCase("input")) {
                //                    target.setTagType(WebElementTagNameEnum.OUTPUT);
                //                }
                target = setElementText(
                        target, target.getOriginalTagName(), ARConstants.DEFAULT_VALUE_NO_IDENTIFICATION);
            }

        } catch (Exception e) {
            ARLogger.getInstance(PerformActions.class).fine("Error defineTargetNameTitles");
        }
        return target;
    }

    private TargetElement setElementText(TargetElement target, String nameLabelText, String nameFieldText) {
        target.setNameLabel(nameLabelText);
        target.setNameField(nameFieldText);
        return target;
    }

    public TargetElement defineTagType(TargetElement targetTagType) {

        try {
            System.out.println("Defined Name: " + targetTagType.getDefinedName());
            System.out.println("Tag Name: " + targetTagType.getOriginalTagName());
            System.out.println("Id: " + targetTagType.getAttribId());
            System.out.println("Name: " + targetTagType.getAttribName());
            System.out.println("xPath: " + targetTagType.getCurrentXPath());
            System.out.println("Absolut xPath: " + targetTagType.getAllAttributes());
            System.out.println("Custom xPath: " + targetTagType.getCustomXPath());
            System.out.println("iFrame xPath: " + targetTagType.getIFrameXPath());

            if (targetTagType.getCoords() != null) {
                String[] coords = targetTagType.getCoords().split(",");
                if (coords.length == 2) {
                    String coordLeft = coords[0].trim();
                    String coordRight = coords[1].trim();
                    // Print or use the extracted values
                    System.out.println("CoordLeft: " + coordLeft);
                    System.out.println("CoordRight: " + coordRight);
                }
            }

            String tagName = targetTagType.getDefinedName();

            // Here I am forcing as Button "CLICKABLE" or "IMPUTABLE"
            if (tagName.equalsIgnoreCase(WebElementTagNameEnum.BUTTON.getValue())
                    || tagName.equalsIgnoreCase(WebElementTagNameEnum.ANCHOR.getValue())
                    || tagName.equalsIgnoreCase(WebElementTagNameEnum.DIV.getValue())
                    || tagName.equalsIgnoreCase(WebElementTagNameEnum.OPTION.getValue())
                    || tagName.equalsIgnoreCase(WebElementTagNameEnum.MAT_SELECT.getValue())) {
                targetTagType.setTagType(WebElementTagNameEnum.BUTTON);
                targetTagType.setIconType(WebElementIcon.CLICK);
            } else if (tagName.equalsIgnoreCase(WebElementTagNameEnum.INPUT.getValue())
                    || tagName.equalsIgnoreCase(WebElementTagNameEnum.TEXT_AREA.getValue())) {
                targetTagType.setTagType(WebElementTagNameEnum.INPUT);
                targetTagType.setIconType(WebElementIcon.INSERT);
            } else {
                targetTagType.setTagType(WebElementTagNameEnum.ALL);
                targetTagType.setIconType(WebElementIcon.TEXT);
            }

            return targetTagType;

        } catch (Exception ex) {
            ARLogger.getInstance(PerformActions.class)
                    .severe("Could not find any Web Element with XPath/Id/Attributes values.");
        }
        return null;
    }

    /**
     * Extracts the file extension from the given string, considering it may be a path.
     *
     * @param input The string from which to extract the file extension.
     * @return The file extension if present and the string is identified as a file, otherwise an empty string.
     */
    public static String extractFileExtension(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        // Find the last slash in the string
        int lastIndexOfSlash = input.lastIndexOf('/');

        // Get the substring after the last slash
        String lastSegment = lastIndexOfSlash == -1 ? input : input.substring(lastIndexOfSlash + 1);

        // If the last segment contains a period, it is considered a file
        int lastIndexOfDot = lastSegment.lastIndexOf('.');
        if (lastIndexOfDot == -1 || lastIndexOfDot == lastSegment.length() - 1) {
            return "";
        }

        // Extract the substring after the last period
        return lastSegment.substring(lastIndexOfDot + 1);
    }

    private boolean isValidString(String value) {
        return value != null && !value.isBlank();
    }

    private boolean isClickable(WebElement element, String tagNameDefined) {
        List<WebElementTagNameEnum> clickableTags = WebElementTagNameEnum.clickableTags();
        boolean isClickableTag =
                clickableTags.stream().anyMatch(t -> t.getValue().equalsIgnoreCase(tagNameDefined));
        List<WebElementAttributeTypeValueEnum> clickableValues = WebElementAttributeTypeValueEnum.getClickableValues();
        boolean isClickableValue = clickableValues.stream().anyMatch(v -> v.getValue()
                .equalsIgnoreCase(element.getAttribute(WebElementAttributeEnum.TYPE.getValue())));
        boolean isInputTag = tagNameDefined.equalsIgnoreCase(WebElementTagNameEnum.INPUT.getValue());
        return (isClickableTag && !isInputTag) || (isInputTag && isClickableValue && isClickableTag);
    }

    public WebElement findElementByXPaths(List<String> xpaths, WebDriver driver) {
        JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;

        for (String xpath : xpaths) {
            try {
                Object result = jsExecutor.executeScript("return document.evaluate(\"" + xpath
                        + "\", document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;");
                if (result instanceof WebElement) {
                    return (WebElement) result;
                }
            } catch (Exception e) {
                // Log or handle the exception if needed
                System.err.println("Error locating element with XPath: " + xpath + ". Exception: " + e.getMessage());
            }
        }
        return null;
    }

    public void highlightElement(JavascriptExecutor jsExecutor, WebElement previousElement, WebElement currentElement) {
        // Reset background color of the previous element
        try {
            if (previousElement != null) {
                jsExecutor.executeScript("arguments[0].style.backgroundColor = '';", previousElement);
            }

            // Highlight the current element
            if (currentElement != null) {
                jsExecutor.executeScript("arguments[0].style.backgroundColor = 'red';", currentElement);
            }
        } catch (Exception error) {

        }
    }

    public static WebElement findElementByID(WebDriver driver, String elementID) {
        JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;
        jsExecutor = (JavascriptExecutor) driver;
        return (WebElement) jsExecutor.executeScript("return document.getElementById(arguments[0]);", elementID);
    }

    public static WebElement findElementsByName(WebDriver driver, String elementName) {
        JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;
        jsExecutor = (JavascriptExecutor) driver;
        return (WebElement)
                jsExecutor.executeScript("return document.getElementsByName(arguments[0])[0];", elementName);
    }

    public static WebElement findElementByAttributeParams(
            WebDriver driver, String attributeName, String attributeValue) {

        attributeName = attributeName.trim().replaceAll("^\"|\"$", "");
        attributeValue = attributeValue.trim().replaceAll("^\"|\"$", "");

        JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;
        try {
            // Remove extra quotes around the attribute name and value before passing them to JavaScript
            return (WebElement) jsExecutor.executeScript(
                    "return document.querySelector('[\"' + arguments[0] + '\"]' + '=\"' + arguments[1] + '\"]');",
                    attributeName.trim(),
                    attributeValue.trim());
        } catch (Exception ignore) {
        }
        return null;
    }

    public static String extractAttribute(WebElement element, WebElementAttributeEnum attributeEnum) {
        return element.getAttribute(attributeEnum.getValue());
    }
}
