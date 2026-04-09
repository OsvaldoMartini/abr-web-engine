package com.allinweb.ch.facade;

import com.allinweb.ch.util.ErrorMessage;
import com.allinweb.ch.util.JsScanResultDTO;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// SEARCH IN USE (SENDER: scannerTool) -> UPDATE_LIST_ELEMENTS_ASYNC
@Slf4j
public class PerformListElements {
    private static final Logger logOperations = LoggerFactory.getLogger("com.allinweb.operations");
    private Gson gson = new GsonBuilder().create();

    private static final PerformLists performLists = PerformLists.getInstance();

    protected static volatile PerformListElements instance;

    /** Flag to log only the first searchListAsync injection per bot-job run. */
    private volatile boolean loggedFirstCall = false;

    /**
     * Reset the first-call log flag. Call this at the start of each bot-job execution
     * so the first searchListAsync injection is logged again.
     */
    public void resetFirstCallLog() {
        this.loggedFirstCall = false;
    }

    /**
     * Cached searchListAsync bundle. Null until the first call to dynamicLoadElementsDTO().
     * Loaded lazily so a missing file does NOT crash the JVM at startup -
     * the error surfaces only when a scan is actually triggered.
     *
     * Loaded from the filesystem path defined by PATH_PLUGINS in ARWeb.config:
     *   {path_plugins}/searchListAsync/build/searchListAsync.min.js
     *
     * To rebuild the bundle:
     *   cd {path_plugins}/searchListAsync
     *   npx esbuild index.js --bundle --minify --outfile=build/searchListAsync.min.js
     */
    private static volatile String jsSearchListAsync = null;

    /** Relative path within the plugins folder */
    private static final String SEARCH_LIST_ASYNC_RELATIVE_PATH = "searchListAsync/searchListAsync.min.enc";

    /**
     * Loads (and caches) the minified searchListAsync bundle from the PATH_PLUGINS folder.
     * Thread-safe via double-checked locking on jsSearchListAsync.
     *
     * @throws PerformPreLoad.PluginLoadException if the config property or file is missing.
     */
    private static String getJsSearchListAsync() {
        if (jsSearchListAsync == null) {
            synchronized (PerformListElements.class) {
                if (jsSearchListAsync == null) {
                    jsSearchListAsync = EncryptedPluginLoader.getInstance().loadPlugin(SEARCH_LIST_ASYNC_RELATIVE_PATH);
                    log.info(
                            "PerformListElements - searchListAsync script loaded from plugins folder ({} chars)",
                            jsSearchListAsync.length());
                }
            }
        }
        return jsSearchListAsync;
    }

    /** Clear cache so the script reloads from disk on next injection. */
    public static void reloadScript() {
        synchronized (PerformListElements.class) {
            jsSearchListAsync = null;
            log.info("PerformListElements - searchListAsync cache cleared");
        }
    }

    // Private constructor to prevent instantiation
    private PerformListElements() {}

    public static PerformListElements getInstance() {
        if (instance == null) {
            synchronized (PerformListElements.class) {
                if (instance == null) {
                    instance = new PerformListElements();
                }
            }
        }
        return instance;
    }

    /**
     * Injects the searchListAsync bundle into the current browser page via
     * Selenium's executeAsyncScript.
     *
     * Argument mapping (matches index.js IIFE parameter order):
     *   arguments[0]  searchTerms        - String[] filter tags
     *   arguments[1]  searchHiddenFields - boolean
     *   arguments[2]  port               - WebSocket server port (unused, kept for alignment)
     *   arguments[3]  sessionId          - UUID string
     *   arguments[4]  destination        - target session ID for routing
     *   arguments[5]  operationId        - operation label string
     *   arguments[6]  homeBankingId      - int
     *   arguments[7]  botJobId           - int
     *
     * @return null on success, or an ErrorMessage on failure.
     */
    public ErrorMessage dynamicLoadElementsDTO(
            WebDriver driver,
            String[] dataArray,
            boolean searchHiddenFields,
            int port,
            String sessionId,
            String destination,
            String operationId,
            int homeBankingId,
            int botJobId) {

        List<String> dataList = Arrays.asList(dataArray);
        try {
            if (!loggedFirstCall) {
                log.info(">> Injecting plugin [searchListAsync] - session={}, botJob={}", sessionId, botJobId);
                loggedFirstCall = true;
            }

            JavascriptExecutor executor = (JavascriptExecutor) driver;

            driver.manage().timeouts().setScriptTimeout(java.time.Duration.ofSeconds(25));

            Object result = executor.executeAsyncScript(
                    getJsSearchListAsync(),
                    dataList, // arguments[0] - searchTerms
                    searchHiddenFields, // arguments[1] - hiddenFields
                    port, // arguments[2] - socketPort (unused)
                    sessionId, // arguments[3] - sessionId
                    destination, // arguments[4] - destination
                    operationId, // arguments[5] - operationId
                    homeBankingId, // arguments[6] - homeBankingId
                    botJobId); // arguments[7] - botJobId

            if (result == null || !(result instanceof String)) {
                logOperations.warn("Cannot return any elements from the page");
                return new ErrorMessage(
                        "Dynamic Scanner Web Page",
                        "Dynamic Load ElementsDTO error",
                        "Cannot return any elements from the page");
            }

            logOperations.info("JS async result: {}", result);

            String jsonScript = String.valueOf(result);

            JsScanResultDTO dto = gson.fromJson(jsonScript, JsScanResultDTO.class);

            // Replace current list and load new one
            performLists.resetListElements();
            performLists.addMapElementsTarget(dto.getElements());

            return null;
        } catch (PerformPreLoad.PluginLoadException ple) {
            log.error("PerformListElements - plugin load failed: {}", ple.getUserTitle(), ple);
            return new ErrorMessage(
                    ple.getUserTitle(),
                    "Search List Async Plugin",
                    ple.getMsg1() + "\n" + (ple.getMsg2() != null ? ple.getMsg2() : "") + "\n"
                            + (ple.getMsg3() != null ? ple.getMsg3() : ""));
        } catch (Exception error) {
            return new ErrorMessage("Error running Scanner", "Dynamic Load ElementsDTO error", error.getMessage());
        }
    }
}
