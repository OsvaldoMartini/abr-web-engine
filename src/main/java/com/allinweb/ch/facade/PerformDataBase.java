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

    private static String previousDB;

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

    public void initialize(String databaseType) {
        this.previousDB = databaseType;
    }

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
        String dataBaseType = ARPropertyManager.getInstance().getProperty(ARPropertyEnum.DATABASE_TYPE);

        //        if (Strings.isNullOrEmpty(previousDB) || (previousDB != null && !previousDB.equals(dataBaseType))) {
        closeConnection();
        previousDB = dataBaseType;

        if (dataBaseType != null && dataBaseType.equalsIgnoreCase("POSTGRES")) {
            POSTGRES_DB = true;

            if (!doesInstructionTableExist()) {
                initializeMainDatabasePostgres();
            }

        } else {
            POSTGRES_DB = false;

            String dbPath = ARPropertyManager.getInstance().getProperty(ARPropertyEnum.FOLDER_PATH_DB);
            String dbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;

            File dbFile = new File(dbPath + ARConstants.FILE_NAME_DB);
            if (!dbFile.exists()) {
                initializeMainDatabaseAccess(dbUrl, dbFile);
            } else {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format("Database '%s' already exists!", dbFile.getName()));
            }
        }
        //        }
    }

    public static void changeDbConnectionHibernate() {
        String priorityPath = ARPropertyManager.getInstance().getProperty(ARPropertyEnum.FOLDER_PATH_PRIORITY);
        String dataBaseType = ARPropertyManager.getInstance().getProperty(ARPropertyEnum.DATABASE_TYPE);

        if (Strings.isNullOrEmpty(previousDB) || (previousDB != null && !previousDB.equals(dataBaseType))) {
            closeConnection();
            previousDB = dataBaseType;

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
                    } catch (Exception error) {

                    }
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
        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class).severe("getConnection Error: " + error.getMessage());
        }

        //        ARSharedResources.getInstance().changeDbConnection(previousDB);

        return conn;
    }

    // Handle DELETE_INSTRUCTION message
    public static void deleteInstruction(int botJobId, InstructionLoadDTO deleteInstructionLoadDTO) {
        if (deleteVariable(botJobId, deleteInstructionLoadDTO.getInstructionId()))
            if (deleteReferences(botJobId, deleteInstructionLoadDTO.getInstructionId()))
                if (deleteRow(deleteInstructionLoadDTO)) {
                    deleteNullBlocks(botJobId);
                    updateBlockOrderNumber(selectAllBlocks(deleteInstructionLoadDTO.getBlockId()), true);
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

    private static boolean deleteCompVariable(InstructionLoadDTO deleteInstructionLoadDTO) {
        // Validate input
        if (deleteInstructionLoadDTO == null || deleteInstructionLoadDTO.getInstructionId() <= 0) {
            ARLogger.getInstance(PerformDataBase.class)
                    .warning("Invalid InstructionLoadDTO provided. Skipping reference deletion.");
            return false;
        }

        // Define SQL delete query
        String deleteSQL = "DELETE FROM component_variable WHERE instruction_id = ?";

        // Use PreparedStatement for security
        try (PreparedStatement stmt = getConnection().prepareStatement(deleteSQL)) {
            stmt.setInt(1, deleteInstructionLoadDTO.getInstructionId()); // Use instruction ID

            int rowsAffected = stmt.executeUpdate();

            // Logging success/failure
            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format(
                                "Deleted all variables for Instruction ID %d.",
                                deleteInstructionLoadDTO.getInstructionId()));
            } else {
                ARLogger.getInstance(PerformDataBase.class)
                        .warning(String.format(
                                "No variables found for Instruction ID %d.",
                                deleteInstructionLoadDTO.getInstructionId()));
            }
            return true;

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error deleting variables for Instruction ID %d. Error: %s",
                            deleteInstructionLoadDTO.getInstructionId(), e.getMessage()));
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

    private static boolean deleteCompReferences(InstructionLoadDTO deleteInstructionLoadDTO) {
        // Validate input
        if (deleteInstructionLoadDTO == null || deleteInstructionLoadDTO.getInstructionId() <= 0) {
            ARLogger.getInstance(PerformDataBase.class)
                    .warning("Invalid InstructionLoadDTO provided. Skipping reference deletion.");
            return false;
        }

        // Define SQL delete query
        String deleteSQL = "DELETE FROM component_reference WHERE instruction_id = ?";

        // Use PreparedStatement for security
        try (PreparedStatement stmt = getConnection().prepareStatement(deleteSQL)) {
            stmt.setInt(1, deleteInstructionLoadDTO.getInstructionId()); // Use instruction ID

            int rowsAffected = stmt.executeUpdate();

            // Logging success/failure
            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format(
                                "Deleted all references for Instruction ID %d.",
                                deleteInstructionLoadDTO.getInstructionId()));
            } else {
                ARLogger.getInstance(PerformDataBase.class)
                        .warning(String.format(
                                "No references found for Instruction ID %d.",
                                deleteInstructionLoadDTO.getInstructionId()));
            }
            return true;

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error deleting references for Instruction ID %d. Error: %s",
                            deleteInstructionLoadDTO.getInstructionId(), e.getMessage()));
        }
        return false;
    }

    private static boolean deleteRow(InstructionLoadDTO deleteInstructionLoadDTO) {
        // Build the SQL delete statement
        try (Statement stmt = getConnection().createStatement()) {

            int rowsAffected = 0;
            String deleteSQL = "DELETE FROM instruction" + " WHERE id = "
                    + deleteInstructionLoadDTO.getInstructionId()
                    + (deleteInstructionLoadDTO.getBlockId() > 0
                            ? " AND block_id = " + deleteInstructionLoadDTO.getBlockId()
                            : " AND block_id IS NULL");

            if (deleteInstructionLoadDTO.getActions() != null
                    && (deleteInstructionLoadDTO.getActions().equals("IF")
                            || deleteInstructionLoadDTO.getActions().equals("ELSE")
                            || deleteInstructionLoadDTO.getActions().equals("ENDIF"))) {

                rowsAffected += stmt.executeUpdate("DELETE FROM instruction  "
                        + " WHERE "
                        + " block_id = " + deleteInstructionLoadDTO.getBlockId() + " AND parent_id = "
                        + deleteInstructionLoadDTO.getParentId());
            } else {

                rowsAffected += stmt.executeUpdate(deleteSQL);
            }

            // Execute the update statement and check if any rows were affected
            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format(
                                "The instruction with ID %d has been successfully deleted from block %d.",
                                deleteInstructionLoadDTO.getInstructionId(), deleteInstructionLoadDTO.getBlockId()));
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
                            deleteInstructionLoadDTO.getInstructionId(),
                            deleteInstructionLoadDTO.getBlockId(),
                            e.getMessage()));
        }
        return false;
    }

    private static boolean deleteCompInstruction(InstructionLoadDTO deleteInstructionLoadDTO) {
        // Validate input
        if (deleteInstructionLoadDTO == null || deleteInstructionLoadDTO.getInstructionId() <= 0) {
            ARLogger.getInstance(PerformDataBase.class)
                    .warning("Invalid InstructionLoadDTO provided. Skipping instruction deletion.");
            return false;
        }

        boolean isConditional = deleteInstructionLoadDTO.getActions() != null
                && (deleteInstructionLoadDTO.getActions().equals("IF")
                        || deleteInstructionLoadDTO.getActions().equals("ELSE")
                        || deleteInstructionLoadDTO.getActions().equals("ENDIF"));

        String deleteSQL;

        if (isConditional) {
            // Delete conditional instructions along with related parent, block, and bot job
            deleteSQL =
                    "DELETE FROM component_instruction WHERE (id = ? OR parent_id = ?) AND block_id = ? AND bot_job_id = ?";
        } else {
            // Simple deletion by instruction ID
            deleteSQL = "DELETE FROM component_instruction WHERE id = ?";
        }

        try (PreparedStatement stmt = getConnection().prepareStatement(deleteSQL)) {
            stmt.setInt(1, deleteInstructionLoadDTO.getInstructionId());

            if (isConditional) {
                stmt.setInt(2, deleteInstructionLoadDTO.getParentId());
                stmt.setInt(3, deleteInstructionLoadDTO.getBlockId());
                stmt.setInt(4, deleteInstructionLoadDTO.getBotJobId());
            }

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format(
                                "Deleted Instruction ID %d%s.",
                                deleteInstructionLoadDTO.getInstructionId(),
                                isConditional ? " and related conditional instructions" : ""));
            } else {
                ARLogger.getInstance(PerformDataBase.class)
                        .warning(String.format(
                                "No matching instruction found for ID %d.",
                                deleteInstructionLoadDTO.getInstructionId()));
            }

            return true;

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error deleting Instruction ID %d. Error: %s",
                            deleteInstructionLoadDTO.getInstructionId(), e.getMessage()));
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

    public static void deleteCompNullBlocks(int homeBanking, int botJobId) {
        // Validate input
        if (homeBanking <= 0) {
            ARLogger.getInstance(PerformDataBase.class)
                    .warning("Invalid InstructionLoadDTO provided. Skipping block deletion.");
            return;
        }

        String deleteSQL = "DELETE FROM component_block b " + "WHERE b.home_banking_id = ? "
                + "AND (b.bot_job_id IS NULL OR b.bot_job_id = ?) "
                + // Handle NULL botJobId case
                "AND NOT EXISTS ( "
                + "    SELECT 1 FROM component_instruction bli "
                + "    WHERE bli.block_id = b.id );";

        try (PreparedStatement stmt = getConnection().prepareStatement(deleteSQL)) {
            stmt.setInt(1, homeBanking);

            if (botJobId > 0) {
                stmt.setInt(2, botJobId);
            } else {
                stmt.setNull(2, Types.INTEGER);
            }

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format("%d Null Blocks successfully deleted.", rowsAffected));
            } else {
                ARLogger.getInstance(PerformDataBase.class).warning("No matching null blocks found for deletion.");
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("Error deleting Null Blocks. Error: %s", e.getMessage()));
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

    public static void updateCompBlockOrderNumber(
            List<BlockOrderDetailDTO> blockOrderDetailDTOList, boolean reorderAll) {
        //         Sort the blockOrderDetailDTOList based on the previous blockOrderNumber in ascending order
        blockOrderDetailDTOList.sort(Comparator.comparingInt(BlockOrderDetailDTO::getBlockOrderNumber));

        try (Statement stmt = getConnection().createStatement()) {
            int newOrderNumber = 1; // Start reordering from 1

            for (BlockOrderDetailDTO blockOrderDetailDTO : blockOrderDetailDTOList) {
                // Update each block's block_order_number starting from 1
                String updateSQL = "UPDATE component_block SET block_order_number = "
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
                                    "updateCompBlockOrderNumber - No matching record found to update botJobId: %d blockId: %d",
                                    blockOrderDetailDTO.getBotJobId(), blockOrderDetailDTO.getBlockId()));
                }

                newOrderNumber++; // Increment the new order number for the next block
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("Error updateCompBlockOrderNumber. Error: %s", e.getMessage()));
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

    public static List<BlockOrderDetailDTO> selectCompAllBlocks(int botJobId) {
        List<BlockOrderDetailDTO> blockOrderDetails = new ArrayList<>();
        try (Statement stmt = getConnection().createStatement()) {

            // Select blocks based on botJobId, ordered by block_order_number ASC
            String selectSQL = "SELECT id FROM component_block WHERE bot_job_id = " + botJobId
                    + " ORDER BY block_order_number ASC";
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
    public void updateCompBlockName(int botJobId, int blockId, String blockName) {
        try (Statement stmt = getConnection().createStatement()) {

            // Update each block's block_order_number starting from 1
            String updateSQL = "UPDATE component_block SET name = '" + blockName + "',"
                    + " description = '" + blockName + "'"
                    + " WHERE id = " + blockId;
            //                    + " and bot_job_id = " + botJobId;

            int rowsAffected = stmt.executeUpdate(updateSQL);

            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format("Block Name updated blockId: %s, name: %s", blockId, blockName));
            } else {
                ARLogger.getInstance(PerformDataBase.class)
                        .warning(String.format(
                                "updateCompBlockName - No matching record found to update botJobId: %d blockId: %d",
                                botJobId, blockId));
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("Error UpdateBlockOrderNumber. Error: %s", e.getMessage()));
        }
    }

    // Handle BLOCK_UPDATE message
    public boolean updateBlockExportFile(String tableTarget, int botJobId, int blockId, String exportFile) {
        try (Statement stmt = getConnection().createStatement()) {

            // Update each block's block_order_number starting from 1
            String updateSQL =
                    "UPDATE " + tableTarget + " SET export_file = '" + exportFile + "'" + " WHERE id = " + blockId;
            //                    + " and bot_job_id = " + botJobId;

            int rowsAffected = stmt.executeUpdate(updateSQL);

            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format("Block Export File updated blockId: %s, name: %s", blockId, exportFile));
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
        List<InstructionLoadDTO> deleteList =
                getInstructionsByBlockId(deleteBlockDTO.getBotJobId(), deleteBlockDTO.getBlockId());
        if (deleteList.size() > 0) {
            for (InstructionLoadDTO deleteDTO : deleteList) {
                deleteDTO.setHomeBankingId(deleteBlockDTO.getHomeBankingId());
                deleteInstruction(deleteBlockDTO.getBotJobId(), deleteDTO);
                //                updateOtherBlocks()
            }
        }
        blockDeletion = deleteBlockDirect((int) deleteBlockDTO.getBotJobId(), (int) deleteBlockDTO.getBlockId());
        //        updateOtherBlocks(deleteBlockDTO.getUpdatedBlockDTO());
        deleteNullBlocks((int) deleteBlockDTO.getBotJobId());
        if (deleteBlockDTO.getUpdatedBlocks() != null
                && deleteBlockDTO.getUpdatedBlocks().size() > 0) {
            updateBlockOrderNumber(
                    selectAllBlocks(deleteBlockDTO.getUpdatedBlocks().get(0).getBotJobId()), true);
        }

        return blockDeletion;
    }

    // Handle DELETE_BLOCK message
    public boolean deleteCompBlock(DeleteBlockDTO deleteBlockDTO) {
        boolean blockDeletion = false;
        List<InstructionLoadDTO> deleteList =
                getCompInstructionsByBlockId(deleteBlockDTO.getBotJobId(), deleteBlockDTO.getBlockId());
        if (deleteList.size() > 0) {
            for (InstructionLoadDTO deleteDTO : deleteList) {
                deleteDTO.setHomeBankingId(deleteBlockDTO.getHomeBankingId());
                deleteComponent(deleteDTO);
                //                updateOtherBlocks()
            }
        }
        return blockDeletion;
    }

    // Method to create a new BlockDTO entity and save it to the database
    public int createNewBlock(BlockDetailsDTO newBlockDetails) {
        try {
            // Persist the BlockDTO entity using the saveBlock method
            int newBlockId = initiateNewBlock(newBlockDetails, newBlockDetails.getBotJobId());
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

    private int initiateNewBlock(BlockDetailsDTO blockDTO, int botJobId) {
        // Generate a Unique-ID for the block
        Integer nextId = loadNextIdBlockData() + 1;
        Integer nextBlockOrder = -1;
        if (blockDTO.getBlockOrderNumber() != null) {
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
                        + (blockDTO.getActive() ? 1 : 0) + ", " // active
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
        // Generate a Unique-ID for the bot job
        Integer nextId = loadNextIdBotJobData() + 1;

        if (nextId < 0) {
            return -1;
        }

        // Build the SQL insert query using PreparedStatement
        String insertSQL =
                "INSERT INTO bot_job (id, name, description, home_banking_id, active) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = getConnection().prepareStatement(insertSQL)) {
            pstmt.setInt(1, nextId);
            pstmt.setString(2, createdBotJob.getName());
            pstmt.setString(3, createdBotJob.getName() + " description");
            pstmt.setInt(4, createdBotJob.getHomeBankingId());
            pstmt.setInt(5, 1); // Setting "active" as true

            pstmt.executeUpdate();

            ARLogger.getInstance(PerformDataBase.class)
                    .info(String.format("BotJob data saved successfully.\n BotJobId: %d", nextId));
            return nextId;
        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("createNewBotJob - \nError: %s", error.getMessage()));
            return -1;
        }
    }

    public boolean updateInstructionsSplitter(
            List<InstructionLoadDTO> instructions, int originalBlockId, int newBlockId) {
        // Build the SQL update statement

        try (Statement stmt = getConnection().createStatement()) {
            for (InstructionLoadDTO instruction : instructions) {

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

    public boolean rowsUpdateName(List<InstructionLoadDTO> instructions) {
        // Build the SQL update statement
        try (Statement stmt = getConnection().createStatement()) {
            for (InstructionLoadDTO instruction : instructions) {

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

    public boolean rowsCompUpdateName(List<InstructionLoadDTO> instructions) {
        // Build the SQL update statement
        try (Statement stmt = getConnection().createStatement()) {
            for (InstructionLoadDTO instruction : instructions) {

                String updateSQL = "UPDATE component_instruction SET  "
                        + " name = '" + instruction.getInstructionName() + "',"
                        + " actions = '" + instruction.getActions() + "'"
                        + " WHERE id = " + instruction.getInstructionId()
                        + " and block_id = " + instruction.getBlockId();

                int rowsAffected = stmt.executeUpdate(updateSQL);
                if (rowsAffected > 0) {
                    ARLogger.getInstance(PerformDataBase.class)
                            .warning(String.format(
                                    "Component Instruction Updated - InstructionId: %s now have name: %s",
                                    instruction.getInstructionId(), instruction.getInstructionName()));
                } else {
                    ARLogger.getInstance(PerformDataBase.class)
                            .warning(String.format(
                                    "Component Instruction Updated - No matching record found to update InstructionId: %d and name: %s",
                                    instruction.getInstructionId(), instruction.getInstructionName()));
                }
            }
            return true;
        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "This Component Instruction \n cannot be updated.\nError: %s", e.getMessage()));
        }
        return false;
    }

    public boolean updateMoveRowsOrder(List<InstructionLoadDTO> instructions) {
        // Build the SQL update statement
        try (Statement stmt = getConnection().createStatement()) {
            for (InstructionLoadDTO instruction : instructions) {

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

    public boolean updateCompMoveRowsOrder(List<InstructionLoadDTO> instructions) {
        // Build the SQL update statement
        try (Statement stmt = getConnection().createStatement()) {
            for (InstructionLoadDTO instruction : instructions) {

                String updateSQL = "UPDATE component_instruction SET  "
                        + " instruction_order_number = " + instruction.getInstructionOrderNumber() + ","
                        + " block_id = " + instruction.getBlockId()
                        + " WHERE id = " + instruction.getInstructionId();

                int rowsAffected = stmt.executeUpdate(updateSQL);
                if (rowsAffected > 0) {
                    ARLogger.getInstance(PerformDataBase.class)
                            .warning(String.format(
                                    "updateCompMoveRowsOrder - InstructionId: %s now have order number: %d",
                                    instruction.getInstructionId(), instruction.getInstructionOrderNumber()));
                } else {
                    ARLogger.getInstance(PerformDataBase.class)
                            .warning(String.format(
                                    "updateCompMoveRowsOrder - No matching record found to update blockId: %d and InstructionId: %d",
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

    public void rollBackBlocksRows(String targetTable, RollBackBlocksDTO rollBackBlocksDTO) {
        // Build the SQL update statement

        try (Statement stmt = getConnection().createStatement()) {
            for (InstructionLoadDTO instruction : rollBackBlocksDTO.getInstructions()) {

                String updateSQL = "UPDATE " + targetTable + " SET  "
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

    public static List<InstructionLoadDTO> getBlockLoopInstructionIdsWithNullBlock(int botJobId) {
        // List to store IDs of block loop instructions where block_id is null
        List<InstructionLoadDTO> instructions = new ArrayList<>();

        // SQL query to select instruction IDs where block_id is null
        String selectSQL = "SELECT i.id FROM instruction i " + " WHERE i.block_id IS NULL";

        // Try-with-resources to handle the SQL statement and result set
        try (Statement stmt = getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery(selectSQL);

            // Iterate through the result set and add each ID to the list
            while (rs.next()) {
                InstructionLoadDTO InstructionLoadDTO = new InstructionLoadDTO();
                InstructionLoadDTO.setInstructionId(rs.getInt("id"));
                InstructionLoadDTO.setBlockId(-1);
                instructions.add(InstructionLoadDTO);
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

    public boolean deleteBlockDirect(int botJobId, int blockId) {
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

    public boolean deleteCompBlockDirect(int botJobId, int blockId) {
        // Build the SQL delete statement
        try (Statement stmt = getConnection().createStatement()) {

            String deleteSQL =
                    "DELETE FROM component_block " + " WHERE id = " + blockId + " and bot_job_id = " + botJobId;

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

    public List<BotJobLoadDTO> loadCompleteJobs(int botJobId) {
        String query = "SELECT bot.home_banking_id, bot.id AS bot_job_id, bot.name AS bot_job_name, "
                + " b.id AS block_id, b.block_order_number, b.name AS block_name, "
                + " b.description AS block_description, b.type_id, "
                + " bli.id AS instruction_id, bli.instruction_order_number, "
                + " bli.actions, bli.name AS instruction_name, bli.xpath, bli.coordinates,  bli.iframe_xpath, "
                + " bli.description AS instruction_description, bli.force_coordinates, "
                + " bli.optional, bli.block_marked, bli.default_value, bli.action_custom_max_wait_sec, "
                + " bli.on_hold_seconds, bli.codified, bli.export_to_abr, "
                + " irl.reference_type, irl.value, "
                + "  bli.operation, bli.parent_id, "
                + "  b.export_file, "
                + "  b.active as block_active, b.wait, "
                + "  bli.active as instruction_active "
                + " FROM bot_job bot "
                + " LEFT JOIN block b ON b.bot_job_id = bot.id "
                + " JOIN instruction bli ON bli.block_id = b.id "
                + " LEFT JOIN reference irl ON irl.instruction_id = bli.id "
                + " where bot.active = 1 and bot.id = " + botJobId
                + "  ORDER BY bot.id, b.block_order_number, bli.instruction_order_number, irl.id ASC";

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
                    instruction.setXpath(rs.getString("xpath"));
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
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error loadBotJobWithBlock for botJobId %d\nError: %s", botJobId, e.getMessage()));
        }

        return botJobLoadList;
    }

    public List<BotJobLoadDTO> loadComponentsComplete(int homeBankingId) {
        String query = "\n" + "\n"
                + "SELECT \n"
                + "    hb.id AS home_banking_id, \n"
                + "\t-1 as bot_job_id,\n"
                + "\t'No Bot Job Name' as bot_job_name,\n"
                + "\tblk.id AS block_id, \n"
                + "    blk.block_order_number, \n"
                + "    blk.name AS block_name, \n"
                + "    blk.description AS block_description, \n"
                + "    blk.type_id, \n"
                + "    blk.export_file,\n"
                + "    blk.active AS block_active, \n"
                + "    blk.wait,\n"
                + "    bli.id AS instruction_id, \n"
                + "    bli.instruction_order_number, \n"
                + "    bli.actions, \n"
                + "    bli.name AS instruction_name, \n"
                + "    bli.xpath, \n"
                + "    bli.coordinates, \n"
                + "    bli.iframe_xpath, \n"
                + "    bli.description AS instruction_description, \n"
                + "    bli.force_coordinates, \n"
                + "    bli.optional, \n"
                + "    bli.block_marked, \n"
                + "    bli.default_value, \n"
                + "    bli.action_custom_max_wait_sec, \n"
                + "    bli.on_hold_seconds, \n"
                + "    bli.codified, \n"
                + "    bli.export_to_abr, \n"
                + "    bli.operation, \n"
                + "    bli.parent_id, \n"
                + "    bli.active AS instruction_active,\n"
                + "    irl.reference_type, \n"
                + "    irl.value AS reference_value\n"
                + "FROM home_banking hb\n"
                + "LEFT JOIN component_block blk ON blk.home_banking_id = hb.id\n"
                + "JOIN component_instruction bli ON bli.block_id = blk.id\n"
                + "LEFT JOIN component_reference irl ON irl.instruction_id = bli.id\n"
                + "WHERE hb.id = "
                + homeBankingId + "\n"
                + "ORDER BY hb.id, blk.block_order_number, bli.instruction_order_number, bli.id ASC;";

        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            Map<Integer, BotJobLoadDTO> botJobMap = new HashMap<>();
            Map<Integer, BlockLoadDTO> blockMap = new HashMap<>();
            Map<Integer, InstructionLoadDTO> instructionMap = new HashMap<>();

            botJobLoadList.clear();

            while (rs.next()) {
                int botJobId = rs.getInt("bot_job_id");
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
                    instruction.setXpath(rs.getString("xpath"));
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
                    reference.setValue(rs.getString("reference_value"));
                    instruction.getInstructionReferenceLoadDTOList().add(reference);
                }
            }
        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error loadBotJobWithBlock for botJobId %d\nError: %s", homeBankingId, error.getMessage()));
            return botJobLoadList = new ArrayList<>();
        }

        return botJobLoadList;
    }

    //    public static List<BotJobLoadDTO> loadBlockAll(int botJobId) {
    //        String query = "SELECT bj.id AS bot_job_id, bj.name AS bot_job_name, "
    //                + " b.id AS block_id, b.block_order_number, b.name AS block_name, "
    //                + " b.description AS block_description, b.type_id, "
    //                + " bli.id AS instruction_id, bli.instruction_order_number, "
    //                + " bli.actions, bli.name AS instruction_name, bli.xpath, bli.coordinates, bli.iframe_xpath,
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
    //                    instruction.setPath(rs.getString("xpath"));
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
    //                List<BlockLoopInstructionLoadDTO> instructionList = null;
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
    //                List<BlockLoopInstructionLoadDTO> finalInstructionList = instructionList;
    //                List<BlockDTO> finalMatchingBlocks = matchingBlocks;
    //
    //                Task<Void> waitTask = new Task<>() {
    //                    @Override
    //                    protected Void call() throws Exception {
    //                        try {
    //                            BlockLoopInstructionLoadDTO instruction = new BlockLoopInstructionLoadDTO();
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
    //                                addEntity(instruction, BlockLoopInstructionLoadDTO.class);
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

    public boolean reorderInstructions(List<InstructionLoadDTO> rowList) {
        int orderNumber = 1;

        // Iterate through the list and update the instructionOrderNumber
        for (InstructionLoadDTO instruction : rowList) {
            instruction.setInstructionOrderNumber(orderNumber);
            orderNumber++; // Increment the order number for the next instruction
        }

        // Build the SQL update statement
        try (Statement stmt = getConnection().createStatement()) {
            // Loop through each instruction in the rowList
            for (InstructionLoadDTO instruction : rowList) {
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

            int rowsAffected = stmt.executeUpdate("DELETE FROM variable where bot_job_id = " + botJobId);
            rowsAffected += stmt.executeUpdate("DELETE FROM reference where bot_job_id = " + botJobId);
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

    public boolean updateStatusBotJob(int botJobId, int status) {
        // Build the SQL delete statement
        try (Statement stmt = getConnection().createStatement()) {

            String updateSQL = "UPDATE bot_job set active = '" + status + "' WHERE id = " + botJobId;

            // Execute the update statement and check if any rows were affected
            int rowsAffected = stmt.executeUpdate(updateSQL);
            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format("The Status Bot Job  id %d has been successfully updated!", botJobId));
            } else {
                ARLogger.getInstance(PerformDataBase.class)
                        .warning(String.format("No matching record found for botJobId %d.", botJobId));
            }
            return true;
        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error updating Status for BotJobId ID %d. Error: %s", botJobId, e.getMessage()));
        }
        return false;
    }

    public BlockLoadDTO loadBlockByBotId(int botJobId, int blockId) {
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

    public BlockLoadDTO loadAllBlockByBotId(int botJobId, int blockId) {
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

    public List<InstructionLoadDTO> getInstructionsByBlockId(int botJobId, int blockId) {
        // List to store the fetched instructions
        List<InstructionLoadDTO> instructions = new ArrayList<>();

        // Build the SQL query statement
        String querySQL =
                "SELECT * FROM instruction WHERE block_id = " + blockId + " order by instruction_order_number ASC";

        // Execute the query and process the result set
        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(querySQL)) {

            while (rs.next()) {
                // Assuming you have an Instruction class, populate it with data from the ResultSet
                InstructionLoadDTO instruction = new InstructionLoadDTO();

                instruction.setInstructionId(rs.getInt("id"));
                instruction.setBotJobId(rs.getInt("bot_job_id"));
                instruction.setBlockId(rs.getInt("block_id"));

                instruction.setInstructionName(rs.getString("name"));
                instruction.setInstructionOrderNumber(rs.getInt("instruction_order_number"));
                instruction.setBlockId(rs.getInt("block_id"));
                instruction.setBlockOrderNumber(instruction.getBlockOrderNumber());
                instruction.setBotJobId(botJobId);

                instruction.setActions(rs.getString("actions"));
                instruction.setXpath(rs.getString("xpath"));
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

    public List<InstructionLoadDTO> getCompInstructionsByBlockId(int botJobId, int blockId) {
        // List to store the fetched instructions
        List<InstructionLoadDTO> instructions = new ArrayList<>();

        // Build the SQL query statement
        String querySQL = "SELECT * FROM component_instruction WHERE block_id = " + blockId
                + " order by instruction_order_number ASC";

        // Execute the query and process the result set
        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(querySQL)) {

            while (rs.next()) {
                // Assuming you have an Instruction class, populate it with data from the ResultSet
                InstructionLoadDTO instruction = new InstructionLoadDTO();
                instruction.setInstructionId(rs.getInt("id"));
                instruction.setHomeBankingId(rs.getInt("home_banking_id"));
                instruction.setBlockId(rs.getInt("block_id"));

                instruction.setInstructionName(rs.getString("name"));
                instruction.setInstructionOrderNumber(rs.getInt("instruction_order_number"));
                instruction.setBlockOrderNumber(instruction.getBlockOrderNumber());
                instruction.setBotJobId(botJobId);

                instruction.setActions(rs.getString("actions"));
                instruction.setXpath(rs.getString("xpath"));
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
                    .info(String.format(
                            "Fetched %d Component Instructions for Block ID %d:", instructions.size(), blockId));

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error fetching Component Instructions for Block ID %d. Error: %s: ",
                            blockId, e.getMessage()));
        }

        return instructions;
    }

    public List<BotJobLoadDTO> loadAllBotJobs() {
        this.botJobLoadList.clear();
        String query =
                """
            SELECT bot.id AS bot_job_id, bot.name AS bot_job_name,
            bot.description AS bot_job_description, bot.priority AS bot_job_priority,
            bot.home_banking_id,
            hb.url AS home_banking_url,
            hb.name AS home_banking_name,
            hb.priority AS home_banking_priority, hb.search_config,
            hb.options_config, hb.cookies, hb.driver_session,
            hb.username, hb.password,
            bot.active
            FROM bot_job bot
            LEFT JOIN home_banking hb ON bot.home_banking_id = hb.id
            ORDER BY bot.id ASC;
            """;

        try (PreparedStatement pstmt = getConnection().prepareStatement(query)) {
            //            pstmt.setInt(1, true);  // Set active = true (Access might need `pstmt.setInt(1, -1);`)

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    BotJobLoadDTO botJobDTO = new BotJobLoadDTO();

                    // Map BotJobLoadDTO fields
                    botJobDTO.setId(rs.getInt("bot_job_id"));
                    botJobDTO.setName(rs.getString("bot_job_name"));
                    botJobDTO.setDescription(rs.getString("bot_job_description"));
                    botJobDTO.setPriority(rs.getString("bot_job_priority"));
                    botJobDTO.setHomeBankingId(rs.getInt("home_banking_id"));
                    botJobDTO.setActive(rs.getBoolean("active"));

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
            }
        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("Error loadAllBotJobs\nError: %s", error.getMessage()));
        }

        return this.botJobLoadList;
    }

    public List<BotJobLoadDTO> loadBotJobAndBlocks(int botJobId) {
        String query = "SELECT bot.id AS bot_job_id, bot.name AS bot_job_name, "
                + " b.id AS block_id, b.block_order_number, b.name AS block_name, "
                + " b.description AS block_description, b.type_id,"
                + " b.active, b.wait"
                + " FROM bot_job bot "
                + " LEFT JOIN block b ON b.bot_job_id = bot.id "
                + " where bot.active = 1 and bot_job_id = " + botJobId
                + "  ORDER BY bot.id, b.block_order_number ASC";

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
            ARLogger.getInstance(PerformDataBase.class)
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

    public boolean updateInstructionStatus(InstructionLoadDTO instruction) {
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

    public boolean updateCompInstructionStatus(InstructionLoadDTO instruction) {
        // Build the SQL update statement
        try (Statement stmt = getConnection().createStatement()) {
            int rowsAffected = 0;

            if (instruction.getActions().equals("IF")
                    || instruction.getActions().equals("ELSEIF")
                    || instruction.getActions().equals("ELSE")
                    || instruction.getActions().equals("ENDIF")) {
                rowsAffected = stmt.executeUpdate(
                        "UPDATE component_instruction SET active = '" + instruction.getInstructionActive() + "'"
                                + " WHERE "
                                + " block_id = " + instruction.getBlockId() + " AND parent_id = "
                                + instruction.getParentId());
            } else {

                String updateSQL =
                        "UPDATE component_instruction SET active = '" + instruction.getInstructionActive() + "'"
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

    public boolean updateCompInstructionStatusByBlock(int botJobId, int blockId, boolean blockActive) {
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

    public void updateCompBlockStatus(int botJobId, int blockId, String blockName, boolean blockActive, int wait) {
        try (Statement stmt = getConnection().createStatement()) {

            // Update each block's block_order_number starting from 1
            String updateSQL = "UPDATE component_block SET active = '" + blockActive + "',"
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
                                "updateCompBlockStatus - No matching record found to update botJobId: %d blockId: %d",
                                botJobId, blockId));
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("Error updateCompBlockStatus. Error: %s", e.getMessage()));
        }
    }

    public BotJobLoadDTO loadBotJobById(int botJobId) {
        // SQL query to get the blocks for a specific bot job
        String query = "SELECT * from bot_job bot WHERE bot.active = 1 and bot.id = " + botJobId;

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
                botJobLoadDTO.setActive(rs.getBoolean("active"));
            }
            return botJobLoadDTO;

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("Error loadBotJob for botJobId %d\nError: %s", botJobId, e.getMessage()));
        }

        return null;
    }

    public boolean updateBotStatus() {
        // SQL query to get the blocks for a specific bot job
        String query = "update bot_job set active = 1";

        // Initialize the necessary data structures

        // Use Statement to execute the query
        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(query)) {
            return true;

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("Error updating Active = 1 all botjobs\nError: %s", e.getMessage()));
        }

        return false;
    }

    //    private void loadBotJobComplex(BotJobDTO botJob) {
    //        String selectSQL =
    //                " SELECT bot.ID botId, bot.Name botName, blk.ID blockId, blk.Name blockName,
    // blk.block_order_number, "
    //                        + " blockInstr.id blockInstrId, blockInstr.instruction_order_number
    // instructionOrderNumber, blockInstr.actions, "
    //                        + " instr.id instId, instr.reference_type, instr.value"
    //                        + " FROM reference instr "
    //                        + " join instruction blockInstr on blockInstr.id = instr.instruction_id"
    //                        + " join bot_job bot on active = 1 and bot.id = " + botJob.getId()
    //                        + " join block blk on blk.bot_job_id = bot.id "
    //                        + " order by blockInstr.id, blockInstr.instruction_order_number, instr.id";
    //        try (Statement stmt = getConnection().createStatement();
    //                ResultSet rs = stmt.executeQuery(selectSQL)) {
    //
    //            List<ReferenceDTO> instructions = new ArrayList<>();
    //
    //            while (rs.next()) {
    //                String botId = rs.getString("botId");
    //                String botName = rs.getString("botName");
    //                String blockId = rs.getString("blockId");
    //                String blockName = rs.getString("blockName");
    //                String blockOrderNumber = rs.getString("block_order_number");
    //
    //                String blockInstrId = rs.getString("blockInstrId");
    //                String instructionOrderNumber = rs.getString("instructionOrderNumber");
    //                String actions = rs.getString("actions");
    //
    //                String instId = rs.getString("instId");
    //                String referenceType = rs.getString("reference_type");
    //                String value = rs.getString("value");
    //
    //                if (botJob.getId() == Integer.parseInt(botId)) {
    //                    for (BlockDTO block : botJob.getBlocks()) {
    //                        if (block.getId() == Integer.parseInt(blockId)) {
    //                            boolean exist = false;
    //                            for (InstructionLoadDTO blockInstruction : block.getBlockLoopInstructionLoadDTOS()) {
    //                                if (blockInstruction.getId() == Integer.parseInt(blockInstrId)) {
    //                                    for (ReferenceDTO instructionReference :
    //                                            blockInstruction.getInstructionReferenceDTOList()) {
    //                                        if (instructionReference.getId() == Integer.parseInt(instId)
    //                                                && instructionReference
    //                                                        .getReferenceType()
    //                                                        .equalsIgnoreCase(referenceType)
    //                                                && instructionReference
    //                                                        .getValue()
    //                                                        .equalsIgnoreCase(value)) {
    //                                            exist = true;
    //                                            break;
    //                                        }
    //                                    }
    //                                    if (!exist) {
    //                                        ReferenceDTO inst = new ReferenceDTO();
    //                                        inst.setId(Integer.parseInt(instId));
    //                                        inst.setReferenceType(referenceType);
    //                                        inst.setValue(value);
    //                                        instructions.add(inst);
    //                                        break;
    //                                    }
    //                                }
    //                                if (exist) {
    //                                    break;
    //                                }
    //                            }
    //                        }
    //                    }
    //                }
    //
    //                //                System.out.println(String.format(
    //                //                        "%s  %s  %s  %s  %s   %s   %s   %s",
    //                //                        botId, botName, blockId, blockName, blockOrderNumber, referenceType,
    // value));
    //
    //                //               databaseUserDto = new DatabaseUserDTO(
    //                //                        id, jobs, name, url, priority, searchConfig, optionsConfig, username,
    //                // password);
    //            }
    //        } catch (SQLException e) {
    //            System.out.println(e.getMessage());
    //        }
    //        //        jobUserList.clear();
    //        //        loadBotJobData();
    //    }

    public List<BlockLoadDTO> loadBlocksByBotJobId(int botJobId) {
        // SQL query to get the blocks for a specific bot job
        String query = "SELECT " + "b.id AS block_id, "
                + "b.block_order_number, "
                + "b.name AS block_name, "
                + "b.description AS block_description, "
                + "b.type_id, "
                + "bot.id AS bot_job_id, "
                + "bot.name AS bot_job_name "
                + "FROM bot_job bot "
                + "JOIN block b ON b.bot_job_id = bot.id "
                + "WHERE bot.active = 1 and bot.id = "
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
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error loadBlocksForBotJob for botJobId %d\nError: %s", botJobId, e.getMessage()));
        }

        return blockLoadList;
    }

    public int insertInstruction(
            InstructionLoadDTO InstructionLoadDTO, Integer currentBotJobId, Integer currentBlockId) {

        try (Statement stmt = getConnection().createStatement()) {
            Integer nextId =
                    InstructionLoadDTO.getId() == null ? loadNextIdInstructionData() + 1 : InstructionLoadDTO.getId();
            InstructionLoadDTO.setId(nextId);

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
            addColumnValue.accept("coordinates", InstructionLoadDTO.getCoordinates());
            addColumnValue.accept("iframe_xpath", InstructionLoadDTO.getIFrameXPath());
            addColumnValue.accept("xpath", InstructionLoadDTO.getXpath());
            addColumnValue.accept("action_custom_max_wait_sec", InstructionLoadDTO.getActionCustomMaxWaitSec());
            addColumnValue.accept("actions", InstructionLoadDTO.getActions());
            addColumnValue.accept("default_value", InstructionLoadDTO.getDefaultValue());
            addColumnValue.accept("description", InstructionLoadDTO.getDescription());
            addColumnValue.accept("instruction_order_number", InstructionLoadDTO.getInstructionOrderNumber());
            addColumnValue.accept("name", InstructionLoadDTO.getName());
            addColumnValue.accept(
                    "on_hold_seconds",
                    InstructionLoadDTO.getOnHoldSeconds() != null ? InstructionLoadDTO.getOnHoldSeconds() : 1);
            addColumnValue.accept("operation", InstructionLoadDTO.getOperation());
            addColumnValue.accept("parent_id", InstructionLoadDTO.getParentId());
            addColumnValue.accept("variable_id", InstructionLoadDTO.getVariableId());
            addColumnValue.accept("block_id", currentBlockId);
            addColumnValue.accept("bot_job_id", currentBotJobId);
            // Add boolean fields with conditional logic
            if (InstructionLoadDTO.getBlockMarked() != null) {
                addColumnValue.accept("block_marked", InstructionLoadDTO.getBlockMarked() ? 1 : 0);
            }

            if (InstructionLoadDTO.getCodified() != null) {
                addColumnValue.accept("codified", InstructionLoadDTO.getCodified() ? 1 : 0);
            }

            if (InstructionLoadDTO.getExportToABR() != null) {
                addColumnValue.accept("export_to_abr", InstructionLoadDTO.getExportToABR() ? 1 : 0);
            }

            if (InstructionLoadDTO.getOptional() != null) {
                addColumnValue.accept("optional", InstructionLoadDTO.getOptional() ? 1 : 0);
            }

            if (InstructionLoadDTO.getInstructionActive() != null) {
                addColumnValue.accept("active", InstructionLoadDTO.getInstructionActive() ? 1 : 0);
            }

            if (InstructionLoadDTO.getExecuted() != null) {
                addColumnValue.accept("executed", InstructionLoadDTO.getExecuted() ? 1 : 0);
            }

            if (InstructionLoadDTO.getBlockActive() != null) {
                addColumnValue.accept("block_active", InstructionLoadDTO.getBlockActive() ? 1 : 0);
            }

            if (InstructionLoadDTO.getRefreshLoop() != null) {
                addColumnValue.accept("refresh_loop", InstructionLoadDTO.getRefreshLoop() ? 1 : 0);
            }

            if (InstructionLoadDTO.getLoopOnly() != null) {
                addColumnValue.accept("loop_only", InstructionLoadDTO.getLoopOnly() ? 1 : 0);
            }

            if (InstructionLoadDTO.getForceCoordinates() != null) {
                addColumnValue.accept("force_coordinates", InstructionLoadDTO.getForceCoordinates() ? 1 : 0);
            }

            // Uncomment if needed
            // if (InstructionLoadDTO.getEditMode() != null) {
            //     addColumnValue.accept("edit_mode", InstructionLoadDTO.getEditMode() ? 1 : 0);
            // }

            // Construct final SQL query
            String insertSQL = String.format("INSERT INTO instruction (%s) VALUES (%s)", columns, values);

            int rowsAffected = stmt.executeUpdate(insertSQL);
            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format(
                                "New Instruction SAVED SUCCESSFULLY id: %d Name: %s Actions: %s Operation: %s",
                                InstructionLoadDTO.getId(),
                                InstructionLoadDTO.getName(),
                                InstructionLoadDTO.getActions(),
                                InstructionLoadDTO.getOperation()));
                return nextId;

            } else {
                ARLogger.getInstance(PerformDataBase.class)
                        .warning(String.format(
                                "Instruction NOT SAVED\nid: %d Name: %s Actions: %s Operations: %s",
                                InstructionLoadDTO.getId(),
                                InstructionLoadDTO.getName(),
                                InstructionLoadDTO.getActions(),
                                InstructionLoadDTO.getOperation()));
                return -1;
            }

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class)
                    .warning(String.format(
                            "Instruction NOT SAVED\nid: %d Name: %s Actions: %s Operations: %s",
                            InstructionLoadDTO.getId(),
                            InstructionLoadDTO.getName(),
                            InstructionLoadDTO.getActions(),
                            InstructionLoadDTO.getOperation()));
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

    public boolean preInsertStep(RowMoveDTO rowMoveDTO, List<InstructionLoadDTO> rowList) {
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
                for (InstructionLoadDTO instruction : rowList) {
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

        List<InstructionLoadDTO> rowList = getInstructionsByBlockId(rowMoveDTO.getBotJobId(), rowMoveDTO.getBlockId());

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
        List<InstructionLoadDTO> finalInstructionList = rowList;

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

    public static List<HomeBankingLoadDTO> loadAllHomeBanking() {
        List<HomeBankingLoadDTO> homeBankingList = new ArrayList<>();

        try (Statement stmt = getConnection().createStatement()) {

            // Select the home banking record based on homeBankingId
            String selectSQL =
                    "SELECT id, cookies, driver_session, name, options_config, password, priority, search_config, url, username "
                            + "FROM home_banking ";

            ResultSet rs = stmt.executeQuery(selectSQL);

            // Iterate through the result set and create HomeBankingLoadDTO objects
            while (rs.next()) {
                HomeBankingLoadDTO homeBanking = new HomeBankingLoadDTO();
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

                // Add the object to the list
                homeBankingList.add(homeBanking);
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Error selecting ALL home banking records");
        }

        return homeBankingList;
    }

    public static HomeBankingLoadDTO loadHomeBanking(int homeBankingId) {
        HomeBankingLoadDTO homeBanking = null;

        try (Statement stmt = getConnection().createStatement()) {

            // Select the home banking record based on homeBankingId
            String selectSQL = "SELECT * FROM home_banking home WHERE home.id = " + homeBankingId;

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
                + "  bot.id AS bot_job_id,  "
                + "  b.id AS block_id,  "
                + "  bli.id AS instruction_id,  "
                + "  bli.instruction_order_number,  "
                + "  bli.actions,  "
                + "  bli.name AS instruction_name,  "
                + "  bli.xpath,  "
                + "  bli.operation      "
                + " FROM bot_job bot  "
                + " LEFT JOIN block b ON b.bot_job_id = bot.id  "
                + " JOIN instruction bli ON bli.block_id = b.id  "
                + " where bot.active = 1 and bot.id = " + botJobId
                + "   and operation is null  "
                + "  ORDER BY bot.id, b.block_order_number, bli.instruction_order_number ASC;";

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

        // Determine if we need to use home_banking_id instead of bot_job_id
        String idColumn = targetTable.equalsIgnoreCase("component_variable") ? "home_banking_id" : "bot_job_id";

        // Build the base query
        String query = "SELECT var.id, var.name, var.type, var.value, var.instruction_id, var." + idColumn;

        // Adjust the query based on oldBlockId
        query += " FROM " + targetTable + " var";

        if (oldBlockId > -1) {
            query += " JOIN instruction bli ON bli.id = var.instruction_id AND var." + idColumn
                    + " = ? AND bli.block_id = ? ";
        } else {
            query += " WHERE var." + idColumn + " = ? ";
        }

        query += " ORDER BY var.id";

        List<VariableLoadDTO> variableDTOList = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(query)) {

            // Set parameters based on the existence of oldBlockId
            if (oldBlockId > -1) {
                stmt.setInt(1, oldBotJobId);
                stmt.setInt(2, oldBlockId);
            } else {
                stmt.setInt(1, oldBotJobId);
            }

            // Execute the query and process results
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                VariableLoadDTO variableDTO = new VariableLoadDTO();
                variableDTO.setId(rs.getInt("id"));
                variableDTO.setName(rs.getString("name"));
                variableDTO.setType(rs.getString("type"));
                variableDTO.setValue(rs.getString("value"));
                variableDTO.setInstructionId(rs.getInt("instruction_id"));

                if (targetTable.equalsIgnoreCase("instruction")) {
                    variableDTO.setBotJobId(rs.getInt("bot_job_id"));
                } else if (targetTable.equalsIgnoreCase("component_instruction")) {
                    variableDTO.setHomeBankingId(rs.getInt("home_banking_id"));
                }

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
            Connection conn, int oldBotJobId, int oldBlockId, String table1, String table2) throws SQLException {

        // Determine the column to use for filtering based on table1
        String idColumn = table1.equalsIgnoreCase("component_reference") ? "home_banking_id" : "bot_job_id";

        // Build the query string
        String query = "SELECT ref.id, ref.reference_type, ref.value, ref.instruction_id, ref." + idColumn;

        // Adjust the query based on oldBlockId
        query += " FROM " + table1 + " ref";

        // Adjust the query for oldBlockId
        if (oldBlockId > -1) {
            query += " JOIN " + table2 + " bli ON bli.id = ref.instruction_id and ref." + idColumn
                    + " = ? and bli.block_id = ? ";
        } else {
            query += " WHERE ref." + idColumn + " = ? ";
        }

        query += " ORDER BY ref.id";

        List<InstructionReferenceLoadDTO> referenceDTOList = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(query)) {

            // Set the parameters based on oldBlockId presence
            if (oldBlockId > -1) {
                stmt.setInt(1, oldBotJobId);
                stmt.setInt(2, oldBlockId);
            } else {
                stmt.setInt(1, oldBotJobId);
            }

            // Execute the query and process the results
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                InstructionReferenceLoadDTO referenceDTO = new InstructionReferenceLoadDTO();
                referenceDTO.setId(rs.getInt("id"));
                referenceDTO.setReferenceType(rs.getString("reference_type"));
                referenceDTO.setValue(rs.getString("value"));
                referenceDTO.setBlockLoopInstructionId(rs.getInt("instruction_id"));

                if (table1.equalsIgnoreCase("instruction")) {
                    referenceDTO.setBotJobId(rs.getInt("bot_job_id"));
                } else if (table1.equalsIgnoreCase("component_instruction")) {
                    referenceDTO.setHomeBankingId(rs.getInt("home_banking_id"));
                }

                referenceDTOList.add(referenceDTO); // Add to the list
            }
        }

        return referenceDTOList; // Return the list of reference DTOs
    }

    public List<ComplexInstructionLoadDTO> instComplexToDuplicate(
            Connection conn, int oldBotJobId, int oldBlockId, String table1, String table2) throws SQLException {
        String query = " SELECT \n" + "  cp.id, \n"
                + "  cp.instruction, \n"
                + "  cp.order_number, \n"
                + "  cp.way, \n"
                + "  cp.instruction_id, \n"
                + "  cp.bot_job_id \n"
                + "FROM \n"
                + table1 + " cp";

        if (oldBlockId > -1) {
            query += " JOIN " + table2
                    + " bli ON bli.id = cp.instruction_id and cp.bot_job_id = ? and bli.block_id = ?  ";
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
                ComplexInstructionLoadDTO complexInstructionLoadDTO = new ComplexInstructionLoadDTO();
                complexInstructionLoadDTO.setId(rs.getInt("id")); // Set the ID from complex_instruction
                complexInstructionLoadDTO.setInstructionId(
                        rs.getInt("instruction_id")); // Set the instruction_id as instructionId
                complexInstructionLoadDTO.setBotJobId(rs.getInt("bot_job_id")); // Set bot_job_id
                complexInstructionLoadDTO.setOrderNumber(rs.getInt("order_number"));
                complexInstructionLoadDTO.setInstruction(rs.getString("instruction"));
                complexInstructionLoadDTO.setWay(rs.getString("way"));

                referenceDTOList.add(complexInstructionLoadDTO);
            }
        }

        return referenceDTOList;
    }

    public List<InstructionLoadDTO> instructionsToDuplicate(
            Connection conn, int homeBankingId, int oldBotJobId, int oldBlockId, String table1, String table2)
            throws SQLException {

        // Determine the column to use for filtering
        String idColumn = table1.equalsIgnoreCase("component_instruction") ? "home_banking_id" : "bot_job_id";

        // Build the query
        String query = "SELECT bli.id, bli.action_custom_max_wait_sec, bli.actions, bli.active, bli.block_marked, "
                + "bli.codified, bli.default_value, bli.description, bli.export_to_abr, bli.instruction_order_number, "
                + "bli.name, bli.on_hold_seconds, bli.operation, bli.optional, bli.parent_id, bli.xpath, bli.coordinates, "
                + "bli.iframe_xpath, bli.force_coordinates, bli.variable_id, bli.block_id, blk.block_order_number, bli."
                + idColumn;

        query += " FROM " + table1 + " bli "
                + " JOIN " + table2 + " blk ON bli.block_id = blk.id "
                + " WHERE bli." + idColumn + " = ? ";

        if (oldBlockId > -1) {
            query += " and blk.id = ? ";
        }
        query += " order by blk.block_order_number, bli.instruction_order_number ";
        List<InstructionLoadDTO> InstructionLoadDTOList = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            if (table1.equalsIgnoreCase("instruction")) {
                stmt.setInt(1, oldBotJobId);
            } else if (table1.equalsIgnoreCase("component_instruction")) {
                stmt.setInt(1, homeBankingId);
            }

            if (oldBlockId > -1) {
                stmt.setInt(2, oldBlockId);
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                InstructionLoadDTO InstructionLoadDTO = new InstructionLoadDTO();
                InstructionLoadDTO.setId(rs.getInt("id")); // Holds the Current Ids
                InstructionLoadDTO.setActionCustomMaxWaitSec(rs.getInt("action_custom_max_wait_sec"));
                InstructionLoadDTO.setActions(rs.getString("actions"));
                InstructionLoadDTO.setInstructionActive(rs.getBoolean("active"));
                InstructionLoadDTO.setBlockMarked(rs.getBoolean("block_marked"));
                InstructionLoadDTO.setCodified(rs.getBoolean("codified"));
                InstructionLoadDTO.setDefaultValue(rs.getString("default_value"));
                InstructionLoadDTO.setDescription(rs.getString("description"));
                InstructionLoadDTO.setExportToABR(rs.getBoolean("export_to_abr"));
                InstructionLoadDTO.setInstructionOrderNumber(rs.getInt("instruction_order_number"));
                InstructionLoadDTO.setInstructionName(rs.getString("name"));
                InstructionLoadDTO.setOnHoldSeconds(rs.getInt("on_hold_seconds"));
                InstructionLoadDTO.setOperation(rs.getString("operation"));
                InstructionLoadDTO.setOptional(rs.getBoolean("optional"));
                InstructionLoadDTO.setParentId(rs.getInt("parent_id"));
                InstructionLoadDTO.setXpath(rs.getString("xpath"));
                InstructionLoadDTO.setCoordinates(rs.getString("coordinates"));
                InstructionLoadDTO.setForceCoordinates(rs.getBoolean("force_coordinates"));
                InstructionLoadDTO.setIFrameXPath(rs.getString("iframe_xpath"));
                InstructionLoadDTO.setVariableId(rs.getInt("variable_id"));
                InstructionLoadDTO.setBlockId(rs.getInt("block_id"));
                InstructionLoadDTO.setBlockOrderNumber(rs.getInt("block_order_number"));

                if (table1.equalsIgnoreCase("instruction")) {
                    InstructionLoadDTO.setBotJobId(rs.getInt("bot_job_id"));
                } else if (table1.equalsIgnoreCase("component_instruction")) {
                    InstructionLoadDTO.setHomeBankingId(rs.getInt("home_banking_id"));
                }

                InstructionLoadDTOList.add(InstructionLoadDTO);
            }
        }

        return InstructionLoadDTOList;
    }

    public ErrorMessage duplicateBotJobById(
            Connection conn,
            int homeBankId,
            int oldBotJobId,
            int newBotJobId,
            String newName,
            String newDescription,
            String[] arrayTables) {

        String botJobInsertQuery = "INSERT INTO bot_job (id, name, description, priority, home_banking_id, active) "
                + "SELECT ?, ?, ?, priority, home_banking_id, ? FROM bot_job WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(botJobInsertQuery)) {
            stmt.setInt(1, newBotJobId); // Set new name
            stmt.setString(2, newName); // Set new name
            stmt.setString(3, newDescription); // Set new description
            stmt.setInt(4, 1); //
            stmt.setInt(5, oldBotJobId); // Set original botJobId for the SELECT query
            stmt.executeUpdate();

            System.out.println("Generated BotJob ID: " + newBotJobId);

            // Now you can proceed with duplicating the related tables
            // arrayTables = {"block", "instruction", "reference", "complex_instruction", "variable"};
            ErrorMessage errorMessage = duplicateRelatedTables(conn, homeBankId, oldBotJobId, newBotJobId, arrayTables);
            if (errorMessage != null) {
                return errorMessage;
            }

            return null;
        } catch (SQLException error) {
            System.out.println(error.getMessage());
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

    //    public ErrorMessage migration2_6f(Connection conn, int oldBotJobId, int newBotJobId, String[] arrayTables)
    //            throws SQLException {
    //
    //        Map<Integer, Integer> blocksOlderAndNewId = new HashMap<>();
    //
    //        List<BlockLoadDTO> blockList = blocksToDuplicate(conn, oldBotJobId);
    //        if (blockList.size() > 0) {
    //            // tablesMigration = {"block", "block_loop_instruction", "instruction",
    // "instruction_reference","reference",
    //            // "variable"};
    //            // Assuming instList is a List<InstructionLoadDTO> and refersList is a
    // List<InstructionReferenceLoadDTO>
    //            for (BlockLoadDTO block : blockList) {
    //                blocksOlderAndNewId.put(block.getId(), block.getId());
    //                block.setId(block.getId());
    //                block.setBotJobId(newBotJobId);
    //            }
    //        }
    //
    //        Map<Integer, Integer> parentOlderAndNewId = new HashMap<>();
    //        Map<Integer, Integer> variableOlderAndNewId = new HashMap<>();
    //
    //        //  "block", "block_loop_instruction", "instruction", "instruction_reference", "reference", "variable"
    //        List<InstructionLoadDTO> instList = instructionsToDuplicate(
    //                conn, oldBotJobId, -1, arrayTables[1], arrayTables[0]); // "block_loop_instruction", "instruction"
    //
    //        List<VariableLoadDTO> varsList = instVariablesToDuplicateOLD(conn, oldBotJobId, arrayTables[5]);
    //
    //        // tablesMigration = {"block", "block_loop_instruction", "instruction",
    // "instruction_reference","reference",
    //        // "variable"};
    //        if (varsList.size() > 0) {
    //            for (VariableLoadDTO variable : varsList) {
    //                if (!variableOlderAndNewId.containsKey(variable.getId())) {
    //                    variableOlderAndNewId.put(variable.getId(), variable.getId());
    //                    variable.setId(variable.getId());
    //                }
    //            }
    //        }
    //
    //        if (instList.size() > 0) {
    //
    //            // Prepare the Ids
    //            // tablesMigration = {"block", "block_loop_instruction", "instruction",
    // "instruction_reference","reference",
    //            // "variable"};
    //            //            int currentId = getMaxId(conn, arrayTables[2]) + 1;
    //            for (InstructionLoadDTO instruction : instList) {
    //                instruction.setInstructionId(instruction.getId()); // Holds the News Ids
    //                instruction.setBotJobId(newBotJobId); // Holds the News Ids
    //
    //                if (!parentOlderAndNewId.containsKey(instruction.getParentId())) {
    //                    parentOlderAndNewId.put(instruction.getId(), instruction.getId());
    //                }
    //
    //                // Loop through the instList and find a matching InstructionLoadDTO
    //                for (BlockLoadDTO block : blockList) {
    //                    if (instruction.getBlockOrderNumber().equals(block.getBlockOrderNumber())) {
    //                        // Once found, update the blockLoopInstructionId with the new instructionId
    //                        instruction.setBlockId(block.getId());
    //                        break; // Exit the inner loop since we've found a match
    //                    }
    //                }
    //                //                currentId++;
    //            }
    //            // Duplicate instruction
    //            // tablesMigration = {"block", "block_loop_instruction", "instruction",
    // "instruction_reference","reference",
    //            // "variable"};
    //            ErrorMessage errorMessage = duplicateBlockLoopInstructions(
    //                    conn, instList, parentOlderAndNewId, variableOlderAndNewId, blocksOlderAndNewId,
    // arrayTables[2]);
    //            if (errorMessage != null) {
    //                return errorMessage;
    //            }
    //
    //            if (varsList.size() > 0) {
    //
    //                // Assuming instList is a List<InstructionLoadDTO> and refersList is a
    // List<InstructionReferenceLoadDTO>
    //                for (VariableLoadDTO variable : varsList) {
    //                    //                    variable.setId(currentVarId);
    //
    //                    // Loop through the instList and find a matching InstructionLoadDTO
    //                    for (InstructionLoadDTO instruction : instList) {
    //                        if (variable.getInstructionId().equals(instruction.getId())) {
    //                            // Once found, update the blockLoopInstructionId with the new instructionId
    //                            variable.setInstructionId(instruction.getInstructionId());
    //                            variable.setBotJobId(newBotJobId);
    //                            break; // Exit the inner loop since we've found a match
    //                        }
    //                    }
    //                    //                    currentVarId++;
    //                }
    //
    //                // Update variables
    //                // tablesMigration = {"block", "block_loop_instruction", "instruction", "instruction_reference",
    //                // "reference", "variable"};
    //                errorMessage = updateVariables(conn, varsList, arrayTables[5]);
    //                if (errorMessage != null) {
    //                    return errorMessage;
    //                }
    //            }
    //
    //            // tablesMigration = {"block", "block_loop_instruction", "instruction", "instruction_reference",
    //            // "reference", "variable"};
    //            List<InstructionReferenceLoadDTO> refersList =
    //                    instReferenceToDuplicateOld(conn, oldBotJobId, arrayTables[3]);
    //            if (refersList.size() > 0) {
    //
    //                //                currentId = getMaxId(conn, arrayTables[2]) + 1;
    //
    //                // Assuming instList is a List<InstructionLoadDTO> and refersList is a
    // List<InstructionReferenceLoadDTO>
    //                for (InstructionReferenceLoadDTO reference : refersList) {
    //                    //                    reference.setId(currentId++);
    //
    //                    // Loop through the instList and find a matching InstructionLoadDTO
    //                    for (InstructionLoadDTO instruction : instList) {
    //                        if (reference.getBlockLoopInstructionId().equals(instruction.getId())) {
    //                            // Once found, update the blockLoopInstructionId with the new instructionId
    //                            reference.setBlockLoopInstructionId(instruction.getInstructionId());
    //                            reference.setBotJobId(newBotJobId);
    //                            break; // Exit the inner loop since we've found a match
    //                        }
    //                    }
    //                }
    //
    //                // Duplicate reference
    //                // tablesMigration = {"block", "block_loop_instruction", "instruction", "instruction_reference",
    //                // "reference", "variable"};
    //                errorMessage = duplicateInstructionReferences(conn, refersList, arrayTables[4]);
    //                if (errorMessage != null) {
    //                    return errorMessage;
    //                }
    //            }
    //        }
    //        return null;
    //    }

    /***
     *  First Sequence   From Instructions tom Components
     * @param conn
     * @param blockDetailsDTO
     * @param arrayTables      // block_|_component_block_|_instruction_|_component_instruction_|_reference_|_component_reference_|_variable_|_component_variable_|_complex_|_component_complex
     *
     * @return
     * @throws SQLException
     */
    public ErrorMessage saveNewComponent(
            Connection conn, BlockDetailsDTO blockDetailsDTO, boolean injected, String[] arrayTables)
            throws SQLException {

        // block_|_component_block_|_instruction_|_component_instruction_|_reference_|_component_reference_|_variable_|_component_variable_|_complex_|_component_complex
        // component_block_|_block_|_component_instruction_|_instruction_|_component_reference_|_reference_|_component_variable_|_variable_|_component_complex_|_complex
        int newBotJobId = blockDetailsDTO.getBotJobId();
        int oldBlockId = blockDetailsDTO.getBlockId();
        int homeBankId = blockDetailsDTO.getHomeBankingId();

        Map<Integer, Integer> blocksOlderAndNewId = new HashMap<>();

        List<BlockLoadDTO> blockList = blockToComponent(conn, homeBankId, oldBlockId, arrayTables[0]); // block
        // Duplicate related blocks
        if (blockList.size() > 0) {

            // block_|_component_block_|_instruction_|_component_instruction_|_reference_|_component_reference_|_variable_|_component_variable_|_complex_|_component_complex
            // component_block_|_block_|_component_instruction_|_instruction_|_component_reference_|_reference_|_component_variable_|_variable_|_component_complex_|_complex
            int currentId = getMaxId(conn, arrayTables[1]) + 1; // block  or component_block

            int nextBlockOrder = -1;
            if (injected) {
                nextBlockOrder = loadNextBlockOrderNumber(blockDetailsDTO.getBotJobId()) + 1;
            }

            // Assuming instList is a List<InstructionLoadDTO> and refersList is a List<InstructionReferenceLoadDTO>
            for (BlockLoadDTO block : blockList) {
                blocksOlderAndNewId.put(block.getId(), currentId);
                block.setId(currentId);
                block.setBotJobId(newBotJobId); // NEW BOT JOB
                block.setHomeBankingId(homeBankId);
                block.setName(blockDetailsDTO.getBlockName());
                block.setDescription(blockDetailsDTO.getBlockDescription());

                if (injected) {
                    block.setBlockOrderNumber(nextBlockOrder);
                }

                currentId++;
            }
            // Duplicate Blocks
            // block_|_component_block_|_instruction_|_component_instruction_|_reference_|_component_reference_|_variable_|_component_variable_|_complex_|_component_complex
            // component_block_|_block_|_component_instruction_|_instruction_|_component_reference_|_reference_|_component_variable_|_variable_|_component_complex_|_complex
            ErrorMessage errorMessage = duplicateBlocks(conn, blockList, arrayTables[1]);
            if (errorMessage != null) {
                return errorMessage;
            }
        }

        Map<Integer, Integer> parentOlderAndNewId = new HashMap<>();
        Map<Integer, Integer> variableOlderAndNewId = new HashMap<>();

        // block_|_component_block_|_instruction_|_component_instruction_|_reference_|_component_reference_|_variable_|_component_variable_|_complex_|_component_complex
        // component_block_|_block_|_component_instruction_|_instruction_|_component_reference_|_reference_|_component_variable_|_variable_|_component_complex_|_complex
        List<InstructionLoadDTO> instList = instructionsToDuplicate(
                conn, homeBankId, newBotJobId, oldBlockId, arrayTables[2], arrayTables[0]); // instruction vs block

        // block_|_component_block_|_instruction_|_component_instruction_|_reference_|_component_reference_|_variable_|_component_variable_|_complex_|_component_complex
        List<VariableLoadDTO> varsList =
                instVariablesToDuplicateNEW(conn, newBotJobId, oldBlockId, arrayTables[6]); // variable

        // block_|_component_block_|_instruction_|_component_instruction_|_reference_|_component_reference_|_variable_|_component_variable_|_complex_|_component_complex
        // component_block_|_block_|_component_instruction_|_instruction_|_component_reference_|_reference_|_component_variable_|_variable_|_component_complex_|_complex
        int currentVarId = getMaxId(conn, arrayTables[7]) + 1; // component_variable

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
            // block_|_component_block_|_instruction_|_component_instruction_|_reference_|_component_reference_|_variable_|_component_variable_|_complex_|_component_complex
            // component_block_|_block_|_component_instruction_|_instruction_|_component_reference_|_reference_|_component_variable_|_variable_|_component_complex_|_complex
            int currentId = getMaxId(conn, arrayTables[3]) + 1; // component_instruction
            for (InstructionLoadDTO instruction : instList) {
                instruction.setInstructionId(currentId); // Holds the News Ids
                instruction.setBotJobId(newBotJobId); // Holds the News Ids

                if (!parentOlderAndNewId.containsKey(instruction.getParentId())) {
                    parentOlderAndNewId.put(instruction.getId(), currentId);
                }

                // Loop through the instList and find a matching InstructionLoadDTO
                if (injected) {
                    for (BlockLoadDTO block : blockList) {
                        // Once found, update the blockLoopInstructionId with the new instructionId
                        instruction.setBlockId(block.getId());
                        instruction.setHomeBankingId(block.getHomeBankingId());
                    }
                } else {
                    for (BlockLoadDTO block : blockList) {
                        if (instruction.getBlockOrderNumber().equals(block.getBlockOrderNumber())) {
                            // Once found, update the blockLoopInstructionId with the new instructionId
                            instruction.setBlockId(block.getId());
                            instruction.setHomeBankingId(block.getHomeBankingId());
                            break; // Exit the inner loop since we've found a match
                        }
                    }
                }
                currentId++;
            }
            // Duplicate instruction
            // block_|_component_block_|_instruction_|_component_instruction_|_reference_|_component_reference_|_variable_|_component_variable_|_complex_|_component_complex
            // component_block_|_block_|_component_instruction_|_instruction_|_component_reference_|_reference_|_component_variable_|_variable_|_component_complex_|_complex
            ErrorMessage errorMessage = duplicateBlockLoopInstructions(
                    conn,
                    instList,
                    parentOlderAndNewId,
                    variableOlderAndNewId,
                    blocksOlderAndNewId,
                    arrayTables[3]); // component_instruction
            if (errorMessage != null) {
                return errorMessage;
            }

            if (varsList.size() > 0) {

                // Assuming instList is a List<InstructionLoadDTO> and refersList is a List<InstructionReferenceLoadDTO>
                for (VariableLoadDTO variable : varsList) {
                    //                    variable.setId(currentVarId);
                    // Loop through the instList and find a matching InstructionLoadDTO
                    for (InstructionLoadDTO instruction : instList) {
                        if (variable.getInstructionId().equals(instruction.getId())) {
                            // Once found, update the blockLoopInstructionId with the new instructionId
                            variable.setInstructionId(instruction.getInstructionId());
                            variable.setBotJobId(newBotJobId);
                            variable.setHomeBankingId(instruction.getHomeBankingId());

                            break; // Exit the inner loop since we've found a match
                        }
                    }
                    //                    currentVarId++;
                }

                // Duplicate variable
                // block_|_component_block_|_instruction_|_component_instruction_|_reference_|_component_reference_|_variable_|_component_variable_|_complex_|_component_complex
                // component_block_|_block_|_component_instruction_|_instruction_|_component_reference_|_reference_|_component_variable_|_variable_|_component_complex_|_complex
                errorMessage = duplicateVariables(conn, varsList, arrayTables[7]); // component_variable
                if (errorMessage != null) {
                    return errorMessage;
                }
            }

            // block_|_component_block_|_instruction_|_component_instruction_|_reference_|_component_reference_|_variable_|_component_variable_|_complex_|_component_complex
            // component_block_|_block_|_component_instruction_|_instruction_|_component_reference_|_reference_|_component_variable_|_variable_|_component_complex_|_complex
            List<InstructionReferenceLoadDTO> refersList = instReferenceToDuplicateNew(
                    conn, newBotJobId, oldBlockId, arrayTables[4], arrayTables[2]); // reference
            if (refersList.size() > 0) {

                currentId = getMaxId(conn, arrayTables[5]) + 1; // component_reference vs reference

                // Assuming instList is a List<InstructionLoadDTO> and refersList is a List<InstructionReferenceLoadDTO>
                for (InstructionReferenceLoadDTO reference : refersList) {
                    reference.setId(currentId++);

                    // Loop through the instList and find a matching InstructionLoadDTO
                    for (InstructionLoadDTO instruction : instList) {
                        if (reference.getBlockLoopInstructionId().equals(instruction.getId())) {
                            // Once found, update the blockLoopInstructionId with the new instructionId
                            reference.setBlockLoopInstructionId(instruction.getInstructionId());
                            reference.setBotJobId(newBotJobId);
                            reference.setHomeBankingId(instruction.getHomeBankingId());
                            break; // Exit the inner loop since we've found a match
                        }
                    }
                }

                // Duplicate reference
                // block_|_component_block_|_instruction_|_component_instruction_|_reference_|_component_reference_|_variable_|_component_variable_|_complex_|_component_complex
                // component_block_|_block_|_component_instruction_|_instruction_|_component_reference_|_reference_|_component_variable_|_variable_|_component_complex_|_complex
                errorMessage = duplicateInstructionReferences(conn, refersList, arrayTables[5]); // component_reference
                if (errorMessage != null) {
                    return errorMessage;
                }
            }

            // block_|_component_block_|_instruction_|_component_instruction_|_reference_|_component_reference_|_variable_|_component_variable_|_complex_|_component_complex
            // component_block_|_block_|_component_instruction_|_instruction_|_component_reference_|_reference_|_component_variable_|_variable_|_component_complex_|_complex
            //            List<ComplexInstructionLoadDTO> complexList =
            //                    instComplexToDuplicate(conn, newBotJobId, oldBlockId, arrayTables[8], arrayTables[2]);
            // // complex
            //            if (complexList.size() > 0) {
            //
            //                //
            // block_|_component_block_|_instruction_|_component_instruction_|_reference_|_component_reference_|_variable_|_component_variable_|_complex_|_component_complex
            //                //
            // component_block_|_block_|_component_instruction_|_instruction_|_component_reference_|_reference_|_component_variable_|_variable_|_component_complex_|_complex
            //                currentId = getMaxId(conn, arrayTables[9]) + 1; // component_complex
            //
            //                // Assuming instList is a List<InstructionLoadDTO> and refersList is a
            // List<InstructionReferenceLoadDTO>
            //                for (ComplexInstructionLoadDTO complex : complexList) {
            //                    complex.setId(currentId++);
            //
            //                    // Loop through the instList and find a matching InstructionLoadDTO
            //                    for (InstructionLoadDTO instruction : instList) {
            //                        if (complex.getInstructionId().equals(instruction.getId())) {
            //                            // Once found, update the blockLoopInstructionId with the new instructionId
            //                            complex.setInstructionId(instruction.getInstructionId());
            //                            complex.setBotJobId(newBotJobId);
            //                            break; // Exit the inner loop since we've found a match
            //                        }
            //                    }
            //                }
            //
            //                // Duplicate complex_instruction
            //                //
            // block_|_component_block_|_instruction_|_component_instruction_|_reference_|_component_reference_|_variable_|_component_variable_|_complex_|_component_complex
            //                //
            // component_block_|_block_|_component_instruction_|_instruction_|_component_reference_|_reference_|_component_variable_|_variable_|_component_complex_|_complex
            //                errorMessage = duplicateComplexInstructions(conn, complexList, arrayTables[9]); // //
            // component_complex
            //                if (errorMessage != null) {
            //                    return errorMessage;
            //                }
            //            }
        }
        return null;
    }

    public ErrorMessage duplicateRelatedTables(
            Connection conn, int homeBankId, int oldBotJobId, int newBotJobId, String[] arrayTables)
            throws SQLException {

        Map<Integer, Integer> blocksOlderAndNewId = new HashMap<>();

        List<BlockLoadDTO> blockList = blocksToDuplicate(conn, oldBotJobId);
        // Duplicate related blocks
        if (blockList.size() > 0) {

            // arrayTables = {"block", "instruction", "reference", "complex_instruction", "variable"};
            int currentId = getMaxId(conn, arrayTables[0]) + 1; // block  or component_block

            // Assuming instList is a List<InstructionLoadDTO> and refersList is a List<InstructionReferenceLoadDTO>
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
        List<InstructionLoadDTO> instList = instructionsToDuplicate(
                conn, homeBankId, oldBotJobId, -1, arrayTables[1], arrayTables[0]); // instruction

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
            for (InstructionLoadDTO instruction : instList) {
                instruction.setInstructionId(currentId); // Holds the News Ids
                instruction.setBotJobId(newBotJobId); // Holds the News Ids

                if (!parentOlderAndNewId.containsKey(instruction.getParentId())) {
                    parentOlderAndNewId.put(instruction.getId(), currentId);
                }

                // Loop through the instList and find a matching InstructionLoadDTO
                for (BlockLoadDTO block : blockList) {
                    if (instruction.getBlockOrderNumber().equals(block.getBlockOrderNumber())) {
                        // Once found, update the blockLoopInstructionId with the new instructionId
                        instruction.setBlockId(block.getId());
                        instruction.setHomeBankingId(block.getHomeBankingId());
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

                // Assuming instList is a List<InstructionLoadDTO> and refersList is a List<InstructionReferenceLoadDTO>
                for (VariableLoadDTO variable : varsList) {
                    //                    variable.setId(currentVarId);

                    // Loop through the instList and find a matching InstructionLoadDTO
                    for (InstructionLoadDTO instruction : instList) {
                        if (variable.getInstructionId().equals(instruction.getId())) {
                            // Once found, update the blockLoopInstructionId with the new instructionId
                            variable.setInstructionId(instruction.getInstructionId());
                            variable.setBotJobId(newBotJobId);
                            variable.setHomeBankingId(instruction.getHomeBankingId());
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
                    instReferenceToDuplicateNew(conn, oldBotJobId, -1, arrayTables[2], arrayTables[1]);
            if (refersList.size() > 0) {

                currentId = getMaxId(conn, arrayTables[2]) + 1;

                // Assuming instList is a List<InstructionLoadDTO> and refersList is a List<InstructionReferenceLoadDTO>
                for (InstructionReferenceLoadDTO reference : refersList) {
                    reference.setId(currentId++);
                    reference.setHomeBankingId(reference.getHomeBankingId());

                    // Loop through the instList and find a matching InstructionLoadDTO
                    for (InstructionLoadDTO instruction : instList) {
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
            //            List<ComplexInstructionLoadDTO> complexList =
            //                    instComplexToDuplicate(conn, oldBotJobId, -1, arrayTables[3], arrayTables[1]);
            //            if (complexList.size() > 0) {
            //
            //                currentId = getMaxId(conn, arrayTables[3]) + 1;
            //
            //                // Assuming instList is a List<InstructionLoadDTO> and refersList is a
            // List<InstructionReferenceLoadDTO>
            //                for (ComplexInstructionLoadDTO complex : complexList) {
            //                    complex.setId(currentId++);
            //
            //                    // Loop through the instList and find a matching InstructionLoadDTO
            //                    for (InstructionLoadDTO instruction : instList) {
            //                        if (complex.getInstructionId().equals(instruction.getId())) {
            //                            // Once found, update the blockLoopInstructionId with the new instructionId
            //                            complex.setInstructionId(instruction.getInstructionId());
            //                            complex.setBotJobId(newBotJobId);
            //                            break; // Exit the inner loop since we've found a match
            //                        }
            //                    }
            //                }
            //
            //                // Duplicate complex_instruction
            //                // arrayTables = {"block", "instruction", "reference", "complex_instruction", "variable"};
            //                errorMessage = duplicateComplexInstructions(conn, complexList, arrayTables[3]);
            //                if (errorMessage != null) {
            //                    return errorMessage;
            //                }
            //            }
        }
        return null;
    }

    public List<BlockLoadDTO> blockToComponent(Connection conn, int homeBankingId, int oldBlockId, String tableName)
            throws SQLException {
        String query = "SELECT * FROM " + tableName + " WHERE id = ? ";

        if (tableName.equals("component_block")) {
            query += "and home_banking_id = ?";
        }

        List<BlockLoadDTO> blockDTOList = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, oldBlockId);
            if (tableName.equals("component_block")) {
                stmt.setInt(2, homeBankingId);
            }
            // Set the oldBotJobId parameter
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                BlockLoadDTO blockDTO = new BlockLoadDTO();

                if (tableName.equals("component_block")) {
                    blockDTO.setHomeBankingId(rs.getInt("home_banking_id"));
                }

                blockDTO.setId(rs.getInt("id"));
                blockDTO.setBlockOrderNumber(rs.getInt("block_order_number"));
                blockDTO.setName(rs.getString("name"));
                blockDTO.setDescription(rs.getString("description"));
                blockDTO.setTypeId(rs.getInt("type_id"));
                blockDTO.setWait(rs.getInt("wait"));
                blockDTO.setExportFile(rs.getString("export_file"));

                // Include bot_job_id if tableName is "block"
                if (tableName.equalsIgnoreCase("block")) {
                    blockDTO.setBotJobId(rs.getInt("bot_job_id"));
                }
                // Include home_banking_id if tableName is "component_block"
                if (tableName.equalsIgnoreCase("component_block")) {
                    blockDTO.setHomeBankingId(rs.getInt("home_banking_id"));
                }

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
        String blockInsertQuery;
        String strValues;

        if (tableTarget.equals("component_block")) {
            // component_block should have home_banking_id but no bot_job_id
            blockInsertQuery = "INSERT INTO component_block "
                    + "(id, block_order_number, name, description, type_id, export_file, active, wait, home_banking_id) ";
            strValues = "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        } else {
            // block should have bot_job_id but no home_banking_id
            blockInsertQuery = "INSERT INTO block "
                    + "(id, block_order_number, name, description, type_id, bot_job_id, export_file, active, wait) ";
            strValues = "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        }

        blockInsertQuery = blockInsertQuery + strValues;

        try (PreparedStatement blockStmt = conn.prepareStatement(blockInsertQuery)) {
            for (BlockLoadDTO block : blockList) {
                int index = 1;
                blockStmt.setInt(index++, block.getId());
                blockStmt.setInt(index++, block.getBlockOrderNumber());
                blockStmt.setString(index++, block.getName());
                blockStmt.setString(index++, block.getDescription());
                blockStmt.setInt(index++, block.getTypeId());

                if (tableTarget.equals("block")) {
                    // Set bot_job_id only for "block"
                    blockStmt.setInt(index++, block.getBotJobId());
                }

                blockStmt.setString(index++, block.getExportFile());
                blockStmt.setInt(index++, (block.getActive() ? 1 : 0));
                blockStmt.setInt(index++, block.getWait());

                if (tableTarget.equals("component_block")) {
                    // Set home_banking_id only for "component_block"
                    blockStmt.setInt(index++, block.getHomeBankingId());
                }

                blockStmt.addBatch(); // Add the current block to the batch
            }

            blockStmt.executeBatch(); // Execute the batch insert
            return null;
        } catch (SQLException error) {
            System.out.println(error.getMessage());
            return new ErrorMessage("Error Duplicating Blocks", "Block Insertion Failure", error.getMessage());
        }
    }

    private ErrorMessage duplicateBlockLoopInstructions(
            Connection conn,
            List<InstructionLoadDTO> instList,
            Map<Integer, Integer> parentOlderAndNewId,
            Map<Integer, Integer> variableOlderAndNewId,
            Map<Integer, Integer> blocksOlderAndNewId,
            String targetTable) {

        // Dynamically construct the INSERT query
        String blockLoopInstructionInsertQuery = "INSERT INTO " + targetTable
                + " (id, action_custom_max_wait_sec, actions, active, block_marked, codified, "
                + "default_value, description, export_to_abr, instruction_order_number, name, on_hold_seconds, operation, optional, "
                + "parent_id, xpath, coordinates, force_coordinates, iframe_xpath, variable_id, block_id";

        if (targetTable.equalsIgnoreCase("instruction")) {
            blockLoopInstructionInsertQuery += ", bot_job_id";
        } else if (targetTable.equalsIgnoreCase("component_instruction")) {
            blockLoopInstructionInsertQuery += ", home_banking_id";
        }

        blockLoopInstructionInsertQuery += ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?";

        if (targetTable.equalsIgnoreCase("instruction") || targetTable.equalsIgnoreCase("component_instruction")) {
            blockLoopInstructionInsertQuery += ", ?";
        }

        blockLoopInstructionInsertQuery += ")";

        try (PreparedStatement blockLoopStmt = conn.prepareStatement(blockLoopInstructionInsertQuery)) {
            for (InstructionLoadDTO instruction : instList) {
                Integer newParentId = instruction.getActions().equals(ARConstants.GOTO)
                        ? blocksOlderAndNewId.get(instruction.getParentId())
                        : parentOlderAndNewId.get(instruction.getParentId());

                Integer newVariableId = variableOlderAndNewId.get(instruction.getVariableId());

                blockLoopStmt.setInt(1, instruction.getInstructionId());
                blockLoopStmt.setInt(2, instruction.getActionCustomMaxWaitSec());
                blockLoopStmt.setString(3, instruction.getActions());
                blockLoopStmt.setInt(4, instruction.getInstructionActive() ? 1 : 0);
                blockLoopStmt.setInt(5, instruction.getBlockMarked() ? 1 : 0);
                blockLoopStmt.setInt(6, instruction.getCodified() ? 1 : 0);

                if (instruction.getDefaultValue() != null) {
                    blockLoopStmt.setString(7, instruction.getDefaultValue());
                } else {
                    blockLoopStmt.setNull(7, Types.VARCHAR);
                }

                blockLoopStmt.setString(8, instruction.getDescription());
                blockLoopStmt.setInt(9, instruction.getExportToABR() ? 1 : 0);
                blockLoopStmt.setInt(10, instruction.getInstructionOrderNumber());
                blockLoopStmt.setString(11, instruction.getInstructionName());
                blockLoopStmt.setInt(12, instruction.getOnHoldSeconds());

                if (instruction.getOperation() != null) {
                    blockLoopStmt.setString(13, instruction.getOperation());
                } else {
                    blockLoopStmt.setNull(13, Types.VARCHAR);
                }

                blockLoopStmt.setInt(14, instruction.getOptional() ? 1 : 0);

                if (instruction.getParentId() != null && instruction.getParentId() > 0) {
                    blockLoopStmt.setInt(15, newParentId != null ? newParentId : instruction.getParentId());
                } else {
                    blockLoopStmt.setNull(15, Types.INTEGER);
                }

                if (instruction.getXpath() != null) {
                    blockLoopStmt.setString(16, instruction.getXpath());
                } else {
                    blockLoopStmt.setNull(16, Types.VARCHAR);
                }

                if (!Strings.isNullOrEmpty(instruction.getCoordinates())) {
                    blockLoopStmt.setString(17, instruction.getCoordinates());
                } else {
                    blockLoopStmt.setNull(17, Types.VARCHAR);
                }

                blockLoopStmt.setInt(18, instruction.getForceCoordinates() ? 1 : 0);

                if (!Strings.isNullOrEmpty(instruction.getIFrameXPath())) {
                    blockLoopStmt.setString(19, instruction.getIFrameXPath());
                } else {
                    blockLoopStmt.setNull(19, Types.VARCHAR);
                }

                if (instruction.getVariableId() != null && instruction.getVariableId() > 0) {
                    blockLoopStmt.setInt(20, newVariableId != null ? newVariableId : instruction.getVariableId());
                } else {
                    blockLoopStmt.setNull(20, Types.INTEGER);
                }

                blockLoopStmt.setInt(21, instruction.getBlockId());

                int paramIndex = 22;
                if (targetTable.equalsIgnoreCase("instruction")) {
                    blockLoopStmt.setInt(paramIndex, instruction.getBotJobId());
                } else if (targetTable.equalsIgnoreCase("component_instruction")) {
                    blockLoopStmt.setInt(paramIndex, instruction.getHomeBankingId());
                }

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

        // Construct the base query
        String instructionReferenceInsertQuery =
                "INSERT INTO " + targetTable + " (id, reference_type, value, instruction_id";

        if (targetTable.equalsIgnoreCase("reference")) {
            instructionReferenceInsertQuery += ", bot_job_id";
        } else if (targetTable.equalsIgnoreCase("component_reference")) {
            instructionReferenceInsertQuery += ", home_banking_id";
        }

        instructionReferenceInsertQuery += ") VALUES (?, ?, ?, ?";

        if (targetTable.equalsIgnoreCase("reference") || targetTable.equalsIgnoreCase("component_reference")) {
            instructionReferenceInsertQuery += ", ?";
        }

        instructionReferenceInsertQuery += ")";

        try (PreparedStatement refStmt = conn.prepareStatement(instructionReferenceInsertQuery)) {
            for (InstructionReferenceLoadDTO reference : refersList) {
                refStmt.setInt(1, reference.getId());
                refStmt.setString(2, reference.getReferenceType());
                refStmt.setString(3, reference.getValue());
                refStmt.setInt(4, reference.getBlockLoopInstructionId());

                int paramIndex = 5;
                if (targetTable.equalsIgnoreCase("reference")) {
                    refStmt.setInt(paramIndex, reference.getBotJobId());
                } else if (targetTable.equalsIgnoreCase("component_reference")) {
                    refStmt.setInt(paramIndex, reference.getHomeBankingId());
                }

                refStmt.addBatch(); // Add to batch
            }

            refStmt.executeBatch(); // Execute the batch insert
            return null;
        } catch (SQLException error) {
            System.out.println(error.getMessage());
            return new ErrorMessage("Error Duplicating References", "Reference Insertion Failure", error.getMessage());
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

        // Construct the base query
        String variableInsertQuery = "INSERT INTO " + targetTable + " (id, name, type, value, instruction_id";

        if (targetTable.equalsIgnoreCase("variable")) {
            variableInsertQuery += ", bot_job_id";
        } else if (targetTable.equalsIgnoreCase("component_variable")) {
            variableInsertQuery += ", home_banking_id";
        }

        variableInsertQuery += ") VALUES (?, ?, ?, ?, ?";

        if (targetTable.equalsIgnoreCase("variable") || targetTable.equalsIgnoreCase("component_variable")) {
            variableInsertQuery += ", ?";
        }

        variableInsertQuery += ")";

        try (PreparedStatement varStmt = conn.prepareStatement(variableInsertQuery)) {
            for (VariableLoadDTO variableDTO : varsList) {
                varStmt.setInt(1, variableDTO.getId());
                varStmt.setString(2, variableDTO.getName());
                varStmt.setString(3, variableDTO.getType());
                varStmt.setString(4, variableDTO.getValue());
                varStmt.setInt(5, variableDTO.getInstructionId());

                int paramIndex = 6;
                if (targetTable.equalsIgnoreCase("variable")) {
                    varStmt.setInt(paramIndex, variableDTO.getBotJobId());
                } else if (targetTable.equalsIgnoreCase("component_variable")) {
                    varStmt.setInt(paramIndex, variableDTO.getHomeBankingId());
                }

                varStmt.addBatch(); // Add to batch
            }

            varStmt.executeBatch(); // Execute the batch insert
            return null;
        } catch (SQLException error) {
            System.out.println(error.getMessage());
            return new ErrorMessage("Error Duplicating Variables", "Variable Insertion Failure", error.getMessage());
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

    public static List<InstructionLoadDTO> filterInstructions(List<InstructionLoadDTO> instructionList) {
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
        List<InstructionLoadDTO> rowList = null;
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
                ARLogger.getInstance(PerformDataBase.class).finer("Data deleted successfully for: " + instructionId);
            } else {
                ARLogger.getInstance(PerformDataBase.class)
                        .finer("No matching record found to delete for: " + instructionId);
            }
        }
    }

    private void deleteInstrReference(int instructionId) throws SQLException {
        String deleteSQL = "delete FROM reference " + " where instruction_id =  " + instructionId;

        try (Statement stmt = getConnection().createStatement()) {
            int rowsAffected = stmt.executeUpdate(deleteSQL);
            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class).finer("Data deleted successfully for: " + instructionId);
            } else {
                ARLogger.getInstance(PerformDataBase.class)
                        .finer("No matching record found to delete for: " + instructionId);
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
                ARLogger.getInstance(PerformDataBase.class).finer("Data deleted successfully for: " + instructionId);
            } else {
                ARLogger.getInstance(PerformDataBase.class)
                        .finer("No matching record found to delete for: " + instructionId);
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
                ARLogger.getInstance(PerformDataBase.class).finer("Data deleted successfully for: " + instructionId);
            } else {
                ARLogger.getInstance(PerformDataBase.class)
                        .finer("No matching record found to delete for: " + instructionId);
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
                + "  bot.id AS bot_job_id, \n"
                + "  bot.name AS bot_job_name \n"
                + "  FROM \n"
                + "  component_block bc \n"
                + "  JOIN bot_job bot on bot.active = 1 and bot.id = bc.bot_job_id \n"
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
            ARLogger.getInstance(PerformDataBase.class)
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

                if (reference.getReferenceType().equalsIgnoreCase("customXPath")) {
                    continue;
                }

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
    public static void deleteComponent(InstructionLoadDTO deleteInstructionLoad) {
        if (deleteCompVariable(deleteInstructionLoad))
            if (deleteCompReferences(deleteInstructionLoad))
                if (deleteCompInstruction(deleteInstructionLoad)) {
                    deleteCompNullBlocks(deleteInstructionLoad.getHomeBankingId(), deleteInstructionLoad.getBotJobId());
                    //                    updateBlockOrderNumber(selectAllBlocks(deleteInstructionLoadDTO.getBlockId()),
                    // true);
                }
    }

    public static void initializeMainDatabaseAccess(String dbUrl, File dbFile) {

        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            try (Statement stmt = conn.createStatement()) {

                // Create home_banking table
                String createHomeBankingTableSQL = "CREATE TABLE home_banking ("
                        + "ID INTEGER PRIMARY KEY, "
                        + "url MEMO, "
                        + "name TEXT, "
                        + "priority MEMO, "
                        + "search_config MEMO, "
                        + "options_config MEMO, "
                        + "cookies MEMO, "
                        + "driver_session MEMO, "
                        + "username TEXT, "
                        + "password TEXT)";
                stmt.executeUpdate(createHomeBankingTableSQL);

                // Create bot_job table with a foreign key reference to home_banking
                String createBotJobTableSQL = "CREATE TABLE bot_job ("
                        + "id INTEGER PRIMARY KEY, "
                        + "name TEXT UNIQUE, "
                        + "description TEXT, "
                        + "priority MEMO, "
                        + "active YESNO NOT NULL, "
                        + "home_banking_id INTEGER);";
                stmt.executeUpdate(createBotJobTableSQL);

                String addBotJobForeignKeySQL = "ALTER TABLE bot_job "
                        + "ADD CONSTRAINT FK_BotJob FOREIGN KEY (home_banking_id) "
                        + "REFERENCES home_banking(id) ON DELETE CASCADE";
                stmt.executeUpdate(addBotJobForeignKeySQL);

                // Create block table with a foreign key reference to bot_job
                String createBlockTableSQL = "CREATE TABLE block ("
                        + "id INTEGER PRIMARY KEY, "
                        + "block_order_number INTEGER NOT NULL, "
                        + "name TEXT NOT NULL, "
                        + "description TEXT, "
                        + "type_id INTEGER, "
                        + "export_file TEXT, "
                        + "active YESNO NOT NULL, "
                        + "wait INTEGER, "
                        + "bot_job_id INTEGER);";
                stmt.executeUpdate(createBlockTableSQL);

                String addForeignKeySQL2 = "ALTER TABLE block "
                        + "ADD CONSTRAINT FK_2 FOREIGN KEY (bot_job_id) "
                        + "REFERENCES bot_job(id) ON DELETE CASCADE";
                stmt.executeUpdate(addForeignKeySQL2);

                // Create instruction table with foreign key references to block and bot_job
                String createInstructionTableSQL = "CREATE TABLE instruction ("
                        + "id INTEGER PRIMARY KEY, "
                        + "instruction_order_number INTEGER NOT NULL, "
                        + "actions MEMO, "
                        + "name TEXT, "
                        + "xpath MEMO, "
                        + "coordinates TEXT, "
                        + "force_coordinates YESNO, "
                        + "iframe_xpath MEMO, "
                        + "description TEXT, "
                        + "operation TEXT, "
                        + "optional YESNO, "
                        + "block_marked YESNO, "
                        + "default_value TEXT, "
                        + "action_custom_max_wait_sec INTEGER, "
                        + "on_hold_seconds INTEGER, "
                        + "codified YESNO, "
                        + "export_to_abr YESNO, "
                        + "active YESNO NOT NULL, "
                        + "block_id INTEGER, "
                        + "variable_id INTEGER, "
                        + "parent_id INTEGER, "
                        + "bot_job_id INTEGER);";
                stmt.executeUpdate(createInstructionTableSQL);

                String addForeignKeySQL3 = "ALTER TABLE instruction "
                        + "ADD CONSTRAINT FK_3 FOREIGN KEY (block_id) "
                        + "REFERENCES block(id) ON DELETE CASCADE";
                stmt.executeUpdate(addForeignKeySQL3);

                String addForeignKeySQL4 = "ALTER TABLE instruction "
                        + "ADD CONSTRAINT FK_4 FOREIGN KEY (bot_job_id) "
                        + "REFERENCES bot_job(id) ON DELETE CASCADE";
                stmt.executeUpdate(addForeignKeySQL4);

                String createReferenceTableSQL = "CREATE TABLE reference ("
                        + "id INTEGER PRIMARY KEY, "
                        + "reference_type TEXT, "
                        + "value MEMO, "
                        + "instruction_id INTEGER NOT NULL, "
                        + "bot_job_id INTEGER);";
                stmt.executeUpdate(createReferenceTableSQL);

                String addForeignKeySQL5 = "ALTER TABLE reference "
                        + "ADD CONSTRAINT FK_5 FOREIGN KEY (instruction_id) "
                        + "REFERENCES instruction(id) ON DELETE CASCADE";
                stmt.executeUpdate(addForeignKeySQL5);

                String addForeignKeySQL6 = "ALTER TABLE reference "
                        + "ADD CONSTRAINT FK_6 FOREIGN KEY (bot_job_id) "
                        + "REFERENCES bot_job(id) ON DELETE CASCADE";
                stmt.executeUpdate(addForeignKeySQL6);

                String createVariableTableSQL = "CREATE TABLE variable ("
                        + "id INTEGER PRIMARY KEY, "
                        + "type TEXT, "
                        + "name TEXT, "
                        + "value MEMO, "
                        + "instruction_id INTEGER, "
                        + "bot_job_id INTEGER);";
                stmt.executeUpdate(createVariableTableSQL);

                String addForeignKeySQL7 = "ALTER TABLE variable "
                        + "ADD CONSTRAINT FK_7 FOREIGN KEY (instruction_id) "
                        + "REFERENCES instruction(id) ON DELETE CASCADE";
                stmt.executeUpdate(addForeignKeySQL7);

                String addForeignKeySQL8 = "ALTER TABLE variable "
                        + "ADD CONSTRAINT FK_8 FOREIGN KEY (bot_job_id) "
                        + "REFERENCES bot_job(id) ON DELETE CASCADE";
                stmt.executeUpdate(addForeignKeySQL8);

                //                String createConfigurationTableSQL = "CREATE TABLE configuration ("
                //                        + "id INTEGER PRIMARY KEY, "
                //                        + "pathJava MEMO, "
                //                        + "logLevel TEXT, "
                //                        + "pathDB TEXT, "
                //                        + "interactionTimeoutSec TEXT, "
                //                        + "pathLog MEMO, "
                //                        + "defaultInstructionStopSeconds TEXT, "
                //                        + "pathReport TEXT, "
                //                        + "browser MEMO, "
                //                        + "dataBaseType TEXT, "
                //                        + "pageUpdateTimeoutSec TEXT, "
                //                        + "pathPriority TEXT, "
                //                        + "pathEngine TEXT, "
                //                        + "pathExcel TEXT, "
                //                        + "pathExport TEXT, "
                //                        + "socketPort TEXT, "
                //                        + "blockLimit TEXT, "
                //                        + "pathJavaFx TEXT)";
                //                stmt.executeUpdate(createConfigurationTableSQL);

                String createComponentBlockTableSQL = "CREATE TABLE component_block ("
                        + "id INTEGER PRIMARY KEY, "
                        + "home_banking_id INTEGER, "
                        + "block_order_number INTEGER NOT NULL, "
                        + "name TEXT NOT NULL, "
                        + "description TEXT, "
                        + "type_id INTEGER, "
                        + "export_file TEXT, "
                        + "active YESNO, "
                        + "wait INTEGER);";
                stmt.executeUpdate(createComponentBlockTableSQL);

                String addForeignKeySQL9 = "ALTER TABLE component_block "
                        + "ADD CONSTRAINT FK_9 FOREIGN KEY (home_banking_id) "
                        + "REFERENCES home_banking(id) ON DELETE CASCADE";
                stmt.executeUpdate(addForeignKeySQL9);

                String createComponentInstructionTableSQL = "CREATE TABLE component_instruction ("
                        + "id INTEGER PRIMARY KEY, "
                        + "instruction_order_number INTEGER NOT NULL, "
                        + "actions MEMO, "
                        + "name TEXT, "
                        + "xpath MEMO, "
                        + "coordinates TEXT, "
                        + "force_coordinates YESNO, "
                        + "iframe_xpath MEMO, "
                        + "description TEXT, "
                        + "operation TEXT, "
                        + "optional YESNO, "
                        + "block_marked YESNO, "
                        + "default_value TEXT, "
                        + "action_custom_max_wait_sec INTEGER, "
                        + "on_hold_seconds INTEGER, "
                        + "codified YESNO, "
                        + "export_to_abr YESNO, "
                        + "active YESNO NOT NULL, "
                        + "block_id INTEGER, "
                        + "variable_id INTEGER, "
                        + "parent_id INTEGER, "
                        + "home_banking_id INTEGER);";
                stmt.executeUpdate(createComponentInstructionTableSQL);

                String addForeignKeySQL10 = "ALTER TABLE component_instruction "
                        + "ADD CONSTRAINT FK_10 FOREIGN KEY (block_id) "
                        + "REFERENCES component_block(id) ON DELETE CASCADE";
                stmt.executeUpdate(addForeignKeySQL10);

                String addCompBlkHomeForeignKeySQL = "ALTER TABLE component_instruction "
                        + "ADD CONSTRAINT FK_BLKHomeBank FOREIGN KEY (home_banking_id) "
                        + "REFERENCES home_banking(id) ON DELETE CASCADE";
                stmt.executeUpdate(addCompBlkHomeForeignKeySQL);

                String createComponentReferenceTableSQL = "CREATE TABLE component_reference ("
                        + "id INTEGER PRIMARY KEY, "
                        + "reference_type TEXT, "
                        + "value MEMO, "
                        + "instruction_id INTEGER NOT NULL, "
                        + "home_banking_id INTEGER);";
                stmt.executeUpdate(createComponentReferenceTableSQL);

                String addForeignKeySQL11 = "ALTER TABLE component_reference "
                        + "ADD CONSTRAINT FK_11 FOREIGN KEY (instruction_id) "
                        + "REFERENCES component_instruction(id) ON DELETE CASCADE";
                stmt.executeUpdate(addForeignKeySQL11);

                String addCompReferForeignKeySQL = "ALTER TABLE component_reference "
                        + "ADD CONSTRAINT FK_CompRefer FOREIGN KEY (home_banking_id) "
                        + "REFERENCES home_banking(id) ON DELETE CASCADE";
                stmt.executeUpdate(addCompReferForeignKeySQL);

                String createComponentVariableTableSQL = "CREATE TABLE component_variable ("
                        + "id INTEGER PRIMARY KEY, "
                        + "type TEXT, "
                        + "name TEXT, "
                        + "value MEMO, "
                        + "instruction_id INTEGER, "
                        + "home_banking_id INTEGER);";
                stmt.executeUpdate(createComponentVariableTableSQL);

                String addForeignKeySQL12 = "ALTER TABLE component_variable "
                        + "ADD CONSTRAINT FK_12 FOREIGN KEY (instruction_id) "
                        + "REFERENCES component_instruction(id) ON DELETE CASCADE";
                stmt.executeUpdate(addForeignKeySQL12);

                String addCompVarForeignKeySQL = "ALTER TABLE component_variable "
                        + "ADD CONSTRAINT FK_CompVar FOREIGN KEY (home_banking_id) "
                        + "REFERENCES home_banking(id) ON DELETE CASCADE";
                stmt.executeUpdate(addCompVarForeignKeySQL);
            }
            System.out.println(String.format("Database %s has been created!", dbFile.getName()));
        } catch (SQLException error) {
            System.out.println("initializeDatabase\nError: " + error.getMessage());
        }
    }

    public static boolean doesInstructionTableExist() {
        try (Connection conn = getConnection()) {
            try (ResultSet rs = conn.getMetaData().getTables(null, null, "instruction", null)) {
                return rs.next(); // Returns true if the table exists
            }
        } catch (SQLException error) {
            System.out.println("Error checking table existence: " + error.getMessage());
        }
        return false; // Default return if an exception occurs or the table does not exist
    }

    public static void initializeMainDatabasePostgres() {

        try (Connection conn = getConnection()) {
            try (Statement stmt = conn.createStatement()) {

                // Create home_banking table
                String createHomeBankingTableSQL = "CREATE TABLE home_banking ("
                        + "ID SERIAL PRIMARY KEY, "
                        + "url TEXT, "
                        + "name TEXT, "
                        + "priority TEXT, "
                        + "search_config TEXT, "
                        + "options_config TEXT, "
                        + "cookies TEXT, "
                        + "driver_session TEXT, "
                        + "username TEXT, "
                        + "password TEXT)";
                stmt.executeUpdate(createHomeBankingTableSQL);

                // Create bot_job table with a foreign key reference to home_banking
                String createBotJobTableSQL = "CREATE TABLE bot_job ("
                        + "id SERIAL PRIMARY KEY, "
                        + "name TEXT UNIQUE, "
                        + "description TEXT, "
                        + "priority TEXT, "
                        + "active INTEGER NOT NULL, "
                        + "home_banking_id INTEGER REFERENCES home_banking(id) ON DELETE CASCADE)";
                stmt.executeUpdate(createBotJobTableSQL);

                // Create block table with a foreign key reference to bot_job
                String createBlockTableSQL = "CREATE TABLE block ("
                        + "id SERIAL PRIMARY KEY, "
                        + "block_order_number INTEGER NOT NULL, "
                        + "name TEXT NOT NULL, "
                        + "description TEXT, "
                        + "type_id INTEGER, "
                        + "export_file TEXT, "
                        + "active INTEGER NOT NULL, "
                        + "wait INTEGER, "
                        + "bot_job_id INTEGER REFERENCES bot_job(id) ON DELETE CASCADE)";
                stmt.executeUpdate(createBlockTableSQL);

                // Create instruction table with foreign key references to block and bot_job
                String createInstructionTableSQL = "CREATE TABLE instruction ("
                        + "id SERIAL PRIMARY KEY, "
                        + "instruction_order_number INTEGER NOT NULL, "
                        + "actions TEXT, "
                        + "name TEXT, "
                        + "xpath TEXT, "
                        + "coordinates TEXT, "
                        + "force_coordinates INTEGER, "
                        + "iframe_xpath TEXT, "
                        + "description TEXT, "
                        + "operation TEXT, "
                        + "optional INTEGER, "
                        + "block_marked INTEGER, "
                        + "default_value TEXT, "
                        + "action_custom_max_wait_sec INTEGER, "
                        + "on_hold_seconds INTEGER, "
                        + "codified INTEGER, "
                        + "export_to_abr INTEGER, "
                        + "active INTEGER NOT NULL, "
                        + "block_id INTEGER REFERENCES block(id) ON DELETE CASCADE, "
                        + "variable_id INTEGER, "
                        + "parent_id INTEGER, "
                        + "bot_job_id INTEGER REFERENCES bot_job(id) ON DELETE CASCADE)";
                stmt.executeUpdate(createInstructionTableSQL);

                String createReferenceTableSQL = "CREATE TABLE reference ("
                        + "id SERIAL PRIMARY KEY, "
                        + "reference_type TEXT, "
                        + "value TEXT, "
                        + "instruction_id INTEGER NOT NULL REFERENCES instruction(id) ON DELETE CASCADE, "
                        + "bot_job_id INTEGER REFERENCES bot_job(id) ON DELETE CASCADE)";
                stmt.executeUpdate(createReferenceTableSQL);

                String createVariableTableSQL = "CREATE TABLE variable ("
                        + "id SERIAL PRIMARY KEY, "
                        + "type TEXT, "
                        + "name TEXT, "
                        + "value TEXT, "
                        + "instruction_id INTEGER REFERENCES instruction(id) ON DELETE CASCADE, "
                        + "bot_job_id INTEGER REFERENCES bot_job(id) ON DELETE CASCADE)";
                stmt.executeUpdate(createVariableTableSQL);

                //                String createConfigurationTableSQL = "CREATE TABLE configuration ("
                //                        + "id SERIAL PRIMARY KEY, "
                //                        + "pathJava TEXT, "
                //                        + "logLevel TEXT, "
                //                        + "pathDB TEXT, "
                //                        + "interactionTimeoutSec TEXT, "
                //                        + "pathLog TEXT, "
                //                        + "defaultInstructionStopSeconds TEXT, "
                //                        + "pathReport TEXT, "
                //                        + "browser TEXT, "
                //                        + "dataBaseType TEXT, "
                //                        + "pageUpdateTimeoutSec TEXT, "
                //                        + "pathPriority TEXT, "
                //                        + "pathEngine TEXT, "
                //                        + "pathExcel TEXT, "
                //                        + "pathExport TEXT, "
                //                        + "socketPort TEXT, "
                //                        + "blockLimit TEXT, "
                //                        + "pathJavaFx TEXT)";
                //                stmt.executeUpdate(createConfigurationTableSQL);

                String createComponentBlockTableSQL = "CREATE TABLE component_block ("
                        + "id SERIAL PRIMARY KEY, "
                        + "home_banking_id INTEGER REFERENCES home_banking(id) ON DELETE CASCADE, "
                        + "block_order_number INTEGER NOT NULL, "
                        + "name TEXT NOT NULL, "
                        + "description TEXT, "
                        + "type_id INTEGER, "
                        + "export_file TEXT, "
                        + "active INTEGER, "
                        + "wait INTEGER)";
                stmt.executeUpdate(createComponentBlockTableSQL);

                String createComponentInstructionTableSQL = "CREATE TABLE component_instruction ("
                        + "id SERIAL PRIMARY KEY, "
                        + "instruction_order_number INTEGER NOT NULL, "
                        + "actions TEXT, "
                        + "name TEXT, "
                        + "xpath TEXT, "
                        + "coordinates TEXT, "
                        + "force_coordinates INTEGER, "
                        + "iframe_xpath TEXT, "
                        + "description TEXT, "
                        + "operation TEXT, "
                        + "optional INTEGER, "
                        + "block_marked INTEGER, "
                        + "default_value TEXT, "
                        + "action_custom_max_wait_sec INTEGER, "
                        + "on_hold_seconds INTEGER, "
                        + "codified INTEGER, "
                        + "export_to_abr INTEGER, "
                        + "active INTEGER NOT NULL, "
                        + "block_id INTEGER REFERENCES component_block(id) ON DELETE CASCADE, "
                        + "variable_id INTEGER, "
                        + "parent_id INTEGER, "
                        + "home_banking_id INTEGER REFERENCES home_banking(id) ON DELETE CASCADE)";
                stmt.executeUpdate(createComponentInstructionTableSQL);

                String createComponentReferenceTableSQL = "CREATE TABLE component_reference ("
                        + "id SERIAL PRIMARY KEY, "
                        + "reference_type TEXT, "
                        + "value TEXT, "
                        + "instruction_id INTEGER NOT NULL REFERENCES component_instruction(id) ON DELETE CASCADE, "
                        + "home_banking_id INTEGER REFERENCES home_banking(id) ON DELETE CASCADE)";
                stmt.executeUpdate(createComponentReferenceTableSQL);

                String createComponentVariableTableSQL = "CREATE TABLE component_variable ("
                        + "id SERIAL PRIMARY KEY, "
                        + "type TEXT, "
                        + "name TEXT, "
                        + "value TEXT, "
                        + "instruction_id INTEGER REFERENCES component_instruction(id) ON DELETE CASCADE, "
                        + "home_banking_id INTEGER REFERENCES home_banking(id) ON DELETE CASCADE)";
                stmt.executeUpdate(createComponentVariableTableSQL);
            }
            System.out.println("Database %s has been created!");
        } catch (SQLException error) {
            System.out.println("initializeDatabase\nError: " + error.getMessage());
        }
    }

    /***
     *  Second Sequence  From Components to Instructions
     * @param blockDetailsDTO
     * // component_block_|_block_|_component_instruction_|_instruction_|_component_reference_|_reference_|_component_variable_|_variable_|_component_complex_|_complex
     *
     * @return
     * @throws SQLException
     */
    public ErrorMessage injectNewComponent(BlockDetailsDTO blockDetailsDTO) {
        ARLogger.getInstance(PerformDataBase.class)
                .fine("Saving New Component Block: " + blockDetailsDTO.getBlockName());

        try (Connection conn = getConnection()) {
            String[] arrayTables = {
                "component_block", // 0
                "block", // 1
                "component_instruction", // 2
                "instruction", // 3
                "component_reference", // 4
                "reference", // 5
                "component_variable", // 6
                "variable", // 7
                "component_complex", // 8
                "complex" // 9
            };
            // Now you can proceed with duplicating the related tables
            return saveNewComponent(conn, blockDetailsDTO, true, arrayTables);

        } catch (SQLException error) {
            System.out.println(error.getMessage());
            return new ErrorMessage("Error Duplicating Variables", "Block Insertion Failure", error.getMessage());
        }
    }
}
