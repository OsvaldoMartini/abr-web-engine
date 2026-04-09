package com.allinweb.ch.facade;

import com.allinweb.ch.util.ARPropertyEnum;
import com.allinweb.ch.util.ARPropertyManager;
import com.allinweb.ch.util.ErrorMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

// SEARCH IN USE (SENDER: scannerTool) -> scannerGrid
@Slf4j
public class PerformPreLoad {

    protected static volatile PerformPreLoad instance;

    private static final ARPropertyManager arPropertyManager = ARPropertyManager.getInstance();

    /**
     * Cached scanner bundle. Null until the first call to dynamicLoadElementsDTO().
     * Loaded lazily so a missing file does NOT crash the JVM at startup -
     * the error surfaces only when a scan is actually triggered.
     *
     * Loaded from the filesystem path defined by PATH_PLUGINS in ARWeb.config:
     *   {path_plugins}/pageScanner/build/scanner.min.js
     *
     * To rebuild the bundle:
     *   cd {path_plugins}/pageScanner
     *   npx esbuild index.js --bundle --minify --outfile=build/scanner.min.js
     */
    private static volatile String jsScanner = null;

    /** Relative path within the plugins folder */
    private static final String SCANNER_RELATIVE_PATH = "pageScanner/scanner.min.enc";

    /**
     * Loads (and caches) the minified scanner bundle from the PATH_PLUGINS folder.
     * Thread-safe via double-checked locking on jsScanner.
     *
     * @throws IllegalStateException if the config property or file is missing.
     * @throws RuntimeException      if the file cannot be read.
     */
    private static String getJsScanner() {
        if (jsScanner == null) {
            synchronized (PerformPreLoad.class) {
                if (jsScanner == null) {
                    jsScanner = EncryptedPluginLoader.getInstance().loadPlugin(SCANNER_RELATIVE_PATH);
                    log.info(
                            "PerformPreLoad - scanner script loaded from plugins folder ({} chars)",
                            jsScanner.length());
                }
            }
        }
        return jsScanner;
    }

    /**
     * Clears the cached pageScanner bundle so the next injection
     * re-reads the file from disk. Call this from a "Refresh Plugins"
     * button to pick up script changes without restarting the JVM.
     */
    public static void reloadScript() {
        synchronized (PerformPreLoad.class) {
            jsScanner = null;
            log.info("PerformPreLoad - pageScanner cache cleared, will reload on next injection");
        }
    }

    /**
     * Clears ALL plugin caches (pageScanner + hoverPick + actionExecutor).
     * Convenience method for a single "Refresh Plugins" button.
     */
    public static void reloadAllPlugins() {
        reloadScript();
        PerformCloneLoad.reloadScript();
        PerformActionExecutorLoad.reloadScript();
        PerformListElements.reloadScript();
        EncryptedPluginLoader.getInstance().reloadAll();
        log.info("All plugin caches cleared - scripts will reload on next injection");
    }

    /**
     * Reads a plugin script from the filesystem, using the PATH_PLUGINS
     * config property as the base directory.
     *
     * <p>Returns the file content on success, or throws a
     * {@link PluginLoadException} with a user-friendly message describing
     * exactly what went wrong and how to fix it.</p>
     *
     * @param relativePath path relative to the plugins folder (e.g. "pageScanner/build/scanner.min.js")
     * @return the file content as a UTF-8 String
     * @throws PluginLoadException if the plugins folder or the script file cannot be resolved
     */
    static String loadPluginScript(String relativePath) {
        // ── 1. Read the raw path_plugins property ───────────────────────────
        String configured = arPropertyManager.getProperty(ARPropertyEnum.PATH_PLUGINS);
        boolean isConfigured = configured != null && !configured.isBlank();

        // ── 2. If path_plugins is not set at all → tell the user to set it ─
        if (!isConfigured) {
            log.error("loadPluginScript - path_plugins is not set in ARWeb.config");
            throw new PluginLoadException(
                    "Plugins folder is not configured",
                    "<span style='color: #E65100; font-weight: bold;'>The property 'path_plugins' is not set in ARWeb.config.</span>",
                    "<span style='font-style: italic;'>Please open Settings and set the path_plugins property pointing to your plugins folder.</span>",
                    "<span style='color: #455A64;'>Example:  path_plugins = C:\\ARWeb\\plugins</span>");
        }

        // ── 3. path_plugins is set - check the folder exists ────────────────
        Path pluginsPath = Paths.get(configured);

        if (!Files.isDirectory(pluginsPath)) {
            log.error("loadPluginScript - path_plugins folder does not exist: {}", configured);
            throw new PluginLoadException(
                    "Plugins folder does not exist",
                    "<span style='color: #E65100; font-weight: bold;'>The folder was not found on disk:</span>  "
                            + configured,
                    "<span style='font-style: italic;'>Please verify that the folder exists and contains the plugin sub-folders (pageScanner, hoverPick, etc.).</span>",
                    "<span style='color: #455A64;'>You can change it in Settings > path_plugins.</span>");
        }

        // ── 4. Check that the script file exists inside the folder ──────────
        Path scriptPath = pluginsPath.resolve(relativePath);

        if (!Files.exists(scriptPath)) {
            String pluginName =
                    relativePath.contains("/") ? relativePath.substring(0, relativePath.indexOf('/')) : relativePath;

            log.error(
                    "loadPluginScript - Plugin script not found: {}: expected at {}",
                    pluginName,
                    scriptPath.toAbsolutePath());
            throw new PluginLoadException(
                    "Plugin script not found: " + pluginName,
                    "<span style='color: #E65100; font-weight: bold;'>File not found:</span>  "
                            + scriptPath.toAbsolutePath(),
                    "<span style='font-style: italic;'>The 'path_plugins' is set to:</span>  <b>" + configured + "</b>",
                    "<span style='color: #455A64;'>Make sure the '" + pluginName
                            + "' plugin is installed in that folder and its build output exists.</span>");
        }

        // ── 5. Read the file ────────────────────────────────────────────────
        try {
            return Files.readString(scriptPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("loadPluginScript - Failed to read plugin script: {}", e.getMessage(), e);
            throw new PluginLoadException(
                    "Failed to read plugin script",
                    "<span style='color: #E65100; font-weight: bold;'>The file exists but could not be read:</span>  "
                            + scriptPath.toAbsolutePath(),
                    "<span style='font-style: italic;'>Check file permissions and ensure it is not locked by another process.</span>",
                    null,
                    e);
        }
    }

    /**
     * Exception thrown when a plugin script cannot be loaded.
     * Carries user-friendly messages suitable for display in a
     * {@code showCustomModalDialogDragWin11Timer} dialog.
     *
     * <p>The three message lines map directly to message2 / message3 / message4
     * in the dialog (message1 is the errorHeader set by the caller).</p>
     */
    static class PluginLoadException extends RuntimeException {
        private final String userTitle;
        private final String msg1;
        private final String msg2;
        private final String msg3;

        PluginLoadException(String userTitle, String msg1, String msg2, String msg3) {
            super(userTitle + " - " + msg1);
            this.userTitle = userTitle;
            this.msg1 = msg1;
            this.msg2 = msg2;
            this.msg3 = msg3;
        }

        PluginLoadException(String userTitle, String msg1, String msg2, String msg3, Throwable cause) {
            super(userTitle + " - " + msg1, cause);
            this.userTitle = userTitle;
            this.msg1 = msg1;
            this.msg2 = msg2;
            this.msg3 = msg3;
        }

        /** Short title for the dialog title bar */
        public String getUserTitle() {
            return userTitle;
        }

        /** Dialog message line 2 - what went wrong */
        public String getMsg1() {
            return msg1;
        }

        /** Dialog message line 3 - where to look / what is configured */
        public String getMsg2() {
            return msg2;
        }

        /** Dialog message line 4 - how to fix it */
        public String getMsg3() {
            return msg3;
        }
    }

    // Private constructor - use getInstance()
    private PerformPreLoad() {}

    public static PerformPreLoad getInstance() {
        if (instance == null) {
            synchronized (PerformPreLoad.class) {
                if (instance == null) {
                    instance = new PerformPreLoad();
                }
            }
        }
        return instance;
    }

    /**
     * Injects the page-scanner bundle into the current browser page via
     * Selenium's JavascriptExecutor.
     *
     * Argument mapping (matches index.js IIFE parameter order):
     *   arguments[0]  searchTerms        - String[] filter tags
     *   arguments[1]  searchHiddenFields - boolean
     *   arguments[2]  port               - WebSocket server port
     *   arguments[3]  sessionId          - UUID string
     *   arguments[4]  destination        - target session ID for WS routing
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
            log.info(">> Injecting plugin [pageScanner] - session={}, botJob={}", sessionId, botJobId);
            JavascriptExecutor executor = (JavascriptExecutor) driver;
            executor.executeScript(
                    getJsScanner(),
                    dataList, // arguments[0] - searchTerms
                    searchHiddenFields, // arguments[1] - searchHiddenFields
                    port, // arguments[2] - WS port
                    sessionId, // arguments[3] - sessionId
                    destination, // arguments[4] - destination
                    operationId, // arguments[5] - operationId
                    homeBankingId, // arguments[6] - homeBankingId
                    botJobId); // arguments[7] - botJobId
            return null;
        } catch (PluginLoadException ple) {
            log.error("PerformPreLoad - plugin load failed: {}", ple.getUserTitle(), ple);
            return new ErrorMessage(
                    ple.getUserTitle(),
                    "Page Scanner Plugin",
                    ple.getMsg1() + "\n" + (ple.getMsg2() != null ? ple.getMsg2() : "") + "\n"
                            + (ple.getMsg3() != null ? ple.getMsg3() : ""));
        } catch (Exception error) {
            log.error("PerformPreLoad - scanner injection failed: {}", error.getMessage(), error);
            return new ErrorMessage("Error running Scanner", "Dynamic Load ElementsDTO error", error.getMessage());
        }
    }
}
