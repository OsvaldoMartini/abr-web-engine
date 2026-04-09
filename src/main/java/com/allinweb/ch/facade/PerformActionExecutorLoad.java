package com.allinweb.ch.facade;

import com.allinweb.ch.util.ErrorMessage;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

/**
 * Injects the actionExecutor plugin into the browser.
 *
 * The plugin stays alive as a WebSocket listener and executes
 * DOM actions (click, type, select, ...) sent from Java - bypassing
 * all Selenium visibility / pointer-events restrictions.
 *
 * Loaded from:
 *   {path_plugins}/actionExecutor/build/actionExecutor.min.js
 *
 * To rebuild:
 *   cd {path_plugins}/actionExecutor
 *   npx esbuild index.js --bundle --minify --outfile=build/actionExecutor.min.js
 */
@Slf4j
public class PerformActionExecutorLoad {
    private static volatile PerformActionExecutorLoad instance;

    /** Cached bundle - null until first injection. */
    private static volatile String jsActionExecutor = null;

    private static final String RELATIVE_PATH = "actionExecutor/actionExecutor.min.enc";

    private PerformActionExecutorLoad() {}

    public static PerformActionExecutorLoad getInstance() {
        if (instance == null) {
            synchronized (PerformActionExecutorLoad.class) {
                if (instance == null) {
                    instance = new PerformActionExecutorLoad();
                }
            }
        }
        return instance;
    }

    private static String getScript() {
        if (jsActionExecutor == null) {
            synchronized (PerformActionExecutorLoad.class) {
                if (jsActionExecutor == null) {
                    jsActionExecutor = EncryptedPluginLoader.getInstance().loadPlugin(RELATIVE_PATH);
                    log.info("PerformActionExecutorLoad - script loaded ({} chars)", jsActionExecutor.length());
                }
            }
        }
        return jsActionExecutor;
    }

    /** Clear cache so the script reloads from disk on next injection. */
    public static void reloadScript() {
        synchronized (PerformActionExecutorLoad.class) {
            jsActionExecutor = null;
            log.info("PerformActionExecutorLoad - cache cleared");
        }
    }

    /**
     * Inject the actionExecutor into the current page.
     * Should be called once after navigation, before bot-job actions.
     *
     * Arguments match the IIFE parameter order in index.js:
     *   [0] port           - WebSocket server port
     *   [1] sessionId      - UUID string
     *   [2] destination    - target session for WS routing
     *   [3] operationId    - fixed "actionExecutor"
     *   [4] homeBankingId  - int
     *   [5] botJobId       - int
     */
    public ErrorMessage injectActionExecutor(
            WebDriver driver, int port, String sessionId, String destination, int homeBankingId, int botJobId) {
        try {
            log.info(">> Injecting plugin [actionExecutor] - session={}, botJob={}", sessionId, botJobId);
            JavascriptExecutor executor = (JavascriptExecutor) driver;
            executor.executeScript(
                    getScript(),
                    port, // arguments[0]
                    sessionId, // arguments[1]
                    destination, // arguments[2]
                    "actionExecutor", // arguments[3]
                    homeBankingId, // arguments[4]
                    botJobId); // arguments[5]
            return null;
        } catch (PerformPreLoad.PluginLoadException ple) {
            log.error("PerformActionExecutorLoad - plugin load failed: {}", ple.getUserTitle(), ple);
            return new ErrorMessage(
                    ple.getUserTitle(),
                    "Action Executor Plugin",
                    ple.getMsg1() + "\n" + (ple.getMsg2() != null ? ple.getMsg2() : "") + "\n"
                            + (ple.getMsg3() != null ? ple.getMsg3() : ""));
        } catch (Exception error) {
            log.error("PerformActionExecutorLoad - injection failed: {}", error.getMessage(), error);
            return new ErrorMessage("Error injecting Action Executor", "Action Executor error", error.getMessage());
        }
    }
}
