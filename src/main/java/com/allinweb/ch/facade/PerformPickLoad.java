package com.allinweb.ch.facade;

import com.allinweb.ch.util.ErrorMessage;
import java.util.Arrays;
import java.util.List;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

/**
 * PerformActions.
 *
 * @author Osvaldo Martini
 * @version 1.0
 */
public class PerformPickLoad {

    protected static volatile PerformPickLoad instance;

    // Private constructor to prevent instantiation
    private PerformPickLoad() {
        // Initialize if necessary
    }

    public static PerformPickLoad getInstance() {
        if (instance == null) {
            synchronized (PerformPickLoad.class) {
                if (instance == null) {
                    instance = new PerformPickLoad();
                }
            }
        }
        return instance;
    }

    private static JavascriptExecutor jsExecutor;

    public ErrorMessage dynamicHoverPickElementsDTO(
            WebDriver driver, String currentUrl, String[] dataArray, boolean searchHiddenFields, int port) {

        List<String> dataList = Arrays.asList(dataArray);
        try {
            jsExecutor = (JavascriptExecutor) driver;
            jsExecutor.executeScript(jsHoverPickInject, currentUrl, currentUrl, dataList, searchHiddenFields, port);
            return null;
        } catch (Exception error) {
            return new ErrorMessage("Error running Scanner", "Dynamic Load ElementsDTO error", error.getMessage());
        }
    }

    private String jsHoverPickInject =
            """
                            (function (
                              targetOriginURL,
                              trustedOriginURL,
                              searchTerms,
                              hiddenFields,
                              socketPort
                            ) {
                              var attempts = 0;
                              var wSocket = null;
                              var tooltip = document.createElement("div");
                              tooltip.id = "Martini-Is-Awesome";
                              tooltip.style.position = "absolute";
                              tooltip.style.backgroundColor = "rgba(0, 0, 0, 0)"; // Full transparency
                              tooltip.style.border = "1px solid #ccc";
                              tooltip.style.padding = "10px";
                              tooltip.style.borderRadius = "5px";
                              tooltip.style.boxShadow = "0 2px 4px rgba(0, 0, 0, 0.2)";
                              tooltip.style.fontFamily = "Arial, sans-serif";
                              tooltip.style.fontSize = "14px";
                              tooltip.style.color = "#333";
                              tooltip.style.zIndex = "10000";
                              tooltip.style.display = "none";
                              document.body.appendChild(tooltip);

                              window.elementInfoMap = new Map();
                              window.allElementInfo = [];

                              function connectWebSocket() {
                                try {
                                  wSocket = new WebSocket(`ws://localhost:${socketPort}/websocket`);

                                  wSocket.onopen = () => {
                                    console.log("WebSocket connected");
                                    attempts = 0; // Reset attempts on successful connection

                                    try {
                                      const subscriptionMessage = {
                                        type: "echo",
                                        body: "subscribe",
                                      };
                                      wSocket.send(JSON.stringify(subscriptionMessage));
                                    } catch (sendError) {
                                      console.error("Failed to send subscription message:", sendError);
                                    }

                                    // Call startCollectingElements AFTER WebSocket is open
                                    sendingData();
                                  };

                                  wSocket.onmessage = (event) => {
                                    let receivedMessage = event.data;

                                    if (receivedMessage.endsWith("\\u0000")) {
                                      receivedMessage = receivedMessage.slice(0, -1);
                                    }

                                    if (receivedMessage) {
                                      try {
                                        const parsedObject = JSON.parse(receivedMessage);
                                        console.log("WebSocket message received:", parsedObject);

                                        // Process parsedObject.body and parsedObject.footer here
                                        if (parsedObject.body.includes("data_updated")) {
                                          //Handle data update
                                        }

                                        if (
                                          parsedObject.body.includes("cannot be processed") ||
                                          (parsedObject.footer &&
                                            parsedObject.footer.includes("cannot be processed"))
                                        ) {
                                          //Handle cannot be processed
                                        }
                                      } catch (parseError) {
                                        console.warn("Non-JSON message received:", receivedMessage);
                                      }
                                    }
                                  };

                                  wSocket.onerror = (error) => {
                                    console.error("WebSocket error:", error);
                                  };

                                  wSocket.onclose = () => {
                                    console.log("WebSocket connection closed");

                                    if (attempts < 100) {
                                      attempts++;
                                      console.log(`Reconnecting attempt ${attempts}...`);
                                      connectWebSocket();
                                    } else {
                                      console.log("100 Attempts to Reconnect with the WebSocket.");
                                    }
                                  };
                                } catch (initError) {
                                  console.error("Failed to initialize WebSocket:", initError);
                                }
                              }

                              // Optionally, expose a cleanup function
                              window.cleanupWebSocket = () => {
                                try {
                                  console.log("Cleaning up WebSocket...");
                                  if (wSocket && wSocket.readyState === WebSocket.OPEN) {
                                    wSocket.close();
                                  }
                                } catch (cleanupError) {
                                  console.error("Error during WebSocket cleanup:", cleanupError);
                                }
                              };

                              function sendingData() {
                                window.allElementInfo = [];
                                limitMapCharacters(window.elementInfoMap);
                                console.log("All element info stored in Map:", window.allElementInfo);

                                if (wSocket && wSocket.readyState) {
                                  console.log("WebSocket readyState:", wSocket.readyState);
                                }

                                if (wSocket && wSocket.readyState === WebSocket.OPEN) {
                                  if (window.allElementInfo.length > 0) {
                                    const message = {
                                      type: "SEARCH_TOOL",
                                      details: window.allElementInfo, // Send allElementInfo
                                    };
                                    wSocket.send(JSON.stringify(message));
                                    console.log("Sent SEARCH_TOOL:", message);
                                    window.elementInfoMap.clear();
                                  }
                                } else {
                                  console.warn("WebSocket is not open. Cannot send message.");
                                }
                              }

                              // Function to collect general elements based on search terms
                              const collectElements = function collectElements(
                                doc,
                                searchTerms,
                                collectionFound
                              ) {
                                // Collect elements from the current document using the provided search terms
                                if (searchTerms.length > 0) {
                                  searchTerms.forEach((selector) => {
                                    // If search term includes "with id", filter only elements that have an "id" attribute
                                    if (selector.includes("with id")) {
                                      collectionFound.push(...Array.from(doc.querySelectorAll("[id]")));
                                    } // If search term includes "with id", filter only elements that have an "id" attribute
                                    else if (selector.includes("with name")) {
                                      foundElements = Array.from(doc.querySelectorAll("[name]"));
                                      collectionFound.push(...Array.from(doc.querySelectorAll("[name]")));
                                    } else {
                                      collectionFound.push(...Array.from(doc.querySelectorAll(selector)));
                                    }
                                  });
                                } else {
                                  // Collect all elements except iframes
                                  collectionFound.push(
                                    ...Array.from(doc.querySelectorAll("*")).filter(
                                      (el) => el.tagName.toLowerCase() !== "iframe"
                                    )
                                  );
                                }

                                // After collecting, process element identities for the parent document
                                collectionFound.forEach((element) => {
                                  if (
                                    [
                                      "html",
                                      "body",
                                      "main",
                                      "script",
                                      "meta",
                                      "head",
                                      "style",
                                      "iframe",
                                    ].includes(element.tagName.toLowerCase())
                                  ) {
                                    return;
                                  }

                                  const elementIdentity = getElementIdentity(element);
                                  if (elementIdentity) {
                                    filterSearchTerms(
                                      "tagName-Found",
                                      elementIdentity.xPath,
                                      elementIdentity,
                                      searchTerms
                                    );
                                  }
                                });
                              };

                              function filterSearchTerms(
                                typeDTO,
                                referXPath,
                                elementIdentity,
                                searchTerms
                              ) {
                                if (
                                  searchTerms.length === 0 ||
                                  (!searchTerms.includes("with id") &&
                                    !searchTerms.includes("with name") &&
                                    !searchTerms.includes("allWithText"))
                                ) {
                                  // If no search terms, directly add the element
                                  window.elementInfoMap.set(
                                    referXPath,
                                    elementDTO(typeDTO, elementIdentity)
                                  );
                                  return;
                                }
                                // Iterate through search terms and apply corresponding checks
                                searchTerms.forEach((term) => {
                                  let matches = false;

                                  if (
                                    term.includes("with id") &&
                                    elementIdentity.attributeData.some((attr) => attr.name === "id")
                                  ) {
                                    matches = true;
                                  } else if (
                                    term.includes("with name") &&
                                    elementIdentity.attributeData.some((attr) => attr.name === "name")
                                  ) {
                                    matches = true;
                                  } else if (
                                    term.includes("allWithText") &&
                                    elementIdentity.someText.length > 0
                                  ) {
                                    matches = true;
                                  }

                                  // If a match is found, set the element in the map
                                  if (matches) {
                                    window.elementInfoMap.set(
                                      referXPath,
                                      elementDTO(typeDTO, elementIdentity)
                                    );
                                  }
                                });
                              }

                              function fetchAndParseIframeContent(iframe) {
                                if (!iframe.src) return null;

                                const xhr = new XMLHttpRequest();
                                xhr.open("GET", iframe.src, false); // 'false' makes the request synchronous

                                try {
                                  xhr.send();

                                  if (xhr.status !== 200) {
                                    console.error("Error fetching the iframe content:", xhr.status);
                                    return null;
                                  }

                                  const htmlContent = xhr.responseText;

                                  // Parse the HTML content
                                  const parser = new DOMParser();
                                  const parsedDocument = parser.parseFromString(htmlContent, "text/html");

                                  // Get all elements inside the parsed document
                                  const srcElements = parsedDocument.querySelectorAll("*");
                                  console.log(`srcElements Total: <${srcElements.length}>`);

                                  // srcElements.forEach((element) => {
                                  //   console.log(`Element: <${element.tagName}>`);
                                  //   console.log("Text Content:", element.textContent.trim());
                                  // });

                                  return srcElements; // Return the NodeList
                                } catch (error) {
                                  console.error("Error fetching the iframe content:", error);
                                  return null;
                                }
                              }

                              const iFrameDetails = function iFrameDetails(iframe, xPathIFrame, childSize) {
                                const iframeDetails = `Elements inside iframe: ${childSize}`;

                                console.log(
                                  `iFrame Found: ${
                                    iframe.src ||
                                    iframe.title ||
                                    iframe.id ||
                                    iframe.name ||
                                    "No description"
                                  }; ${iframeDetails}`
                                );
                                // // Store the iframe details in the elementInfoMap
                                // elementInfoMap.set(
                                //   xPathIFrame,
                                //   `xpath:${xPathIFrame};text:${
                                //     iframe.src ||
                                //     iframe.title ||
                                //     iframe.id ||
                                //     iframe.name ||
                                //     "No description"
                                //   };${iframeDetails}`
                                // );
                              };

                              // Function to collect iframe elements recursively
                              const collectIframeElements = function collectIframeElements(
                                doc,
                                collectionFound,
                                isIframeChild = false,
                                iframe
                              ) {
                                console.log("isIframe");

                                if (iframe) {
                                  //doc.querySelectorAll("iframe").forEach((iframe) => {
                                  try {
                                    let iframeDocument =
                                      iframe.contentDocument || iframe.contentWindow.document;

                                    try {
                                      console.log(
                                        "Iframe origin:",
                                        new URL(iframe.src, window.location.origin).origin
                                      );
                                      console.log("Parent origin:", window.location.origin);
                                    } catch (e) {
                                      console.warn("Cross-origin access denied for iframe:", iframe.src);
                                    }

                                    if (iframe) {
                                      let iframeParsed = null;
                                      let srcDocElements = null;

                                      const xPathIFrame = getMartiniXPath(iframe); // Get the XPath of the iframe

                                      const elementIdentity = getElementIdentity(iframe);
                                      if (elementIdentity) {
                                        filterSearchTerms(
                                          "iFrame-Found",
                                          elementIdentity.xPath,
                                          elementIdentity,
                                          searchTerms
                                        );
                                      }

                                      const parser = new DOMParser();

                                      if (iframe.srcdoc) {
                                        iframeParsed = parser.parseFromString(iframe.srcdoc, "text/html");

                                        // Select all elements inside the parsed document
                                        srcDocElements = iframeParsed.querySelectorAll("*");
                                      }

                                      if (iframe.src) {
                                        const srcElements = fetchAndParseIframeContent(iframe);
                                        if (srcElements) {
                                          // console.log("Fetched Elements:", srcElements);

                                          iFrameDetails(iframe, xPathIFrame, srcElements.length);

                                          srcElements.forEach(function (element) {
                                            const elementIdentity = getElementIdentity(element);
                                            // console.log(
                                            //   "elementIdentity.xPath",
                                            //   `${xPathIFrame}${elementIdentity?.xPath}`
                                            // );
                                            if (elementIdentity) {
                                              elementIdentity.iFrameXPath = xPathIFrame;
                                              filterSearchTerms(
                                                "iFrame-Child",
                                                `${xPathIFrame}${elementIdentity?.xPath}`,
                                                elementIdentity,
                                                searchTerms
                                              );
                                            }
                                          });
                                        }
                                      }

                                      // Collect all elements inside the iframe
                                      if (!iframe.src) {
                                        iFrameDetails(
                                          iframe,
                                          xPathIFrame,
                                          srcDocElements
                                            ? srcDocElements.length
                                            : iframeDocument
                                            ? iframeDocument.querySelectorAll("*").length
                                            : 0
                                        );
                                      }

                                      iframeDocument
                                        .querySelectorAll("*")
                                        .forEach(function (elementInsideIframe) {
                                          const elementIdentity = getElementIdentity(elementInsideIframe);

                                          // console.log(
                                          //   "elementIdentity.xPath",
                                          //   `${xPathIFrame}${elementIdentity?.xPath}`
                                          // );
                                          if (elementIdentity) {
                                            elementIdentity.iFrameXPath = xPathIFrame;
                                            filterSearchTerms(
                                              "iFrame-Child",
                                              `${xPathIFrame}${elementIdentity?.xPath}`,
                                              elementIdentity,
                                              searchTerms
                                            );
                                          }
                                        });

                                      // Loop through all the elements and extract their properties
                                      srcDocElements?.forEach(function (element) {
                                        const elementIdentity = getElementIdentity(element);
                                        // console.log(
                                        //   "elementIdentity.xPath",
                                        //   `${xPathIFrame}${elementIdentity?.xPath}`
                                        // );
                                        if (elementIdentity) {
                                          elementIdentity.iFrameXPath = xPathIFrame;
                                          filterSearchTerms(
                                            "iFrame-Child",
                                            `${xPathIFrame}${elementIdentity?.xPath}`,
                                            elementIdentity,
                                            searchTerms
                                          );
                                        }
                                      });

                                      // Process iframe content depending on the presence of srcdoc
                                      if (iframeParsed) {
                                        processIframeElements(iframeParsed, xPathIFrame);
                                      }

                                      // If the iframe contains nested iframes, recursively collect them
                                      collectIframeElements(iframeDocument, collectionFound, true);
                                    } else {
                                      console.warn(`Skipping cross-origin iframe: ${iframe.src}`);
                                    }
                                  } catch (e) {
                                    console.error(
                                      `Error accessing iframe: ${iframe.src || "Unknown iframe"}`,
                                      e
                                    );
                                  }
                                }
                              };

                              const processIframeElements = function (iframeDocument, xPathIFrame) {
                                // Collect all elements inside the iframe
                                iframeDocument
                                  .querySelectorAll("*")
                                  .forEach(function (elementInsideIframe) {
                                    const elementIdentity = getElementIdentity(elementInsideIframe);

                                    // console.log(
                                    //   "elementIdentity.xPath",
                                    //   `${xPathIFrame}${elementIdentity?.xPath}`
                                    // );
                                    if (elementIdentity) {
                                      elementIdentity.iFrameXPath = xPathIFrame;
                                      filterSearchTerms(
                                        "iFrame-Child",
                                        `${xPathIFrame}${elementIdentity?.xPath}`,
                                        elementIdentity,
                                        searchTerms
                                      );
                                    }
                                  });
                              };

                              var lastHoveredIsIframe = null; // Keep track of the last hovered element type

                              let lastHoveredElement = null; // Keep track of the previously hovered element
                              // Declare global variables to store iframe details
                              var iframeDocument = null;
                              var iframeElementsCount = 0;

                              function showMartiniTooltip(event) {
                                var elementBelowTooltip = document.elementFromPoint(
                                  event.clientX,
                                  event.clientY
                                );

                                // Do nothing if the hovered element is the tooltip itself or an excluded tag (html, body, main)
                                if (
                                  !elementBelowTooltip ||
                                  elementBelowTooltip === tooltip ||
                                  ["html", "body", "main"].includes(
                                    elementBelowTooltip.tagName.toLowerCase()
                                  )
                                ) {
                                  return;
                                }

                                var isIframe = elementBelowTooltip.tagName.toLowerCase() === "iframe";

                                // Reset only if switching between iframe and non-iframe elements
                                if (lastHoveredIsIframe !== isIframe) {
                                  console.clear();
                                  elementInfoMap.clear();
                                  window.allElementInfo = [];
                                }

                                lastHoveredIsIframe = isIframe; // Update last hovered element type

                                // Get the tag name of the element
                                var tagNameTemp = elementBelowTooltip.tagName.toLowerCase();

                                // If it's an iframe, get the number of elements inside the iframe
                                var iframeDetails = "";
                                if (isIframe) {
                                  iframeDocument =
                                    elementBelowTooltip.contentDocument ||
                                    elementBelowTooltip.contentWindow.document;
                                  iframeElementsCount = iframeDocument
                                    ? iframeDocument.body.getElementsByTagName("*").length
                                    : 0;
                                  iframeDetails = `Elements inside iframe: ${iframeElementsCount}`;
                                }

                                const elementIdentity = getElementIdentity(elementBelowTooltip);

                                // Store tagName and other details in the Map
                                if (elementIdentity && iframeDetails && iframeDetails.length > 0) {
                                  // window.elementInfoMap.set(
                                  //   elementIdentity.xPath,
                                  //   elementDTO("iFrame-Found", elementIdentity)
                                  // );

                                  let collectionFound = [];

                                  // First, collect iframe elements
                                  collectIframeElements(document, collectionFound, elementInfoMap);
                                } else if (elementIdentity) {
                                  // window.elementInfoMap.set(
                                  //   elementIdentity.xPath,
                                  //   elementDTO("tagName-Found", elementIdentity)
                                  // );

                                  let collectionFound = [];
                                  // Then, collect general elements based on search terms
                                  collectElements(document, searchTerms, collectionFound, elementInfoMap);
                                }

                                // Parse the someText using the semicolon delimiter
                                var parsedText = elementIdentity.someText?.split(";") || [];

                                // Format the tooltip content to make it more readable
                                var tooltipContent = "";
                                tooltipContent += isIframe ? "[Iframe] <br>" : "";
                                tooltipContent += `Tag Name: ${tagNameTemp}<br>`;
                                tooltipContent += isIframe ? `- ${iframeDetails}<br>` : "";

                                // Replace new lines with <br> before adding each item from parsedText
                                tooltipContent += elementIdentity.someText
                                  ? parsedText.map((item) => `- ${item}<br>`).join("")
                                  : "No Text<br>";

                                // Set the tooltip content with line breaks
                                tooltip.innerHTML = tagNameTemp;

                                // Position the tooltip near the mouse cursor
                                var tooltipWidth = tooltip.offsetWidth;
                                var tooltipHeight = tooltip.offsetHeight;
                                var left = event.pageX - tooltipWidth / 2;
                                var top = event.pageY - tooltipHeight / 2;

                                tooltip.style.left = left + "px";
                                tooltip.style.top = top + "px";
                                tooltip.style.display = "block";

                                // Highlight the hovered element
                                if (lastHoveredElement !== elementBelowTooltip) {
                                  // Remove highlight from the previous element if any
                                  if (lastHoveredElement) {
                                    lastHoveredElement.style.outline = ""; // Remove the previous highlight
                                  }
                                  // Add a border to highlight the current element
                                  elementBelowTooltip.style.outline = "3px solid red"; // Highlight the element

                                  lastHoveredElement = elementBelowTooltip; // Update the last hovered element
                                }

                                // console.log("Element Info:", elementInfoMap);
                              }

                              function hideMartiniTooltip() {
                                tooltip.style.display = "none";
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
                                const xPath = getMartiniXPath(element);
                                const tagName = element.tagName.toLowerCase();
                                const attributeData = Array.from(element.attributes).map((attr) => ({
                                  name: attr.name,
                                  value: attr.value,
                                }));
                                const attribId = element.id || "";
                                const attribName = element.name || "";
                                const coords = `${element.getBoundingClientRect().left.toFixed(2)},${element
                                  .getBoundingClientRect()
                                  .top.toFixed(2)}`;
                                const someText = getSomeText(tagName, attributeData, element);

                                return {
                                  xPath,
                                  tagName,
                                  attributeData,
                                  customXPath: "",
                                  attribId,
                                  attribName,
                                  coords,
                                  someText,
                                };
                              };

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

                              const elementDTO = function elementDTO(typeElement, identity) {
                                return {
                                  typeElement: typeElement,
                                  tagName: identity.tagName ?? "No Tag Name Detected",
                                  xPath: identity.xPath ?? "",
                                  someText: identity.someText ?? "",
                                  attribId: identity.attribId ?? "",
                                  attribName: identity.attribName ?? "",
                                  coords: identity.coords ?? "",
                                  attributeData: identity.attributeData ?? "",
                                  customXPath: identity.customXPath ?? "",
                                  iFrameXPath: identity.iFrameXPath ?? "",
                                  attributeValue: identity.attributeValue ?? "",
                                  attributeType: identity.attributeType ?? "",
                                  searchAttributeValue: identity.searchAttributeValue ?? "",
                                };
                              };

                              function limitMapCharacters(elementInfoMap) {
                                elementInfoMap.forEach((value, key) => {
                                  let modifiedValue = value;
                                  window.allElementInfo.push(modifiedValue);
                                });
                              }

                              function getSomeText(tagName, attributeData, element) {
                                let textSet = new Set();
                                let textResult = "";

                                if (["input", "textarea", "select", "button"].includes(tagName)) {
                                  const extractedText = extractTextFromHTML(element || "");
                                  textResult = [
                                    ...extractedText.titles,
                                    ...extractedText.text,
                                    ...extractedText.labels,
                                  ]
                                    .join("; ")
                                    .trim();
                                } else if (["option", "label", "a"].includes(tagName)) {
                                  const extractedText = extractTextFromHTML(element || "");
                                  textResult = [
                                    ...extractedText.titles,
                                    ...extractedText.text,
                                    ...extractedText.labels,
                                  ]
                                    .join("; ")
                                    .trim();
                                } else if (!["html", "body", "script"].includes(tagName)) {
                                  const extractedText = extractTextFromHTML(element || "");
                                  textResult = [
                                    ...extractedText.titles,
                                    ...extractedText.text,
                                    ...extractedText.labels,
                                  ]
                                    .join("; ")
                                    .trim();
                                }

                                // Now, extract text from attributes AFTER processing the element
                                attributeData.forEach((attr) => {
                                  const trimmedValue = attr.value.trim();

                                  if (trimmedValue) {
                                    // Process only if value is not empty
                                    if (
                                      attr.name === "placeholder" ||
                                      attr.name === "label" ||
                                      attr.name === "name" ||
                                      attr.name === "title" ||
                                      attr.name === "id"
                                    ) {
                                      textSet.add(trimmedValue);
                                    }
                                  }
                                });

                                // Continue processing srcdoc separately
                                attributeData.forEach((attr) => {
                                  if (attr.name === "srcdoc") {
                                    try {
                                      const doc = new DOMParser().parseFromString(attr.value, "text/html");
                                      const extractedText = extractTextFromHTML(doc.body);
                                      [
                                        ...extractedText.titles,
                                        ...extractedText.text,
                                        ...extractedText.labels,
                                      ].forEach((text) => textSet.add(text.trim()));
                                    } catch (e) {
                                      console.warn("Error parsing srcdoc:", e);
                                    }
                                  }
                                });

                                // Add the extracted text from the element to the set to avoid duplicates
                                textResult
                                  .split(";")
                                  .map((text) => text.trim())
                                  .filter(Boolean)
                                  .forEach((text) => textSet.add(text));

                                // Return a clean, unique, and deduplicated string
                                return Array.from(textSet).join("; ");
                              }

                              function extractTextFromHTML(element) {
                                // If element is invalid or empty, return an empty result
                                if (!element || element === " ") {
                                  return {
                                    text: [],
                                    labels: [],
                                    titles: [],
                                  };
                                }
                                const result = {
                                  text: new Set(), // Using Set to avoid duplicate text
                                  labels: new Set(), // Using Set to avoid duplicate labels
                                  titles: new Set(), // Using Set to avoid duplicate titles
                                };

                                // Extract text content directly from the element (in case it has no children)
                                if (element.textContent) {
                                  let elementText = element.textContent.trim();
                                  if (elementText) {
                                    result.text.add(elementText); // Using .add() instead of .push() for Set
                                  }
                                }

                                // Extract label text from input placeholders and other form-related data
                                element.querySelectorAll("label").forEach((label) => {
                                  if (label.textContent) {
                                    let labelText = label.textContent.trim();
                                    if (labelText) {
                                      result.labels.add(labelText); // Using .add() for Set to ensure uniqueness
                                    }
                                  }

                                  // Handle associated input fields (if the label has a 'for' attribute)
                                  let forAttribute = label.getAttribute("for");
                                  if (forAttribute) {
                                    let associatedInput = element.querySelector(`#${forAttribute}`);
                                    if (associatedInput) {
                                      // Check if it's an input field or textarea and extract value or placeholder
                                      let inputValue = associatedInput.value?.trim();
                                      let inputPlaceholder = associatedInput.placeholder?.trim();
                                      if (inputValue) {
                                        result.text.add(inputValue); // Using .add() for Set to ensure uniqueness
                                      } else if (inputPlaceholder) {
                                        result.text.add(inputPlaceholder); // Fallback to placeholder
                                      }
                                    }
                                  }
                                });

                                // Extract text from common block and inline elements
                                const textExtractors = [
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

                                textExtractors.forEach((tagName) => {
                                  element.querySelectorAll(tagName).forEach((childElement) => {
                                    if (childElement.textContent) {
                                      let elemText = childElement.textContent.trim();
                                      if (elemText) {
                                        result.text.add(elemText); // Using .add() for Set to ensure uniqueness
                                      }
                                    }
                                  });
                                });

                                // Extract text from <a> tags (links)
                                element.querySelectorAll("a").forEach((link) => {
                                  if (link.textContent) {
                                    let linkText = link.textContent.trim();
                                    if (linkText) {
                                      result.text.add(linkText); // Using .add() for Set to ensure uniqueness
                                    }
                                  }
                                });

                                // Extract iframe titles and nested content
                                element.querySelectorAll("iframe").forEach((iframe) => {
                                  if (iframe.getAttribute("title")) {
                                    let title = iframe.getAttribute("title")?.trim();
                                    if (title) {
                                      result.titles.add(title); // Using .add() for Set to ensure uniqueness
                                    }
                                  }

                                  try {
                                    let iframeDoc =
                                      iframe.contentDocument ||
                                      new DOMParser().parseFromString(iframe.srcdoc || "", "text/html");
                                    let iframeContent = extractTextFromHTML(iframeDoc); // Here we assume iframeDoc is an element.
                                    iframeContent.titles.forEach((title) => result.titles.add(title));
                                    iframeContent.text.forEach((text) => result.text.add(text));
                                    iframeContent.labels.forEach((label) => result.labels.add(label));
                                  } catch (e) {
                                    console.warn("Could not access iframe content", e);
                                  }
                                });

                                // Convert Sets to arrays before returning to maintain previous structure
                                return {
                                  text: Array.from(result.text),
                                  labels: Array.from(result.labels),
                                  titles: Array.from(result.titles),
                                };
                              }

                              function cleanOldValues() {
                                window.allElementInfo = [];
                              }

                              cleanOldValues();

                              window.revertPickInjections = function () {
                                // alert("revertPickInjections");

                                document.removeEventListener("mouseover", showMartiniTooltip);
                                document.removeEventListener("click", handleMartiniClick);
                                console.log("revertPickInjections");

                                // Remove the tooltip from the page and delete the reference after 5 seconds
                                setTimeout(() => {
                                  removeElements();
                                  window.allElementInfo = [];
                                }, 1000);
                              };

                              function removeElements() {
                                // Remove highlight from the previous element if any
                                if (lastHoveredElement) {
                                  lastHoveredElement.style.outline = ""; // Remove the previous highlight
                                }

                                if (tooltip) {
                                  tooltip.remove(); // Completely remove the tooltip from the DOM
                                  tooltip = null; // Clear the reference to free memory
                                  console.log("Tooltip completely removed.");
                                }
                              }

                              connectWebSocket();

                              function handleMartiniClick(event) {
                                event.preventDefault();
                                event.stopPropagation();
                                tooltip.style.display = "none";

                                // Determine the element below the tooltip (mouse position)
                                var elementBelowTooltip = document.elementFromPoint(
                                  event.clientX,
                                  event.clientY
                                );

                                // Hide the tooltip
                                tooltip.style.display = "none";

                                // If the element below the tooltip is an iframe
                                if (
                                  elementBelowTooltip &&
                                  elementBelowTooltip.tagName.toLowerCase() === "iframe"
                                ) {
                                  // Get the document inside the iframe
                                  var iframeDocument =
                                    elementBelowTooltip.contentDocument ||
                                    elementBelowTooltip.contentWindow.document;

                                  // If the iframe document is valid
                                  if (iframeDocument) {
                                    // Format the iframe details
                                    var iframeDetails = `Elements inside iframe: ${
                                      iframeDocument.body.getElementsByTagName("*").length
                                    }`;

                                    // Display the tooltip with iframe details
                                    tooltip.innerHTML = `[Iframe] <br> ${iframeDetails}`;

                                    // Position the tooltip near the mouse cursor
                                    var tooltipWidth = tooltip.offsetWidth;
                                    var tooltipHeight = tooltip.offsetHeight;
                                    var left = event.pageX - tooltipWidth / 2;
                                    var top = event.pageY - tooltipHeight / 2;

                                    tooltip.style.left = left + "px";
                                    tooltip.style.top = top + "px";
                                    tooltip.style.display = "block";

                                    // Initialize an array to store the iframe element information
                                    window.allElementInfo = [];

                                    const elementIdentity = getElementIdentity(elementBelowTooltip);

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
                                  } else {
                                    tooltip.innerHTML = "No iframe document found.";

                                    // Position the tooltip near the mouse cursor
                                    var tooltipWidth = tooltip.offsetWidth;
                                    var tooltipHeight = tooltip.offsetHeight;
                                    var left = event.pageX - tooltipWidth / 2;
                                    var top = event.pageY - tooltipHeight / 2;

                                    tooltip.style.left = left + "px";
                                    tooltip.style.top = top + "px";
                                    tooltip.style.display = "block";
                                  }
                                } else {
                                  // If the clicked element is not an iframe, gather its regular information
                                  var tagName = elementBelowTooltip.tagName.toLowerCase();

                                  // Avoid main, body, and html tags
                                  if (["html", "body", "main"].includes(tagName)) {
                                    return; // Don't proceed if it's one of these elements
                                  }

                                  // window.elementInfoMap.clear();
                                  const elementIdentity = getElementIdentity(elementBelowTooltip);
                                  // Store tagName and other details in the Map
                                  if (elementIdentity) {
                                    window.elementInfoMap.set(
                                      elementIdentity.xPath,
                                      elementDTO("clicked", elementIdentity)
                                    );

                                    // Show the tooltip with the element details
                                    // tooltip.innerHTML = `${tagName} <br> ${someText}`;
                                    tooltip.innerHTML = `${tagName} <br> ${elementIdentity.someText}`;
                                    var tooltipWidth = tooltip.offsetWidth;
                                    var tooltipHeight = tooltip.offsetHeight;
                                    var left = event.pageX - tooltipWidth / 2;
                                    var top = event.pageY - tooltipHeight / 2;

                                    tooltip.style.left = left + "px";
                                    tooltip.style.top = top + "px";
                                    tooltip.style.display = "block";
                                  }
                                }

                                sendingData();

                                // window.revertPickInjections();

                                // Remove the tooltip from the page and delete the reference after 5 seconds
                                setTimeout(() => {
                                  window.allElementInfo = [];
                                  window.elementInfoMap.clear();

                                  // if (tooltip) {
                                  //   tooltip.remove(); // Completely remove the tooltip from the DOM
                                  //   tooltip = null; // Clear the reference to free memory
                                  //   console.log("Tooltip completely removed.");
                                  // }

                                  // if (lastHoveredElement || elementBelowTooltip) {
                                  //   // Remove highlight from the previous element if any
                                  //   if (lastHoveredElement) {
                                  //     lastHoveredElement.style.outline = ""; // Remove the previous highlight
                                  //   }

                                  //   // Remove highlight from the previous element if any
                                  //   if (elementBelowTooltip) {
                                  //     elementBelowTooltip.style.outline = ""; // Remove the previous highlight
                                  //   }
                                  // }
                                }, 1000);
                              }

                              document.addEventListener("mouseover", showMartiniTooltip);
                              //                document.addEventListener('mouseout', hideMartiniTooltip);
                              document.addEventListener("click", handleMartiniClick);

                              window.postMessage({ type: "myMessage", data: "some data" }, targetOriginURL);

                              window.addEventListener("message", function (event) {
                                if (event.origin !== trustedOriginURL) return; // check the origin
                                console.log(event.data);
                              });

                              // window.pickTerms = null; // Invalidating the function
                            })(arguments[0], arguments[1], arguments[2], arguments[3], arguments[4]);
                            // })("http://localhost:3000/", "http://localhost:3000/", ["*"], false, 8181);

                    """;
}
