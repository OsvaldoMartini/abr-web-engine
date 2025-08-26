package com.allinweb.ch.facade;

import com.allinweb.ch.component.model.*;
import com.allinweb.ch.persistence.DatabaseUserDTO;
import com.allinweb.ch.persistence.ReferenceDTO;
import com.allinweb.ch.socket.WebSocketSessionManager;
import com.allinweb.ch.util.ARPropertyManager;
import com.allinweb.ch.util.ComboBoxVars;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javax.websocket.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ClientEndpoint
public class PerformLists {

    // Static final variable to hold the singleton instance
    protected static volatile PerformLists instance;

    // Private constructor to prevent instantiation
    private PerformLists() {

        initialize();
    }

    // WebSocket needs
    private static final CountDownLatch latch = new CountDownLatch(1);
    private Session session;
    private ExecutorService executorWebSocket;
    private int portSocketInitial = 54525;
    private boolean isConnectWebSocket = false;
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Gson gson = new Gson();
    // Lists for tables
    private static final ARPropertyManager arPropertyManager = ARPropertyManager.getInstance();
    private static final WebSocketSessionManager webSocketSessionManager = WebSocketSessionManager.getInstance();

    // Public method to access the singleton instance
    public static PerformLists getInstance() {
        if (instance == null) {
            synchronized (PerformLists.class) {
                if (instance == null) {
                    instance = new PerformLists();
                }
            }
        }
        return instance;
    }

    private ObservableList<HomeBankingLoadDTO> listHomeBanking = FXCollections.observableArrayList();
    private ObservableList<HomeUrlDTO> listHomeUrl = FXCollections.observableArrayList();
    private ObservableList<BotJobLoadDTO> quickBotJobs = FXCollections.observableArrayList();
    private ObservableList<BotJobLoadDTO> listBotJob = FXCollections.observableArrayList();
    private List<BotJobLoadDTO> listBotJobComp = FXCollections.observableArrayList();
    private List<BlockLoadDTO> listBlock = new ArrayList<>();
    private List<BlockLoadDTO> listBlockComp = new ArrayList<>();
    private ObservableList<InstructionLoadDTO> listInstruction = FXCollections.observableArrayList();
    private ObservableList<InstructionLoadDTO> listInstructionComp = FXCollections.observableArrayList();
    private List<VariableLoadDTO> listVariable = new ArrayList<>();
    private List<VariableLoadDTO> listVariableComp = new ArrayList<>();
    private List<ReferenceDTO> listReference = new ArrayList<>();
    private List<ReferenceDTO> listReferenceComp = new ArrayList<>();

    // Quick Lists
    private List<InstructionOperationDTO> instrucOperList = new ArrayList<>();

    // Observable lists
    private ObservableList<DatabaseUserDTO> listDatabaseUsers = FXCollections.observableArrayList();
    private ObservableList<VariableUserDTO> listVariablesUser = FXCollections.observableArrayList();
    private ObservableList<ComboBoxVars> listWebPageItems = FXCollections.observableArrayList();

    public void initialize() {
        this.executorWebSocket = Executors.newSingleThreadExecutor();

        String port =
                System.getProperty("ARWebChosenPort"); // arPropertyManager.getProperty(ARPropertyEnum.PORT_SOCKET);
        if (!Strings.isNullOrEmpty(port)) {
            portSocketInitial = Integer.parseInt(port);
        }

        if (!isConnectWebSocket) {
            connectWebSocketClient(portSocketInitial, "perform-list-data");
        }
    }

    // WebSocket Controls
    //    private static final ScheduledExecutorService pingScheduler = Executors.newScheduledThreadPool(1);

    private void stopKeepAlivePings() {
        scheduler.shutdownNow();
    }

    private void startKeepAlivePings() {
        scheduler.scheduleAtFixedRate(
                () -> {
                    try {
                        if (session != null && session.isOpen()) {
                            session.getBasicRemote()
                                    .sendText("ping-perform-list-data"); // Or a specific keep-alive message
                        }
                    } catch (IOException e) {
                        System.err.println("Error sending ping: " + e.getMessage());
                        // Handle potential disconnection
                    }
                },
                0,
                15,
                TimeUnit.SECONDS); // Adjust interval as needed
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        latch.countDown(); // Release the latch after connection is established
        System.out.println("Connected to WebSocket server at: " + session.getRequestURI());
        // Sending an initial message
        sendMessage("Hello from JavaFX WebSocket client!");
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("Connection closed.");
        stopKeepAlivePings();
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.out.println("Error: " + throwable.getMessage());
        stopKeepAlivePings();
    }

    // Method to send a message
    public void sendMessage(String message) {
        executorWebSocket.submit(() -> {
            //            if (session != null && session.isOpen()) {
            //                try {
            //                    session.getBasicRemote().sendText(message);
            //                } catch (Exception e) {
            //                    e.printStackTrace();
            //                }
            //            }
        });
    }

    public void connectWebSocketClient(int portSocket, String sessionId) {
        executorWebSocket.submit(() -> {
            String serverUri = "ws://localhost:" + portSocket + "/websocket?sessionId=" + sessionId;
            try {
                WebSocketContainer container = ContainerProvider.getWebSocketContainer();
                container.connectToServer(this, new URI(serverUri));
                latch.await();
                startKeepAlivePings();
                isConnectWebSocket = true;
            } catch (Exception e) {
                isConnectWebSocket = false;
                System.err.println("WebSocket connection failed sessionId: " + sessionId + " error: " + e.getMessage());
            }
        });
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println("Received: " + message);
        if (message == null || message.contains("CONNECT") || message.contains("ping")) {
            // Ignore null, CONNECT, or ping messages
            message = message.replaceAll("ping-", "");
            System.out.println("Active : " + message);
            return;
        }

        int homeBankingId = -1;
        String sessionId = null;
        String type = "unknown";
        String body = null;

        try {
            // Parse the incoming message (assuming JSON format)
            JsonObject jsonObjMSG = JsonParser.parseString(message).getAsJsonObject();

            // Extract homeBankingId
            if (jsonObjMSG.has("homeBankingId")) {
                homeBankingId = jsonObjMSG.get("homeBankingId").getAsInt();
            }

            // Extract body
            body = jsonObjMSG.has("body") ? jsonObjMSG.get("body").getAsString() : "unknown";

            // Determine type (priority: body.type ? json.type ? operationId)
            if (!"unknown".equalsIgnoreCase(body)) {
                try {
                    JsonObject objSecond = JsonParser.parseString(body).getAsJsonObject();
                    if (objSecond.has("type")) {
                        type = objSecond.get("type").getAsString();
                    }
                } catch (Exception ignore) {

                }
            }

            if ("unknown".equals(type) && jsonObjMSG.has("type")) {
                type = jsonObjMSG.get("type").getAsString();
            }

            if ("unknown".equals(type) && jsonObjMSG.has("operationId")) {
                type = jsonObjMSG.get("operationId").getAsString();
            }

            // Extract sessionId
            sessionId =
                    jsonObjMSG.has("sessionId") ? jsonObjMSG.get("sessionId").getAsString() : null;

            // Debug print (optional)
            System.out.printf(
                    "homeBankingId=%d, sessionId=%s, type=%s, body=%s%n", homeBankingId, sessionId, type, body);
            // After Decoding
            if (type == null || type.trim().isEmpty() || type.contains("CONNECT") || type.contains("ping")) {
                // Ignore null or empty messages
                type = type.replaceAll("ping-", "");
                System.out.println("Active : " + type);
                return;
            }
            // After Decoding
            if (type == null || type.trim().isEmpty() || type.contains("CONNECT") || type.contains("ping")) {
                // Ignore null or empty messages
                type = type.replaceAll("ping-", "");
                System.out.println("Active : " + type);
                return;
            }

            // Process the message based on its type
            switch (type) {
                case "UPDATE_BLOCKS":
                    BlockMoveDTO blockMoveDTO = gson.fromJson(body, BlockMoveDTO.class);
                    blockMoveDTO.setType("UPDATE_BLOCKS");

                    String jsonData = gson.toJson(blockMoveDTO);
                    // Just a Signal to update the combos
                    webSocketSessionManager.sendMessageJson(
                            homeBankingId, "new-command-scene", jsonData, "UPDATE_BLOCKS");

                    webSocketSessionManager.sendMessageJson(
                            homeBankingId, "scanner-element-pane", jsonData, "UPDATE_BLOCKS");

                    break;
                case "UPDATE_BLOCKS_COMP":
                    blockMoveDTO = gson.fromJson(jsonObjMSG, BlockMoveDTO.class);
                    blockMoveDTO.setType("UPDATE_BLOCKS_COMP");

                    jsonData = gson.toJson(blockMoveDTO);
                    // Just a Signal to update the combos
                    webSocketSessionManager.sendMessageJson(
                            homeBankingId, "new-command-scene", jsonData, "UPDATE_BLOCKS_COMP");
                    break;
                case "UPDATE_BOT_JOBS":
                    jsonData = gson.toJson("[]");
                    // Just a Signal to update the combos
                    webSocketSessionManager.sendMessageJson(homeBankingId, "main-pane", jsonData, "UPDATE_JOBS");
                    break;
                default:
                    break;
            }
        } catch (Exception error) {
            if (error.getMessage().contains("invalid session id")) {
                //                performMessage.errorMessage(
                //                        "Browser is Closed",
                //                        "<span style='color: #2E7D32; font-weight: bold; font-size: 1.1em;'>To perform
                // this action, please</span> ?",
                //                        "<span style='color: #1976D2;'>reopen the browser via the Scanner:</span>",
                //                        "<span style='font-weight: bold;'>Click the \"Scanner\" button in the previous
                // window</span>",
                //                        null,
                //                        0);
            }

            System.err.println("Closed processing message: " + error.getMessage());
        }
    }

    public void destroy() {
        instance = null;
    }

    // Methods
    public List<HomeUrlDTO> getHomeUrlsByBankId(Integer homeBankingId) {
        return getListHomeUrl().stream()
                .filter(dto ->
                        dto.getHomeBankingId() != null && dto.getHomeBankingId().equals(homeBankingId))
                .toList(); // Java 16+; use .collect(Collectors.toList()) for older versions
    }

    // Get HomeBankingLoadDTO by homeBankingId
    public HomeBankingLoadDTO getHomeBankingById(Integer homeBankingId) {
        return getListHomeBanking().stream()
                .filter(hb -> Objects.equals(hb.getId(), homeBankingId))
                .findFirst()
                .orElse(null); // null if not found
    }

    // Get the first HomeBankingLoadDTO from the list
    public HomeBankingLoadDTO getFirstHomeBanking() {
        return getListHomeBanking().stream().findFirst().orElse(null); // null if the list is empty
    }

    // Get HomeUrlDTO by homeBankingId and homeUrlId
    public HomeUrlDTO getHomeUrlByBankId(Integer homeBankingId, Integer homeUrlId) {
        return getListHomeUrl().stream()
                .filter(url ->
                        Objects.equals(url.getHomeBankingId(), homeBankingId) && Objects.equals(url.getId(), homeUrlId))
                .findFirst()
                .orElse(null); // null if not found
    }

    // Get BotJobLoadDTO by botJobId
    public BotJobLoadDTO getQuickBotJobById(Integer botJobId) {
        return getQuickBotJobs().stream()
                .filter(job -> Objects.equals(job.getId(), botJobId))
                .findFirst()
                .orElse(null); // null if not found
    }
}
