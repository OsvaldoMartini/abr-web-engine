package com.allinweb.ch.supportTypes;

import com.allinweb.ch.builder.WebElementAttributeEnum;
import com.allinweb.ch.cryptingAlgorithm.CryptationAlgorithm;
import com.allinweb.ch.driver.ABRWebDriver;
import com.allinweb.ch.dto.BlockLoopInstructionDTO;
import com.allinweb.ch.dto.BotJobDTO;
import com.allinweb.ch.dto.ComplexInstructionDTO;
import com.allinweb.ch.dto.InstructionReferenceDTO;
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
    private static ABRPriorities abrPriorities;

    // Static block to initialize
    static {
        abrPriorities = ABRPriorities.getInstance();
    }

    private ABRWebDriver abrWebDriver;
    private WebDriver driver;
    private static Wait<WebDriver> waitForPage;
    private static Wait<WebDriver> waitForAction;
    private boolean justCalledRefreshPage = false;

    public WebPage(String driverType, String url, String optionsConfig) {

        abrWebDriver = new ABRWebDriver();

        this.driver = abrWebDriver.openDriver(url, optionsConfig);

        //        this.driver = initDriver(driverType);
        if (waitForPage == null) {
            String updateTimeout =
                    ABRPropertyManager.getInstance().getProperty(ABRPropertyEnum.WEBDRIVER_PAGE_UPDATE_TIMEOUT_SEC);
            String interactionTimeout =
                    ABRPropertyManager.getInstance().getProperty(ABRPropertyEnum.WEBDRIVER_PAGE_UPDATE_TIMEOUT_SEC);
            waitForPage = new WebDriverWait(driver, Duration.ofSeconds(Integer.parseInt(updateTimeout)));
            waitForAction = new WebDriverWait(driver, Duration.ofSeconds(Integer.parseInt(interactionTimeout)));
        }
        openBrowser(url);
    }

    public WebDriver getDriver() {
        return driver;
    }

    public WebDriver initDriver(String driverType) {
        if (driverType.equalsIgnoreCase(Constants.FIREFOX)) {
            FirefoxOptions options = new FirefoxOptions();
            options.setBinary(ABRConstants.CURRENT_PATH + "\\geckodriver.exe");
            driver = new FirefoxDriver(options);

        } else if (driverType.equalsIgnoreCase(Constants.EDGE)) {
            System.setProperty("webdriver.edge.driver", ABRConstants.CURRENT_PATH + "\\msedgedriver.exe");
            EdgeOptions options = new EdgeOptions();
            options.setExperimentalOption("useAutomationExtension", false);
            options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
            driver = new EdgeDriver(options);

        } else if (driverType.equalsIgnoreCase(Constants.CHROME)) {

            ChromeOptions options = new ChromeOptions();
            options.setBinary(ABRConstants.CURRENT_PATH + "\\chrome\\chrome.exe");
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

        } catch (Exception e) {
            ABRLogger.getInstance(ABRWebDriver.class)
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

    private WebElement locateElement(BlockLoopInstructionDTO instruction, Map<String, String> data) throws Exception {

        String instructionPath = instruction.getPath();
        String tagName = null;
        try {
            tagName = removeTrailingSlash(instructionPath);
            tagName = extractTagName(instructionPath);
        } catch (Exception e) {
            System.out.println("Error trying to get tagName" + e.getMessage());
        }
        List<InstructionReferenceDTO> instructionReferenceList = instruction.getInstructionReferenceDTOList();

        waitPage();

        // If Not Loaded get if the JobId Changed
        if (abrPriorities.getJobId() == null) {
            abrPriorities.setJobId(instruction.getBlock().getBotJob().getId());
            if (instruction.getBlock().getBotJob().getPriority() != null) {
                abrPriorities.loadPrioritiesFromString(
                        instruction.getBlock().getBotJob().getPriority());
            } else {
                abrPriorities.loadPriorities();
            }
        } else if (abrPriorities.getJobId()
                != instruction.getBlock().getBotJob().getId()) {
            abrPriorities.setJobId(instruction.getBlock().getBotJob().getId());
            if (instruction.getBlock().getBotJob().getPriority() != null) {
                abrPriorities.loadPrioritiesFromString(
                        instruction.getBlock().getBotJob().getPriority());
            } else {
                abrPriorities.loadPriorities();
            }
        }

        List<Priority> priorityList = abrPriorities.getAllPriorityList();
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
                Optional<InstructionReferenceDTO> instructionReference = instructionReferenceList.stream()
                        .filter(reference ->
                                priority.getPriorityType().toString().equalsIgnoreCase(reference.getReferenceType()))
                        .findFirst();

                // Print or process the first matching instruction reference
                instructionReference.ifPresent((f) -> System.out.println(String.format(
                        "Search for %s   Type:  %s   Value: %s",
                        priority.getName(), f.getReferenceType(), f.getValue())));

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
                            List<WebElement> foundElementList = driver.findElements(criteria);

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
                                        System.out.println("Could not fin the element");
                                    }
                                } else if (instruction.getActionCustomMaxWaitSec() != null) {
                                    try {

                                        new WebDriverWait(
                                                        driver,
                                                        Duration.ofSeconds(instruction.getActionCustomMaxWaitSec()))
                                                .until(ExpectedConditions.presenceOfElementLocated(criteria));
                                    } catch (Exception e) {
                                        System.out.println("Could not fin the element");
                                    }
                                } else {
                                    try {

                                        waitForAction.until(ExpectedConditions.visibilityOfElementLocated(criteria));
                                    } catch (Exception e) {
                                        System.out.println("Could not fin the element");
                                    }
                                }
                                int k = 0;
                                //                            MAYBE THIS SHOUL BE NOT NECESSARY  USE UNIQUE ID   OR
                                // SESSION  SAVED TO GET THE SAME XPATHORELEMENT
                                while (elementFound == null && k < foundElementList.size()) {
                                    String xpath = ABRWebUtil.extractXPath(
                                            foundElementList.get(k).toString());
                                    Optional<InstructionReferenceDTO> xpathReference = instructionReferenceList.stream()
                                            .filter(ref ->
                                                    ref.getReferenceType().equals(PriorityTypeEnum.xpath.name()))
                                            .findFirst();
                                    if (xpathReference.isPresent()
                                            && xpath.equals(xpathReference.get().getValue())) {
                                        elementFound = foundElementList.get(k);
                                        break;
                                    }
                                    k++;
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

    private void executeActionsAtInstructionCoordinates(BlockLoopInstructionDTO instruction, Map<String, String> data)
            throws Exception {

        List<Priority> priorityList = ABRPriorities.getAllPriorityList();
        Optional<Priority> priority = priorityList.stream()
                .filter(p -> p.getPriorityType() == PriorityTypeEnum.coordinates)
                .findFirst();
        if (priority.isPresent()) {
            List<InstructionReferenceDTO> instructionReferenceList = instruction.getInstructionReferenceDTOList();
            Optional<InstructionReferenceDTO> reference = instructionReferenceList.stream()
                    .filter(ref -> ref.getReferenceType().equals(priority.get().getName()))
                    .findFirst();
            int x = 0;
            int y = 0;
            int xCoord = 0;
            int yCoord = 0;
            if (reference.isPresent()) {
                String[] coordinates = reference.get().getValue().split(ABRConstants.FIELDS_SEPARATOR);
                x = Integer.parseInt(coordinates[0]);
                y = Integer.parseInt(coordinates[1]);
                int maxHeight = driver.manage().window().getSize().getHeight();
                int maxWidth = driver.manage().window().getSize().getWidth();
                int offsetY = y - maxHeight;
                int offsetX = x - maxWidth;
                xCoord = x > maxWidth ? x - offsetX : x;
                yCoord = y > maxHeight ? y - offsetY : y;
            }
            String[] actions = instruction.getActions().split(ABRConstants.ACTIONS_AND_PATHS_SPLITTER);
            for (String action : actions) {
                switch (String.valueOf(action.charAt(0))) {
                    case Constants.VISUALIZE:
                        scrollToCoordinates(x, y);
                        break;
                    case Constants.CLICK:
                        scrollToCoordinates(x, y);
                        onHoldForSeconds(null);
                        clickAtCoordinates(xCoord, yCoord);
                        break;
                    case Constants.INSERT:
                        scrollToCoordinates(x, y);
                        onHoldForSeconds(null);
                        clickAtCoordinates(xCoord, yCoord);
                        onHoldForSeconds(null);
                        typeCharacters(instruction, action, data);
                        break;
                    case Constants.HOLD:
                        onHoldForSeconds(instruction);
                        break;
                    case Constants.REFRESH:
                        refreshPage();
                        break;
                    case Constants.QUIT:
                        quit(0);
                        break;
                    case Constants.SCREEN:
                        // screenshot();
                        break;
                    case Constants.EXTRACT:
                        break;
                    case Constants.LIST_OPERATION:
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

    private void typeCharacters(BlockLoopInstructionDTO instruction, String action, Map<String, String> data) {
        String value = null;
        if (data != null) {
            String[] arr = UtilsMethods.splitIfContains(action, Constants.ACTION_SPECIFICATIONS_SPLITTER);
            if (arr.length > 1) {
                String dataFieldName = arr[1].split(Constants.PATH_FIELD_SUBSTITUTION)[0];
                value = data.get(dataFieldName);
            }
        } else {
            value = instruction.getDefaultValue();
        }
        if (instruction.isEncrypted()) {
            value = CryptationAlgorithm.decrypt(value);
        }
        new Actions(driver).sendKeys(value).perform();
    }

    public void performActions(Map<String, String> data, BlockLoopInstructionDTO instruction) throws Exception {
        WebElement instructionElement = null;
        String[] actions = instruction.getActions().split(Constants.ACTIONS_AND_PATHS_SPLITTER);

        if (!StringUtils.isBlank(instruction.getPath())) {
            instructionElement = locateElement(instruction, data);
        }

        if (instructionElement != null) {
            for (String action : actions) {
                switch (String.valueOf(action.charAt(0))) {
                    case Constants.VISUALIZE:
                        scrollToElement(instructionElement);
                        break;
                    case Constants.CLICK:
                        clickElement(instructionElement);
                        break;
                    case Constants.INSERT:
                        insertInElement(instructionElement, data, action, instruction);
                        break;
                    case Constants.LIST_OPERATION:
                        listOperation(instruction, data);
                        break;
                    case Constants.HOLD:
                        onHoldForSeconds(instruction);
                        break;
                    case Constants.REFRESH:
                        refreshPage();
                        break;
                    case Constants.QUIT:
                        quit(0);
                        break;
                    case Constants.EXTRACT:
                        insertValueFieldNameInExcel(instructionElement, instruction, action);
                        break;
                    case Constants.SCREEN:
                        break;
                }
                onHoldForSeconds(null);
            }
        } else {
            executeActionsAtInstructionCoordinates(instruction, data);
            onHoldForSeconds(null);
        }
    }

    private void insertValueFieldNameInExcel(WebElement element, BlockLoopInstructionDTO instruction, String action) {
        String innerHTMLValue = element.getAttribute(WebElementAttributeEnum.INNER_HTML.getValue());
        if (innerHTMLValue.contains("<div")) {
            int lastIndexOfDiv = innerHTMLValue.lastIndexOf("<div");
            innerHTMLValue = innerHTMLValue.substring(lastIndexOfDiv + 1);
            int firstIndexOfOpenTag = innerHTMLValue.indexOf("<");
            int firstIndexOfCloseTag = innerHTMLValue.indexOf(">");
            innerHTMLValue = innerHTMLValue.substring(firstIndexOfCloseTag + 1, firstIndexOfOpenTag);
        }
        String fieldName = null;
        String[] arr = UtilsMethods.splitIfContains(action, Constants.ACTION_SPECIFICATIONS_SPLITTER);
        if (arr.length > 1) {
            fieldName = arr[1].split(Constants.PATH_FIELD_SUBSTITUTION)[0];
        }

        BotJobDTO botJob = instruction.getBlock().getBotJob();
        new ExcelWriter(botJob).withPurpose("excel").insertValueFieldName(fieldName, innerHTMLValue);
    }

    private void listOperation(BlockLoopInstructionDTO instructionDTO, Map<String, String> data) {

        /*
        TODO: Da rivedere, attualmente non del tutto funzionante
        Complex instruction string interpretation:
        [       0       ||       1      ||       2         ||    3    ||        4       ||  5   ||            6                ]
        [backward_button||forward_button||list_elements_tag||condition||expected_results||action||sub_element_on_execute_action]
        */
        List<ComplexInstructionDTO> complexInstructionDTOS = instructionDTO.getComplexInstrucions();
        String[] complexActionParts =
                complexInstructionDTOS.get(0).getInstruction().split(Constants.COMPLEX_INSTRUCTION_SEPARATOR);
        List<WebElement> webElementList;
        WebElement forwardButton;
        WebElement backwardButton;
        boolean shouldContinue = true;

        boolean existNextPage;
        do {
            waitForPage.until(ExpectedConditions.visibilityOfElementLocated(By.tagName(complexActionParts[2])));
            backwardButton = driver.findElement(By.xpath(complexActionParts[0]));
            forwardButton = driver.findElement(By.xpath(complexActionParts[1]));
            webElementList = driver.findElements(By.tagName(complexActionParts[2]));

            WebElement element;
            WebElement reasonWebElement;
            for (int i = 0; i < 5; i++) {

                if (i != 0) {
                    waitForPage.until(ExpectedConditions.visibilityOfElementLocated(By.tagName(complexActionParts[2])));
                    webElementList = driver.findElements(By.tagName(complexActionParts[2]));
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
                    clickElement(driver.findElement(By.xpath(
                            ".//button[@test-id='web-banking-payment-core.payment-ctx-action.payment-action-VIEW']")));

                    Thread.sleep(1000);
                    clickElement(driver.findElement(
                            By.xpath(".//button[@test-id='web-banking-common.export-to-file.single-file-button']")));

                    Thread.sleep(1000);
                    clickElement(driver.findElement(By.xpath(
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

    private void insertInElement(
            WebElement element,
            Map<String, String> data,
            String singleInstruction,
            BlockLoopInstructionDTO instructionDTO)
            throws Exception {
        UtilsMethods.exceptionIfNullWebElement(element);
        waitForAction.until(ExpectedConditions.visibilityOf(element));
        if (data != null) {
            String[] arr = UtilsMethods.splitIfContains(singleInstruction, Constants.ACTION_SPECIFICATIONS_SPLITTER);
            if (arr.length > 1) {
                String dataFieldName = arr[1].split(Constants.PATH_FIELD_SUBSTITUTION)[0];

                String value = data.get(dataFieldName);
                if (instructionDTO.isEncrypted()) {
                    value = CryptationAlgorithm.decrypt(value);
                }

                if (value != null) {
                    element.sendKeys(value);
                    element.sendKeys(Keys.TAB);

                } else {
                    element.sendKeys(UtilsMethods.generateRandomID(10));
                    element.sendKeys(Keys.TAB);
                }
            }
        } else if (instructionDTO.getDefaultValue() != null) {
            String defaultValue = instructionDTO.getDefaultValue();
            if (instructionDTO.isEncrypted()) {
                defaultValue = CryptationAlgorithm.decrypt(defaultValue);
            }
            element.sendKeys(defaultValue);
        }
    }

    private void scrollToElement(WebElement element) throws Exception {
        UtilsMethods.exceptionIfNullWebElement(element);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
    }

    private void clickElement(WebElement element) throws Exception {
        UtilsMethods.exceptionIfNullWebElement(element);
        if (!element.isEnabled()) {
            // throw new TimeoutException();
        }
        waitForAction.until(ExpectedConditions.visibilityOf(element).andThen(e -> {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
            return waitForAction.until(ExpectedConditions.elementToBeClickable(element));
        }));
        try {
            element.click();
        } catch (ElementClickInterceptedException e) {
            JavascriptExecutor jse = (JavascriptExecutor) driver;
            jse.executeScript("arguments[0].click()", element);
        }
    }

    private synchronized void onHoldForSeconds(BlockLoopInstructionDTO instruction) throws Exception {
        if (instruction != null) {
            Integer instructionSeconds = instruction.getOnHoldSeconds();
            if (instructionSeconds != null && instructionSeconds > 0) {
                wait(fromSecondsToMilliseconds(TimeUnit.SECONDS, instructionSeconds));
            } else {
                String stopSeconds =
                        ABRPropertyManager.getInstance().getProperty(ABRPropertyEnum.DEFAULT_INSTRUCTION_STOP_SECONDS);
                wait(fromSecondsToMilliseconds(TimeUnit.SECONDS, Integer.parseInt(stopSeconds)));
            }
        } else {
            wait(400);
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
