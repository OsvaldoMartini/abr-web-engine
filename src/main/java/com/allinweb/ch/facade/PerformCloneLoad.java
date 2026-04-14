package com.allinweb.ch.facade;

import com.allinweb.ch.util.ErrorMessage;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

// HOVER PICK IN USE (SENDER: scannerTool) -> scannerGrid
@Slf4j
public class PerformCloneLoad {
    protected static volatile PerformCloneLoad instance;

    /**
     * Cached hoverPick bundle. Null until the first call to dynamicPickOneCloneElementsDTO().
     * Loaded lazily so a missing file does NOT crash the JVM at startup -
     * the error surfaces only when a pick is actually triggered.
     *
     * Loaded from the filesystem path defined by PATH_PLUGINS in ARWeb.config:
     *   {path_plugins}/hoverPick/build/hoverPick.min.js
     *
     * To rebuild the bundle:
     *   cd {path_plugins}/hoverPick
     *   npx esbuild index.js --bundle --minify --outfile=build/hoverPick.min.js
     */
    private static volatile String jsHoverPick = null;

    /** Relative path within the plugins folder */
    private static final boolean useNoEncrypted = false;

    public static final String HOVER_PICK_RELATIVE_PATH = "hoverPick/hoverPick.min.enc";
    public static final String HOVER_PICK_RELATIVE_PATH_MIN = "hoverPick/build/hoverPick.min.js";
    public static final String HOVER_PICK_RELATIVE_PATH_ORIG_MIN = "hoverPick/build/script-hover-pick-in-use.min.js";
    public static final String HOVER_PICK_RELATIVE_PATH_NOT_MIN = "hoverPick/build/script-hover-pick-in-use.js";
    public static final String HOVER_PICK_RELATIVE_PATH_MANUAL = "hoverPick/build/script-hover-pick-in-use-manual.js";

    /**
     * Loads (and caches) the minified hoverPick bundle from the PATH_PLUGINS folder.
     * Thread-safe via double-checked locking on jsHoverPick.
     *
     * @throws IllegalStateException if the config property or file is missing.
     * @throws RuntimeException      if the file cannot be read.
     */
    private static String getJsHoverPick() {
        if (jsHoverPick == null) {
            synchronized (PerformCloneLoad.class) {
                if (jsHoverPick == null) {
                    jsHoverPick = EncryptedPluginLoader.getInstance()
                            .loadPlugin(useNoEncrypted ? HOVER_PICK_RELATIVE_PATH_MIN : HOVER_PICK_RELATIVE_PATH);
                    log.info(
                            "PerformCloneLoad - hoverPick script loaded from plugins folder ({} chars)",
                            jsHoverPick.length());
                }
            }
        }
        return jsHoverPick;
    }

    /**
     * Clears the cached hoverPick bundle so the next injection
     * re-reads the file from disk. Call this from a "Refresh Plugins"
     * button to pick up script changes without restarting the JVM.
     */
    public static void reloadScript() {
        synchronized (PerformCloneLoad.class) {
            jsHoverPick = null;
            log.info("PerformCloneLoad - hoverPick cache cleared, will reload on next injection");
        }
    }

    // Private constructor - use getInstance()
    private PerformCloneLoad() {}

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

    /**
     * Injects the hoverPick bundle into the current browser page via
     * Selenium's JavascriptExecutor.
     *
     * Argument mapping (matches index.js IIFE parameter order):
     *   arguments[0]  hiddenFields    - boolean
     *   arguments[1]  port            - WebSocket server port
     *   arguments[2]  sessionId       - UUID string
     *   arguments[3]  destination     - target session ID for WS routing
     *   arguments[4]  operationId     - operation label string
     *   arguments[5]  homeBankingId   - int
     *   arguments[6]  botJobId        - int
     *   arguments[7]  targetOriginURL - origin for postMessage
     *   arguments[8]  trustedOriginURL - trusted origin for message validation
     *
     * @return null on success, or an ErrorMessage on failure.
     */
    public ErrorMessage dynamicPickOneCloneElementsDTO(
            WebDriver driver,
            boolean searchHiddenFields,
            int port,
            String sessionId,
            String destination,
            String operationId,
            int homeBankingId,
            int botJobId,
            String currentUrl) {
        try {
            log.info(">> Injecting plugin [hoverPick] - session={}, botJob={}", sessionId, botJobId);
            JavascriptExecutor executor = (JavascriptExecutor) driver;
            // hoverPick.min.js (and script-hover-pick-in-use.min.js) is an IIFE that ends with
            //   })( arguments[0], arguments[1], ..., arguments[8] );
            // so it expects 9 positional executeScript args, not a single ctx object.
            executor.executeScript(
                    getJsHoverPick(),
                    searchHiddenFields, // hiddenFields
                    port, // socketPort
                    sessionId, // sessionId
                    destination, // destination
                    operationId, // operationId
                    homeBankingId, // homeBankingId
                    botJobId, // botJobId
                    currentUrl, // targetOriginURL
                    currentUrl); // trustedOriginURL
            return null;
        } catch (PerformPreLoad.PluginLoadException ple) {
            log.error("PerformCloneLoad - plugin load failed: {}", ple.getUserTitle(), ple);
            return new ErrorMessage(
                    ple.getUserTitle(),
                    "Hover Pick Plugin",
                    ple.getMsg1() + "\n" + (ple.getMsg2() != null ? ple.getMsg2() : "") + "\n"
                            + (ple.getMsg3() != null ? ple.getMsg3() : ""));
        } catch (Exception error) {
            log.error("PerformCloneLoad - hoverPick injection failed: {}", error.getMessage(), error);
            return new ErrorMessage("Error running Scanner", "Dynamic Load ElementsDTO error", error.getMessage());
        }
    }
}
