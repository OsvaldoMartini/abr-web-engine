package com.allinweb.ch.facade;

import com.allinweb.ch.builder.WebElementAttributeEnum;
import com.allinweb.ch.builder.WebElementAttributeTypeValueEnum;
import com.allinweb.ch.builder.WebElementIcon;
import com.allinweb.ch.builder.WebElementTagNameEnum;
import com.allinweb.ch.model.*;
import com.allinweb.ch.readersAndWriters.ExcelWriter;
import com.allinweb.ch.util.*;
import com.google.common.base.Strings;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PerformActions.
 *
 * @author Osvaldo Martini
 * @version 1.0
 */
@Slf4j
public class PerformActions {
    private static final Logger logOperations = LoggerFactory.getLogger("com.allinweb.operations");

    //    private static final AndroidDevice androidDevice = AndroidDevice.getInstance();
    private static final PerformMessage performMessage;
    private static final IframeInputLocator iframeInputLocator;
    private static final ARPropertyManager arPropertyManager;
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 30;
    private static final Random RANDOM = new Random();
    private static final DateTimeFormatter FORMAT_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    public static Wait<WebDriver> waitForPage;
    public static Wait<WebDriver> waitForAction;
    // Static final variable to hold the singleton instance
    protected static volatile PerformActions instance;
    private static JavascriptExecutor jsExecutor;

    static {
        arPropertyManager = ARPropertyManager.getInstance();
        performMessage = PerformMessage.getInstance();
        iframeInputLocator = IframeInputLocator.getInstance();
    }

    public List<String> windowHandlesList = new ArrayList<>();
    public int currentTabIndex = 0; // Track the currently active tab index

    @Getter
    long totalExecutionTime = 0;

    private AtomicBoolean interceptBotJob = new AtomicBoolean(false);
    private ARPriorities arPriorities;

    private static final String DEFAULT_LOCATOR_PRIORITIES =
            "1,xpath,currentXPath" + System.lineSeparator() + "2,xpath,xpath"
                    + System.lineSeparator() + "3,xpath"
                    + System.lineSeparator() + "4,ById,locator.best.byId"
                    + System.lineSeparator() + "5,ByName,locator.best.byName"
                    + System.lineSeparator() + "6,ByCssSelector,locator.css.id"
                    + System.lineSeparator() + "7,ByCssSelector,locator.css.tagId"
                    + System.lineSeparator() + "8,ByCssSelector,locator.css.name"
                    + System.lineSeparator() + "9,ByCssSelector,locator.css.generated"
                    + System.lineSeparator() + "10,xpath,locator.xpath.id"
                    + System.lineSeparator() + "11,xpath,locator.xpath.name"
                    + System.lineSeparator() + "12,xpath,locator.xpath.nameType"
                    + System.lineSeparator() + "13,attributeID,attributeID"
                    + System.lineSeparator() + "14,attributeName,attributeName"
                    + System.lineSeparator() + "15,searchAttribute,searchAttribute"
                    + System.lineSeparator() + "16,coordinates,coordinates"
                    + System.lineSeparator() + "17,attribute,test-id"
                    + System.lineSeparator();

    @Getter
    @Setter
    private WebDriver currentDriver;

    private Map<WebElement, List<WebElement>> iframeElementsMap;

    @Getter
    @Setter
    private boolean justCalledRefreshPage = false;

    /** Called after page refresh/navigation to re-inject plugins (e.g. actionExecutor). */
    @Setter
    private Runnable onPageRefresh;

    /** Called to re-inject the actionExecutor plugin when it's not alive in the browser. */
    @Setter
    private Runnable actionExecutorInjector;

    // Private constructor to prevent instantiation
    private PerformActions() {}

    // Public method to access the singleton instance
    public static PerformActions getInstance() {
        if (instance == null) {
            synchronized (PerformActions.class) {
                if (instance == null) {
                    instance = new PerformActions();
                }
            }
        }
        return instance;
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

    public static String convertToCssSelector(String tagName, List<String> priorityToSearch, String attributeValue) {

        for (String priority : priorityToSearch) {
            priority = priority.trim();
            String attributeName;

            if (priority.equalsIgnoreCase("attributeID")) {
                attributeName = "id";
            } else if (priority.equalsIgnoreCase("attributeName")) {
                attributeName = "name";
            } else {
                attributeName = priority; // Use the priority as the attribute name for other cases
            }

            // Create the CSS selector string and add it to the list
            return tagName + "[" + attributeName + "='" + attributeValue.trim() + "']";
        }

        return null;
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

    public static FieldData insertRandomName(String key) {
        String randomName = generateRandomName();
        return new FieldData(key, randomName);
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

    // Function to check if the element is visible
    private static boolean isElementVisible(WebElement element, WebDriver driver) {
        // Check if the element is displayed and within the viewport
        try {
            return element.isDisplayed() && isInViewport(element, driver);
        } catch (Exception e) {
            logOperations.info(e.getMessage());
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

    public static String insertValueIFrameElement(
            WebDriver driver, String iframeXPath, String inputXPath, String inputValue) {
        jsExecutor = (JavascriptExecutor) driver;

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

        jsExecutor = (JavascriptExecutor) driver;

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

    public static WebElement findElementByID(WebDriver driver, String elementID) {
        jsExecutor = (JavascriptExecutor) driver;
        jsExecutor = (JavascriptExecutor) driver;
        return (WebElement) jsExecutor.executeScript("return document.getElementById(arguments[0]);", elementID);
    }

    public static WebElement findElementsByName(WebDriver driver, String elementName) {
        jsExecutor = (JavascriptExecutor) driver;
        jsExecutor = (JavascriptExecutor) driver;
        return (WebElement)
                jsExecutor.executeScript("return document.getElementsByName(arguments[0])[0];", elementName);
    }

    public static WebElement findElementByAttributeParams(
            WebDriver driver, String attributeName, String attributeValue) {

        attributeName = attributeName.trim().replaceAll("^\"|\"$", "");
        attributeValue = attributeValue.trim().replaceAll("^\"|\"$", "");

        jsExecutor = (JavascriptExecutor) driver;
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

    private static String insertGroupingSeparators(String number, String separator) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = number.length() - 1; i >= 0; i--) {
            sb.insert(0, number.charAt(i));
            count++;
            if (count % 3 == 0 && i != 0) {
                sb.insert(0, separator);
            }
        }
        return sb.toString();
    }

    public AtomicBoolean interceptBotJobProperty() {
        return interceptBotJob;
    }

    public boolean isInterceptBotJob() {
        return interceptBotJob.get();
    }

    public void setInterceptBotJob(boolean value) {
        interceptBotJob.set(value);
    }

    public void initialize(ARPriorities arPriorities) {
        this.arPriorities = arPriorities;
    }

    public WebElement searchElement(
            InstructionLoad instruction, int botJobId, boolean forceCoordinates, boolean byPassFlagLoop) {
        WebElement instructionElement = null;

        if (!StringUtils.isBlank(instruction.getXpath())) {
            instructionElement = locateElement(instruction, botJobId, forceCoordinates, byPassFlagLoop);
        }
        return instructionElement;
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
            FieldData data,
            InstructionLoad currentInstruction,
            Map<String, String> mapOperators,
            WebElement instructionElement,
            String[] actions,
            boolean isMobileApp,
            SplitDTO splitDTO)
            throws Exception {

        // Ensure actionExecutor plugin is alive before executing any action
        ensureActionExecutor();

        WebDriver originalDriver = this.currentDriver; // Save the original WebDriver state
        boolean switchedToIframe = false;

        try {
            String xPath = currentInstruction.getXpath().toLowerCase();
            if (currentInstruction.getXpath() != null && xPath.contains("iframe")) {
                // Locate and switch to the iframe
                WebElement iframeElement = this.currentDriver.findElement(By.xpath(xPath));
                WebDriver driver = this.currentDriver.switchTo().frame(iframeElement);

                setCurrentDriver(driver);
                switchedToIframe = true;
            }

            // The legacy "I:E:..." token in actions is gone; Enter is now a bit in
            // force_coordinates. See InputFlags + migration 2026-04-26.
            Boolean pressEnterAfter =
                    InputFlags.of(currentInstruction.getForceCoordinates()).hasEnter();

            if (instructionElement != null) {
                boolean passed = true;
                switch (actions[0]) {
                    case ARConstantsEngine.VISUALIZE:
                        passed = scrollToElement(byPassNotFound, instructionElement);

                        if (!passed) {
                            // Try by coordinates
                            FieldData filedData = new FieldData("&EMPTY", "&EMPTY");
                            passed = executeActionsAtCoordinates(
                                    savedCoordinates, filedData, ARConstantsEngine.VISUALIZE, pressEnterAfter);
                        }
                        return passed;
                    case ARConstantsEngine.OUTPUT:
                        String fieldName = currentInstruction.getId() + "-" + currentInstruction.getName();
                        String valueElem = getOutPutElement(
                                byPassNotFound,
                                instructionElement,
                                fieldName,
                                currentInstruction.getActions(),
                                mapOperators);

                        return !Strings.isNullOrEmpty(valueElem);
                    case ARConstantsEngine.CLICK:
                    case ARConstantsEngine.OTHER:
                        if (isMobileApp) {
                            //                            androidDevice.executeAction(instructionElement, splitDTO);
                        } else {
                            try {
                                passed = clickElement(byPassNotFound, instructionElement);
                            } catch (Exception clickEx) {
                                logOperations.warn(
                                        "clickElement threw: {} - trying actionExecutor", clickEx.getMessage());
                                passed = false;
                            }
                            if (!passed) {
                                // Fallback: try via actionExecutor (JS in browser, no visibility checks)
                                passed = tryActionExecutor("click", currentInstruction, null);
                            }
                        }
                        return passed;
                    case ARConstantsEngine.INSERT:
                        if ("select".equalsIgnoreCase(instructionElement.getTagName())) {
                            if (isMobileApp) {
                                //                                androidDevice.executeAction(instructionElement,
                                // splitDTO, null, data.getValue());
                            } else {
                                try {
                                    passed = insertDataInSelectElement(
                                            byPassNotFound,
                                            instructionElement,
                                            savedCoordinates,
                                            data,
                                            pressEnterAfter);
                                } catch (Exception selectEx) {
                                    logOperations.warn(
                                            "Selenium select threw: {} - trying fallbacks", selectEx.getMessage());
                                    passed = false;
                                }

                                if (!passed) {
                                    // Fallback: try via actionExecutor (JS in browser)
                                    passed = tryActionExecutor("select", currentInstruction, data.getValue());
                                }
                                if (!passed) {
                                    // Last resort: try by coordinates
                                    passed = executeActionsAtCoordinates(
                                            savedCoordinates, data, ARConstantsEngine.SELECT, pressEnterAfter);
                                }
                                return passed;
                            }
                        } else {
                            if (isMobileApp) {
                                //                                androidDevice.executeAction(instructionElement,
                                // splitDTO, null, data.getValue());
                            } else {
                                try {
                                    //                            instructionElement.click();
                                    instructionElement.clear();
                                    clearElement(instructionElement);
                                    //                            clearValueAtCoordinates(savedCoordinates);

                                    passed = insertInElement(
                                            byPassNotFound,
                                            instructionElement,
                                            data.getValue(),
                                            currentInstruction.getDefaultValue(),
                                            currentInstruction.getCodified(),
                                            InputFlags.ofLegacy(
                                                    currentInstruction.getForceCoordinates(), pressEnterAfter));
                                } catch (Exception insertEx) {
                                    logOperations.warn(
                                            "Selenium insert threw: {} - trying fallbacks", insertEx.getMessage());
                                    passed = false;
                                }

                                if (!passed) {
                                    // Fallback: try via actionExecutor (JS in browser)
                                    passed = tryActionExecutor("type", currentInstruction, data.getValue());
                                }
                                if (!passed) {
                                    // Last resort: try by coordinates
                                    passed = executeActionsAtCoordinates(
                                            savedCoordinates, data, ARConstantsEngine.INSERT, pressEnterAfter);
                                }
                                return passed;
                            }
                        }
                }

                onHoldForSeconds(null);
            }

            return true;
        } finally {
            // Restore the original WebDriver state
            if (switchedToIframe) {
                setCurrentDriver(originalDriver);
            }
        }
    }

    /**
     * Check if the actionExecutor JS plugin is alive in the browser.
     * If not, re-inject it via the callback set by ARScannedElementPane.
     * Called before every action step to ensure the plugin is always available.
     */
    public void ensureActionExecutor() {
        if (currentDriver == null || actionExecutorInjector == null) return;

        try {
            JavascriptExecutor js = (JavascriptExecutor) currentDriver;
            Object alive = js.executeScript("return window.__actionExecutorActive === true;");
            if (Boolean.TRUE.equals(alive)) return;

            logOperations.info("actionExecutor not alive in browser - re-injecting");
            actionExecutorInjector.run();
        } catch (Exception e) {
            logOperations.warn("ensureActionExecutor check failed: {} - re-injecting", e.getMessage());
            try {
                actionExecutorInjector.run();
            } catch (Exception re) {
                logOperations.warn("actionExecutor re-injection failed: {}", re.getMessage());
            }
        }
    }

    /**
     * Fallback: send an action command to the injected actionExecutor JS plugin
     * via WebSocket.  The browser executes it directly in DOM context -
     * no Selenium visibility / pointer-events checks.
     *
     * @param action      "click", "type", "select", "clear", etc.
     * @param instruction the current instruction (provides xPath, cssSelector, coordinates, attribId)
     * @param value       the value to type or select (nullable)
     * @return true if the JS-side action succeeded
     */
    private boolean tryActionExecutor(String action, InstructionLoad instruction, String value) {
        // Make sure the plugin is alive before sending a command
        ensureActionExecutor();

        try {
            ActionExecutorClient client = ActionExecutorClient.getInstance();
            ActionExecutorClient.ActionResult result = client.sendAction(
                    action,
                    instruction.getXpath(),
                    instruction.getCssSelector(),
                    instruction.getCoordinates(),
                    null, // attribId not on InstructionLoad; JS will fallback to xPath/css/coords
                    value);

            if (result.isSuccess()) {
                logOperations.info(
                        "actionExecutor fallback succeeded: {} - {} (verified={})",
                        action,
                        result.getMessage(),
                        result.isVerified());
                return true;
            } else {
                logOperations.warn(
                        "actionExecutor fallback failed: {} - {} (verified={})",
                        action,
                        result.getMessage(),
                        result.isVerified());
                return false;
            }
        } catch (Exception e) {
            logOperations.warn("actionExecutor fallback error: {} - {}", action, e.getMessage());
            return false;
        }
    }

    public void performOtherActions(boolean byPassNotFound, InstructionLoad instruction, String[] actions)
            throws Exception {

        switch (actions[0]) {
            case ARConstantsEngine.LIST_OPERATION:
                //                listOperation(byPassNotFound, instruction);
                break;
            case ARConstantsEngine.HOLD:
            case ARConstantsEngine.REFRESH_HOLD:
                //                        executeAlert(instruction);
                onHoldForSeconds(instruction);
                break;
            case ARConstantsEngine.REFRESH_ONLY:
            case ARConstantsEngine.REFRESH_LOOP:
                refreshPage();
                break;
            case ARConstantsEngine.QUIT:
                // Minimal confirmation using your custom modal
                ARExecution.DialogModal respModal = performMessage.showCustomModalDialogDragWin11(
                        "Confirmation",
                        "Do you want to continue?",
                        "This Action Closes the Browser and Scanner!",
                        null,
                        null,
                        true,
                        "OK",
                        "Close Browser",
                        350);

                if (respModal.equals(ARExecution.DialogModal.STOP)) {
                    quit(1);
                }

                break;
                //                    case ARConstants.EXTRACT:
                //                        result = "insertValueFieldNameInExcel-->"
                //                                + insertValueFieldNameInExcel(instructionElement, instruction,
                // action, blockJobName);
                //                        break;
            case ARConstantsEngine.SCREEN:
                break;
        }

        onHoldForSeconds(null);
    }

    public String performOperatorActions(
            boolean byPassNotFound,
            InstructionLoad instruction,
            String targetXPath,
            String[] parentOperations,
            String action,
            String[] operations,
            String parentField,
            String variableField,
            Map<String, String> mapOperators,
            WebElement instructionElement) {

        try {
            onHoldInSeconds(1);
        } catch (Exception ignore) {
        }

        if (!StringUtils.isBlank(targetXPath) && instructionElement == null) {
            instructionElement =
                    locateTargetElement(byPassNotFound, targetXPath, instruction.getActionCustomMaxWaitSec());
        }

        String msgReturn = "Error performing GET or SET";
        boolean success = false;

        if (instructionElement != null) {
            try {
                switch (action) {
                    case "SET":
                        msgReturn = "SET_VALUE to (Parent: " + parentField + ") Var:" + variableField + " <-- "
                                + operations[1];
                        insertTargetElement(byPassNotFound, instructionElement, operations[0], operations[1]);
                        mapOperators.put(variableField.trim(), operations[1].trim());
                        success = true;
                        break;

                    case "GET":
                        String valueElem;
                        msgReturn = "GET_VALUE from (Parent: " + parentField + ") Var" + variableField;
                        if (parentOperations[0].equals(ARConstantsEngine.OUTPUT)) {
                            valueElem = getOutPutElement(
                                    byPassNotFound,
                                    instructionElement,
                                    parentField,
                                    instruction.getActions(),
                                    mapOperators);
                        } else {
                            valueElem = getValueInElement(byPassNotFound, instructionElement);
                        }
                        if (!Strings.isNullOrEmpty(valueElem)) {
                            msgReturn += " <-- " + valueElem;
                        }
                        mapOperators.put(variableField.trim(), valueElem.trim());
                        success = true;
                        break;

                    case "CopyVar":
                        String valueVar = mapOperators.getOrDefault(variableField, "");
                        msgReturn =
                                "COPY_VAR from (Parent: " + parentField + ") Var" + variableField + " <-- " + valueVar;
                        success = true;
                        break;
                }
                onHoldForSeconds(null);

            } catch (Exception error) {
                switch (action) {
                    case "SET":
                        msgReturn = String.format(
                                "SET failed - unable to set value \"%s\". Verify that the target element is correct, visible, and editable.",
                                "\"" + operations[1] + "\"");
                        break;
                }
            }
        } else {
            msgReturn = "Error: Instruction is null";
        }

        // =========================
        // 🔹 ONLY NEW PART (RETURN)
        // =========================

        String time = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());

        String testName = "";
        String mainField = "";

        if (parentField != null && parentField.contains("-")) {
            int idx = parentField.indexOf('-');
            testName = parentField.substring(0, idx).trim();
            mainField = parentField.substring(idx + 1).trim();
        } else {
            mainField = parentField == null ? "" : parentField;
        }

        String desc = instruction != null && instruction.getName() != null ? instruction.getName() : "";
        String result = success ? "PASSED" : "FAIL";
        String conditionText = msgReturn;

        return time + " | " + testName + " | " + desc + " | " + mainField + " | " + conditionText + " | " + result;
    }

    private WebElement locateTargetElement(boolean byPassNotFound, String targetXPath, Integer actionCustomMaxWaitSec) {

        String tagName = null;
        try {
            tagName = removeTrailingSlash(targetXPath);
            tagName = extractTagName(targetXPath);
        } catch (Exception e) {

            logOperations.info(String.format(
                    "Error RemoveTrailingSlash for %s -> xPath  %s -> Cause: %s",
                    tagName, targetXPath, e.getMessage()));
        }

        waitPage();

        WebElement elementFound = null;
        List<By> criterias = Arrays.asList(new By[] {By.xpath(targetXPath)});

        // Actually here is Calling the Actions
        if (criterias != null) {

            for (By criteria : criterias) {
                List<WebElement> foundElementList = this.currentDriver.findElements(criteria);

                if (foundElementList != null && foundElementList.size() > 0) {
                    if (justCalledRefreshPage) {
                        justCalledRefreshPage = false;
                        try {
                            waitForPage.until(ExpectedConditions.visibilityOfElementLocated(criteria));
                        } catch (Exception e) {

                            logOperations.warn(String.format(
                                    "Could Not Find xPath \"%s\" Criteria \"%s\" -> Cause: %s",
                                    targetXPath, criteria, e.getMessage()));

                            if (!byPassNotFound) {
                                performMessage.couldNotFindElement(String.valueOf(criteria));
                            }
                        }
                    } else if (actionCustomMaxWaitSec != null) {
                        try {
                            new WebDriverWait(this.currentDriver, Duration.ofSeconds(actionCustomMaxWaitSec))
                                    .until(ExpectedConditions.presenceOfElementLocated(criteria));
                        } catch (Exception e) {
                            logOperations.warn(String.format(
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

                            logOperations.warn(String.format(
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
                "Continue",
                "Stop all",
                0);
    }

    private void showNotFoundElement(String targetXPath, By criteria) {}

    private WebElement locateElement(
            InstructionLoad currentInstruction, int botJobId, boolean forceCoordinates, boolean byPassFlagLoop) {

        String instructionPath = currentInstruction.getXpath();
        String tagName = null;

        WebDriverWait waitLocator = new WebDriverWait(getCurrentDriver(), Duration.ofSeconds(0));

        this.currentDriver.switchTo().defaultContent();
        if (this.currentDriver.getWindowHandles().size() > 1) {
            try {
                this.currentDriver.switchTo().window(windowHandlesList.get(currentTabIndex));
            } catch (Exception ignore) {
            }
        }

        try {
            tagName = extractTagName(removeTrailingSlash(instructionPath));
        } catch (Exception e) {
            logOperations.warn(String.format(
                    "Error RemoveTrailingSlash for %s -> xPath %s -> Cause: %s",
                    tagName, instructionPath, e.getMessage()));
        }

        List<ReferenceLoadDTO> instructionReferenceList = currentInstruction.getReferenceLoadDTOList();

        if (instructionReferenceList.isEmpty()) {
            logOperations.warn("#### Not XPath to Be Located! ####");
            return null;
        }

        //        waitPage();

        //        if (arPriorities.getJobId() == null || !arPriorities.getJobId().equals(botJobId)) {
        //            arPriorities.setJobId(botJobId);
        //            if (currentInstruction.getPriority() != null) {
        //                arPriorities.loadPrioritiesFromString(currentInstruction.getPriority());
        //            } else {
        //                arPriorities.loadPriorities();
        //            }
        //        }

        if (arPriorities.getAllPriorityList() == null
                || arPriorities.getAllPriorityList().isEmpty()
                || arPriorities.getAllPriorityList().size() < 15) {
            arPriorities.loadPrioritiesFromString(DEFAULT_LOCATOR_PRIORITIES);
        }

        WebElement elementFound = null;

        if (!Strings.isNullOrEmpty(currentInstruction.getIFrameXPath())) {
            try {
                WebElement iframe = this.currentDriver.findElement(By.xpath(currentInstruction.getIFrameXPath()));
                this.currentDriver.switchTo().frame(iframe);
            } catch (Exception e) {
                logOperations.warn("iFrame Not Found: " + currentInstruction.getIFrameXPath());
                return null;
            }
        }

        if (!Strings.isNullOrEmpty(currentInstruction.getShadowHost())
                && !Strings.isNullOrEmpty(currentInstruction.getCssSelector())) {
            elementFound = findShadowElementByCssSelector(
                    currentInstruction.getShadowHost(), currentInstruction.getCssSelector());
        }

        int attempts = 0;
        int maxAttempts = forceCoordinates || byPassFlagLoop ? 2 : 4;

        while (elementFound == null && attempts < maxAttempts) {

            for (Priority priority : arPriorities.getAllPriorityList()) {
                if (elementFound != null) break;

                PriorityTypeEnum priorityTypeEnum;
                try {
                    priorityTypeEnum = priority.getPriorityType(); // already returns enum
                } catch (Exception e) {
                    continue;
                }

                // ✅ IMPORTANT: try ALL references matching this priority (not only findFirst)
                List<ReferenceLoadDTO> instructionReferences = instructionReferenceList.stream()
                        .filter(ref ->
                                priority.getName().stream().anyMatch(p -> p.equalsIgnoreCase(ref.getReferenceType())))
                        .toList();

                if (instructionReferences.isEmpty()) {
                    continue;
                }

                for (ReferenceLoadDTO ref : instructionReferences) {
                    if (elementFound != null) break;

                    List<By> criterias = null;
                    String value = ref.getValue();

                    switch (priorityTypeEnum) {
                        case xpath -> criterias = List.of(By.xpath(value));

                        case ById -> criterias = List.of(By.id(normalizeLocatorValue(ref.getReferenceType(), value)));

                        case ByName -> criterias =
                                List.of(By.name(normalizeLocatorValue(ref.getReferenceType(), value)));

                        case ByCssSelector -> criterias =
                                List.of(By.cssSelector(normalizeLocatorValue(ref.getReferenceType(), value)));

                        case ByClassName -> criterias = List.of(By.className(value));
                        case ByTagName -> criterias = List.of(By.tagName(value));
                        case ByLinkText -> criterias = List.of(By.linkText(value));
                        case ByPartialLinkText -> criterias = List.of(By.partialLinkText(value));

                        case attribute, attributeID, attributeName, searchAttribute -> criterias =
                                convertToCriteriaList(tagName, priority.getName(), value);

                        default -> {}
                    }

                    if (criterias == null) continue;

                    for (By criteria : criterias) {

                        List<WebElement> foundElementList = new ArrayList<>();
                        try {
                            waitLocator.until(ExpectedConditions.presenceOfElementLocated(criteria));
                            foundElementList = getCurrentDriver().findElements(criteria);

                            if (!foundElementList.isEmpty()) {
                                elementFound = foundElementList.get(0);
                                break;
                            }
                        } catch (TimeoutException ignored) {
                        } catch (Exception ignored) {
                        }
                    }
                }
            }

            attempts++;
            if (elementFound == null) {
                try {
                    if (isInterceptBotJob()) {
                        break;
                    }
                    //                    onHoldInSeconds(1);

                    logOperations.warn(String.format(
                            "Re-try %d Locate Web Element TagName \"%s\"", attempts, currentInstruction.getName()));

                } catch (Exception e) {
                }
            }
        }

        return elementFound;
    }

    private String normalizeLocatorValue(String referenceType, String value) {
        if (value == null) return null;

        // If DB stores full css/xpath already, use it as-is.
        // If DB stores only the raw id/name, convert where needed.
        switch (referenceType) {
            case "locator.css.id":
                // stored could be "password" or "#password"
                return value.startsWith("#") ? value : "#" + value;

            default:
                return value;
        }
    }

    private String insertTargetElement(
            boolean byPassNotFound, WebElement element, String fieldName, String dataFieldValue) throws Exception {
        UtilsMethods.exceptionIfNullWebElement(element);
        try {
            waitForAction.until(ExpectedConditions.visibilityOf(element));
        } catch (Exception e) {

            logOperations.warn(String.format(
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

            logOperations.warn(
                    String.format("Could Not Find TagName \"%s\" -> Cause: %s", element.getTagName(), e.getMessage()));

            if (!byPassNotFound) {
                performMessage.couldNotFindElement(element.getTagName());
            }
        }

        // Assuming instructionElement is an input field
        return element.getAttribute("value");
    }

    public synchronized String onHoldForSeconds(InstructionLoad instruction) throws Exception {
        if (instruction != null) {
            Integer instructionSeconds = instruction.getOnHoldSeconds();
            if (instructionSeconds != null && instructionSeconds > 0) {
                wait(fromSecondsToMilliseconds(TimeUnit.SECONDS, instructionSeconds));
                return "HOLD" + "->" + instructionSeconds + " seconds";
            } else {
                String stopSeconds = arPropertyManager.getProperty(ARPropertyEnum.INSTRUCTION_STOP_SECONDS);
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

    public void waitPage() {
        WebDriver driver = this.currentDriver;
        if (driver != null) {
            try {

                waitForPage.until(d -> ((JavascriptExecutor) driver)
                        .executeScript("return document.readyState")
                        .equals("complete"));
            } catch (Exception ex) {

                logOperations.warn(String.format(
                        "WaitForPage.until(d -> ((JavascriptExecutor) driver) error: %s", ex.getMessage()));

                performMessage.couldNotFindElement("WaitForPage.until");
            }
        } else {
            // Handle the case when driver is null (e.g., throw an exception or initialize the driver)

            logOperations.warn("WaitForPage.until(d -> ((JavascriptExecutor) driver) is returning nulls");
        }
    }

    public boolean scrollToElement(boolean byPassNotFound, WebElement element) throws Exception {
        try {
            UtilsMethods.exceptionIfNullWebElement(element);
            ((JavascriptExecutor) this.currentDriver).executeScript("arguments[0].scrollIntoView(true);", element);
            return true;
        } catch (Exception e) {

            logOperations.error(String.format(
                    "Failed to Scroll to Element \"%s\" -> Cause: %s", element.getTagName(), e.getMessage()));
            if (!byPassNotFound) {
                performMessage.couldNotFindElement("Failed to Scroll to Element " + element.getTagName());
            }
            return false;
        }
    }

    private String safeTag(WebElement el) {
        try {
            return el.getTagName();
        } catch (Exception ignore) {
            return "unknown";
        }
    }

    public boolean clickElement(boolean byPassNotFound, WebElement element) throws Exception {
        UtilsMethods.exceptionIfNullWebElement(element);

        try {
            // A quick check
            if (element != null && (!element.isEnabled() || !element.isDisplayed())) {
                logOperations.error(
                        "Step Failed - Web Field is not Visible. Verify the rules and behavior of your web page.");
                return false;
            }
            waitForAction.until(ExpectedConditions.visibilityOf(element).andThen(e -> {
                ((JavascriptExecutor) this.currentDriver).executeScript("arguments[0].scrollIntoView(true);", element);
                return waitForAction.until(ExpectedConditions.elementToBeClickable(element));
            }));
        } catch (Exception e) {

            logOperations.error(
                    "Step Failed - Web Field is not Visible. Verify the rules and behavior of your web page.");

            if (!byPassNotFound) {
                performMessage.couldNotFindElement(element.getTagName());
            }
            return false;
        }

        // Custom visibility and enabled checks
        if (!element.isDisplayed()) {
            logOperations.error(
                    "Step Failed - Web Field is not Visible. Verify the rules and behavior of your web page.");
            //            performMessage.errorMessage(
            //                    "BOT JOB STOP - Web Field is not Visible",
            //                    "<span style='color: #D32F2F; font-weight: bold; font-size: 1.1em;'>Verify the rules
            // and behavior of your web page.</span>",
            //                    "<span style='color: #D32F2F; font-weight: bold;'>Some fields may be conditionally
            // enabled based on other inputs.</span>",
            //                    "<span style='color: #E65100; font-weight: bold; font-size: 1.1em;'>Element is present
            // but not visible. It may be hidden or overlapped.</span>",
            //                    "<span style='color: #D32F2F; font-style: italic;'>Example: Invalid IBAN may block
            // branch autofill.</span>",
            //                    0);
            return false;
        }

        if (!element.isEnabled()) {
            //        callErrorMessageNotEnabled(element.getTagName());
            logOperations.error(
                    "Step Failed - Web Field is not Visible. Verify the rules and behavior of your web page.");
            //            performMessage.errorMessage(
            //                    "BOT JOB STOP - Web Field is not Enabled",
            //                    "<span style='color: #D32F2F; font-weight: bold; font-size: 1.1em;'>Verify the rules
            // and behavior of your web page.</span>",
            //                    "<span style='color: #D32F2F; font-weight: bold;'>Some fields may be conditionally
            // enabled based on other inputs.</span>",
            //                    "<span style='color: #E65100; font-weight: bold; font-size: 1.1em;'>It is visually
            // present but cannot be clicked.</span>",
            //                    "<span style='color: #D32F2F; font-style: italic;'>Example: Invalid IBAN may block
            // branch autofill.</span>",
            //                    0);
            //            // throw new TimeoutException();
            return false;
        }

        String pointerEvents = element.getCssValue("pointer-events");
        if ("none".equals(pointerEvents)) {
            //            performMessage.errorMessage(
            //                    "BOT JOB STOP - Web Field is is not Clickable",
            //                    "<span style='color: #D32F2F; font-weight: bold; font-size: 1.1em;'>Verify the rules
            // and behavior of your web page.</span>",
            //                    "<span style='color: #D32F2F; font-weight: bold;'>Some fields may be conditionally
            // enabled based on other inputs.</span>",
            //                    "<span style='color: #E65100; font-weight: bold; font-size: 1.1em;'>It is visually
            // present but cannot be clicked.</span>",
            //                    "<span style='color: #D32F2F; font-style: italic;'>Example: Invalid IBAN may block
            // branch autofill.</span>",
            //                    0);
            logOperations.error(
                    "Step Failed - Web Field is not Visible. Verify the rules and behavior of your web page.");

            return false;
        }

        try {
            element.click();
            return true;
        } catch (ElementClickInterceptedException e) {
            try {
                JavascriptExecutor jse = (JavascriptExecutor) this.currentDriver;
                jse.executeScript("arguments[0].click()", element);
                return true;
            } catch (Exception ex) {

                logOperations.error(
                        "Step Failed - Web Field is not Visible. Verify the rules and behavior of your web page.");
                return false;
            }
        }
    }

    public void refreshPage() {
        justCalledRefreshPage = true;

        this.currentDriver.navigate().refresh();

        this.currentDriver.switchTo().defaultContent();
        if (this.currentDriver.getWindowHandles().size() > 1) {
            try {
                this.currentDriver.switchTo().window(windowHandlesList.get(currentTabIndex));
            } catch (Exception ignore) {

            }
        }

        // Re-inject plugins lost during page reload (actionExecutor, etc.)
        if (onPageRefresh != null) {
            try {
                onPageRefresh.run();
            } catch (Exception e) {
                logOperations.warn("onPageRefresh callback failed: {}", e.getMessage());
            }
        }
    }

    private boolean insertInElement(
            boolean byPassNotFound,
            WebElement element,
            String dataFieldValue,
            String defaultValue,
            boolean isEncrypted,
            InputFlags flags)
            throws Exception {
        UtilsMethods.exceptionIfNullWebElement(element);

        try {
            waitForAction.until(ExpectedConditions.visibilityOf(element));
        } catch (Exception e) {

            logOperations.warn(
                    String.format("Could Not Find TagName \"%s\" -> Cause: %s", element.getTagName(), e.getMessage()));
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
                    // Pause briefly to let JS clearing take effect
                    Thread.sleep(100); // Consider using WebDriverWait for stability
                    // Clear using sendKeys with BACK_SPACE (optional but defensive)
                    element.sendKeys(Keys.chord(Keys.CONTROL, "a"));
                    element.sendKeys(Keys.BACK_SPACE);
                    // Pause again if needed (some inputs behave asynchronously)
                    Thread.sleep(100);

                    element.sendKeys(dataFieldValue);
                    // Waits component reaction
                    onHoldInSeconds(1);
                    pressAfter(element, flags);
                } else {
                    element.sendKeys(UtilsMethods.generateRandomID(10));
                    // Waits component reaction
                    onHoldInSeconds(1);
                    pressAfter(element, flags);
                }
            } else {
                dataFieldValue = defaultValue;

                if (isEncrypted) {
                    dataFieldValue = CryptationAlgorithm.decrypt(dataFieldValue);
                }
                element.sendKeys(dataFieldValue);
                // Waits component reaction
                onHoldInSeconds(1);
                pressAfter(element, flags);
            }
        } catch (Exception e) {

            logOperations.error(String.format(
                    "Could Not Input Value to \"%s\" -> Cause: %s", element.getTagName(), e.getMessage()));

            //            performMessage.couldNotFindElement("Could Input Values to Element " + element.getTagName());
            return false;
        }

        return true;
    }

    // ── Post-input key dispatch ──────────────────────────────────────────────

    /**
     * Fire the post-input keys for the given flag set.
     * <ul>
     *   <li>N solo (no E, no T)        → cascade N → T → E with failure fallback
     *   <li>Any explicit combination   → fire each key in order N, E, T, NO cascade
     *   <li>E alone                    → pressEnterStrong
     *   <li>T alone                    → Keys.TAB
     *   <li>nothing                    → default to TAB (legacy behaviour)
     * </ul>
     */
    private void pressAfter(WebElement element, InputFlags flags) {
        if (flags == null) flags = InputFlags.of(0);
        if (flags.isNextSolo()) {
            pressNextWithFallback(element);
            return;
        }
        boolean anyExplicit = flags.hasNext() || flags.hasEnter() || flags.hasTab();
        if (!anyExplicit) {
            // Legacy default: TAB to move focus and commit the field.
            try {
                element.sendKeys(Keys.TAB);
            } catch (Exception ignored) {
            }
            return;
        }
        if (flags.hasNext()) tryPressNext(element); // explicit combo: no cascade
        if (flags.hasEnter()) pressEnterStrong(element);
        if (flags.hasTab()) {
            try {
                element.sendKeys(Keys.TAB);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Stronger ENTER than bare {@code sendKeys(Keys.ENTER)}.
     *   1) Native sendKeys — best-effort WebDriver keyboard input.
     *   2) JS-dispatched KeyboardEvent (keydown + keypress + keyup) — fires the exact
     *      sequence framework handlers (Angular, React) listen for even when the
     *      WebDriver input stack is intercepted by custom onkeydown handlers.
     *   3) {@code form.requestSubmit()} if the element is inside a &lt;form&gt;.
     */
    private void pressEnterStrong(WebElement element) {
        try {
            element.sendKeys(Keys.ENTER);
        } catch (Exception ignored) {
        }
        try {
            ((JavascriptExecutor) currentDriver)
                    .executeScript(
                            "var el = arguments[0];"
                                    + "var opts = {key:'Enter', code:'Enter', keyCode:13, which:13, bubbles:true, cancelable:true};"
                                    + "el.dispatchEvent(new KeyboardEvent('keydown', opts));"
                                    + "el.dispatchEvent(new KeyboardEvent('keypress', opts));"
                                    + "el.dispatchEvent(new KeyboardEvent('keyup', opts));"
                                    + "try { if (el.form && el.form.requestSubmit) el.form.requestSubmit(); } catch(_) {}",
                            element);
        } catch (Exception e) {
            logOperations.debug("pressEnterStrong JS dispatch failed: {}", e.getMessage());
        }
    }

    /**
     * N-solo cascade: try NEXT, fall back to TAB, finally pressEnterStrong.
     * Fallback triggers on exception OR unchanged focus after the attempt.
     */
    private void pressNextWithFallback(WebElement element) {
        WebElement before = safeActiveElement();
        if (tryPressNext(element) && focusMoved(before)) return;
        if (tryPressTab(element) && focusMoved(before)) return;
        pressEnterStrong(element);
    }

    /**
     * Attempt the platform "Next" action.
     *   • Appium mobile drivers use the on-screen IME "Next" button (accessibility id "Next")
     *     when available; otherwise fall back to a TAB key event.
     *   • Desktop Selenium falls back to a JS focus shift to the next form control.
     * Returns true if the attempt ran without throwing.
     */
    private boolean tryPressNext(WebElement element) {
        try {
            String driverClass =
                    currentDriver == null ? "" : currentDriver.getClass().getSimpleName();
            if (driverClass.contains("Android") || driverClass.contains("IOS") || driverClass.contains("Appium")) {
                try {
                    // Try tapping an on-screen "Next" button (iOS/Android soft keyboards commonly expose this).
                    WebElement nextBtn = currentDriver.findElement(org.openqa.selenium.By.xpath(
                            "//*[@name='Next' or @content-desc='Next' or @accessibility-id='Next']"));
                    nextBtn.click();
                    return true;
                } catch (Exception ignored) {
                    // Fall through to TAB as the platform key proxy.
                    element.sendKeys(Keys.TAB);
                    return true;
                }
            }
            // Desktop: move focus to the next form element via JS.
            ((JavascriptExecutor) currentDriver)
                    .executeScript(
                            "var el = arguments[0], f = el.form;"
                                    + "if (f) { var els = Array.from(f.elements), i = els.indexOf(el);"
                                    + "  for (var k = i + 1; k < els.length; k++) {"
                                    + "    var n = els[k]; if (n && !n.disabled && n.offsetParent !== null) { n.focus(); return; }"
                                    + "  }"
                                    + "}",
                            element);
            return true;
        } catch (Exception e) {
            logOperations.debug("tryPressNext failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean tryPressTab(WebElement element) {
        try {
            element.sendKeys(Keys.TAB);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private WebElement safeActiveElement() {
        try {
            return currentDriver.switchTo().activeElement();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean focusMoved(WebElement before) {
        WebElement after = safeActiveElement();
        if (before == null || after == null) return false;
        try {
            return !before.equals(after);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extracts the dataFieldName and dataFieldValue based on the instruction and DTO.
     */
    /**
     * Extracts the fieldName and fieldValue based on the instruction and DTO.
     */
    public FieldData extractFieldData(
            Map<String, String> data, String[] actions, String defaultValue, boolean isEncrypted) throws Exception {

        String dataFieldName = "";
        String dataFieldValue = "";

        if (data != null) {
            if (actions.length >= 3
                    && actions[0].equals(ARConstantsEngine.INSERT)
                    && actions[1].equals(ARConstantsEngine.ENTER)) {

                dataFieldName = actions[2].split(ARConstantsEngine.PATH_FIELD_SUBSTITUTION)[0];
                dataFieldValue = data.get(dataFieldName);

                if (isEncrypted && dataFieldValue != null) {
                    dataFieldValue = CryptationAlgorithm.decrypt(dataFieldValue);
                }

            } else if (actions.length == 2 && actions[0].equals(ARConstantsEngine.INSERT)) {

                dataFieldName = actions[1].split(ARConstantsEngine.PATH_FIELD_SUBSTITUTION)[0];
                dataFieldValue = data.get(dataFieldName);

                if (isEncrypted && dataFieldValue != null) {
                    dataFieldValue = CryptationAlgorithm.decrypt(dataFieldValue);
                }
            }
        } else if (defaultValue != null && !defaultValue.isEmpty()) {
            dataFieldValue = defaultValue;
            if (isEncrypted) {
                dataFieldValue = CryptationAlgorithm.decrypt(dataFieldValue);
            }
        }

        return new FieldData(dataFieldName, dataFieldValue);
    }

    private boolean insertDataInSelectElement(
            boolean byPassNotFound, WebElement element, String coordinates, FieldData data, boolean pressEnterAfter)
            throws Exception {
        UtilsMethods.exceptionIfNullWebElement(element);
        try {
            waitForAction.until(ExpectedConditions.visibilityOf(element));
        } catch (Exception e) {

            logOperations.warn(String.format(
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
            sequenceOfCommands(
                    element, ARConstantsEngine.SELECT, coordArray, data, this.currentDriver, pressEnterAfter);

        } catch (Exception e) {

            logOperations.error(String.format(
                    "Could Not Input Value to \"%s\" -> Cause: %s", element.getTagName(), e.getMessage()));

            performMessage.couldNotFindElement("Could Input Values to Element " + element.getTagName());

            return false;
        }
        return true;
    }

    private String getOutPutElement(
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

            logOperations.warn(
                    String.format("Could Not Find Field Name \"%s\" -> Cause: %s", fieldName, ex.getMessage()));

            if (!byPassNotFound) {
                performMessage.couldNotFindElement(fieldName);
            }
            return null;
        }

        String textByhJS = "";
        String finalTextNested = "";
        String textAttribute = "";
        String textContext = "";

        try {
            JavascriptExecutor js = (JavascriptExecutor) this.currentDriver;
            textByhJS = (String) js.executeScript("return arguments[0].textContent;", element);
        } catch (Exception ex) {

            logOperations.warn(
                    String.format("By JavascriptExecutor - Not succeeded to get a Text from Label for: %s", fieldName));
        }

        try {
            List<WebElement> children = element.findElements(By.xpath(".//*"));
            StringBuilder textByNested = new StringBuilder();
            for (WebElement child : children) {
                textByNested.append(child.getText()).append(" ");
            }
            finalTextNested = textByNested.toString().trim();
        } catch (Exception ex) {

            logOperations.warn(
                    String.format("By Text Nested - Not succeeded to get a Text from Label for: %s", fieldName));
        }

        try {
            textAttribute = element.getAttribute("value");
        } catch (Exception ex) {

            logOperations.warn(String.format(
                    "By Text Attribute - Not succeeded to get a Text from Label for: %s Operation: %s",
                    fieldName, action));
        }

        try {
            textContext = element.getAttribute("textContent");
        } catch (Exception ex) {

            logOperations.warn(String.format(
                    "By Text Content - Not succeeded to get a Text from Label for: %s Operation: %s",
                    fieldName, action));
        }

        // Check if the element is clickable
        boolean isClickable = false;
        try {
            waitForAction.until(ExpectedConditions.elementToBeClickable(element));
            isClickable = true;
        } catch (Exception e) {

            logOperations.warn(String.format("Element is not clickable: \"%s\"", fieldName));
        }

        // Set the final text value by priority and add to mapOperators
        String finalText = "";

        if (isClickable && finalTextNested != null && !finalTextNested.trim().isEmpty()) {
            finalText = finalTextNested; // Use nested text if the element is clickable
            mapOperators.put(fieldName.trim(), finalText.trim());
        } else if (textByhJS != null && !textByhJS.trim().isEmpty()) {
            finalText = textByhJS;
            mapOperators.put(fieldName.trim(), finalText.trim());
        } else if (finalTextNested != null && !finalTextNested.trim().isEmpty()) {
            finalText = finalTextNested;
            mapOperators.put(fieldName.trim(), finalText.trim());
        } else if (textAttribute != null && !textAttribute.trim().isEmpty()) {
            finalText = textAttribute;
            mapOperators.put(fieldName.trim(), finalText.trim());
        } else if (textContext != null && !textContext.trim().isEmpty()) {
            finalText = textContext;
            mapOperators.put(fieldName.trim(), finalText.trim());
        } else {
            mapOperators.put(fieldName.trim(), "Failed to Load teh Text");

            logOperations.error(String.format("Failed to retrieve text from element for: %s", fieldName));
        }

        return finalText;
    }

    public void quit(int status) {
        this.currentDriver.quit();
        if (status == 0) {
            System.exit(status);
        }
    }

    public short operationLog(boolean success, String mainMsg, String currentExecution, long duration) {

        if (success) {

            logOperations.info(String.format(
                    success ? "Success %s Current Cmd: %s - Duration: %s" : "Failed %s Current Cmd: %s - Duration: %s",
                    mainMsg,
                    currentExecution,
                    LocalTime.ofNanoOfDay(duration).format(FORMAT_TIME)));
        } else {

            logOperations.warn(String.format(
                    success ? "Success %s Current Cmd: %s - Duration: %s" : "Failed %s Current Cmd: %s - Duration: %s",
                    mainMsg,
                    currentExecution,
                    LocalTime.ofNanoOfDay(duration).format(FORMAT_TIME)));
        }

        return (short) (success ? ExcelReportStatusEnum.SUCCESS.ordinal() : ExcelReportStatusEnum.ERROR.ordinal());
    }

    public String pauseEngine(String blockName) {

        //        JavascriptExecutor js = (JavascriptExecutor) this.currentDriver;
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
            InstructionLoad currentInstruction, String lastInstructionExecuted, boolean ifClause, boolean elseClause) {

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
            return conditionalBlock + " -> " + lastInstructionExecuted;

        } else {
            return lastInstructionExecuted;
        }
    }

    public String getValueIsNotDefined(
            String action,
            InstructionLoad currentInstruction,
            String lastInstructionExecuted,
            ARExecution.ConditionStatus conditionStatus,
            String parentField,
            String variableField) {

        if (conditionStatus.equals(ARExecution.ConditionStatus.NONE)) {
            String msg1, msg2, msg3, msg4 = null;

            if (action.equals(ARConstantsEngine.EXTRACT_FIELD)
                    || action.equals(ARConstantsEngine.CHECK_VALUE)
                    || action.equals(ARConstantsEngine.PDF_CHECK)
                    || action.equals(ARConstantsEngine.CSV_CHECK)) {
                msg1 = "The variable \"" + variableField + "\" has not been assigned.";
                msg2 = "Please add a <span style='color: #000080; font-weight: bold;'>GET</span> step for \""
                        + currentInstruction.getName() + "\" to assign this variable.";
                msg3 = "Missing a <span style='color: #000080; font-weight: bold;'>GET</span> for variable \""
                        + variableField + "\" .";
            } else {
                msg1 = "No GET value has been defined for: \"" + currentInstruction.getName() + "\".";
                msg2 = "Please add a GET step for instruction ID: " + currentInstruction.getParentId()
                        + " - Operation: " + currentInstruction.getOperation() + ".";

                if (parentField != null) {
                    msg3 = "Parent Web Field:";
                    msg4 = "Instruction ID " + currentInstruction.getParentId() + " - \"" + parentField + "\".";
                } else {
                    msg3 = "Parent Web Field is not defined!";
                    msg4 = "Ensure a valid parent field is assigned.";
                }
            }
            logOperations.error(
                    "Missing Variable for \"{}\" - {} - {} - {} - {}",
                    currentInstruction.getName(),
                    msg1,
                    msg2,
                    msg3,
                    msg4);
            performMessage.errorMessage(
                    "Missing Variable for \"" + currentInstruction.getName() + "\"", msg1, msg2, msg3, msg4, 0);
        }

        String conditionalBlock = conditionStatus.equals(ARExecution.ConditionStatus.IF_PASSED)
                ? "Closing Block { IF -> ELSE }  -> "
                : conditionStatus.equals(ARExecution.ConditionStatus.ELSEIF_PASSED)
                        ? "Closing Block { ELSEIF -> ELSE }  -> "
                        : conditionStatus.equals(ARExecution.ConditionStatus.ELSE_PASSED)
                                ? "Closing Block { ELSE -> ENDIF }  -> "
                                : "Get Value Is Not Defined";

        if (!conditionStatus.equals(ARExecution.ConditionStatus.NONE)) {
            return conditionalBlock + " -> " + lastInstructionExecuted;

        } else {
            return lastInstructionExecuted;
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

        logOperations.error("Parent Id Error: {} - {} - {}", msg1, msg2, msg3);
        performMessage.errorMessage("Parent Id Error", msg1, msg2, msg3, null, 0);

        return resultActions;
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

        logOperations.error("Parent Id Error: {} - {} - {}", msg1, msg2, msg3);
        performMessage.errorMessage("Parent Id Error", msg1, msg2, msg3, null, 0);

        return resultActions;
    }

    public String parentIdWrongBlockEngine(
            InstructionLoad currentInstruction, BlockLoadDTO blockLoad, boolean ifClause, boolean elseClause) {
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

            logOperations.warn(String.format(
                    "%sParent Id Error Check Parent Id: %d "
                            + "For the \"%s\" Does not belong to this block: "
                            + blockLoad.getId() + "-" + blockLoad.getName(),
                    conditionalBlock,
                    currentInstruction.getParentId(),
                    currentInstruction.getOperation()));

        } else {

            logOperations.error(String.format(
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
            InstructionLoad currentInstruction,
            BlockLoadDTO blockLoad,
            String lastInstructionExecuted,
            ARExecution.ConditionStatus conditionStatus) {

        if (conditionStatus.equals(ARExecution.ConditionStatus.NONE)) {
            String operation = currentInstruction.getOperation();
            int colonIndex = operation.indexOf(":");
            String parentOperationPart = colonIndex != -1 ? operation.substring(0, colonIndex) : "Unknown Operation";

            String msg1 = "The Parent Id: \"(" + currentInstruction.getParentId() + ")" + parentOperationPart + "\"";
            String msg2 = "Does not belong to the block: \"" + blockLoad.getBlockOrderNumber() + "-"
                    + blockLoad.getName() + "\"";
            String msg3 = "Attempted Operation : \""
                    + (currentInstruction.getActions().equals(ARConstantsEngine.EXTRACT_FIELD)
                            ? "Extract "
                            : currentInstruction.getActions())
                    + "\" -> \""
                    + operation + "\"";
            String msg4 = "Check the Web Field \" ( ID ) <NAME> \" per Block";

            logOperations.error("Parent Id Error: {} - {} - {} - {}", msg1, msg2, msg3, msg4);
            performMessage.errorMessage("Parent Id Error", msg1, msg2, msg3, msg4, 0);
        }

        String conditionalBlock = conditionStatus.equals(ARExecution.ConditionStatus.IF_PASSED)
                ? "Closing Block { IF -> ELSE }  -> "
                : conditionStatus.equals(ARExecution.ConditionStatus.ELSEIF_PASSED)
                        ? "Closing Block { ELSEIF -> ELSE }  -> "
                        : conditionStatus.equals(ARExecution.ConditionStatus.ELSE_PASSED)
                                ? "Closing Block { ELSE -> ENDIF }  -> "
                                : "Parent Id in Wrong Block";

        if (!conditionStatus.equals(ARExecution.ConditionStatus.NONE)) {

            logOperations.warn(String.format(
                    "%sParent Id Error Check Parent Id: %d For the \"%s\" Does not belong to this block: %d-%s",
                    conditionalBlock,
                    currentInstruction.getParentId(),
                    currentInstruction.getOperation(),
                    blockLoad.getId(),
                    blockLoad.getName()));
        } else {

            logOperations.error(String.format(
                    "Parent Id Error Check Parent Id: %d For the \"%s\" Does not belong to this block: %d-%s",
                    currentInstruction.getParentId(),
                    currentInstruction.getOperation(),
                    blockLoad.getId(),
                    blockLoad.getName()));
        }

        if (!conditionStatus.equals(ARExecution.ConditionStatus.NONE)) {
            return conditionalBlock + " -> " + lastInstructionExecuted;
        } else {
            return lastInstructionExecuted;
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
            return conditionalBlock + " -> " + lastInstructionExecuted;

        } else {
            return lastInstructionExecuted;
        }
    }

    public String checkValidationFailed(
            String invalidValues,
            String parent,
            String expected,
            String lastInstructionExecuted,
            String[] operations,
            ARExecution.ConditionStatus conditionStatus,
            boolean byPassFlagLoop) {

        if (conditionStatus.equals(ARExecution.ConditionStatus.NONE) && !byPassFlagLoop) {

            String msg1;
            if (operations[1].equals(">")) {
                msg1 = "The Value of: \"" + expected + "\" is not <span style='color: #000080; font-weight: bold;'>( "
                        + operations[1] + " )</span> \"" + operations[2] + "\"";
            } else if (operations[1].equals("<")) {
                msg1 = "The Value of: \"" + operations[2]
                        + "\" is not <span style='color: #000080; font-weight: bold;'>( &lt; )</span> \"" + expected
                        + "\"";
            } else {
                msg1 = "The Value of: \"" + operations[2] + "\" is not " + operations[1] + " \""
                        + expected + "\" Length: ("
                        + expected.length()
                        + ")";
            }

            String msg2 = "The Variable \"" + operations[0] + "\" holds value \"" + operations[2] + "\"";

            String msg3;
            if (operations[1].equals(">") || operations[1].equals("<")) {
                msg3 = "Current Web Field \"" + parent + "\" value: \"" + expected + "\"";
            } else {
                msg3 = "Current Web Field \"" + parent + "\" value: \""
                        + expected + "\" Length: (" + expected.length()
                        + ")";
            }

            String msg4;
            if (operations[1].equals(">") || operations[1].equals("<")) {
                msg4 = "Expected value: " + operations[2];
            } else {
                msg4 = "Expected value: " + operations[2] + " Length: (" + operations[2].length() + ")";
            }

            if (Strings.isNullOrEmpty(invalidValues)) {
                invalidValues = "Check Validation Value Error";
            } else {

                if (operations[1].equals("<")) {
                    invalidValues += " Operator: (\" &lt; \")";
                } else {
                    invalidValues += " Operator: (\" " + operations[1] + " \")";
                }
            }
            logOperations.error("Invalid Values Error: {} - {} - {} - {} - {}", invalidValues, msg1, msg2, msg3, msg4);
            performMessage.errorMessage(invalidValues, msg1, msg2, msg3, msg4, 0);
        }

        String conditionalBlock = conditionStatus.equals(ARExecution.ConditionStatus.IF_PASSED)
                ? "Closing Block { IF -> ELSE }  -> "
                : conditionStatus.equals(ARExecution.ConditionStatus.ELSEIF_PASSED)
                        ? "Closing Block { ELSEIF -> ELSE }  -> "
                        : conditionStatus.equals(ARExecution.ConditionStatus.ELSE_PASSED)
                                ? "Closing Block { ELSE -> ENDIF }  -> "
                                : "";

        if (!conditionStatus.equals(ARExecution.ConditionStatus.NONE)) {
            return conditionalBlock + " -> " + lastInstructionExecuted;

        } else {
            return lastInstructionExecuted;
        }
    }

    public String buildValidationReason(
            String invalidValues,
            String parent,
            String actualValue, // current web field value
            String expectedValue, // EXPECTED VALUE AS PARAM
            String lastInstructionExecuted,
            String[] operations, // [0]=variableName, [1]=operator
            ARExecution.ConditionStatus conditionStatus,
            boolean byPassFlagLoop,
            boolean includeLengths,
            String blockName,
            Integer testRow,
            boolean success) {

        if (operations == null || operations.length < 2) {
            return withConditionalPrefix(conditionStatus, "Validation failed - malformed operation definition");
        }

        if (byPassFlagLoop) {
            return withConditionalPrefix(conditionStatus, lastInstructionExecuted);
        }

        String varName = operations.length > 0 ? operations[0] : "?";
        String op = operations.length > 1 ? operations[1] : "?";

        String rawActual = actualValue == null ? "" : actualValue;
        String rawExpected = expectedValue == null ? "" : expectedValue;

        String safeActual = rawActual;
        String safeExpected = rawExpected;

        if (">".equals(op) || "<".equals(op)) {
            safeActual = normalizeNumber(rawActual);
            safeExpected = normalizeNumber(rawExpected);
        }

        // ✅ Professional summary: passed / failed
        String summary;
        if (invalidValues == null || invalidValues.trim().isEmpty()) {
            summary = success ? "Validation passed" : "Validation failed";
        } else {
            summary = invalidValues.trim() + " Operator: (" + op + ")";
        }

        String conditionText; // ✅ will go into "Condition" column

        if (">".equals(op)) {
            conditionText = String.format(
                    "value \"%s\" %s \"%s\" (variable \"%s\")",
                    safeActual, (success ? "is >" : "is not >"), safeExpected, varName);

        } else if ("<".equals(op)) {
            conditionText = String.format(
                    "value \"%s\" %s \"%s\" (variable \"%s\")",
                    safeActual, (success ? "is <" : "is not <"), safeExpected, varName);

        } else if ("!=".equals(op)) {
            conditionText = String.format(
                    "value \"%s\" %s \"%s\" (variable \"%s\")",
                    safeActual, (success ? "is !=" : "is not !="), safeExpected, varName);

        } else {
            String opPhrase = success ? ("is " + op) : ("is not " + op);

            conditionText = String.format(
                    "value \"%s\" %s \"%s\" (variable \"%s\")", safeActual, opPhrase, safeExpected, varName);

            if (includeLengths) {
                conditionText +=
                        String.format(" [actualLen=%d, expectedLen=%d]", safeActual.length(), safeExpected.length());
            }
        }

        // ✅ Main Field column should be just the field name (no quotes)
        String mainField = (parent == null) ? "" : parent;

        // ✅ Description column
        String desc = (blockName == null) ? "" : blockName;

        // ✅ Test column
        String testName = (testRow == null) ? "" : String.valueOf(testRow);

        // ✅ Result column
        String result = success ? "PASSED" : "FAIL";

        // ✅ Time column (your JTable expects Time as first col)
        String time = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());

        // Keep your output format (Condition column = conditionText).
        // If you want to include summary too, replace conditionText with (summary + " - " + conditionText).
        String row =
                time + " | " + testName + " | " + desc + " | " + mainField + " | " + conditionText + " | " + result;

        return row;
    }

    //    /**
    //     * Builds a human-readable reason when a required GET variable is missing / not assigned.
    //     *
    //     * IMPORTANT:
    //     * - This method does NOT log.
    //     * - Callers can logOperations.error(...) OUTSIDE using the returned string.
    //     * - It follows the same principle as buildValidationFailureReason:
    //     *   - If conditionStatus != NONE -> prepend block transition text + " -> " + lastInstructionExecuted
    //     *   - If conditionStatus == NONE -> return lastInstructionExecuted (keep original behavior)
    //     *
    //     * If you want a message even when NONE, just change the last return branch accordingly.
    //     */
    //    public String buildGetVariableReason(
    //            String action,
    //            InstructionLoad currentInstruction,
    //            String lastInstructionExecuted,
    //            ARExecution.ConditionStatus conditionStatus,
    //            String parentField,
    //            String variableField,
    //            boolean byPassFlagLoop) {
    //
    //        // If bypassing, behave like original logic: just return the last instruction executed
    //        if (byPassFlagLoop) {
    //            return withConditionalPrefix(conditionStatus, lastInstructionExecuted);
    //        }
    //
    //        // Preserve raw values
    //        String instrName =
    //                currentInstruction != null && currentInstruction.getName() != null ? currentInstruction.getName()
    // : "?";
    //
    //        String var = variableField == null ? "?" : variableField;
    //        String parent = parentField == null ? "" : parentField;
    //
    //        // Build a concise reason (plain text; keep HTML out because caller may log it)
    //        String summary;
    //        if (ARConstantsEngine.EXTRACT_FIELD.equals(action) || ARConstantsEngine.CHECK_VALUE.equals(action)) {
    //            summary = String.format(
    //                    "Get Value Is Not Defined - variable \"%s\" has not been assigned (instruction \"%s\")",
    //                    var, instrName);
    //        } else {
    //            if (parentField != null) {
    //                summary = String.format(
    //                        "Get Value Is Not Defined - no GET value defined for instruction \"%s\" (parent field
    // \"%s\")",
    //                        instrName, parent);
    //            } else {
    //                summary = String.format(
    //                        "Get Value Is Not Defined - no GET value defined for instruction \"%s\" (parent field not
    // defined)",
    //                        instrName);
    //            }
    //        }
    //
    //        // Follow the SAME “append lastInstructionExecuted + conditional prefix” pattern
    //        // Note: original getValueIsNotDefined returned lastInstructionExecuted when conditionStatus==NONE.
    //        // We keep that behavior here.
    //        String reasonWithTrail = summary + " -> " + lastInstructionExecuted;
    //
    //        return withConditionalPrefix(conditionStatus, reasonWithTrail);
    //    }

    public String messageExcel(
            String action, // ✅ action FIRST (e.g. "EXCEL", "INSERT", etc.)
            InstructionLoad instruction, // for desc
            String parentField, // e.g. "8838-BancaStato"
            String variableField, // e.g. "331-$BancaStato"
            String value, // value written to Excel
            String blockName, // fallback desc
            Integer testRow, // fallback test
            boolean success // PASSED / FAIL
            ) {

        String time = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());

        // TEST + Main Field parsing (unchanged)
        String testName = "";
        String mainField = "";

        if (parentField != null && parentField.contains("-")) {
            int idx = parentField.indexOf('-');
            testName = parentField.substring(0, idx).trim();
            mainField = parentField.substring(idx + 1).trim();
        } else {
            mainField = (parentField == null) ? "" : parentField;
            testName = (testRow == null) ? "" : String.valueOf(testRow);
        }

        // desc (unchanged)
        String desc = (instruction != null && instruction.getName() != null)
                ? instruction.getName()
                : (blockName == null ? "" : blockName);

        // ✅ ONLY CHANGE: conditionText built here, ACTION first
        String conditionText = success
                ? action + " --> Insert into Excel -> " + variableField + "-" + value
                : action + " --> NO Export Excel File defined -> " + variableField + "-" + value;

        String result = success ? "PASSED" : "FAIL";

        return time + " | " + testName + " | " + desc + " | " + mainField + " | " + conditionText + " | " + result;
    }

    public static String sanitizeValue(String input) {
        if (input == null) return "";

        return input.replace('\u00A0', ' ') // NO-BREAK SPACE
                .replace('\u202F', ' ') // NARROW NO-BREAK SPACE
                .replace('\u2007', ' ') // FIGURE SPACE
                .replaceAll("\\s+", " ") // collapse whitespace
                .trim();
    }

    /**
     * Builds a JTable row (pipe-separated) for CHECK_VALUE / validation messages.
     *
     * Output pattern:
     *   time | testName | desc | mainField | conditionText | result
     */
    public String checkValidationMesssage(
            String action,
            InstructionLoad currentInstruction,
            String lastInstructionExecuted,
            ARExecution.ConditionStatus conditionStatus,
            String parentField, // e.g. "8838-BancaStato" OR "BancaStato"
            String variableField, // e.g. "$BancaStato"
            String actualValue, // actual extracted value
            String expectedValue, // expected value
            String operator, // "=", "!=", ">", "<", etc.
            boolean byPassFlagLoop,
            String blockName,
            Integer testRow,
            boolean includeLengths,
            boolean success) {

        if (byPassFlagLoop) {
            return withConditionalPrefix(conditionStatus, lastInstructionExecuted);
        }

        String instrName =
                currentInstruction != null && currentInstruction.getName() != null ? currentInstruction.getName() : "?";

        String var = (variableField == null) ? "?" : variableField;

        String rawActual = (actualValue == null) ? "" : actualValue;
        String rawExpected = (expectedValue == null) ? "" : expectedValue;

        String safeActual = rawActual;
        String safeExpected = rawExpected;

        if (">".equals(operator) || "<".equals(operator)) {
            safeActual = normalizeNumber(rawActual);
            safeExpected = normalizeNumber(rawExpected);
        }

        // ✅ parse TEST + Main Field like your other methods
        String testName = "";
        String mainField = "";
        if (parentField != null && parentField.contains("-")) {
            int idx = parentField.indexOf('-');
            testName = parentField.substring(0, idx).trim();
            mainField = parentField.substring(idx + 1).trim();
        } else {
            mainField = (parentField == null) ? "" : parentField;
            testName = (testRow == null) ? "" : String.valueOf(testRow);
        }

        // ✅ Description column
        String desc = (blockName == null) ? "" : blockName;

        // ✅ Condition column (action first)
        String op = (operator == null) ? "?" : operator;

        String conditionText;
        if (">".equals(op)) {
            conditionText = String.format(
                    "%s] --> value \"%s\" is not > \"%s\" (variable \"%s\", instruction \"%s\")",
                    action, safeActual, safeExpected, var, instrName);

        } else if ("<".equals(op)) {
            conditionText = String.format(
                    "%s] --> value \"%s\" is not < \"%s\" (variable \"%s\", instruction \"%s\")",
                    action, safeActual, safeExpected, var, instrName);

        } else if ("!=".equals(op)) {
            conditionText = String.format(
                    "%s] --> value \"%s\" is not != \"%s\" (variable \"%s\", instruction \"%s\")",
                    action, safeActual, safeExpected, var, instrName);

        } else {
            conditionText = String.format(
                    "%s] --> value \"%s\" is not %s \"%s\" (variable \"%s\", instruction \"%s\")",
                    action, safeActual, op, safeExpected, var, instrName);

            if (includeLengths) {
                conditionText +=
                        String.format(" [actualLen=%d, expectedLen=%d]", safeActual.length(), safeExpected.length());
            }
        }

        // ✅ Result column
        String result = success ? "PASSED" : "FAIL";

        // ✅ Time column
        String time = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());

        String row =
                time + " | " + testName + " | " + desc + " | " + mainField + " | " + conditionText + " | " + result;

        return withConditionalPrefix(conditionStatus, row);
    }

    /**
     * Builds a JTable row (same pipe-separated pattern as buildValidationReason)
     * for the case "GET variable missing / not assigned".
     *
     * Output pattern:
     *   time | testName | desc | mainField | conditionText | result
     */
    public String buildGetVariableReason(
            String action,
            InstructionLoad currentInstruction,
            String lastInstructionExecuted,
            ARExecution.ConditionStatus conditionStatus,
            String parentField, // MAIN FIELD (e.g. "BancaStato")
            String variableField, // variable name (e.g. "$BancaStato")
            boolean byPassFlagLoop,
            String blockName,
            Integer testRow,
            boolean success // usually false for "not defined"
            ) {

        if (byPassFlagLoop) {
            return withConditionalPrefix(conditionStatus, lastInstructionExecuted);
        }

        // Preserve raw values
        String instrName =
                currentInstruction != null && currentInstruction.getName() != null ? currentInstruction.getName() : "?";

        String var = (variableField == null) ? "?" : variableField;
        String parent = (parentField == null) ? "" : parentField;

        // ✅ Condition column text (human readable, like buildValidationReason)
        String conditionText;
        if (ARConstantsEngine.EXTRACT_FIELD.equals(action) || ARConstantsEngine.CHECK_VALUE.equals(action)) {
            conditionText = String.format(
                    "Get Value Is Not Defined - variable \"%s\" has not been assigned (instruction \"%s\")",
                    var, instrName);
        } else {
            if (parentField != null) {
                conditionText = String.format(
                        "Get Value Is Not Defined - no GET value defined for instruction \"%s\" (parent field \"%s\")",
                        instrName, parent);
            } else {
                conditionText = String.format(
                        "Get Value Is Not Defined - no GET value defined for instruction \"%s\" (parent field not defined)",
                        instrName);
            }
        }

        // ✅ Main Field column: just field name
        String mainField = parent;

        // ✅ Description column
        String desc = (blockName == null) ? "" : blockName;

        // ✅ Test column
        String testName = (testRow == null) ? "" : String.valueOf(testRow);

        // ✅ Result column
        String result = success ? "PASSED" : "FAIL";

        // ✅ Time column
        String time = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());

        String row =
                time + " | " + testName + " | " + desc + " | " + mainField + " | " + conditionText + " | " + result;

        // Preserve conditional prefix behavior
        return withConditionalPrefix(conditionStatus, row);
    }

    private String normalizeNumber(String value) {
        if (value == null) return null;
        return value.replaceAll("[^0-9,.-]", "").replace(",", ".");
    }

    private String withConditionalPrefix(ARExecution.ConditionStatus conditionStatus, String message) {
        String conditionalBlock = conditionStatus == ARExecution.ConditionStatus.IF_PASSED
                ? "Closing Block { IF -> ELSE } -> "
                : conditionStatus == ARExecution.ConditionStatus.ELSEIF_PASSED
                        ? "Closing Block { ELSEIF -> ELSE } -> "
                        : conditionStatus == ARExecution.ConditionStatus.ELSE_PASSED
                                ? "Closing Block { ELSE -> ENDIF } -> "
                                : "";

        if (conditionStatus != null && conditionStatus != ARExecution.ConditionStatus.NONE) {
            return conditionalBlock + message;
        }
        return message;
    }

    public boolean excelReportWrite(
            ARExecution.ConditionStatus currentCondition,
            String blockName,
            boolean success,
            String[] actions,
            FieldData msgLoop,
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

        logOperations.error("Parent Id Error: {} - {} - {}", msg1, msg2, msg3);
        performMessage.errorMessage("Parent Id Error", msg1, msg2, msg3, null, 0);

        logOperations.error("Block GO TO Error: -> Check Correct Block Existence! -> CMD: " + resultActions);

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

        logOperations.warn(
                "Block Execution LIMIT Reached!. Process Reached BLOCK LIMIT of {} executions. Last Exetution: {}",
                executionTimes,
                lastInstructionExecuted);
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
        Set<String> windowHandles = this.currentDriver.getWindowHandles();
        windowHandlesList = new ArrayList<>(windowHandles);
    }

    public String getSessionId() {
        if (this.currentDriver instanceof RemoteWebDriver) {
            return ((RemoteWebDriver) this.currentDriver).getSessionId().toString();
        } else {
            throw new IllegalStateException("Driver is not an instance of RemoteWebDriver");
        }
    }

    public void alertMessage(String message) {
        JavascriptExecutor js = (JavascriptExecutor) this.currentDriver;

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
        org.openqa.selenium.Alert alert = this.currentDriver.switchTo().alert();

        // Optional: pause for a few seconds to view the alert
        try {
            Thread.sleep(5000); // 10 minutes in milliseconds
        } catch (InterruptedException e) {
            logOperations.warn(e.getMessage());
        }

        // Accept (close) the alert
        alert.accept();
    }

    public String actionResultMessage(String blockJobName, String[] actions, FieldData msgInstruction) {

        // ✅ existing message becomes "conditionText"
        String conditionText;

        switch (actions[0]) {
            case ARConstantsEngine.VISUALIZE:
                conditionText = "Visualize " + msgInstruction.getKey();
                break;
            case ARConstantsEngine.OTHER:
                conditionText = "Other Element --> " + msgInstruction.getKey();
                break;
            case ARConstantsEngine.OUTPUT:
                conditionText = "Output Element --> " + msgInstruction.getKey();
                break;
            case ARConstantsEngine.CLICK:
                conditionText = "Click Element --> " + msgInstruction.getKey();
                break;
            case ARConstantsEngine.INSERT:
                if (actions[0].equals(ARConstantsEngine.INSERT) && actions[1].equals(ARConstantsEngine.ENTER)) {
                    conditionText = "Insert/<Enter> action for  -> " + msgInstruction.getKey() + " = "
                            + msgInstruction.getValue();
                } else {
                    conditionText =
                            "Insert action for  -> " + msgInstruction.getKey() + " = " + msgInstruction.getValue();
                }
                break;
            case ARConstantsEngine.LIST_OPERATION:
                conditionText = "List Operation " + msgInstruction.getKey();
                break;
            case ARConstantsEngine.HOLD:
                conditionText = "Hold executed " + msgInstruction.getKey();
                break;
            case ARConstantsEngine.PAUSE:
                conditionText = "Pause action triggered";
                break;
            case ARConstantsEngine.NEXT_ENTER: // NEXT FIELD / FOCUS NEXT / ENTER
                conditionText = "Next/Enter action triggered";
                break;
            case ARConstantsEngine.SWIPE_UP:
                conditionText = "Swipe UP action triggered";
                break;
            case ARConstantsEngine.SWIPE_DOWN:
                conditionText = "Swipe DOWN action triggered";
                break;
            case ARConstantsEngine.GOTO:
                if (msgInstruction.getValue().equals("Unknown")) {
                    conditionText = msgInstruction.getKey();
                } else {
                    String[] parts = msgInstruction.getKey().split(":");
                    conditionText = String.format(
                            "GO TO Block \"%s\" Limit %s times",
                            "(" + parts[0] + ")-#" + parts[2] + " " + parts[3], msgInstruction.getValue());
                }
                break;
            case ARConstantsEngine.REFRESH_ONLY:
                conditionText = " Refresh Web Page";
                break;
            case ARConstantsEngine.REFRESH_HOLD:
                String[] msgParent = msgInstruction.getKey().split(":");
                String[] msgValue = msgInstruction.getValue().split(":");
                conditionText = String.format(
                        "Wait for Parent \"%s\" Limit %s seconds",
                        "(" + msgParent[1] + ") " + msgParent[2], msgValue[0]);
                break;
            case ARConstantsEngine.LOOP:
                if (msgInstruction.getValue().equals("Unknown")) {
                    conditionText = msgInstruction.getKey();
                } else {
                    msgParent = msgInstruction.getKey().split(":");
                    conditionText = String.format(
                            "Jump To Parent \"%s\" Limit %s times",
                            msgParent[0] + "-(" + msgParent[1] + ") " + msgParent[2], msgInstruction.getValue());
                }
                break;
            case ARConstantsEngine.REFRESH_LOOP:
                if (msgInstruction.getValue().equals("Unknown")) {
                    conditionText = msgInstruction.getKey();
                } else {
                    msgParent = msgInstruction.getKey().split(":");
                    msgValue = msgInstruction.getValue().split(":");
                    conditionText = String.format(
                            "Refresh in %s seconds Loop %s times Jump To Parent \"%s\" ",
                            msgValue[0], msgValue[1], msgParent[0] + "-(" + msgParent[1] + ") " + msgParent[2]);
                }
                break;
            case ARConstantsEngine.QUIT:
                conditionText = "Quit action processed";
                break;
            case ARConstantsEngine.SCREEN:
                conditionText = "Screen action executed for " + msgInstruction.getKey() + " --> " + blockJobName;
                break;
            case ARConstantsEngine.GET_VALUE:
            case ARConstantsEngine.SET_VALUE:
                conditionText = actions[0]
                        + ARConstantsEngine.BLANK_STRING
                        + msgInstruction.getKey()
                        + ARConstantsEngine.BLANK_STRING
                        + msgInstruction.getValue();
                break;
            case ARConstantsEngine.CHECK_VALUE:
            case ARConstantsEngine.PDF_CHECK:
            case ARConstantsEngine.CSV_CHECK:
                conditionText = actions[0]
                        + ARConstantsEngine.BLANK_STRING
                        + msgInstruction.getValue()
                        + ARConstantsEngine.BLANK_STRING
                        + msgInstruction.getKey();
                break;
            case ARConstantsEngine.EXTRACT_FIELD:
                conditionText = ARConstantsEngine.BLANK_STRING
                        + msgInstruction.getKey() + " Extract "
                        + ARConstantsEngine.BLANK_STRING
                        + msgInstruction.getValue();
                break;

            default:
                conditionText = "No Action Detected for " + msgInstruction.getKey();
                break;
        }

        // ✅ SAME PATTERN AS performOperatorActions
        String time = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());

        // If you don't have a test number here, keep it empty (caller can fill elsewhere)
        String testName = "";

        // Best default mainField: the key (e.g. "(8869)-OK" or "8838-BancaStato")
        String mainField = (msgInstruction == null || msgInstruction.getKey() == null) ? "" : msgInstruction.getKey();

        // Description: block/job name (or empty)
        String desc = (blockJobName == null) ? "" : blockJobName;

        // ✅ default PASSED for this method
        String result = "PASSED";

        return time + " | " + testName + " | " + desc + " | " + mainField + " | " + conditionText + " | " + result;
    }

    public String buildMessageResult(
            boolean success, String testName, String description, String mainField, String conditionText) {

        String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));

        String result = success ? "PASSED" : "FAIL";

        return time + " | " + testName + " | " + description + " | " + mainField + " | " + conditionText + " | "
                + result;
    }

    public int[] addElementToArray(int[] refreshLoopArray, int newItem) {
        int[] extendedRefreshArray = new int[refreshLoopArray.length + 1];
        System.arraycopy(refreshLoopArray, 0, extendedRefreshArray, 0, refreshLoopArray.length);
        extendedRefreshArray[refreshLoopArray.length] = newItem;
        return extendedRefreshArray;
    }

    public String getXPathInstruction(InstructionLoad currentInstruction, BlockLoadDTO blockLoad) {
        try {
            return blockLoad.getInstructionLoad().stream()
                    .filter(f -> f.getId().equals(currentInstruction.getParentId()))
                    .findFirst()
                    .get()
                    .getXpath();
        } catch (Exception ex) {
            return null;
        }
    }

    public String getInstructionParentField(InstructionLoad currentInstruction, BlockLoadDTO blockLoad) {
        try {
            return blockLoad.getInstructionLoad().stream()
                    .filter(f -> f.getId().equals(currentInstruction.getParentId()))
                    .findFirst()
                    .get()
                    .getName()
                    .trim();
        } catch (Exception ex) {
            return null;
        }
    }

    public String getInstructionParentActions(InstructionLoad currentInstruction, BlockLoadDTO blockLoad) {
        try {
            return blockLoad.getInstructionLoad().stream()
                    .filter(f -> f.getId().equals(currentInstruction.getParentId()))
                    .findFirst()
                    .get()
                    .getActions();
        } catch (Exception ex) {
            return null;
        }
    }

    public String getInstructionVariableField(InstructionLoad currentInstruction, List<VariableLoadDTO> variableLoad) {
        try {
            return variableLoad.stream()
                    .filter(f -> f.getId().equals(currentInstruction.getVariableId()))
                    .findFirst()
                    .map(v -> {
                        return v.getId() + "-" + String.valueOf(v.getType().charAt(0))
                                + v.getName().trim();
                    })
                    .orElse(null);
        } catch (Exception ex) {
            return null;
        }
    }

    public String getInstructionVariableFormat(InstructionLoad currentInstruction, List<VariableLoadDTO> variableLoad) {
        try {
            return variableLoad.stream()
                    .filter(f -> f.getId().equals(currentInstruction.getVariableId()))
                    .findFirst()
                    .map(v -> {
                        return v.getLocalFormat().trim();
                    })
                    .orElse(null);
        } catch (Exception ex) {
            return null;
        }
    }

    public String getInstructionVariableDelimiter(
            InstructionLoad currentInstruction, List<VariableLoadDTO> variableLoad) {
        try {
            return variableLoad.stream()
                    .filter(f -> f.getId().equals(currentInstruction.getVariableId()))
                    .findFirst()
                    .map(v -> {
                        return v.getDelimiter().trim();
                    })
                    .orElse(null);
        } catch (Exception ex) {
            return null;
        }
    }

    // It Must be Greater than CurrentIndex
    // Ir Predicts if is going to have multiple ENSEIFs
    public int searchMapConditional(
            Map<String, List<Integer>> mapConditional,
            int parentBlockCondition,
            ARExecution.ConditionStatus condition,
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
                    "OK",
                    null,
                    0);
        }

        return -1; // Return -1 if no valid index is found
    }

    public Map<String, List<Integer>> getConditionIndexMapByParentId(BlockLoadDTO blockLoad) {
        try {
            // Create a map where key is "parentId-actions" and value is a list of indices
            return IntStream.range(0, blockLoad.getInstructionLoad().size())
                    .filter(index -> {
                        InstructionLoad instruction =
                                blockLoad.getInstructionLoad().get(index);
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
                                InstructionLoad instruction =
                                        blockLoad.getInstructionLoad().get(index);
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
        String htmlPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_EXPORT);
        try (FileWriter writer = new FileWriter(htmlPath + "/" + type + ".json")) {
            // Convert the list of strings to a JSON-like array format
            writer.write(htmlArray.stream()
                    .map(s -> "\"" + s.replace("\"", "\\\"") + "\"") // Escape double quotes
                    .collect(Collectors.joining(", ", "[", "]"))); // Format as JSON array
        } catch (IOException e) {
            logOperations.warn("Error writing to file: " + e.getMessage());
        } finally {
            // Close the browser if necessary
            // driver.quit();
        }
    }

    /**
     * Find elements by splitting a CSS locator into tag, ID, and classes.
     * Returns a combined list of unique WebElements.
     */
    public List<WebElement> findBySmartLocator(String locator) {
        Set<WebElement> uniqueElements = new HashSet<>();

        // Extract tag
        String tag = locator.split("#")[0]; // e.g., "input"

        // Extract ID (if present)
        String idPart = locator.contains("#") ? locator.split("#")[1].split("\\.")[0] : null;

        // Extract classes (if present)
        String[] classes = new String[0];
        if (locator.contains(".")) {
            String classesPart = locator.substring(locator.indexOf('.') + 1);
            classes = classesPart.split("\\.");
        }

        // Try locating by full CSS
        uniqueElements.addAll(this.currentDriver.findElements(By.cssSelector(locator)));

        // Try locating by tag
        if (tag != null && !tag.isEmpty()) {
            uniqueElements.addAll(this.currentDriver.findElements(By.tagName(tag)));
        }

        // Try locating by ID
        if (idPart != null && !idPart.isEmpty()) {
            uniqueElements.addAll(this.currentDriver.findElements(By.id(idPart)));
        }

        // Try locating by each class
        for (String cls : classes) {
            if (!cls.isEmpty()) {
                uniqueElements.addAll(this.currentDriver.findElements(By.className(cls)));
            }
        }

        return new ArrayList<>(uniqueElements);
    }

    public boolean executeActionsAtCoordinates(
            String savedCoordinates, FieldData data, String action, boolean pressEnterAfter) {

        boolean forceCLick = false;

        int x = 0;
        int y = 0;
        int xCoord = 0;
        int yCoord = 0;
        try {
            String[] coordinates = savedCoordinates.split(ARConstantsEngine.FIELDS_SEPARATOR);
            double temp1 = Double.parseDouble(coordinates[0]);
            double temp2 = Double.parseDouble(coordinates[1]);
            x = (int) temp1;
            y = (int) temp2;
            int maxHeight = this.currentDriver.manage().window().getSize().getHeight();
            int maxWidth = this.currentDriver.manage().window().getSize().getWidth();
            int offsetY = y - maxHeight;
            int offsetX = x - maxWidth;
            xCoord = x > maxWidth ? x - offsetX : x;
            yCoord = y > maxHeight ? y - offsetY : y;

            if (ARConstantsEngine.VISUALIZE.equals(action)) {
                scrollToCoordinates(x, y);
            } else if (ARConstantsEngine.CLICK.equals(action)) {
                scrollToCoordinates(x, y);
                //                circleAtCoordinates(x, y, this.currentDriver);
                onHoldForSeconds(null);
                clickAtCoordinates(xCoord, yCoord);
            } else if (ARConstantsEngine.INSERT.equals(action)) {
                scrollToCoordinates(x, y);
                //                sendInputJS(x, y, data.getValue(),this.currentDriver);
                //                circleAtCoordinates(x, y, this.currentDriver);
                onHoldForSeconds(null);
                //                clickAtCoordinates(xCoord, yCoord);
                //                onHoldForSeconds(null);
                typeCharacters(savedCoordinates, data);
                if (pressEnterAfter) {
                    boolean respAction = sendActionEnter(xCoord, yCoord);
                    if (!respAction) {
                        sendEnterWithJS();
                    }
                }
            } else if (ARConstantsEngine.INSERT.equals(action) && forceCLick) {
                scrollToCoordinates(x, y);
                //                sendInputJS(x, y, data.getValue(),this.currentDriver);
                //                circleAtCoordinates(x, y, this.currentDriver);
                onHoldForSeconds(null);
                clickAtCoordinates(xCoord, yCoord);
                onHoldForSeconds(null);
                typeCharacters(savedCoordinates, data);

                if (pressEnterAfter) {
                    boolean respAction = sendActionEnter(xCoord, yCoord);
                    if (!respAction) {
                        sendEnterWithJS();
                    }
                }
            }
            onHoldForSeconds(null);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void scrollToCoordinates(int x, int y) {
        int maxHeight = this.currentDriver.manage().window().getSize().getHeight();
        int maxWidth = this.currentDriver.manage().window().getSize().getWidth();
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
            new WebDriverWait(this.currentDriver, Duration.ofSeconds(5))
                    .until((item) -> (Boolean) ((JavascriptExecutor) this.currentDriver).executeScript(script));
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
        new Actions(this.currentDriver).moveToLocation(x, y).click().perform();
    }

    public WebElement getElementFromCoordinates(String savedCoordinates) {
        int x = 0;
        int y = 0;
        int xCoord = 0;
        int yCoord = 0;
        try {
            String[] coordinates = savedCoordinates.split(ARConstantsEngine.FIELDS_SEPARATOR);
            double temp1 = Double.parseDouble(coordinates[0]);
            double temp2 = Double.parseDouble(coordinates[1]);
            x = (int) temp1;
            y = (int) temp2;
            int maxHeight = this.currentDriver.manage().window().getSize().getHeight();
            int maxWidth = this.currentDriver.manage().window().getSize().getWidth();
            int offsetY = y - maxHeight;
            int offsetX = x - maxWidth;
            xCoord = x > maxWidth ? x - offsetX : x;
            yCoord = y > maxHeight ? y - offsetY : y;

            JavascriptExecutor js = (JavascriptExecutor) this.currentDriver;

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

    private void typeCharacters(String savedCoords, FieldData fieldData) {
        clearValueAtCoordinates(savedCoords);
        boolean passed = setValueAtCoordinates(savedCoords, fieldData.getValue().trim());
        if (!passed) {
            new Actions(this.currentDriver)
                    .sendKeys(fieldData.getValue().trim())
                    .perform();
        }
    }

    private boolean sendActionEnter(int x, int y) {
        try {
            new Actions(this.currentDriver)
                    .moveByOffset(x, y)
                    .sendKeys(Keys.ENTER)
                    .perform();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean sendEnterWithJS() {
        try {
            JavascriptExecutor js = (JavascriptExecutor) currentDriver;

            String script =
                    """
                                var evt = new KeyboardEvent('keydown', {
                                    key: 'Enter',
                                    code: 'Enter',
                                    keyCode: 13,
                                    which: 13,
                                    bubbles: true,
                                    cancelable: true
                                });
                                document.activeElement.dispatchEvent(evt);
                            """;

            js.executeScript(script);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String sequenceOfCommands(
            WebElement element,
            String typeCommand,
            String[] coordinates,
            FieldData fieldData,
            WebDriver driver,
            boolean pressEnterAfter) {

        String message = "Nothing to execute";
        try {
            if (typeCommand.equals(ARConstantsEngine.SELECT)) {
                // Create a Select instance to interact with the dropdown
                message = "Select(element)";
                Select selectCountry = new Select(element);
                selectCountry.selectByVisibleText(fieldData.getValue());
            } else if (typeCommand.equals(ARConstantsEngine.CLEAR)) {
                message = "clear()";
                element.clear();
                //                clearElement(element);
                for (String coords : coordinates) {
                    //                    executeActionsAtCoordinates(coords, fieldData, ARConstants.INSERT,
                    // pressEnterAfter);
                    clearValueAtCoordinates(coords);
                }

            } else if (typeCommand.equals(ARConstantsEngine.CLICK)) {
                message = "click()";
                element.click();
            } else if (typeCommand.equals(ARConstantsEngine.INSERT)) {
                message = "sendKeys(\"" + fieldData.getValue() + "\")";
                element.sendKeys(fieldData.getValue());
            } else if (typeCommand.equals(ARConstantsEngine.TAB)) {
                message = "(Keys.TAB)";
                element.sendKeys(Keys.TAB);
            } else if (typeCommand.equals(ARConstantsEngine.GET_VALUE)) {
                message = "getText()";
                element.getText();
            } else if (typeCommand.equals(ARConstantsEngine.FOCUS)) {
                message = "focusElement(element, driver)";
                focusElement(element, driver);
            } else if (typeCommand.equals(ARConstantsEngine.COORD_VISUALIZA)) {
                message = "Coordinates Visualiza";
                for (String coords : coordinates) {
                    executeActionsAtCoordinates(coords, fieldData, ARConstantsEngine.VISUALIZE, pressEnterAfter);
                }
            } else if (typeCommand.equals(ARConstantsEngine.COORD_CLICK)) {
                message = "Coordinates Click";
                for (String coords : coordinates) {
                    //                    executeActionsAtCoordinates(coords, fieldData, ARConstants.CLICK,
                    // pressEnterAfter);
                    clickElementAtCoordinates(coords);
                }
            } else if (typeCommand.equals(ARConstantsEngine.COORD_INSERT)) {
                message = "Coordinates Insert";
                if (pressEnterAfter) {
                    message = "Coordinates Insert with <ENTER>";
                }
                for (String coords : coordinates) {
                    //                    executeActionsAtCoordinates(coords, fieldData, ARConstants.INSERT,
                    // pressEnterAfter);
                    setValueAtCoordinates(coords, fieldData.getValue());
                }
                //                insertElement(element, fieldData.getValue());
            } else if (typeCommand.equals(ARConstantsEngine.COORD_MOVE_CLICK_RED)) {
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

    private void clearElement(WebElement element) {
        JavascriptExecutor js = (JavascriptExecutor) currentDriver;
        js.executeScript("arguments[0].value='';", element);

        Actions actions = new Actions(currentDriver);
        actions.moveToElement(element).perform();
    }

    private void insertElement(WebElement element, String text) {
        JavascriptExecutor js = (JavascriptExecutor) currentDriver;
        js.executeScript("arguments[0].value=arguments[1];", element, text);

        Actions actions = new Actions(currentDriver);
        actions.moveToElement(element).perform();
    }

    public boolean setValueAtCoordinates(String savedCoords, String textToSet) {

        try {
            String[] coordinates = savedCoords.split(ARConstantsEngine.FIELDS_SEPARATOR);
            double temp1 = Double.parseDouble(coordinates[0]);
            double temp2 = Double.parseDouble(coordinates[1]);

            JavascriptExecutor jsExecutor = (JavascriptExecutor) currentDriver;

            String script = "const temp1 = Number(arguments[0]);\n" + "const temp2 = Number(arguments[1]);\n"
                    + "console.log('temp1', temp1);\n"
                    + "console.log('temp2', temp2);\n"
                    + "const elementAtPoint = document.elementFromPoint(temp1, temp2);\n"
                    + "if (elementAtPoint && (elementAtPoint.tagName === 'INPUT' || elementAtPoint.tagName === 'TEXTAREA')) {\n"
                    + "\telementAtPoint.value = \"" + textToSet + "\";\n"
                    + "} else if (elementAtPoint && elementAtPoint.isContentEditable) {\n"
                    + "\telementAtPoint.textContent = arguments[2];\n"
                    + "} else {\n"
                    + "\tconsole.log(\"No suitable element (input, textarea, or contenteditable) found at coordinates (\" + arguments[0] + \", \" + arguments[1] + \")\");\n"
                    + "}";

            jsExecutor.executeScript(script, temp1, temp2, textToSet);
            return true;
        } catch (Exception ignore) {
            return false;
        }
    }

    public boolean clearValueAtCoordinates(String savedCoords) {

        try {
            String[] coordinates = savedCoords.split(ARConstantsEngine.FIELDS_SEPARATOR);
            double temp1 = Double.parseDouble(coordinates[0]);
            double temp2 = Double.parseDouble(coordinates[1]);
            JavascriptExecutor jsExecutor = (JavascriptExecutor) currentDriver;

            String script =
                    """
                                function getElementAtCoordinates(x, y) {
                                  return document.elementFromPoint(x, y);
                                }

                                const elementAtPoint = getElementAtCoordinates(arguments[0], arguments[1]);

                                if (elementAtPoint && (elementAtPoint.tagName === 'INPUT' || elementAtPoint.tagName === 'TEXTAREA')) {
                                  elementAtPoint.value = '';
                                } else if (elementAtPoint && elementAtPoint.isContentEditable) {
                                  elementAtPoint.textContent = '';
                                } else {
                                  console.log("No suitable element (input, textarea, or contenteditable) found at coordinates (" + arguments[0] + ", " + arguments[1] + ")");
                                }
                            """;

            jsExecutor.executeScript(script, temp1, temp2);
            return true;
        } catch (Exception ignore) {
            return false;
        }
    }

    public boolean clickElementAtCoordinates(String savedCoords) {
        try {
            String[] coordinates = savedCoords.split(ARConstantsEngine.FIELDS_SEPARATOR);
            double temp1 = Double.parseDouble(coordinates[0]);
            double temp2 = Double.parseDouble(coordinates[1]);
            JavascriptExecutor jsExecutor = (JavascriptExecutor) currentDriver;
            String script =
                    """
                                function getElementAtCoordinates(x, y) {
                                  return document.elementFromPoint(x, y);
                                }

                                const elementAtPoint = getElementAtCoordinates(arguments[0], arguments[1]);

                                if (elementAtPoint) {
                                  elementAtPoint.click();
                                } else {
                                  console.log("No element found at coordinates (" + arguments[0] + ", " + arguments[1] + ")");
                                }
                            """;

            jsExecutor.executeScript(script, temp1, temp2);
            return true;
        } catch (Exception ignore) {
            return false;
        }
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
        String[] coordinates = savedCoordinates.split(ARConstantsEngine.FIELDS_SEPARATOR);
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

            ((JavascriptExecutor) this.currentDriver).executeScript(script, xCoord, yCoord);

            if (pressEnterAfter) {
                boolean respAction = sendActionEnter(xCoord, yCoord);
                if (!respAction) {
                    sendEnterWithJS();
                }
            }

            return "Success Move And Click -> Red Circle";

        } catch (Exception error) {
            return "Failed Move And Click -> Red Circle";
        }
    }

    public FieldData getBlockDetailsById(List<BlockLoadDTO> blocksLoaded, InstructionLoad currentInstruction) {
        for (BlockLoadDTO block : blocksLoaded) {
            if (block.getId() != null && block.getId().equals(currentInstruction.getParentBlockId())) {
                FieldData blockDetails = new FieldData(
                        currentInstruction.getId() + ":" + block.getId() + ":" + block.getBlockOrderNumber() + ":"
                                + block.getName().trim(),
                        currentInstruction.getOperation());
                return blockDetails;
            }
        }
        return null; // or throw an exception if the block is not found
    }

    public int getBlockOrderNumber(List<BlockLoadDTO> blocksLoaded, Integer parentBlockId) {
        for (BlockLoadDTO block : blocksLoaded) {
            if (block.getId() != null && block.getId().equals(parentBlockId)) {
                return block.getBlockOrderNumber();
            }
        }
        return -1;
    }

    public FieldData getInstructionDetailsById(
            List<InstructionLoad> InstructionLoadS, InstructionLoad currentInstruction) {
        for (InstructionLoad instParent : InstructionLoadS) {
            if (instParent.getId() != null && instParent.getId().equals(currentInstruction.getParentId())) {
                FieldData blockDetails = new FieldData(
                        currentInstruction.getId() + ":" + instParent.getId() + ":"
                                + instParent.getName().trim(),
                        currentInstruction.getOperation());
                return blockDetails;
            }
        }
        return null; // or throw an exception if the block is not found
    }

    public Map<String, Integer[]> getLoopAndRefreshLoops(List<InstructionLoad> InstructionLoadS) {
        // Step 2: Filter rows where actions = "REFRESH_LOOP" or "LOOP" and collect into the map
        Map<String, Integer[]> mapRefreshLoops = new HashMap<>();

        for (InstructionLoad instruction : InstructionLoadS) {
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
            logOperations.info("Key: " + key + ", Value: " + valuesAsString);
        }

        return mapRefreshLoops;
    }

    public Set<Integer> getParentIdsForLoop(List<InstructionLoad> InstructionLoadS) {
        return InstructionLoadS.stream()
                .filter(instruction -> "REFRESH_LOOP".equalsIgnoreCase(instruction.getActions())
                        || "LOOP".equalsIgnoreCase(instruction.getActions()))
                .map(InstructionLoad::getParentId)
                .collect(Collectors.toSet());
    }

    public Set<Integer> getAllOutputsPerBlock(List<InstructionLoad> InstructionLoadS) {
        return InstructionLoadS.stream()
                .filter(instruction -> instruction.getActions() != null
                        && instruction.getActions().trim().toUpperCase().startsWith("O:"))
                .map(InstructionLoad::getId)
                .collect(Collectors.toSet());
    }

    public void logAndReport(
            ARExecution.ConditionStatus currentCondition,
            boolean excelReport,
            boolean logOperation,
            long blockStartTime,
            String blockReportName,
            boolean success,
            String[] action,
            FieldData msgBlock,
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

    public ARExecution.ConditionStatus updateProgressSuccess(
            boolean success, ARExecution.ConditionStatus currentCondition) {
        // It Gets last Progress Status
        // Machine State
        if (currentCondition.equals(ARExecution.ConditionStatus.IF)) {
            return success ? ARExecution.ConditionStatus.IF_PASSED : ARExecution.ConditionStatus.IF_FAILED;
        } else if (currentCondition.equals(ARExecution.ConditionStatus.ELSEIF)) {
            return success ? ARExecution.ConditionStatus.ELSEIF_PASSED : ARExecution.ConditionStatus.ELSEIF_FAILED;
        } else if (currentCondition.equals(ARExecution.ConditionStatus.ELSE)) {
            return success ? ARExecution.ConditionStatus.ELSE_PASSED : ARExecution.ConditionStatus.ELSE_FAILED;
        } else if (currentCondition.equals(ARExecution.ConditionStatus.ENDIF)) {
            return ARExecution.ConditionStatus.NONE;
        }
        return ARExecution.ConditionStatus.NONE;
    }

    public int checkActionToJump(
            String action,
            ARExecution.ConditionStatus progressCondition,
            Map<String, List<Integer>> mapConditional,
            int parentBlockCondition,
            int currentIndex) {
        if (action.equalsIgnoreCase(ARConstantsEngine.ELSEIF)) {
            // Goes to the ENDIF (ENDIF index + 1);
            return searchMapConditional(
                    mapConditional, parentBlockCondition, ARExecution.ConditionStatus.ENDIF, currentIndex, true);

        } else if (action.equalsIgnoreCase(ARConstantsEngine.ELSE)) {
            // Goes to the ENDIF (ENDIF index + 1);
            return searchMapConditional(
                    mapConditional, parentBlockCondition, ARExecution.ConditionStatus.ENDIF, currentIndex, true);

        } else if (action.equalsIgnoreCase(ARConstantsEngine.ELSE)) {
            // Goes to the ENDIF (ENDIF index + 1);
            return searchMapConditional(
                    mapConditional, parentBlockCondition, ARExecution.ConditionStatus.ENDIF, currentIndex, true);
        }
        return 0;
    }

    public Map<WebElement, List<WebElement>> getIframeElementsMap() {
        iframeElementsMap = new HashMap<>();

        if (this.currentDriver != null) {
            // Get all iframe elements on the page
            List<WebElement> iframeList = this.currentDriver.findElements(By.tagName("iframe"));
            logOperations.info("Number of iframes found: " + iframeList.size());

            for (WebElement iframe : iframeList) {
                try {
                    // Switch to the iframe
                    this.currentDriver.switchTo().frame(iframe);

                    // Get all elements inside the iframe
                    List<WebElement> elementsInsideIframe = this.currentDriver.findElements(By.xpath("//*"));
                    iframeElementsMap.put(iframe, elementsInsideIframe);

                    logOperations.info("Iframe contains " + elementsInsideIframe.size() + " elements");
                } catch (Exception e) {
                    logOperations.warn("Could not access iframe: " + e.getMessage());
                } finally {
                    // Switch back to the main page
                    this.currentDriver.switchTo().defaultContent();
                }
            }

            iframeInputLocator.initializeIframeInputLocator(iframeElementsMap, this.currentDriver);
        }
        return iframeElementsMap;
    }

    public ElementDTO convertTargetToElementDTO(TargetElement targetElement) {
        if (targetElement == null) {
            return null;
        }

        ElementDTO elementDTO = new ElementDTO();

        elementDTO.setTagName(targetElement.getTagName());
        elementDTO.setXPath(targetElement.getXPath());
        elementDTO.setSomeText(targetElement.getSomeText());
        elementDTO.setAttribId(targetElement.getAttribId());
        elementDTO.setAttribName(targetElement.getAttribName());
        elementDTO.setCoordinates(targetElement.getCoordinates());
        elementDTO.setAttributeData(targetElement.getAttributeData());
        elementDTO.setCustomXPath(targetElement.getCustomXPath());
        elementDTO.setIFrameXPath(targetElement.getIFrameXPath());

        elementDTO.setShadowHost(targetElement.getShadowHost());
        elementDTO.setShadowRoot(targetElement.getShadowRoot());
        elementDTO.setNestedShadow(targetElement.getNestedShadow());
        elementDTO.setCssSelector(targetElement.getCssSelector());

        elementDTO.setAttributeValue(targetElement.getAttributeValue());
        elementDTO.setAttributeType(targetElement.getAttributeType());
        elementDTO.setSearchAttributeValue(
                targetElement.getSearchAttributeValue()); // Assuming this is not directly available in TargetElement
        elementDTO.setAutoScroll(targetElement.getAutoScroll());
        elementDTO.setAutoEnter(targetElement.getAutoEnter());

        // Determine typeElement based on tagType
        if (targetElement.getTagType() != null) {
            switch (targetElement.getTagType()) {
                case BUTTON:
                case ANCHOR:
                case OPTION:
                case MAT_SELECT:
                    elementDTO.setTypeElement("tagName-Found");
                    elementDTO.setTagName("button");
                    break;
                case INPUT:
                case TEXT_AREA:
                    elementDTO.setTypeElement("tagName-Found");
                    elementDTO.setTagName("input");
                    break;
                case OUTPUT:
                case PARAGRAPH:
                case HEADER:
                case LABEL:
                case FOR_LABEL:
                case DIV:
                case STRONG:
                case SPAN:
                    elementDTO.setTypeElement("tagName-Found");
                    elementDTO.setTagName("output");
                    break;
                case IFRAME:
                    elementDTO.setTypeElement("iframe");
                    elementDTO.setTagName("iframe");
                    break;
                default:
                    elementDTO.setTypeElement("tagName-Found"); // Default to tag name
                    break;
            }
        } else {
            elementDTO.setTypeElement("tagName-Found"); // Default to tag name if tagType is null
        }

        return elementDTO;
    }

    public TargetElement defineSearchReturn(ElementDTO elemenDTO, TargetElement targetDefine) {
        if (targetDefine == null || targetDefine.getElement() == null) {
            if (targetDefine == null) {
                targetDefine = new TargetElement();
            }

            // Reset Previous Values
            targetDefine.setAttribId(elemenDTO.getAttribId());
            targetDefine.setAttribName(elemenDTO.getAttribName());
            targetDefine.setTagName(elemenDTO.getTagName());
            targetDefine.setSomeText(elemenDTO.getSomeText());
            targetDefine.setCoordinates(elemenDTO.getCoordinates());

            targetDefine.setXPath(elemenDTO.getXPath());
            targetDefine.setCurrentXPath(elemenDTO.getXPath());

            targetDefine.setIFrameXPath(elemenDTO.getIFrameXPath());

            targetDefine.setTagName(elemenDTO.getTagName());

            targetDefine.setShadowHost(elemenDTO.getShadowHost());
            targetDefine.setShadowRoot(elemenDTO.getShadowRoot());
            targetDefine.setCssSelector(elemenDTO.getCssSelector());
            targetDefine.setNestedShadow(elemenDTO.getNestedShadow());

            targetDefine.setAttributeData(elemenDTO.getAttributeData());
            targetDefine.setCustomXPath(elemenDTO.getCustomXPath());

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

            targetDefine.setXPathWorkedFirst(ARConstantsEngine.REGULAR_XPATH);

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
                targetDefine.setTagName("input");
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
                targetDefine.setTagName("label");
            } else if (elemenDTO.getTagName().equalsIgnoreCase(WebElementTagNameEnum.IFRAME.getValue())) {
                targetDefine.setTagType(WebElementTagNameEnum.IFRAME);
                targetDefine.setIconType(WebElementIcon.IFRAME);
            } else {
                targetDefine.setTagType(WebElementTagNameEnum.OUTPUT);
                targetDefine.setIconType(WebElementIcon.OUTPUT);
                targetDefine.setTagName("label");
            }
        }
        return targetDefine;
    }

    public ElementDTO buildElementDTO(InstructionLoad instructionDTO) {
        // Reset Previous Values
        ElementDTO elemenDTO = new ElementDTO();
        elemenDTO.setTagName(instructionDTO.getTagName());
        elemenDTO.setCoordinates(instructionDTO.getCoordinates());
        elemenDTO.setXPath(instructionDTO.getXpath());
        elemenDTO.setIFrameXPath(instructionDTO.getIFrameXPath());

        elemenDTO.setShadowHost(instructionDTO.getShadowHost());
        elemenDTO.setShadowRoot(instructionDTO.getShadowRoot());
        elemenDTO.setCssSelector(instructionDTO.getCssSelector());
        //            elemenDTO.setNestedShadow(instructionDTO.getNestedShadow());
        elemenDTO.setCustomXPath(instructionDTO.getXpath());

        return elemenDTO;
    }

    // TODO MORE INTELLIGENT  LOGIC
    public TargetElement defineNameTitles(TargetElement target) {

        try {
            String tagNameDefined = target.getDefinedName() != null ? target.getDefinedName() : target.getTagName();
            WebElement targetElem = target.getElement();

            // Check element tag names
            boolean isAnchor = target.getTagName().equalsIgnoreCase(WebElementTagNameEnum.ANCHOR.getValue());
            boolean isOption = target.getTagName().equalsIgnoreCase(WebElementTagNameEnum.OPTION.getValue());

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
            boolean hasButton = target.getTagName().equalsIgnoreCase("button")
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
            boolean hasParagraph =
                    !Strings.isNullOrEmpty(textLabel) && target.getTagName().equalsIgnoreCase("p");
            boolean hasSpan =
                    !Strings.isNullOrEmpty(textLabel) && target.getTagName().equalsIgnoreCase("span");
            boolean hasDiv =
                    !Strings.isNullOrEmpty(textLabel) && target.getTagName().equalsIgnoreCase("div");

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
            if (isLabel) {
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
                target = setElementText(target, target.getTagName(), tagNameDefined);
            } else {
                //                if (tagNameDefined.equalsIgnoreCase("input")) {
                //                    target.setTagType(WebElementTagNameEnum.INPUT);
                //                }else  if (tagNameDefined.equalsIgnoreCase("input")) {
                //                    target.setTagType(WebElementTagNameEnum.OUTPUT);
                //                } else  if (tagNameDefined.equalsIgnoreCase("input")) {
                //                    target.setTagType(WebElementTagNameEnum.OUTPUT);
                //                }
                target = setElementText(target, target.getTagName(), ARConstantsEngine.VALUE_NO_IDENTIFICATION);
            }

        } catch (Exception e) {
            logOperations.warn("Cannot define Target Name Titles");
        }
        return target;
    }

    private TargetElement setElementText(TargetElement target, String nameLabelText, String nameFieldText) {
        target.setNameLabel(nameLabelText == null ? "" : nameLabelText.trim().replaceAll("\\s+", " "));
        target.setNameField(nameFieldText == null ? "" : nameFieldText.trim().replaceAll("\\s+", " "));

        String nameDefinedPriority = target.getNameLabel();
        if (!Strings.isNullOrEmpty(target.getAttribId())
                || !Strings.isNullOrEmpty(target.getAttribName())
                || !Strings.isNullOrEmpty(target.getSomeText())) {
            nameDefinedPriority = (!Strings.isNullOrEmpty(target.getSomeText())
                    ? PerformActions.truncateAndNormalize(target.getSomeText(), 30)
                    : !Strings.isNullOrEmpty(target.getAttribId())
                            ? target.getAttribId()
                            : !Strings.isNullOrEmpty(target.getAttribName())
                                    ? target.getAttribName()
                                    : nameDefinedPriority);
        }

        target.setDefinedName(nameDefinedPriority);

        return target;
    }

    public TargetElement defineTagType(TargetElement targetTagType) {

        try {
            logOperations.info("Defined Name: " + targetTagType.getDefinedName());
            logOperations.info("Tag Name: " + targetTagType.getTagName());
            logOperations.info("Id: " + targetTagType.getAttribId());
            logOperations.info("Name: " + targetTagType.getAttribName());
            logOperations.info("xPath: " + targetTagType.getCurrentXPath());
            logOperations.info("Absolut xPath: " + targetTagType.getAttributeData());
            logOperations.info("Custom xPath: " + targetTagType.getCustomXPath());
            logOperations.info("iFrame xPath: " + targetTagType.getIFrameXPath());

            if (targetTagType.getCoordinates() != null) {
                String[] coords = targetTagType.getCoordinates().split(",");
                if (coords.length == 2) {
                    String coordLeft = coords[0].trim();
                    String coordRight = coords[1].trim();
                    // Print or use the extracted values
                    logOperations.info("CoordLeft: " + coordLeft);
                    logOperations.info("CoordRight: " + coordRight);
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

            logOperations.error("Could not find any Web Element with XPath/Id/Attributes values.");
        }
        return null;
    }

    private boolean isValidString(String value) {
        return value != null && !value.isBlank();
    }

    public boolean isClickable(WebElement element, String tagNameDefined) {
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
        jsExecutor = (JavascriptExecutor) driver;

        for (String xpath : xpaths) {
            try {
                Object result = jsExecutor.executeScript("return document.evaluate(\"" + xpath
                        + "\", document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;");
                if (result instanceof WebElement) {
                    return (WebElement) result;
                }
            } catch (Exception e) {
                // Log or handle the exception if needed
                logOperations.error("Error locating element with XPath: " + xpath + ". Exception: " + e.getMessage());
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

    public WebElement findShadowElementByCssSelector(String shadowLocator, String cssSelector) {
        try {
            // Find the shadow host
            WebElement shadowHost = this.currentDriver.findElement(By.cssSelector(shadowLocator));
            SearchContext shadowRoot = shadowHost.getShadowRoot();
            return shadowRoot.findElement(By.cssSelector(cssSelector));
        } catch (Exception e) {

        }
        return null;
    }

    public InstructionLoad buildNewInstruction(
            WebElementTagNameEnum forceTag,
            String actionReq,
            boolean identityHover,
            Integer orderNumber,
            TargetElement targetBuild) {

        InstructionLoad loop = new InstructionLoad();
        loop.setActionCustomMaxWaitSec(30);
        loop.setDescription("loop desc");
        loop.setCodified(false);
        loop.setInstructionOrderNumber(orderNumber);
        loop.setOptional(false);
        loop.setInstructionActive(true);
        loop.setXpath(targetBuild.getXPath());

        loop.setTagName(targetBuild.getTagName());
        loop.setShadowHost(targetBuild.getShadowHost());
        loop.setShadowRoot(targetBuild.getShadowRoot());
        loop.setCssSelector(targetBuild.getCssSelector());

        String action = buildAction(forceTag, actionReq, identityHover, targetBuild);
        loop.setActions(action);
        loop.setExportToABR(true);

        return loop;
    }

    private String buildAction(
            WebElementTagNameEnum forceTag, String actionReq, boolean identityHover, TargetElement targetBuild) {

        if (identityHover) {
            return handleIdentityHover(actionReq, forceTag, targetBuild.getNameLabel(), targetBuild.getClickElement());
        } else {
            return handleTargetBuildAction(
                    forceTag, targetBuild, targetBuild.getNameLabel(), targetBuild.getClickElement());
        }
    }

    private String handleIdentityHover(
            String actionReq, WebElementTagNameEnum forceTag, String nameLabel, Boolean clickElement) {
        return switch (actionReq.toUpperCase()) {
            case ARConstantsEngine.INSERT -> buildInsertAction(forceTag, nameLabel);
            case ARConstantsEngine.OUTPUT -> ARConstantsEngine.OUTPUT
                    + ARConstantsEngine.ACTION_SPECIFICATIONS_SPLITTER
                    + nameLabel;
            case ARConstantsEngine.OTHER -> ARConstantsEngine.OTHER
                    + ARConstantsEngine.ACTION_SPECIFICATIONS_SPLITTER
                    + nameLabel;
            case ARConstantsEngine.CLICK -> ARConstantsEngine.CLICK;
            default -> clickElement
                    ? ARConstantsEngine.CLICK
                    : ARConstantsEngine.INSERT + ARConstantsEngine.ACTION_SPECIFICATIONS_SPLITTER + nameLabel;
        };
    }

    private String buildInsertAction(WebElementTagNameEnum forceTag, String nameLabel) {
        // Action is always plain "I:<field>". The "press ENTER after" behaviour now lives
        // in the force_coordinates flag column ('E' bit), not the action code.
        return ARConstantsEngine.INSERT + ARConstantsEngine.ACTION_SPECIFICATIONS_SPLITTER + nameLabel;
    }

    private String handleTargetBuildAction(
            WebElementTagNameEnum forceTag, TargetElement targetBuild, String nameLabel, boolean clickElement) {
        if (targetBuild.getTagType() == null) {
            return clickElement
                    ? ARConstantsEngine.CLICK
                    : ARConstantsEngine.INSERT + ARConstantsEngine.ACTION_SPECIFICATIONS_SPLITTER + nameLabel;
        }

        return switch (targetBuild.getTagType()) {
            case INPUT -> buildInsertAction(forceTag, nameLabel);
            case HIDDEN -> ARConstantsEngine.INSERT
                    + ARConstantsEngine.ACTION_SPECIFICATIONS_SPLITTER
                    + nameLabel
                    + ARConstantsEngine.ACTION_SPECIFICATIONS_SPLITTER
                    + ARConstantsEngine.HIDDEN;
            case BUTTON -> ARConstantsEngine.CLICK;
            default -> ARConstantsEngine.OUTPUT + ARConstantsEngine.ACTION_SPECIFICATIONS_SPLITTER + nameLabel;
        };
    }

    public Map<String, String> defineSavedReferenced(TargetElement targetRefs) {

        // Handle XPath and attribute cases
        processXPathAndAttributes(targetRefs, targetRefs.getSavedReferences());

        // If no match for XPath or attributes, process coordinates or dynamic creation
        if (targetRefs.getSavedReferences().isEmpty()) {
            processDynamicCreation(targetRefs, targetRefs.getSavedReferences());
        }

        // Process coordinates
        processCoordinates(targetRefs, targetRefs.getSavedReferences());

        return targetRefs.getSavedReferences();
    }

    private void processXPathAndAttributes(TargetElement targetRefs, Map<String, String> savedReferences) {

        // --- existing ---
        addAttributeIfNotNull(savedReferences, "xpath", targetRefs.getXPath());
        addAttributeIfNotNull(savedReferences, "currentXPath", targetRefs.getCurrentXPath());
        addAttributeIfNotNull(savedReferences, "customXPath", targetRefs.getCustomXPath());
        addAttributeIfNotNull(savedReferences, "attributeID", targetRefs.getAttribId());
        addAttributeIfNotNull(savedReferences, "attributeName", targetRefs.getAttribName());
        addAttributeIfNotNull(savedReferences, "searchAttribute", targetRefs.getSearchAttributeValue());
        addAttributeIfNotNull(savedReferences, "attribute", targetRefs.getAttributeValue());
        addAttributeIfNotNull(savedReferences, "someText", targetRefs.getSomeText());

        // --- best locator extraction (from attributeData first, then fall back to attribId/attribName) ---
        AttributeData[] attrs = targetRefs.getAttributeData();

        String id = getAttr(attrs, "id");
        String name = getAttr(attrs, "name");
        String type = getAttr(attrs, "type");
        String tag = targetRefs.getTagName() != null ? targetRefs.getTagName().toLowerCase() : null;

        if (id == null || id.isBlank()) id = targetRefs.getAttribId();
        if (name == null || name.isBlank()) name = targetRefs.getAttribName();

        // --- store best locators (ranked) ---
        addIfNotBlank(savedReferences, "locator.best.byId", id); // By.id(...)
        addIfNotBlank(savedReferences, "locator.best.byName", name); // By.name(...)

        // CSS
        if (id != null && !id.isBlank()) {
            addIfNotBlank(savedReferences, "locator.css.id", "#" + id); // By.cssSelector("#id")
            if (tag != null && !tag.isBlank()) {
                addIfNotBlank(savedReferences, "locator.css.tagId", tag + "#" + id); // input#password
            }
        }
        if (tag != null && name != null && !name.isBlank()) {
            addIfNotBlank(savedReferences, "locator.css.name", tag + "[name='" + name + "']"); // input[name='password']
        }

        // XPath
        if (tag == null || tag.isBlank()) tag = "*";

        if (id != null && !id.isBlank()) {
            addIfNotBlank(savedReferences, "locator.xpath.id", "//" + tag + "[@id='" + id + "']");
        }
        if (name != null && !name.isBlank()) {
            addIfNotBlank(savedReferences, "locator.xpath.name", "//" + tag + "[@name='" + name + "']");
        }
        if (name != null && !name.isBlank() && type != null && !type.isBlank()) {
            addIfNotBlank(
                    savedReferences,
                    "locator.xpath.nameType",
                    "//" + tag + "[@name='" + name + "' and @type='" + type + "']");
        }

        // Optional: store the cssSelector you already have (if it’s good)
        addIfNotBlank(savedReferences, "locator.css.generated", targetRefs.getCssSelector());

        // Optional: iframe/shadow metadata if relevant for finding context
        addIfNotBlank(savedReferences, "context.iframeXPath", targetRefs.getIFrameXPath());
        addIfNotBlank(savedReferences, "context.shadowHost", targetRefs.getShadowHost());
        addIfNotBlank(savedReferences, "context.shadowRoot", targetRefs.getShadowRoot());
    }

    private String getAttr(AttributeData[] attrs, String attrName) {
        if (attrs == null) return null;
        for (AttributeData a : attrs) {
            if (a != null && attrName.equalsIgnoreCase(a.getName())) {
                return a.getValue();
            }
        }
        return null;
    }

    private void addIfNotBlank(Map<String, String> map, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            map.put(key, value);
        }
    }

    private void addAttributeIfNotNull(Map<String, String> savedReferences, String key, String value) {
        if (!Strings.isNullOrEmpty(value)) {
            savedReferences.put(key, value);
        }
    }

    private void processDynamicCreation(TargetElement targetRefs, Map<String, String> savedReferences) {
        // Handle dynamic creation (fallback to XPath extraction)
        if (targetRefs.getElement() != null) {
            savedReferences.put("xpath", ARWebUtil.extractWebElementXPath(targetRefs.getElement()));
        }
    }

    private void processCoordinates(TargetElement targetRefs, Map<String, String> savedReferences) {

        savedReferences.put("js_coordinates", targetRefs.getCoordinates());

        try {
            // Attempt to get coordinates from the element
            Rectangle coordinates = targetRefs.getElement().getRect();

            // Compute new coordinates based on element's dimensions
            String newCoordinates = (coordinates.getX() + (coordinates.getWidth() / 2.0)) + ","
                    + (coordinates.getY() + (coordinates.getHeight() / 2.0));

            // webdriver
            savedReferences.put("coordinates", newCoordinates);
            targetRefs.setCoordinates(newCoordinates);
        } catch (Exception coords) {
            //            logOperations.error("Invalid coordinates from WebDriver Selenium");
        }

        String[] parts = targetRefs.getCoordinates().split(",");

        try {
            // Parse coordinates as doubles
            double x = Double.parseDouble(parts[0].trim());
            double y = Double.parseDouble(parts[1].trim());

            int width = 100; // Replace with actual width if available
            int height = 100; // Replace with actual height if available

            // Create a Rectangle with rounded integer values
            Rectangle coordinates = new Rectangle((int) Math.round(x), (int) Math.round(y), width, height);

            // Compute new coordinates using double precision
            String newCoordinates = (coordinates.getX() + (coordinates.getWidth() / 2.0)) + ","
                    + (coordinates.getY() + (coordinates.getHeight() / 2.0));

            // Computed
            savedReferences.put("cp_coordinates", newCoordinates);
        } catch (NumberFormatException e) {
            //            logOperations.error("Invalid coordinates from Javascript code: " +
            // targetRefs.getCoordinates());
        }
    }

    public WebElement findWebElement(TargetElement targetFind) {

        WebElement elementFound = null;

        this.currentDriver.switchTo().defaultContent();
        if (this.currentDriver.getWindowHandles().size() > 1) {
            try {
                this.currentDriver.switchTo().window(windowHandlesList.get(currentTabIndex));
            } catch (Exception ignore) {

            }
        }

        try {

            if (!Strings.isNullOrEmpty(targetFind.getShadowHost())
                    && !Strings.isNullOrEmpty(targetFind.getCssSelector())) {
                elementFound = findShadowElementByCssSelector(targetFind.getShadowHost(), targetFind.getCssSelector());
            } else if (!Strings.isNullOrEmpty(targetFind.getIFrameXPath())) {

                try {
                    WebElement iFrame = getCurrentDriver().findElement(By.xpath(targetFind.getIFrameXPath()));

                    getCurrentDriver().switchTo().frame(iFrame);
                    elementFound = getCurrentDriver().findElement(By.xpath(targetFind.getXPath()));
                } catch (Exception error) {

                    logOperations.warn("iFrame Element not Located\niFrameXPath"
                            + targetFind.getIFrameXPath()
                            + "iFrameChild: "
                            + targetFind.getXPath());
                }
            } else {
                elementFound = getCurrentDriver().findElement(By.xpath(targetFind.getXPath()));
            }

        } catch (Exception error) {
            logOperations.warn("Scope Changed - Element not Located - : " + targetFind.getXPath());
            //            performMessage.errorMessage(
            //                    "Element not Located",
            //                    "Cannot able to find the ",
            //                    "Verify the Correct Browser Version",
            //                    null,
            //                    null,
            //                    0);
            return null;
        }

        return elementFound;
    }

    public WebElement findElementByCssSelector(String cssSelector) throws Exception {
        try {
            if (cssSelector == null || cssSelector.isEmpty()) {
                throw new IllegalArgumentException("CSS Selector cannot be null or empty.");
            }

            // Escape single quotes within the CSS selector for JavaScript
            String escapedCssSelector = cssSelector.replace("'", "\\'");

            String script = "return document.querySelectorAll('" + escapedCssSelector + "')[0];";

            WebElement foundElement = (WebElement) ((JavascriptExecutor) this.currentDriver).executeScript(script);

            if (foundElement == null) {

                logOperations.warn(String.format("Element with CSS Selector \"%s\" not found.", cssSelector));
                return null;
            }
            return foundElement;

        } catch (Exception e) {

            logOperations.error(String.format(
                    "Error finding element with CSS Selector \"%s\" -> Cause: %s", cssSelector, e.getMessage()));
            return null;
        }
    }

    public WebElement findElementByCssSelector(String cssSelector, boolean byPassNotFound) throws Exception {
        WebElement element = findElementByCssSelector(cssSelector);
        if (element == null && !byPassNotFound) {
            logOperations.warn("Could not find element with CSS Selector: " + cssSelector);
            performMessage.couldNotFindElement("Could not find element with CSS Selector: " + cssSelector);
        }
        return element;
    }

    public Map<String, String> removeCurrencySymbols(Map<String, String> mapExport) {
        // Use LinkedHashMap to preserve the insertion order
        Map<String, String> cleanedMap = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : mapExport.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            String cleanedValue = removeAllCurrencySymbols(value);
            cleanedMap.put(key, cleanedValue);
        }
        return cleanedMap;
    }

    /**
     * Removes all characters that are not numbers or the decimal separator.
     *
     * @param input The string to clean.
     * @return A cleaned version of the string.
     */
    public String removeAllCurrencySymbols(String input) {
        // Remove all non-numeric and non-decimal characters (e.g., $, €, etc.)
        return input.replaceAll("[^0-9.,]", "");
    }

    public String formatLocalNumber(String numberString, String localFormat) {
        try {
            String decimalPart = "";
            String integerPart = "";

            // Find last occurrence of "," or "." as decimal separator
            int decimalIndex = Math.max(numberString.lastIndexOf(','), numberString.lastIndexOf('.'));
            if (decimalIndex != -1) {
                decimalPart = numberString.substring(decimalIndex + 1);
                integerPart = numberString.substring(0, decimalIndex).replaceAll("[^0-9]", "");
            } else {
                integerPart = numberString.replaceAll("[^0-9]", "");
            }

            // Determine formatting style
            String groupingSeparator;
            String decimalSeparator;

            if ("US".equalsIgnoreCase(localFormat)) {
                groupingSeparator = ",";
                decimalSeparator = ".";
            } else if ("EU".equalsIgnoreCase(localFormat)) {
                groupingSeparator = ".";
                decimalSeparator = ",";
            } else { // Default
                groupingSeparator = ",";
                decimalSeparator = ".";
            }

            // Rebuild integer part with grouping
            String groupedInteger = insertGroupingSeparators(integerPart, groupingSeparator);

            return decimalPart.isEmpty() ? groupedInteger : groupedInteger + decimalSeparator + decimalPart;

        } catch (Exception e) {
            logOperations.error("Error formatting number: " + numberString + " - " + e.getMessage());
            return numberString;
        }
    }

    //    private void listOperation(boolean byPassNotFound, InstructionLoad instructionDTO) {
    //
    //        /*
    //        TODO: Da rivedere, attualmente non del tutto funzionante
    //        Complex instruction string interpretation:
    //        [       0       ||       1      ||       2         ||    3    ||        4       ||  5   ||            6
    //             ]
    //
    // [backward_button||forward_button||list_elements_tag||condition||expected_results||action||sub_element_on_execute_action]
    //        */
    //        List<ComplexInstructionLoad> complexInstructionDTOS =
    // instructionDTO.getComplexInstructionLoadList();
    //        String[] complexActionParts =
    //                complexInstructionDTOS.get(0).getInstruction().split(ARConstants.COMPLEX_INSTRUCTION_SEPARATOR);
    //        List<WebElement> webElementList;
    //        WebElement forwardButton;
    //        WebElement backwardButton;
    //        boolean shouldContinue = true;
    //
    //        boolean existNextPage;
    //        do {
    //            try {
    //
    // waitForPage.until(ExpectedConditions.visibilityOfElementLocated(By.tagName(complexActionParts[2])));
    //            } catch (Exception e) {
    //
    //                        operationsLog.warn(String.format(
    //                                "Could Not Find TagName \"%s\" Criteria \"%s\" -> Cause: %s",
    //                                complexActionParts[2], By.tagName(complexActionParts[2]), e.getMessage()));
    //
    //                if (!byPassNotFound) {
    //                    performMessage.couldNotFindElement(complexActionParts[2]);
    //                }
    //            }
    //
    //            backwardButton = this.currentDriver.findElement(By.xpath(complexActionParts[0]));
    //            forwardButton = this.currentDriver.findElement(By.xpath(complexActionParts[1]));
    //            webElementList = this.currentDriver.findElements(By.tagName(complexActionParts[2]));
    //
    //            WebElement element;
    //            WebElement reasonWebElement;
    //            for (int i = 0; i < 5; i++) {
    //
    //                if (i != 0) {
    //
    //                    try {
    //                        waitForPage.until(
    //                                ExpectedConditions.visibilityOfElementLocated(By.tagName(complexActionParts[2])));
    //                        webElementList = this.currentDriver.findElements(By.tagName(complexActionParts[2]));
    //                    } catch (Exception e) {
    //
    //                                operationsLog.warn(String.format(
    //                                        "Could Not Find TagName \"%s\" Criteria \"%s\" -> Cause: %s",
    //                                        complexActionParts[2], By.tagName(complexActionParts[2]),
    // e.getMessage()));
    //
    //                        if (!byPassNotFound) {
    //                            performMessage.couldNotFindElement(complexActionParts[2]);
    //                        }
    //                    }
    //                }
    //
    //                element = webElementList.get(i);
    //                try {
    //                    Thread.sleep(1000);
    //
    //                    reasonWebElement = element.findElement(
    //                            By.xpath(".//div[@class='payments-table-field reason ng-star-inserted']"));
    //                    if (!UtilsMethods.testFixedCheck(reasonWebElement.getText())) {
    //                        continue;
    //                    }
    //
    //                    clickElement(
    //                            byPassNotFound,
    //                            element.findElement(By.xpath(
    //                                    ".//button[@test-id='web-banking-payment-core.payment-ctx-action.button']")));
    //                    clickElement(
    //                            byPassNotFound,
    //                            this.currentDriver.findElement(
    //                                    By.xpath(
    //
    // ".//button[@test-id='web-banking-payment-core.payment-ctx-action.payment-action-VIEW']")));
    //
    //                    Thread.sleep(1000);
    //                    clickElement(
    //                            byPassNotFound,
    //                            this.currentDriver.findElement(By.xpath(
    //
    // ".//button[@test-id='web-banking-common.export-to-file.single-file-button']")));
    //
    //                    Thread.sleep(1000);
    //                    clickElement(
    //                            byPassNotFound,
    //                            this.currentDriver.findElement(
    //                                    By.xpath(
    //
    // ".//avq-breadcrumb[@test-id='web-banking-portal.pages.payments-overview.breadcrumb']")));
    //                } catch (Exception e) {
    //                    operationsLog.warn("Impossible execute operation on this element: " + element.toString());
    //                }
    //            }
    //
    //            try {
    //                scrollToElement(byPassNotFound, forwardButton);
    //                clickElement(byPassNotFound, forwardButton);
    //                existNextPage = true;
    //            } catch (Exception e) {
    //                existNextPage = false;
    //            }
    //
    //        } while (existNextPage && shouldContinue);
    //    }

}
