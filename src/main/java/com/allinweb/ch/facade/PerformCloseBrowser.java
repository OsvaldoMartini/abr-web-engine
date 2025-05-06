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
public class PerformCloseBrowser {
    protected static volatile PerformCloseBrowser instance;

    // Private constructor to prevent instantiation
    private PerformCloseBrowser() {
        // Initialize if necessary
    }

    public static PerformCloseBrowser getInstance() {
        if (instance == null) {
            synchronized (PerformCloseBrowser.class) {
                if (instance == null) {
                    instance = new PerformCloseBrowser();
                }
            }
        }
        return instance;
    }

    private static JavascriptExecutor jsExecutor;

    public ErrorMessage dynamicCloseBrowser(
            WebDriver driver,
            int port,
            String sessionId,
            String destination,
            String operationId,
            int homeBankingId,
            String urlTarget) {
        try {
            jsExecutor = (JavascriptExecutor) driver;
            jsExecutor.executeScript(
                    jsCloseBrowserInject, port, sessionId, destination, operationId, homeBankingId, urlTarget);
            return null;
        } catch (Exception error) {
            return new ErrorMessage("Error running Scanner", "Dynamic Load ElementsDTO error", error.getMessage());
        }
    }

    private String jsCloseBrowserInject =
            """
// CLOSE BROWSER IN USE CSP (SENDER: scannerTool) -> scannerGrid
(function (
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

  window.destination = destination;
  window.operationId = operationId;
  window.homeBankingId = homeBankingId;
  window.sessionId = `${sessionId}-${homeBankingId}`;

  function logCSPDirectives() {
    const csp = document.querySelector(
      "meta[http-equiv='Content-Security-Policy']"
    );
    if (csp) {
      console.log("Content Security Policy:", csp.content);
      const directives = csp.content.split(";").map((d) => d.trim());
      const connectSrcDirective = directives.find((d) =>
        d.startsWith("connect-src")
      );
      if (connectSrcDirective) {
        console.log("connect-src:", connectSrcDirective);
      } else {
        // Check if default-src might apply to connections
        const defaultSrcDirective = directives.find((d) =>
          d.startsWith("default-src")
        );
        if (defaultSrcDirective) {
          console.log(
            "connect-src not explicitly set. Falling back to default-src:",
            defaultSrcDirective
          );
        } else {
          console.log(
            "connect-src not explicitly set, and no default-src found."
          );
        }
      }
    } else {
      // Check for CSP in HTTP headers (this is more complex and often requires a server request)
      // For a client-side script, you might not have direct access to these headers easily.
      // One potential (but less clean) way could involve a dummy fetch request and inspecting the headers.
      // However, this can be complex and might trigger CORS issues.
      console.log(
        "Content Security Policy meta tag not found. CSP might be set via HTTP headers."
      );
    }
  }

  // Call this function early in your script's execution
  logCSPDirectives();

  function connectWebSocket() {
    if (attempts >= maxAttempts) {
      //console.error("Reached maximum reconnection attempts. Stopping.");
      return;
    }

    try {
      //console.log(`Attempt ${attempts + 1} to connect to WebSocket...`);
      wSocket = new WebSocket(
        `wss://localhost:${socketPort}/websocket?sessionId=${window.sessionId}`
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
        console.error("WebSocket error:", error);
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
      console.error("Failed to initialize WebSocket:", initError);
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

  function startPing() {
    // Send a ping every 30 seconds (adjust if needed)
    pingIntervalId = setInterval(() => {
      if (wSocket && wSocket.readyState === WebSocket.OPEN) {
        const pingMessage = {
          type: "ping-close-browser-csp",
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

  window.addEventListener("beforeunload", function (event) {
    // event.preventDefault();
    // event.returnValue =
    //   "⚠️ Warning: Closing this tab will terminate an active WebDriver session!";

    if (wSocket && wSocket.readyState === WebSocket.OPEN) {
      const message = {
        type: "CLOSE_BROWSER",
        sessionId: `scannerReceiver-${window.homeBankingId}`,
        operationId: "closeBrowser",
        homeBankingId: window.homeBankingId,
        details: window.allElementInfo, // Send allElementInfo
      };

      // Convert the JSON message to a buffer
      const base64Message = btoa(
        unescape(encodeURIComponent(JSON.stringify(message)))
      );
      // Convert the buffer to a Base64 string
      wSocket.send(base64Message);

      alreadySent = true;
      window.allElementInfo = [];
      window.elementInfoMap.clear();
      window.revertSearchInjections();
    }
  });

  window.postMessage({ type: "myMessage", data: "some data" }, targetOriginURL);

  window.addEventListener("message", function (event) {
    if (event.origin !== trustedOriginURL) return; // check the origin
    //console.log(event.data);
  });

  connectWebSocket();

  // window.cloneTerms = null; // Invalidating the function
})(
  arguments[0],
  arguments[1],
  arguments[2],
  arguments[3],
  arguments[4],
  arguments[5],
  arguments[6]
);
// })(
//   61757,
//   "closeBrowser",
//   "scannerReceiver-2",
//   "closeBrowser",
//   2,
//   "https://www.tradingview.com/",
//   "https://www.tradingview.com/"
//   // "https://www.bloomberg.com/",
//   // "https://www.bloomberg.com/"
// );
""";
}
