package com.allinweb.ch.facade;

import com.allinweb.ch.component.model.BlockDetailsDTO;
import com.allinweb.ch.component.model.BlockLoadDTO;
import com.allinweb.ch.component.model.BlockOrderDetailDTO;
import com.allinweb.ch.component.model.BotJobLoadDTO;
import com.allinweb.ch.component.model.ComplexInstructionLoadDTO;
import com.allinweb.ch.component.model.DeleteBlockDTO;
import com.allinweb.ch.component.model.HomeBankingLoadDTO;
import com.allinweb.ch.component.model.InstructionLoadDTO;
import com.allinweb.ch.component.model.InstructionReferenceLoadDTO;
import com.allinweb.ch.component.model.RollBackBlocksDTO;
import com.allinweb.ch.component.model.RowMoveDTO;
import com.allinweb.ch.component.model.VariableLoadDTO;
import com.allinweb.ch.component.model.VariableUserDTO;
import com.allinweb.ch.persistence.BlockDTO;
import com.allinweb.ch.persistence.BotJobDTO;
import com.allinweb.ch.persistence.InstructionDTO;
import com.allinweb.ch.persistence.ReferenceDTO;
import com.allinweb.ch.util.ARConstants;
import com.allinweb.ch.util.ARLogger;
import com.allinweb.ch.util.ARPropertyEnum;
import com.allinweb.ch.util.ARPropertyManager;
import com.allinweb.ch.util.ComboBoxVars;
import com.allinweb.ch.util.ErrorMessage;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class PerformDataBase {

    private static Connection conn = null;

    private static final String CONNECTION_TYPE = "jdbc:ucanaccess://";
    private static final String CONNECTION_PARAMETERS = ";memory=false;newDatabaseVersion=V2010";

    // Postgres
    private static boolean POSTGRES_DB = false;
    private static final String CONNECTION_POSTGRES = "jdbc:postgresql://";
    private static final String DB_HOST = "localhost"; // or your PostgreSQL server address
    private static final String DB_PORT = "5432"; // default PostgreSQL port
    private static final String DB_NAME = "abr_web"; // your database name
    private static final String USERNAME = "postgres"; // your database username
    private static final String PASSWORD = "martini"; // your database password

    private static SessionFactory sessionFactory = null;
    private static Session session = null;

    private ObservableList<VariableUserDTO> variablesList = FXCollections.observableArrayList();
    private ObservableList<ComboBoxVars> webPageItems = FXCollections.observableArrayList();

    private BotJobLoadDTO botJobLoadDTO;

    private List<BotJobLoadDTO> botJobLoadList = new ArrayList<>();
    private List<BlockLoadDTO> blockLoadList = new ArrayList<>();

    private Gson gson = new Gson();

    // Static final variable to hold the singleton instance
    protected static final SingletonSupplier<PerformDataBase> instance = () -> new PerformDataBase();

    private static final DateTimeFormatter FORMAT_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    // Private constructor to prevent instantiation
    private PerformDataBase() {
        // Initialize if necessary
    }

    public void initializePerformActions() {}

    // Public method to access the singleton instance
    public static PerformDataBase getInstance() {
        return instance.get();
    }

    public static Connection getConn() {
        return conn;
    }

    public static void setConn(Connection conn) {
        PerformDataBase.conn = conn;
    }

    public static void closeConnection() {
        if (conn != null) {
            try {
                conn.close();
                conn = null; // Reset the connection to null after closing
            } catch (SQLException e) {
                System.out.println(e.getMessage()); // Handle the exception, log it or rethrow it as needed
            }
        }
    }

    public static void changeDbConnection() {
        String priorityPath = ARPropertyManager.getInstance().getProperty(ARPropertyEnum.FOLDER_PATH_PRIORITY);
        String dataBaseType = ARPropertyManager.getInstance().getProperty(ARPropertyEnum.DATABASE_TYPE);

        closeConnection();

        if (dataBaseType != null && dataBaseType.equalsIgnoreCase("POSTGRES")) {
            POSTGRES_DB = true;
        } else {
            POSTGRES_DB = false;
        }

        if (priorityPath != null) {

            //            if (priorityPath != null && !priorityPath.isBlank()) {
            //                arPriorities.loadPriorities();
            //            }

            if (POSTGRES_DB) {
                String dbUrl = CONNECTION_POSTGRES + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;
                sessionFactory = new Configuration()
                        .configure()
                        .setProperty("hibernate.connection.url", dbUrl)
                        .setProperty("hibernate.connection.username", USERNAME)
                        .setProperty("hibernate.connection.password", PASSWORD)
                        .setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                        .setProperty("hibernate.connection.driver_class", "org.postgresql.Driver")
                        .buildSessionFactory();
                session = sessionFactory.openSession();
                //                cacheEntitiesFromDB();
            } else {

                try {
                    String dbPath = ARPropertyManager.getInstance().getProperty(ARPropertyEnum.FOLDER_PATH_DB);
                    if (!dbPath.isBlank()) {
                        File dbFolder = new File(dbPath);
                        dbFolder.mkdirs();
                        String dbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;
                        sessionFactory = new Configuration()
                                .configure()
                                .setProperty("hibernate.connection.url", dbUrl)
                                .buildSessionFactory();
                        session = sessionFactory.openSession();
                        //                    cacheEntitiesFromDB();
                    }
                }catch (Exception error) {
                    
                }
            }
        }
    }

    public static Connection getConnection() {
        String dataBaseType = ARPropertyManager.getInstance().getProperty(ARPropertyEnum.DATABASE_TYPE);

        if (dataBaseType != null && dataBaseType.equalsIgnoreCase("POSTGRES")) {
            POSTGRES_DB = true;
        } else {
            POSTGRES_DB = false;
        }

        try {
            if (conn == null || conn.isClosed()) {
                if (!POSTGRES_DB) {
                    String dbPath = ARPropertyManager.getInstance().getProperty(ARPropertyEnum.FOLDER_PATH_DB);
                    String dbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;
                    conn = DriverManager.getConnection(dbUrl);
                } else {
                    String dbUrl = CONNECTION_POSTGRES + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;
                    conn = DriverManager.getConnection(dbUrl, USERNAME, PASSWORD);
                }
            }
        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("getConnection Error: " + e.getMessage());
        }

        return conn;
    }

    // Handle DELETE_INSTRUCTION message
    public static void deleteInstruction(
            int botJobId, com.allinweb.ch.component.model.InstructionDTO deleteInstructionDTO) {
        if (deleteVariable(botJobId, deleteInstructionDTO.getInstructionId()))
            if (deleteReferences(botJobId, deleteInstructionDTO.getInstructionId()))
                if (deleteRow(deleteInstructionDTO)) {
                    deleteNullBlocks(botJobId);
                    updateBlockOrderNumber(selectAllBlocks(deleteInstructionDTO.getBlockId()), true);
                }
    }

    private static boolean deleteVariable(int bot_job_id, int instructionId) {
        // Build the SQL delete statement

        try (Statement stmt = getConnection().createStatement()) {

            String deleteSQL = "DELETE FROM variable WHERE "
                    + " instruction_id = " + instructionId
                    + " AND bot_job_id = " + bot_job_id;

            // Execute the update statement and check if any rows were affected
            int rowsAffected = stmt.executeUpdate(deleteSQL);
            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format(
                                "Delete Variables for instruction ID %d has been successfully deleted from botJobId %d:",
                                instructionId, bot_job_id));
            } else {
                /*ARLogger.getInstance(PerformDataBase.class)
                       .warning(String.format(
                               "No matching record found for instruction ID %d in botJobId %d:",
                               instructionId, bot_job_id));

                */
            }
            return true;

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error deleting  Variable ID %d from botJobId ID %d. Error: %s: ",
                            instructionId, bot_job_id, e.getMessage()));
        }
        return false;
    }

    private static boolean deleteCompVariable(int homeBankingId, int blockId, int botJobId) {
        // Build the SQL delete statement

        try (Statement stmt = getConnection().createStatement()) {

            String deleteSQL =
                    "DELETE FROM variable WHERE " + " instruction_id = " + blockId + " AND bot_job_id = " + blockId;

            // Execute the update statement and check if any rows were affected
            int rowsAffected = stmt.executeUpdate(deleteSQL);
            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format(
                                "Delete Variables for instruction ID %d has been successfully deleted from botJobId %d:",
                                homeBankingId, blockId));
            } else {
                /*ARLogger.getInstance(PerformDataBase.class)
                       .warning(String.format(
                               "No matching record found for instruction ID %d in botJobId %d:",
                               instructionId, bot_job_id));

                */
            }
            return true;

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error deleting  Variable ID %d from botJobId ID %d. Error: %s: ",
                            homeBankingId, blockId, e.getMessage()));
        }
        return false;
    }

    private static boolean deleteReferences(int botJobId, int instructionId) {
        // Build the SQL delete statement
        try (Statement stmt = getConnection().createStatement()) {

            String deleteSQL = "DELETE FROM reference" + " WHERE instruction_id = " + instructionId;

            // Execute the update statement and check if any rows were affected
            int rowsAffected = stmt.executeUpdate(deleteSQL);
            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format(
                                "Delete References for Instruction ID %d has been successfully deleted from botJobId %d.",
                                instructionId, botJobId));
            } else {
                //                ARLogger.getInstance(PerformDataBase.class)
                //                        .warning(String.format(
                //                                "No matching record found for instruction ID %d in block %d.",
                //                                instructionId, botJobId));
            }
            return true;

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error deleting instruction ID %d from botJobId ID %d. Error: %s",
                            instructionId, botJobId, e.getMessage()));
        }
        return false;
    }

    private static boolean deleteCompReferences(int homeBankingId, int blockId, int botJobId) {
        // Build the SQL delete statement
        try (Statement stmt = getConnection().createStatement()) {

            String deleteSQL = "DELETE FROM reference" + " WHERE instruction_id = " + blockId;

            // Execute the update statement and check if any rows were affected
            int rowsAffected = stmt.executeUpdate(deleteSQL);
            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format(
                                "Delete References for Instruction ID %d has been successfully deleted from botJobId %d.",
                                homeBankingId, blockId));
            } else {
                //                ARLogger.getInstance(PerformDataBase.class)
                //                        .warning(String.format(
                //                                "No matching record found for instruction ID %d in block %d.",
                //                                instructionId, botJobId));
            }
            return true;

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error deleting instruction ID %d from botJobId ID %d. Error: %s",
                            homeBankingId, blockId, e.getMessage()));
        }
        return false;
    }

    private static boolean deleteRow(com.allinweb.ch.component.model.InstructionDTO deleteInstructionDTO) {
        // Build the SQL delete statement
        try (Statement stmt = getConnection().createStatement()) {

            int rowsAffected = 0;
            String deleteSQL = "DELETE FROM instruction" + " WHERE id = "
                    + deleteInstructionDTO.getInstructionId()
                    + (deleteInstructionDTO.getBlockId() > 0
                            ? " AND block_id = " + deleteInstructionDTO.getBlockId()
                            : " AND block_id IS NULL");

            if (deleteInstructionDTO.getActions() != null
                    && (deleteInstructionDTO.getActions().equals("IF")
                            || deleteInstructionDTO.getActions().equals("ELSE")
                            || deleteInstructionDTO.getActions().equals("ENDIF"))) {

                rowsAffected += stmt.executeUpdate("DELETE FROM instruction  "
                        + " WHERE "
                        + " block_id = " + deleteInstructionDTO.getBlockId() + " AND parent_id = "
                        + deleteInstructionDTO.getParentId());
            } else {

                rowsAffected += stmt.executeUpdate(deleteSQL);
            }

            // Execute the update statement and check if any rows were affected
            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format(
                                "The instruction with ID %d has been successfully deleted from block %d.",
                                deleteInstructionDTO.getInstructionId(), deleteInstructionDTO.getBlockId()));
            } else {
                //                ARLogger.getInstance(PerformDataBase.class)
                //                        .warning(String.format(
                //                                "No matching record found for instruction ID %d in block %d.",
                // instructionId, blockId));
            }
            return true;

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error deleting instruction ID %d from block ID %d. Error: %s",
                            deleteInstructionDTO.getInstructionId(),
                            deleteInstructionDTO.getBlockId(),
                            e.getMessage()));
        }
        return false;
    }

    private static boolean deleteCompRow(int homeBankingId, int blockId, int botJobId) {
        // Build the SQL delete statement
        try (Statement stmt = getConnection().createStatement()) {

            int rowsAffected = 0;
            String deleteSQL = "DELETE FROM instruction" + " WHERE id = "
                    + homeBankingId
                    + (blockId > 0 ? " AND block_id = " + blockId : " AND block_id IS NULL");

            if ("deleteInstructionDTO.getActions()" != null
                    && ("deleteInstructionDTO.getActions()".equals("IF")
                            || "deleteInstructionDTO.getActions()".equals("ELSE")
                            || "deleteInstructionDTO.getActions()".equals("ENDIF"))) {

                rowsAffected += stmt.executeUpdate("DELETE FROM instruction  "
                        + " WHERE "
                        + " block_id = " + blockId + " AND parent_id = "
                        + "deleteInstructionDTO.getParentId()");
            } else {

                rowsAffected += stmt.executeUpdate(deleteSQL);
            }

            // Execute the update statement and check if any rows were affected
            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format(
                                "The instruction with ID %d has been successfully deleted from block %d.",
                                "deleteInstructionDTO.getInstructionId()", blockId));
            } else {
                //                ARLogger.getInstance(PerformDataBase.class)
                //                        .warning(String.format(
                //                                "No matching record found for instruction ID %d in block %d.",
                // instructionId, blockId));
            }
            return true;

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error deleting instruction ID %d from block ID %d. Error: %s",
                            "deleteInstructionDTO.getInstructionId()", blockId, e.getMessage()));
        }
        return false;
    }

    public static void deleteNullBlocks(int botJobId) {
        // Build the SQL delete statement
        try (Statement stmt = getConnection().createStatement()) {

            String deleteSQL = "DELETE FROM block b "
                    + "WHERE b.bot_job_id = " + botJobId
                    //                    + " AND b.block_order_number != 1 " // Exclude block with blockOrderNumber = 1
                    + " AND NOT EXISTS ( "
                    + "     SELECT 1 "
                    + "     FROM instruction bli "
                    + "     WHERE bli.block_id = b.id);";

            // Execute the update statement and check if any rows were affected
            int rowsAffected = stmt.executeUpdate(deleteSQL);
            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format(
                                "The %d Nulls Blocks successfully deleted from botJobId %d.", rowsAffected, botJobId));
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error deleting Null Blocks with BotJobId ID %d. Error: %s", botJobId, e.getMessage()));
        }
    }

    public static void deleteCompNullBlocks(int homeBankingId, int blockId, int botJobId) {
        // Build the SQL delete statement
        try (Statement stmt = getConnection().createStatement()) {

            String deleteSQL = "DELETE FROM block b "
                    + "WHERE b.bot_job_id = " + botJobId
                    //                    + " AND b.block_order_number != 1 " // Exclude block with blockOrderNumber = 1
                    + " AND NOT EXISTS ( "
                    + "     SELECT 1 "
                    + "     FROM instruction bli "
                    + "     WHERE bli.block_id = b.id);";

            // Execute the update statement and check if any rows were affected
            int rowsAffected = stmt.executeUpdate(deleteSQL);
            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format(
                                "The %d Nulls Blocks successfully deleted from botJobId %d.", rowsAffected, botJobId));
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error deleting Null Blocks with BotJobId ID %d. Error: %s", botJobId, e.getMessage()));
        }
    }

    public static void updateBlockOrderNumber(List<BlockOrderDetailDTO> blockOrderDetailDTOList, boolean reorderAll) {
        //         Sort the blockOrderDetailDTOList based on the previous blockOrderNumber in ascending order
        blockOrderDetailDTOList.sort(Comparator.comparingInt(BlockOrderDetailDTO::getBlockOrderNumber));

        try (Statement stmt = getConnection().createStatement()) {
            int newOrderNumber = 1; // Start reordering from 1

            for (BlockOrderDetailDTO blockOrderDetailDTO : blockOrderDetailDTOList) {
                // Update each block's block_order_number starting from 1
                String updateSQL = "UPDATE block SET block_order_number = "
                        + (reorderAll ? newOrderNumber : blockOrderDetailDTO.getBlockOrderNumber())
                        + " WHERE id = "
                        + blockOrderDetailDTO.getBlockId()
                        + " AND bot_job_id = " + blockOrderDetailDTO.getBotJobId();

                int rowsAffected = stmt.executeUpdate(updateSQL);

                if (rowsAffected > 0) {
                    ARLogger.getInstance(PerformDataBase.class)
                            .info(String.format(
                                    "Block Order Number updated blockId: %s, newBlockOrderNumber: %s",
                                    blockOrderDetailDTO.getBlockId(), newOrderNumber));
                } else {
                    ARLogger.getInstance(PerformDataBase.class)
                            .warning(String.format(
                                    "UpdateBlockOrderNumber - No matching record found to update botJobId: %d blockId: %d",
                                    blockOrderDetailDTO.getBotJobId(), blockOrderDetailDTO.getBlockId()));
                }

                newOrderNumber++; // Increment the new order number for the next block
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("Error UpdateBlockOrderNumber. Error: %s", e.getMessage()));
        }
    }

    public static List<BlockOrderDetailDTO> selectAllBlocks(int botJobId) {
        List<BlockOrderDetailDTO> blockOrderDetails = new ArrayList<>();
        try (Statement stmt = getConnection().createStatement()) {

            // Select blocks based on botJobId, ordered by block_order_number ASC
            String selectSQL =
                    "SELECT id FROM block WHERE bot_job_id = " + botJobId + " ORDER BY block_order_number ASC";
            ResultSet rs = stmt.executeQuery(selectSQL);

            int newOrderNumber = 1;
            // Iterate through the result set and build BlockOrderDetailDTO list
            while (rs.next()) {
                int blockId = rs.getInt("id");

                // Create a BlockOrderDetailDTO object with blockId and the new order number
                BlockOrderDetailDTO blockDetail = BlockOrderDetailDTO.builder()
                        .blockId(blockId)
                        .botJobId(botJobId)
                        .blockOrderNumber(newOrderNumber)
                        .build();

                // Add the block detail to the list
                blockOrderDetails.add(blockDetail);

                // Increment the order number for the next block
                newOrderNumber++;
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error selecting blocks for botJobId ID %d. Error: %s", botJobId, e.getMessage()));
        }
        return blockOrderDetails;
    }

    // Handle BLOCK_UPDATE message
    public void updateBlockName(int botJobId, int blockId, String blockName) {
        try (Statement stmt = getConnection().createStatement()) {

            // Update each block's block_order_number starting from 1
            String updateSQL = "UPDATE block SET name = '" + blockName + "',"
                    + " description = '" + blockName + "'"
                    + " WHERE id = " + blockId
                    + " and bot_job_id = " + botJobId;

            int rowsAffected = stmt.executeUpdate(updateSQL);

            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format("Block Name updated blockId: %s, name: %s", blockId, blockName));
            } else {
                ARLogger.getInstance(PerformDataBase.class)
                        .warning(String.format(
                                "UpdateBlockOrderName - No matching record found to update botJobId: %d blockId: %d",
                                botJobId, blockId));
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("Error UpdateBlockOrderNumber. Error: %s", e.getMessage()));
        }
    }

    // Handle BLOCK_UPDATE message
    public boolean updateBlockExportFile(int botJobId, int blockId, String expoprtFile) {
        try (Statement stmt = getConnection().createStatement()) {

            // Update each block's block_order_number starting from 1
            String updateSQL = "UPDATE block SET export_file = '" + expoprtFile + "'"
                    + " WHERE id = " + blockId
                    + " and bot_job_id = " + botJobId;

            int rowsAffected = stmt.executeUpdate(updateSQL);

            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format("Block Export File updated blockId: %s, name: %s", blockId, expoprtFile));
            } else {
                ARLogger.getInstance(PerformDataBase.class)
                        .warning(String.format(
                                "updateBlockExportFile - No matching record found to update botJobId: %d blockId: %d",
                                botJobId, blockId));

                return false;
            }

            return true;

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("Error updateBlockExportFile. Error: %s", e.getMessage()));
        }
        return false;
    }

    // Handle BLOCK_UPDATE message
    public boolean updateExportAR(InstructionLoadDTO instruction) {
        try (Statement stmt = getConnection().createStatement()) {

            // Update each export_to_abr
            String updateSQL = "UPDATE instruction SET export_to_abr = " + instruction.getExportToABR()
                    + " WHERE id = " + instruction.getBlockId()
                    + " and bot_job_id = " + instruction.getBotJobId();

            int rowsAffected = stmt.executeUpdate(updateSQL);

            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format(
                                "Instruction updated blockId: %s, Export to AR: %s",
                                instruction.getBlockId(), instruction.getExportToABR()));
            } else {
                ARLogger.getInstance(PerformDataBase.class)
                        .warning(String.format(
                                "updateExportAR - No matching record found to update botJobId: %d blockId: %d",
                                instruction.getBotJobId(), instruction.getBlockId()));

                return false;
            }

            return true;

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("Error updateBlockExportFile. Error: %s", e.getMessage()));
        }
        return false;
    }

    // Handle DELETE_BLOCK message
    public boolean deleteBlock(DeleteBlockDTO deleteBlockDTO) {
        boolean blockDeletion = false;
        List<com.allinweb.ch.component.model.InstructionDTO> deleteList =
                getInstructionsByBlockId(deleteBlockDTO.getBotJobId(), deleteBlockDTO.getBlockId());
        if (deleteList.size() > 0) {
            for (com.allinweb.ch.component.model.InstructionDTO deleteDTO : deleteList) {
                deleteInstruction(deleteBlockDTO.getBotJobId(), deleteDTO);
                //                updateOtherBlocks()
            }
        }
        blockDeletion = deleteBlock((int) deleteBlockDTO.getBotJobId(), (int) deleteBlockDTO.getBlockId());
        //        updateOtherBlocks(deleteBlockDTO.getUpdatedBlockDTO());
        deleteNullBlocks((int) deleteBlockDTO.getBotJobId());
        if (deleteBlockDTO.getUpdatedBlocks() != null
                && deleteBlockDTO.getUpdatedBlocks().size() > 0) {
            updateBlockOrderNumber(
                    selectAllBlocks(deleteBlockDTO.getUpdatedBlocks().get(0).getBotJobId()), true);
        }

        return blockDeletion;
    }

    // Method to create a new BlockDTO entity and save it to the database
    public int createNewBlock(BlockDetailsDTO newBlockDetails) {
        try {
            // Persist the BlockDTO entity using the saveBlock method
            int newBlockId = saveBlock(newBlockDetails, newBlockDetails.getBotJobId());
            if (newBlockId > -1) {
                // Update the Instruction blockId
                return newBlockId;
            }

        } catch (Exception e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("createNewBlock - \nError: %s", e.getMessage()));
        }

        return -1;
    }

    private int saveBlock(BlockDetailsDTO blockDTO, int botJobId) {
        // Generate a Unique-ID for the block
        Integer nextId = loadNextIdBlockData() + 1;
        Integer nextBlockOrder = -1;
        if (blockDTO.getForceOrder()) {
            nextBlockOrder = blockDTO.getBlockOrderNumber();
        } else {
            nextBlockOrder = loadNextBlockOrderNumber(blockDTO.getBotJobId()) + 1;
        }

        if (nextId < 0 || nextBlockOrder < 0) {
            return -1;
        }

        // Build the SQL insert query
        String insertSQL =
                "INSERT INTO block(id, block_order_number, description, name, type_id, active, wait, bot_job_id) VALUES ("
                        + nextId + ", "
                        + nextBlockOrder + ", " // block_order_number
                        + "'" + blockDTO.getBlockName() + " description', " // description
                        + "'" + blockDTO.getBlockName() + "', " // name
                        + 1 + ", " // type_id
                        + blockDTO.getActive() + ", " // active
                        + 3 + ", " // wait
                        + botJobId + ")"; // bot_job_id, assuming BotJobDTO has an ID
        try (Statement stmt = getConnection().createStatement()) {
            stmt.executeUpdate(insertSQL);
            ARLogger.getInstance(PerformDataBase.class)
                    .info(String.format("Block data saved successfully.\n BlockId: %d", nextId));
            return nextId;
        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("saveBlock - \nError: %s", e.getMessage()));
            return -1;
        }
    }

    public int createNewBotJob(BotJobLoadDTO createdBotJob) {
        // Generate a Unique-ID for the block
        Integer nextId = loadNextIdBotJobData() + 1;

        if (nextId < 0) {
            return -1;
        }

        // Build the SQL insert query
        String insertSQL = "INSERT INTO bot_job(id, name, description, home_banking_id) VALUES ("
                + nextId + ", "
                + "'" + createdBotJob.getName() + "', " // name
                + "'" + createdBotJob.getName() + " description', " // description
                + createdBotJob.getHomeBankingId() + ")"; // bot_job_id, assuming BotJobDTO has an ID
        try (Statement stmt = getConnection().createStatement()) {
            stmt.executeUpdate(insertSQL);
            ARLogger.getInstance(PerformDataBase.class)
                    .info(String.format("BotJob data saved successfully.\n BotJobId: %d", nextId));
            return nextId;
        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("createNewBotJob - \nError: %s", e.getMessage()));
            return -1;
        }
    }

    public boolean updateInstructionsSplitter(
            List<com.allinweb.ch.component.model.InstructionDTO> instructions, int originalBlockId, int newBlockId) {
        // Build the SQL update statement

        try (Statement stmt = getConnection().createStatement()) {
            for (com.allinweb.ch.component.model.InstructionDTO instruction : instructions) {

                String updateSQL = "UPDATE instruction SET  "
                        + " instruction_order_number = " + instruction.getInstructionOrderNumber() + ","
                        + " block_id = " + newBlockId
                        + " WHERE id = " + instruction.getInstructionId()
                        + " and block_id = " + originalBlockId;

                int rowsAffected = stmt.executeUpdate(updateSQL);
                if (rowsAffected > 0) {
                } else {
                    ARLogger.getInstance(PerformDataBase.class)
                            .warning(String.format(
                                    "updateInstructionsSplitter - No matching record found to update blockId: ",
                                    originalBlockId));
                }
            }
            return true;
        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "This '%s' \n cannot be updated.\nError: %s", originalBlockId, e.getMessage()));
        }
        return false;
    }

    public boolean rowsUpdateName(List<com.allinweb.ch.component.model.InstructionDTO> instructions) {
        // Build the SQL update statement
        try (Statement stmt = getConnection().createStatement()) {
            for (com.allinweb.ch.component.model.InstructionDTO instruction : instructions) {

                String updateSQL = "UPDATE instruction SET  "
                        + " name = '" + instruction.getInstructionName() + "',"
                        + " actions = '" + instruction.getActions() + "'"
                        + " WHERE id = " + instruction.getInstructionId()
                        + " and block_id = " + instruction.getBlockId();

                int rowsAffected = stmt.executeUpdate(updateSQL);
                if (rowsAffected > 0) {
                    ARLogger.getInstance(PerformDataBase.class)
                            .warning(String.format(
                                    "RowsUpdateName - InstructionId: %s now have name: %s",
                                    instruction.getInstructionId(), instruction.getInstructionName()));
                } else {
                    ARLogger.getInstance(PerformDataBase.class)
                            .warning(String.format(
                                    "UpdateMoveRowsOrder - No matching record found to update InstructionId: %d and name: %s",
                                    instruction.getInstructionId(), instruction.getInstructionName()));
                }
            }
            return true;
        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("This Instruction\n cannot be updated.\nError: %s", e.getMessage()));
        }
        return false;
    }

    public boolean updateMoveRowsOrder(List<com.allinweb.ch.component.model.InstructionDTO> instructions) {
        // Build the SQL update statement
        try (Statement stmt = getConnection().createStatement()) {
            for (com.allinweb.ch.component.model.InstructionDTO instruction : instructions) {

                String updateSQL = "UPDATE instruction SET  "
                        + " instruction_order_number = " + instruction.getInstructionOrderNumber() + ","
                        + " block_id = " + instruction.getBlockId()
                        + " WHERE id = " + instruction.getInstructionId();

                int rowsAffected = stmt.executeUpdate(updateSQL);
                if (rowsAffected > 0) {
                    ARLogger.getInstance(PerformDataBase.class)
                            .warning(String.format(
                                    "UpdateMoveRowsOrder - InstructionId: %s now have order number: %d",
                                    instruction.getInstructionId(), instruction.getInstructionOrderNumber()));
                } else {
                    ARLogger.getInstance(PerformDataBase.class)
                            .warning(String.format(
                                    "UpdateMoveRowsOrder - No matching record found to update blockId: %d and InstructionId: %d",
                                    instruction.getBlockId(), instruction.getInstructionId()));
                }
            }

            return true;
        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "This Order Number for Instructions\n cannot be updated.\nError: %s", e.getMessage()));
        }
        return false;
    }

    public void rollBackBlocksRows(RollBackBlocksDTO rollBackBlocksDTO) {
        // Build the SQL update statement

        try (Statement stmt = getConnection().createStatement()) {
            for (com.allinweb.ch.component.model.InstructionDTO instruction : rollBackBlocksDTO.getInstructions()) {

                String updateSQL = "UPDATE instruction SET  "
                        + " instruction_order_number = " + instruction.getInstructionOrderNumber() + ","
                        + " block_id = " + rollBackBlocksDTO.getBlockId()
                        + " WHERE id = " + instruction.getInstructionId();

                int rowsAffected = stmt.executeUpdate(updateSQL);
                if (rowsAffected > 0) {
                    ARLogger.getInstance(PerformDataBase.class)
                            .warning(String.format(
                                    "RollBackBlocks - InstructionId %d for blockId: %d updated successfully",
                                    instruction.getInstructionId(), rollBackBlocksDTO.getBlockId()));

                } else {
                    ARLogger.getInstance(PerformDataBase.class)
                            .warning(String.format(
                                    "RollBackBlocks - No matching record found to update InstructionId %d for blockId: %d",
                                    instruction.getInstructionId(), rollBackBlocksDTO.getBlockId()));
                }
            }
        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "This BlockId '%d' \n cannot be updated.\nError: %s",
                            rollBackBlocksDTO.getBlockId(), e.getMessage()));
            return;
        }
    }

    public void rollBackBlocksOrder(RollBackBlocksDTO rollBackBlocksDTO) {
        // Build the SQL update statement

        try (Statement stmt = getConnection().createStatement()) {

            String updateSQL = "UPDATE block SET  "
                    + " block_order_number = " + 1
                    + " WHERE id = " + rollBackBlocksDTO.getBlockId()
                    + " and bot_job_id = " + rollBackBlocksDTO.getBotJobId();

            int rowsAffected = stmt.executeUpdate(updateSQL);
            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .warning(String.format(
                                "rollBackBlocksOrder - Block Order Reset for blockId: %d - Name: %s",
                                rollBackBlocksDTO.getBlockId(), rollBackBlocksDTO.getBlockName()));
            } else {
                ARLogger.getInstance(PerformDataBase.class)
                        .warning(String.format(
                                "RollBackBlocks - No matching record found to update for blockId: %d - Name: %s",
                                rollBackBlocksDTO.getBlockId(), rollBackBlocksDTO.getBlockName()));
            }
        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "This BlockId '%d' - Name: %s \n cannot be updated.\nError: %s",
                            rollBackBlocksDTO.getBlockId(), rollBackBlocksDTO.getBlockName(), e.getMessage()));
            return;
        }
    }

    public static List<com.allinweb.ch.component.model.InstructionDTO> getBlockLoopInstructionIdsWithNullBlock(
            int botJobId) {
        // List to store IDs of block loop instructions where block_id is null
        List<com.allinweb.ch.component.model.InstructionDTO> instructions = new ArrayList<>();

        // SQL query to select instruction IDs where block_id is null
        String selectSQL = "SELECT i.id FROM instruction i " + " WHERE i.block_id IS NULL";

        // Try-with-resources to handle the SQL statement and result set
        try (Statement stmt = getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery(selectSQL);

            // Iterate through the result set and add each ID to the list
            while (rs.next()) {
                com.allinweb.ch.component.model.InstructionDTO instructionDTO =
                        new com.allinweb.ch.component.model.InstructionDTO();
                instructionDTO.setInstructionId(rs.getInt("id"));
                instructionDTO.setBlockId(-1);
                instructions.add(instructionDTO);
            }

        } catch (SQLException e) {
            // Log the error if any SQL exception occurs
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error fetching block loop instruction IDs with null block_id for botJobId %d. Error: %s",
                            botJobId, e.getMessage()));
        }

        // Return the list of block loop instruction IDs
        return instructions;
    }

    public boolean deleteBlock(int botJobId, int blockId) {
        // Build the SQL delete statement
        try (Statement stmt = getConnection().createStatement()) {

            String deleteSQL = "DELETE FROM block " + " WHERE id = " + blockId + " and bot_job_id = " + botJobId;

            // Execute the update statement and check if any rows were affected
            int rowsAffected = stmt.executeUpdate(deleteSQL);
            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format(
                                "The Block id %d has been successfully deleted from botJobId %d.", blockId, botJobId));
            } else {
                ARLogger.getInstance(PerformDataBase.class)
                        .warning(String.format(
                                "No matching record found for blockId ID %d in botJobId %d.", blockId, botJobId));
            }

            return true;

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error deleting BotJobId ID %d from block ID %d. Error: %s",
                            botJobId, blockId, e.getMessage()));
        }
        return false;
    }

    private Integer loadNextIdBotJobData() {
        //        String selectSQL = "SELECT NEXT_VAL fROM homeBankingSeq";
        String selectSQL = "SELECT MAX(ID) AS max_id FROM bot_job";
        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(selectSQL)) {
            while (rs.next()) {
                return rs.getInt("max_id");
            }
        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("loadNextIdBotJobData  \nError: " + e.getMessage());
        }
        return null;
    }

    private Integer loadNextIdBlockData() {
        //        String selectSQL = "SELECT NEXT_VAL fROM homeBankingSeq";
        String selectSQL = "SELECT MAX(ID) AS max_id FROM block";
        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(selectSQL)) {
            while (rs.next()) {
                return rs.getInt("max_id");
            }
        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("loadNextIdBlockData  \nError: " + e.getMessage());
        }
        return null;
    }

    public int loadNextBlockOrderNumber(int botJobId) {
        //        String selectSQL = "SELECT NEXT_VAL fROM homeBankingSeq";
        String selectSQL = "SELECT COUNT(*) AS quantity FROM block WHERE bot_job_id = " + botJobId;
        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(selectSQL)) {
            while (rs.next()) {
                return rs.getInt("quantity");
            }
        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("loadNextIdBlockData  \nError: " + e.getMessage());
        }
        return -1;
    }

    public List<BotJobLoadDTO> loadBotJobComplete(int botJobId) {
        String query = "SELECT bj.home_banking_id, bj.id AS bot_job_id, bj.name AS bot_job_name, "
                + " b.id AS block_id, b.block_order_number, b.name AS block_name, "
                + " b.description AS block_description, b.type_id, "
                + " bli.id AS instruction_id, bli.instruction_order_number, "
                + " bli.actions, bli.name AS instruction_name, bli.path, bli.coordinates,  bli.iframe_xpath, "
                + " bli.description AS instruction_description, bli.force_coordinates, "
                + " bli.optional, bli.block_marked, bli.default_value, bli.action_custom_max_wait_sec, "
                + " bli.on_hold_seconds, bli.codified, bli.export_to_abr, "
                + " irl.reference_type, irl.value, "
                + "  bli.operation, bli.parent_id, "
                + "  b.export_file, "
                + "  b.active as block_active, b.wait, "
                + "  bli.active as instruction_active "
                + " FROM bot_job bj "
                + " LEFT JOIN block b ON b.bot_job_id = bj.id "
                + " JOIN instruction bli ON bli.block_id = b.id "
                + " LEFT JOIN reference irl ON irl.instruction_id = bli.id "
                + " where bj.id = " + botJobId
                + "  ORDER BY bj.id, b.block_order_number, bli.instruction_order_number, irl.id ASC";

        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            Map<Integer, BotJobLoadDTO> botJobMap = new HashMap<>();
            Map<Integer, BlockLoadDTO> blockMap = new HashMap<>();
            Map<Integer, InstructionLoadDTO> instructionMap = new HashMap<>();

            botJobLoadList.clear();

            while (rs.next()) {
                botJobId = rs.getInt("bot_job_id");
                BotJobLoadDTO botJobDTO = botJobMap.get(botJobId);

                if (botJobDTO == null) {
                    botJobDTO = new BotJobLoadDTO();
                    botJobDTO.setId(botJobId);
                    botJobDTO.setHomeBankingId(rs.getInt("home_banking_id"));
                    botJobDTO.setName(rs.getString("bot_job_name"));
                    botJobDTO.setBlockLoadDTOList(new ArrayList<>());
                    botJobMap.put(botJobId, botJobDTO);
                    botJobLoadList.add(botJobDTO);
                }

                int blockId = rs.getInt("block_id");
                BlockLoadDTO blockDTO = blockMap.get(blockId);

                if (blockDTO == null) {
                    blockDTO = new BlockLoadDTO();
                    blockDTO.setId(blockId);
                    blockDTO.setBlockOrderNumber(rs.getInt("block_order_number"));
                    blockDTO.setName(rs.getString("block_name"));
                    blockDTO.setDescription(rs.getString("block_description"));
                    blockDTO.setTypeId(rs.getInt("type_id"));
                    blockDTO.setActive(rs.getBoolean("block_active"));
                    blockDTO.setWait(rs.getInt("wait"));
                    blockDTO.setBotJobId(botJobDTO.getId());
                    blockDTO.setBotJobName(botJobDTO.getName());
                    blockDTO.setExportFile(rs.getString("export_file"));

                    blockDTO.setInstructionLoadDTOS(new ArrayList<>());
                    botJobDTO.getBlockLoadDTOList().add(blockDTO);
                    blockMap.put(blockId, blockDTO);
                }

                int instructionId = rs.getInt("instruction_id");
                InstructionLoadDTO instruction = instructionMap.get(instructionId);

                if (instruction == null) {
                    instruction = new InstructionLoadDTO();
                    instruction.setId(instructionId);
                    instruction.setInstructionOrderNumber(rs.getInt("instruction_order_number"));
                    instruction.setActions(rs.getString("actions"));
                    instruction.setName(rs.getString("instruction_name"));
                    instruction.setPath(rs.getString("path"));
                    instruction.setCoordinates(rs.getString("coordinates"));
                    instruction.setForceCoordinates(rs.getBoolean("force_coordinates"));
                    instruction.setIFrameXPath(rs.getString("iframe_xpath"));
                    instruction.setDescription(rs.getString("instruction_description"));
                    instruction.setOptional(rs.getBoolean("optional"));
                    instruction.setBlockMarked(rs.getBoolean("block_marked"));
                    instruction.setDefaultValue(rs.getString("default_value"));
                    instruction.setActionCustomMaxWaitSec(rs.getInt("action_custom_max_wait_sec"));
                    instruction.setOnHoldSeconds(rs.getInt("on_hold_seconds"));
                    instruction.setCodified(rs.getBoolean("codified"));
                    instruction.setExportToABR(rs.getBoolean("export_to_abr"));
                    instruction.setOperation(rs.getString("operation"));
                    instruction.setParentId(rs.getInt("parent_id"));
                    instruction.setInstructionActive(rs.getBoolean("instruction_active"));

                    instruction.setInstructionReferenceLoadDTOList(new ArrayList<>());
                    blockDTO.getInstructionLoadDTOS().add(instruction);
                    instructionMap.put(instructionId, instruction);
                }

                String referenceType = rs.getString("reference_type");
                if (referenceType != null) {
                    InstructionReferenceLoadDTO reference = new InstructionReferenceLoadDTO();
                    reference.setReferenceType(referenceType);
                    reference.setValue(rs.getString("value"));
                    instruction.getInstructionReferenceLoadDTOList().add(reference);
                }
            }
        } catch (SQLException e) {
            ARLogger.getInstance(Thread.class)
                    .severe(String.format(
                            "Error loadBotJobWithBlock for botJobId %d\nError: %s", botJobId, e.getMessage()));
        }

        return botJobLoadList;
    }

    //    public static List<BotJobLoadDTO> loadBlockAll(int botJobId) {
    //        String query = "SELECT bj.id AS bot_job_id, bj.name AS bot_job_name, "
    //                + " b.id AS block_id, b.block_order_number, b.name AS block_name, "
    //                + " b.description AS block_description, b.type_id, "
    //                + " bli.id AS instruction_id, bli.instruction_order_number, "
    //                + " bli.actions, bli.name AS instruction_name, bli.path, bli.coordinates, bli.iframe_xpath,
    // bli.description AS
    // instruction_description, "
    //                + " bli.optional, bli.block_marked, bli.default_value, bli.action_custom_max_wait_sec, "
    //                + " bli.on_hold_seconds, bli.codified, bli.export_to_abr, "
    //                + " irl.reference_type, irl.value, "
    //                + "  bli.operation, bli.parent_id, "
    //                + "  b.export_file, b.active, b.wait "
    //                + " FROM bot_job bj "
    //                + " LEFT JOIN block b ON b.bot_job_id = bj.id "
    //                + " JOIN instruction bli ON bli.block_id = b.id "
    //                + " LEFT JOIN reference irl ON irl.instruction_id = bli.id "
    //                + " where bot_job_id = " + botJobId
    //                + "  ORDER BY bj.id, b.block_order_number, bli.instruction_order_number, irl.id ASC";
    //
    //        try (Statement stmt = getConnection().createStatement();
    //             ResultSet rs = stmt.executeQuery(query)) {
    //
    //            Map<Integer, BotJobLoadDTO> botJobMap = new HashMap<>();
    //            Map<Integer, BlockLoadDTO> blockMap = new HashMap<>();
    //            Map<Integer, InstructionLoadDTO> instructionMap = new HashMap<>();
    //
    //            botJobLoadList.clear();
    //
    //            while (rs.next()) {
    //                botJobId = rs.getInt("bot_job_id");
    //                BotJobLoadDTO botJobDTO = botJobMap.get(botJobId);
    //
    //                if (botJobDTO == null) {
    //                    botJobDTO = new BotJobLoadDTO();
    //                    botJobDTO.setId(botJobId);
    //                    botJobDTO.setName(rs.getString("bot_job_name"));
    //                    botJobDTO.setBlockLoadDTOList(new ArrayList<>());
    //                    botJobMap.put(botJobId, botJobDTO);
    //                    botJobLoadList.add(botJobDTO);
    //                }
    //
    //                int blockId = rs.getInt("block_id");
    //                BlockLoadDTO blockDTO = blockMap.get(blockId);
    //
    //                if (blockDTO == null) {
    //                    blockDTO = new BlockLoadDTO();
    //                    blockDTO.setId(blockId);
    //                    blockDTO.setBlockOrderNumber(rs.getInt("block_order_number"));
    //                    blockDTO.setName(rs.getString("block_name"));
    //                    blockDTO.setDescription(rs.getString("block_description"));
    //                    blockDTO.setTypeId(rs.getInt("type_id"));
    //                    blockDTO.setActive(rs.getBoolean("active"));
    //                    blockDTO.setWait(rs.getInt("wait"));
    //                    blockDTO.setExportFile(rs.getString("export_file"));
    //                    blockDTO.setBotJobId(botJobDTO.getId());
    //                    blockDTO.setBotJobName(botJobDTO.getName());
    //
    //                    blockDTO.setInstructionLoadDTOS(new ArrayList<>());
    //                    botJobDTO.getBlockLoadDTOList().add(blockDTO);
    //                    blockMap.put(blockId, blockDTO);
    //                }
    //
    //                int instructionId = rs.getInt("instruction_id");
    //                InstructionLoadDTO instruction = instructionMap.get(instructionId);
    //
    //                if (instruction == null) {
    //                    instruction = new InstructionLoadDTO();
    //                    instruction.setId(instructionId);
    //                    instruction.setInstructionOrderNumber(rs.getInt("instruction_order_number"));
    //                    instruction.setActions(rs.getString("actions"));
    //                    instruction.setName(rs.getString("instruction_name"));
    //                    instruction.setPath(rs.getString("path"));
    //                    instruction.setCoordinates(rs.getString("coordinates"));
    //                    instruction.setForceCoordinates(rs.getBoolean("force_coordinates"));
    //                    instruction.setIFrameXPath(rs.getString("iframe_xpath"));
    //                    instruction.setDescription(rs.getString("instruction_description"));
    //                    instruction.setOptional(rs.getBoolean("optional"));
    //                    instruction.setBlockMarked(rs.getBoolean("block_marked"));
    //                    instruction.setDefaultValue(rs.getString("default_value"));
    //                    instruction.setActionCustomMaxWaitSec(rs.getInt("action_custom_max_wait_sec"));
    //                    instruction.setOnHoldSeconds(rs.getInt("on_hold_seconds"));
    //                    instruction.setCodified(rs.getBoolean("codified"));
    //                    instruction.setExportToAR(rs.getBoolean("export_to_abr"));
    //                     instruction.setInstructionActive(rs.getBoolean("active"));
    //                    instruction.setOperation(rs.getString("operation"));
    //                    instruction.setParentId(rs.getInt("parent_id"));
    //
    //                    instruction.setInstructionReferenceLoadDTOList(new ArrayList<>());
    //                    blockDTO.getInstructionLoadDTOS().add(instruction);
    //                    instructionMap.put(instructionId, instruction);
    //                }
    //
    //                String referenceType = rs.getString("reference_type");
    //                if (referenceType != null) {
    //                    InstructionReferenceLoadDTO reference = new InstructionReferenceLoadDTO();
    //                    reference.setReferenceType(referenceType);
    //                    reference.setValue(rs.getString("value"));
    //                    instruction.getInstructionReferenceLoadDTOList().add(reference);
    //                }
    //            }
    //        } catch (SQLException e) {
    //            ARLogger.getInstance(PerformDataBase.class).severe("loadBlockAll Error: " + e.getMessage());
    //        }
    //
    //        return botJobLoadList;
    //    }

    //    private void addInstruction(
    //            String name, String operation, Integer variableId, Integer parentId, RowMoveDTO rowMoveDTO) {
    //
    //        // Create and show alert inside Platform.runLater
    //        Platform.runLater(() -> {
    //            // Create a label to display the instruction
    //            javafx.scene.control.Label newInstruction = new Label("\"" + name + "\" -> \"" + operation + "\"");
    //            newInstruction.setStyle("-fx-font-size: 18px; -fx-text-fill: red;");;
    //
    //            StackPane stackPane = new StackPane(newInstruction);
    //            stackPane.setPadding(new Insets(20));
    //
    //            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, null, ButtonType.YES, ButtonType.NO);
    //            alert.setHeaderText("Are you sure you want to Add the Instruction to the Bot-Job?");
    //            alert.getDialogPane().setContent(stackPane);
    //
    //            Optional<ButtonType> result = alert.showAndWait();
    //            if (result.isPresent() && result.get() == ButtonType.YES) {
    //                List<BlockLoopInstructionDTO> instructionList = null;
    //                BotJobDTO botJob =
    //                        getEntityById(BotJobDTO.class, rowMoveDTO.getBotJobId());
    //
    //                List<BlockDTO> matchingBlocks = null;
    //                if (rowMoveDTO != null && rowMoveDTO.getUpdatedRows().size() > 0) {
    //                    int targetBlockId = rowMoveDTO.getUpdatedRows().get(0).getBlockId();
    //
    //                    matchingBlocks = botJob.getBlocks().stream()
    //                            .filter(block -> block.getId() == targetBlockId)
    //                            .collect(Collectors.toList());
    //
    //                    if (!matchingBlocks.isEmpty()) {
    //                        instructionList = matchingBlocks.get(0).getBlockLoopInstructions();
    //                    } else {
    //                        instructionList = botJob.getBlocks().get(0).getBlockLoopInstructions();
    //                    }
    //                }
    //
    //                List<BlockLoopInstructionDTO> finalInstructionList = instructionList;
    //                List<BlockDTO> finalMatchingBlocks = matchingBlocks;
    //
    //                Task<Void> waitTask = new Task<>() {
    //                    @Override
    //                    protected Void call() throws Exception {
    //                        try {
    //                            BlockLoopInstructionDTO instruction = new BlockLoopInstructionDTO();
    //                            instruction.setName(name);
    //                            instruction.setDescription("loop desc");
    //                            instruction.setOperation(operation);
    //                            instruction.setVariableId(variableId);
    //                            instruction.setParentId(parentId);
    //                            instruction.setCodified(false);
    //                            instruction.setExportToAR(true);
    //                            if (rowMoveDTO != null
    //                                    && rowMoveDTO.getUpdatedRows().size() > 0) {
    //                                instruction.setInstructionOrderNumber(
    //                                        rowMoveDTO.getUpdatedRows().get(0).getInstructionOrderNumber());
    //                            } else {
    //                                instruction.setInstructionOrderNumber(finalInstructionList.size());
    //                            }
    //                            instruction.setOptional(false);
    //                            if (name.equalsIgnoreCase("setValue")) {
    //                                instruction.setActions(ARConstants.SET_VALUE);
    //                            } else if (name.equalsIgnoreCase("getValue")) {
    //                                instruction.setActions(ARConstants.GET_VALUE);
    //                            } else if (name.equalsIgnoreCase("check")) {
    //                                instruction.setActions(ARConstants.CHECK_VALUE);
    //                            } else if (name.equalsIgnoreCase("ExcelWrite")) {
    //                                instruction.setActions(ARConstants.EXTRACT_FIELD);
    //                            } else if (name.equalsIgnoreCase("GoTo")) {
    //                                instruction.setActions(ARConstants.GOTO);
    //                            } else if (name.equalsIgnoreCase("IF")) {
    //                                instruction.setActions(ARConstants.IF);
    //                            }
    //                            instruction.setActionCustomMaxWaitSec(30);
    //                            instruction.setOnHoldSeconds(1);
    //                            if (finalMatchingBlocks != null) {
    //                                instruction.setBlock(finalMatchingBlocks.get(0));
    //                            } else {
    //                                instruction.setBlock(botJob.getBlocks().get(0));
    //                            }
    //                            instruction.setExportToAR(false);
    //
    //                            // Wrap the persistence in a try-catch block
    //                            try {
    //                                addEntity(instruction, BlockLoopInstructionDTO.class);
    //                            } catch (Exception e) {
    //                                System.err.println("Error while saving instruction: " + e.getMessage());
    //                                System.out.println(e.getMessage());
    //                            }
    //
    //                            // Move the UI update to the JavaFX Application Thread
    //                            Platform.runLater(() -> {
    //                                new ARAlertScene(
    //                                        Alert.AlertType.INFORMATION,
    //                                        "Instruction Added",
    //                                        "Instruction " + instruction.getName() + " has been added successfully",
    //                                        ButtonType.OK);
    //                            });
    //                        } catch (Exception ex) {
    //                            ex.printStackTrace(); // Handle any exception
    //                        }
    //                        return null;
    //                    }
    //                };
    //                new Thread(waitTask).start();
    //            }
    //        });
    //    }

    public boolean reorderInstructions(List<com.allinweb.ch.component.model.InstructionDTO> rowList) {
        int orderNumber = 1;

        // Iterate through the list and update the instructionOrderNumber
        for (com.allinweb.ch.component.model.InstructionDTO instruction : rowList) {
            instruction.setInstructionOrderNumber(orderNumber);
            orderNumber++; // Increment the order number for the next instruction
        }

        // Build the SQL update statement
        try (Statement stmt = getConnection().createStatement()) {
            // Loop through each instruction in the rowList
            for (com.allinweb.ch.component.model.InstructionDTO instruction : rowList) {
                // Increment the instructionOrderNumber by 1 for each instruction
                String updateSQL = "UPDATE instruction SET  "
                        + " instruction_order_number = " + instruction.getInstructionOrderNumber()
                        + " WHERE id = " + instruction.getInstructionId()
                        + " AND block_id = " + instruction.getBlockId();

                int rowsAffected = stmt.executeUpdate(updateSQL);
                if (rowsAffected > 0) {
                    //                    ARLogger.getInstance(PerformDataBase.class)
                    //                            .info(String.format(
                    //                                    "preInsertStep - InstructionId: %s in BlockId: %s now has
                    // order number: %d",
                    //                                    instruction.getInstructionId(),
                    //                                    instruction.getBlockId(),
                    //                                    instruction.getInstructionOrderNumber() + 1));
                } else {
                    ARLogger.getInstance(PerformDataBase.class)
                            .warning(String.format(
                                    "preInsertStep - No matching record found for BlockId: %d and InstructionId: %d",
                                    instruction.getBlockId(), instruction.getInstructionId()));
                }
            }

            return true;
        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("Error updating instruction order numbers.\nError: %s", e.getMessage()));
        }
        return false;
    }

    public int deleteBotJob(int botJobId) {
        // Build the SQL delete statement
        try (Statement stmt = getConnection().createStatement()) {

            // Saved Blocks
            int rowsAffected = stmt.executeUpdate("DELETE FROM component_reference where bot_job_id = " + botJobId);
            rowsAffected += stmt.executeUpdate("DELETE FROM component_instruction where bot_job_id = " + botJobId);
            rowsAffected += stmt.executeUpdate("DELETE FROM component_block where bot_job_id = " + botJobId);

            rowsAffected += stmt.executeUpdate("DELETE FROM variable where bot_job_id = " + botJobId);

            rowsAffected += stmt.executeUpdate("DELETE FROM reference where bot_job_id = " + botJobId);
            rowsAffected += stmt.executeUpdate("DELETE FROM complex_instruction where bot_job_id = " + botJobId);

            rowsAffected += stmt.executeUpdate("DELETE FROM instruction where bot_job_id = " + botJobId);

            rowsAffected += stmt.executeUpdate("DELETE FROM block " + " WHERE bot_job_id = " + botJobId);

            String deleteSQL = "DELETE FROM bot_job " + " WHERE id = " + botJobId;

            // Execute the update statement and check if any rows were affected
            rowsAffected += stmt.executeUpdate(deleteSQL);
            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format("The Bot Job  id %d has been successfully deleted!", botJobId));
            } else {
                ARLogger.getInstance(PerformDataBase.class)
                        .warning(String.format("No matching record found for botJobId %d.", botJobId));
            }
            return rowsAffected;
        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("Error deleting BotJobId ID %d. Error: %s", botJobId, e.getMessage()));
            return -1;
        }
    }

    public boolean updateBotJobNme(int botJobId, String name, String description) {
        // Build the SQL delete statement
        try (Statement stmt = getConnection().createStatement()) {

            String updateSQL = "UPDATE bot_job set name = '" + name + "', description = '" + description
                    + "' WHERE id = " + botJobId;

            // Execute the update statement and check if any rows were affected
            int rowsAffected = stmt.executeUpdate(updateSQL);
            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format("The Bot Job  id %d has been successfully updated!", botJobId));
            } else {
                ARLogger.getInstance(PerformDataBase.class)
                        .warning(String.format("No matching record found for botJobId %d.", botJobId));
            }
            return true;
        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("Error updating BotJobId ID %d. Error: %s", botJobId, e.getMessage()));
        }
        return false;
    }

    public BlockLoadDTO getBlockByBlockId(int botJobId, int blockId) {
        // List to store the fetched instructions
        BlockLoadDTO blockLoadDTO = new BlockLoadDTO();

        // Build the SQL query statement
        String querySQL = "SELECT * FROM block WHERE block_id = " + blockId + " and bot_job_id = " + botJobId;

        // Execute the query and process the result set
        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(querySQL)) {

            while (rs.next()) {
                // Assuming you have an Instruction class, populate it with data from the ResultSet
                blockLoadDTO.setId(rs.getInt("id"));
                blockLoadDTO.setBotJobId(botJobId);
                blockLoadDTO.setActive(rs.getBoolean("active"));
                blockLoadDTO.setBlockOrderNumber(rs.getInt("block_order_number"));
                blockLoadDTO.setDescription(rs.getString("description"));
                blockLoadDTO.setExportFile(rs.getString("export_file"));
                blockLoadDTO.setTypeId(rs.getInt("type"));
                blockLoadDTO.setBlockOrderNumber(rs.getInt("wait"));
            }

            ARLogger.getInstance(PerformDataBase.class)
                    .info(String.format("Fetched Block \"%s\"", blockLoadDTO.getName()));

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error fetching Block ID %d with BotJob Id %d. Error: %s: ",
                            blockId, botJobId, e.getMessage()));
        }

        return blockLoadDTO;
    }

    public List<com.allinweb.ch.component.model.InstructionDTO> getInstructionsByBlockId(int botJobId, int blockId) {
        // List to store the fetched instructions
        List<com.allinweb.ch.component.model.InstructionDTO> instructions = new ArrayList<>();

        // Build the SQL query statement
        String querySQL =
                "SELECT * FROM instruction WHERE block_id = " + blockId + " order by instruction_order_number ASC";

        // Execute the query and process the result set
        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(querySQL)) {

            while (rs.next()) {
                // Assuming you have an Instruction class, populate it with data from the ResultSet
                com.allinweb.ch.component.model.InstructionDTO instruction =
                        new com.allinweb.ch.component.model.InstructionDTO();
                instruction.setInstructionId(rs.getInt("id"));
                instruction.setInstructionName(rs.getString("name"));
                instruction.setInstructionOrderNumber(rs.getInt("instruction_order_number"));
                instruction.setBlockId(rs.getInt("block_id"));
                instruction.setBlockOrderNumber(instruction.getBlockOrderNumber());
                instruction.setBotJobId(botJobId);

                instruction.setActions(rs.getString("actions"));
                instruction.setPath(rs.getString("path"));
                instruction.setCoordinates(rs.getString("coordinates"));
                instruction.setForceCoordinates(rs.getBoolean("force_coordinates"));
                instruction.setIFrameXPath(rs.getString("iframe_xpath"));
                instruction.setDescription(rs.getString("description"));
                instruction.setOptional(rs.getBoolean("optional"));
                instruction.setActionCustomMaxWaitSec(rs.getInt("action_custom_max_wait_sec"));
                instruction.setOnHoldSeconds(rs.getInt("on_hold_seconds"));
                instruction.setCodified(rs.getBoolean("codified"));
                instruction.setExportToABR(rs.getBoolean("export_to_abr"));
                instruction.setInstructionActive(rs.getBoolean("active"));

                // Add the instruction to the list
                instructions.add(instruction);
            }

            ARLogger.getInstance(PerformDataBase.class)
                    .info(String.format("Fetched %d instructions for Block ID %d:", instructions.size(), blockId));

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error fetching instructions for Block ID %d. Error: %s: ", blockId, e.getMessage()));
        }

        return instructions;
    }

    public List<BotJobLoadDTO> loadAllBotJobs() {

        this.botJobLoadList.clear();
        String query = "SELECT bj.id AS bot_job_id, bj.name AS bot_job_name, "
                + "bj.description AS bot_job_description, bj.priority AS bot_job_priority, "
                + "bj.home_banking_id, "
                + "hb.url AS home_banking_url, "
                + "hb.name AS home_banking_name, "
                + "hb.priority AS home_banking_priority, hb.search_config, "
                + "hb.options_config, hb.cookies, hb.driver_session, "
                + "hb.username, hb.password "
                + "FROM bot_job bj "
                + "LEFT JOIN home_banking hb ON bj.home_banking_id = hb.id "
                + "ORDER BY bj.id ASC";

        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                BotJobLoadDTO botJobDTO = new BotJobLoadDTO();

                // Map BotJobLoadDTO fields
                botJobDTO.setId(rs.getInt("bot_job_id"));
                botJobDTO.setName(rs.getString("bot_job_name"));
                botJobDTO.setDescription(rs.getString("bot_job_description"));
                botJobDTO.setPriority(rs.getString("bot_job_priority"));
                botJobDTO.setHomeBankingId(rs.getInt("home_banking_id"));

                // Map HomeBankingLoadDTO fields if home banking details exist
                Integer homeBankingId = rs.getObject("home_banking_id", Integer.class);
                if (homeBankingId != null) {
                    HomeBankingLoadDTO homeBankingDTO = new HomeBankingLoadDTO();
                    homeBankingDTO.setId(rs.getInt("home_banking_id"));
                    homeBankingDTO.setUrl(rs.getString("home_banking_url"));
                    homeBankingDTO.setName(rs.getString("home_banking_name"));
                    homeBankingDTO.setPriority(rs.getString("home_banking_priority"));
                    homeBankingDTO.setSearchConfig(rs.getString("search_config"));
                    homeBankingDTO.setOptionsConfig(rs.getString("options_config"));
                    homeBankingDTO.setCookies(rs.getString("cookies"));
                    homeBankingDTO.setDriverSession(rs.getString("driver_session"));
                    homeBankingDTO.setUsername(rs.getString("username"));
                    homeBankingDTO.setPassword(rs.getString("password"));

                    botJobDTO.setHomeBankingLoadDTO(homeBankingDTO);
                }

                this.botJobLoadList.add(botJobDTO);
            }
        } catch (SQLException e) {
            ARLogger.getInstance(Thread.class).severe(String.format("Error loadAllBotJobs\nError: %s", e.getMessage()));
        }

        return this.botJobLoadList;
    }

    public List<BotJobLoadDTO> loadBotJobAndBlocks(int botJobId) {
        String query = "SELECT bj.id AS bot_job_id, bj.name AS bot_job_name, "
                + " b.id AS block_id, b.block_order_number, b.name AS block_name, "
                + " b.description AS block_description, b.type_id,"
                + " b.active, b.wait"
                + " FROM bot_job bj "
                + " LEFT JOIN block b ON b.bot_job_id = bj.id "
                + " where bot_job_id = " + botJobId
                + "  ORDER BY bj.id, b.block_order_number ASC";

        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            Map<Integer, BotJobLoadDTO> botJobMap = new HashMap<>();
            Map<Integer, BlockLoadDTO> blockMap = new HashMap<>();

            botJobLoadList.clear();

            while (rs.next()) {
                botJobId = rs.getInt("bot_job_id");
                BotJobLoadDTO botJobDTO = botJobMap.get(botJobId);

                if (botJobDTO == null) {
                    botJobDTO = new BotJobLoadDTO();
                    botJobDTO.setId(botJobId);
                    botJobDTO.setName(rs.getString("bot_job_name"));
                    botJobDTO.setBlockLoadDTOList(new ArrayList<>());
                    botJobMap.put(botJobId, botJobDTO);
                    botJobLoadList.add(botJobDTO);
                }

                int blockId = rs.getInt("block_id");
                BlockLoadDTO blockDTO = blockMap.get(blockId);

                if (blockDTO == null) {
                    blockDTO = new BlockLoadDTO();
                    blockDTO.setId(blockId);
                    blockDTO.setBlockOrderNumber(rs.getInt("block_order_number"));
                    blockDTO.setName(rs.getString("block_name"));
                    blockDTO.setDescription(rs.getString("block_description"));
                    blockDTO.setTypeId(rs.getInt("type_id"));
                    blockDTO.setActive(rs.getBoolean("active"));
                    blockDTO.setWait(rs.getInt("wait"));

                    blockDTO.setBotJobId(botJobDTO.getId());
                    blockDTO.setBotJobName(botJobDTO.getName());

                    blockDTO.setInstructionLoadDTOS(new ArrayList<>());
                    botJobDTO.getBlockLoadDTOList().add(blockDTO);
                    blockMap.put(blockId, blockDTO);
                }
            }
        } catch (SQLException e) {
            ARLogger.getInstance(Thread.class)
                    .severe(String.format(
                            "Error loadJustJobBlocks for botJobId %d\nError: %s", botJobId, e.getMessage()));
        }

        return botJobLoadList;
    }

    public List<String> loadAllActionsPerBlock(List<BlockLoadDTO> blockLoadDTOList) {
        List<String> actionsList = new ArrayList<>();

        // Construct the SQL query with the dynamic WHERE clause
        // Loop through the list of BlockLoadDTO and create a set of unique keys
        for (BlockLoadDTO blockDTO : blockLoadDTOList) {

            String query = "SELECT actions FROM instruction "
                    + " WHERE block_id = " + blockDTO.getId()
                    + " and  bot_job_id = " + blockDTO.getBotJobId();

            try (Statement stmt = getConnection().createStatement();
                    ResultSet rs = stmt.executeQuery(query)) {

                // Iterate through the result set and add actions to the list
                while (rs.next()) {
                    actionsList.add(rs.getString("actions"));
                }

            } catch (SQLException e) {
                ARLogger.getInstance(PerformDataBase.class)
                        .severe(String.format("Error loading actions for blocks. Error: %s", e.getMessage()));
            }
        }
        // Return the filtered list of actions
        return actionsList;
    }

    public boolean updateInstructionStatus(com.allinweb.ch.component.model.InstructionDTO instruction) {
        // Build the SQL update statement
        try (Statement stmt = getConnection().createStatement()) {
            int rowsAffected = 0;

            if (instruction.getActions().equals("IF")
                    || instruction.getActions().equals("ELSEIF")
                    || instruction.getActions().equals("ELSE")
                    || instruction.getActions().equals("ENDIF")) {
                rowsAffected = stmt.executeUpdate(
                        "UPDATE instruction SET active = '" + instruction.getInstructionActive() + "'"
                                + " WHERE "
                                + " block_id = " + instruction.getBlockId() + " AND parent_id = "
                                + instruction.getParentId());
            } else {

                String updateSQL = "UPDATE instruction SET active = '" + instruction.getInstructionActive() + "'"
                        + " WHERE id = " + instruction.getInstructionId()
                        + " and block_id = " + instruction.getBlockId();

                rowsAffected = stmt.executeUpdate(updateSQL);
            }
            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .warning(String.format(
                                "RowsUpdateName - InstructionId: %s now have name: %s",
                                instruction.getInstructionId(), instruction.getInstructionName()));
            } else {
                ARLogger.getInstance(PerformDataBase.class)
                        .warning(String.format(
                                "UpdateMoveRowsOrder - No matching record found to update InstructionId: %d and name: %s",
                                instruction.getInstructionId(), instruction.getInstructionName()));
            }
            return true;
        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("This Instruction\n cannot be updated.\nError: %s", e.getMessage()));
        }
        return false;
    }

    public boolean updateInstructionStatusByBlock(int botJobId, int blockId, boolean blockActive) {
        // Build the SQL update statement
        try (Statement stmt = getConnection().createStatement()) {
            int rowsAffected = 0;

            rowsAffected = stmt.executeUpdate("UPDATE instruction SET active = '" + blockActive + "'"
                    + " WHERE "
                    + " block_id = " + blockId + " AND bot_job_id = " + botJobId);

            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format("Instruction Status Updated - rowsAffected: %s ", rowsAffected));
            } else {
                ARLogger.getInstance(PerformDataBase.class).warning("No Instruction Status were Updated!");
            }
            return true;
        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("This Instruction\n cannot be updated.\nError: %s", e.getMessage()));
        }
        return false;
    }

    public void updateBlockStatus(int botJobId, int blockId, String blockName, boolean blockActive, int wait) {
        try (Statement stmt = getConnection().createStatement()) {

            // Update each block's block_order_number starting from 1
            String updateSQL = "UPDATE block SET active = '" + blockActive + "',"
                    + " wait = " + wait
                    + " WHERE id = " + blockId
                    + " and bot_job_id = " + botJobId;

            int rowsAffected = stmt.executeUpdate(updateSQL);

            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format(
                                "Block Status updated blockId: %s, name: %s, Active: %s",
                                blockId, blockName, blockActive));
            } else {
                ARLogger.getInstance(PerformDataBase.class)
                        .warning(String.format(
                                "updateBlockStatus - No matching record found to update botJobId: %d blockId: %d",
                                botJobId, blockId));
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("Error updateBlockStatus. Error: %s", e.getMessage()));
        }
    }

    public BotJobLoadDTO loadBotJobById(int botJobId) {
        // SQL query to get the blocks for a specific bot job
        String query = "SELECT bj.id, "
                + " bj.name, "
                + " bj.description, "
                + " bj.home_banking_id, "
                + " bj.priority "
                + " FROM bot_job bj "
                + " WHERE bj.id = " + botJobId;

        // Initialize the necessary data structures

        // Use Statement to execute the query
        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(query)) {
            BotJobLoadDTO botJobLoadDTO = new BotJobLoadDTO();

            while (rs.next()) {
                botJobLoadDTO = new BotJobLoadDTO();

                botJobLoadDTO.setId(rs.getInt("id"));
                botJobLoadDTO.setName(rs.getString("name"));
                botJobLoadDTO.setDescription(rs.getString("description"));
                botJobLoadDTO.setPriority(rs.getString("priority"));
                botJobLoadDTO.setHomeBankingId(rs.getInt("home_banking_id"));
            }
            return botJobLoadDTO;

        } catch (SQLException e) {
            ARLogger.getInstance(Thread.class)
                    .severe(String.format("Error loadBotJob for botJobId %d\nError: %s", botJobId, e.getMessage()));
        }

        return null;
    }

    private void loadBotJobComplex(BotJobDTO botJob) {
        String selectSQL =
                " SELECT bot.ID botId, bot.Name botName, blk.ID blockId, blk.Name blockName, blk.block_order_number, "
                        + " blockInstr.id blockInstrId, blockInstr.instruction_order_number instructionOrderNumber, blockInstr.actions, "
                        + " instr.id instId, instr.reference_type, instr.value"
                        + " FROM reference instr "
                        + " join instruction blockInstr on blockInstr.id = instr.instruction_id"
                        + " join bot_job bot on bot.id = " + botJob.getId()
                        + " join block blk on blk.bot_job_id = bot.id "
                        + " order by blockInstr.id, blockInstr.instruction_order_number, instr.id";
        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(selectSQL)) {

            List<ReferenceDTO> instructions = new ArrayList<>();

            while (rs.next()) {
                String botId = rs.getString("botId");
                String botName = rs.getString("botName");
                String blockId = rs.getString("blockId");
                String blockName = rs.getString("blockName");
                String blockOrderNumber = rs.getString("block_order_number");

                String blockInstrId = rs.getString("blockInstrId");
                String instructionOrderNumber = rs.getString("instructionOrderNumber");
                String actions = rs.getString("actions");

                String instId = rs.getString("instId");
                String referenceType = rs.getString("reference_type");
                String value = rs.getString("value");

                if (botJob.getId() == Integer.parseInt(botId)) {
                    for (BlockDTO block : botJob.getBlocks()) {
                        if (block.getId() == Integer.parseInt(blockId)) {
                            boolean exist = false;
                            for (InstructionDTO blockInstruction : block.getBlockLoopInstructionDTOS()) {
                                if (blockInstruction.getId() == Integer.parseInt(blockInstrId)) {
                                    for (ReferenceDTO instructionReference :
                                            blockInstruction.getInstructionReferenceDTOList()) {
                                        if (instructionReference.getId() == Integer.parseInt(instId)
                                                && instructionReference
                                                        .getReferenceType()
                                                        .equalsIgnoreCase(referenceType)
                                                && instructionReference
                                                        .getValue()
                                                        .equalsIgnoreCase(value)) {
                                            exist = true;
                                            break;
                                        }
                                    }
                                    if (!exist) {
                                        ReferenceDTO inst = new ReferenceDTO();
                                        inst.setId(Integer.parseInt(instId));
                                        inst.setReferenceType(referenceType);
                                        inst.setValue(value);
                                        instructions.add(inst);
                                        break;
                                    }
                                }
                                if (exist) {
                                    break;
                                }
                            }
                        }
                    }
                }

                //                System.out.println(String.format(
                //                        "%s  %s  %s  %s  %s   %s   %s   %s",
                //                        botId, botName, blockId, blockName, blockOrderNumber, referenceType, value));

                //               databaseUserDto = new DatabaseUserDTO(
                //                        id, jobs, name, url, priority, searchConfig, optionsConfig, username,
                // password);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        //        jobUserList.clear();
        //        loadBotJobData();
    }

    public List<BlockLoadDTO> loadBlocksByBotJobId(int botJobId) {
        // SQL query to get the blocks for a specific bot job
        String query = "SELECT " + "b.id AS block_id, "
                + "b.block_order_number, "
                + "b.name AS block_name, "
                + "b.description AS block_description, "
                + "b.type_id, "
                + "bj.id AS bot_job_id, "
                + "bj.name AS bot_job_name "
                + "FROM bot_job bj "
                + "JOIN block b ON b.bot_job_id = bj.id "
                + "WHERE bj.id = "
                + botJobId + " " + // Use the botJobId directly in the query string
                "ORDER BY b.block_order_number ASC";

        // Initialize the necessary data structures
        blockLoadList.clear();
        Map<Integer, BlockLoadDTO> blockMap = new HashMap<>();

        // Use Statement to execute the query
        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                // Load the Block information
                int blockId = rs.getInt("block_id");
                BlockLoadDTO blockDTO = blockMap.get(blockId);

                if (blockDTO == null) {
                    blockDTO = new BlockLoadDTO();
                    blockDTO.setId(blockId);
                    blockDTO.setBlockOrderNumber(rs.getInt("block_order_number"));
                    blockDTO.setName(rs.getString("block_name"));
                    blockDTO.setDescription(rs.getString("block_description"));
                    blockDTO.setTypeId(rs.getInt("type_id"));
                    blockDTO.setBotJobId(rs.getInt("bot_job_id"));
                    blockDTO.setBotJobName(rs.getString("bot_job_name"));

                    blockMap.put(blockId, blockDTO);
                    blockLoadList.add(blockDTO);
                }
            }
        } catch (SQLException e) {
            ARLogger.getInstance(Thread.class)
                    .severe(String.format(
                            "Error loadBlocksForBotJob for botJobId %d\nError: %s", botJobId, e.getMessage()));
        }

        return blockLoadList;
    }

    public int insertInstruction(InstructionLoadDTO instructionDTO, Integer currentBotJobId, Integer currentBlockId) {

        boolean isPostgres = POSTGRES_DB;

        try (Statement stmt = getConnection().createStatement()) {
            Integer nextId = instructionDTO.getId() == null ? loadNextIdInstructionData() + 1 : instructionDTO.getId();
            instructionDTO.setId(nextId);

            StringBuilder columns = new StringBuilder("id");
            StringBuilder values = new StringBuilder(nextId.toString());

            // Helper method to add columns and values
            BiConsumer<String, Object> addColumnValue = (column, value) -> {
                if (value != null) {
                    columns.append(", ").append(column);
                    if (value instanceof String) {
                        values.append(", '")
                                .append(((String) value).replace("'", "''"))
                                .append("'");
                    } else {
                        values.append(", ").append(value);
                    }
                }
            };

            // Add non-boolean fields
            addColumnValue.accept("coordinates", instructionDTO.getCoordinates());
            addColumnValue.accept("iframe_xpath", instructionDTO.getIFrameXPath());
            addColumnValue.accept("path", instructionDTO.getPath());
            addColumnValue.accept("action_custom_max_wait_sec", instructionDTO.getActionCustomMaxWaitSec());
            addColumnValue.accept("actions", instructionDTO.getActions());
            addColumnValue.accept("default_value", instructionDTO.getDefaultValue());
            addColumnValue.accept("description", instructionDTO.getDescription());
            addColumnValue.accept("instruction_order_number", instructionDTO.getInstructionOrderNumber());
            addColumnValue.accept("name", instructionDTO.getName());
            addColumnValue.accept(
                    "on_hold_seconds",
                    instructionDTO.getOnHoldSeconds() != null ? instructionDTO.getOnHoldSeconds() : 1);
            addColumnValue.accept("operation", instructionDTO.getOperation());
            addColumnValue.accept("parent_id", instructionDTO.getParentId());
            addColumnValue.accept("variable_id", instructionDTO.getVariableId());
            addColumnValue.accept("block_id", currentBlockId);
            addColumnValue.accept("bot_job_id", currentBotJobId);

            // Add boolean fields with conditional logic
            if (instructionDTO.getBlockMarked() != null) {
                if (isPostgres) {
                    addColumnValue.accept("block_marked", instructionDTO.getBlockMarked());
                } else if (instructionDTO.getBlockMarked()) {
                    addColumnValue.accept("block_marked", 1);
                }
            }

            if (instructionDTO.getCodified() != null) {
                if (isPostgres) {
                    addColumnValue.accept("codified", instructionDTO.getCodified());
                } else if (instructionDTO.getCodified()) {
                    addColumnValue.accept("codified", 1);
                }
            }

            if (instructionDTO.getExportToABR() != null) {
                if (isPostgres) {
                    addColumnValue.accept("export_to_abr", instructionDTO.getExportToABR());
                } else if (instructionDTO.getExportToABR()) {
                    addColumnValue.accept("export_to_abr", 1);
                }
            }

            if (instructionDTO.getOptional() != null) {
                if (isPostgres) {
                    addColumnValue.accept("optional", instructionDTO.getOptional());
                } else if (instructionDTO.getOptional()) {
                    addColumnValue.accept("optional", 1);
                }
            }

            if (instructionDTO.getInstructionActive() != null) {
                if (isPostgres) {
                    addColumnValue.accept("active", instructionDTO.getInstructionActive());
                } else if (instructionDTO.getInstructionActive()) {
                    addColumnValue.accept("active", 1);
                }
            }

            if (instructionDTO.getExecuted() != null) {
                if (isPostgres) {
                    addColumnValue.accept("executed", instructionDTO.getExecuted());
                } else if (instructionDTO.getExecuted()) {
                    addColumnValue.accept("executed", 1);
                }
            }

            if (instructionDTO.getBlockActive() != null) {
                if (isPostgres) {
                    addColumnValue.accept("block_active", instructionDTO.getBlockActive());
                } else if (instructionDTO.getBlockActive()) {
                    addColumnValue.accept("block_active", 1);
                }
            }

            if (instructionDTO.getRefreshLoop() != null) {
                if (isPostgres) {
                    addColumnValue.accept("refresh_loop", instructionDTO.getRefreshLoop());
                } else if (instructionDTO.getRefreshLoop()) {
                    addColumnValue.accept("refresh_loop", 1);
                }
            }

            if (instructionDTO.getLoopOnly() != null) {
                if (isPostgres) {
                    addColumnValue.accept("loop_only", instructionDTO.getLoopOnly());
                } else if (instructionDTO.getLoopOnly()) {
                    addColumnValue.accept("loop_only", 1);
                }
            }

            if (instructionDTO.getForceCoordinates() != null) {
                if (isPostgres) {
                    addColumnValue.accept("force_coordinates", instructionDTO.getForceCoordinates());
                } else if (instructionDTO.getForceCoordinates()) {
                    addColumnValue.accept("force_coordinates", 1);
                }
            }

            //            if (instructionDTO.getEditMode() != null) {
            //                if (isPostgres) {
            //                    addColumnValue.accept("edit_mode", instructionDTO.getEditMode());
            //                } else if (instructionDTO.getEditMode()) {
            //                    addColumnValue.accept("edit_mode", 1);
            //                }
            //            }

            // Construct final SQL query
            String insertSQL = String.format("INSERT INTO instruction (%s) VALUES (%s)", columns, values);

            int rowsAffected = stmt.executeUpdate(insertSQL);
            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format(
                                "New Instruction SAVED SUCCESSFULLY id: %d Name: %s Actions: %s Operation: %s",
                                instructionDTO.getId(),
                                instructionDTO.getName(),
                                instructionDTO.getActions(),
                                instructionDTO.getOperation()));
                return nextId;

            } else {
                ARLogger.getInstance(PerformDataBase.class)
                        .warning(String.format(
                                "Instruction NOT SAVED\nid: %d Name: %s Actions: %s Operations: %s",
                                instructionDTO.getId(),
                                instructionDTO.getName(),
                                instructionDTO.getActions(),
                                instructionDTO.getOperation()));
                return -1;
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .warning(String.format(
                            "Instruction NOT SAVED\nid: %d Name: %s Actions: %s Operations: %s",
                            instructionDTO.getId(),
                            instructionDTO.getName(),
                            instructionDTO.getActions(),
                            instructionDTO.getOperation()));
            return -1;
        }
    }

    private Integer loadNextIdInstructionData() {
        //        String selectSQL = "SELECT NEXT_VAL fROM homeBankingSeq;
        String selectSQL = "SELECT MAX(ID) AS max_id FROM instruction";
        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(selectSQL)) {
            while (rs.next()) {
                return rs.getInt("max_id");
            }
        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("loadNextIdInstructionData  \nError: " + e.getMessage());
        }
        return null;
    }

    public boolean preInsertStep(RowMoveDTO rowMoveDTO, List<com.allinweb.ch.component.model.InstructionDTO> rowList) {
        // Check if the operation type is either "INSERT_BEFORE" or "INSERT_AFTER"
        String operationType = rowMoveDTO.getType();
        if ("INSERT_BEFORE".equals(operationType)
                || "INSERT_AFTER".equals(operationType)
                || "INSERT_AFTER_ELSEIF".equals(operationType)) {
            // Get the instruction order number from the first instruction in the updated rows
            int targetOrderNumber = rowMoveDTO.getUpdatedRows().get(0).getInstructionOrderNumber();

            // Check if the targetOrderNumber exists in the rowList
            boolean orderNumberExists = rowList.stream()
                    .anyMatch(instruction -> instruction.getInstructionOrderNumber() == targetOrderNumber);

            if (!orderNumberExists) {
                // If the target order number doesn't exist, return false without shifting
                ARLogger.getInstance(PerformDataBase.class)
                        .warning(String.format(
                                "preInsertStep - Target order number %d does not exist in the row list.",
                                targetOrderNumber));
                return false;
            }

            // Build the SQL update statement
            try (Statement stmt = getConnection().createStatement()) {
                // Loop through each instruction in the rowList
                for (com.allinweb.ch.component.model.InstructionDTO instruction : rowList) {
                    // For "INSERT_BEFORE", shift instructions with an order number greater than or equal to the target
                    // For "INSERT_AFTER", shift instructions with an order number strictly greater than the target
                    boolean shouldShift = ("INSERT_BEFORE".equals(operationType))
                            ? instruction.getInstructionOrderNumber() >= targetOrderNumber
                            : instruction.getInstructionOrderNumber() > targetOrderNumber;

                    if (shouldShift) {
                        // Increment the instructionOrderNumber by 1 for each instruction
                        String updateSQL = "UPDATE instruction SET  "
                                + " instruction_order_number = " + (instruction.getInstructionOrderNumber() + 1)
                                + " WHERE id = " + instruction.getInstructionId()
                                + " AND block_id = " + instruction.getBlockId();

                        int rowsAffected = stmt.executeUpdate(updateSQL);
                        if (rowsAffected > 0) {
                            //                            ARLogger.getInstance(PerformDataBase.class)
                            //                                    .info(String.format(
                            //                                            "preInsertStep - InstructionId: %s in BlockId:
                            // %s now has order number: %d",
                            //                                            instruction.getInstructionId(),
                            //                                            instruction.getBlockId(),
                            //                                            instruction.getInstructionOrderNumber() + 1));
                        } else {
                            ARLogger.getInstance(PerformDataBase.class)
                                    .warning(String.format(
                                            "preInsertStep - No matching record found for BlockId: %d and InstructionId: %d",
                                            instruction.getBlockId(), instruction.getInstructionId()));
                        }
                    }
                }
                return true;
            } catch (SQLException e) {
                ARLogger.getInstance(PerformDataBase.class)
                        .severe(String.format("Error updating instruction order numbers.\nError: %s", e.getMessage()));
            }
        }
        return false;
    }

    public int preFillInstruction(
            String name,
            String description,
            String actions,
            String operation,
            Integer onHold,
            Integer varId,
            Integer instructionId,
            Integer parentId,
            RowMoveDTO rowMoveDTO,
            BotJobLoadDTO botJob,
            boolean isShowAlert) {

        this.botJobLoadDTO = loadBotJobById(rowMoveDTO.getBotJobId());

        List<com.allinweb.ch.component.model.InstructionDTO> rowList =
                getInstructionsByBlockId(rowMoveDTO.getBotJobId(), rowMoveDTO.getBlockId());

        reorderInstructions(rowList);

        preInsertStep(rowMoveDTO, rowList);

        List<InstructionLoadDTO> instructionList = null;
        List<BlockLoadDTO> matchingBlocks = null;

        this.botJobLoadList = loadBotJobAndBlocks(rowMoveDTO.getBotJobId());
        this.blockLoadList = loadBlocksByBotJobId(rowMoveDTO.getBotJobId());

        if (rowMoveDTO != null && rowMoveDTO.getUpdatedRows().size() > 0) {

            Integer targetBlockId = -1;

            if (rowMoveDTO.getType().equals("INSERT_NEW")) {
                targetBlockId = rowMoveDTO.getBlockOrderNumber();

                Integer finalTargetBlockId = targetBlockId;
                matchingBlocks = blockLoadList.stream()
                        .filter(block -> block.getBlockOrderNumber().equals(finalTargetBlockId))
                        .collect(Collectors.toList());

            } else {
                targetBlockId = rowMoveDTO.getBlockId();
                Integer finalTargetBlockId1 = targetBlockId;
                matchingBlocks = blockLoadList.stream()
                        .filter(block -> block.getId().equals(finalTargetBlockId1))
                        .collect(Collectors.toList());
            }
        }

        List<BlockLoadDTO> finalMatchingBlocks = matchingBlocks;
        List<com.allinweb.ch.component.model.InstructionDTO> finalInstructionList = rowList;

        InstructionLoadDTO instruction = new InstructionLoadDTO();

        instruction.setName(name);

        instruction.setCodified(false);
        instruction.setExportToABR(false);
        instruction.setInstructionActive(true);
        if (rowMoveDTO != null && rowMoveDTO.getUpdatedRows().size() > 0) {
            if ("INSERT_BEFORE".equals(rowMoveDTO.getType())) {
                instruction.setInstructionOrderNumber(
                        rowMoveDTO.getUpdatedRows().get(0).getInstructionOrderNumber());
            } else {
                instruction.setInstructionOrderNumber(
                        rowMoveDTO.getUpdatedRows().get(0).getInstructionOrderNumber() + 1);
            }
        } else {
            instruction.setInstructionOrderNumber(finalMatchingBlocks.size() + 1);
        }
        instruction.setOptional(false);

        instruction.setOperation(operation);
        instruction.setActions(actions);
        instruction.setDescription(description);

        instruction.setVariableId(varId);

        Integer nextId = loadNextIdInstructionData() + 1;

        if (actions.equalsIgnoreCase(ARConstants.IF)) {
            instruction.setId(nextId);
            instruction.setParentId(nextId);
        } else if (actions.equalsIgnoreCase(ARConstants.ELSE)) {
            instruction.setId(nextId);
            instruction.setParentId(parentId);
        } else if (actions.equalsIgnoreCase(ARConstants.ENDIF)) {
            instruction.setId(nextId);
            instruction.setParentId(parentId);
        } else if (actions.equalsIgnoreCase(ARConstants.ELSEIF)) {
            instruction.setId(nextId);
            instruction.setParentId(parentId);
        } else {
            instruction.setId(nextId);
            instruction.setParentId(instructionId);
        }

        instruction.setActionCustomMaxWaitSec(30);
        instruction.setOnHoldSeconds(onHold);

        // Define where to get the BojJobId
        if (finalMatchingBlocks != null && finalMatchingBlocks.size() > 0) {
            instruction.setBlockId(finalMatchingBlocks.get(0).getId());
        } else if (botJob != null
                && botJob.getBlockLoadDTOList() != null
                && botJob.getBlockLoadDTOList().size() > 0) {
            instruction.setBlockId(botJob.getBotJobId());
        } else {

            BlockDetailsDTO newBlockDetails = new BlockDetailsDTO();
            newBlockDetails.setBlockName("Default Block");
            newBlockDetails.setBlockDescription("Default Block description");
            newBlockDetails.setTypeId(1);
            newBlockDetails.setActive(true);
            newBlockDetails.setWait(3);

            newBlockDetails.setBotJobId(rowMoveDTO.getBotJobId());
            newBlockDetails.setBlockId(rowMoveDTO.getBlockId());

            int newBlockId = createNewBlock(newBlockDetails);

            if (newBlockId > 0) {

                // IT SETS THE NEW TARGET IN CASE TO ADD MORE INSTRUCTIONS
                rowMoveDTO.setBlockId(newBlockId);

                this.blockLoadList = loadBlocksByBotJobId(rowMoveDTO.getBotJobId());

                instruction.setBlockId(newBlockId);
            }
        }
        instruction.setExportToABR(false);
        instruction.setInstructionActive(true);
        // Wrap the persistence in a try-catch block
        int response;

        try {
            Integer currentBlockId = rowMoveDTO.getBlockId();

            if (instruction.getBlockId() != null && !instruction.getBlockId().equals(currentBlockId)) {
                currentBlockId = instruction.getBlockId();
            }
            response = insertInstruction(instruction, botJob.getId(), currentBlockId);

            int targetOrderNumber = rowMoveDTO.getUpdatedRows().get(0).getInstructionOrderNumber();
            rowMoveDTO.getUpdatedRows().get(0).setInstructionOrderNumber(targetOrderNumber + 1);

            int finalResponse = response;
            Platform.runLater(() -> {
                if (isShowAlert) {
                    if (finalResponse > -1) {

                        ARLogger.getInstance(PerformDataBase.class)
                                .info(String.format(
                                        "\"Component\" Instruction: \"%s\"\nhas been added successfully!",
                                        instruction.getName()));
                    } else {

                        ARLogger.getInstance(PerformDataBase.class)
                                .severe(String.format(
                                        "Error Add New \"Component\" Instruction: \"%s\"\nCannot be saved!",
                                        instruction.getName()));
                    }
                }
            });

            if (response > -1) {
                return nextId;
            }

        } catch (Exception e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Cannot Insert Instruction\nError: " + e.getMessage());
        }

        return -1;
    }

    public static HomeBankingLoadDTO loadHomeBanking(int homeBankingId) {
        HomeBankingLoadDTO homeBanking = null;

        try (Statement stmt = getConnection().createStatement()) {

            // Select the home banking record based on homeBankingId
            String selectSQL =
                    "SELECT id, cookies, driver_session, name, options_config, password, priority, search_config, url, username "
                            + "FROM home_banking WHERE id = " + homeBankingId;

            ResultSet rs = stmt.executeQuery(selectSQL);

            // Check if the result set contains a record
            if (rs.next()) {
                // Create HomeBankingLoadDTO object and set its fields based on the result set
                homeBanking = new HomeBankingLoadDTO();
                homeBanking.setId(rs.getInt("id"));
                homeBanking.setCookies(rs.getString("cookies"));
                homeBanking.setDriverSession(rs.getString("driver_session"));
                homeBanking.setName(rs.getString("name"));
                homeBanking.setOptionsConfig(rs.getString("options_config"));
                homeBanking.setPassword(rs.getString("password"));
                homeBanking.setPriority(rs.getString("priority"));
                homeBanking.setSearchConfig(rs.getString("search_config"));
                homeBanking.setUrl(rs.getString("url"));
                homeBanking.setUsername(rs.getString("username"));
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error selecting home banking record with ID %d. Error: %s",
                            homeBankingId, e.getMessage()));
        }
        return homeBanking;
    }

    public ObservableList<VariableUserDTO> loadJobVariables(int botJobId) {
        variablesList.clear();
        String selectSQL = " SELECT vars.id, vars.type, vars.name, vars.value, COUNT(blk.variable_id) UsedVars "
                + " FROM variable vars "
                + " left join instruction blk on blk.variable_id = vars.id "
                + " where blk.bot_job_id = " + botJobId
                //                                + " and  instruction_id = " + instructionId
                + " group by vars.id, vars.type, vars.Name, vars.value ";
        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(selectSQL)) {
            while (rs.next()) {
                int id = rs.getInt("ID");
                String type = rs.getString("type");
                String name = rs.getString("name");
                String value = rs.getString("value");
                String usedVars = rs.getString("UsedVars");
                variablesList.add(new VariableUserDTO(id, type, name, value, botJobId, -1, usedVars));
            }
        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("loadJobVariables  \nError: " + e.getMessage());
        }

        return variablesList;
    }

    public ObservableList<ComboBoxVars> loadWebPageFields(int botJobId) {
        webPageItems.clear();
        String selectSQL = " SELECT  "
                + "  bj.id AS bot_job_id,  "
                + "  b.id AS block_id,  "
                + "  bli.id AS instruction_id,  "
                + "  bli.instruction_order_number,  "
                + "  bli.actions,  "
                + "  bli.name AS instruction_name,  "
                + "  bli.path,  "
                + "  bli.operation      "
                + " FROM bot_job bj  "
                + " LEFT JOIN block b ON b.bot_job_id = bj.id  "
                + " JOIN instruction bli ON bli.block_id = b.id  "
                + " where bj.id = " + botJobId
                + "   and operation is null  "
                + "  ORDER BY bj.id, b.block_order_number, bli.instruction_order_number ASC;";

        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(selectSQL)) {
            while (rs.next()) {
                int id = rs.getInt("instruction_id");
                String name = rs.getString("instruction_name").trim();
                String actions = rs.getString("actions").trim();
                Integer blockId = rs.getInt("block_id");

                // Filter out "SET", "GET", "CK", adn "H"
                if (actions != null
                        && !actions.equalsIgnoreCase(ARConstants.SET_VALUE)
                        && !actions.equalsIgnoreCase(ARConstants.GET_VALUE)
                        && !actions.equalsIgnoreCase(ARConstants.CHECK_VALUE)
                        && !actions.equalsIgnoreCase(ARConstants.HOLD)) {
                    webPageItems.add(new ComboBoxVars("(" + id + ")" + name, name, id, blockId));
                }
            }
        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "loadWebPageFields - Error selecting Web Page Fields. Error: %s", e.getMessage()));
        }
        return webPageItems;
    }

    // Migration Scripts
    public int migrationScriptsv2_1f() {
        // Build the SQL update statement
        try (Statement stmt = getConnection().createStatement()) {
            int rowsAffected = 0;

            if (POSTGRES_DB) {

                // Update the bot_job_id in instruction using the bot_job_id from block
                String updateSQL = "UPDATE instruction\n" + "                SET bot_job_id = (\n"
                        + "                        SELECT b.bot_job_id\n"
                        + "                FROM block AS b\n"
                        + "                WHERE b.id = instruction.block_id\n"
                        + ");";

                rowsAffected = stmt.executeUpdate(updateSQL);

                // Update the bot_job_id in reference using the instruction_id from
                // instruction
                updateSQL = "UPDATE reference AS ref "
                        + "SET bot_job_id = (SELECT bli.bot_job_id FROM instruction AS bli WHERE bli.id = ref.instruction_id);";

                rowsAffected += stmt.executeUpdate(updateSQL);

                // Update the bot_job_id in complex_instruction using the instruction_id from
                // instruction
                updateSQL = "UPDATE complex_instruction AS com "
                        + "SET bot_job_id = (SELECT bli.bot_job_id FROM instruction AS bli WHERE bli.id = com.instruction_id);";

                rowsAffected += stmt.executeUpdate(updateSQL);

                // Update All Active on instruction
                updateSQL = "UPDATE instruction " + "SET active = true;";

                rowsAffected += stmt.executeUpdate(updateSQL);

                // Update All Active on block
                updateSQL = "UPDATE block " + "SET active = true;";

                rowsAffected += stmt.executeUpdate(updateSQL);
            } else {
                // Update the bot_job_id in instruction using the bot_job_id from block
                String updateSQL = "UPDATE instruction AS bli "
                        + "SET bli.bot_job_id = (SELECT b.bot_job_id FROM block AS b WHERE b.id = bli.block_id);";

                rowsAffected = stmt.executeUpdate(updateSQL);

                // Update the bot_job_id in reference using the instruction_id from
                // instruction
                updateSQL = "UPDATE reference AS ref "
                        + "SET ref.bot_job_id = (SELECT bli.bot_job_id FROM instruction AS bli WHERE bli.id = ref.instruction_id);";

                rowsAffected += stmt.executeUpdate(updateSQL);

                // Update the bot_job_id in complex_instruction using the instruction_id from
                // instruction
                updateSQL = "UPDATE complex_instruction AS com "
                        + "SET com.bot_job_id = (SELECT bli.bot_job_id FROM instruction AS bli WHERE bli.id = com.instruction_id);";

                rowsAffected += stmt.executeUpdate(updateSQL);

                // Update All Active on instruction
                updateSQL = "UPDATE instruction " + "SET active = true;";

                rowsAffected += stmt.executeUpdate(updateSQL);

                // Update All Active on block
                updateSQL = "UPDATE block " + "SET active = true;";

                rowsAffected += stmt.executeUpdate(updateSQL);
            }

            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .warning(String.format("Migration DB Scripts - RowsUpdated - %s", rowsAffected));
            } else {
                ARLogger.getInstance(PerformDataBase.class).info("Migration DB Scripts - No Rows were updated");
            }
            return rowsAffected;
        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).warning("Migration DB Scripts - Error: " + e.getMessage());
        }
        return -1;
    }

    public ErrorMessage dropTablesMigrationScriptsv2_7f() {
        // Build the SQL update statement
        try (Statement stmt = getConnection().createStatement()) {
            int rowsAffected = 0;

            if (POSTGRES_DB) {

                stmt.executeUpdate("DROP TABLE job_run_report;");
                stmt.executeUpdate("DROP TABLE  variable;");
                stmt.executeUpdate("DROP TABLE  instruction_reference;");
                stmt.executeUpdate("DROP TABLE  block_loop_instruction;");

                stmt.executeUpdate("DROP TABLE  saved_instruction_reference;");
                stmt.executeUpdate("DROP TABLE  saved_block_loop_instruction;");
                stmt.executeUpdate("DROP TABLE  saved_blocks;");

                rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"blockLoopInstructionSeq\";");
                rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"blockSeq\";");
                rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"botJobSeq\";");
                rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"variableSeq\";");
                rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"instructionReferenceSeq\";");
                rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"savedInstructionReferenceSeq\";");
                rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"excelReportSeq\";");
                rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"savedInstructionReferenceSeq\";");
                rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"savedBlockLoopInstructionSeq\";");
                rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"complexInstructionSeq\";");
                rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"configurationSeq\";");
                rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"homeBankingSeq\";");
                rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"savedBlockSeq\";");
                rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"idgen\";");

                // Update the bot_job_id in reference using the instruction_id from

            } else {

                stmt.executeUpdate("DROP TABLE job_run_report;");
                stmt.executeUpdate("DROP TABLE  variable;");
                stmt.executeUpdate("DROP TABLE  instruction_reference;");
                stmt.executeUpdate("DROP TABLE  complex_instruction;");
                stmt.executeUpdate("DROP TABLE  block_loop_instruction;");

                stmt.executeUpdate("DROP TABLE  saved_instruction_reference;");
                stmt.executeUpdate("DROP TABLE  saved_block_loop_instruction;");
                stmt.executeUpdate("DROP TABLE  saved_blocks;");

                rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"blockLoopInstructionSeq\";");
                rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"blockSeq\";");
                rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"botJobSeq\";");
                rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"variableSeq\";");
                rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"instructionReferenceSeq\";");
                rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"savedInstructionReferenceSeq\";");
                rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"excelReportSeq\";");
                rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"savedInstructionReferenceSeq\";");
                rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"savedBlockLoopInstructionSeq\";");
                rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"complexInstructionSeq\";");
                rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"configurationSeq\";");
                rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"homeBankingSeq\";");
                rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"savedBlockSeq\";");
                rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"idgen\";");
            }

            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .warning(String.format("Migration DB Scripts - RowsUpdated - %s", rowsAffected));
            } else {
                ARLogger.getInstance(PerformDataBase.class).info("Migration DB Scripts - No Rows were updated");
            }
            return null;
        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class).warning("Migration DB Scripts - Error: " + error.getMessage());
            return new ErrorMessage(
                    "Error Drop Tables Migration 2.7f", "Error dropping OLD objects", error.getMessage());
        }
    }

    public List<VariableLoadDTO> instVariablesToDuplicateOLD(Connection conn, int oldBotJobId, String targetTable)
            throws SQLException {
        String query = "SELECT id, name, type, value, block_loop_instruction_id, bot_job_id "
                + " FROM " + targetTable
                + " WHERE bot_job_id = ? "
                + " ORDER BY id";

        List<VariableLoadDTO> variableDTOList = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, oldBotJobId); // Set the oldBotJobId parameter
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                VariableLoadDTO variableDTO = new VariableLoadDTO();
                variableDTO.setId(rs.getInt("id"));
                variableDTO.setName(rs.getString("name"));
                variableDTO.setType(rs.getString("type"));
                variableDTO.setValue(rs.getString("value"));
                variableDTO.setInstructionId(rs.getInt("block_loop_instruction_id"));
                variableDTO.setBotJobId(rs.getInt("bot_job_id"));

                variableDTOList.add(variableDTO); // Add to the list
            }
        }

        return variableDTOList; // Return the list of variable DTOs
    }

    public List<VariableLoadDTO> instVariablesToDuplicateNEW(
            Connection conn, int oldBotJobId, int oldBlockId, String targetTable) throws SQLException {
        String query = "SELECT var.id, var.name, var.type, var.value, var.instruction_id, var.bot_job_id " + " FROM "
                + targetTable + " var";

        if (oldBlockId > -1) {
            query +=
                    " JOIN instruction bli ON bli.id = var.instruction_id and var.bot_job_id = ? and bli.block_id = ?  ";
        } else {
            query += " WHERE var.bot_job_id = ? ";
        }

        query += " ORDER BY var.id";

        List<VariableLoadDTO> variableDTOList = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(query)) {

            if (oldBlockId > -1) {
                stmt.setInt(1, oldBotJobId);
                stmt.setInt(2, oldBlockId);
            } else {
                stmt.setInt(1, oldBotJobId);
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                VariableLoadDTO variableDTO = new VariableLoadDTO();
                variableDTO.setId(rs.getInt("id"));
                variableDTO.setName(rs.getString("name"));
                variableDTO.setType(rs.getString("type"));
                variableDTO.setValue(rs.getString("value"));
                variableDTO.setInstructionId(rs.getInt("instruction_id"));
                variableDTO.setBotJobId(rs.getInt("bot_job_id"));

                variableDTOList.add(variableDTO); // Add to the list
            }
        }

        return variableDTOList; // Return the list of variable DTOs
    }

    public List<InstructionReferenceLoadDTO> instReferenceToDuplicateOld(
            Connection conn, int oldBotJobId, String targetTable) throws SQLException {
        String query = "SELECT id, reference_type, value, block_loop_instruction_id, bot_job_id "
                + "  FROM " + targetTable
                + "  WHERE bot_job_id = ? "
                + "  ORDER BY id";

        List<InstructionReferenceLoadDTO> referenceDTOList = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, oldBotJobId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                InstructionReferenceLoadDTO referenceDTO = new InstructionReferenceLoadDTO();
                referenceDTO.setId(rs.getInt("id"));
                referenceDTO.setReferenceType(rs.getString("reference_type"));
                referenceDTO.setValue(rs.getString("value"));
                referenceDTO.setBlockLoopInstructionId(rs.getInt("block_loop_instruction_id"));
                referenceDTO.setBotJobId(rs.getInt("bot_job_id"));

                referenceDTOList.add(referenceDTO);
            }
        }

        return referenceDTOList;
    }

    public List<InstructionReferenceLoadDTO> instReferenceToDuplicateNew(
            Connection conn, int oldBotJobId, int oldBlockId, String targetTable) throws SQLException {
        String query = "SELECT ref.id, ref.reference_type, ref.value, ref.instruction_id, ref.bot_job_id " + "  FROM "
                + targetTable + " ref ";

        if (oldBlockId > -1) {
            query +=
                    " JOIN instruction bli ON bli.id = ref.instruction_id and ref.bot_job_id = ? and bli.block_id = ?  ";
        } else {
            query += " WHERE ref.bot_job_id = ? ";
        }

        query += " ORDER BY ref.id";

        List<InstructionReferenceLoadDTO> referenceDTOList = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(query)) {

            if (oldBlockId > -1) {
                stmt.setInt(1, oldBotJobId);
                stmt.setInt(2, oldBlockId);
            } else {
                stmt.setInt(1, oldBotJobId);
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                InstructionReferenceLoadDTO referenceDTO = new InstructionReferenceLoadDTO();
                referenceDTO.setId(rs.getInt("id"));
                referenceDTO.setReferenceType(rs.getString("reference_type"));
                referenceDTO.setValue(rs.getString("value"));
                referenceDTO.setBlockLoopInstructionId(rs.getInt("instruction_id"));
                referenceDTO.setBotJobId(rs.getInt("bot_job_id"));

                referenceDTOList.add(referenceDTO);
            }
        }

        return referenceDTOList;
    }

    public List<ComplexInstructionLoadDTO> instComplexToDuplicate(
            Connection conn, int oldBotJobId, int oldBlockId, String targetTable) throws SQLException {
        String query = "SELECT id, instruction, order_number, way, instruction_id, bot_job_id " + "FROM " + targetTable
                + " cp";

        if (oldBlockId > -1) {
            query += " JOIN instruction bli ON bli.id = cp.instruction_id and cp.bot_job_id = ? and bli.block_id = ?  ";
        } else {
            query += " WHERE cp.bot_job_id = ? ";
        }

        query += " ORDER BY cp.id";

        List<ComplexInstructionLoadDTO> referenceDTOList = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(query)) {

            if (oldBlockId > -1) {
                stmt.setInt(1, oldBotJobId);
                stmt.setInt(2, oldBlockId);
            } else {
                stmt.setInt(1, oldBotJobId);
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                ComplexInstructionLoadDTO complexInstructionDTO = new ComplexInstructionLoadDTO();
                complexInstructionDTO.setId(rs.getInt("id")); // Set the ID from complex_instruction
                complexInstructionDTO.setInstructionId(
                        rs.getInt("instruction_id")); // Set the instruction_id as instructionId
                complexInstructionDTO.setBotJobId(rs.getInt("bot_job_id")); // Set bot_job_id
                complexInstructionDTO.setOrderNumber(rs.getInt("order_number"));
                complexInstructionDTO.setInstruction(rs.getString("instruction"));
                complexInstructionDTO.setWay(rs.getString("way"));

                referenceDTOList.add(complexInstructionDTO);
            }
        }

        return referenceDTOList;
    }

    public List<com.allinweb.ch.component.model.InstructionDTO> instructionsToDuplicate(
            Connection conn, int oldBotJobId, int oldBlockId, String tableTarget) throws SQLException {
        String query =
                "SELECT bli.id, bli.action_custom_max_wait_sec, bli.actions, bli.active, bli.block_marked, bli.codified, bli.default_value, \n"
                        + " bli.description, bli.export_to_abr, bli.instruction_order_number, bli.name, bli.on_hold_seconds, "
                        + " bli.operation, bli.optional, \n"
                        + " bli.parent_id, bli.path, bli.coordinates, bli.iframe_xpath, bli.force_coordinates"
                        + "  bli.variable_id, bli.block_id, bli.bot_job_id, b.block_order_number \n"
                        + " FROM " + tableTarget + " bli \n"
                        + " JOIN block b ON bli.block_id = b.id \n"
                        + " WHERE bli.bot_job_id = ? ";

        if (oldBlockId > -1) {
            query += " and b.id = ? ";
        }
        query += " order by b.block_order_number, bli.instruction_order_number ";
        List<com.allinweb.ch.component.model.InstructionDTO> instructionDTOList = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, oldBotJobId);
            if (oldBlockId > -1) {
                stmt.setInt(2, oldBlockId);
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                com.allinweb.ch.component.model.InstructionDTO instructionDTO =
                        new com.allinweb.ch.component.model.InstructionDTO();
                instructionDTO.setId(rs.getInt("id")); // Holds the Current Ids
                instructionDTO.setActionCustomMaxWaitSec(rs.getInt("action_custom_max_wait_sec"));
                instructionDTO.setActions(rs.getString("actions"));
                instructionDTO.setInstructionActive(rs.getBoolean("active"));
                instructionDTO.setBlockMarked(rs.getBoolean("block_marked"));
                instructionDTO.setCodified(rs.getBoolean("codified"));
                instructionDTO.setDefaultValue(rs.getString("default_value"));
                instructionDTO.setDescription(rs.getString("description"));
                instructionDTO.setExportToABR(rs.getBoolean("export_to_abr"));
                instructionDTO.setInstructionOrderNumber(rs.getInt("instruction_order_number"));
                instructionDTO.setInstructionName(rs.getString("name"));
                instructionDTO.setOnHoldSeconds(rs.getInt("on_hold_seconds"));
                instructionDTO.setOperation(rs.getString("operation"));
                instructionDTO.setOptional(rs.getBoolean("optional"));
                instructionDTO.setParentId(rs.getInt("parent_id"));
                instructionDTO.setPath(rs.getString("path"));
                instructionDTO.setCoordinates(rs.getString("coordinates"));
                instructionDTO.setForceCoordinates(rs.getBoolean("force_coordinates"));
                instructionDTO.setIFrameXPath(rs.getString("iframe_xpath"));
                instructionDTO.setVariableId(rs.getInt("variable_id"));
                instructionDTO.setBlockId(rs.getInt("block_id"));
                instructionDTO.setBotJobId(rs.getInt("bot_job_id"));
                instructionDTO.setBlockOrderNumber(rs.getInt("block_order_number"));
                instructionDTOList.add(instructionDTO);
            }
        }

        return instructionDTOList;
    }

    public ErrorMessage duplicateBotJobById(
            Connection conn,
            int oldBotJobId,
            int newBotJobId,
            String newName,
            String newDescription,
            String[] arrayTables) {

        String botJobInsertQuery = "INSERT INTO bot_job (id, name, description, priority, home_banking_id) "
                + "SELECT ?, ?, ?, priority, home_banking_id FROM bot_job WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(botJobInsertQuery)) {
            stmt.setInt(1, newBotJobId); // Set new name
            stmt.setString(2, newName); // Set new name
            stmt.setString(3, newDescription); // Set new description
            stmt.setInt(4, oldBotJobId); // Set original botJobId for the SELECT query
            stmt.executeUpdate();

            System.out.println("Generated BotJob ID: " + newBotJobId);

            // Now you can proceed with duplicating the related tables
            // arrayTables = {"block", "instruction", "reference", "complex_instruction", "variable"};
            ErrorMessage errorMessage = duplicateRelatedTables(conn, oldBotJobId, newBotJobId, arrayTables);
            if (errorMessage != null) {
                return errorMessage;
            }

            return null;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return new ErrorMessage("Error Duplicating Bot Job", "Bot Job Name", newName);
        }
    }

    public int getMaxId(Connection conn, String tableName) throws SQLException {
        String query = "SELECT MAX(id) FROM " + tableName;
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public ErrorMessage migration2_6f(Connection conn, int oldBotJobId, int newBotJobId, String[] arrayTables)
            throws SQLException {

        Map<Integer, Integer> blocksOlderAndNewId = new HashMap<>();

        List<BlockLoadDTO> blockList = blocksToDuplicate(conn, oldBotJobId);
        if (blockList.size() > 0) {
            // tablesMigration = {"block", "block_loop_instruction", "instruction", "instruction_reference","reference",
            // "variable"};
            // Assuming instList is a List<InstructionDTO> and refersList is a List<InstructionReferenceLoadDTO>
            for (BlockLoadDTO block : blockList) {
                blocksOlderAndNewId.put(block.getId(), block.getId());
                block.setId(block.getId());
                block.setBotJobId(newBotJobId);
            }
        }

        Map<Integer, Integer> parentOlderAndNewId = new HashMap<>();
        Map<Integer, Integer> variableOlderAndNewId = new HashMap<>();

        //  "block", "block_loop_instruction", "instruction", "instruction_reference", "reference", "variable"
        List<com.allinweb.ch.component.model.InstructionDTO> instList = instructionsToDuplicate(
                conn, oldBotJobId, -1, arrayTables[1]); // "block_loop_instruction", "instruction"

        List<VariableLoadDTO> varsList = instVariablesToDuplicateOLD(conn, oldBotJobId, arrayTables[5]);

        // tablesMigration = {"block", "block_loop_instruction", "instruction", "instruction_reference","reference",
        // "variable"};
        if (varsList.size() > 0) {
            for (VariableLoadDTO variable : varsList) {
                if (!variableOlderAndNewId.containsKey(variable.getId())) {
                    variableOlderAndNewId.put(variable.getId(), variable.getId());
                    variable.setId(variable.getId());
                }
            }
        }

        if (instList.size() > 0) {

            // Prepare the Ids
            // tablesMigration = {"block", "block_loop_instruction", "instruction", "instruction_reference","reference",
            // "variable"};
            //            int currentId = getMaxId(conn, arrayTables[2]) + 1;
            for (com.allinweb.ch.component.model.InstructionDTO instruction : instList) {
                instruction.setInstructionId(instruction.getId()); // Holds the News Ids
                instruction.setBotJobId(newBotJobId); // Holds the News Ids

                if (!parentOlderAndNewId.containsKey(instruction.getParentId())) {
                    parentOlderAndNewId.put(instruction.getId(), instruction.getId());
                }

                // Loop through the instList and find a matching InstructionDTO
                for (BlockLoadDTO block : blockList) {
                    if (instruction.getBlockOrderNumber().equals(block.getBlockOrderNumber())) {
                        // Once found, update the blockLoopInstructionId with the new instructionId
                        instruction.setBlockId(block.getId());
                        break; // Exit the inner loop since we've found a match
                    }
                }
                //                currentId++;
            }
            // Duplicate instruction
            // tablesMigration = {"block", "block_loop_instruction", "instruction", "instruction_reference","reference",
            // "variable"};
            ErrorMessage errorMessage = duplicateBlockLoopInstructions(
                    conn, instList, parentOlderAndNewId, variableOlderAndNewId, blocksOlderAndNewId, arrayTables[2]);
            if (errorMessage != null) {
                return errorMessage;
            }

            if (varsList.size() > 0) {

                // Assuming instList is a List<InstructionDTO> and refersList is a List<InstructionReferenceLoadDTO>
                for (VariableLoadDTO variable : varsList) {
                    //                    variable.setId(currentVarId);

                    // Loop through the instList and find a matching InstructionDTO
                    for (com.allinweb.ch.component.model.InstructionDTO instruction : instList) {
                        if (variable.getInstructionId().equals(instruction.getId())) {
                            // Once found, update the blockLoopInstructionId with the new instructionId
                            variable.setInstructionId(instruction.getInstructionId());
                            variable.setBotJobId(newBotJobId);
                            break; // Exit the inner loop since we've found a match
                        }
                    }
                    //                    currentVarId++;
                }

                // Update variables
                // tablesMigration = {"block", "block_loop_instruction", "instruction", "instruction_reference",
                // "reference", "variable"};
                errorMessage = updateVariables(conn, varsList, arrayTables[5]);
                if (errorMessage != null) {
                    return errorMessage;
                }
            }

            // tablesMigration = {"block", "block_loop_instruction", "instruction", "instruction_reference",
            // "reference", "variable"};
            List<InstructionReferenceLoadDTO> refersList =
                    instReferenceToDuplicateOld(conn, oldBotJobId, arrayTables[3]);
            if (refersList.size() > 0) {

                //                currentId = getMaxId(conn, arrayTables[2]) + 1;

                // Assuming instList is a List<InstructionDTO> and refersList is a List<InstructionReferenceLoadDTO>
                for (InstructionReferenceLoadDTO reference : refersList) {
                    //                    reference.setId(currentId++);

                    // Loop through the instList and find a matching InstructionDTO
                    for (com.allinweb.ch.component.model.InstructionDTO instruction : instList) {
                        if (reference.getBlockLoopInstructionId().equals(instruction.getId())) {
                            // Once found, update the blockLoopInstructionId with the new instructionId
                            reference.setBlockLoopInstructionId(instruction.getInstructionId());
                            reference.setBotJobId(newBotJobId);
                            break; // Exit the inner loop since we've found a match
                        }
                    }
                }

                // Duplicate reference
                // tablesMigration = {"block", "block_loop_instruction", "instruction", "instruction_reference",
                // "reference", "variable"};
                errorMessage = duplicateInstructionReferences(conn, refersList, arrayTables[4]);
                if (errorMessage != null) {
                    return errorMessage;
                }
            }
        }
        return null;
    }

    //    public ErrorMessage saveNewComponent(
    //            Connection conn, ComponentBlockDTO componentBlockDTO, DetailsDTO detailsDTO, String[] arrayTables)
    //            throws SQLException {
    //
    //        // component_block / instruction / component_instruction / reference / component_reference / variable /
    //        // component_variable / complex / component_complex
    //        int oldBotJobId = detailsDTO.getNewBlock().getBotJobId();
    //        int oldBlockId = detailsDTO.getNewBlock().getBlockId();
    //        int homeBankId = detailsDTO.getNewBlock().getHomeBankingId();
    //
    //        Map<Integer, Integer> blocksOlderAndNewId = new HashMap<>();
    //
    //        List<BlockLoadDTO> blockList = blockToComponent(conn, oldBotJobId, oldBlockId);
    //        // Duplicate related blocks
    //        if (blockList.size() > 0) {
    //
    //            // component_block / instruction / component_instruction / reference / component_reference / variable
    // /
    //            // component_variable / complex / component_complex
    //            int currentId = getMaxId(conn, arrayTables[0]) + 1; // block  or component_block
    //
    //            // Assuming instList is a List<InstructionDTO> and refersList is a List<InstructionReferenceLoadDTO>
    //            for (BlockLoadDTO block : blockList) {
    //                blocksOlderAndNewId.put(block.getId(), currentId);
    //                block.setId(currentId);
    //                block.setBotJobId(oldBotJobId);
    //                block.setHomeBankingId(homeBankId);
    //                block.setName(componentBlockDTO.getName());
    //                block.setDescription(componentBlockDTO.getDescription());
    //                currentId++;
    //            }
    //            // Duplicate Blocks
    //            // component_block / instruction / component_instruction / reference / component_reference / variable
    // /
    //            // component_variable / complex / component_complex
    //            ErrorMessage errorMessage = duplicateBlocks(conn, blockList, arrayTables[0]);
    //            if (errorMessage != null) {
    //                return errorMessage;
    //            }
    //        }
    //
    //        Map<Integer, Integer> parentOlderAndNewId = new HashMap<>();
    //        Map<Integer, Integer> variableOlderAndNewId = new HashMap<>();
    //
    //        // component_block / instruction / component_instruction / reference / component_reference / variable /
    //        // component_variable / complex / component_complex
    //        List<com.allinweb.ch.component.model.InstructionDTO> instList =
    //                instructionsToDuplicate(conn, oldBotJobId, oldBlockId, arrayTables[1]); // instruction
    //
    //        // component_block / instruction / component_instruction / reference / component_reference / variable /
    //        // component_variable / complex / component_complex
    //        List<VariableLoadDTO> varsList = instVariablesToDuplicateNEW(conn, oldBotJobId, oldBlockId,
    // arrayTables[5]);
    //
    //        // component_block / instruction / component_instruction / reference / component_reference / variable /
    //        // component_variable / complex / component_complex
    //        int currentVarId = getMaxId(conn, arrayTables[6]) + 1;
    //
    //        if (varsList.size() > 0) {
    //            for (VariableLoadDTO variable : varsList) {
    //                if (!variableOlderAndNewId.containsKey(variable.getId())) {
    //                    variableOlderAndNewId.put(variable.getId(), currentVarId);
    //                    variable.setId(currentVarId);
    //                }
    //                currentVarId++;
    //            }
    //        }
    //
    //        if (instList.size() > 0) {
    //
    //            // Prepare the Ids
    //            // component_block / instruction / component_instruction / reference / component_reference / variable
    // /
    //            // component_variable / complex / component_complex
    //            int currentId = getMaxId(conn, arrayTables[2]) + 1;
    //            for (com.allinweb.ch.component.model.InstructionDTO instruction : instList) {
    //                instruction.setInstructionId(currentId); // Holds the News Ids
    //                instruction.setBotJobId(oldBotJobId); // Holds the News Ids
    //
    //                if (!parentOlderAndNewId.containsKey(instruction.getParentId())) {
    //                    parentOlderAndNewId.put(instruction.getId(), currentId);
    //                }
    //
    //                // Loop through the instList and find a matching InstructionDTO
    //                for (BlockLoadDTO block : blockList) {
    //                    if (instruction.getBlockOrderNumber().equals(block.getBlockOrderNumber())) {
    //                        // Once found, update the blockLoopInstructionId with the new instructionId
    //                        instruction.setBlockId(block.getId());
    //                        break; // Exit the inner loop since we've found a match
    //                    }
    //                }
    //                currentId++;
    //            }
    //            // Duplicate instruction
    //            //
    // component_block/instruction/component_instruction/reference/component_reference/variable/component_variable/complex/component_complex
    //            ErrorMessage errorMessage = duplicateBlockLoopInstructions(
    //                    conn, instList, parentOlderAndNewId, variableOlderAndNewId, blocksOlderAndNewId,
    // arrayTables[2]);
    //            if (errorMessage != null) {
    //                return errorMessage;
    //            }
    //
    //            if (varsList.size() > 0) {
    //
    //                // Assuming instList is a List<InstructionDTO> and refersList is a
    // List<InstructionReferenceLoadDTO>
    //                for (VariableLoadDTO variable : varsList) {
    //                    //                    variable.setId(currentVarId);
    //
    //                    // Loop through the instList and find a matching InstructionDTO
    //                    for (com.allinweb.ch.component.model.InstructionDTO instruction : instList) {
    //                        if (variable.getInstructionId().equals(instruction.getId())) {
    //                            // Once found, update the blockLoopInstructionId with the new instructionId
    //                            variable.setInstructionId(instruction.getInstructionId());
    //                            variable.setBotJobId(oldBotJobId);
    //                            break; // Exit the inner loop since we've found a match
    //                        }
    //                    }
    //                    //                    currentVarId++;
    //                }
    //
    //                // Duplicate variable
    //                //
    // component_block/instruction/component_instruction/reference/component_reference/variable/component_variable/complex/component_complex
    //                errorMessage = duplicateVariables(conn, varsList, arrayTables[6]);
    //                if (errorMessage != null) {
    //                    return errorMessage;
    //                }
    //            }
    //
    //            //
    // component_block/instruction/component_instruction/reference/component_reference/variable/component_variable/complex/component_complex
    //            List<InstructionReferenceLoadDTO> refersList =
    //                    instReferenceToDuplicateNew(conn, oldBotJobId, oldBlockId, arrayTables[3]);
    //            if (refersList.size() > 0) {
    //
    //                currentId = getMaxId(conn, arrayTables[4]) + 1;
    //
    //                // Assuming instList is a List<InstructionDTO> and refersList is a
    // List<InstructionReferenceLoadDTO>
    //                for (InstructionReferenceLoadDTO reference : refersList) {
    //                    reference.setId(currentId++);
    //
    //                    // Loop through the instList and find a matching InstructionDTO
    //                    for (com.allinweb.ch.component.model.InstructionDTO instruction : instList) {
    //                        if (reference.getBlockLoopInstructionId().equals(instruction.getId())) {
    //                            // Once found, update the blockLoopInstructionId with the new instructionId
    //                            reference.setBlockLoopInstructionId(instruction.getInstructionId());
    //                            reference.setBotJobId(oldBotJobId);
    //                            break; // Exit the inner loop since we've found a match
    //                        }
    //                    }
    //                }
    //
    //                // Duplicate reference
    //                //
    // component_block/instruction/component_instruction/reference/component_reference/variable/component_variable/complex/component_complex
    //                errorMessage = duplicateInstructionReferences(conn, refersList, arrayTables[4]);
    //                if (errorMessage != null) {
    //                    return errorMessage;
    //                }
    //            }
    //
    //            //
    // component_block/instruction/component_instruction/reference/component_reference/variable/component_variable/complex/component_complex
    //            List<ComplexInstructionLoadDTO> complexList =
    //                    instComplexToDuplicate(conn, oldBotJobId, oldBlockId, arrayTables[7]);
    //            if (complexList.size() > 0) {
    //
    //                currentId = getMaxId(conn, arrayTables[8]) + 1;
    //
    //                // Assuming instList is a List<InstructionDTO> and refersList is a
    // List<InstructionReferenceLoadDTO>
    //                for (ComplexInstructionLoadDTO complex : complexList) {
    //                    complex.setId(currentId++);
    //
    //                    // Loop through the instList and find a matching InstructionDTO
    //                    for (com.allinweb.ch.component.model.InstructionDTO instruction : instList) {
    //                        if (complex.getInstructionId().equals(instruction.getId())) {
    //                            // Once found, update the blockLoopInstructionId with the new instructionId
    //                            complex.setInstructionId(instruction.getInstructionId());
    //                            complex.setBotJobId(oldBotJobId);
    //                            break; // Exit the inner loop since we've found a match
    //                        }
    //                    }
    //                }
    //
    //                // Duplicate complex_instruction
    //                //
    // component_block/instruction/component_instruction/reference/component_reference/variable/component_variable/complex/component_complex
    //                errorMessage = duplicateComplexInstructions(conn, complexList, arrayTables[8]);
    //                if (errorMessage != null) {
    //                    return errorMessage;
    //                }
    //            }
    //        }
    //        return null;
    //    }

    public ErrorMessage duplicateRelatedTables(Connection conn, int oldBotJobId, int newBotJobId, String[] arrayTables)
            throws SQLException {

        Map<Integer, Integer> blocksOlderAndNewId = new HashMap<>();

        List<BlockLoadDTO> blockList = blocksToDuplicate(conn, oldBotJobId);
        // Duplicate related blocks
        if (blockList.size() > 0) {

            // arrayTables = {"block", "instruction", "reference", "complex_instruction", "variable"};
            int currentId = getMaxId(conn, arrayTables[0]) + 1; // block  or component_block

            // Assuming instList is a List<InstructionDTO> and refersList is a List<InstructionReferenceLoadDTO>
            for (BlockLoadDTO block : blockList) {
                blocksOlderAndNewId.put(block.getId(), currentId);
                block.setId(currentId);
                block.setBotJobId(newBotJobId);
                currentId++;
            }

            // Duplicate Blocks
            ErrorMessage errorMessage = duplicateBlocks(conn, blockList, arrayTables[0]);
            if (errorMessage != null) {
                return errorMessage;
            }
        }

        Map<Integer, Integer> parentOlderAndNewId = new HashMap<>();
        Map<Integer, Integer> variableOlderAndNewId = new HashMap<>();

        // arrayTables = {"block", "instruction", "reference", "complex_instruction", "variable"};
        List<com.allinweb.ch.component.model.InstructionDTO> instList =
                instructionsToDuplicate(conn, oldBotJobId, -1, arrayTables[1]); // instruction
        List<VariableLoadDTO> varsList = instVariablesToDuplicateNEW(conn, oldBotJobId, -1, arrayTables[4]);

        // arrayTables = {"block", "instruction", "reference", "complex_instruction", "variable"};
        int currentVarId = getMaxId(conn, arrayTables[4]) + 1;

        if (varsList.size() > 0) {
            for (VariableLoadDTO variable : varsList) {
                if (!variableOlderAndNewId.containsKey(variable.getId())) {
                    variableOlderAndNewId.put(variable.getId(), currentVarId);
                    variable.setId(currentVarId);
                }
                currentVarId++;
            }
        }

        if (instList.size() > 0) {

            // Prepare the Ids
            // arrayTables = {"block", "instruction", "reference", "complex_instruction", "variable"};
            int currentId = getMaxId(conn, arrayTables[1]) + 1;
            for (com.allinweb.ch.component.model.InstructionDTO instruction : instList) {
                instruction.setInstructionId(currentId); // Holds the News Ids
                instruction.setBotJobId(newBotJobId); // Holds the News Ids

                if (!parentOlderAndNewId.containsKey(instruction.getParentId())) {
                    parentOlderAndNewId.put(instruction.getId(), currentId);
                }

                // Loop through the instList and find a matching InstructionDTO
                for (BlockLoadDTO block : blockList) {
                    if (instruction.getBlockOrderNumber().equals(block.getBlockOrderNumber())) {
                        // Once found, update the blockLoopInstructionId with the new instructionId
                        instruction.setBlockId(block.getId());
                        break; // Exit the inner loop since we've found a match
                    }
                }
                currentId++;
            }
            // Duplicate instruction
            // arrayTables = {"block", "instruction", "reference", "complex_instruction", "variable"};
            ErrorMessage errorMessage = duplicateBlockLoopInstructions(
                    conn, instList, parentOlderAndNewId, variableOlderAndNewId, blocksOlderAndNewId, arrayTables[1]);
            if (errorMessage != null) {
                return errorMessage;
            }

            if (varsList.size() > 0) {

                // Assuming instList is a List<InstructionDTO> and refersList is a List<InstructionReferenceLoadDTO>
                for (VariableLoadDTO variable : varsList) {
                    //                    variable.setId(currentVarId);

                    // Loop through the instList and find a matching InstructionDTO
                    for (com.allinweb.ch.component.model.InstructionDTO instruction : instList) {
                        if (variable.getInstructionId().equals(instruction.getId())) {
                            // Once found, update the blockLoopInstructionId with the new instructionId
                            variable.setInstructionId(instruction.getInstructionId());
                            variable.setBotJobId(newBotJobId);
                            break; // Exit the inner loop since we've found a match
                        }
                    }
                    //                    currentVarId++;
                }

                // Duplicate variable
                // arrayTables = {"block", "instruction", "reference", "complex_instruction", "variable"};
                errorMessage = duplicateVariables(conn, varsList, arrayTables[4]);
                if (errorMessage != null) {
                    return errorMessage;
                }
            }

            // arrayTables = {"block", "instruction", "reference", "complex_instruction", "variable"};
            List<InstructionReferenceLoadDTO> refersList =
                    instReferenceToDuplicateNew(conn, oldBotJobId, -1, arrayTables[2]);
            if (refersList.size() > 0) {

                currentId = getMaxId(conn, arrayTables[2]) + 1;

                // Assuming instList is a List<InstructionDTO> and refersList is a List<InstructionReferenceLoadDTO>
                for (InstructionReferenceLoadDTO reference : refersList) {
                    reference.setId(currentId++);

                    // Loop through the instList and find a matching InstructionDTO
                    for (com.allinweb.ch.component.model.InstructionDTO instruction : instList) {
                        if (reference.getBlockLoopInstructionId().equals(instruction.getId())) {
                            // Once found, update the blockLoopInstructionId with the new instructionId
                            reference.setBlockLoopInstructionId(instruction.getInstructionId());
                            reference.setBotJobId(newBotJobId);
                            break; // Exit the inner loop since we've found a match
                        }
                    }
                }

                // Duplicate reference
                // arrayTables = {"block", "instruction", "reference", "complex_instruction", "variable"};
                errorMessage = duplicateInstructionReferences(conn, refersList, arrayTables[2]);
                if (errorMessage != null) {
                    return errorMessage;
                }
            }

            // arrayTables = {"block", "instruction", "reference", "complex_instruction", "variable"};
            List<ComplexInstructionLoadDTO> complexList = instComplexToDuplicate(conn, oldBotJobId, -1, arrayTables[3]);
            if (complexList.size() > 0) {

                currentId = getMaxId(conn, arrayTables[3]) + 1;

                // Assuming instList is a List<InstructionDTO> and refersList is a List<InstructionReferenceLoadDTO>
                for (ComplexInstructionLoadDTO complex : complexList) {
                    complex.setId(currentId++);

                    // Loop through the instList and find a matching InstructionDTO
                    for (com.allinweb.ch.component.model.InstructionDTO instruction : instList) {
                        if (complex.getInstructionId().equals(instruction.getId())) {
                            // Once found, update the blockLoopInstructionId with the new instructionId
                            complex.setInstructionId(instruction.getInstructionId());
                            complex.setBotJobId(newBotJobId);
                            break; // Exit the inner loop since we've found a match
                        }
                    }
                }

                // Duplicate complex_instruction
                // arrayTables = {"block", "instruction", "reference", "complex_instruction", "variable"};
                errorMessage = duplicateComplexInstructions(conn, complexList, arrayTables[3]);
                if (errorMessage != null) {
                    return errorMessage;
                }
            }
        }
        return null;
    }

    public List<BlockLoadDTO> blockToComponent(Connection conn, int oldBotJobId, int oldBlockId) throws SQLException {
        String query = "SELECT id, block_order_number, description, export_file, name, type_id, wait "
                + "FROM block WHERE bot_job_id = ? and id = ?";

        List<BlockLoadDTO> blockDTOList = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, oldBotJobId); // Set the oldBotJobId parameter
            stmt.setInt(2, oldBlockId); // Set the oldBotJobId parameter
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                BlockLoadDTO blockDTO = new BlockLoadDTO();
                blockDTO.setId(rs.getInt("id"));
                blockDTO.setBlockOrderNumber(rs.getInt("block_order_number"));
                blockDTO.setName(rs.getString("name"));
                blockDTO.setDescription(rs.getString("description"));
                blockDTO.setTypeId(rs.getInt("type_id"));
                blockDTO.setWait(rs.getInt("wait"));
                blockDTO.setExportFile(rs.getString("export_file"));
                blockDTO.setBotJobId(oldBotJobId); // Use the oldBotJobId
                blockDTO.setActive(true); // You can set this based on your logic, assuming active as true for now

                blockDTOList.add(blockDTO); // Add to the list
            }
        }

        return blockDTOList; // Return the list of block DTOs
    }

    public List<BlockLoadDTO> blocksToDuplicate(Connection conn, int oldBotJobId) throws SQLException {
        String query = "SELECT id, block_order_number, description, export_file, name, type_id, wait "
                + "FROM block WHERE bot_job_id = ?";

        List<BlockLoadDTO> blockDTOList = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, oldBotJobId); // Set the oldBotJobId parameter
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                BlockLoadDTO blockDTO = new BlockLoadDTO();
                blockDTO.setId(rs.getInt("id"));
                blockDTO.setBlockOrderNumber(rs.getInt("block_order_number"));
                blockDTO.setName(rs.getString("name"));
                blockDTO.setDescription(rs.getString("description"));
                blockDTO.setTypeId(rs.getInt("type_id"));
                blockDTO.setWait(rs.getInt("wait"));
                blockDTO.setExportFile(rs.getString("export_file"));
                blockDTO.setBotJobId(oldBotJobId); // Use the oldBotJobId
                blockDTO.setActive(true); // You can set this based on your logic, assuming active as true for now

                blockDTOList.add(blockDTO); // Add to the list
            }
        }

        return blockDTOList; // Return the list of block DTOs
    }

    private ErrorMessage duplicateBlocks(Connection conn, List<BlockLoadDTO> blockList, String tableTarget)
            throws SQLException {
        String blockInsertQuery = "INSERT INTO " + tableTarget
                + " (id, block_order_number, name, description, type_id, bot_job_id, export_file, active, wait ";
        String strValues = " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?";

        if (tableTarget.equals("component_block")) {
            blockInsertQuery += ", home_banking_id )";
            strValues += ", ?)";
        } else {
            blockInsertQuery += " )";
            strValues += " )";
        }

        blockInsertQuery = blockInsertQuery + strValues;

        try (PreparedStatement blockStmt = conn.prepareStatement(blockInsertQuery)) {
            for (BlockLoadDTO block : blockList) {
                // Set values for the insert
                blockStmt.setInt(1, block.getId());
                blockStmt.setInt(2, block.getBlockOrderNumber());
                blockStmt.setString(3, block.getName()); // Changing name as required
                blockStmt.setString(4, block.getDescription());
                blockStmt.setInt(5, block.getTypeId());
                blockStmt.setInt(6, block.getBotJobId());
                blockStmt.setString(7, block.getExportFile());
                blockStmt.setBoolean(8, block.getActive());
                blockStmt.setInt(9, block.getWait());
                if (tableTarget.equals("component_block")) {
                    blockStmt.setInt(10, block.getHomeBankingId());
                }

                blockStmt.addBatch(); // Add the current block to the batch
            }

            blockStmt.executeBatch(); // Execute the batch insert
            return null;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return new ErrorMessage("Error Duplicating Blocks", "Block Insertion Failure", e.getMessage());
        }
    }

    private ErrorMessage duplicateBlockLoopInstructions(
            Connection conn,
            List<com.allinweb.ch.component.model.InstructionDTO> instList,
            Map<Integer, Integer> parentOlderAndNewId,
            Map<Integer, Integer> variableOlderAndNewId,
            Map<Integer, Integer> blocksOlderAndNewId,
            String targetTable)
            throws SQLException {
        String blockLoopInstructionInsertQuery = "INSERT INTO " + targetTable
                + " (id, action_custom_max_wait_sec, actions, active, block_marked, codified, "
                + "default_value, description, export_to_abr, instruction_order_number, name, on_hold_seconds, operation, optional, parent_id, path, coordinates, force_coordinates, iframe_xpath, variable_id, block_id, bot_job_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        //        if (targetTable.equalsIgnoreCase("component_instruction")) {
        //            blockLoopInstructionInsertQuery = blockLoopInstructionInsertQuery.replace("block_id",
        // "component_block_id");
        //        }

        try (PreparedStatement blockLoopStmt = conn.prepareStatement(blockLoopInstructionInsertQuery)) {
            for (com.allinweb.ch.component.model.InstructionDTO instruction : instList) {

                // GOTO Action Gets the Block Order Number Kept on parentId
                Integer newParentId = null;

                if (instruction.getActions().equals(ARConstants.GOTO)) {
                    newParentId = blocksOlderAndNewId.get(instruction.getParentId());
                } else {
                    newParentId = parentOlderAndNewId.get(instruction.getParentId());
                }

                Integer newVariableId = variableOlderAndNewId.get(instruction.getVariableId());

                blockLoopStmt.setInt(1, instruction.getInstructionId()); // The New Id
                blockLoopStmt.setInt(2, instruction.getActionCustomMaxWaitSec());
                blockLoopStmt.setString(3, instruction.getActions());
                blockLoopStmt.setBoolean(4, instruction.getInstructionActive());
                blockLoopStmt.setBoolean(5, instruction.getBlockMarked());
                blockLoopStmt.setBoolean(6, instruction.getCodified());

                if (instruction.getDefaultValue() != null) {
                    blockLoopStmt.setString(7, instruction.getDefaultValue());
                } else {
                    blockLoopStmt.setNull(7, Types.VARCHAR);
                }

                blockLoopStmt.setString(8, instruction.getDescription());
                blockLoopStmt.setBoolean(9, instruction.getExportToABR());
                blockLoopStmt.setInt(10, instruction.getInstructionOrderNumber());
                blockLoopStmt.setString(11, instruction.getInstructionName());
                blockLoopStmt.setInt(12, instruction.getOnHoldSeconds());

                if (instruction.getOperation() != null) {
                    blockLoopStmt.setString(13, instruction.getOperation());
                } else {
                    blockLoopStmt.setNull(13, Types.VARCHAR);
                }

                blockLoopStmt.setBoolean(14, instruction.getOptional());

                if (instruction.getParentId() != null && instruction.getParentId() > 0) {
                    blockLoopStmt.setInt(15, newParentId != null ? newParentId : instruction.getParentId());
                } else {
                    blockLoopStmt.setNull(15, java.sql.Types.INTEGER);
                }

                if (instruction.getPath() != null) {
                    blockLoopStmt.setString(16, instruction.getPath());
                } else {
                    blockLoopStmt.setNull(16, Types.VARCHAR);
                }

                if (!Strings.isNullOrEmpty(instruction.getCoordinates())) {
                    blockLoopStmt.setString(17, instruction.getCoordinates());
                } else {
                    blockLoopStmt.setNull(17, Types.VARCHAR);
                }

                blockLoopStmt.setBoolean(18, instruction.getForceCoordinates());

                if (!Strings.isNullOrEmpty(instruction.getIFrameXPath())) {
                    blockLoopStmt.setString(19, instruction.getIFrameXPath());
                } else {
                    blockLoopStmt.setNull(19, Types.VARCHAR);
                }

                if (instruction.getVariableId() != null && instruction.getVariableId() > 0) {
                    blockLoopStmt.setInt(20, newVariableId != null ? newVariableId : instruction.getVariableId());
                } else {
                    blockLoopStmt.setNull(20, java.sql.Types.INTEGER);
                }

                blockLoopStmt.setInt(21, instruction.getBlockId());
                blockLoopStmt.setInt(22, instruction.getBotJobId());

                blockLoopStmt.addBatch(); // Add to batch
            }

            blockLoopStmt.executeBatch(); // Execute the batch insert
            return null;
        } catch (SQLException error) {
            System.out.println(error.getMessage());
            return new ErrorMessage("Error Duplicating Instructions", "Block Insertion Failure", error.getMessage());
        }
    }

    private ErrorMessage duplicateInstructionReferences(
            Connection conn, List<InstructionReferenceLoadDTO> refersList, String targetTable) throws SQLException {
        // Prepare the insert statement for instruction references
        String instructionReferenceInsertQuery = "INSERT INTO " + targetTable
                + " (id, reference_type, value, instruction_id, bot_job_id) " + "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement refStmt = conn.prepareStatement(instructionReferenceInsertQuery)) {
            // Loop through each InstructionReferenceLoadDTO in the refersList
            for (InstructionReferenceLoadDTO reference : refersList) {
                // Set the parameters for the INSERT statement
                refStmt.setInt(1, reference.getId());
                refStmt.setString(2, reference.getReferenceType());
                refStmt.setString(3, reference.getValue());
                refStmt.setInt(4, reference.getBlockLoopInstructionId()); // Use the updated blockLoopInstructionId
                refStmt.setInt(5, reference.getBotJobId()); // Set the new bot job ID

                // Execute the insert statement for each reference
                // refStmt.executeUpdate();
                refStmt.addBatch(); // Add to batch
            }

            refStmt.executeBatch(); // Execute the batch insert
            return null;
        } catch (SQLException error) {
            System.out.println(error.getMessage());
            return new ErrorMessage("Error Duplicating References", "Block Insertion Failure", error.getMessage());
        }
    }

    private ErrorMessage duplicateComplexInstructions(
            Connection conn, List<ComplexInstructionLoadDTO> complexList, String targetTable) throws SQLException {
        // Prepare the insert statement for complex instructions
        String complexInstructionInsertQuery = "INSERT INTO " + targetTable
                + " (instruction, order_number, way, instruction_id, bot_job_id) " + "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement complexStmt = conn.prepareStatement(complexInstructionInsertQuery)) {
            // Loop through each ComplexInstructionLoadDTO in the complexList
            for (ComplexInstructionLoadDTO complexInstruction : complexList) {
                // Set the parameters for the INSERT statement
                complexStmt.setString(1, complexInstruction.getInstruction());
                complexStmt.setInt(2, complexInstruction.getOrderNumber());
                complexStmt.setString(3, complexInstruction.getWay());
                complexStmt.setInt(4, complexInstruction.getInstructionId()); // Assuming you have the updated ID
                complexStmt.setInt(5, complexInstruction.getBotJobId()); // Set the new bot job ID

                // Execute the insert statement for each complex instruction
                // complexStmt.executeUpdate();

                complexStmt.addBatch(); // Add to batch
            }

            complexStmt.executeBatch(); // Execute the batch insert
            return null;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return new ErrorMessage(
                    "Error Duplicating Complex Instructions", "Block Insertion Failure", e.getMessage());
        }
    }

    private ErrorMessage duplicateVariables(Connection conn, List<VariableLoadDTO> varsList, String targetTable)
            throws SQLException {
        String variableInsertQuery = "INSERT INTO " + targetTable
                + " (id, name, type, value, instruction_id, bot_job_id) " + "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement varStmt = conn.prepareStatement(variableInsertQuery)) {
            for (VariableLoadDTO variableDTO : varsList) {
                varStmt.setInt(1, variableDTO.getId());
                varStmt.setString(2, variableDTO.getName());
                varStmt.setString(3, variableDTO.getType());
                varStmt.setString(4, variableDTO.getValue());
                varStmt.setInt(5, variableDTO.getInstructionId());
                varStmt.setInt(6, variableDTO.getBotJobId());

                varStmt.addBatch(); // Add to batch
            }

            varStmt.executeBatch(); // Execute the batch insert
            return null;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return new ErrorMessage("Error Duplicating Variables", "Block Insertion Failure", e.getMessage());
        }
    }

    private ErrorMessage updateVariables(Connection conn, List<VariableLoadDTO> varsList, String targetTable)
            throws SQLException {
        String variableInsertQuery = "update " + targetTable + " set instruction_id = ?  where id = ?";

        try (PreparedStatement varStmt = conn.prepareStatement(variableInsertQuery)) {
            for (VariableLoadDTO variableDTO : varsList) {
                varStmt.setInt(1, variableDTO.getInstructionId());
                varStmt.setInt(2, variableDTO.getId());

                varStmt.addBatch(); // Add to batch
            }

            varStmt.executeBatch(); // Execute the batch insert
            return null;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return new ErrorMessage("Error Duplicating Variables", "Block Insertion Failure", e.getMessage());
        }
    }

    public static List<com.allinweb.ch.component.model.InstructionDTO> filterInstructions(
            List<com.allinweb.ch.component.model.InstructionDTO> instructionList) {
        return instructionList.stream()
                .filter(instruction -> !ARConstants.EXTRACT_FIELD.equals(instruction.getActions())
                        && !ARConstants.SET_VALUE.equals(instruction.getActions())
                        && !ARConstants.GET_VALUE.equals(instruction.getActions())
                        && !ARConstants.CHECK_VALUE.equals(instruction.getActions())
                        && !ARConstants.GOTO.equals(instruction.getActions())
                        && !ARConstants.IF.equals(instruction.getActions())
                        && !ARConstants.ELSE.equals(instruction.getActions())
                        && !ARConstants.ENDIF.equals(instruction.getActions()))
                .collect(Collectors.toList());
    }

    public List<InstructionLoadDTO> buildJsonViewData(List<BotJobLoadDTO> botJobLoadList) {
        List<com.allinweb.ch.component.model.InstructionDTO> rowList = null;
        for (BlockLoadDTO block : botJobLoadList.get(0).getBlockLoadDTOList()) {
            rowList = getInstructionsByBlockId(botJobLoadList.get(0).getId(), block.getId());
            reorderInstructions(rowList);
        }

        List<InstructionLoadDTO> blockLoopInstructions = botJobLoadList.get(0).getBlockLoadDTOList().stream()
                .flatMap(itemBlock -> itemBlock.getInstructionLoadDTOS().stream()
                        .map(loopInstLoad -> new InstructionLoadDTO(
                                botJobLoadList.get(0).getHomeBankingId(), // homBankingId
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
                                loopInstLoad.getParentId(),
                                loopInstLoad.getOperation(),
                                itemBlock.getExportFile())))
                .collect(Collectors.toList());

        // Step 1: Filter rows where actions = "REFRESH_LOOP" and collect their parent IDs
        Set<Integer> parentIdsForRefreshLoop = blockLoopInstructions.stream()
                .filter(instruction -> "REFRESH_LOOP".equalsIgnoreCase(instruction.getActions()))
                .map(InstructionLoadDTO::getParentId)
                .collect(Collectors.toSet());

        // Step 2: Iterate through the list and set refreshLoop = true for rows with id in parentIdsForRefreshLoop
        blockLoopInstructions.forEach(instruction -> {
            if (parentIdsForRefreshLoop.contains(instruction.getId())) {
                instruction.setRefreshLoop(true);
            }
        });

        // Step 1: Filter rows where actions = "LOOP" and collect their parent IDs
        Set<Integer> parentIdsForLoopOnly = blockLoopInstructions.stream()
                .filter(instruction -> "LOOP".equalsIgnoreCase(instruction.getActions()))
                .map(InstructionLoadDTO::getParentId)
                .collect(Collectors.toSet());

        // Step 2: Iterate through the list and set loopOnly = true for rows with id in parentIdsForLoopOnly
        blockLoopInstructions.forEach(instruction -> {
            if (parentIdsForLoopOnly.contains(instruction.getId())) {
                instruction.setLoopOnly(true);
            }
        });

        return blockLoopInstructions;
    }

    private void deleteBlockInstruction(int instructionId) throws SQLException {
        String deleteBlockInstruction = "delete FROM instruction " + " where id = " + instructionId;

        try (Statement stmt = getConnection().createStatement()) {
            int rowsAffected = stmt.executeUpdate(deleteBlockInstruction);
            if (rowsAffected > 0) {
                ARLogger.getInstance(Thread.class).finer("Data deleted successfully for: " + instructionId);
            } else {
                ARLogger.getInstance(Thread.class).finer("No matching record found to delete for: " + instructionId);
            }
        }
    }

    private void deleteInstrReference(int instructionId) throws SQLException {
        String deleteSQL = "delete FROM reference " + " where instruction_id =  " + instructionId;

        try (Statement stmt = getConnection().createStatement()) {
            int rowsAffected = stmt.executeUpdate(deleteSQL);
            if (rowsAffected > 0) {
                ARLogger.getInstance(Thread.class).finer("Data deleted successfully for: " + instructionId);
            } else {
                ARLogger.getInstance(Thread.class).finer("No matching record found to delete for: " + instructionId);
            }
        }
    }

    private boolean existVariables(int instructionId) throws SQLException {
        String query = "select id FROM variable " + " where instruction_id =  " + instructionId;
        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                return true;
            }
        }

        return false;
    }

    private void forceDeleteOrphan(int instructionId) throws SQLException {
        String deleteSQL = "delete FROM reference " + " where instruction_id is null ";

        try (Statement stmt = getConnection().createStatement()) {
            int rowsAffected = stmt.executeUpdate(deleteSQL);
            if (rowsAffected > 0) {
                ARLogger.getInstance(Thread.class).finer("Data deleted successfully for: " + instructionId);
            } else {
                ARLogger.getInstance(Thread.class).finer("No matching record found to delete for: " + instructionId);
            }
        }
    }

    private void forceDeleteFatherNoChild(int instructionId) throws SQLException {
        String deleteSQL = "DELETE FROM instruction " + "WHERE id IN ( "
                + "    SELECT bli.id "
                + "    FROM instruction bli "
                + "    LEFT JOIN reference irl ON irl.instruction_id = bli.id "
                + "    WHERE irl.id IS NULL "
                + "    AND bli.name NOT IN ('Check', 'GetValue', 'SetValue')"
                + ") ";

        try (Statement stmt = getConnection().createStatement()) {
            int rowsAffected = stmt.executeUpdate(deleteSQL);
            if (rowsAffected > 0) {
                ARLogger.getInstance(Thread.class).finer("Data deleted successfully for: " + instructionId);
            } else {
                ARLogger.getInstance(Thread.class).finer("No matching record found to delete for: " + instructionId);
            }
        }
    }

    public List<BlockLoadDTO> loadSavedBlocksForBotJob(int homeBankingId) {
        // SQL query to get the blocks for a specific bot job
        String query = "\n" + "SELECT \n"
                + "  hb.id as home_banking_id,\n"
                + "  hb.name as home_banking_name, \n"
                + "  bc.id AS block_id, \n"
                + "  bc.block_order_number, \n"
                + "  bc.name AS block_name, \n"
                + "  bc.description AS block_description, \n"
                + "  bc.type_id, \n"
                + "  bj.id AS bot_job_id, \n"
                + "  bj.name AS bot_job_name \n"
                + "  FROM \n"
                + "  component_block bc \n"
                + "  JOIN bot_job bj ON bj.id = bc.bot_job_id \n"
                + "  JOIN home_banking hb ON hb.id = bc.home_banking_id \n"
                + "WHERE \n"
                + "  hb.id = "
                + homeBankingId;

        // Initialize the necessary data structures
        List<BlockLoadDTO> savedBlockLoadList = new ArrayList<>();

        Map<Integer, BlockLoadDTO> blockMap = new HashMap<>();

        // Use Statement to execute the query
        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                // Load the Block information
                int blockId = rs.getInt("block_id");
                BlockLoadDTO blockDTO = blockMap.get(blockId);

                if (blockDTO == null) {
                    blockDTO = new BlockLoadDTO();
                    blockDTO.setHomeBankingName(rs.getString("home_banking_name"));
                    blockDTO.setHomeBankingId(rs.getInt("home_banking_id"));
                    blockDTO.setId(blockId);
                    blockDTO.setBlockOrderNumber(rs.getInt("block_order_number"));
                    blockDTO.setName(rs.getString("block_name"));
                    blockDTO.setDescription(rs.getString("block_description"));
                    blockDTO.setTypeId(rs.getInt("type_id"));
                    blockDTO.setBotJobId(rs.getInt("bot_job_id"));
                    blockDTO.setBotJobName(rs.getString("bot_job_name"));

                    blockMap.put(blockId, blockDTO);
                    savedBlockLoadList.add(blockDTO);
                }
            }
        } catch (SQLException e) {
            ARLogger.getInstance(Thread.class)
                    .severe(String.format(
                            "Error loadSavedBlocksForBotJob for Home Banking Id %d\nError: %s",
                            homeBankingId, e.getMessage()));
        }

        return savedBlockLoadList;
    }

    public boolean insertReferences(List<InstructionReferenceLoadDTO> queue, int instructionId) {
        String insertSQL =
                "INSERT INTO reference(id, reference_type, value, instruction_id, bot_job_id) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            int batchSize = 100; // Define a batch size
            int count = 0;

            Integer currentId = loadNextIdBReferenceData() + 1;

            for (InstructionReferenceLoadDTO reference : queue) {

                // Set parameters
                pstmt.setInt(1, currentId);
                pstmt.setString(2, reference.getReferenceType());
                pstmt.setString(3, reference.getValue());
                pstmt.setInt(4, instructionId);
                pstmt.setInt(5, reference.getBotJobId());

                currentId++;
                pstmt.addBatch(); // Add to batch

                // Execute batch if batch size is reached
                if (++count % batchSize == 0) {
                    pstmt.executeBatch();
                    pstmt.clearBatch(); // Clear executed batch to free memory
                }
            }

            // Execute remaining queries in batch
            if (count % batchSize != 0) {
                pstmt.executeBatch();
                pstmt.clearBatch();
            }

            ARLogger.getInstance(PerformDataBase.class).info("Batch insert completed successfully.");
            return true;

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Cannot Insert References\nError: " + e.getMessage());
            return false;
        }
    }

    private Integer loadNextIdBReferenceData() {
        //        String selectSQL = "SELECT NEXT_VAL fROM homeBankingSeq";
        String selectSQL = "SELECT MAX(ID) AS max_id FROM reference";
        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(selectSQL)) {
            while (rs.next()) {
                return rs.getInt("max_id");
            }
        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("loadNextIdBReferenceData  \nError: " + e.getMessage());
        }
        return null;
    }

    // Handle DELETE_INSTRUCTION message
    public static void deleteComponent(int homeBankingId, int blockId, int botJobId) {
        if (deleteCompVariable(homeBankingId, blockId, botJobId))
            if (deleteCompReferences(homeBankingId, blockId, botJobId))
                if (deleteCompRow(homeBankingId, blockId, botJobId)) {
                    deleteCompNullBlocks(homeBankingId, blockId, botJobId);
                    //                    updateBlockOrderNumber(selectAllBlocks(deleteInstructionDTO.getBlockId()),
                    // true);
                }
    }
}
