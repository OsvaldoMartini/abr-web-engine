package com.allinweb.ch.facade;

import com.allinweb.ch.util.ErrorMessage;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

/**
 * PerformActions.
 *
 * @author Osvaldo Martini
 * @version 1.0
 */
public class PerformCloneLoad {
    protected static volatile PerformCloneLoad instance;

    // Private constructor to prevent instantiation
    private PerformCloneLoad() {
        // Initialize if necessary
    }

    public static PerformCloneLoad getInstance() {
        if (instance == null) {
            synchronized (PerformCloneLoad.class) {
                if (instance == null) {
                    instance = new PerformCloneLoad();
                }
            }
        }
        return instance;
    }

    private static JavascriptExecutor jsExecutor;

    public ErrorMessage dynamicPickOneCloneElementsDTO(
            WebDriver driver,
            boolean searchHiddenFields,
            int port,
            String sessionId,
            String destination,
            String operationId,
            int homeBankingId,
            String currentUrl) {
        try {
            jsExecutor = (JavascriptExecutor) driver;
            jsExecutor.executeScript(
                    jsHoverPickInject,
                    searchHiddenFields,
                    port,
                    sessionId,
                    destination,
                    operationId,
                    homeBankingId,
                    currentUrl,
                    currentUrl);
            return null;
        } catch (Exception error) {
            return new ErrorMessage("Error running Scanner", "Dynamic Load ElementsDTO error", error.getMessage());
        }
    }

    private String jsHoverPickInject =
            """
// HOVER PICK IN USE (SENDER: scannerTool) -> scannerGrid
(function (
  hiddenFields,
  socketPort,
  sessionId,
  destination,
  operationId,
  homeBankingId,
  targetOriginURL,
  trustedOriginURL
) {
  let pingIntervalId = null;
  let attempts = 0;
  let maxAttempts = 100;
  var wSocket = null;
  let alreadySent = false;
  let previousXPath = null;
  // Temporary storage for original styles
  const originalStyles = new Map();
  const hoveredXPathMap = new Set(); // Changed from Map to Set
  let previousHighlightedElement = null;
  var coordinatesElement = document.createElement("div");
  coordinatesElement.id = "coordinates";
  coordinatesElement.style.position = "fixed"; // Fixed so it stays above all elements
  coordinatesElement.style.padding = "10px";
  coordinatesElement.style.backgroundColor = "rgba(0, 0, 0, 0.5)";
  coordinatesElement.style.color = "white";
  coordinatesElement.style.borderRadius = "5px";
  coordinatesElement.style.fontSize = "14px";
  coordinatesElement.style.zIndex = Number.MAX_SAFE_INTEGER; // Set zIndex to the maximum allowed value
  coordinatesElement.style.cursor = "pointer"; // Set cursor to hand (pointer)
  coordinatesElement.textContent = "X: 0nbsp;&nbsp;&nbsp;&nbsp;Y: 0";

  // Append the coordinates div to the body
  document.body.appendChild(coordinatesElement);

  window.elementInfoMap = new Map();
  window.allElementInfo = [];
  window.destination = destination;
  window.operationId = operationId;
  window.homeBankingId = homeBankingId;
  window.sessionId = `${sessionId}`; //-${homeBankingId}`;

  // Track the last hovered element to remove the border from it
  let lastHoveredElement = null;

  function connectWebSocket() {
    if (attempts >= maxAttempts) {
      //console.error("Reached maximum reconnection attempts. Stopping.");
      return;
    }

    try {
      //console.log(`Attempt ${attempts + 1} to connect to WebSocket...`);
      wSocket = new WebSocket(
        `ws://localhost:${socketPort}/websocket?sessionId=${window.sessionId}`
      );

      wSocket.onopen = () => {
        //console.log(`WebSocket connected for session: ${window.sessionId}`);
        attempts = 0; // Reset attempts on successful connection

        try {
          const subscriptionMessage = {
            type: "echo",
            sessionId: window.sessionId,
            operationId: "test echo",
            body: "subscribe",
          };
          // Convert the JSON message to a buffer
          const base64Message = btoa(
            unescape(encodeURIComponent(JSON.stringify(subscriptionMessage)))
          );
          // Convert the buffer to a Base64 string
          wSocket.send(base64Message);
          // wSocket.send(JSON.stringify(message));
          //console.log("Sent SEARCH_TOOL:", subscriptionMessage);
          //console.log("Sent ENCODED Length:", base64Message.length);
          //console.log("Sent ENCODED:", base64Message);
        } catch (sendError) {
          //console.error("Failed to send subscription message:", sendError);
        }

        // Call startCollectingElements AFTER WebSocket is open
        sendingData();
        startPing();
      };

      wSocket.onmessage = (event) => {
        let receivedMessage = event.data;

        if (receivedMessage.endsWith("\\u0000")) {
          receivedMessage = receivedMessage.slice(0, -1);
        }

        if (receivedMessage) {
          try {
            const parsedMessage = JSON.parse(receivedMessage);
            //console.log("WebSocket message received:", parsedMessage);

            const bodyData =
              typeof parsedMessage.body === "string"
                ? JSON.parse(parsedMessage.body)
                : parsedMessage.body;

            if (window.sessionId === bodyData.sessionId) {
              if (bodyData.operationId === "highlight") {
                const detailsData = Array.isArray(bodyData.details)
                  ? bodyData.details
                  : [];

                //console.log("detailsData", detailsData[0]);

                var hoveredElement = findElementByXPath(detailsData[0].xPath);

                if (!hoveredElement) {
                  hoveredElement = document.querySelector(
                    detailsData[0].cssSelector
                  );
                }

                if (!hoveredElement) {
                  var hoveredElement = getElementByCoordinates(
                    detailsData[0].coordinates
                  );
                }

                if (hoveredElement) {
                  // console.log("hoveredElement", hoveredElement);

                  const currentXPath = detailsData[0].xPath;

                  // Restore style of previous element (if XPath is different)
                  if (previousXPath && previousXPath !== currentXPath) {
                    const originalOutline = originalStyles.get(previousXPath);
                    previousHighlightedElement.style.outline =
                      originalOutline || "";
                  }

                  // Save original style using XPath as key
                  if (!originalStyles.has(currentXPath)) {
                    originalStyles.set(
                      currentXPath,
                      hoveredElement.style.outline
                    );
                    hoveredXPathMap.add(currentXPath);
                  }

                  const originalOutline =
                    originalStyles.get(currentXPath) || "";

                  // Check if original style already had red
                  if (originalOutline.includes("#2323FF")) {
                    hoveredElement.style.outline = "3px solid #FF3131";
                  } else if (originalOutline.includes("#FF3131")) {
                    hoveredElement.style.outline = "3px solid #2323FF";
                  } else {
                    hoveredElement.style.outline = "3px solid #FF3131";
                  }

                  previousHighlightedElement = hoveredElement;
                  previousXPath = currentXPath;
                } else {
                  restoreOriginalStyles();
                }
              }

              if (
                parsedMessage.body.includes("cannot be processed") ||
                (parsedMessage.footer &&
                  parsedMessage.footer.includes("cannot be processed"))
              ) {
                //Handle cannot be processed
              }
            }
          } catch (parseError) {
            console.log("Non-JSON message received:", receivedMessage);
          }
        }
      };

      wSocket.onerror = (error) => {
        //console.error("WebSocket error:", error);
      };

      wSocket.onclose = () => {
        //console.log("WebSocket connection closed");

        if (attempts < maxAttempts) {
          attempts++;
          //console.log(`Reconnecting attempt ${attempts}...`);
          if (!alreadySent) {
            connectWebSocket(); // Retry connection
          }
        } else {
          //console.log(`${maxAttempts} Attempts to Reconnect with the WebSocket.`);
        }
      };
    } catch (initError) {
      //console.error("Failed to initialize WebSocket:", initError);
    }
  }

  // Optionally, expose a cleanup function
  window.cleanupWebSocket = () => {
    try {
      //console.log("Cleaning up WebSocket...");
      if (wSocket && wSocket.readyState === WebSocket.OPEN) {
        wSocket.close();
      }
      if (pingIntervalId) {
        clearInterval(pingIntervalId);
        pingIntervalId = null;
      }
    } catch (cleanupError) {
      //console.error("Error during WebSocket cleanup:", cleanupError);
    }
  };

  function sendingData() {
    window.allElementInfo = [];

    limitMapCharacters(window.elementInfoMap);

    findMatLabel(window.allElementInfo);

    changeDivToLabelWithSomeText(window.allElementInfo);

    console.log("All element info stored in Map:", window.allElementInfo);

    if (wSocket && wSocket.readyState) {
      //console.log("WebSocket readyState:", wSocket.readyState);
    }

    if (wSocket && wSocket.readyState === WebSocket.OPEN) {
      if (window.allElementInfo.length > 0) {
        const message = {
          type: "SEARCH_TOOL",
          sessionId: window.destination,
          operationId: window.operationId,
          homeBankingId: window.homeBankingId,
          details: window.allElementInfo, // Send allElementInfo
        };

        // Convert the JSON message to a buffer
        const base64Message = btoa(
          unescape(encodeURIComponent(JSON.stringify(message)))
        );
        // Convert the buffer to a Base64 string
        wSocket.send(base64Message);
        // wSocket.send(JSON.stringify(message));
        //console.log("Sent SEARCH_TOOL:", message);
        //console.log("Sent ENCODED Length:", base64Message.length);
        //console.log("Sent ENCODED:", base64Message);
        window.elementInfoMap.clear();
      }
    } else {
      console.log("WebSocket is not open. Cannot send message.");
    }
  }

  function startPing() {
    // Send a ping every 30 seconds (adjust if needed)
    pingIntervalId = setInterval(() => {
      if (wSocket && wSocket.readyState === WebSocket.OPEN) {
        const pingMessage = {
          type: "ping-hover",
          sessionId: window.sessionId,
          timestamp: new Date().toISOString(),
        };

        try {
          const encodedPing = btoa(
            unescape(encodeURIComponent(JSON.stringify(pingMessage)))
          );
          wSocket.send(encodedPing);
          //console.log("Ping sent:", pingMessage);
        } catch (pingError) {
          //console.error("Ping error:", pingError);
        }
      }
    }, 15000); // 15 seconds
  }

  function findElementByXPath(xpath) {
    try {
      const result = document.evaluate(
        xpath,
        document,
        null,
        XPathResult.FIRST_ORDERED_NODE_TYPE,
        null
      );
      return result.singleNodeValue;
    } catch (error) {
      console.error("Error finding element by XPath:", error);
      return null; // Return null in case of error
    }
  }

  function getElementByCoordinates(coordString) {
    const [xStr, yStr] = coordString.split(",");
    const x = parseFloat(xStr.trim());
    const y = parseFloat(yStr.trim());

    if (isNaN(x) || isNaN(y)) {
      //console.error("Invalid coordinates:", coordString);
      return null;
    }

    const element = document.elementFromPoint(x, y);
    //console.log("Element found at", x, y, "=>", element);
    return element;
  }

  const getElementIdentity = function getElementIdentity(element) {
    if (!hiddenFields) {
      if (
        (element.offsetWidth === 0 ||
          element.offsetHeight === 0 ||
          window.getComputedStyle(element).visibility === "hidden") &&
        !(
          element.tagName.toLowerCase() === "input" &&
          element.type.toLowerCase() === "hidden"
        )
      ) {
        return null; // Ignore all hidden elements except <input type="hidden">
      }
    }

    const attributeData = Array.from(element.attributes).map((attr) => ({
      name: attr.name,
      value: attr.value,
    }));
    const attribId = element.id || "";
    const attribName = element.name || "";
    const coordinates = `${element
      .getBoundingClientRect()
      .left.toFixed(2)},${element.getBoundingClientRect().top.toFixed(2)}`;

    let tagName = element.tagName.toLowerCase();

    const someText = getVisibleText(tagName, attributeData, element);

    const xPath = getMartiniXPath(element);

    const tagNameTemp = identifyElementTypeFromXPath(tagName, xPath, someText);
    if (tagNameTemp !== tagName) {
      tagName = tagNameTemp;
    }

    return {
      xPath,
      tagName,
      attributeData,
      customXPath: "",
      attribId,
      attribName,
      coordinates,
      someText,
    };
  };

  // Function to check if an element is hidden (using computed styles and attributes)
  const isHidden = (el) => {
    const style = window.getComputedStyle(el);
    return (
      style.display === "none" ||
      style.visibility === "hidden" ||
      el.hasAttribute("aria-hidden")
    );
  };

  function getVisibleText(tagName, attributeData, element) {
    let textResult = "";

    if (element && !isHidden(element)) {
      const extractedText = extractVisibleTextFromHTML(element);
      textResult = [
        ...extractedText.titles,
        ...extractedText.text,
        ...extractedText.labels,
      ]
        .map((text) => text.trim())
        .filter(Boolean)
        .join("; ");
    }

    // Define priority order for attributes
    const attributePriority = [
      "aria-label",
      "aria-labelledby",
      "aria-describedby",
      "placeholder",
      "label",
      "name",
      "title",
      "alt",
      "for",
      "data-label",
      "data-name",
      "data-title",
      "id",
      "data-testid",
    ];

    let firstMeaningfulText = "";

    // Function to get attribute text with priority
    const getAttributeText = (name, value) => {
      if (name === "aria-labelledby" || name === "aria-describedby") {
        const referencedElement = document.getElementById(value);
        if (referencedElement && !isHidden(referencedElement)) {
          return referencedElement.textContent.trim();
        }
      }
      return value.trim();
    };

    // Check element's text first
    if (textResult && !/^\\..*\\{.*\\}$/.test(textResult)) {
      firstMeaningfulText = textResult;
    } else {
      // Directly prioritize title before checking others
      const titleAttr = attributeData.find(({ name }) => name === "title");
      if (titleAttr) {
        firstMeaningfulText = getAttributeText(titleAttr.name, titleAttr.value);
      }

      if (!firstMeaningfulText) {
        for (const attr of attributePriority) {
          const foundAttr = attributeData.find(({ name }) => name === attr);
          if (foundAttr) {
            firstMeaningfulText = getAttributeText(
              foundAttr.name,
              foundAttr.value
            );
            if (firstMeaningfulText) break; // Stop at first meaningful attribute
          }
        }
      }
    }

    return firstMeaningfulText; // Return the most meaningful text
  }

  function extractVisibleTextFromHTML(element) {
    if (!element) {
      return { text: [], labels: [], titles: [] };
    }

    const result = {
      text: new Set(),
      labels: new Set(),
      titles: new Set(),
    };

    // Utility function to check if an element is visible
    const isVisible = (el) => {
      const style = window.getComputedStyle(el);
      return !(
        style.display === "none" ||
        style.visibility === "hidden" ||
        el.hasAttribute("aria-hidden")
      );
    };

    // Function to filter out technical patterns
    const isTechnicalPattern = (word) => {
      return word.includes("_") || word.includes("--") || word.includes("-");
    };

    // Extract visible text content from an element
    if (element.textContent?.trim() && isVisible(element)) {
      // Ignore text content that looks like CSS rules and words with technical patterns
      const textContent = element.textContent.trim();
      const words = textContent.split(/\\s+/);
      const filteredWords = words.filter((word) => !isTechnicalPattern(word));
      const filteredText = filteredWords.join(" ").trim();
      if (filteredText) {
        result.text.add(filteredText);
      }
    }

    // Extract text from labels (including associated input fields)
    element.querySelectorAll("label").forEach((label) => {
      if (isVisible(label) && label.textContent?.trim()) {
        result.labels.add(label.textContent.trim());
      }

      // Handle labels associated with form elements
      const forAttr = label.getAttribute("for");
      if (forAttr) {
        const inputElement = document.getElementById(forAttr);
        if (inputElement && isVisible(inputElement)) {
          const value = inputElement.value?.trim();
          const placeholder = inputElement.placeholder?.trim();
          if (value) {
            const words = value.split(/\\s+/);
            const filteredWords = words.filter(
              (word) => !isTechnicalPattern(word)
            );
            const filteredText = filteredWords.join(" ").trim();
            if (filteredText) result.text.add(filteredText);
          } else if (placeholder) {
            const words = placeholder.split(/\\s+/);
            const filteredWords = words.filter(
              (word) => !isTechnicalPattern(word)
            );
            const filteredText = filteredWords.join(" ").trim();
            if (filteredText) result.text.add(filteredText);
          }
        }
      }
    });

    // Extract text from common inline and block elements
    const visibleTextElements = [
      "p",
      "h1",
      "h2",
      "h3",
      "h4",
      "h5",
      "h6",
      "li",
      "span",
      "div",
      "strong",
      "em",
      "b",
      "i",
      "blockquote",
    ];
    visibleTextElements.forEach((tag) => {
      element.querySelectorAll(tag).forEach((child) => {
        if (isVisible(child) && child.textContent?.trim()) {
          const textContent = child.textContent.trim();
          const words = textContent.split(/\\s+/);
          const filteredWords = words.filter(
            (word) => !isTechnicalPattern(word)
          );
          const filteredText = filteredWords.join(" ").trim();
          if (filteredText) {
            result.text.add(filteredText);
          }
        }
      });
    });

    // Extract visible link text
    element.querySelectorAll("a").forEach((link) => {
      if (isVisible(link) && link.textContent?.trim()) {
        const textContent = link.textContent.trim();
        const words = textContent.split(/\\s+/);
        const filteredWords = words.filter((word) => !isTechnicalPattern(word));
        const filteredText = filteredWords.join(" ").trim();
        if (filteredText) {
          result.text.add(filteredText);
        }
      }
    });

    // Extract titles from iframes if accessible
    element.querySelectorAll("iframe").forEach((iframe) => {
      if (iframe.hasAttribute("title")) {
        const title = iframe.getAttribute("title")?.trim();
        if (title) result.titles.add(title);
      }

      try {
        const iframeDoc =
          iframe.contentDocument ||
          new DOMParser().parseFromString(iframe.srcdoc || "", "text/html");
        if (iframeDoc.body) {
          const extractedText = extractVisibleTextFromHTML(iframeDoc.body);
          extractedText.titles.forEach((title) => result.titles.add(title));
          extractedText.text.forEach((text) => result.text.add(text));
          extractedText.labels.forEach((label) => result.labels.add(label));
        }
      } catch (e) {
        console.log("Could not access iframe content", e);
      }
    });

    // Return arrays instead of Sets
    return {
      text: Array.from(result.text),
      labels: Array.from(result.labels),
      titles: Array.from(result.titles),
    };
  }
  const getMartiniXPath = function getMartiniXPath(element) {
    if (element === document.body) return "/html/body";
    let ix = 0;
    const siblings = element.parentNode ? element.parentNode.childNodes : [];
    for (let i = 0; i < siblings.length; i++) {
      let sibling = siblings[i];
      if (sibling.nodeType === 1 && sibling.tagName === element.tagName) {
        if (sibling === element) {
          return (
            getMartiniXPath(element.parentNode) +
            "/" +
            element.tagName.toLowerCase() +
            "[" +
            (ix + 1) +
            "]"
          );
        }
        ix++;
      }
    }
    return "";
  };

  function identifyElementTypeFromXPath(tagName, xpath) {
    if (typeof xpath !== "string" || xpath.trim() === "") {
      return "unknown";
    }

    const parts = xpath.split("/").filter((part) => part.trim() !== "");

    for (let i = parts.length - 1; i >= 0; i--) {
      const part = parts[i];

      const tagMatch = part.match(/^([a-zA-Z-]+)(?:\\[\\d+\\])?/);
      if (!tagMatch) continue;

      const tag = tagMatch[1].toLowerCase();

      if (tag === "a") {
        return "a"; // Link
      }

      if (tag === "input") {
        const typeMatch = part.match(/@type=["']?([^"'\\]]+)["']?/);
        const type = typeMatch ? typeMatch[1].toLowerCase() : "";

        if (["button", "submit", "reset"].includes(type)) {
          return "button";
        }
        return "input";
      }

      if (tag === "button") {
        return "button";
      }

      // Detect if it's an Angular Material expansion panel (likely a button)
      if (
        tag.includes("expansion-panel-header") ||
        tag.includes("sidenav") ||
        tag.includes("nav")
      ) {
        return "button";
      }

      if (tag === "select" || tag === "option") {
        return "select"; // or option
      }

      if (tag === "textarea") {
        return "input";
      }

      // Framework specific detection from isInteractiveElement function.
      if (
        tag.includes("mat-button") ||
        tag.includes("mat-raised-button") ||
        tag.includes("mat-icon-button") ||
        tag.includes("mat-menu-item") ||
        tag.includes("mat-select") ||
        tag.includes("mat-option") ||
        tag.includes("matinput")
      ) {
        return "button"; // or select, input, option.
      }

      if (
        tag.includes("data-testid") ||
        tag.includes("aria-label") ||
        part.includes("@role='button'") ||
        part.includes("@role='textbox'") ||
        part.includes("react-button") ||
        part.includes("react-link") ||
        part.includes("react-input")
      ) {
        if (part.includes("react-input")) {
          return "input";
        } else if (part.includes("react-link")) {
          return "a";
        } else {
          return "button";
        }
      }

      if (
        part.includes("mdc-button") ||
        part.includes("mdc-text-field") ||
        part.includes("mdc-list-item")
      ) {
        if (part.includes("mdc-text-field")) {
          return "input";
        } else {
          return "button";
        }
      }

      if (
        part.includes("el-button") ||
        part.includes("el-input__inner") ||
        part.includes("el-select-dropdown__item")
      ) {
        if (part.includes("el-input__inner")) {
          return "input";
        } else if (part.includes("el-select-dropdown__item")) {
          return "select";
        } else {
          return "button";
        }
      }
    }

    return tagName; // Default to the given tagName if no match
  }

  const elementDTO = function elementDTO(typeElement, identity) {
    return {
      typeElement: typeElement,
      tagName: identity.tagName ?? "No Tag Name Detected",
      xPath: identity.xPath ?? "",
      someText: identity.someText ?? "",
      attribId: identity.attribId ?? "",
      attribName: identity.attribName ?? "",
      coordinates: identity.coordinates ?? "",
      attributeData: identity.attributeData ?? "",
      customXPath: identity.customXPath ?? "",
      iFrameXPath: identity.iFrameXPath ?? "",
      shadowHost: identity.shadowHost ?? "",
      shadowRoot: identity.shadowRoot ?? "",
      nestedShadow: identity.nestedShadow ?? "",
      cssSelector: identity.cssSelector ?? "",
      attributeValue: identity.attributeValue ?? "",
      attributeType: identity.attributeType ?? "",
      searchAttributeValue: identity.searchAttributeValue ?? "",
    };
  };

  function limitMapCharacters(elementInfoMap) {
    elementInfoMap.forEach((value, key) => {
      let modifiedValue = value;
      window.allElementInfo.push({ ...value, id: 1 }); // given '1' for  id
    });
  }

  // Add event listener for mouse movement to update coordinates
  function showMartiniTooltip(event) {
    const x = event.clientX; // X position
    const y = event.clientY; // Y position

    // Get the dimensions of the coordinatesElement
    const elementWidth = coordinatesElement.offsetWidth;
    const elementHeight = coordinatesElement.offsetHeight;

    // Update the coordinates display
    coordinatesElement.innerHTML = `X: ${x}&nbsp;&nbsp;&nbsp;&nbsp;Y: ${y}`;

    // Update the position of coordinatesElement to follow the cursor
    // Position the element such that the cursor is at the center of the element
    coordinatesElement.style.left = `${x - elementWidth / 2}px`; // Center the element on the X axis
    coordinatesElement.style.top = `${y - elementHeight / 2}px`; // Center the element on the Y axis

    // Find the element directly under the mouse cursor
    const elementBelowTooltip = document.elementFromPoint(x, y);

    // Highlight the hovered element
    if (lastHoveredElement !== elementBelowTooltip) {
      // Remove highlight from the previous element if any
      if (lastHoveredElement) {
        lastHoveredElement.style.outline = ""; // Remove the previous highlight
      }

      // Add a border to highlight the current element
      if (elementBelowTooltip && elementBelowTooltip !== coordinatesElement) {
        elementBelowTooltip.style.outline = "3px solid red"; // Highlight the element
      }

      lastHoveredElement = elementBelowTooltip; // Update the last hovered element
    }
  }

  function handleMartiniClick(event) {
    event.preventDefault(); // Prevent the default click action
    event.stopPropagation(); // Prevent the event from propagating upwards
    coordinatesElement.style.display = "none";

    // Get the coordinates of the click
    const clickX = event.clientX;
    const clickY = event.clientY;

    // Get the element at the clicked position
    const clickedElement = document.elementFromPoint(clickX, clickY);

    coordinatesElement.style.display = "block";

    // Highlight the clicked element (optional)
    if (clickedElement) {
      if (!originalStyles.has(clickedElement)) {
        // Store the original outline before changing it
        originalStyles.set(clickedElement, clickedElement.style.outline);
      }
      clickedElement.style.outline = "3px solid blue"; // Optionally highlight the element with a blue border
    }

    // If the element below the tooltip is an iframe
    if (clickedElement && clickedElement.tagName.toLowerCase() === "iframe") {
      // Get the document inside the iframe
      var iframeDocument =
        clickedElement.contentDocument || clickedElement.contentWindow.document;

      // If the iframe document is valid
      if (iframeDocument) {
        // Initialize an array to store the iframe element information
        window.allElementInfo = [];

        const elementIdentity = getElementIdentity(clickedElement);

        xPathIFrame = elementIdentity.xPath;

        if (elementIdentity) {
          window.elementInfoMap.set(
            elementIdentity.xPath,
            elementDTO("clicked-iFrame", elementIdentity)
          );
        }

        // Get all elements inside the iframe and log their details
        var iframeElements = iframeDocument.querySelectorAll("*");
        iframeElements.forEach(function (elementInsideIframe) {
          const elementIdentity = getElementIdentity(elementInsideIframe);
          // console.log(
          //   "elementIdentity.xPath",
          //   `${xPathIFrame}${elementIdentity?.xPath}`
          // );
          if (elementIdentity) {
            elementIdentity.iFrameXPath = xPathIFrame;
            window.elementInfoMap.set(
              elementIdentity.xPath,
              elementDTO("iFrame-Child", elementIdentity)
            );
          }
        });
      }
    } else {
      var tagName = clickedElement.tagName.toLowerCase();

      if (["html", "body", "main"].includes(tagName)) {
        return; // Don't proceed if it's one of these elements
      }

      window.elementInfoMap.clear();

      console.log("Clicked element:", clickedElement);

      // Check if the clicked element has a shadow root
      let shadowHost = clickedElement;

      // Locate the shadow host element if it has a shadow root
      while (shadowHost && !shadowHost.shadowRoot) {
        shadowHost = shadowHost.parentElement; // Traverse upwards in the DOM
      }

      if (shadowHost && shadowHost.shadowRoot) {
        // Access the Shadow DOM
        const shadowRoot = shadowHost.shadowRoot;

        // Find all clickable elements inside the Shadow DOM
        let clickableElements = findClickableElements(shadowRoot);

        // If clickable elements are found, perform your action (e.g., highlight them)
        clickableElements.forEach((element) => {
          pushElement(element, shadowHost, shadowRoot);
        });
      } else {
        // Commom Elementes
        pushElement(clickedElement, null, null);
      }
    }

    sendingData();

    // window.revertCloneInjections();

    // Remove the tooltip from the page and delete the reference after 5 seconds
    setTimeout(() => {
      window.allElementInfo = [];
      window.elementInfoMap.clear();
    }, 1000);
  }

  function pushElement(element, shadowHost, shadowRoot) {
    const elementIdentityTemp = getElementIdentity(element);

    let shadowHostSelector = "";
    let elementCssSelector = "";
    let shadowPath = [];

    function buildCssSelector(el) {
      if (!el) return "";
      let selector = el.tagName.toLowerCase();
      if (el.id) selector += `#${el.id}`;
      if (el.className) selector += `.${el.className.replace(/\\s+/g, ".")}`;
      return selector;
    }

    // Traverse shadow hosts if nested shadow DOM exists
    let currentHost = shadowHost;
    while (currentHost) {
      shadowPath.unshift(buildCssSelector(currentHost));
      currentHost =
        currentHost.parentNode instanceof ShadowRoot
          ? currentHost.parentNode.host
          : null;
    }

    if (shadowHost) {
      shadowHostSelector = buildCssSelector(shadowHost);
    }

    if (element) {
      elementCssSelector = buildCssSelector(element);
    }

    // Construct the natural CSS selector for nested Shadow DOM
    let cssSelector = elementCssSelector;

    // Build nested CSS selector, if shadowPath is not empty.
    if (shadowPath.length > 0) {
      cssSelector = shadowPath.reduceRight((acc, hostSelector) => {
        return `${hostSelector} ${acc}`;
      }, elementCssSelector);
    }

    const elementIdentity = {
      ...elementIdentityTemp,
      shadowHost: shadowHostSelector,
      shadowRoot: shadowRoot ? true : false,
      nestedShadow: shadowPath.length > 1, // Detects if multiple shadow roots are involved
      cssSelector: elementCssSelector, // cssSelector shadowRoot
    };

    // Store tagName and other details in the Map
    if (elementIdentity) {
      if (!originalStyles.has(element)) {
        // Store the original outline before changing it
        originalStyles.set(element, element.style.outline);
      }
      element.style.outline = "3px solid red";

      window.elementInfoMap.set(
        elementIdentity.xPath,
        elementDTO("clicked", elementIdentity)
      );
    }
  }

  function findMatLabel(sortedList) {
    sortedList.forEach((item) => {
      if (item.attribId || item.attribName) {
        let searchText = item.someText;
        let searchId = item.attribId;
        let searchName = item.attribName;
        let foundLabelText = null;

        const selectors = [];
        if (searchId) {
          selectors.push(`label[for="${searchId}"] mat-label`);
          selectors.push(`mat-label[for="${searchId}"]`);
          selectors.push(`mat-checkbox[test-id="${searchName}"] .mdc-label`); // Keep this in case 'test-id' is relevant
          selectors.push(`label[for="${searchId}"]`); // Direct label using 'for' attribute
        }
        if (searchName) {
          selectors.push(`label[for="${searchName}"] mat-label`);
          selectors.push(`mat-label[for="${searchName}"]`);
          selectors.push(`mat-checkbox[test-id="${searchName}"] .mdc-label`); // Keep this in case 'test-id' is relevant
          selectors.push(`label[for="${searchId}"]`); // Direct label using 'for' attribute
        }

        selectors.forEach((selector) => {
          const labelElement = document.querySelector(selector);
          if (labelElement && foundLabelText === null) {
            foundLabelText = labelElement.textContent.trim();
            console.log(
              `Found mat-label text for input with id/name '${
                searchId || searchName
              }':`,
              foundLabelText
            );

            // Add "someText" to attributeData
            item.attributeData.push({ name: "someText", value: searchText });

            // Replace the value of someText with the found label text
            item.someText = foundLabelText;
          }
        });

        if (foundLabelText === null) {
          // console.log(`No mat-label found for input with id/name '${searchId || searchName}'.`);
        }
      }
    });
  }

  function changeDivToLabelWithSomeText(sortedList) {
    sortedList.forEach((item) => {
      if (item.someText && item.tagName === "div") {
        item.tagName = "label";
      }
    });
  }

  // Function to find clickable elements (buttons, links, etc.)
  function findClickableElements(root) {
    const clickableSelectors = ["button", "a"]; // Add other clickable elements if needed
    const clickableElements = [];
    clickableSelectors.forEach((selector) => {
      clickableElements.push(...root.querySelectorAll(selector));
    });
    return clickableElements;
  }

  connectWebSocket();

  window.revertHoverPickInjections = function () {
    document.removeEventListener("mousemove", showMartiniTooltip);
    document.removeEventListener("click", handleMartiniClick);
    //console.log("revertHoverPickInjections");

    // Remove the tooltip from the page and delete the reference after 5 seconds
    setTimeout(() => {
      removeElements();
      restoreOriginalStyles();
      window.allElementInfo = [];
    }, 1000);
  };

  // Function to restore the original outline
  function restoreOriginalStyles() {
    // console.log("restoreOriginalStyles", originalStyles);
    if (originalStyles && originalStyles.size > 0) {
      originalStyles.forEach((originalStyle, element) => {
        if (element && element.style) {
          element.style.outline = originalStyle; // Restore original outline
        }
      });

      // Paranoic
      if (hoveredXPathMap && hoveredXPathMap.size > 0) {
        //console.log("hoveredXPathMap");
        hoveredXPathMap.forEach((xPath) => {
          const originalOutline = originalStyles.get(xPath);
          var element = findElementByXPath(xPath);
          if (element && element.style) {
            element.style.outline = originalOutline; // Restore original outline
          }
        });
      }

      // originalStyles.clear(); // Clear the stored styles
    }
  }

  // Set up the interval to call the function every 5 seconds (5000 milliseconds)
  setInterval(restoreOriginalStyles, 5000);

  function removeElements() {
    // Remove highlight from the previous element if any
    if (lastHoveredElement) {
      lastHoveredElement.style.outline = ""; // Remove the previous highlight
    }

    if (coordinatesElement) {
      coordinatesElement.remove(); // Completely remove the tooltip from the DOM
      coordinatesElement = null; // Clear the reference to free memory
      //console.log("coordinatesElement completely removed.");
    }
  }

  document.addEventListener("mousemove", showMartiniTooltip);
  document.addEventListener("click", handleMartiniClick);

  window.postMessage({ type: "myMessage", data: "some data" }, targetOriginURL);

  window.addEventListener("message", function (event) {
    if (event.origin !== trustedOriginURL) return; // check the origin
    //console.log(event.data);
  });


//  window.addEventListener("beforeunload", function (event) {
//    if (wSocket && wSocket.readyState === WebSocket.OPEN) {
//      const message = {
//        type: "CLOSE_BROWSER",
//        sessionId: `scannerReceiver`, //-${window.homeBankingId}`,
//        operationId: "closeBrowser",
//        homeBankingId: window.homeBankingId,
//        details: window.allElementInfo, // Send allElementInfo
//      };
//
//      // Convert the JSON message to a buffer
//      const base64Message = btoa(
//        unescape(encodeURIComponent(JSON.stringify(message)))
//      );
//      // Convert the buffer to a Base64 string
//      wSocket.send(base64Message);
//
//      alreadySent = true;
//      window.allElementInfo = [];
//      window.elementInfoMap.clear();
//      window.revertSearchInjections();
//    }
//  });

  // window.cloneTerms = null; // Invalidating the function
})(
  arguments[0],
  arguments[1],
  arguments[2],
  arguments[3],
  arguments[4],
  arguments[5],
  arguments[6],
  arguments[7]
);
            """;
}
