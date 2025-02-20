package com.allinweb.ch.facade;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class IframeInputLocator {

    private WebDriver driver;
    private Map<WebElement, List<WebElement>> iframeElementsMap;

    // Static final variable to hold the singleton instance
    protected static final SingletonSupplier<IframeInputLocator> instance = () -> new IframeInputLocator();

    private IframeInputLocator() {
        // Initialize if necessary
    }

    public void initializeIframeInputLocator(Map<WebElement, List<WebElement>> iframeElementsMap, WebDriver drive) {
        this.driver = drive;
        this.iframeElementsMap = iframeElementsMap;
    }

    // Public method to access the singleton instance
    public static IframeInputLocator getInstance() {
        return instance.get();
    }

    public IframeInputLocator(WebDriver driver) {
        this.driver = driver;
    }

    // Method to find an input element inside a specific iframe using the iframeElementsMap
    public WebElement findInputInsideIframe(By criteria) {
        // Loop through each iframe and its corresponding elements
        for (Map.Entry<WebElement, List<WebElement>> entry : iframeElementsMap.entrySet()) {
            WebElement iframe = entry.getKey();
            List<WebElement> elementsInsideIframe = entry.getValue();

            // Switch to the iframe
            driver.switchTo().frame(iframe);

            // Iterate over elements inside the iframe
            for (WebElement element : elementsInsideIframe) {
                try {
                    // Print the XPath of each element
                    //                    String elementXPath = getElementXPath(element, driver);
                    //                    System.out.println("Element XPath: " + elementXPath);

                    // Ensure the element is an input field
                    if (element.getTagName().equalsIgnoreCase("input")
                            || element.getTagName().equalsIgnoreCase("textarea")) {

                        // Send keys to the input element
                        String inputText = "aaaaaa";
                        element.clear(); // Clear any existing value
                        element.sendKeys(inputText);

                        // Retrieve the value back
                        String retrievedValue = element.getAttribute("value");

                        // Validate if the input was correctly received
                        if (inputText.equals(retrievedValue)) {
                            System.out.println("SUCCESS: Sent '" + inputText + "' and received '" + retrievedValue
                                    + "' in IFrame.");
                        } else {
                            System.out.println(
                                    "ERROR: Sent '" + inputText + "' but received '" + retrievedValue + "' in IFrame.");
                        }

                        // Return the first successfully interacted input element
                        //                        driver.switchTo().defaultContent();

                    }
                } catch (Exception e) {
                    System.out.println("Element interaction failed in IFrame. Error: " + e.getMessage());
                }
            }

            // Switch back to the main page before checking the next iframe
            driver.switchTo().defaultContent();
        }

        // If no matching input is found in any iframe, return null
        return null;
    }

    // Helper method to extract XPath of a WebElement
    public String getElementXPath(WebElement element, WebDriver driver) {
        return (String) ((JavascriptExecutor) driver)
                .executeScript(
                        "function getElementXPath(element) {" + "    var paths = [];"
                                + "    for (; element && element.nodeType == 1; element = element.parentNode) {"
                                + "        var index = 0;"
                                + "        for (var sibling = element.previousSibling; sibling; sibling = sibling.previousSibling) {"
                                + "            if (sibling.nodeType == 1 && sibling.tagName == element.tagName) {"
                                + "                index++;"
                                + "            }"
                                + "        }"
                                + "        var tagName = element.tagName.toLowerCase();"
                                + "        var pathIndex = (index ? '[' + (index+1) + ']' : '');"
                                + "        paths.unshift(tagName + pathIndex);"
                                + "    }"
                                + "    return '/' + paths.join('/');"
                                + "}"
                                + "return getElementXPath(arguments[0]);",
                        element);
    }

    // Helper method to extract XPath of a WebElement
    public String getElementXPathIFrame(WebElement element, WebDriver driver) {
        // Make sure we're in the correct frame before executing the script
        return (String) ((JavascriptExecutor) driver)
                .executeScript(
                        "function getElementXPath(element) {" + "    var paths = [];"
                                + "    for (; element && element.nodeType == 1; element = element.parentNode) {"
                                + "        var index = 0;"
                                + "        for (var sibling = element.previousSibling; sibling; sibling = sibling.previousSibling) {"
                                + "            if (sibling.nodeType == 1 && sibling.tagName == element.tagName) {"
                                + "                index++;"
                                + "            }"
                                + "        }"
                                + "        var tagName = element.tagName.toLowerCase();"
                                + "        var pathIndex = (index ? '[' + (index+1) + ']' : '');"
                                + "        paths.unshift(tagName + pathIndex);"
                                + "    }"
                                + "    return '/' + paths.join('/');"
                                + "}"
                                + "return getElementXPath(arguments[0]);",
                        element);
    }

    // Helper method to extract XPath of a WebElement, including tagName for iframe
    public String getElementTagName(WebElement element, WebDriver driver) {
        // Make sure we're in the correct frame before executing the script
        return (String) ((JavascriptExecutor) driver)
                .executeScript(
                        "function getElementXPath(element) {" + "    var paths = [];"
                                + "    while (element && element.nodeType == 1) {"
                                + "        var index = 0;"
                                + "        // Loop through previous siblings of the current element"
                                + "        for (var sibling = element.previousSibling; sibling; sibling = sibling.previousSibling) {"
                                + "            if (sibling.nodeType == 1 && sibling.tagName == element.tagName) {"
                                + "                index++;"
                                + "            }"
                                + "        }"
                                + "        var tagName = element.tagName.toLowerCase();"
                                + "        var pathIndex = (index ? '[' + (index + 1) + ']' : '');"
                                + "        // Add the element's tag and index to the path"
                                + "        paths.unshift(tagName + pathIndex);"
                                + "        // If element is an iframe, include its tagName too"
                                + "        if (tagName === 'iframe') {"
                                + "            paths.unshift('iframe');"
                                + "        }"
                                + "        // Traverse up the DOM tree"
                                + "        element = element.parentNode;"
                                + "    }"
                                + "    return '/' + paths.join('/');"
                                + "}"
                                + "return getElementXPath(arguments[0]);",
                        element);
    }

    // Helper method to extract XPath of a WebElement, including nested elements
    public String getElementChildXPathIFrame(WebElement element, WebDriver driver) {
        // Make sure we're in the correct frame before executing the script
        return (String) ((JavascriptExecutor) driver)
                .executeScript(
                        "function getElementXPath(element) {" + "    var paths = [];"
                                + "    while (element && element.nodeType == 1) {"
                                + "        var index = 0;"
                                + "        // Loop through previous siblings of the current element"
                                + "        for (var sibling = element.previousSibling; sibling; sibling = sibling.previousSibling) {"
                                + "            if (sibling.nodeType == 1 && sibling.tagName == element.tagName) {"
                                + "                index++;"
                                + "            }"
                                + "        }"
                                + "        var tagName = element.tagName.toLowerCase();"
                                + "        var pathIndex = (index ? '[' + (index + 1) + ']' : '');"
                                + "        // Add the element's tag and index to the path"
                                + "        paths.unshift(tagName + pathIndex);"
                                + "        // Traverse into child nodes if the element is not root"
                                + "        element = element.parentNode;"
                                + "    }"
                                + "    return '/' + paths.join('/');"
                                + "}"
                                + "return getElementXPath(arguments[0]);",
                        element);
    }

    public List<String> getIframeElementsInfo(WebElement iFrameElement, WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Wait for iframe to be available
        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(iFrameElement));

        String script = "var doc = document;" + "var elements = doc.querySelectorAll('*');"
                + "return Array.from(elements).map(el => {"
                + "   var path = '';"
                + "   while (el && el.nodeType === 1) {"
                + "       var index = Array.from(el.parentNode.children).indexOf(el) + 1;"
                + "       path = '/' + el.tagName.toLowerCase() + '[' + index + ']' + path;"
                + "       el = el.parentNode;"
                + "   }"
                + "   return path;"
                + "});";

        JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;
        List<String> allXPathInsideIframe = new ArrayList<>();

        // Execute JavaScript inside the iframe
        List<String> xpaths = (List<String>) jsExecutor.executeScript(script);

        allXPathInsideIframe.addAll(xpaths);

        // Switch back to the main content
        driver.switchTo().defaultContent();

        return allXPathInsideIframe;
    }

    // Helper method to extract XPath of a WebElement
    public String getElementXPathAll(WebElement element, WebDriver driver) {
        return (String) ((JavascriptExecutor) driver)
                .executeScript(
                        "function getElementXPath(element) {" + "    var paths = [];"
                                + "    for (; element && element.nodeType == 1; element = element.parentNode) {"
                                + "        var index = 0;"
                                + "        for (var sibling = element.previousSibling; sibling; sibling = sibling.previousSibling) {"
                                + "            if (sibling.nodeType == 1 && sibling.tagName == element.tagName) {"
                                + "                index++;"
                                + "            }"
                                + "        }"
                                + "        var tagName = element.tagName.toLowerCase();"
                                + "        var pathIndex = (index ? '[' + (index+1) + ']' : '');"
                                + "        paths.unshift(tagName + pathIndex);"
                                + "    }"
                                + "    return '/' + paths.join('/');"
                                + "}"
                                + "return getElementXPath(arguments[0]);",
                        element);
    }

    public List<String> listAllXPaths(WebDriver driver) {
        // Get all elements on the page (excluding iframes)
        List<WebElement> allElements = driver.findElements(By.xpath("//*")); // Get all elements

        List<String> allXPaths = new ArrayList<>();

        // Iterate through all elements and print their XPath
        for (WebElement element : allElements) {
            String elementXPath = getElementXPathAll(element, driver);
            System.out.println("Element XPath: " + elementXPath);
            allXPaths.add(elementXPath);
        }

        return allXPaths;
    }
}
