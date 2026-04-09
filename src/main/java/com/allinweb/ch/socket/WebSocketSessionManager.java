package com.allinweb.ch.socket;

import com.allinweb.ch.model.ElementDTO;
import com.allinweb.ch.model.SplitDTO;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import javax.websocket.Session;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebSocketSessionManager {

    private static final ConcurrentHashMap<String, Session> activeSessions = new ConcurrentHashMap<>();
    protected static volatile WebSocketSessionManager instance;

    // Private constructor to prevent instantiation
    public WebSocketSessionManager() {}

    public static WebSocketSessionManager getInstance() {
        if (instance == null) {
            synchronized (WebSocketSessionManager.class) {
                if (instance == null) {
                    instance = new WebSocketSessionManager();
                }
            }
        }
        return instance;
    }

    private BiConsumer<String, String> logFn;

    public void setLogger(BiConsumer<String, String> logger) {
        this.logFn = logger;
    }

    private void appendLog(String msg, String level) {
        if (logFn != null) logFn.accept(msg, level);
    }

    public static void addSession(String sessionId, Session session) {
        activeSessions.put(sessionId, session);
    }

    public static void removeSession(String sessionId) {
        activeSessions.remove(sessionId);
    }

    public static Session getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }

    public static ConcurrentHashMap<String, Session> getAllSessions() {
        return activeSessions;
    }

    public static boolean containsSession(String sessionId) {
        return activeSessions.containsKey(sessionId);
    }

    public static boolean isSessionOpen(String sessionId) {
        Session session = activeSessions.get(sessionId);
        return session != null && session.isOpen();
    }

    public static void sendMessageJson(
            int homeBankingId, Session session, String sessionId, String body, String operationId) {
        if (session != null && session.isOpen()) {
            try {
                JsonObject jsonMessage = new JsonObject();
                jsonMessage.addProperty("homeBankingId", homeBankingId);
                jsonMessage.addProperty("sessionId", sessionId);
                jsonMessage.addProperty("body", body);
                if (operationId != null && !operationId.isEmpty()) {
                    jsonMessage.addProperty("operationId", operationId);
                }
                session.getBasicRemote().sendText(jsonMessage.toString());
            } catch (IOException e) {
                log.error("Error sending message to session " + sessionId + ": " + e.getMessage());
            }
        } else {
            log.error("Session " + sessionId + " not found or closed.");
        }
    }

    // Method to get the session ID based on the session object
    public String getSessionIdBySession(Session session) {
        return activeSessions.entrySet().stream()
                .filter(entry -> entry.getValue().equals(session))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public void broadcastMessageToAll(int homeBankingId, String message) {
        for (Session session : getAllSessions().values()) { // Looping correctly
            if (session.isOpen()) {
                sendMessageJson(homeBankingId, session, "Broad-All", message, null);
            }
        }
    }

    public void broadcastMessageToAll(int homeBankingId, String broadTo, String body, String operationId) {
        for (Map.Entry<String, Session> entry : activeSessions.entrySet()) {
            String sessionKey = entry.getKey();
            Session session = entry.getValue();

            // Ensure session is open before sending
            if (session.isOpen() && sessionKey.contains(broadTo)) {
                try {
                    sendMessageJson(homeBankingId, session, entry.getKey(), body, operationId);
                } catch (Exception e) {
                    log.error("Failed to send message to session: " + sessionKey);
                    e.printStackTrace();
                }
            }
        }
    }

    // Method to send a message to a specific session ID
    public void sendMessageJson(String sessionId, String message) {
        Session session = getAllSessions().get(sessionId);

        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                log.debug("Cannot send to session {}: {}", sessionId, e.getMessage());
            }
        } else {
            // ARWeb may not be running - silently skip
            log.debug("Session {} not available - ARWeb may be offline.", sessionId);
        }
    }

    // Method to send a message to a specific session ID
    public Session sendMessageJson(int homeBankingId, String sessionId, String body, String operationId) {
        Session session = getAllSessions().get(sessionId);

        if (session != null && session.isOpen()) {
            try {
                JsonObject jsonMessage = new JsonObject();
                jsonMessage.addProperty("body", body);
                jsonMessage.addProperty("sessionId", sessionId);
                jsonMessage.addProperty("homeBankingId", homeBankingId);
                if (operationId != null && !operationId.isEmpty()) {
                    jsonMessage.addProperty("operationId", operationId);
                }
                session.getBasicRemote().sendText(jsonMessage.toString());
                return session;
            } catch (IOException e) {
                log.debug("Cannot send to session {}: {}", sessionId, e.getMessage());
            }
        } else {
            // ARWeb may not be running - silently skip
            log.debug("Session {} not available - ARWeb may be offline.", sessionId);
        }
        return null;
    }

    //    public static void sendMessageJson(int homeBankingId, String sessionId, String msg1, String msg2) {
    //        Session session = getAllSessions().get(sessionId);
    //
    //        if (session != null && session.isOpen()) {
    //            try {
    //                JsonObject jsonMessage = new JsonObject();
    //                jsonMessage.addProperty("body", msg1);
    //                jsonMessage.addProperty("sessionId", sessionId);
    //                jsonMessage.addProperty("homeBankingId", homeBankingId);
    //                if (msg2 != null && !msg2.isEmpty()) {
    //                    jsonMessage.addProperty("operationId", msg2);
    //                }
    //                session.getBasicRemote().sendText(jsonMessage.toString());
    //            } catch (IOException e) {
    //                log.error("Error sending message to session " + sessionId + ": " + e.getMessage());
    //            }
    //        } else {
    //            log.error("Session " + sessionId + " not found or closed.");
    //        }
    //    }

    private void sendMessageJson(Session session, String msg1, String msg2) {
        if (session != null && session.isOpen()) {
            try {
                // Create a JSON object with the key "body" and the provided message
                JsonObject jsonMessage = new JsonObject();
                jsonMessage.addProperty("body", msg1);
                if (!Strings.isNullOrEmpty(msg2)) {
                    jsonMessage.addProperty("footer", msg2);
                }
                // Convert the JSON object to a string
                String jsonString = jsonMessage.toString();

                // Send the JSON string over WebSocket
                session.getBasicRemote().sendText(jsonString);
            } catch (IOException e) {
                log.error("Error sending message to session " + session.getId() + ": " + e.getMessage());
            }
        }
    }

    public void sendChunks(
            List<ElementDTO> elements, int chunkSize, SplitDTO splitDTO, String server, String routingKey) {
        if (elements == null || elements.isEmpty()) {
            appendLog("No elements to send.", "warn");
            return;
        }

        appendLog("Sending " + elements.size() + " elements in chunks of " + chunkSize, "info");

        for (int i = 0; i < elements.size(); i += chunkSize) {

            int end = Math.min(i + chunkSize, elements.size());
            List<ElementDTO> chunk = elements.subList(i, end);

            // update DTO
            splitDTO.setElementDetails(chunk.toArray(new ElementDTO[0]));

            // serialize
            String jsonData = new Gson().toJson(splitDTO);

            // log
            appendLog("Sending chunk " + (i / chunkSize + 1) + " containing " + chunk.size() + " elements", "info");

            // send
            sendMessageJson(0, server, jsonData, routingKey);
        }
    }
}
