package com.allinweb.ch.facade;

import com.allinweb.ch.socket.WebSocketSessionManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.UUID;
import java.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;

/**
 * Java-side client for the actionExecutor browser plugin.
 *
 * Sends action commands (click, type, select, ...) to the injected
 * JavaScript via WebSocket, and waits for the result.
 *
 * Usage:
 *   ActionExecutorClient client = ActionExecutorClient.getInstance();
 *   client.configure(homeBankingId, sessionId);
 *   ActionResult result = client.click(xPath, cssSelector, coordinates, attribId);
 *   if (result.isSuccess()) { ... }
 */
@Slf4j
public class ActionExecutorClient {
    private static volatile ActionExecutorClient instance;

    private final WebSocketSessionManager wsManager = WebSocketSessionManager.getInstance();
    private final Gson gson = new Gson();

    /** Pending results keyed by commandId */
    private final ConcurrentHashMap<String, CompletableFuture<ActionResult>> pendingResults = new ConcurrentHashMap<>();

    private int homeBankingId;
    private String sessionId; // the browser's WS session ID

    /** Default timeout for waiting on a command result */
    private static final int DEFAULT_TIMEOUT_SECONDS = 15;

    private ActionExecutorClient() {}

    public static ActionExecutorClient getInstance() {
        if (instance == null) {
            synchronized (ActionExecutorClient.class) {
                if (instance == null) {
                    instance = new ActionExecutorClient();
                }
            }
        }
        return instance;
    }

    /**
     * Configure the client with the target browser session.
     * Call after injecting the actionExecutor plugin.
     */
    public void configure(int homeBankingId, String sessionId) {
        // Cancel any pending futures from the previous session (page was reloaded)
        if (!pendingResults.isEmpty()) {
            log.info(
                    "ActionExecutorClient — clearing {} stale pending results from previous session",
                    pendingResults.size());
            pendingResults.forEach(
                    (id, future) -> future.complete(new ActionResult(false, "session reset — page reloaded")));
            pendingResults.clear();
        }
        this.homeBankingId = homeBankingId;
        this.sessionId = sessionId;
    }

    // ── Result handling (called by SimpleWebSocketServer) ───────────────────

    /**
     * Called by the WebSocket server when an ACTION_EXECUTOR result arrives.
     * Completes the pending future so the calling thread unblocks.
     */
    public void onResult(JsonObject jsonMessage) {
        try {
            String commandId =
                    jsonMessage.has("commandId") ? jsonMessage.get("commandId").getAsString() : null;

            JsonObject result =
                    jsonMessage.has("result") ? jsonMessage.get("result").getAsJsonObject() : null;

            if (commandId != null && result != null) {
                boolean success = result.has("success") && result.get("success").getAsBoolean();
                String message = result.has("message") ? result.get("message").getAsString() : "";
                boolean verified =
                        result.has("verified") && result.get("verified").getAsBoolean();

                // Log before/after state for debugging
                if (result.has("before") && result.has("after")) {
                    log.info("ActionExecutorClient — commandId={}, verified={}", commandId, verified);
                    log.info("  BEFORE: {}", result.get("before"));
                    log.info("  AFTER:  {}", result.get("after"));
                }

                CompletableFuture<ActionResult> future = pendingResults.remove(commandId);
                if (future != null) {
                    future.complete(new ActionResult(success, verified, message));
                } else {
                    log.warn("ActionExecutorClient — no pending future for commandId={}", commandId);
                }
            }
        } catch (Exception e) {
            log.error("ActionExecutorClient — error processing result: {}", e.getMessage(), e);
        }
    }

    // ── Command senders ─────────────────────────────────────────────────────

    public ActionResult click(String xPath, String cssSelector, String coordinates, String attribId) {
        return sendCommand("click", xPath, cssSelector, coordinates, attribId, null);
    }

    public ActionResult type(String xPath, String cssSelector, String coordinates, String attribId, String value) {
        return sendCommand("type", xPath, cssSelector, coordinates, attribId, value);
    }

    public ActionResult clear(String xPath, String cssSelector, String coordinates, String attribId) {
        return sendCommand("clear", xPath, cssSelector, coordinates, attribId, null);
    }

    public ActionResult select(String xPath, String cssSelector, String coordinates, String attribId, String value) {
        return sendCommand("select", xPath, cssSelector, coordinates, attribId, value);
    }

    public ActionResult focus(String xPath, String cssSelector, String coordinates, String attribId) {
        return sendCommand("focus", xPath, cssSelector, coordinates, attribId, null);
    }

    public ActionResult scroll(String xPath, String cssSelector, String coordinates, String attribId) {
        return sendCommand("scroll", xPath, cssSelector, coordinates, attribId, null);
    }

    public ActionResult getValue(String xPath, String cssSelector, String coordinates, String attribId) {
        return sendCommand("getValue", xPath, cssSelector, coordinates, attribId, null);
    }

    public ActionResult check(String xPath, String cssSelector, String coordinates, String attribId) {
        return sendCommand("check", xPath, cssSelector, coordinates, attribId, null);
    }

    public ActionResult uncheck(String xPath, String cssSelector, String coordinates, String attribId) {
        return sendCommand("uncheck", xPath, cssSelector, coordinates, attribId, null);
    }

    /**
     * Generic action sender — used by performWebActions fallback.
     */
    public ActionResult sendAction(
            String action, String xPath, String cssSelector, String coordinates, String attribId, String value) {
        return sendCommand(action, xPath, cssSelector, coordinates, attribId, value);
    }

    // ── Core send + wait ────────────────────────────────────────────────────

    private ActionResult sendCommand(
            String action, String xPath, String cssSelector, String coordinates, String attribId, String value) {

        String commandId = UUID.randomUUID().toString();
        CompletableFuture<ActionResult> future = new CompletableFuture<>();
        pendingResults.put(commandId, future);

        try {
            JsonObject command = new JsonObject();
            command.addProperty("operationId", "actionExecutor");
            command.addProperty("sessionId", sessionId);
            command.addProperty("action", action);
            command.addProperty("commandId", commandId);
            if (xPath != null) command.addProperty("xPath", xPath);
            if (cssSelector != null) command.addProperty("cssSelector", cssSelector);
            if (coordinates != null) command.addProperty("coordinates", coordinates);
            if (attribId != null) command.addProperty("attribId", attribId);
            if (value != null) command.addProperty("value", value);

            String body = gson.toJson(command);

            log.info("ActionExecutorClient — sending {} to session={}, commandId={}", action, sessionId, commandId);

            wsManager.sendMessageJson(homeBankingId, sessionId, body, "actionExecutor");

            // Wait for the result with timeout
            ActionResult result = future.get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info(
                    "ActionExecutorClient — {} result: success={}, message={}",
                    action,
                    result.isSuccess(),
                    result.getMessage());
            return result;

        } catch (TimeoutException e) {
            pendingResults.remove(commandId);
            log.error("ActionExecutorClient — {} timed out after {}s", action, DEFAULT_TIMEOUT_SECONDS);
            return new ActionResult(false, action + " timed out after " + DEFAULT_TIMEOUT_SECONDS + "s");
        } catch (Exception e) {
            pendingResults.remove(commandId);
            log.error("ActionExecutorClient — {} failed: {}", action, e.getMessage(), e);
            return new ActionResult(false, action + " failed: " + e.getMessage());
        }
    }

    // ── Result record ───────────────────────────────────────────────────────

    public static class ActionResult {
        private final boolean success;
        private final boolean verified;
        private final String message;

        public ActionResult(boolean success, String message) {
            this.success = success;
            this.verified = false;
            this.message = message;
        }

        public ActionResult(boolean success, boolean verified, String message) {
            this.success = success;
            this.verified = verified;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }
        /** True if the element's state actually changed after the action. */
        public boolean isVerified() {
            return verified;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "ActionResult{success=" + success + ", message='" + message + "'}";
        }
    }
}
