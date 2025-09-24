package com.allinweb.ch.facade;

import com.allinweb.ch.component.model.*;
import com.allinweb.ch.socket.WebSocketSessionManager;
import com.allinweb.ch.util.ARPropertyManager;
import com.allinweb.ch.util.ComboBoxVars;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import javax.websocket.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@ClientEndpoint
@Slf4j
public class PerformLists {

    // WebSocket needs
    private static final CountDownLatch latch = new CountDownLatch(1);
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    // Lists for tables
    private static final ARPropertyManager arPropertyManager = ARPropertyManager.getInstance();
    private static final WebSocketSessionManager webSocketSessionManager = WebSocketSessionManager.getInstance();
    // Static final variable to hold the singleton instance
    protected static volatile PerformLists instance;
    private final Gson gson = new Gson();
    private Session session;
    private ExecutorService executorWebSocket;
    private int portSocketInitial = 54525;
    private boolean isConnectWebSocket = false;
    private List<HomeBankingLoadDTO> listHomeBanking = new ArrayList<>();
    private List<HomeUrlDTO> listHomeUrl = new ArrayList<>();
    private List<BotJobLoadDTO> quickBotJobs = new ArrayList<>();
    private List<BotJobLoadDTO> listBotJob = new ArrayList<>();
    private List<BotJobLoadDTO> listBotJobComp = new ArrayList<>();
    private List<BlockLoadDTO> listBlock = new ArrayList<>();
    private List<BlockLoadDTO> listBlockComp = new ArrayList<>();
    private List<InstructionLoad> listInstruction = new ArrayList<>();
    private List<InstructionLoad> listInstructionComp = new ArrayList<>();
    private List<VariableLoadDTO> listVariable = new ArrayList<>();
    private List<VariableLoadDTO> listVariableComp = new ArrayList<>();
    private List<ReferenceLoadDTO> listReference = new ArrayList<>();
    private List<ReferenceLoadDTO> listReferenceComp = new ArrayList<>();
    private List<String> allActions = new ArrayList<>();
    // Quick Lists
    private List<InstructionOperationDTO> instrucOperList = new ArrayList<>();
    private List<DatabaseUserDTO> listDatabaseUsers = new ArrayList<>();
    private List<VariableUserDTO> listVariablesUser = new ArrayList<>();
    private List<ComboBoxVars> listWebPageItems = new ArrayList<>();
    private List<ParentOperations> listParentOperations = new ArrayList<>();

    // Private constructor to prevent instantiation
    private PerformLists() {}

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

    //    private List<BlockOptions> listComboOptions = new ArrayList<>();

    public void initialize(String sessionId) {
        this.executorWebSocket = Executors.newSingleThreadExecutor();

        String port =
                System.getProperty("ARWebChosenPort"); // arPropertyManager.getProperty(ARPropertyEnum.PORT_SOCKET);
        if (!Strings.isNullOrEmpty(port)) {
            try {
                portSocketInitial = Integer.parseInt(port);
            } catch (Exception error) {
                log.error("Pot Socket wrong Format: {}", port);
            }
        }

        if (!isConnectWebSocket) {
            connectWebSocketClient(portSocketInitial, sessionId);
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
                        log.error("Error sending ping: " + e.getMessage());
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
        log.info("Connected to WebSocket server at: " + session.getRequestURI());
        // Sending an initial message
        sendMessage("Hello from JavaFX WebSocket client!");

        String sessionId = null;
        try {
            sessionId = session.getRequestParameterMap().get("sessionId").get(0);

            // Add the Session to be Possiboe to send messages from Clients via PerformLists
            if (!Strings.isNullOrEmpty(sessionId) && sessionId.equals("engine-perform-bot-job")) {
                webSocketSessionManager.addSession(sessionId, session);
            } else {
                //                addSession(generateCustomSessionId(session), session);
            }
        } catch (Exception noSessionId) {
            //            addSession(generateCustomSessionId(session), session);
        }
    }

    @OnClose
    public void onClose(Session session) {
        log.info("Connection closed.");
        stopKeepAlivePings();
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        log.info("Error: " + throwable.getMessage());
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
                log.warn("WebSocket connection failed sessionId: " + sessionId + " error: " + e.getMessage());
            }
        });
    }

    @OnMessage
    public void onMessage(String message) {
        log.info("Received: " + message);
        if (message == null || message.contains("CONNECT") || message.contains("ping")) {
            // Ignore null, CONNECT, or ping messages
            message = message.replaceAll("ping-", "");
            // log.info("Active : " + message);
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
            log.info("homeBankingId={}, sessionId={}, type={}, body={}", homeBankingId, sessionId, type, body);
            // After Decoding
            if (type == null || type.trim().isEmpty() || type.contains("CONNECT") || type.contains("ping")) {
                // Ignore null or empty messages
                type = type.replaceAll("ping-", "");
                // log.info("Active : " + type);
                return;
            }
            // After Decoding
            if (type == null || type.trim().isEmpty() || type.contains("CONNECT") || type.contains("ping")) {
                // Ignore null or empty messages
                type = type.replaceAll("ping-", "");
                // log.info("Active : " + type);
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
                log.error("Browser is Closed");
                //                performMessage.errorMessage(
                //                        "Browser is Closed",
                //                        "<span style='color: #2E7D32; font-weight: bold; font-size: 1.1em;'>To perform
                // this action, please</span> ?",
                //                        "<span style='color: #1976D2;'>reopen the browser via the Scanner:</span>",
                //                        "<span style='font-weight: bold;'>Click the \"Scanner\" button in the previous
                // window</span>",
                //                        null,
                //                        0);
            } else {
                log.error("Closed processing message: " + error.getMessage());
            }
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

    public InstructionLoad getInstructionById(String tableName, int whereId, int instructionId) {
        List<InstructionLoad> targetList = "instruction".equals(tableName) ? listInstruction : listInstructionComp;

        return targetList.stream()
                .filter(instr -> Objects.equals(instr.getId(), instructionId)
                        && ("instruction".equals(tableName)
                                ? Objects.equals(instr.getBotJobId(), whereId)
                                : Objects.equals(instr.getHomeBankingId(), whereId)))
                .findFirst()
                .orElse(null); // returns null if not found
    }

    //    public String getParentName(InstructionLoad instructionLoad) {
    //        if (instructionLoad == null
    //                || instructionLoad.getActions() == null
    //                || instructionLoad.getActions().isEmpty()) {
    //            return "Parent Name";
    //        }
    //
    //        String[] parts = instructionLoad.getActions().split(":");
    //        return parts.length > 0 ? parts[parts.length - 1].trim() : "Parent Name";
    //    }

    // Get BlockLoadDTO by homeBankingId and id
    // Get BlockLoadDTO by homeBankingId and id
    public BlockLoadDTO getBlockLoadByBankId(String blockTable, Integer whereId, Integer blockId) {
        if ("block".equalsIgnoreCase(blockTable)) {
            return getListBlock().stream()
                    .filter(block -> Objects.equals(block.getBotJobId(), whereId))
                    .filter(block -> blockId == null || Objects.equals(block.getId(), blockId))
                    .findFirst()
                    .orElse(null);

        } else if ("component_block".equalsIgnoreCase(blockTable)) {
            return getListBlockComp().stream()
                    .filter(block -> Objects.equals(block.getHomeBankingId(), whereId))
                    .filter(block -> blockId == null || Objects.equals(block.getId(), blockId))
                    .findFirst()
                    .orElse(null);
        }

        return null; // Unknown table
    }

    public List<BlockOptions> loadComboOptions(String tableName, String paneName) {
        try {
            // Pick the right list depending on the tableName
            List<BlockLoadDTO> blocks = tableName.equals("block") ? getListBlock() : getListBlockComp();

            // Decide mapping function based on paneName
            List<BlockOptions> newList;
            if ("ScannerPane".equalsIgnoreCase(paneName)) {
                newList = blocks.stream()
                        .map(BlockOptions::fromBlockWithInstructionId)
                        .sorted(Comparator.comparingInt(BlockOptions::getBlockOrderNumber))
                        .collect(Collectors.toList());

                // Add "Execute All Blocks" if needed
                if (newList.size() > 1) {
                    newList.add(0, new BlockOptions("Execute All Blocks", "", -1, -1, -1));
                } else if (newList.isEmpty()) {
                    newList.add(new BlockOptions("#1 Default Block", "Default Block", -1, -1, -1));
                }

            } else if ("NewCommandPane".equalsIgnoreCase(paneName)) {
                newList = blocks.stream()
                        .map(BlockOptions::fromBlockWithOrderNumber)
                        .sorted(Comparator.comparingInt(BlockOptions::getBlockOrderNumber))
                        .collect(Collectors.toList());

                if (!newList.isEmpty()) {
                    if (newList.size() > 1) {
                        newList.add(0, new BlockOptions("Select the Block", "", -1, -1, -1));
                    }

                } else {
                    newList = new ArrayList<>();
                    newList.add(new BlockOptions("#1 Default Block", "Default Block", -1, -1, -1));
                }

            } else {
                // default behavior, use fromBlockWithInstructionId
                newList = blocks.stream()
                        .map(BlockOptions::fromBlockWithInstructionId)
                        .collect(Collectors.toList());
            }

            // Replace current list with the new one
            return newList;

        } catch (Exception error) {
            log.error("Error loading combo options: " + error.getMessage());
        }

        return new ArrayList<>();
    }

    // Update Block Lists
    public void updateMemoryBlockName(String tableName, Integer whereId, Integer blockId, String newBlockName) {
        try {
            if ("block".equalsIgnoreCase(tableName)) {
                // Update in listBlock (global list)
                for (BlockLoadDTO block : getListBlock()) {
                    if (block.getId().equals(blockId)) {
                        block.setName(newBlockName);
                        break;
                    }
                }

                // Also update inside BotJobLoadDTO -> blockLoadDTOList
                for (BotJobLoadDTO botJob : getListBotJob()) {
                    if (botJob.getId().equals(whereId)) { // botJobId
                        if (botJob.getBlockLoadDTOList() != null) {
                            for (BlockLoadDTO block : botJob.getBlockLoadDTOList()) {
                                if (block.getId().equals(blockId)) {
                                    block.setName(newBlockName);
                                    break; // done
                                }
                            }
                        }
                    }
                }

            } else if ("component_block".equalsIgnoreCase(tableName)) {
                // Only update component blocks
                for (BlockLoadDTO block : getListBlockComp()) {
                    if (block.getId().equals(blockId)) {
                        block.setName(newBlockName);
                        break;
                    }
                }

                // Also update inside BotJobLoadDTO -> blockLoadDTOList
                for (BotJobLoadDTO botJob : getListBotJobComp()) {
                    if (botJob.getHomeBankingId().equals(whereId)) { // homeBankId
                        if (botJob.getBlockLoadDTOList() != null) {
                            for (BlockLoadDTO block : botJob.getBlockLoadDTOList()) {
                                if (block.getId().equals(blockId)) {
                                    block.setName(newBlockName);
                                    break; // done
                                }
                            }
                        }
                    }
                }

            } else {
                throw new IllegalArgumentException("Invalid tableName: " + tableName);
            }
        } catch (Exception error) {

            log.error("Error: Memory Update failed for 'updateMemoryBlockName': " + error.getMessage());
        }
    }

    public void updateMemoryInstructionName(String tableName, Integer whereId, List<InstructionLoad> listToUpdate) {

        if (listToUpdate == null || listToUpdate.isEmpty()) {
            return; // nothing to update
        }

        try {
            if ("instruction".equalsIgnoreCase(tableName)) {
                // Update global listInstruction
                for (InstructionLoad updateInstr : listToUpdate) {
                    for (InstructionLoad instr : getListInstruction()) {
                        if (instr.getId().equals(updateInstr.getId())) {
                            instr.setName(updateInstr.getName());
                            break;
                        }
                    }
                }

                // Update inside BotJobLoadDTO -> BlockLoadDTO -> instructionLoad
                for (BotJobLoadDTO botJob : getListBotJob()) {
                    if (botJob.getId().equals(whereId)) { // botJobId
                        if (botJob.getBlockLoadDTOList() != null) {
                            for (BlockLoadDTO block : botJob.getBlockLoadDTOList()) {
                                if (block.getInstructionLoad() != null) {
                                    for (InstructionLoad updateInstr : listToUpdate) {
                                        for (InstructionLoad instr : block.getInstructionLoad()) {
                                            if (instr.getId().equals(updateInstr.getId())) {
                                                instr.setName(updateInstr.getName());
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            } else if ("component_instruction".equalsIgnoreCase(tableName)) {
                // Update global listInstructionComp
                for (InstructionLoad updateInstr : listToUpdate) {
                    for (InstructionLoad instr : getListInstructionComp()) {
                        if (instr.getId().equals(updateInstr.getId())) {
                            instr.setName(updateInstr.getName());
                            break;
                        }
                    }
                }

                // Update inside BotJobLoadDTO component blocks
                for (BotJobLoadDTO botJob : getListBotJobComp()) {
                    if (botJob.getHomeBankingId().equals(whereId)) { // homeBankingId
                        if (botJob.getBlockLoadDTOList() != null) {
                            for (BlockLoadDTO block : botJob.getBlockLoadDTOList()) {
                                if (block.getInstructionLoad() != null) {
                                    for (InstructionLoad updateInstr : listToUpdate) {
                                        for (InstructionLoad instr : block.getInstructionLoad()) {
                                            if (instr.getId().equals(updateInstr.getId())) {
                                                instr.setName(updateInstr.getName());
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            } else {
                throw new IllegalArgumentException("Invalid tableName: " + tableName);
            }

        } catch (Exception error) {

            log.error("Error: Memory Update failed for 'updateMemoryInstructionName': " + error.getMessage());
        }
    }

    public void updateMemoryBlockStatusUpdate(String blockTable, Integer whereId, Integer blockId, boolean status) {
        try {
            if ("block".equalsIgnoreCase(blockTable)) {

                // 1. Update global instruction list
                for (InstructionLoad instr : getListInstruction()) {
                    if (Objects.equals(instr.getBlockId(), blockId) && Objects.equals(instr.getBotJobId(), whereId)) {
                        instr.setInstructionActive(status);
                    }
                }

                // 2. Update global block list
                for (BlockLoadDTO block : getListBlock()) {
                    if (Objects.equals(block.getId(), blockId) && Objects.equals(block.getBotJobId(), whereId)) {
                        block.setActive(status);
                    }
                }

                // 3. Update inside BotJob -> Block -> Instruction
                for (BotJobLoadDTO botJob : getListBotJob()) {
                    if (Objects.equals(botJob.getId(), whereId)) {
                        if (botJob.getBlockLoadDTOList() != null) {
                            for (BlockLoadDTO block : botJob.getBlockLoadDTOList()) {
                                if (Objects.equals(block.getId(), blockId)) {
                                    block.setActive(status);
                                    if (block.getInstructionLoad() != null) {
                                        for (InstructionLoad instr : block.getInstructionLoad()) {
                                            instr.setInstructionActive(status);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            } else if ("component_block".equalsIgnoreCase(blockTable)) {

                // 1. Update global instruction list
                for (InstructionLoad instr : getListInstructionComp()) {
                    if (Objects.equals(instr.getBlockId(), blockId)
                            && Objects.equals(instr.getHomeBankingId(), whereId)) {
                        instr.setInstructionActive(status);
                    }
                }

                // 2. Update global block list
                for (BlockLoadDTO block : getListBlockComp()) {
                    if (Objects.equals(block.getId(), blockId) && Objects.equals(block.getHomeBankingId(), whereId)) {
                        block.setActive(status);
                    }
                }

                // 3. Update inside BotJob -> Block -> Instruction
                for (BotJobLoadDTO botJob : getListBotJobComp()) {
                    if (Objects.equals(botJob.getHomeBankingId(), whereId)) {
                        if (botJob.getBlockLoadDTOList() != null) {
                            for (BlockLoadDTO block : botJob.getBlockLoadDTOList()) {
                                if (Objects.equals(block.getId(), blockId)) {
                                    block.setActive(status);
                                    if (block.getInstructionLoad() != null) {
                                        for (InstructionLoad instr : block.getInstructionLoad()) {
                                            instr.setInstructionActive(status);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            } else {
                throw new IllegalArgumentException("Invalid tableName: " + blockTable);
            }

        } catch (Exception error) {

            log.error("Error: Memory Update failed for 'updateMemoryStatusUpdate': " + error.getMessage());
        }
    }

    public void updateMemoryInstructionStatusUpdate(
            String tableName, Integer whereId, Integer instructionId, boolean status) {
        try {
            if ("instruction".equalsIgnoreCase(tableName)) {

                // Update global instruction list
                for (InstructionLoad instr : getListInstruction()) {
                    if (Objects.equals(instr.getId(), instructionId) && Objects.equals(instr.getBotJobId(), whereId)) {
                        instr.setInstructionActive(status);
                        break; // only one instruction matches
                    }
                }

                // Update inside BotJob -> Block -> Instruction
                for (BotJobLoadDTO botJob : getListBotJob()) {
                    if (Objects.equals(botJob.getId(), whereId)) {
                        if (botJob.getBlockLoadDTOList() != null) {
                            for (BlockLoadDTO block : botJob.getBlockLoadDTOList()) {
                                if (block.getInstructionLoad() != null) {
                                    for (InstructionLoad instr : block.getInstructionLoad()) {
                                        if (Objects.equals(instr.getId(), instructionId)) {
                                            instr.setInstructionActive(status);
                                            break; // only one instruction matches
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            } else if ("component_instruction".equalsIgnoreCase(tableName)) {

                // Update global component instruction list
                for (InstructionLoad instr : getListInstructionComp()) {
                    if (Objects.equals(instr.getId(), instructionId)
                            && Objects.equals(instr.getHomeBankingId(), whereId)) {
                        instr.setInstructionActive(status);
                        break; // only one instruction matches
                    }
                }

                // Update inside BotJobComp -> Block -> Instruction
                for (BotJobLoadDTO botJob : getListBotJobComp()) {
                    if (Objects.equals(botJob.getHomeBankingId(), whereId)) {
                        if (botJob.getBlockLoadDTOList() != null) {
                            for (BlockLoadDTO block : botJob.getBlockLoadDTOList()) {
                                if (block.getInstructionLoad() != null) {
                                    for (InstructionLoad instr : block.getInstructionLoad()) {
                                        if (Objects.equals(instr.getId(), instructionId)) {
                                            instr.setInstructionActive(status);
                                            break; // only one instruction matches
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            } else {
                throw new IllegalArgumentException("Invalid tableName: " + tableName);
            }

        } catch (Exception error) {

            log.error("Error: Memory Update failed for 'updateMemoryInstructionStatusUpdate': " + error.getMessage());
        }
    }

    public void updateMemoryBlockExcelExport(String tableName, Integer whereId, Integer blockId, String exportFile) {
        try {
            if ("block".equalsIgnoreCase(tableName)) {

                // 1. Update global block list
                for (BlockLoadDTO block : getListBlock()) {
                    if (Objects.equals(block.getId(), blockId) && Objects.equals(block.getBotJobId(), whereId)) {
                        block.setExportFile(exportFile);
                        break;
                    }
                }

                // 2. Update inside BotJob -> Block -> Instruction
                for (BotJobLoadDTO botJob : getListBotJob()) {
                    if (Objects.equals(botJob.getId(), whereId)) {
                        if (botJob.getBlockLoadDTOList() != null) {
                            for (BlockLoadDTO block : botJob.getBlockLoadDTOList()) {
                                if (Objects.equals(block.getId(), blockId)) {
                                    block.setExportFile(exportFile);
                                    break;
                                }
                            }
                        }
                    }
                }

            } else if ("component_block".equalsIgnoreCase(tableName)) {

                // 1. Update global block list
                for (BlockLoadDTO block : getListBlockComp()) {
                    if (Objects.equals(block.getId(), blockId) && Objects.equals(block.getHomeBankingId(), whereId)) {
                        block.setExportFile(exportFile);
                        break;
                    }
                }

                // 2. Update inside BotJob -> Block -> Instruction
                for (BotJobLoadDTO botJob : getListBotJobComp()) {
                    if (Objects.equals(botJob.getHomeBankingId(), whereId)) {
                        if (botJob.getBlockLoadDTOList() != null) {
                            for (BlockLoadDTO block : botJob.getBlockLoadDTOList()) {
                                if (Objects.equals(block.getId(), blockId)) {
                                    block.setExportFile(exportFile);
                                    break;
                                }
                            }
                        }
                    }
                }

            } else {
                throw new IllegalArgumentException("Invalid tableName: " + tableName);
            }

        } catch (Exception error) {

            log.error("Error: Memory Update failed for 'updateMemoryStatusUpdate': " + error.getMessage());
        }
    }

    public void updateMemoryParentOpenName(String tableName, Integer whereId, List<ParentOperations> listToUpdate) {

        if (listToUpdate == null || listToUpdate.isEmpty()) {
            return; // nothing to update
        }

        try {
            if ("instruction".equalsIgnoreCase(tableName)) {
                // Update in global listInstruction
                for (ParentOperations updateInstr : listToUpdate) {
                    for (InstructionLoad instr : getListInstruction()) {
                        if (instr.getId().equals(updateInstr.getId())
                                && instr.getParentId() != null
                                && instr.getParentId().equals(updateInstr.getInstructionId())) {
                            instr.setOperation(updateInstr.getOperations());
                            break;
                        }
                    }
                }

                // Update inside BotJobLoadDTO -> BlockLoadDTO -> instructionLoad
                for (BotJobLoadDTO botJob : getListBotJob()) {
                    if (botJob.getId().equals(whereId)) { // botJobId
                        if (botJob.getBlockLoadDTOList() != null) {
                            for (BlockLoadDTO block : botJob.getBlockLoadDTOList()) {
                                if (block.getInstructionLoad() != null) {
                                    for (ParentOperations updateInstr : listToUpdate) {
                                        for (InstructionLoad instr : block.getInstructionLoad()) {
                                            if (instr.getId().equals(updateInstr.getId())
                                                    && instr.getParentId() != null
                                                    && instr.getParentId().equals(updateInstr.getInstructionId())) {
                                                instr.setOperation(updateInstr.getOperations());
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            } else if ("component_instruction".equalsIgnoreCase(tableName)) {
                // Update in global listInstructionComp
                for (ParentOperations updateInstr : listToUpdate) {
                    for (InstructionLoad instr : getListInstructionComp()) {
                        if (instr.getId().equals(updateInstr.getId())
                                && instr.getParentId() != null
                                && instr.getParentId().equals(updateInstr.getInstructionId())) {
                            instr.setOperation(updateInstr.getOperations());
                            break;
                        }
                    }
                }

                // Update inside BotJobLoadDTO component blocks
                for (BotJobLoadDTO botJob : getListBotJobComp()) {
                    if (botJob.getHomeBankingId().equals(whereId)) { // homeBankingId
                        if (botJob.getBlockLoadDTOList() != null) {
                            for (BlockLoadDTO block : botJob.getBlockLoadDTOList()) {
                                if (block.getInstructionLoad() != null) {
                                    for (ParentOperations updateInstr : listToUpdate) {
                                        for (InstructionLoad instr : block.getInstructionLoad()) {
                                            if (instr.getId().equals(updateInstr.getId())
                                                    && instr.getParentId() != null
                                                    && instr.getParentId().equals(updateInstr.getInstructionId())) {
                                                instr.setOperation(updateInstr.getOperations());
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            } else {
                throw new IllegalArgumentException("Invalid tableName: " + tableName);
            }

        } catch (Exception error) {

            log.error("Error: Memory Update failed for 'updateMemoryParentOpenName': " + error.getMessage());
        }
    }

    // Update Block Order Numbers
    public void updateMemorySwiftBlockOrder(String tableName, Integer whereId, List<BlockLoadDTO> mappedBlocks) {
        try {

            if ("block".equalsIgnoreCase(tableName)) {
                // Update in listBlock (global list)
                for (BlockLoadDTO mapped : mappedBlocks) {
                    for (BlockLoadDTO block : getListBlock()) {
                        if (block.getId().equals(mapped.getId())) {
                            block.setBlockOrderNumber(mapped.getBlockOrderNumber());
                            break;
                        }
                    }
                }

                // Also update inside BotJobLoadDTO -> blockLoadDTOList
                for (BotJobLoadDTO botJob : getListBotJob()) {
                    if (botJob.getId().equals(whereId)) { // filter by botJobId
                        if (botJob.getBlockLoadDTOList() != null) {
                            for (BlockLoadDTO mapped : mappedBlocks) {
                                for (BlockLoadDTO block : botJob.getBlockLoadDTOList()) {
                                    if (block.getId().equals(mapped.getId())) {
                                        block.setBlockOrderNumber(mapped.getBlockOrderNumber());
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

            } else if ("component_block".equalsIgnoreCase(tableName)) {
                // Update in listBlockComp (global list)
                for (BlockLoadDTO mapped : mappedBlocks) {
                    for (BlockLoadDTO block : getListBlockComp()) {
                        if (block.getId().equals(mapped.getId())) {
                            block.setBlockOrderNumber(mapped.getBlockOrderNumber());
                            break;
                        }
                    }
                }

                // Also update inside BotJobLoadDTOComp -> blockLoadDTOList
                for (BotJobLoadDTO botJob : getListBotJobComp()) {
                    if (botJob.getHomeBankingId().equals(whereId)) { // filter by homeBankingId
                        if (botJob.getBlockLoadDTOList() != null) {
                            for (BlockLoadDTO mapped : mappedBlocks) {
                                for (BlockLoadDTO block : botJob.getBlockLoadDTOList()) {
                                    if (block.getId().equals(mapped.getId())) {
                                        block.setBlockOrderNumber(mapped.getBlockOrderNumber());
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

            } else {
                throw new IllegalArgumentException("Invalid tableName: " + tableName);
            }
        } catch (Exception error) {

            log.error("Error: Memory Update failed for 'updateMemoryBlockOrder': " + error.getMessage());
        }
    }

    // Remove Instruction by Id and update order numbers
    public void updateMemoryRemoveInstructionId(String tableName, Integer whereId, Integer instructionId) {
        try {
            List<BotJobLoadDTO> jobs;
            boolean isInstructionTable;

            if ("instruction".equalsIgnoreCase(tableName)) {
                jobs = getListBotJob();
                isInstructionTable = true;
            } else if ("component_instruction".equalsIgnoreCase(tableName)) {
                jobs = getListBotJobComp();
                isInstructionTable = false;
            } else {
                throw new IllegalArgumentException("Invalid tableName: " + tableName);
            }

            for (BotJobLoadDTO botJob : jobs) {
                Integer jobKey = isInstructionTable ? botJob.getId() : botJob.getHomeBankingId();
                if (jobKey.equals(whereId) && botJob.getBlockLoadDTOList() != null) {
                    Iterator<BlockLoadDTO> blockIt =
                            botJob.getBlockLoadDTOList().iterator();
                    while (blockIt.hasNext()) {
                        BlockLoadDTO block = blockIt.next();
                        if (block.getInstructionLoad() != null) {
                            // Remove the instruction
                            block.getInstructionLoad()
                                    .removeIf(instr -> instr.getId().equals(instructionId));

                            // Remove the block itself if empty
                            if (block.getInstructionLoad().isEmpty()) {
                                blockIt.remove();
                            } else {
                                // Reorder remaining instructions
                                reorderInstructions(block);
                            }
                        }
                    }
                }
            }
        } catch (Exception error) {

            log.error("Error: Memory Update failed for 'updateMemoryRemoveInstructionId': " + error.getMessage());
        }
    }

    // Remove Instruction as ParentId
    public void updateMemoryRemoveParentId(String tableName, Integer whereId, Integer parentId) {
        try {
            if ("instruction".equalsIgnoreCase(tableName)) {
                // Then remove inside BotJobLoadDTO -> blockLoadDTOList
                for (BotJobLoadDTO botJob : getListBotJob()) {
                    if (botJob.getId().equals(whereId)) { // filter by botJobId
                        if (botJob.getBlockLoadDTOList() != null) {
                            Iterator<BlockLoadDTO> blockIt =
                                    botJob.getBlockLoadDTOList().iterator();
                            while (blockIt.hasNext()) {
                                BlockLoadDTO block = blockIt.next();
                                if (block.getInstructionLoad() != null) {
                                    block.getInstructionLoad().removeIf(instr -> instr.getParentId()
                                            .equals(parentId));
                                    // Remove block itself if no instructions remain
                                    if (block.getInstructionLoad().isEmpty()) {
                                        blockIt.remove();
                                    }
                                }
                            }
                        }
                    }
                }

            } else if ("component_instruction".equalsIgnoreCase(tableName)) {
                // Then remove inside BotJobLoadDTOComp -> blockLoadDTOList
                for (BotJobLoadDTO botJob : getListBotJobComp()) {
                    if (botJob.getHomeBankingId().equals(whereId)) { // filter by homeBankingId
                        if (botJob.getBlockLoadDTOList() != null) {
                            Iterator<BlockLoadDTO> blockIt =
                                    botJob.getBlockLoadDTOList().iterator();
                            while (blockIt.hasNext()) {
                                BlockLoadDTO block = blockIt.next();
                                if (block.getInstructionLoad() != null) {
                                    block.getInstructionLoad().removeIf(instr -> instr.getParentId()
                                            .equals(parentId));
                                    // Remove block itself if no instructions remain
                                    if (block.getInstructionLoad().isEmpty()) {
                                        blockIt.remove();
                                    }
                                }
                            }
                        }
                    }
                }

            } else {
                throw new IllegalArgumentException("Invalid tableName: " + tableName);
            }
        } catch (Exception error) {

            log.error("Error: Memory Update failed for 'updateMemoryRemoveInstructionId': " + error.getMessage());
        }
    }

    // Remove Blocks by Id List
    public void updateMemoryRemoveBlockIds(String tableName, Integer whereId, List<Integer> restToDeleteIds) {
        try {
            if ("block".equalsIgnoreCase(tableName)) {
                // First remove from listBlock (global list)
                if (getListBlock() != null) {
                    getListBlock().removeIf(block -> restToDeleteIds.contains(block.getId()));
                }

                // Then remove inside BotJobLoadDTO -> blockLoadDTOList
                for (BotJobLoadDTO botJob : getListBotJob()) {
                    if (botJob.getId().equals(whereId)) { // filter by botJobId
                        if (botJob.getBlockLoadDTOList() != null) {
                            botJob.getBlockLoadDTOList().removeIf(block -> restToDeleteIds.contains(block.getId()));
                        }
                    }
                }

            } else if ("component_block".equalsIgnoreCase(tableName)) {
                // First remove from listBlockComp (global list)
                if (getListBlockComp() != null) {
                    getListBlockComp().removeIf(block -> restToDeleteIds.contains(block.getId()));
                }

                // Then remove inside BotJobLoadDTOComp -> blockLoadDTOList
                for (BotJobLoadDTO botJob : getListBotJobComp()) {
                    if (botJob.getHomeBankingId().equals(whereId)) { // filter by homeBankingId
                        if (botJob.getBlockLoadDTOList() != null) {
                            botJob.getBlockLoadDTOList().removeIf(block -> restToDeleteIds.contains(block.getId()));
                        }
                    }
                }

            } else {
                throw new IllegalArgumentException("Invalid tableName: " + tableName);
            }
        } catch (Exception error) {

            log.error("Error: Memory Update failed for 'updateMemoryRemoveBlockIds': " + error.getMessage());
        }
    }

    public void updateMemoryRollBackToOneBlock(String tableName, Integer whereId, List<Integer> restToDeleteIds) {
        try {
            if ("block".equalsIgnoreCase(tableName)) {
                // Remove from global list
                if (getListBlock() != null) {
                    getListBlock().removeIf(block -> restToDeleteIds.contains(block.getId()));
                }

                // Process BotJobLoadDTO
                for (BotJobLoadDTO botJob : getListBotJob()) {
                    if (botJob.getId().equals(whereId)) { // filter by botJobId
                        List<BlockLoadDTO> blocks = botJob.getBlockLoadDTOList();
                        if (blocks != null && !blocks.isEmpty()) {
                            BlockLoadDTO firstBlock = blocks.get(0);

                            // Collect instructions from blocks to delete
                            List<BlockLoadDTO> blocksToRemove = new ArrayList<>();
                            for (int i = 1; i < blocks.size(); i++) {
                                BlockLoadDTO block = blocks.get(i);
                                if (restToDeleteIds.contains(block.getId())) {
                                    if (block.getInstructionLoad() != null) {
                                        // Update blockId for each instruction before merging
                                        for (InstructionLoad instr : block.getInstructionLoad()) {
                                            if (instr.getBlockId() != null) {
                                                instr.setBlockId(firstBlock.getId());
                                            }
                                            if (instr.getParentBlockId() != null) {
                                                instr.setParentBlockId(firstBlock.getId());
                                            }
                                        }
                                        firstBlock.getInstructionLoad().addAll(block.getInstructionLoad());
                                    }
                                    blocksToRemove.add(block);
                                }
                            }

                            // Remove deleted blocks
                            blocks.removeAll(blocksToRemove);

                            // Reorder instructions in first block
                            List<InstructionLoad> instr = firstBlock.getInstructionLoad();
                            if (instr != null) {
                                for (int i = 0; i < instr.size(); i++) {
                                    instr.get(i).setInstructionOrderNumber(i + 1);
                                }
                            }
                        }
                    }
                }

            } else if ("component_block".equalsIgnoreCase(tableName)) {
                // Remove from global component list
                if (getListBlockComp() != null) {
                    getListBlockComp().removeIf(block -> restToDeleteIds.contains(block.getId()));
                }

                // Process BotJobLoadDTOComp
                for (BotJobLoadDTO botJob : getListBotJobComp()) {
                    if (botJob.getHomeBankingId().equals(whereId)) { // filter by homeBankingId
                        List<BlockLoadDTO> blocks = botJob.getBlockLoadDTOList();
                        if (blocks != null && !blocks.isEmpty()) {
                            BlockLoadDTO firstBlock = blocks.get(0);

                            List<BlockLoadDTO> blocksToRemove = new ArrayList<>();
                            for (int i = 1; i < blocks.size(); i++) {
                                BlockLoadDTO block = blocks.get(i);
                                if (restToDeleteIds.contains(block.getId())) {
                                    if (block.getInstructionLoad() != null) {
                                        // Update blockId for each instruction before merging
                                        for (InstructionLoad instr : block.getInstructionLoad()) {
                                            if (instr.getBlockId() != null) {
                                                instr.setBlockId(firstBlock.getId());
                                            }
                                            if (instr.getParentBlockId() != null) {
                                                instr.setParentBlockId(firstBlock.getId());
                                            }
                                        }
                                        firstBlock.getInstructionLoad().addAll(block.getInstructionLoad());
                                    }
                                    blocksToRemove.add(block);
                                }
                            }

                            blocks.removeAll(blocksToRemove);

                            // Reorder instructions in first block
                            List<InstructionLoad> instr = firstBlock.getInstructionLoad();
                            if (instr != null) {
                                for (int i = 0; i < instr.size(); i++) {
                                    instr.get(i).setInstructionOrderNumber(i + 1);
                                }
                            }
                        }
                    }
                }

            } else {
                throw new IllegalArgumentException("Invalid tableName: " + tableName);
            }
        } catch (Exception error) {

            log.error("Error: Memory Update failed for 'updateMemoryRemoveBlockIds': " + error.getMessage());
        }
    }

    public void updateMemoryRowMove(String tableName, Integer whereId, List<UpdatedRow> updatedRows) {
        try {
            if ("block".equalsIgnoreCase(tableName)) {
                for (BotJobLoadDTO botJob : getListBotJob()) {
                    if (botJob.getId().equals(whereId)) { // filter by botJobId
                        if (botJob.getBlockLoadDTOList() != null) {
                            applyUpdates(botJob.getBlockLoadDTOList(), updatedRows);
                        }
                    }
                }

            } else if ("component_block".equalsIgnoreCase(tableName)) {
                for (BotJobLoadDTO botJob : getListBotJobComp()) {
                    if (botJob.getHomeBankingId().equals(whereId)) { // filter by homeBankingId
                        if (botJob.getBlockLoadDTOList() != null) {
                            applyUpdates(botJob.getBlockLoadDTOList(), updatedRows);
                        }
                    }
                }

            } else {
                throw new IllegalArgumentException("Invalid tableName: " + tableName);
            }
        } catch (Exception error) {

            log.error("Error: Memory Update failed for 'updateMemoryRowMove': " + error.getMessage());
        }
    }

    private void applyUpdates(List<BlockLoadDTO> blockList, List<UpdatedRow> updatedRows) {
        Map<Integer, BlockLoadDTO> blockMap = blockList.stream().collect(Collectors.toMap(BlockLoadDTO::getId, b -> b));

        for (UpdatedRow mapped : updatedRows) {
            for (BlockLoadDTO block : blockList) {
                if (block.getInstructionLoad() != null) {
                    Iterator<InstructionLoad> it = block.getInstructionLoad().iterator();
                    while (it.hasNext()) {
                        InstructionLoad instr = it.next();
                        if (instr.getId().equals(mapped.getInstructionId())) {

                            // If blockId changed, move instruction to new block
                            if (!Objects.equals(instr.getBlockId(), mapped.getBlockId())) {
                                it.remove(); // remove from old block
                                BlockLoadDTO newBlock = blockMap.get(mapped.getBlockId());
                                if (newBlock != null) {
                                    if (newBlock.getInstructionLoad() == null) {
                                        newBlock.setInstructionLoad(new ArrayList<>());
                                    }
                                    instr.setBlockId(mapped.getBlockId());
                                    newBlock.getInstructionLoad().add(instr);
                                }
                            }

                            // Update order number
                            if (!Objects.equals(
                                    instr.getInstructionOrderNumber(), mapped.getInstructionOrderNumber())) {
                                instr.setInstructionOrderNumber(mapped.getInstructionOrderNumber());
                            }

                            break; // found and updated
                        }
                    }
                }
            }
        }

        // Re-sort each blocks instructions by instructionOrderNumber
        for (BlockLoadDTO block : blockList) {
            if (block.getInstructionLoad() != null) {
                block.getInstructionLoad().sort(Comparator.comparing(InstructionLoad::getInstructionOrderNumber));
            }
        }
    }

    /**
     * Clears all internal lists in PerformLists to reset the state.
     */
    public void clearAllLists() {
        // You don't need to check for null here because the lists are initialized
        // as new ArrayLists, so they will never be null. This makes the code cleaner.

        listHomeBanking.clear();
        listHomeUrl.clear();
        quickBotJobs.clear();
        listBotJob.clear();
        listBotJobComp.clear();
        listBlock.clear();
        listBlockComp.clear();
        listInstruction.clear();
        listInstructionComp.clear();
        listVariable.clear();
        listVariableComp.clear();
        listReference.clear();
        listReferenceComp.clear();
        allActions.clear();
        instrucOperList.clear();
        listDatabaseUsers.clear();
        listVariablesUser.clear();
        listWebPageItems.clear();
        listParentOperations.clear();
    }

    /**
     * Reorders instructionOrderNumber for instructions in a block,
     * starting from 1 and incrementing sequentially.
     */
    private void reorderInstructions(BlockLoadDTO block) {
        int order = 1;
        for (InstructionLoad instr : block.getInstructionLoad()) {
            instr.setInstructionOrderNumber(order++);
        }
    }

    public List<InstructionLoad> buildJsonViewData(List<BotJobLoadDTO> listInstruction) {
        if (!listInstruction.isEmpty()
                && !listInstruction.get(0).getBlockLoadDTOList().isEmpty()) {

            List<InstructionLoad> rowList = null;
            try {

                //                for (BlockLoadDTO block : listInstruction.get(0).getBlockLoadDTOList()) {
                //                    loadInstructions(whereId, block.getId(), -1, tableName);
                //                    rowList = tableName.equals("instruction")
                //                            ? performLists.getListInstruction()
                //                            : performLists.getListInstructionComp();
                //                    reorderInstructions(rowList, tableName, false);
                //                }

                List<InstructionLoad> blockLoopInstructions = listInstruction.get(0).getBlockLoadDTOList().stream()
                        .flatMap(itemBlock -> itemBlock.getInstructionLoad().stream()
                                .map(loopInstLoad -> new InstructionLoad(
                                        listInstruction.get(0).getHomeBankingId(), // homBankingId
                                        itemBlock.getBotJobId(), // botJobId
                                        itemBlock.getBotJobName(), // botJob Name
                                        loopInstLoad.getId(), // Instruction Id
                                        loopInstLoad.getInstructionOrderNumber(), // Instruction Order
                                        loopInstLoad.getName(), // Instruction Name
                                        loopInstLoad.getDescription(), // Instruction Description
                                        itemBlock.getId(), // block ID
                                        itemBlock.getBlockOrderNumber(), // block Order
                                        itemBlock.getName(), // block Name
                                        itemBlock.getActive(),
                                        loopInstLoad.getInstructionActive(),
                                        itemBlock.getWait(),
                                        loopInstLoad.getActions(),
                                        loopInstLoad.getParentBlockId(), // Parent Block Id
                                        loopInstLoad.getParentId(),
                                        loopInstLoad.getVariableId(),
                                        loopInstLoad.getOperation(),
                                        itemBlock.getExportFile(),
                                        loopInstLoad.getTagName())))
                        .collect(Collectors.toList());

                // Step 1: Filter rows where actions = "REFRESH_LOOP" and collect their parent IDs
                Set<Integer> parentIdsForRefreshLoop = blockLoopInstructions.stream()
                        .filter(instruction -> "REFRESH_LOOP".equalsIgnoreCase(instruction.getActions()))
                        .map(InstructionLoad::getParentId)
                        .collect(Collectors.toSet());

                // Step 2: Iterate through the list and set refreshLoop = true for rows with id in
                // parentIdsForRefreshLoop
                blockLoopInstructions.forEach(instruction -> {
                    if (parentIdsForRefreshLoop.contains(instruction.getId())) {
                        instruction.setRefreshLoop(true);
                    }
                });

                // Step 1: Filter rows where actions = "LOOP" and collect their parent IDs
                Set<Integer> parentIdsForLoopOnly = blockLoopInstructions.stream()
                        .filter(instruction -> "LOOP".equalsIgnoreCase(instruction.getActions()))
                        .map(InstructionLoad::getParentId)
                        .collect(Collectors.toSet());

                // Step 2: Iterate through the list and set loopOnly = true for rows with id in parentIdsForLoopOnly
                blockLoopInstructions.forEach(instruction -> {
                    if (parentIdsForLoopOnly.contains(instruction.getId())) {
                        instruction.setLoopOnly(true);
                    }
                });

                return blockLoopInstructions;
            } catch (Exception error) {
                log.error("No BotJob Loaded for buildJsonViewData");
            }
        }

        return new ArrayList<>();
    }
}
