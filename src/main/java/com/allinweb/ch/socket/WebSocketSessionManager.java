package com.allinweb.ch.socket;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
                log.error("Error sending message to session " + sessionId + ": " + e.getMessage());
            }
        } else {
            removeSession(sessionId);
            log.error("Session " + sessionId + " not found or closed.");
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
                log.error("Error sending message to session " + sessionId + ": " + e.getMessage());
            }
        } else {
            log.error("Session " + sessionId + " not found or closed.");
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
}
