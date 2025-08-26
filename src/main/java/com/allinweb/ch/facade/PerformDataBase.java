package com.allinweb.ch.facade;

import com.allinweb.ch.component.model.*;
import com.allinweb.ch.persistence.DatabaseUserDTO;
import com.allinweb.ch.util.ARConstants;
import com.allinweb.ch.util.ARLogger;
import com.allinweb.ch.util.ARPropertyEnum;
import com.allinweb.ch.util.ARPropertyManager;
import com.allinweb.ch.util.ComboBoxVars;
import com.allinweb.ch.util.ErrorMessage;
import com.google.common.base.Strings;
import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.sqlite.SQLiteConfig;

public class PerformDataBase {

    // Static final variable to hold the singleton instance
    protected static volatile PerformDataBase instance;

    // Private constructor to prevent instantiation
    private PerformDataBase() {}

    // Public method to access the singleton instance
    public static PerformDataBase getInstance() {
        if (instance == null) {
            synchronized (PerformDataBase.class) {
                if (instance == null) {
                    instance = new PerformDataBase();
                }
            }
        }
        return instance;
    }

    public static final ARPropertyManager arPropertyManager;
    public static final PerformMessage performMessage;
    public static final PerformInitializer performInitializer;
    public static final PerformDBScripts performDBScripts;
    public static final PerformLists performLists;

    static {
        performLists = PerformLists.getInstance();
        performMessage = PerformMessage.getInstance();
        arPropertyManager = ARPropertyManager.getInstance();
        performInitializer = PerformInitializer.getInstance();
        performDBScripts = PerformDBScripts.getInstance();
    }

    @Getter
    @Setter
    public Connection conn = null;

    // Open connection counter
    public int openConnections = 0;

    public final String CONNECTION_TYPE = "jdbc:ucanaccess://";
    public final String CONNECTION_PARAMETERS = ";memory=false;newDatabaseVersion=V2010";
    public final String CONNECTION_TYPE_SQLITE = "jdbc:sqlite:"; // no parameters needed

    private TreeMap<Integer, Integer> homeBankMap = new TreeMap<>();
    private TreeMap<Integer, Integer> botJobMap = new TreeMap<>();
    private TreeMap<Integer, Integer> blockMap = new TreeMap<>();
    private TreeMap<Integer, Integer> instructionMap = new TreeMap<>();
    private TreeMap<Integer, Integer> instrVariablesMap = new TreeMap<>();
    private TreeMap<Integer, Integer> instrNewInverted = new TreeMap<>();
    private TreeMap<Integer, Integer> variableMap = new TreeMap<>();
    private TreeMap<Integer, Integer> referenceMap = new TreeMap<>();

    @Getter
    @Setter
    public List<Integer> idsBlockAfter = new ArrayList<>();

    @Getter
    @Setter
    public List<Integer> idsInstrucAfter = new ArrayList<>();

    @Getter
    @Setter
    public List<Integer> idsBotJobAfter = new ArrayList<>();

    @Getter
    @Setter
    public List<Integer> idsHomeUrlAfter = new ArrayList<>();

    @Getter
    @Setter
    public List<Integer> idsHomeBankAfter = new ArrayList<>();

    @Getter
    @Setter
    public List<Integer> idsVariableAfter = new ArrayList<>();

    // Postgres
    public boolean ACCESS_DB = false;
    public boolean POSTGRES_DB = false;
    public boolean SQLITE_DB = false;

    public void initialize(String databaseType) {}

    public void closeConnection() {
        if (conn != null) {
            try {
                conn.close();
                conn = null; // Reset the connection to null after closing
                decrementOpenConnections();
            } catch (SQLException e) {
                System.out.println(e.getMessage()); // Handle the exception, log it or rethrow it as needed
            }
        }
    }

    // Increment open connections counter
    public synchronized void incrementOpenConnections() {
        openConnections++;
        System.out.println("Open connections: " + openConnections);
    }

    // Decrement open connections counter
    public synchronized void decrementOpenConnections() {
        openConnections--;
        System.out.println("Open connections: " + openConnections);
    }

    // Get the current open connections count
    public int getOpenConnectionsCount() {
        return openConnections;
    }

    public void changeDbConnection() throws SQLException {
        String dataBaseType = arPropertyManager.getProperty(ARPropertyEnum.DATABASE_TYPE);
        //        if (Strings.isNullOrEmpty(previousDB) || (previousDB != null && !previousDB.equals(dataBaseType))) {
        ErrorMessage errorMessage = null;

        closeConnection();

        POSTGRES_DB = false;
        SQLITE_DB = false;
        ACCESS_DB = false;

        if (dataBaseType != null) {
            if ("Postgres".equalsIgnoreCase(dataBaseType)) {
                // Postgres-specific logic
                POSTGRES_DB = true;
            } else if ("SQLite".equalsIgnoreCase(dataBaseType)) {
                // SQLite-specific logic
                SQLITE_DB = true;
            } else if ("Access".equalsIgnoreCase(dataBaseType)) {
                // Access-specific logic
                ACCESS_DB = true;
            }
        } else {
            // Access-specific logic
            ACCESS_DB = true;
        }

        if (getConnection() != null) {
            performInitializer.initialize(getConn());
            performInitializer.initializeDBS();
        }
    }

    public Connection getConnection() throws SQLException {
        // Determine DB type from properties
        String dataBaseType = arPropertyManager.getProperty(ARPropertyEnum.DATABASE_TYPE);

        // Booleans to track DB type
        POSTGRES_DB = false;
        SQLITE_DB = false;
        ACCESS_DB = false;

        if (dataBaseType != null) {
            if (dataBaseType.equalsIgnoreCase("POSTGRES")) {
                POSTGRES_DB = true;
            } else if (dataBaseType.equalsIgnoreCase("SQLITE")) {
                SQLITE_DB = true;
            } else
                // else default to Access
                ACCESS_DB = true;
        }

        try {
            if ((conn == null || conn.isClosed()) && POSTGRES_DB) {
                // Postgres connection
                String dbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
                String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
                String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

                ARLogger.getInstance(PerformDataBase.class).info("POSTGRES connection URL: " + dbUrl);
                // ARLogger.getInstance(PerformDataBase.class).info("User Details: " + userDB + " - [PROTECTED]");

                Class.forName("org.postgresql.Driver");
                conn = DriverManager.getConnection(dbUrl, userDB, userPwd);
                conn.setReadOnly(false);

            } else if (SQLITE_DB) {
                // SQLite connection
                String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
                String sqliteUrl = CONNECTION_TYPE_SQLITE
                        + dbPath
                        + ARConstants.FILE_NAME_SQLITE; // make sure you have FILE_NAME_SQLITE constant

                ARLogger.getInstance(PerformDataBase.class).info("SQLITE connection URL: " + sqliteUrl);

                Class.forName("org.sqlite.JDBC");

                SQLiteConfig config = new SQLiteConfig();
                config.enforceForeignKeys(true);

                conn = DriverManager.getConnection(sqliteUrl, config.toProperties());
                //                    conn = SQLiteHelper.getConnection(sqliteUrl);
                conn.setReadOnly(false);

            } else if (conn == null || conn.isClosed()) {
                // Default to Access connection
                String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
                String dbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_ACCESS + CONNECTION_PARAMETERS;

                ARLogger.getInstance(PerformDataBase.class).info("ACCESS connection URL: " + dbUrl);

                Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
                conn = DriverManager.getConnection(dbUrl);
                conn.setReadOnly(false);
            }

            // Reset open connections counter if too many
            if (getOpenConnectionsCount() > 10) {
                this.openConnections = 0;
            }
            incrementOpenConnections();

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class).severe("getConnection Error: " + error.getMessage());

            String database = POSTGRES_DB ? "Postgres" : (SQLITE_DB ? "SQLite" : "Access");

            performMessage.errorMessage(
                    "Database connection Failed",
                    "<span style='color: #D32F2F; font-weight: bold; font-size: 1.1em;'>An error occurred during the Database connection.</span>",
                    "<span style='font-weight: bold;'>" + database + "</span>.",
                    "<span style='color: #E65100; font-weight: bold;'>Please ensure the Database connections are correct.</span>",
                    "<span style='font-style: italic;'>Details: " + error.getMessage() + "</span>",
                    0);

            throw error;
        } catch (ClassNotFoundException error) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe("Driver DB Class not Found Error: " + error.getMessage());
        }

        return conn;
    }

    public List<ParentOperations> loadAllParents(int bot_job_id, int instructionId) {
        List<ParentOperations> parentList = new ArrayList<>();

        String selectSQL =
                """
            SELECT
                parent.name as parent_name,
                child.actions,
                child.operation,
                child.name as child_name,
                child.id
            FROM instruction AS child
            LEFT JOIN instruction AS parent ON child.parent_id = parent.id
            WHERE child.parent_id = ?
              AND child.bot_job_id = ?
            ORDER BY child.id;
            """;

        try (PreparedStatement stmt = getConnection().prepareStatement(selectSQL)) {
            stmt.setInt(1, instructionId); // child.parent_id = ?
            stmt.setInt(2, bot_job_id); // child.bot_job_id = ?

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ParentOperations parentOper = new ParentOperations();
                    parentOper.setId(rs.getInt("id"));
                    parentOper.setName(rs.getString("child_name"));
                    parentOper.setParentName(rs.getString("parent_name"));
                    parentOper.setActions(rs.getString("actions"));
                    parentOper.setOperations(rs.getString("operation"));
                    parentOper.setInstructionId(instructionId);

                    parentList.add(parentOper);
                }
            }

            if (!parentList.isEmpty()) {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format(
                                "Loaded parents for instruction ID %d from botJobId %d", instructionId, bot_job_id));
            } else {
                ARLogger.getInstance(PerformDataBase.class)
                        .warning(String.format(
                                "No parents found for instruction ID %d in botJobId %d.", instructionId, bot_job_id));
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error loading parents for instruction ID %d from botJobId %d. Error: %s",
                            instructionId, bot_job_id, e.getMessage()));
        }

        return parentList;
    }

    public List<ParentOperations> loadParents(String tableName, int whereId, int instructionId, int parentId) {
        List<ParentOperations> parentList = new ArrayList<>();

        // Determine the foreign key column based on table
        String foreignKeyColumn = "instruction".equalsIgnoreCase(tableName) ? "bot_job_id" : "home_banking_id";

        // Use ? placeholders for PreparedStatement
        String selectSQL =
                """
            SELECT
                parent.name as parent_name,
                child.name as child_name,
                child.parent_id
            FROM %s AS child
            LEFT JOIN %s AS parent ON child.parent_id = parent.id
            WHERE child.id != ?
              AND child.parent_id = ?
              AND child.%s = ?
            ORDER BY child.id;
            """
                        .formatted(tableName, tableName, foreignKeyColumn);

        try (PreparedStatement stmt = getConnection().prepareStatement(selectSQL)) {
            stmt.setInt(1, 0); // child.id != 0
            stmt.setInt(2, parentId); // child.parent_id = ?
            stmt.setInt(3, whereId); // child.bot_job_id or home_banking_id = ?

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("child_name") + " --> (" + rs.getString("parent_id") + ")-"
                            + rs.getString("parent_name");

                    ParentOperations parentOper = new ParentOperations();
                    parentOper.setName(name);
                    parentOper.setInstructionId(instructionId);
                    parentOper.setParentId(rs.getInt("parent_id"));

                    parentList.add(parentOper);
                }
            }

            if (!parentList.isEmpty()) {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format(
                                "Loaded parents for instruction ID %d from %s = %d",
                                instructionId, foreignKeyColumn, whereId));
            } else {
                ARLogger.getInstance(PerformDataBase.class)
                        .warning(String.format(
                                "No parents found for instruction ID %d in %s = %d",
                                instructionId, foreignKeyColumn, whereId));
            }

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error loading parents for instruction ID %d in %s = %d. Error: %s",
                            instructionId, foreignKeyColumn, whereId, error.getMessage()));
        }

        return parentList;
    }

    public ErrorMessage deleteVariablesBatch(
            String tableName, // e.g., "instruction_variable" or "component_instruction_variable"
            int whereId, // e.g., bot_job_id or home_banking_id
            List<InstructionLoadDTO> dtos // contains instructionId(s) + variableId(s)
            ) {
        if (dtos == null || dtos.isEmpty()) {
            return new ErrorMessage(
                    "Invalid Input", "Variable deletion list is null or empty", "Cannot delete variables");
        }

        // Validate table name to avoid SQL injection
        List<String> allowedTables = Arrays.asList("variable", "component_variable");
        if (!allowedTables.contains(tableName)) {
            return new ErrorMessage(
                    "Invalid Table",
                    "Invalid table name: " + tableName,
                    "Only variable/component_variable are allowed");
        }

        // Determine foreign key column
        String foreignKeyColumn = "variable".equalsIgnoreCase(tableName) ? "bot_job_id" : "home_banking_id";

        final int BATCH_SIZE = 100;
        int count = 0;

        String deleteSQL =
                "DELETE FROM " + tableName + " WHERE instruction_id = ? AND id = ? AND " + foreignKeyColumn + " = ?";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {

            conn.setAutoCommit(false); // transaction control

            for (InstructionLoadDTO dto : dtos) {
                if (dto == null
                        || dto.getInstructionId() == null
                        || dto.getInstructionId() <= 0
                        || dto.getVariableId() == null
                        || dto.getVariableId() <= 0) {
                    continue; // skip invalid
                }

                pstmt.setInt(1, dto.getInstructionId());
                pstmt.setInt(2, dto.getVariableId());
                pstmt.setInt(3, whereId);
                pstmt.addBatch();

                if (++count % BATCH_SIZE == 0) {
                    pstmt.executeBatch();
                    pstmt.clearBatch();
                }
            }

            // Final flush
            if (count % BATCH_SIZE != 0) {
                pstmt.executeBatch();
                pstmt.clearBatch();
            }

            conn.commit();

            ARLogger.getInstance(PerformDataBase.class)
                    .info(String.format(
                            "Batch delete completed for %d variable records in %s", count, tableName.toUpperCase()));

            return null; // success

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe("Batch delete error for " + tableName + ": " + e.getMessage());
            return new ErrorMessage(
                    "Delete Variables Error",
                    "Failed batch deletion for variables in table: " + tableName,
                    e.getMessage());
        }
    }

    public ErrorMessage deleteInstructionsBatch(
            String tableName, // "instruction" or "component_instruction"
            int whereId, // bot_job_id or home_banking_id
            List<InstructionLoadDTO> dtos // contains list of instructionId(s)
            ) {
        if (dtos == null || dtos.isEmpty()) {
            return new ErrorMessage("Invalid Input", "Instruction list is null or empty", "Cannot delete instructions");
        }

        // Validate table name to avoid SQL injection
        List<String> allowedTables = Arrays.asList("instruction", "component_instruction");
        if (!allowedTables.contains(tableName)) {
            return new ErrorMessage(
                    "Invalid Table",
                    "Invalid table name: " + tableName,
                    "Only instruction/component_instruction are allowed");
        }

        // Determine foreign key column
        String foreignKeyColumn =
                "component_instruction".equalsIgnoreCase(tableName) ? "home_banking_id" : "bot_job_id";

        final int BATCH_SIZE = 100;
        int count = 0;

        String deleteSQL = "DELETE FROM " + tableName + " WHERE id = ? AND " + foreignKeyColumn + " = ?";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {

            conn.setAutoCommit(false); // transaction control

            for (InstructionLoadDTO dto : dtos) {
                if (dto == null || dto.getInstructionId() == null || dto.getInstructionId() <= 0) {
                    continue; // skip invalid
                }

                pstmt.setInt(1, dto.getInstructionId()); // "id" in instruction table
                pstmt.setInt(2, whereId);
                pstmt.addBatch();

                if (++count % BATCH_SIZE == 0) {
                    pstmt.executeBatch();
                    pstmt.clearBatch();
                }
            }

            // final flush
            if (count % BATCH_SIZE != 0) {
                pstmt.executeBatch();
                pstmt.clearBatch();
            }

            conn.commit();

            ARLogger.getInstance(PerformDataBase.class)
                    .info(String.format(
                            "Batch delete completed for %d instruction records in %s", count, tableName.toUpperCase()));

            return null; // success

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe("Batch delete error for " + tableName + ": " + e.getMessage());
            return new ErrorMessage(
                    "Delete Instructions Error",
                    "Failed batch deletion for instructions in table: " + tableName,
                    e.getMessage());
        }
    }

    public ErrorMessage deleteReferencesBatch(
            String tableName, // "component_reference" or "reference"
            int whereId, // bot_job_id or home_banking_id
            List<InstructionLoadDTO> dtos // contains list of instructionId(s)
            ) {
        if (dtos == null || dtos.isEmpty()) {
            return new ErrorMessage("Invalid Input", "Instruction list is null or empty", "Cannot delete references");
        }

        // Validate table name to avoid SQL injection
        List<String> allowedTables = Arrays.asList("reference", "component_reference");
        if (!allowedTables.contains(tableName)) {
            return new ErrorMessage(
                    "Invalid Table",
                    "Invalid table name: " + tableName,
                    "Only reference/component_reference are allowed");
        }

        // Determine foreign key column
        String foreignKeyColumn = "component_reference".equalsIgnoreCase(tableName) ? "home_banking_id" : "bot_job_id";

        final int BATCH_SIZE = 100;
        int count = 0;

        String deleteSQL = "DELETE FROM " + tableName + " WHERE instruction_id = ? AND " + foreignKeyColumn + " = ?";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {

            conn.setAutoCommit(false); // transaction control

            for (InstructionLoadDTO dto : dtos) {
                if (dto == null || dto.getInstructionId() == null || dto.getInstructionId() <= 0) {
                    continue; // skip invalid
                }

                pstmt.setInt(1, dto.getInstructionId());
                pstmt.setInt(2, whereId);
                pstmt.addBatch();

                if (++count % BATCH_SIZE == 0) {
                    pstmt.executeBatch();
                    pstmt.clearBatch();
                }
            }

            // final flush
            if (count % BATCH_SIZE != 0) {
                pstmt.executeBatch();
                pstmt.clearBatch();
            }

            conn.commit();

            ARLogger.getInstance(PerformDataBase.class)
                    .info(String.format(
                            "Batch delete completed for %d reference records in %s", count, tableName.toUpperCase()));

            return null; // success

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe("Batch delete error for " + tableName + ": " + e.getMessage());
            return new ErrorMessage(
                    "Delete References Error",
                    "Failed batch deletion for references in table: " + tableName,
                    e.getMessage());
        }
    }

    public boolean deleteRow(InstructionLoadDTO deleteInstructionLoadDTO) {
        // Build the SQL delete statement
        try (Statement stmt = getConnection().createStatement()) {

            int rowsAffected = 0;
            String deleteSQL = "DELETE FROM instruction WHERE id = "
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

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error deleting instruction ID %d from block ID %d. Error: %s",
                            deleteInstructionLoadDTO.getInstructionId(),
                            deleteInstructionLoadDTO.getBlockId(),
                            error.getMessage()));
        }
        return false;
    }

    public ErrorMessage deleteRowParents(
            String tableName, // "instruction" or "component_instruction"
            int whereId, // bot_job_id or home_banking_id
            int parentId) {
        // Determine foreign key column
        String foreignKeyColumn = "instruction".equalsIgnoreCase(tableName) ? "bot_job_id" : "home_banking_id";

        String deleteSQL = "DELETE FROM " + tableName + " WHERE parent_id = ? AND " + foreignKeyColumn + " = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // disable auto-commit

            try (PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {
                pstmt.setInt(1, parentId);
                pstmt.setInt(2, whereId);

                int rowsAffected = pstmt.executeUpdate();
                conn.commit();

                if (rowsAffected > 0) {
                    ARLogger.getInstance(PerformDataBase.class)
                            .info(String.format(
                                    "Deleted %d parents - parent ID %d in %s = %d",
                                    rowsAffected, parentId, foreignKeyColumn, whereId));
                } else {
                    ARLogger.getInstance(PerformDataBase.class)
                            .warning(String.format(
                                    "No parents found to delete - parent ID %d in %s = %d",
                                    parentId, foreignKeyColumn, whereId));
                }

                return null; // success
            } catch (SQLException e) {

                ARLogger.getInstance(PerformDataBase.class)
                        .severe(String.format(
                                "Error deleting parent ID %d in %s = %d. Error: %s",
                                parentId, foreignKeyColumn, whereId, e.getMessage()));

                return new ErrorMessage(
                        "Delete Parent Error", "Failed to delete parent ID: " + parentId, e.getMessage());
            }
        } catch (SQLException ex) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Connection error while deleting parent ID %d in %s = %d. Error: %s",
                            parentId, foreignKeyColumn, whereId, ex.getMessage()));

            return new ErrorMessage("Database Connection Error", "Could not connect to database", ex.getMessage());
        }
    }

    public ErrorMessage updateBlockOrderNumber(
            String tableName,
            int whereId, // either "bot_job_id" or "home_banking_id"
            boolean reorderAll) {
        final int BATCH_SIZE = 100;
        List<BlockLoadDTO> listBlocks =
                tableName.equals("block") ? performLists.getListBlock() : performLists.getListBlockComp();

        listBlocks.sort(Comparator.comparingInt(BlockLoadDTO::getBlockOrderNumber));

        String updateSQL = "UPDATE " + tableName + " SET block_order_number = ? WHERE id = ? AND " + whereId + " = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // Disable auto-commit

            try (PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
                int newOrderNumber = 1;
                int count = 0;

                for (BlockLoadDTO blockOrder : listBlocks) {
                    int orderNumber = reorderAll ? newOrderNumber : blockOrder.getBlockOrderNumber();
                    pstmt.setInt(1, orderNumber);
                    pstmt.setInt(2, blockOrder.getId());

                    // Choose the correct ID value based on the column
                    if ("block".equalsIgnoreCase(tableName)) {
                        pstmt.setInt(3, blockOrder.getBotJobId());
                    } else if ("component_block".equalsIgnoreCase(tableName)) {
                        pstmt.setInt(3, blockOrder.getHomeBankingId());
                    } else {
                        return new ErrorMessage("Invalid Table naem", "Invalid 'tableName' value", tableName);
                    }

                    pstmt.addBatch();
                    count++;
                    newOrderNumber++;

                    if (count % BATCH_SIZE == 0) {
                        pstmt.executeBatch();
                        conn.commit();
                        ARLogger.getInstance(PerformDataBase.class)
                                .info("Executed batch of " + BATCH_SIZE + " block order updates for table: "
                                        + tableName);
                    }
                }

                // Execute remaining updates
                if (count % BATCH_SIZE != 0) {
                    pstmt.executeBatch();
                    conn.commit();
                    ARLogger.getInstance(PerformDataBase.class)
                            .info("Executed final batch of " + (count % BATCH_SIZE) + " block order updates for table: "
                                    + tableName);
                }

                //                loadBlocks(whereId, "", tableName);
                return null; // Success
            } catch (SQLException e) {

                ARLogger.getInstance(PerformDataBase.class)
                        .severe(String.format(
                                "Error updating block order numbers in table '%s'. Error: %s",
                                tableName, e.getMessage()));
                return new ErrorMessage(
                        "Update Block Order Error", "Failed to update block order numbers", e.getMessage());
            }
        } catch (SQLException ex) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Connection error while updating block order numbers in table '%s'. Error: %s",
                            tableName, ex.getMessage()));
            return new ErrorMessage("Database Connection Error", "Could not connect to database", ex.getMessage());
        }
    }

    public ErrorMessage updateBlockOrderNumber(
            String tableName,
            int whereId, // either "bot_job_id" or "home_banking_id"
            List<BlockLoadDTO> listBlocks) {

        final int BATCH_SIZE = 100;

        String updateSQL = "UPDATE " + tableName
                + " SET block_order_number = ? WHERE id = ? AND "
                + (tableName.equalsIgnoreCase("block") ? "bot_job_id" : "home_banking_id")
                + " = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // Disable auto-commit

            try (PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
                int count = 0;

                for (BlockLoadDTO blockOrder : listBlocks) {
                    pstmt.setInt(1, blockOrder.getBlockOrderNumber());
                    pstmt.setInt(2, blockOrder.getId());

                    // Choose correct id based on table type
                    if ("block".equalsIgnoreCase(tableName)) {
                        pstmt.setInt(3, blockOrder.getBotJobId());
                    } else if ("component_block".equalsIgnoreCase(tableName)) {
                        pstmt.setInt(3, blockOrder.getHomeBankingId());
                    } else {
                        return new ErrorMessage("Invalid Table Name", "Invalid 'tableName' value", tableName);
                    }

                    pstmt.addBatch();
                    count++;

                    if (count % BATCH_SIZE == 0) {
                        pstmt.executeBatch();
                        conn.commit();
                        ARLogger.getInstance(PerformDataBase.class)
                                .info("Executed batch of " + BATCH_SIZE + " block order updates for table: "
                                        + tableName);
                    }
                }

                // Execute remaining updates
                if (count % BATCH_SIZE != 0) {
                    pstmt.executeBatch();
                    conn.commit();
                    ARLogger.getInstance(PerformDataBase.class)
                            .info("Executed final batch of " + (count % BATCH_SIZE) + " block order updates for table: "
                                    + tableName);
                }

                // ✅ reload blocks if necessary (optional — remove if list is always passed in externally)
                loadBlocks(whereId, "", tableName);
                return null; // Success
            } catch (SQLException e) {
                conn.rollback();
                ARLogger.getInstance(PerformDataBase.class)
                        .severe(String.format(
                                "Error updating block order numbers in table '%s'. Error: %s",
                                tableName, e.getMessage()));
                return new ErrorMessage(
                        "Update Block Order Error", "Failed to update block order numbers", e.getMessage());
            }
        } catch (SQLException ex) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Connection error while updating block order numbers in table '%s'. Error: %s",
                            tableName, ex.getMessage()));
            return new ErrorMessage("Database Connection Error", "Could not connect to database", ex.getMessage());
        }
    }

    public List<BlockOrderDetailDTO> selectAllBlocks(int botJobId) {
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

    public List<BlockOrderDetailDTO> selectCompAllBlocks(int homeBankId, int blockId) {
        List<BlockOrderDetailDTO> blockOrderDetails = new ArrayList<>();
        try (Statement stmt = getConnection().createStatement()) {

            // Select blocks based on botJobId, ordered by block_order_number ASC
            String selectSQL = "SELECT id FROM component_block WHERE home_banking_id = " + homeBankId
                    + " and block_id = " + blockId + " ORDER BY block_order_number ASC";
            ResultSet rs = stmt.executeQuery(selectSQL);

            int newOrderNumber = 1;
            // Iterate through the result set and build BlockOrderDetailDTO list
            while (rs.next()) {
                blockId = rs.getInt("id");

                // Create a BlockOrderDetailDTO object with blockId and the new order number
                BlockOrderDetailDTO blockDetail = BlockOrderDetailDTO.builder()
                        .homeBankId(homeBankId)
                        .blockId(blockId)
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
                            "Error selecting blocks for homeBankId %d and bockId %d. Error: %s",
                            homeBankId, blockId, e.getMessage()));
        }
        return blockOrderDetails;
    }

    public List<BlockOrderDetailDTO> selectCompAllBlocks(int botJobId) {
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

    // Generic Update Block Name
    public ErrorMessage updateBlockName(
            int whereId,
            String tableName, // "block" or "component_block"
            int blockId,
            String blockName) {

        // Determine foreign key column
        String foreignKeyColumn = "block".equalsIgnoreCase(tableName) ? "bot_job_id" : "home_banking_id";

        String updateSQL = "UPDATE " + tableName + " SET name = ?" + " WHERE id = ? AND " + foreignKeyColumn + " = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // disable auto-commit

            try (PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
                pstmt.setString(1, blockName);
                pstmt.setInt(2, blockId);
                pstmt.setInt(3, whereId);

                int rowsAffected = pstmt.executeUpdate();
                conn.commit();

                if (rowsAffected > 0) {
                    ARLogger.getInstance(PerformDataBase.class)
                            .info(String.format(
                                    "Updated block name in table %s - blockId %d, whereId %d, name: %s",
                                    tableName, blockId, whereId, blockName));
                    return null; // success
                } else {
                    ARLogger.getInstance(PerformDataBase.class)
                            .warning(String.format(
                                    "No record found to update in %s - blockId %d, whereId %d",
                                    tableName, blockId, whereId));
                    return new ErrorMessage(
                            "Update Block Name",
                            "No matching record found",
                            String.format("table: %s, blockId: %d, whereId: %d", tableName, blockId, whereId));
                }
            } catch (SQLException e) {
                ARLogger.getInstance(PerformDataBase.class)
                        .severe(String.format(
                                "Error updating block name in %s - blockId %d, whereId %d. Error: %s",
                                tableName, blockId, whereId, e.getMessage()));
                return new ErrorMessage("Update Block Name", "Failed to update block name", e.getMessage());
            }
        } catch (SQLException ex) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Connection error while updating block name in %s - blockId %d, whereId %d. Error: %s",
                            tableName, blockId, whereId, ex.getMessage()));
            return new ErrorMessage("Database Connection Error", "Could not connect to database", ex.getMessage());
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

    //    // Handle DELETE_BLOCK message
    //    public boolean deleteBlock(String tableName, int whereId, int blockId, int homeBankId) {
    //        boolean blockDeletion = false;
    //
    //        String extraTable = tableName.equals("block") ? "instruction" : "component_instruction";
    //        List<InstructionLoadDTO> deleteList = getInstructionsList(whereId, blockId, -1, extraTable);
    //
    //        if (deleteList.size() > 0) {
    //            for (InstructionLoadDTO deleteDTO : deleteList) {
    //                deleteInstruction(tableName, whereId, homeBankId, blockId, true);
    //                //                updateOtherBlocks()
    //            }
    //        }
    //        ErrorMessage errorMessage = deleteBlockDirect(tableName, botJobId, blockId);
    //        if (errorMessage == null) {
    //            blockDeletion = true;
    //            deleteNullBlocks(botJobId);
    //            if (deleteBlockDTO.getUpdatedBlocks() != null
    //                    && deleteBlockDTO.getUpdatedBlocks().size() > 0) {
    //
    //                loadBlocks(botJobId, "", tableName);
    //                updateBlockOrderNumber(tableName, botJobId, performLists.getListBlock(), true);
    //            }
    //        }
    //
    //        return blockDeletion;
    //    }

    //    // Handle DELETE_BLOCK message
    //    public boolean deleteCompBlock(DeleteBlockDTO deleteBlockDTO) {
    //        boolean blockDeletion = false;
    //        String tableName = "instruction";
    //        if (deleteBlockDTO.getSessionId().equals("componentTasks")) {
    //            tableName = "component_instruction";
    //        }
    //        List<InstructionLoadDTO> instructionsList =
    //                getInstructionsList(deleteBlockDTO.getHomeBankingId(), deleteBlockDTO.getBlockId(), -1,
    // tableName);
    //        if (instructionsList.size() > 0) {
    //            for (InstructionLoadDTO deleteDTO : instructionsList) {
    //                deleteDTO.setHomeBankingId(deleteBlockDTO.getHomeBankingId());
    //                deleteComponent(deleteBlockDTO.getHomeBankingId(), deleteBlockDTO.getBlockId(), deleteDTO, true);
    //                //                updateOtherBlocks()
    //            }
    //        }
    //        return blockDeletion;
    //    }

    public ErrorMessage initiateNewBlock(BlockDetailsDTO blockDTO, int botJobId, boolean splitted) {
        String selectIdsSQL = "SELECT id FROM block ORDER BY id";
        String insertSQL =
                "INSERT INTO block (block_order_number, description, name, type_id, active, wait, bot_job_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
                Statement idStmtBefore = conn.createStatement();
                Statement idStmtAfter = conn.createStatement();
                PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {

            // Step 1: Get all block IDs before insertion
            List<Integer> idsBefore = new ArrayList<>();
            try (ResultSet rsBefore = idStmtBefore.executeQuery(selectIdsSQL)) {
                while (rsBefore.next()) {
                    idsBefore.add(rsBefore.getInt("id"));
                }
            }

            // Step 2: Set parameters and insert new block
            if (splitted) {
                insertStmt.setInt(1, blockDTO.getBlockOrderNumber());
            } else {
                insertStmt.setInt(1, 1);
            }
            insertStmt.setString(2, blockDTO.getBlockName() + " description");
            insertStmt.setString(3, blockDTO.getBlockName());
            insertStmt.setInt(4, 1); // type_id
            insertStmt.setInt(5, blockDTO.getActive() ? 1 : 0);
            insertStmt.setInt(6, 3); // wait
            insertStmt.setInt(7, botJobId);

            int rowsInserted = insertStmt.executeUpdate();

            // Step 3: Get all block IDs after insertion
            idsBlockAfter.clear();
            try (ResultSet rsAfter = idStmtAfter.executeQuery(selectIdsSQL)) {
                while (rsAfter.next()) {
                    idsBlockAfter.add(rsAfter.getInt("id"));
                }
            }

            // Step 4: Determine the new ID
            idsBlockAfter.removeAll(idsBefore);
            if (idsBlockAfter.size() == 1) {
                int newId = idsBlockAfter.get(0);
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format("Block data saved successfully.\nBlockId: %d", newId));
            } else {
                ARLogger.getInstance(PerformDataBase.class)
                        .warning("Block inserted, but new ID could not be uniquely identified.");
            }

            return null;

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("Error Initiate New Block: %s", error.getMessage()));
            return new ErrorMessage("Error Initiate New Block", "Cannot create a new block", error.getMessage());
        }
    }

    // CREATE NEW BOT JOB
    public Integer getNewBotJobId() {
        if (idsBotJobAfter != null && idsBotJobAfter.size() == 1) {
            return idsBotJobAfter.get(0); // return the only new ID
        }
        return -1; // invalid or multiple IDs
    }

    public ErrorMessage createNewBotJob(BotJobLoadDTO createdBotJob) {
        String tableName = "bot_job";
        String insertSQL =
                "INSERT INTO bot_job (name, description, home_banking_id, home_url_id, active) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
                Statement idStmtBefore = conn.createStatement();
                Statement idStmtAfter = conn.createStatement();
                PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            // Step 1: Get IDs before insertion
            List<Integer> idsBefore = new ArrayList<>();
            try (ResultSet rsBefore = idStmtBefore.executeQuery("SELECT id FROM " + tableName + " ORDER BY id")) {
                while (rsBefore.next()) {
                    idsBefore.add(rsBefore.getInt("id"));
                }
            }

            // Step 2: Insert new bot job
            pstmt.setString(1, createdBotJob.getName());
            pstmt.setString(2, createdBotJob.getName() + " description");
            pstmt.setInt(3, createdBotJob.getHomeBankingId());
            pstmt.setInt(4, createdBotJob.getHomeUrlId());
            pstmt.setInt(5, 1); // active = true

            pstmt.executeUpdate();

            // Step 3: Get IDs after insertion
            idsBotJobAfter.clear();
            try (ResultSet rsAfter = idStmtAfter.executeQuery("SELECT id FROM " + tableName + " ORDER BY id")) {
                while (rsAfter.next()) {
                    idsBotJobAfter.add(rsAfter.getInt("id"));
                }
            }

            // Step 4: Keep only the new IDs
            idsBotJobAfter.removeAll(idsBefore);

            ARLogger.getInstance(PerformDataBase.class)
                    .info(String.format("BotJob inserted successfully. New IDs: %s", idsBotJobAfter));

            return null; // null means no error

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("createNewBotJob - Error: %s", error.getMessage()));

            return new ErrorMessage("Bot Job Insertion Error", "Error inserting a new bot job.", error.getMessage());
        }
    }

    public ErrorMessage updateInstructionsSplitter(
            List<InstructionLoadDTO> instructions, int oldBlockId, int newBlockId) {
        final int BATCH_SIZE = 100;
        String updateSQL =
                "UPDATE instruction SET instruction_order_number = ?, block_id = ? WHERE id = ? AND block_id = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // Disable auto-commit

            try (PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
                int count = 0;

                for (InstructionLoadDTO instruction : instructions) {
                    pstmt.setInt(1, instruction.getInstructionOrderNumber());
                    pstmt.setInt(2, newBlockId);
                    pstmt.setInt(3, instruction.getInstructionId());
                    pstmt.setInt(4, oldBlockId);
                    pstmt.addBatch();
                    count++;

                    if (count % BATCH_SIZE == 0) {
                        int[] rowsAffected = pstmt.executeBatch();
                        conn.commit();
                        ARLogger.getInstance(PerformDataBase.class)
                                .info("Executed batch of " + BATCH_SIZE + " updates for oldBlockId " + oldBlockId
                                        + " -> newBlockId " + newBlockId);
                    }
                }

                // Execute any remaining batch
                if (count % BATCH_SIZE != 0) {
                    int[] rowsAffected = pstmt.executeBatch();
                    conn.commit();
                    ARLogger.getInstance(PerformDataBase.class)
                            .info("Executed final batch of " + (count % BATCH_SIZE) + " updates for oldBlockId "
                                    + oldBlockId + " -> newBlockId " + newBlockId);
                }

                return null; // Success
            } catch (SQLException e) {
                conn.rollback(); // rollback changes on error
                ARLogger.getInstance(PerformDataBase.class)
                        .severe(String.format(
                                "updateInstructionsSplitter - Error updating instructions from blockId %d to %d. Error: %s",
                                oldBlockId, newBlockId, e.getMessage()));
                return new ErrorMessage("Update Error", "Failed to update instructions", e.getMessage());
            }
        } catch (SQLException ex) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Connection error while updating instructions from blockId %d to %d. Error: %s",
                            oldBlockId, newBlockId, ex.getMessage()));
            return new ErrorMessage("Database Connection Error", "Could not connect to database", ex.getMessage());
        }
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

    public boolean rowsGetUpdateName(List<ParentOperations> listParents) {
        // Build the SQL update statement
        try (Statement stmt = getConnection().createStatement()) {
            for (ParentOperations parent : listParents) {

                if ("GET".equals(parent.getActions())) {

                    String updateSQL = "UPDATE instruction SET  "
                            + " operation = '" + parent.getOperations() + "' "
                            + " WHERE id = " + parent.getId()
                            + " and parent_id = " + parent.getInstructionId();

                    int rowsAffected = stmt.executeUpdate(updateSQL);
                    if (rowsAffected > 0) {
                        ARLogger.getInstance(PerformDataBase.class)
                                .warning(String.format(
                                        "RowsUpdateName - InstructionId: %s now have name: %s",
                                        parent.getInstructionId(), parent.getName()));
                    } else {
                        ARLogger.getInstance(PerformDataBase.class)
                                .warning(String.format(
                                        "UpdateMoveRowsOrder - No matching record found to update InstructionId: %d and name: %s",
                                        parent.getInstructionId(), parent.getName()));
                    }
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

    public ErrorMessage updateMoveRowsOrder(
            String tableName,
            int whereId, // either bot_job_id or home_banking_id
            List<InstructionLoadDTO> instructions) {
        if (instructions == null || instructions.isEmpty()) {
            return null; // nothing to do
        }

        String idColumn = "id";
        String blockIdColumn = "block_id";
        String orderColumn = "instruction_order_number";
        String whereColumn = tableName.equals("block") ? "bot_job_id" : "home_banking_id";

        String instructionTable = tableName.equals("block") ? "instruction" : "component_instruction";
        String updateSQL = "UPDATE " + instructionTable + " SET "
                + orderColumn + " = ?, "
                + blockIdColumn + " = ? "
                + "WHERE " + idColumn + " = ? AND " + whereColumn + " = ?";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {

            conn.setAutoCommit(false);

            for (InstructionLoadDTO instruction : instructions) {
                pstmt.setInt(1, instruction.getInstructionOrderNumber());
                pstmt.setInt(2, instruction.getBlockId());
                pstmt.setInt(3, instruction.getInstructionId());
                pstmt.setInt(4, whereId);
                pstmt.addBatch();
            }

            pstmt.executeBatch();
            conn.commit();

            return null; // success
        } catch (SQLException e) {
            return new ErrorMessage(
                    "Update Move Rows Order Error", "Failed to update instruction order numbers", e.getMessage());
        }
    }

    public ErrorMessage rollBackBlocksRows(String targetTable, RollBackBlocksDTO rollBackBlocksDTO) {
        final int BATCH_SIZE = 100; // Batch size for executeBatch()
        String updateSQL = "UPDATE " + targetTable + " SET instruction_order_number = ?, block_id = ? WHERE id = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // Disable auto-commit

            try (PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
                int count = 0;

                for (InstructionLoadDTO instruction : rollBackBlocksDTO.getInstructions()) {
                    pstmt.setInt(1, instruction.getInstructionOrderNumber());
                    pstmt.setInt(2, rollBackBlocksDTO.getBlockId());
                    pstmt.setInt(3, instruction.getInstructionId());
                    pstmt.addBatch();
                    count++;

                    if (count % BATCH_SIZE == 0) {
                        int[] rowsAffected = pstmt.executeBatch();
                        conn.commit();
                        ARLogger.getInstance(PerformDataBase.class)
                                .info("Executed batch of " + BATCH_SIZE + " updates for blockId "
                                        + rollBackBlocksDTO.getBlockId());
                    }
                }

                // Execute any remaining batch
                if (count % BATCH_SIZE != 0) {
                    int[] rowsAffected = pstmt.executeBatch();
                    conn.commit();
                    ARLogger.getInstance(PerformDataBase.class)
                            .info("Executed final batch of " + (count % BATCH_SIZE) + " updates for blockId "
                                    + rollBackBlocksDTO.getBlockId());
                }

                return null; // Success
            } catch (SQLException e) {

                ARLogger.getInstance(PerformDataBase.class)
                        .severe(String.format(
                                "RollBackBlocks - Error updating BlockId %d. Error: %s",
                                rollBackBlocksDTO.getBlockId(), e.getMessage()));
                return new ErrorMessage("RollBack Error", "Failed to roll back instructions", e.getMessage());
            }
        } catch (SQLException ex) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Connection error while rolling back BlockId %d. Error: %s",
                            rollBackBlocksDTO.getBlockId(), ex.getMessage()));
            return new ErrorMessage("Database Connection Error", "Could not connect to database", ex.getMessage());
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

    public List<InstructionLoadDTO> getBlockLoopInstructionIdsWithNullBlock(int botJobId) {
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

    public ErrorMessage deleteBlockDirect(
            String tableName,
            int whereId, // bot_job_id or home_banking_id
            int blockId) {
        // Determine the correct column based on the table
        String whereColumn;
        if ("block".equalsIgnoreCase(tableName)) {
            whereColumn = "bot_job_id";
        } else if ("component_block".equalsIgnoreCase(tableName)) {
            whereColumn = "home_banking_id";
        } else {
            return new ErrorMessage("Invalid Table", "Unknown table: " + tableName, null);
        }

        ErrorMessage errorMessage;
        String instructionTable = tableName.equals("block") ? "instruction" : "component_instruction";
        loadInstructions(whereId, blockId, -1, instructionTable);
        List<InstructionLoadDTO> lstInstruc = instructionTable.equals("instruction")
                ? performLists.getListInstruction()
                : performLists.getListInstructionComp();

        if (!lstInstruc.isEmpty()) {
            String referenceTable = tableName.equals("block") ? "reference" : "component_reference";
            errorMessage = deleteReferencesBatch(referenceTable, whereId, lstInstruc);

            if (errorMessage == null) {
                errorMessage = deleteInstructionsBatch(instructionTable, whereId, lstInstruc);
            }

            if (errorMessage != null) {
                ARLogger.getInstance(PerformDataBase.class)
                        .severe("Error: " + errorMessage.getErrorTitle() + "-" + errorMessage.getErrorMessage());
            }
        }

        String deleteSQL = "DELETE FROM " + tableName + " WHERE id = ? AND " + whereColumn + " = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // disable auto-commit

            try (PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {
                pstmt.setInt(1, blockId);
                pstmt.setInt(2, whereId);

                int rowsAffected = pstmt.executeUpdate();
                conn.commit();

                if (rowsAffected > 0) {
                    ARLogger.getInstance(PerformDataBase.class)
                            .info(String.format(
                                    "The Block id %d has been successfully deleted from %s = %d.",
                                    blockId, whereColumn, whereId));
                } else {
                    ARLogger.getInstance(PerformDataBase.class)
                            .warning(String.format(
                                    "No matching record found for blockId %d in %s = %d.",
                                    blockId, whereColumn, whereId));
                }

                return null; // success
            } catch (SQLException e) {

                ARLogger.getInstance(PerformDataBase.class)
                        .severe(String.format(
                                "Error deleting blockId %d from %s = %d. Error: %s",
                                blockId, whereColumn, whereId, e.getMessage()));
                return new ErrorMessage("Delete Block Error", "Failed to delete block", e.getMessage());
            }
        } catch (SQLException ex) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Connection error while deleting blockId %d from %s = %d. Error: %s",
                            blockId, whereColumn, whereId, ex.getMessage()));
            return new ErrorMessage("Database Connection Error", "Could not connect to database", ex.getMessage());
        }
    }

    public List<BotJobLoadDTO> loadCompleteJobs(int botJobId) {
        String query = "SELECT bot.home_banking_id, bot.home_url_id, bot.id AS bot_job_id, bot.name AS bot_job_name, "
                + " b.id AS block_id, b.block_order_number, b.name AS block_name, "
                + " b.description AS block_description, b.type_id, "
                + " bli.id AS instruction_id, bli.instruction_order_number, "
                + " bli.actions, bli.name AS instruction_name, bli.xpath, bli.coordinates,  bli.iframe_xpath, "
                + " bli.tag_name, bli.shadow_host, bli.shadow_root, bli.css_selector, "
                + " bli.description AS instruction_description, bli.force_coordinates, "
                + " bli.optional, bli.block_marked, bli.default_value, bli.action_custom_max_wait_sec, "
                + " bli.on_hold_seconds, bli.codified, bli.export_to_abr, "
                + " irl.reference_type, irl.value, "
                + "  bli.operation, bli.parent_id, "
                + "  b.export_file, "
                + "  b.active as block_active, b.wait, "
                + "  bli.active as instruction_active, "
                + "  bli.variable_id, "
                + "  bli.parent_block_id "
                + " FROM bot_job bot "
                + " LEFT JOIN block b ON b.bot_job_id = bot.id "
                + " JOIN instruction bli ON bli.block_id = b.id "
                + " LEFT JOIN reference irl ON irl.instruction_id = bli.id "
                + " where bot.active = 1 and bot.id = " + botJobId
                + "  ORDER BY bot.id, b.block_order_number, bli.instruction_order_number, irl.id ASC";

        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            Map<Integer, BotJobLoadDTO> botJobMapDTO = new HashMap<>();
            Map<Integer, BlockLoadDTO> blockMapDTO = new HashMap<>();
            Map<Integer, InstructionLoadDTO> instructionMapDTO = new HashMap<>();

            performLists.getListBotJob().clear();

            while (rs.next()) {
                botJobId = rs.getInt("bot_job_id");
                BotJobLoadDTO botJobDTO = botJobMapDTO.get(botJobId);

                if (botJobDTO == null) {
                    botJobDTO = new BotJobLoadDTO();
                    botJobDTO.setId(botJobId);
                    botJobDTO.setHomeBankingId(rs.getInt("home_banking_id"));
                    botJobDTO.setHomeUrlId(rs.getInt("home_url_id"));
                    botJobDTO.setName(rs.getString("bot_job_name"));
                    botJobDTO.setBlockLoadDTOList(new ArrayList<>());
                    botJobMapDTO.put(botJobId, botJobDTO);
                    performLists.getListBotJob().add(botJobDTO);
                }

                int blockId = rs.getInt("block_id");
                BlockLoadDTO blockDTO = blockMapDTO.get(blockId);

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
                    blockMapDTO.put(blockId, blockDTO);
                }

                int instructionId = rs.getInt("instruction_id");
                InstructionLoadDTO instruction = instructionMapDTO.get(instructionId);

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

                    instruction.setTagName(rs.getString("tag_name"));
                    instruction.setShadowHost(rs.getString("shadow_host"));
                    instruction.setShadowRoot(rs.getString("shadow_root"));
                    instruction.setCssSelector(rs.getString("css_selector"));

                    instruction.setDescription(rs.getString("instruction_description"));
                    instruction.setOptional(rs.getBoolean("optional"));
                    instruction.setBlockMarked(rs.getBoolean("block_marked"));
                    instruction.setDefaultValue(rs.getString("default_value"));
                    instruction.setActionCustomMaxWaitSec(rs.getInt("action_custom_max_wait_sec"));
                    instruction.setOnHoldSeconds(rs.getInt("on_hold_seconds"));
                    instruction.setCodified(rs.getBoolean("codified"));
                    instruction.setExportToABR(rs.getBoolean("export_to_abr"));
                    instruction.setOperation(rs.getString("operation"));
                    instruction.setParentBlockId(rs.getInt("parent_block_id"));
                    instruction.setParentId(rs.getInt("parent_id"));
                    instruction.setVariableId(rs.getInt("variable_id"));

                    instruction.setInstructionActive(rs.getBoolean("instruction_active"));

                    instruction.setReferenceLoadDTOList(new ArrayList<>());
                    blockDTO.getInstructionLoadDTOS().add(instruction);
                    instructionMapDTO.put(instructionId, instruction);
                }

                String referenceType = rs.getString("reference_type");
                if (referenceType != null) {
                    ReferenceLoadDTO reference = new ReferenceLoadDTO();
                    reference.setReferenceType(referenceType);
                    reference.setValue(rs.getString("value"));
                    instruction.getReferenceLoadDTOList().add(reference);
                }
            }
        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error loadCompleteJobs for Bot Job Id %d. Error: %s", botJobId, e.getMessage()));
            performLists.getListBotJob().clear();
        }

        return performLists.getListBotJob();
    }

    public List<BotJobLoadDTO> loadComponentsComplete(int homeBankingId, int botJobIdDest, String botJobNameDest) {
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
                + "    bli.tag_name, \n"
                + "    bli.shadow_host, \n"
                + "    bli.shadow_root, \n"
                + "    bli.css_selector, \n"
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
                + "    irl.value AS reference_value, \n"
                + "    bli.variable_id, \n"
                + "    bli.parent_block_id \n"
                + "FROM home_banking hb\n"
                + "LEFT JOIN component_block blk ON blk.home_banking_id = hb.id\n"
                + "JOIN component_instruction bli ON bli.block_id = blk.id\n"
                + "LEFT JOIN component_reference irl ON irl.instruction_id = bli.id\n"
                + "WHERE hb.id = "
                + homeBankingId + "\n"
                + "ORDER BY hb.id, blk.block_order_number, bli.instruction_order_number, bli.id ASC;";

        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            Map<Integer, BotJobLoadDTO> botJobMapDTO = new HashMap<>();
            Map<Integer, BlockLoadDTO> blockMapDTO = new HashMap<>();
            Map<Integer, InstructionLoadDTO> instructionMapDTO = new HashMap<>();

            performLists.getListBotJobComp().clear();

            while (rs.next()) {
                //                int botJobId = rs.getInt("bot_job_id");
                BotJobLoadDTO botJobDTO = botJobMapDTO.get(botJobIdDest);

                if (botJobDTO == null) {
                    botJobDTO = new BotJobLoadDTO();
                    botJobDTO.setId(botJobIdDest);
                    botJobDTO.setHomeBankingId(rs.getInt("home_banking_id"));
                    botJobDTO.setName(botJobNameDest); // rs.getString("bot_job_name"));
                    botJobDTO.setBlockLoadDTOList(new ArrayList<>());
                    botJobMapDTO.put(botJobIdDest, botJobDTO);
                    performLists.getListBotJobComp().add(botJobDTO);
                }

                int blockId = rs.getInt("block_id");
                BlockLoadDTO blockDTO = blockMapDTO.get(blockId);

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
                    blockMapDTO.put(blockId, blockDTO);
                }

                int instructionId = rs.getInt("instruction_id");
                InstructionLoadDTO instruction = instructionMapDTO.get(instructionId);

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

                    instruction.setTagName(rs.getString("tag_name"));
                    instruction.setShadowHost(rs.getString("shadow_host"));
                    instruction.setShadowRoot(rs.getString("shadow_root"));
                    instruction.setCssSelector(rs.getString("css_selector"));

                    instruction.setDescription(rs.getString("instruction_description"));
                    instruction.setOptional(rs.getBoolean("optional"));
                    instruction.setBlockMarked(rs.getBoolean("block_marked"));
                    instruction.setDefaultValue(rs.getString("default_value"));
                    instruction.setActionCustomMaxWaitSec(rs.getInt("action_custom_max_wait_sec"));
                    instruction.setOnHoldSeconds(rs.getInt("on_hold_seconds"));
                    instruction.setCodified(rs.getBoolean("codified"));
                    instruction.setExportToABR(rs.getBoolean("export_to_abr"));
                    instruction.setOperation(rs.getString("operation"));
                    instruction.setParentBlockId(rs.getInt("parent_block_id"));
                    instruction.setParentId(rs.getInt("parent_id"));
                    instruction.setVariableId(rs.getInt("variable_id"));

                    instruction.setInstructionActive(rs.getBoolean("instruction_active"));

                    instruction.setReferenceLoadDTOList(new ArrayList<>());
                    blockDTO.getInstructionLoadDTOS().add(instruction);
                    instructionMapDTO.put(instructionId, instruction);
                }

                String referenceType = rs.getString("reference_type");
                if (referenceType != null) {
                    ReferenceLoadDTO reference = new ReferenceLoadDTO();
                    reference.setReferenceType(referenceType);
                    reference.setValue(rs.getString("reference_value"));
                    instruction.getReferenceLoadDTOList().add(reference);
                }
            }
        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error loadComponentsComplete for Home Bank %d. Error: %s",
                            homeBankingId, error.getMessage()));
            performLists.getListBotJobComp().clear();
        }

        return performLists.getListBotJobComp();
    }

    public boolean reorderInstructions(List<InstructionLoadDTO> rowList, String tableName, boolean explicity) {
        final int BATCH_SIZE = 100;
        int orderNumber = 1;
        int count = 0;

        String updateSQL =
                String.format("UPDATE %s SET instruction_order_number = ? WHERE id = ? AND block_id = ?", tableName);

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {

            conn.setAutoCommit(false); // Start transaction

            for (InstructionLoadDTO instruction : rowList) {
                if (!explicity) {
                    instruction.setInstructionOrderNumber(orderNumber);
                }

                Integer instrId = instruction.getInstructionId();
                Integer blockId = instruction.getBlockId();

                if (instrId == null || blockId == null) {
                    ARLogger.getInstance(PerformDataBase.class)
                            .warning("Skipping reorder: instructionId or blockId is null.");
                    continue;
                }

                pstmt.setInt(1, instruction.getInstructionOrderNumber());
                pstmt.setInt(2, instrId);
                pstmt.setInt(3, blockId);

                pstmt.addBatch();
                orderNumber++;

                if (++count % BATCH_SIZE == 0) {
                    pstmt.executeBatch();
                    pstmt.clearBatch();
                }
            }

            // Execute remaining batch
            if (count % BATCH_SIZE != 0) {
                pstmt.executeBatch();
            }

            conn.commit(); // Commit transaction
            return true;

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe("Error batch updating instruction order numbers: " + e.getMessage());
            return false;
        }
    }

    public ErrorMessage deleteBotJobData(int botJobId) {
        String deleteSQL = "DELETE FROM bot_job WHERE id = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(deleteSQL)) {
                ps.setInt(1, botJobId);
                ps.executeUpdate();
                conn.commit();
                return null; // success
            } catch (SQLException error) {
                return new ErrorMessage("Error deleting Bot Job", "I cannot delete the BotJob Now", error.getMessage());
            }

        } catch (SQLException error) {
            return new ErrorMessage("Error deleting Bot Job", "I cannot delete the BotJob Now", error.getMessage());
        }
    }

    public ErrorMessage updateBotJobDetails(int botJobId, int homeUrlId, String name, String description) {
        String updateSQL = "UPDATE bot_job SET name = ?, description = ?, home_url_id = ? WHERE id = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // Disable auto-commit

            try (PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
                pstmt.setString(1, name);
                pstmt.setString(2, description);
                pstmt.setInt(3, homeUrlId);
                pstmt.setInt(4, botJobId);

                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    ARLogger.getInstance(PerformDataBase.class)
                            .info(String.format("Bot Job id %d successfully updated!", botJobId));
                } else {
                    conn.commit(); // Commit even though nothing changed
                    return new ErrorMessage(
                            "No matching record found for Bot Job",
                            "Update Warning",
                            "No Bot Job with ID '" + botJobId + "' exists.");
                }

                conn.commit(); // Commit transaction
                return null; // Success, no error
            } catch (SQLException e) {
                ARLogger.getInstance(PerformDataBase.class)
                        .severe(String.format("Error updating BotJobId %d. Error: %s", botJobId, e.getMessage()));
                return new ErrorMessage("Bot Job Update Error", "Error updating Bot Job", e.getMessage());
            }
        } catch (SQLException ex) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Connection error while updating BotJobId %d. Error: %s", botJobId, ex.getMessage()));
            return new ErrorMessage("Database Connection Error", "Could not connect to database", ex.getMessage());
        }
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

    public ErrorMessage loadQuickBotJobs() {
        performLists.getQuickBotJobs().clear();

        String query =
                """
                SELECT bot.id AS bot_job_id, bot.name AS bot_job_name,
                       bot.description AS bot_job_description, bot.priority AS bot_job_priority,
                       bot.home_banking_id, bot.home_url_id,
                       hu.url AS home_banking_url,
                       hb.name AS home_banking_name,
                       hb.priority AS home_banking_priority, hb.search_config,
                       hb.options_config, hb.cookies, hb.driver_session,
                       hb.username, hb.password,
                       bot.active,
                       b.id AS block_id, b.block_order_number, b.name AS block_name,
                       b.description AS block_description, b.type_id, b.active AS block_active, b.wait
                FROM bot_job bot
                LEFT JOIN home_banking hb ON bot.home_banking_id = hb.id
                LEFT JOIN home_url hu ON bot.home_url_id = hu.id AND hu.home_banking_id = hb.id
                LEFT JOIN block b ON b.bot_job_id = bot.id
                ORDER BY bot.id ASC, b.block_order_number ASC;
                """;

        Map<Integer, BotJobLoadDTO> botJobMap = new HashMap<>();
        Map<Integer, BlockLoadDTO> blockMap = new HashMap<>();

        try (PreparedStatement pstmt = getConnection().prepareStatement(query);
                ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                int botJobId = rs.getInt("bot_job_id");

                // Get or create BotJobLoadDTO
                BotJobLoadDTO botJobDTO = botJobMap.get(botJobId);
                if (botJobDTO == null) {
                    botJobDTO = new BotJobLoadDTO();
                    botJobDTO.setId(botJobId);
                    botJobDTO.setName(rs.getString("bot_job_name"));
                    botJobDTO.setDescription(rs.getString("bot_job_description"));
                    botJobDTO.setPriority(rs.getString("bot_job_priority"));
                    botJobDTO.setHomeBankingId(rs.getInt("home_banking_id"));
                    botJobDTO.setHomeUrlId(rs.getInt("home_url_id"));
                    botJobDTO.setActive(rs.getBoolean("active"));

                    // Set HomeBankingLoadDTO
                    Integer homeBankingId = rs.getObject("home_banking_id", Integer.class);
                    if (homeBankingId != null) {
                        HomeBankingLoadDTO homeBankingDTO = new HomeBankingLoadDTO();
                        homeBankingDTO.setId(homeBankingId);
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

                    botJobDTO.setBlockLoadDTOList(new ArrayList<>());
                    botJobMap.put(botJobId, botJobDTO);
                    performLists.getQuickBotJobs().add(botJobDTO);
                }

                // Add BlockLoadDTO if present
                int blockId = rs.getInt("block_id");
                if (!rs.wasNull()) {
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

                        blockDTO.setBotJobId(botJobId);
                        blockDTO.setBotJobName(botJobDTO.getName());

                        blockMap.put(blockId, blockDTO);
                        botJobDTO.getBlockLoadDTOList().add(blockDTO);
                    }
                }
            }

            return null;

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("Error loadQuickBotJobs: %s", e.getMessage()));

            return new ErrorMessage("Failed to load Quick Bot Jobs", "Database query error", e.getMessage());
        }
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
        String updateSQL;

        boolean isConditional = instruction.getActions().equals("IF")
                || instruction.getActions().equals("ELSEIF")
                || instruction.getActions().equals("ELSE")
                || instruction.getActions().equals("ENDIF");

        if (isConditional) {
            updateSQL = "UPDATE instruction SET active = ? WHERE block_id = ? AND parent_id = ?";
        } else {
            updateSQL = "UPDATE instruction SET active = ? WHERE id = ? AND block_id = ?";
        }

        try (PreparedStatement updateStmt = getConnection().prepareStatement(updateSQL)) {

            if (POSTGRES_DB) {
                updateStmt.setInt(1, instruction.getInstructionActive() ? 1 : 0);
            } else {
                updateStmt.setBoolean(1, instruction.getInstructionActive());
            }

            if (isConditional) {
                updateStmt.setInt(2, instruction.getBlockId());
                updateStmt.setInt(3, instruction.getParentId());
            } else {
                updateStmt.setInt(2, instruction.getInstructionId());
                updateStmt.setInt(3, instruction.getBlockId());
            }

            int rowsAffected = updateStmt.executeUpdate();

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

    public boolean updateBotStatus() {
        // SQL query to get the blocks for a specific bot job
        String query = "update bot_job set active = 1";

        // Initialize the necessary data structures

        // Use Statement to execute the query
        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(query)) {
            return true;

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("Error updating Active = 1 all botjobs,  Error: %s", error.getMessage()));
        }

        return false;
    }

    public ErrorMessage loadBlocks(
            Integer whereId, // botJobId or homeBankingId depending on tableName
            String botJobName,
            String tableName // "block" or "component_block"
            ) {
        // Validate tableName
        if (!"block".equals(tableName) && !"component_block".equals(tableName)) {
            return new ErrorMessage(
                    "Invalid Table Name", "tableName must be 'block' or 'component_block'", "Provided: " + tableName);
        }

        // Build SQL dynamically based on tableName
        StringBuilder query = new StringBuilder("SELECT ");
        if ("block".equals(tableName)) {
            query.append("b.id AS block_id, ")
                    .append("b.block_order_number, ")
                    .append("b.name AS block_name, ")
                    .append("b.description, ")
                    .append("b.type_id, ")
                    .append("b.wait, ")
                    .append("b.active, ")
                    .append("b.bot_job_id ")
                    //                    .append("bot.name AS bot_job_name ")
                    //                    append("FROM bot_job bot ")
                    .append("FROM block b ")
                    .append("WHERE b.bot_job_id = ? ")
                    .append("ORDER BY b.block_order_number ASC");
        } else { // component_block
            query.append("b.id AS block_id, ")
                    .append("b.block_order_number, ")
                    .append("b.name AS block_name, ")
                    .append("b.description, ")
                    .append("b.type_id, ")
                    .append("b.wait, ")
                    .append("b.active, ")
                    .append("b.home_banking_id ")
                    //                    .append("FROM home_banking hb ")
                    .append("FROM component_block b ")
                    //                    .append("JOIN component_instruction ci ON ci.home_banking_id = hb.id AND
                    // ci.block_id = b.id ")
                    .append("WHERE b.home_banking_id = ? ")
                    .append("ORDER BY b.block_order_number ASC");
        }

        // Choose target list depending on tableName
        List<BlockLoadDTO> targetList;
        if ("block".equals(tableName)) {
            performLists.getListBlock().clear();
            targetList = performLists.getListBlock();
        } else {
            performLists.getListBlockComp().clear();
            targetList = performLists.getListBlockComp();
        }

        Map<Integer, BlockLoadDTO> blockMapDTO = new HashMap<>();

        try (PreparedStatement pstmt = getConnection().prepareStatement(query.toString())) {
            pstmt.setInt(1, whereId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int blockId = rs.getInt("block_id");
                    BlockLoadDTO blockDTO = blockMapDTO.get(blockId);

                    if (blockDTO == null) {
                        blockDTO = new BlockLoadDTO();
                        blockDTO.setId(blockId);
                        blockDTO.setBlockOrderNumber(rs.getInt("block_order_number"));
                        blockDTO.setName(rs.getString("block_name"));
                        blockDTO.setDescription(rs.getString("description"));
                        blockDTO.setTypeId(rs.getInt("type_id"));
                        blockDTO.setWait(rs.getInt("wait"));
                        blockDTO.setActive(rs.getBoolean("active"));

                        if ("block".equals(tableName)) {
                            blockDTO.setBotJobId(rs.getInt("bot_job_id"));
                            blockDTO.setBotJobName(botJobName);
                        } else {
                            blockDTO.setHomeBankingId(rs.getInt("home_banking_id"));
                            blockDTO.setBotJobId(whereId != null ? whereId : 0);
                            blockDTO.setBotJobName(botJobName);
                        }

                        blockMapDTO.put(blockId, blockDTO);
                        targetList.add(blockDTO);
                    }
                }
            }
            return null; // ✅ success
        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error loading blocks for %s id %d\nError: %s", tableName, whereId, e.getMessage()));

            return new ErrorMessage(
                    "Load Blocks Error", "Failed to load blocks from table: " + tableName, e.getMessage());
        }
    }

    public ErrorMessage insertInstructionsBatch(
            String typeTask,
            List<InstructionLoadDTO> instructions,
            Integer currentBotJobId,
            Integer currentBlockId,
            Integer homeBankingId) {

        String tableName = "instruction";
        if ("componentTasks".equals(typeTask)) {
            tableName = "component_instruction";
        }

        final int BATCH_SIZE = 100;
        int count = 0;

        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement();
                Statement idStmtBefore = conn.createStatement();
                Statement idStmtAfter = conn.createStatement()) {

            // Step 1: Get all IDs before insertion
            List<Integer> idsBefore = new ArrayList<>();
            try (ResultSet rsBefore = idStmtBefore.executeQuery("SELECT id FROM " + tableName + " ORDER BY id")) {
                while (rsBefore.next()) {
                    idsBefore.add(rsBefore.getInt("id"));
                }
            }

            // Step 2: Perform batch insert
            for (InstructionLoadDTO instructionLoad : instructions) {
                StringBuilder columns = new StringBuilder();
                StringBuilder values = new StringBuilder();

                BiConsumer<String, Object> addColumnValue = (column, value) -> {
                    if (value != null) {
                        if (columns.length() > 0) {
                            columns.append(", ");
                            values.append(", ");
                        }
                        columns.append(column);
                        if (value instanceof String) {
                            values.append("'")
                                    .append(((String) value).replace("'", "''"))
                                    .append("'");
                        } else {
                            values.append(value);
                        }
                    }
                };

                // Add non-boolean fields
                addColumnValue.accept("coordinates", instructionLoad.getCoordinates());
                addColumnValue.accept("iframe_xpath", instructionLoad.getIFrameXPath());
                addColumnValue.accept("tag_name", instructionLoad.getTagName());
                addColumnValue.accept("shadow_host", instructionLoad.getShadowHost());
                addColumnValue.accept("shadow_root", instructionLoad.getShadowRoot());
                addColumnValue.accept("css_selector", instructionLoad.getCssSelector());
                addColumnValue.accept("xpath", instructionLoad.getXpath());
                addColumnValue.accept("action_custom_max_wait_sec", instructionLoad.getActionCustomMaxWaitSec());
                addColumnValue.accept("actions", instructionLoad.getActions());
                addColumnValue.accept("default_value", instructionLoad.getDefaultValue());
                addColumnValue.accept("description", instructionLoad.getDescription());
                addColumnValue.accept("instruction_order_number", instructionLoad.getInstructionOrderNumber());
                addColumnValue.accept("name", instructionLoad.getName());
                addColumnValue.accept(
                        "on_hold_seconds",
                        instructionLoad.getOnHoldSeconds() != null ? instructionLoad.getOnHoldSeconds() : 1);
                addColumnValue.accept("operation", instructionLoad.getOperation());
                addColumnValue.accept("parent_block_id", instructionLoad.getParentBlockId());
                addColumnValue.accept("parent_id", instructionLoad.getParentId());
                addColumnValue.accept("variable_id", instructionLoad.getVariableId());
                addColumnValue.accept("block_id", currentBlockId);

                if ("componentTasks".equals(typeTask)) {
                    addColumnValue.accept("home_banking_id", homeBankingId);
                } else {
                    addColumnValue.accept("bot_job_id", currentBotJobId);
                }

                // Boolean fields
                addColumnValue.accept(
                        "block_marked",
                        instructionLoad.getBlockMarked() != null ? (instructionLoad.getBlockMarked() ? 1 : 0) : null);
                addColumnValue.accept(
                        "codified",
                        instructionLoad.getCodified() != null ? (instructionLoad.getCodified() ? 1 : 0) : null);
                addColumnValue.accept(
                        "export_to_abr",
                        instructionLoad.getExportToABR() != null ? (instructionLoad.getExportToABR() ? 1 : 0) : null);
                addColumnValue.accept(
                        "optional",
                        instructionLoad.getOptional() != null ? (instructionLoad.getOptional() ? 1 : 0) : null);
                addColumnValue.accept(
                        "active",
                        instructionLoad.getInstructionActive() != null
                                ? (instructionLoad.getInstructionActive() ? 1 : 0)
                                : null);
                addColumnValue.accept(
                        "executed",
                        instructionLoad.getExecuted() != null ? (instructionLoad.getExecuted() ? 1 : 0) : null);
                addColumnValue.accept(
                        "block_active",
                        instructionLoad.getBlockActive() != null ? (instructionLoad.getBlockActive() ? 1 : 0) : null);
                addColumnValue.accept(
                        "refresh_loop",
                        instructionLoad.getRefreshLoop() != null ? (instructionLoad.getRefreshLoop() ? 1 : 0) : null);
                addColumnValue.accept(
                        "loop_only",
                        instructionLoad.getLoopOnly() != null ? (instructionLoad.getLoopOnly() ? 1 : 0) : null);
                addColumnValue.accept(
                        "force_coordinates",
                        instructionLoad.getForceCoordinates() != null
                                ? (instructionLoad.getForceCoordinates() ? 1 : 0)
                                : null);

                // Final insert
                String insertSQL = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columns, values);
                stmt.addBatch(insertSQL);

                if (++count % BATCH_SIZE == 0) {
                    stmt.executeBatch();
                    stmt.clearBatch();
                }
            }

            // Final batch
            if (count % BATCH_SIZE != 0) {
                stmt.executeBatch();
                stmt.clearBatch();
            }

            // Step 3: Get all IDs after insertion
            idsInstrucAfter.clear();
            try (ResultSet rsAfter = idStmtAfter.executeQuery("SELECT id FROM " + tableName + " ORDER BY id")) {
                while (rsAfter.next()) {
                    idsInstrucAfter.add(rsAfter.getInt("id"));
                }
            }

            idsInstrucAfter.removeAll(idsBefore);

            ARLogger.getInstance(PerformDataBase.class)
                    .info(String.format(
                            "Batch insert completed for %d %s records. New IDs: %s",
                            count, tableName.toUpperCase(), idsInstrucAfter));

            return null;

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe("Batch insert error for " + tableName + ": " + error.getMessage());
            return new ErrorMessage(
                    "Instruction Insertion Error", "Error inserting batch instructions.", error.getMessage());
        }
    }

    public ErrorMessage insertInstruction(
            String typeTask,
            List<InstructionOperationDTO> instructions,
            Integer currentBotJobId,
            Integer currentBlockId,
            Integer homeBankingId) {

        String tableName = "instruction";
        if ("componentTasks".equals(typeTask)) {
            tableName = "component_instruction";
        }

        final int BATCH_SIZE = 100;
        int count = 0;

        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement();
                Statement idStmtBefore = conn.createStatement();
                Statement idStmtAfter = conn.createStatement()) {

            // Step 1: Get all IDs before insertion
            List<Integer> idsBefore = new ArrayList<>();
            try (ResultSet rsBefore = idStmtBefore.executeQuery("SELECT id FROM " + tableName + " ORDER BY id")) {
                while (rsBefore.next()) {
                    idsBefore.add(rsBefore.getInt("id"));
                }
            }

            // Step 2: Build and add batch statements
            for (InstructionOperationDTO instructionLoad : instructions) {
                StringBuilder columns = new StringBuilder();
                StringBuilder values = new StringBuilder();

                BiConsumer<String, Object> addColumnValue = (column, value) -> {
                    if (value != null) {
                        if (columns.length() > 0) {
                            columns.append(", ");
                            values.append(", ");
                        }
                        columns.append(column);
                        if (value instanceof String) {
                            values.append("'")
                                    .append(((String) value).replace("'", "''"))
                                    .append("'");
                        } else {
                            values.append(value);
                        }
                    }
                };

                // Non-boolean fields
                addColumnValue.accept("action_custom_max_wait_sec", instructionLoad.getActionCustomMaxWaitSec());
                addColumnValue.accept("actions", instructionLoad.getActions());
                addColumnValue.accept("description", instructionLoad.getDescription());
                addColumnValue.accept("instruction_order_number", instructionLoad.getInstructionOrderNumber());
                addColumnValue.accept("name", instructionLoad.getName());
                addColumnValue.accept(
                        "on_hold_seconds",
                        instructionLoad.getOnHoldSeconds() != null ? instructionLoad.getOnHoldSeconds() : 1);
                addColumnValue.accept("operation", instructionLoad.getOperation());
                addColumnValue.accept("parent_block_id", instructionLoad.getParentBlockId());
                addColumnValue.accept("parent_id", instructionLoad.getParentId());
                addColumnValue.accept("variable_id", instructionLoad.getVariableId());
                addColumnValue.accept("block_id", currentBlockId);

                if ("componentTasks".equals(typeTask)) {
                    addColumnValue.accept("home_banking_id", homeBankingId);
                } else {
                    addColumnValue.accept("bot_job_id", currentBotJobId);
                }

                // Boolean fields
                addColumnValue.accept(
                        "active",
                        instructionLoad.getInstructionActive() != null
                                ? (instructionLoad.getInstructionActive() ? 1 : 0)
                                : null);
                addColumnValue.accept(
                        "block_active",
                        instructionLoad.getBlockActive() != null ? (instructionLoad.getBlockActive() ? 1 : 0) : null);
                addColumnValue.accept(
                        "refresh_loop",
                        instructionLoad.getRefreshLoop() != null ? (instructionLoad.getRefreshLoop() ? 1 : 0) : null);
                addColumnValue.accept(
                        "loop_only",
                        instructionLoad.getLoopOnly() != null ? (instructionLoad.getLoopOnly() ? 1 : 0) : null);
                addColumnValue.accept(
                        "force_coordinates",
                        instructionLoad.getForceCoordinates() != null
                                ? (instructionLoad.getForceCoordinates() ? 1 : 0)
                                : null);
                // Final SQL
                String insertSQL = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columns, values);
                stmt.addBatch(insertSQL);

                if (++count % BATCH_SIZE == 0) {
                    stmt.executeBatch();
                    stmt.clearBatch();
                }
            }

            // Final batch if not aligned with BATCH_SIZE
            if (count % BATCH_SIZE != 0) {
                stmt.executeBatch();
                stmt.clearBatch();
            }

            // Step 3: Get IDs after insertion
            idsInstrucAfter.clear();
            try (ResultSet rsAfter = idStmtAfter.executeQuery("SELECT id FROM " + tableName + " ORDER BY id")) {
                while (rsAfter.next()) {
                    idsInstrucAfter.add(rsAfter.getInt("id"));
                }
            }

            // Step 4: Compute new inserted IDs
            idsInstrucAfter.removeAll(idsBefore);

            ARLogger.getInstance(PerformDataBase.class)
                    .info(String.format(
                            "Batch insert completed for %d %s records. New IDs: %s",
                            count, tableName.toUpperCase(), idsInstrucAfter));

            return null;

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe("Batch insert error for " + tableName + ": " + error.getMessage());
            return new ErrorMessage("Instruction Insert Error", "Failed during batch insert", error.getMessage());
        }
    }

    public ErrorMessage updateInstructionParentIdOnly(String typeTask, List<InstructionOperationDTO> operations) {

        String tableName = "instruction";
        if ("componentTasks".equals(typeTask)) {
            tableName = "component_instruction";
        }

        StringBuilder batchSQL = new StringBuilder();

        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {

            for (InstructionOperationDTO operation : operations) {
                if (operation.getId() == null || operation.getParentId() == null) {
                    ARLogger.getInstance(PerformDataBase.class)
                            .warning("Skipping: Instruction ID or Parent ID is null.");
                    continue;
                }

                String updateSQL = String.format(
                        "UPDATE %s SET parent_id = %d WHERE id = %d;",
                        tableName, operation.getParentId(), operation.getId());

                batchSQL.append(updateSQL).append("\n");
                stmt.addBatch(updateSQL);
            }

            stmt.executeBatch();
            ARLogger.getInstance(PerformDataBase.class).info("Parent ID update batch executed:\n" + batchSQL);

            return null;

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Batch parent_id update failed: " + e.getMessage());

            return new ErrorMessage("Update Error", "Failed to update parent_id in instructions.", e.getMessage());
        }
    }

    public ErrorMessage updateInstruction(
            String typeTask,
            InstructionOperationDTO InstructionOperation,
            Integer currentBotJobId,
            Integer currentBlockId,
            Integer homeBankingId) {

        String tableName = "instruction";
        if (typeTask.equals("componentTasks")) {
            tableName = "component_instruction";
        }

        try (Statement stmt = getConnection().createStatement()) {
            if (InstructionOperation.getId() == null) {
                ARLogger.getInstance(PerformDataBase.class).warning("Instruction ID is null. Update failed.");
                return new ErrorMessage(
                        "Error Update Instruction",
                        "Error during updating instruction",
                        "Instruction ID is null. Update failed.");
            }

            StringBuilder setClause = new StringBuilder();

            // Helper method to add column assignments
            BiConsumer<String, Object> addColumnValue = (column, value) -> {
                if (value != null) {
                    if (setClause.length() > 0) {
                        setClause.append(", ");
                    }
                    if (value instanceof String) {
                        setClause
                                .append(column)
                                .append(" = '")
                                .append(((String) value).replace("'", "''"))
                                .append("'");
                    } else {
                        setClause.append(column).append(" = ").append(value);
                    }
                }
            };

            // Add fields to update
            addColumnValue.accept("action_custom_max_wait_sec", InstructionOperation.getActionCustomMaxWaitSec());
            addColumnValue.accept("actions", InstructionOperation.getActions());
            addColumnValue.accept("description", InstructionOperation.getDescription());
            addColumnValue.accept("instruction_order_number", InstructionOperation.getInstructionOrderNumber());
            addColumnValue.accept("name", InstructionOperation.getName());
            addColumnValue.accept("on_hold_seconds", InstructionOperation.getOnHoldSeconds());
            addColumnValue.accept("operation", InstructionOperation.getOperation());
            addColumnValue.accept("parent_block_id", InstructionOperation.getParentBlockId());
            addColumnValue.accept("parent_id", InstructionOperation.getParentId());
            addColumnValue.accept("variable_id", InstructionOperation.getVariableId());
            addColumnValue.accept("block_id", currentBlockId);

            if (typeTask.equals("componentTasks")) {
                addColumnValue.accept("home_banking_id", homeBankingId);
            } else {
                addColumnValue.accept("bot_job_id", currentBotJobId);
            }

            addColumnValue.accept(
                    "active",
                    InstructionOperation.getInstructionActive() != null
                            ? (InstructionOperation.getInstructionActive() ? 1 : 0)
                            : null);
            addColumnValue.accept(
                    "block_active",
                    InstructionOperation.getBlockActive() != null
                            ? (InstructionOperation.getBlockActive() ? 1 : 0)
                            : null);
            addColumnValue.accept(
                    "refresh_loop",
                    InstructionOperation.getRefreshLoop() != null
                            ? (InstructionOperation.getRefreshLoop() ? 1 : 0)
                            : null);
            addColumnValue.accept(
                    "loop_only",
                    InstructionOperation.getLoopOnly() != null ? (InstructionOperation.getLoopOnly() ? 1 : 0) : null);
            addColumnValue.accept(
                    "force_coordinates",
                    InstructionOperation.getForceCoordinates() != null
                            ? (InstructionOperation.getForceCoordinates() ? 1 : 0)
                            : null);

            if (setClause.isEmpty()) {
                ARLogger.getInstance(PerformDataBase.class)
                        .warning("No fields to update for instruction ID: " + InstructionOperation.getId());
                return new ErrorMessage(
                        "Error Update Instruction",
                        "Error during updating instruction",
                        "No fields to update for instruction ID: " + InstructionOperation.getId());
            }

            // Construct final SQL query
            String updateSQL =
                    String.format("UPDATE %s SET %s WHERE id = %d", tableName, setClause, InstructionOperation.getId());

            int rowsAffected = stmt.executeUpdate(updateSQL);
            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format(
                                "%s UPDATED SUCCESSFULLY id: %d Name: %s Actions: %s Operation: %s",
                                tableName.toUpperCase(),
                                InstructionOperation.getId(),
                                InstructionOperation.getName(),
                                InstructionOperation.getActions(),
                                InstructionOperation.getOperation()));
            } else {
                ARLogger.getInstance(PerformDataBase.class)
                        .warning(String.format(
                                "%s NOT UPDATED id: %d Name: %s Actions: %s Operations: %s",
                                tableName.toUpperCase(),
                                InstructionOperation.getId(),
                                InstructionOperation.getName(),
                                InstructionOperation.getActions(),
                                InstructionOperation.getOperation()));
            }
            return null;
        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class)
                    .warning(String.format(
                            "%s UPDATE FAILED id: %d Name: %s Actions: %s Operations: %s",
                            tableName.toUpperCase(),
                            InstructionOperation.getId(),
                            InstructionOperation.getName(),
                            InstructionOperation.getActions(),
                            InstructionOperation.getOperation()));
            return new ErrorMessage(
                    "Error Update Instruction", "Error during updating instruction", error.getMessage());
        }
    }

    public boolean instructionIdExists(int instructionId) {
        String query = "SELECT COUNT(*) FROM instruction WHERE id = ?";
        try (Connection connection = getConnection(); // Assuming getConnection() provides a valid Connection
                PreparedStatement pstmt = connection.prepareStatement(query)) {

            pstmt.setInt(1, instructionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0; // If count is greater than 0, the ID exists
                }
            }
        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error checking for instruction ID %d. Error: %s", instructionId, error.getMessage()));
        }
        return false; // Return false if an error occurs or the ID is not found
    }

    public List<InstructionLoadDTO> preInsertStep(
            RowMoveDTO rowMoveDTO, List<InstructionLoadDTO> rowList, int shiftQty) {

        String operationType = rowMoveDTO.getType();

        if ("INSERT_BEFORE".equals(operationType)
                || "INSERT_AFTER".equals(operationType)
                || "INSERT_AFTER_ELSEIF".equals(operationType)) {

            int targetOrderNumber = rowMoveDTO.getUpdatedRows().get(0).getInstructionOrderNumber();

            boolean orderNumberExists = rowList.stream()
                    .anyMatch(instruction -> instruction.getInstructionOrderNumber() == targetOrderNumber);

            if (!orderNumberExists) {
                ARLogger.getInstance(PerformDataBase.class)
                        .warning(String.format(
                                "preInsertStep - Target order number %d does not exist in the row list.",
                                targetOrderNumber));
            }

            for (InstructionLoadDTO instruction : rowList) {
                boolean shouldShift = "INSERT_BEFORE".equals(operationType)
                        ? instruction.getInstructionOrderNumber() >= targetOrderNumber
                        : instruction.getInstructionOrderNumber() > targetOrderNumber;

                if (shouldShift) {
                    instruction.setInstructionOrderNumber(instruction.getInstructionOrderNumber() + shiftQty);
                }
            }
        }
        return rowList;
    }

    public ErrorMessage preFillNewInstruction(
            String name,
            String description,
            String actions,
            String operation,
            Integer onHold,
            RowMoveDTO rowMoveDTO,
            boolean blockIdChanged) {

        //        this.botJobLoadDTO = loadBotJobById(rowMoveDTO.getBotJobId());

        boolean updateRow = rowMoveDTO.getType().equals("EDIT_OPERATION");
        boolean isIF = actions.equalsIgnoreCase(ARConstants.IF);

        if (!updateRow) {
            List<InstructionLoadDTO> rowList = null;
            String tableName = "instruction";
            int whereId = rowMoveDTO.getBotJobId();
            if (rowMoveDTO.getSessionId().equals("componentTasks")) {
                tableName = "component_instruction";
                whereId = rowMoveDTO.getHomeBankingId();
            }

            loadInstructions(whereId, rowMoveDTO.getBlockId(), -1, tableName);
            rowList = tableName.equals("instruction")
                    ? performLists.getListInstruction()
                    : performLists.getListInstructionComp();

            if (!blockIdChanged) {
                if (isIF) {
                    rowList = preInsertStep(rowMoveDTO, rowList, 3);
                } else {
                    rowList = preInsertStep(rowMoveDTO, rowList, 1);
                }
                reorderInstructions(rowList, tableName, true);
            } else {
                rowMoveDTO.getUpdatedRows().get(0).setInstructionOrderNumber(rowList.size() + 1);
            }
        }

        List<BlockLoadDTO> matchingBlocks = null;

        if (performLists.getQuickBotJobs().isEmpty()) {
            loadQuickBotJobs();
        }

        String blockTable = "block";
        int whereId = rowMoveDTO.getBotJobId();
        if (rowMoveDTO.getSessionId().equals("componentTasks")) {
            blockTable = "component_block";
            whereId = rowMoveDTO.getHomeBankingId();
        }
        loadBlocks(whereId, "", blockTable);

        if (!rowMoveDTO.getUpdatedRows().isEmpty()) {

            Integer targetBlockId = -1;

            if (rowMoveDTO.getType().equals("INSERT_NEW")) {
                targetBlockId = rowMoveDTO.getBlockOrderNumber();

                Integer finalTargetBlockId = targetBlockId;

                List<BlockLoadDTO> listBlock = null;
                if (rowMoveDTO.getSessionId().equals("componentTasks")) {
                    listBlock = performLists.getListBlockComp();
                } else {
                    listBlock = performLists.getListBlock();
                }

                matchingBlocks = listBlock.stream()
                        .filter(block -> block.getBlockOrderNumber().equals(finalTargetBlockId))
                        .collect(Collectors.toList());

            } else {
                targetBlockId = rowMoveDTO.getBlockId();
                Integer finalTargetBlockId1 = targetBlockId;

                List<BlockLoadDTO> listBlock = null;
                if (rowMoveDTO.getSessionId().equals("componentTasks")) {
                    listBlock = performLists.getListBlockComp();
                } else {
                    listBlock = performLists.getListBlock();
                }

                matchingBlocks = listBlock.stream()
                        .filter(block -> block.getId().equals(finalTargetBlockId1))
                        .collect(Collectors.toList());
            }
        }

        List<BlockLoadDTO> finalMatchingBlocks = matchingBlocks;

        InstructionOperationDTO instruction = new InstructionOperationDTO();
        // EDIT_OPERATION
        if (updateRow) {
            int idToUpdate = rowMoveDTO.getUpdatedRows().get(0).getInstructionId();
            instruction.setId(idToUpdate);
        }
        instruction.setName(name);
        instruction.setInstructionActive(true);

        if (rowMoveDTO != null && !rowMoveDTO.getUpdatedRows().isEmpty()) {
            if ("INSERT_BEFORE".equals(rowMoveDTO.getType()) || "EDIT_OPERATION".equals(rowMoveDTO.getType())) {
                instruction.setInstructionOrderNumber(
                        rowMoveDTO.getUpdatedRows().get(0).getInstructionOrderNumber());
            } else {
                instruction.setInstructionOrderNumber(
                        rowMoveDTO.getUpdatedRows().get(0).getInstructionOrderNumber() + 1);
            }
        } else {
            instruction.setInstructionOrderNumber(finalMatchingBlocks.size() + 1);
        }

        // PARENT BLOCK ID
        if (rowMoveDTO.getParentBlockId() != null
                && (actions.equalsIgnoreCase("GOTO") || actions.equalsIgnoreCase("EXCEL GOTO"))) {
            instruction.setParentBlockId(rowMoveDTO.getParentBlockId());
        }

        // PARENT ID
        if (rowMoveDTO.getUpdatedRows().get(0).getParentId() != null
                && (actions.equalsIgnoreCase(ARConstants.GET_VALUE)
                        || actions.equalsIgnoreCase(ARConstants.SET_VALUE)
                        || actions.equalsIgnoreCase(ARConstants.CHECK_VALUE)
                        || actions.equalsIgnoreCase(ARConstants.EXTRACT_FIELD)
                        || actions.equalsIgnoreCase(ARConstants.LOOP)
                        || actions.equalsIgnoreCase(ARConstants.REFRESH_LOOP))) {
            instruction.setParentId(rowMoveDTO.getUpdatedRows().get(0).getParentId());
        }

        if (rowMoveDTO.getUpdatedRows().get(0).getVariableId() != null) {
            instruction.setVariableId(rowMoveDTO.getUpdatedRows().get(0).getVariableId());
        }

        instruction.setOperation(operation);
        instruction.setActions(actions);
        instruction.setDescription(description);

        instruction.setActionCustomMaxWaitSec(30);
        instruction.setOnHoldSeconds(onHold);

        // Define where to get the BlockId
        //        instruction.setBlockId(rowMoveDTO.getBotJobId());
        //        if (!rowMoveDTO.getSessionId().equals("componentTasks")) {
        if (finalMatchingBlocks != null && !finalMatchingBlocks.isEmpty()) {
            instruction.setBlockId(finalMatchingBlocks.get(0).getId());
        } else {

            BlockDetailsDTO newBlockDetails = new BlockDetailsDTO();
            newBlockDetails.setBlockName("Default Block");
            newBlockDetails.setBlockDescription("Default Block description");
            newBlockDetails.setTypeId(1);
            newBlockDetails.setActive(true);
            newBlockDetails.setWait(3);

            newBlockDetails.setBotJobId(rowMoveDTO.getBotJobId());
            newBlockDetails.setBlockId(rowMoveDTO.getBlockId());

            ErrorMessage errorMessage = initiateNewBlock(newBlockDetails, rowMoveDTO.getBotJobId(), false);

            if (errorMessage == null) {
                int newBlockId = -9999;
                if (!getIdsBlockAfter().isEmpty() && getIdsBlockAfter().get(0) > 0) {
                    newBlockId = getIdsBlockAfter().get(0);
                }

                // IT SETS THE NEW TARGET IN CASE TO ADD MORE INSTRUCTIONS
                rowMoveDTO.setBlockId(newBlockId);

                String tableName = "block";
                if (rowMoveDTO.getSessionId().equals("componentTasks")) {
                    tableName = "component_block";
                }
                loadBlocks(rowMoveDTO.getBotJobId(), rowMoveDTO.getBotJobName(), tableName);
                instruction.setBlockId(newBlockId);
            } else {
                return errorMessage;
            }
        }
        //        }
        instruction.setInstructionActive(true);
        // Wrap the persistence in a try-catch block
        ErrorMessage errorMessage = null;

        try {
            int targetOrderNumber = rowMoveDTO.getUpdatedRows().get(0).getInstructionOrderNumber();

            Integer currentBlockId = rowMoveDTO.getBlockId();

            if (instruction.getBlockId() != null && !instruction.getBlockId().equals(currentBlockId)) {
                currentBlockId = instruction.getBlockId();
            }
            if (!updateRow) {
                performLists.getInstrucOperList().clear();
                performLists.getInstrucOperList().add(instruction);

                if (isIF) {
                    // Create ELSE
                    int orderNumber = instruction.getInstructionOrderNumber();
                    orderNumber++;
                    InstructionOperationDTO elseInstr = buildFromLoadDTO(instruction);
                    elseInstr.setName("ELSE");
                    elseInstr.setDescription("ELSE");
                    elseInstr.setActions(ARConstants.ELSE);
                    elseInstr.setOperation(ARConstants.ELSE);
                    elseInstr.setInstructionOrderNumber(orderNumber);
                    performLists.getInstrucOperList().add(elseInstr);

                    // Create ENDIF
                    orderNumber++;
                    InstructionOperationDTO endifInstr = buildFromLoadDTO(instruction);
                    endifInstr.setName("ENDIF");
                    endifInstr.setDescription("ENDIF");
                    endifInstr.setActions(ARConstants.ENDIF);
                    endifInstr.setOperation(ARConstants.ENDIF);
                    endifInstr.setInstructionOrderNumber(orderNumber);
                    performLists.getInstrucOperList().add(endifInstr);

                    // Last OrderNumber added
                    targetOrderNumber = orderNumber;
                }

                errorMessage = insertInstruction(
                        rowMoveDTO.getSessionId(),
                        performLists.getInstrucOperList(),
                        rowMoveDTO.getBotJobId(),
                        currentBlockId,
                        rowMoveDTO.getHomeBankingId());

                if (isIF && errorMessage == null && idsInstrucAfter.size() == 3) {
                    int index = 0;
                    for (InstructionOperationDTO instruct : performLists.getInstrucOperList()) {
                        instruct.setId(idsInstrucAfter.get(index));
                        instruct.setParentId(idsInstrucAfter.get(0)); // PARENT ID FOR ALL
                        index++; // To get the NEWS Ids
                    }
                    errorMessage =
                            updateInstructionParentIdOnly(rowMoveDTO.getSessionId(), performLists.getInstrucOperList());
                }
            } else {
                errorMessage = updateInstruction(
                        rowMoveDTO.getSessionId(),
                        instruction,
                        rowMoveDTO.getBotJobId(),
                        currentBlockId,
                        rowMoveDTO.getHomeBankingId());
            }

            if (!updateRow || blockIdChanged) {
                rowMoveDTO.getUpdatedRows().get(0).setInstructionOrderNumber(targetOrderNumber);
            }

            if (errorMessage == null) {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format(
                                "\"Component\" Instruction: \"%s\" has been added successfully!",
                                instruction.getName()));
            } else {
                ARLogger.getInstance(PerformDataBase.class)
                        .severe(String.format(
                                "Error Add New \"Component\" Instruction: \"%s\" Cannot be saved!",
                                instruction.getName()));

                performMessage.errorMessage(
                        errorMessage.getErrorTitle(),
                        "<span style='color: #D32F2F; font-weight: bold; font-size: 1.1em;'>Operation Failed!</span> ❌",
                        "<span style='color: #E65100; font-weight: bold;'>Error Type:</span> "
                                + errorMessage.getErrorTitle(),
                        "<span style='font-style: italic;'>Detail:</span> " + errorMessage.getErrorMessage(),
                        null,
                        0);
            }

        } catch (Exception e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Cannot Insert Instruction\nError: " + e.getMessage());
        } finally {
            performLists.getInstrucOperList().clear();
        }
        return errorMessage;
    }

    public void loadHomeBanking(Integer homeBankingId) {
        performLists.getListHomeBanking().clear();
        Map<Integer, HomeBankingLoadDTO> homeBankingMap = new HashMap<>();

        String selectSQL = "SELECT hb.id AS hb_id, " + "       hb.cookies, "
                + "       hb.driver_session, "
                + "       hb.name, "
                + "       hb.options_config, "
                + "       hb.password, "
                + "       hb.priority, "
                + "       hb.search_config, "
                + "       hb.url AS hb_url, "
                + "       hb.username, "
                + "       COUNT(bot.id) AS jobs "
                + "FROM home_banking hb "
                + "LEFT JOIN bot_job bot ON hb.id = bot.home_banking_id "
                +
                //                        "WHERE (? IS NULL OR hb.id = ?) " +
                "GROUP BY hb.id "
                + "ORDER BY hb.id";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {

            //            // set parameter twice (because of "? IS NULL OR hb.id = ?")
            //            if (homeBankingId != null) {
            //                pstmt.setInt(1, homeBankingId);
            //                pstmt.setInt(2, homeBankingId);
            //            } else {
            //                pstmt.setNull(1, java.sql.Types.INTEGER);
            //                pstmt.setNull(2, java.sql.Types.INTEGER);
            //            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Integer currentHomeBankingId = rs.getInt("hb_id");

                    HomeBankingLoadDTO homeBanking = new HomeBankingLoadDTO();
                    homeBanking.setId(currentHomeBankingId);
                    homeBanking.setCookies(rs.getString("cookies"));
                    homeBanking.setDriverSession(rs.getString("driver_session"));
                    homeBanking.setName(rs.getString("name"));
                    homeBanking.setOptionsConfig(rs.getString("options_config"));
                    homeBanking.setPassword(rs.getString("password"));
                    homeBanking.setPriority(rs.getString("priority"));
                    homeBanking.setSearchConfig(rs.getString("search_config"));
                    homeBanking.setUrl(rs.getString("hb_url"));
                    homeBanking.setUsername(rs.getString("username"));
                    homeBanking.setJobs(rs.getInt("jobs"));

                    homeBankingMap.put(currentHomeBankingId, homeBanking);
                }
            }

            performLists.getListHomeBanking().addAll(homeBankingMap.values());

        } catch (SQLException e) {
            String message = homeBankingId != null
                    ? String.format(
                            "Error selecting home banking record with ID %d. Error: %s", homeBankingId, e.getMessage())
                    : "Error selecting ALL home banking records";
            ARLogger.getInstance(PerformDataBase.class).severe(message);
        }
    }

    public void loadWebPageFields(int whereId, String tableName) {
        performLists.getListWebPageItems().clear();

        String sql;
        boolean useComponent = "home_banking".equalsIgnoreCase(tableName);

        if (useComponent) {
            sql = "SELECT hb.id AS home_banking_id, " + "b.id AS block_id, bli.id AS instruction_id, "
                    + "bli.instruction_order_number, bli.actions, bli.name AS instruction_name, "
                    + "bli.xpath, bli.operation, bli.tag_name "
                    + "FROM home_banking hb "
                    + "LEFT JOIN component_block b ON b.home_banking_id = hb.id "
                    + "JOIN component_instruction bli ON bli.block_id = b.id "
                    + "WHERE hb.id = ? AND operation IS NULL "
                    + "ORDER BY hb.id, b.block_order_number, bli.instruction_order_number ASC";
        } else {
            sql = "SELECT bot.id AS bot_job_id, " + "b.id AS block_id, bli.id AS instruction_id, "
                    + "bli.instruction_order_number, bli.actions, bli.name AS instruction_name, "
                    + "bli.xpath, bli.operation, bli.tag_name "
                    + "FROM bot_job bot "
                    + "LEFT JOIN block b ON b.bot_job_id = bot.id "
                    + "JOIN instruction bli ON bli.block_id = b.id "
                    + "WHERE bot.active = 1 AND bot.id = ? AND operation IS NULL "
                    + "ORDER BY bot.id, b.block_order_number, bli.instruction_order_number ASC";
        }

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, whereId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("instruction_id");
                    String name = rs.getString("instruction_name");
                    String actions = rs.getString("actions");

                    if (name != null) name = name.trim();
                    if (actions != null) actions = actions.trim();

                    // Filter out unwanted actions
                    if (actions == null
                            || actions.equalsIgnoreCase(ARConstants.SET_VALUE)
                            || actions.equalsIgnoreCase(ARConstants.GET_VALUE)
                            || actions.equalsIgnoreCase(ARConstants.CHECK_VALUE)
                            || actions.equalsIgnoreCase(ARConstants.HOLD)
                            || actions.equalsIgnoreCase(ARConstants.PAUSE)
                            || actions.equalsIgnoreCase(ARConstants.EXCEL_GOTO)
                            || actions.equalsIgnoreCase(ARConstants.SCREEN)
                            || actions.equalsIgnoreCase(ARConstants.QUIT)) {
                        continue;
                    }

                    int blockId = rs.getInt("block_id");
                    String tagName = rs.getString("tag_name");
                    if (tagName != null) tagName = tagName.trim();

                    int orderNumber = rs.getInt("instruction_order_number");

                    performLists
                            .getListWebPageItems()
                            .add(new ComboBoxVars(
                                    "(" + id + ")" + name, name, id, blockId, -1, -1, tagName, orderNumber, null));
                }
            }
        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("loadWebPageFields - SQL Error: %s", e.getMessage()));
        } catch (Exception ex) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("loadWebPageFields - General Error: %s", ex.getMessage()));
        }
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

    public List<InstructionLoadDTO> filterInstructions(List<InstructionLoadDTO> instructionList) {
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

    public List<InstructionLoadDTO> buildJsonViewData(
            List<BotJobLoadDTO> listInstruction, int whereId, String tableName) {
        if (!listInstruction.isEmpty()
                && !listInstruction.get(0).getBlockLoadDTOList().isEmpty()) {

            List<InstructionLoadDTO> rowList = null;
            try {

                for (BlockLoadDTO block : listInstruction.get(0).getBlockLoadDTOList()) {
                    loadInstructions(whereId, block.getId(), -1, tableName);
                    rowList = tableName.equals("instruction")
                            ? performLists.getListInstruction()
                            : performLists.getListInstructionComp();
                    reorderInstructions(rowList, tableName, false);
                }

                List<InstructionLoadDTO> blockLoopInstructions = listInstruction.get(0).getBlockLoadDTOList().stream()
                        .flatMap(itemBlock -> itemBlock.getInstructionLoadDTOS().stream()
                                .map(loopInstLoad -> new InstructionLoadDTO(
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
                        .map(InstructionLoadDTO::getParentId)
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
                        .map(InstructionLoadDTO::getParentId)
                        .collect(Collectors.toSet());

                // Step 2: Iterate through the list and set loopOnly = true for rows with id in parentIdsForLoopOnly
                blockLoopInstructions.forEach(instruction -> {
                    if (parentIdsForLoopOnly.contains(instruction.getId())) {
                        instruction.setLoopOnly(true);
                    }
                });

                return blockLoopInstructions;
            } catch (Exception error) {
                System.err.println("No BotJob Loaded for buildJsonViewData");
            }
        }

        return new ArrayList<>();
    }

    public List<BlockLoadDTO> loadSavedBlocksForBotJob(int homeBankingId, Integer botJobId, String botJobName) {
        // SQL query to get the blocks for a specific bot job
        String query = "\n" + "SELECT \n"
                + "  hb.id as home_banking_id,\n"
                + "  hb.name as home_banking_name, \n"
                + "  bc.id AS block_id, \n"
                + "  bc.block_order_number, \n"
                + "  bc.name AS block_name, \n"
                + "  bc.description AS block_description, \n"
                + "  bc.type_id \n"
                //                + "  bot.id AS bot_job_id, \n"
                //                + "  bot.name AS bot_job_name \n"
                + "  FROM \n"
                + "  component_block bc \n"
                //                + "  JOIN bot_job bot on bot.active = 1 and bot.id = bc.bot_job_id \n"
                + "  JOIN home_banking hb ON hb.id = bc.home_banking_id \n"
                + "WHERE \n"
                + "  hb.id = "
                + homeBankingId;

        // Initialize the necessary data structures
        List<BlockLoadDTO> savedlistBlock = new ArrayList<>();

        Map<Integer, BlockLoadDTO> blockMapDTO = new HashMap<>();

        // Use Statement to execute the query
        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                // Load the Block information
                int blockId = rs.getInt("block_id");
                BlockLoadDTO blockDTO = blockMapDTO.get(blockId);

                if (blockDTO == null) {
                    blockDTO = new BlockLoadDTO();
                    blockDTO.setHomeBankingName(rs.getString("home_banking_name"));
                    blockDTO.setHomeBankingId(rs.getInt("home_banking_id"));
                    blockDTO.setId(blockId);
                    blockDTO.setBlockOrderNumber(rs.getInt("block_order_number"));
                    blockDTO.setName(rs.getString("block_name"));
                    blockDTO.setDescription(rs.getString("block_description"));
                    blockDTO.setTypeId(rs.getInt("type_id"));
                    blockDTO.setBotJobId(botJobId);
                    blockDTO.setBotJobName(botJobName);

                    blockMapDTO.put(blockId, blockDTO);
                    savedlistBlock.add(blockDTO);
                }
            }
        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error loadSavedBlocksForBotJob for Home Banking Id %d\nError: %s",
                            homeBankingId, e.getMessage()));
        }

        return savedlistBlock;
    }

    public ErrorMessage insertReferencesBatch(List<InstructionLoadDTO> instructionList) {
        String insertSQL =
                "INSERT INTO reference(reference_type, value, instruction_id, bot_job_id) VALUES (?, ?, ?, ?)";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            final int BATCH_SIZE = 100;
            int count = 0;

            for (InstructionLoadDTO instruction : instructionList) {
                Integer instructionId = instruction.getId();
                Integer botJobId = instruction.getBotJobId();

                if (instruction.getReferenceLoadDTOList() == null) continue;

                for (ReferenceLoadDTO reference : instruction.getReferenceLoadDTOList()) {
                    if ("customXPath".equalsIgnoreCase(reference.getReferenceType())) {
                        continue; // Skip this type
                    }

                    pstmt.setString(1, reference.getReferenceType());
                    pstmt.setString(2, reference.getValue());
                    pstmt.setInt(3, instructionId);
                    pstmt.setInt(4, botJobId);

                    pstmt.addBatch();

                    if (++count % BATCH_SIZE == 0) {
                        pstmt.executeBatch();
                        pstmt.clearBatch();
                    }
                }
            }

            if (count % BATCH_SIZE != 0) {
                pstmt.executeBatch();
                pstmt.clearBatch();
            }

            ARLogger.getInstance(PerformDataBase.class).info("Reference batch insert completed successfully.");
            return null; // No error

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe("Failed to insert references into database: " + error.getMessage());
            return new ErrorMessage(
                    "Reference Insertion Error", "An error occurred during reference insertion.", error.getMessage());
        } catch (Exception error) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe("Unexpected error inserting references: " + error.getMessage());
            return new ErrorMessage(
                    "Reference Insertion Error",
                    "An unexpected error occurred during reference insertion.",
                    error.getMessage());
        }
    }

    public ErrorMessage insertReferences(List<ReferenceLoadDTO> queue, int instructionId) {
        String insertSQL =
                "INSERT INTO reference(reference_type, value, instruction_id, bot_job_id) VALUES (?, ?, ?, ?)";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            final int BATCH_SIZE = 100;
            int count = 0;

            for (ReferenceLoadDTO reference : queue) {
                if ("customXPath".equalsIgnoreCase(reference.getReferenceType())) {
                    continue; // Skip this type
                }

                pstmt.setString(1, reference.getReferenceType());
                pstmt.setString(2, reference.getValue());
                pstmt.setInt(3, instructionId);
                pstmt.setInt(4, reference.getBotJobId());

                pstmt.addBatch();

                if (++count % BATCH_SIZE == 0) {
                    pstmt.executeBatch();
                    pstmt.clearBatch();
                }
            }

            if (count % BATCH_SIZE != 0) {
                pstmt.executeBatch();
                pstmt.clearBatch();
            }

            ARLogger.getInstance(PerformDataBase.class).info("Reference batch insert completed successfully.");
            return null; // No error
        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe("Failed to insert references into database: " + error.getMessage());
            return new ErrorMessage(
                    "Reference Insertion Error", "An error occurred during reference insertion.", error.getMessage());
        } catch (Exception error) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe("Failed to insert references into database: " + error.getMessage());
            return new ErrorMessage(
                    "Reference Insertion Error", "An error occurred during reference insertion.", error.getMessage());
        }
    }

    // Handle DELETE_INSTRUCTION message
    public ErrorMessage deleteInstruction(
            String tableName, int whereId, InstructionLoadDTO toDelete, boolean blockDeletion) {

        List<InstructionLoadDTO> listInstruc = new ArrayList<>();
        listInstruc.add(toDelete);

        ErrorMessage errorMessage = null;

        if (toDelete.getParentId() != null) {
            List<ParentOperations> listParents =
                    loadParents(tableName, whereId, toDelete.getInstructionId(), toDelete.getParentId());

            if (!listParents.isEmpty()) {

                boolean isIF = toDelete.getActions().equalsIgnoreCase("IF")
                        || toDelete.getActions().equalsIgnoreCase("ELSE")
                        || toDelete.getActions().equalsIgnoreCase("ENDIF")
                        || toDelete.getActions().equalsIgnoreCase("ELSEIF");

                if (!blockDeletion && !isIF) {
                    List<String> lstMsg = performMessage.distributeMsg(
                            listParents.stream().map(ParentOperations::getName).collect(Collectors.toList()));

                    ARConstants.DialogModal respModal = performMessage.showCustomModalDialogDragWin11(
                            "Steps Attached",
                            "Are you Sure you want to delete?",
                            lstMsg.get(0),
                            lstMsg.get(1),
                            lstMsg.get(2),
                            false,
                            "Confirm",
                            "Cancel",
                            0);

                    if (respModal.equals(ARConstants.DialogModal.STOP)) {
                        return null;
                    }
                }

                errorMessage = deleteRowParents(tableName, whereId, toDelete.getInstructionId());
            }
        }

        if (errorMessage == null) {
            String variableTable = tableName.equals("instruction") ? "variable" : "component_variable";
            errorMessage = deleteVariablesBatch(variableTable, whereId, listInstruc);
        }

        if (errorMessage == null) {

            String referenceTable = tableName.equals("instruction") ? "reference" : "component_reference";
            errorMessage = deleteReferencesBatch(referenceTable, whereId, listInstruc);
        }

        if (errorMessage == null) {
            String instructionTable = tableName.equals("instruction") ? "instruction" : "component_instruction";
            errorMessage = deleteInstructionsBatch(instructionTable, whereId, listInstruc);
        }

        //        if (errorMessage == null) {
        //            String blockTable = tableName.equals("instruction") ? "block" : "component_block";
        //            errorMessage = deleteNullBlocks(blockTable, whereId);
        //        }
        if (errorMessage == null) {
            String blockTable = tableName.equals("instruction") ? "block" : "component_block";
            loadBlocks(whereId, "", blockTable);
            errorMessage = updateBlockOrderNumber(blockTable, whereId, true);
        }

        if (errorMessage != null) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe("Error: " + errorMessage.getErrorTitle() + "-" + errorMessage.getErrorMessage());
        }

        return errorMessage;
    }

    public void updateDatabaseSchema(String dbUrl, File dbFile) {

        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            try (Statement stmt = conn.createStatement()) {

                DatabaseMetaData dbMeta = conn.getMetaData();

                // 1. Check if the foreign key constraint already exists
                boolean fkExists = false;
                ResultSet rsFK = null;
                try {
                    // getImportedKeys(catalog, schema, table)
                    // For Access, catalog and schema are usually null or empty string.
                    // "bot_job" is the table that *has* the foreign key.
                    rsFK = dbMeta.getImportedKeys(null, null, "bot_job");

                    while (rsFK.next()) {
                        String fkColumnName = rsFK.getString("FKCOLUMN_NAME");
                        String pkTableName = rsFK.getString("PKTABLE_NAME");
                        String pkColumnName = rsFK.getString("PKCOLUMN_NAME");

                        // Check if it's the specific foreign key we want
                        // Matching by foreign key column, referenced table, and referenced primary key column
                        if ("home_url_id".equalsIgnoreCase(fkColumnName)
                                && "home_url".equalsIgnoreCase(pkTableName)
                                && "id".equalsIgnoreCase(pkColumnName)) {
                            fkExists = true;
                            // Optional: You can print the FK_NAME if you want to know what Access called it
                            // String fkName = rsFK.getString("FK_NAME");
                            // System.out.println(String.format("Foreign key '%s' (home_url_id -> home_url.id) already
                            // exists.", fkName));
                            break;
                        }
                    }
                } finally {
                    if (rsFK != null) {
                        try {
                            rsFK.close();
                        } catch (SQLException e) {
                            System.err.println("Error closing ResultSet for FK check: " + e.getMessage());
                        }
                    }
                }

                // 2. Add the foreign key constraint only if it doesn't exist
                if (!fkExists) {
                    System.out.println(String.format(
                            "Foreign key 'FK_NewHomeURL' (home_url_id -> home_url.id) not found. Adding it..."));
                    String addHomrURLForeignKeySQL = "ALTER TABLE bot_job "
                            + "ADD CONSTRAINT FK_NewHomeURL FOREIGN KEY (home_url_id) "
                            + "REFERENCES home_url(id) ";
                    stmt.executeUpdate(addHomrURLForeignKeySQL);
                    System.out.println(String.format("Foreign key 'FK_NewHomeURL' added to 'bot_job' table."));
                    System.out.println(
                            String.format("Database %s has been updated with the foreign key!", dbFile.getName()));
                } else {
                    System.out.println(String.format(
                            "Database %s no need for foreign key 'FK_NewHomeURL' updates (constraint exists).",
                            dbFile.getName()));
                }

                // Ensure the statement is closed
                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch (SQLException e) {
                        System.err.println("Error closing Statement: " + e.getMessage());
                    }
                }
            }
        } catch (SQLException error) {
            System.out.println("initializeDatabase\nError: " + error.getMessage());
        }
    }

    public void disableForeignKeyConstraints(String dbUrl) {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            DatabaseMetaData meta = conn.getMetaData();
            Statement stmt = conn.createStatement();

            // Loop through all tables
            ResultSet tables = meta.getTables(null, null, null, new String[] {"TABLE"});
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");

                // Get foreign keys for the table
                ResultSet fks = meta.getImportedKeys(null, null, tableName);
                while (fks.next()) {
                    String fkName = fks.getString("FK_NAME");

                    if (fkName != null && !fkName.trim().isEmpty()) {
                        String dropSQL = String.format("ALTER TABLE [%s] DROP CONSTRAINT [%s]", tableName, fkName);
                        System.out.println("Dropping FK: " + dropSQL);
                        try {
                            stmt.executeUpdate(dropSQL);
                        } catch (SQLException ex) {
                            System.err.println("Failed to drop constraint " + fkName + ": " + ex.getMessage());
                        }
                    }
                }
                fks.close();
            }

            tables.close();
            stmt.close();
            System.out.println("All foreign key constraints removed.");

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Error disable Foreign Key Constraints");
        }
    }

    private void updateBotJobHomeUrlId(List<HomeUrlDTO> listHomeUrl) {
        try (Connection conn = getConnection()) {

            ErrorMessage errorMessage = updateBotJobHomeUrlIds(conn, listHomeUrl);

            if (errorMessage != null) {
                performMessage.errorMessage(
                        "Updating Bot Job Home URL IDs Error",
                        "<span style='color: #2E7D32; font-weight: bold; font-size: 1.1em;'>Bot Job Update Home URL IDs error!</span>",
                        null,
                        null,
                        null,
                        0);
            }

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private ErrorMessage updateBotJobHomeUrlIds(Connection conn, List<HomeUrlDTO> listHomeUrl) {
        // Update bot_job only if the home_url_id to be set exists in the home_url table
        String blockInsertQuery = "UPDATE bot_job AS bj " + "SET bj.home_url_id = ? "
                + "WHERE bj.home_banking_id = ? "
                + "AND EXISTS (SELECT 1 FROM home_url WHERE id = ?);"; // Parameter for home_url.id check

        try (PreparedStatement blockStmt = conn.prepareStatement(blockInsertQuery)) {
            boolean batchModeEnabled = false;
            for (HomeUrlDTO homeUrl : listHomeUrl) {
                blockStmt.setInt(1, homeUrl.getId()); // 1st ?: Sets home_url_id in bot_job
                blockStmt.setInt(2, homeUrl.getHomeBankingId()); // 2nd ?: Sets home_banking_id in bot_job
                blockStmt.setInt(3, homeUrl.getId()); // 4th ?: Used in AND EXISTS (SELECT 1 FROM home_url WHERE id = ?)

                blockStmt.addBatch(); // Add the current block to the batch
                batchModeEnabled = true;
            }
            if (batchModeEnabled) {
                blockStmt.executeBatch(); // Execute the batch update
            }
            return null;
        } catch (SQLException error) {
            System.out.println(error.getMessage());
            return new ErrorMessage("Error Duplicating Blocks", "Block Insertion Failure", error.getMessage());
        }
    }

    // CREATE NEW HOME BANK
    public Integer getNewHomeBankId() {
        if (idsHomeBankAfter != null && idsHomeBankAfter.size() == 1) {
            return idsHomeBankAfter.get(0); // return the only new ID
        }
        return -1; // invalid or multiple IDs
    }

    public ErrorMessage createNewHomeBanking(DatabaseUserDTO user) {
        String tableName = "home_banking";
        String insertSQL = "INSERT INTO " + tableName
                + " (Name, Url, priority, search_config, options_config, username, password) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
                Statement idStmtBefore = conn.createStatement();
                Statement idStmtAfter = conn.createStatement();
                PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            // Step 1: Get IDs before insertion
            List<Integer> idsBefore = new ArrayList<>();
            try (ResultSet rsBefore = idStmtBefore.executeQuery("SELECT id FROM " + tableName + " ORDER BY id")) {
                while (rsBefore.next()) {
                    idsBefore.add(rsBefore.getInt("id"));
                }
            }

            // Step 2: Prepare values
            String priority = Strings.isNullOrEmpty(user.getPriority()) ? "" : user.getPriority();
            String searchConfig = Strings.isNullOrEmpty(user.getSearchConfig()) ? "" : user.getSearchConfig();
            String optionsConfig = Strings.isNullOrEmpty(user.getOptionsConfig()) ? "" : user.getOptionsConfig();

            // Step 3: Insert new home_banking record
            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getUrl());
            pstmt.setString(3, priority);
            pstmt.setString(4, searchConfig);
            pstmt.setString(5, optionsConfig);
            pstmt.setString(6, user.getUsername());
            pstmt.setString(7, user.getPassword());
            pstmt.addBatch();
            pstmt.executeBatch();

            // Step 4: Get IDs after insertion
            idsHomeBankAfter.clear();
            try (ResultSet rsAfter = idStmtAfter.executeQuery("SELECT id FROM " + tableName + " ORDER BY id")) {
                while (rsAfter.next()) {
                    idsHomeBankAfter.add(rsAfter.getInt("id"));
                }
            }

            // Step 5: Keep only the new IDs
            idsHomeBankAfter.removeAll(idsBefore);

            ARLogger.getInstance(PerformDataBase.class)
                    .info(String.format("HomeBanking inserted successfully. New IDs: %s", idsHomeBankAfter));

            return null; // null means no error

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("saveUserData - Error: %s", error.getMessage()));

            return new ErrorMessage(
                    "HomeBanking Insertion Error", "Error inserting a new HomeBanking record.", error.getMessage());
        }
    }

    // CREATE NEW HOME URL CHILD
    public Integer getNewHomeUrlId() {
        if (idsHomeUrlAfter != null && idsHomeUrlAfter.size() == 1) {
            return idsHomeUrlAfter.get(0); // return the only new ID
        }
        return -1; // invalid or multiple IDs
    }

    public ErrorMessage createHomeUrlChild(int homeBankId, String newUrl) {
        String tableName = "home_url";
        String insertSQL = "INSERT INTO " + tableName + " (url, home_banking_id) VALUES (?, ?)";

        try (Connection conn = getConnection();
                Statement idStmtBefore = conn.createStatement();
                Statement idStmtAfter = conn.createStatement();
                PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            conn.setAutoCommit(false); // Disable auto-commit

            // Step 1: Get IDs before insertion
            List<Integer> idsBefore = new ArrayList<>();
            try (ResultSet rsBefore = idStmtBefore.executeQuery("SELECT id FROM " + tableName + " ORDER BY id")) {
                while (rsBefore.next()) {
                    idsBefore.add(rsBefore.getInt("id"));
                }
            }

            // Step 2: Insert new home_url record
            pstmt.setString(1, newUrl);
            pstmt.setInt(2, homeBankId);
            pstmt.addBatch();
            pstmt.executeBatch();

            // Step 3: Get IDs after insertion
            idsHomeUrlAfter.clear();
            try (ResultSet rsAfter = idStmtAfter.executeQuery("SELECT id FROM " + tableName + " ORDER BY id")) {
                while (rsAfter.next()) {
                    idsHomeUrlAfter.add(rsAfter.getInt("id"));
                }
            }

            // Step 4: Keep only the new IDs
            idsHomeUrlAfter.removeAll(idsBefore);

            ARLogger.getInstance(PerformDataBase.class)
                    .info(String.format("HomeUrl inserted successfully. New IDs: %s", idsHomeUrlAfter));

            conn.commit(); // Commit transaction
            return null; // Success

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("createHomeUrlChild - Error: %s", error.getMessage()));

            return new ErrorMessage("Home URL Insertion Error", "Error inserting a new home URL.", error.getMessage());
        }
    }

    public ErrorMessage createNewHomeUrl(int homeBankId, String newUrl) {
        String tableName = "home_url";
        String checkQuery = "SELECT COUNT(*) FROM " + tableName + " WHERE url = ? AND home_banking_id = ?";
        String insertSQL = "INSERT INTO " + tableName + " (url, home_banking_id) VALUES (?, ?)";

        try (Connection conn = getConnection();
                Statement idStmtBefore = conn.createStatement();
                Statement idStmtAfter = conn.createStatement()) {

            conn.setAutoCommit(false); // Disable auto-commit

            // Step 1: Check if the URL already exists for the given home_banking_id
            try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                checkStmt.setString(1, newUrl);
                checkStmt.setInt(2, homeBankId);

                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        conn.commit(); // Commit even though nothing was inserted
                        return new ErrorMessage(
                                "Error: URL already exists for this organization.",
                                "Duplicate URL Entry",
                                "The URL '" + newUrl + "' is already assigned to this home_banking_id: " + homeBankId);
                    }
                }
            }

            // Step 2: Get IDs before insertion
            List<Integer> idsBefore = new ArrayList<>();
            try (ResultSet rsBefore = idStmtBefore.executeQuery("SELECT id FROM " + tableName + " ORDER BY id")) {
                while (rsBefore.next()) {
                    idsBefore.add(rsBefore.getInt("id"));
                }
            }

            // Step 3: Prepare and execute insert
            try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                pstmt.setString(1, newUrl);
                pstmt.setInt(2, homeBankId);

                pstmt.addBatch();
                pstmt.executeBatch();
            }

            // Step 4: Get IDs after insertion
            idsHomeUrlAfter.clear();
            try (ResultSet rsAfter = idStmtAfter.executeQuery("SELECT id FROM " + tableName + " ORDER BY id")) {
                while (rsAfter.next()) {
                    idsHomeUrlAfter.add(rsAfter.getInt("id"));
                }
            }

            // Step 5: Keep only the new IDs
            idsHomeUrlAfter.removeAll(idsBefore);

            ARLogger.getInstance(PerformDataBase.class)
                    .info(String.format("HomeUrl inserted successfully. New IDs: %s", idsHomeUrlAfter));

            conn.commit(); // Commit transaction
            return null; // Success, no error

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("insertNewHomeUrl - Error: %s", error.getMessage()));

            return new ErrorMessage("Home URL Insertion Error", "Error inserting a new home URL.", error.getMessage());
        }
    }

    public ErrorMessage updateHomeUrl(int homeUrlId, int homeBankId, String newUrl) throws SQLException {
        String checkQuery = "SELECT COUNT(*) FROM home_url WHERE url = ? AND home_banking_id = ? AND id != ?";
        String updateQuery = "UPDATE home_url SET url = ? WHERE id = ? AND home_banking_id = ?";

        try (Connection conn = getConnection()) {
            // Check if the URL already exists for this org but with a different ID
            try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                checkStmt.setString(1, newUrl);
                checkStmt.setInt(2, homeBankId);
                checkStmt.setInt(3, homeUrlId);

                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return new ErrorMessage(
                                "Error: URL already exists for this organization.",
                                "Duplicate URL Entry",
                                "The URL '" + newUrl + "' is already assigned to this home_banking_id: " + homeBankId);
                    }
                }
            }

            // Proceed with update
            try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                updateStmt.setString(1, newUrl);
                updateStmt.setInt(2, homeUrlId);
                updateStmt.setInt(3, homeBankId);

                int updated = updateStmt.executeUpdate();
                if (updated == 0) {
                    return new ErrorMessage(
                            "No URL updated",
                            "Update Failed",
                            "No record was found with ID " + homeUrlId + " for home_banking_id " + homeBankId);
                }

                return null;
            }

        } catch (SQLException error) {
            System.out.println(error.getMessage());
            return new ErrorMessage("Error updating URL", "Org URL Update Failure", error.getMessage());
        }
    }

    public ErrorMessage deleteHomeUrl(int homeUrlId) throws SQLException {
        String usageCheckQuery = "SELECT COUNT(*) AS usage_count FROM bot_job WHERE home_url_id = ?";
        String deleteQuery = "DELETE FROM home_url WHERE id = ?";

        try (Connection conn = getConnection()) {
            // Step 1: Check usage in bot_job
            try (PreparedStatement checkStmt = conn.prepareStatement(usageCheckQuery)) {
                checkStmt.setInt(1, homeUrlId);

                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        int usageCount = rs.getInt("usage_count");
                        if (usageCount > 0) {
                            return new ErrorMessage(
                                    "Cannot delete URL",
                                    "URL in Use",
                                    "This URL is used in " + usageCount
                                            + " bot_job(s). Please detach it before deletion.");
                        }
                    }
                }
            }

            // Step 2: Proceed with deletion
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteQuery)) {
                deleteStmt.setInt(1, homeUrlId);
                int rows = deleteStmt.executeUpdate();

                if (rows == 0) {
                    return new ErrorMessage("URL not found", "Deletion Failure", "No matching URL ID found.");
                }

                return null; // Success
            }

        } catch (SQLException e) {
            return new ErrorMessage("Error deleting URL", "Database Error", e.getMessage());
        }
    }

    public int countUsageOfHomeUrlId(int homeUrlId) {
        String countQuery = "SELECT COUNT(*) AS usage_count FROM bot_job WHERE home_url_id = ?";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(countQuery)) {

            stmt.setInt(1, homeUrlId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("usage_count");
                }
            }

        } catch (SQLException e) {
            System.out.println("Error counting usage of Home URL ID " + homeUrlId + ": " + e.getMessage());
        }

        return 0; // Return 0 if query fails or no result
    }

    public void loadAllDataUsers() {
        performLists.getListDatabaseUsers().clear();
        String selectSQL =
                """
SELECT
  bank.ID,
  bank.Name,
  hu.url,
  bank.priority,
  COUNT(bot.ID) AS Jobs,
  bank.search_config AS searchConfig,
  bank.options_config AS optionsConfig,
  bank.username,
  bank.password
FROM home_banking bank
LEFT JOIN bot_job bot ON bot.home_banking_id = bank.id
LEFT JOIN home_url hu ON hu.home_banking_id = bank.id
GROUP BY
  bank.ID,
  bank.Name,
  hu.url,
  bank.priority,
  bank.search_config,
  bank.options_config,
  bank.username,
  bank.password
  order by bank.ID;
                        """;

        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(selectSQL)) {
            while (rs.next()) {
                String id = rs.getString("ID");
                String jobs = rs.getString("Jobs");
                String name = rs.getString("Name");
                String url = rs.getString("Url");
                String priority = rs.getString("Priority");
                String searchConfig = rs.getString("searchConfig");
                String optionsConfig = rs.getString("optionsConfig");
                String username = rs.getString("username");
                String password = rs.getString("password");

                // Create StringBuilder and split using "£"
                StringBuilder prioritySb = new StringBuilder();
                StringBuilder searchConfigSb = new StringBuilder();
                StringBuilder optionsConfigSb = new StringBuilder();

                for (String part : priority.split("£")) {
                    prioritySb.append(part).append("\n"); // Replacing "£" back with newline
                }

                for (String part : searchConfig.split("£")) {
                    searchConfigSb.append(part).append("\n");
                }

                for (String part : optionsConfig.split("£")) {
                    optionsConfigSb.append(part).append("\n");
                }

                // Remove the last extra newline if needed
                if (prioritySb.length() > 0) prioritySb.setLength(prioritySb.length() - 1);
                if (searchConfigSb.length() > 0) searchConfigSb.setLength(searchConfigSb.length() - 1);
                if (optionsConfigSb.length() > 0) optionsConfigSb.setLength(optionsConfigSb.length() - 1);

                performLists
                        .getListDatabaseUsers()
                        .add(new DatabaseUserDTO(
                                id,
                                jobs,
                                name,
                                url,
                                prioritySb.toString(),
                                searchConfigSb.toString(),
                                optionsConfigSb.toString(),
                                username,
                                password));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public ErrorMessage loadHomeUrls(Integer homeBankingId) {
        performLists.getListHomeUrl().clear();

        StringBuilder sql = new StringBuilder(
                "SELECT hu.id AS id, hu.url AS url, hu.home_banking_id AS home_banking_id, hb.name AS org_name "
                        + "FROM home_url hu "
                        + "LEFT JOIN home_banking hb ON hu.home_banking_id = hb.id ");

        if (homeBankingId != null && homeBankingId > 0) {
            sql.append("WHERE hu.home_banking_id = ? ");
        }

        sql.append("ORDER BY hb.name , hu.id");

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql.toString())) {
            if (homeBankingId != null && homeBankingId > 0) {
                pstmt.setInt(1, homeBankingId);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Integer id = rs.getInt("id");
                    String url = rs.getString("url");
                    Integer hbId = rs.getInt("home_banking_id");
                    String orgName = rs.getString("org_name");

                    performLists.getListHomeUrl().add(new HomeUrlDTO(id, url, hbId, orgName));
                }
            }

            return null; // success → no error

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to load Home URLs");
            return new ErrorMessage("Failed to load Home URLs", "Home URL Load Failure", error.getMessage());
        }
    }

    public void selectHomeBankinOneRow() {
        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
        String accessDbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_ACCESS + CONNECTION_PARAMETERS;
        ARLogger.getInstance(PerformDataBase.class).info("ACCESS connection URL: " + accessDbUrl);

        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        // String userData = userDB + " - " + userPwd;

        ARLogger.getInstance(PerformDataBase.class).info("POSTGRES connection URL: " + postgresDbUrl);
        // ARLogger.getInstance(PerformDataBase.class).info("User Details: " + userData);

        final int BATCH_SIZE = 100;

        try (Connection accessConn = DriverManager.getConnection(accessDbUrl);
                Connection postgresConn = DriverManager.getConnection(postgresDbUrl, userDB, userPwd);
                Statement postgreStmt = postgresConn.createStatement(); ) {
            accessConn.setAutoCommit(false); // Use manual commit for batch performance

            String selectPostgresSQL =
                    "SELECT ID, url, name, priority, search_config, options_config, cookies, driver_session, username, password FROM home_banking order by id";
            ResultSet rsHomeBank = postgreStmt.executeQuery(selectPostgresSQL);

            String checkSQL = "SELECT id FROM home_banking WHERE url = ?";
            String insertSQL =
                    "INSERT INTO home_banking (url, name, priority, search_config, options_config, cookies, driver_session, username, password, id) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            homeBankMap.clear();
            try (PreparedStatement checkStmt = accessConn.prepareStatement(checkSQL);
                    PreparedStatement insertStmt = accessConn.prepareStatement(insertSQL)) {
                int count = 0;

                while (rsHomeBank.next()) {
                    int id = rsHomeBank.getInt("id");
                    String url = rsHomeBank.getString("url");

                    if (url != null && !url.trim().isEmpty()) {
                        homeBankMap.put(id, -1);
                    }

                    // Check for existence
                    checkStmt.setString(1, url);
                    ResultSet checkResult = checkStmt.executeQuery();

                    if (!checkResult.next()) {
                        // Add to batch
                        insertStmt.setInt(10, id);

                        insertStmt.setString(1, url);
                        insertStmt.setString(2, rsHomeBank.getString("name"));
                        insertStmt.setString(3, rsHomeBank.getString("priority"));
                        insertStmt.setString(4, rsHomeBank.getString("search_config"));
                        insertStmt.setString(5, rsHomeBank.getString("options_config"));
                        insertStmt.setString(6, rsHomeBank.getString("cookies"));
                        insertStmt.setString(7, rsHomeBank.getString("driver_session"));
                        insertStmt.setString(8, rsHomeBank.getString("username"));
                        insertStmt.setString(9, rsHomeBank.getString("password"));

                        insertStmt.addBatch();

                        count++;

                        if (count % BATCH_SIZE == 0) {
                            insertStmt.executeBatch();
                            accessConn.commit();
                            System.out.println("Inserted batch of " + BATCH_SIZE);
                        }
                    } else {
                        System.out.println("Skipped (exists): " + url);
                    }
                }

                // Final batch
                if (count % BATCH_SIZE != 0) {
                    insertStmt.executeBatch();
                    accessConn.commit();
                    System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                }
            }

            System.out.println("Sync completed.");
        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class).severe("Error export HomeBanking");
        }
    }

    public ErrorMessage dropPostGresSequences() {
        // Build the SQL update statement

        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        try (Connection postgresConn = DriverManager.getConnection(postgresDbUrl, userDB, userPwd)) {

            try (Statement stmt = postgresConn.createStatement()) {
                int rowsAffected = 0;

                rowsAffected += stmt.executeUpdate("DELETE  FROM \"home_url\";");
                rowsAffected += stmt.executeUpdate("DELETE FROM \"home_banking\";");

                rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"home_url_id_seq\";");
                rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"home_banking_id_seq\";");

                // Recreate sequences
                rowsAffected += stmt.executeUpdate("CREATE SEQUENCE \"home_url_id_seq\" START WITH 1 INCREMENT BY 1;");
                rowsAffected +=
                        stmt.executeUpdate("CREATE SEQUENCE \"home_banking_id_seq\" START WITH 1 INCREMENT BY 1;");

                // Uncomment if needed
                //            rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"instruction_id_seq\";");
                //            rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"block_id_seq\";");
                //            rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"bot_job_id_seq\";");
                //            rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"component_block_id_seq\";");
                //            rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS
                // \"component_instruction_id_seq\";");
                //            rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS
                // \"component_reference_id_seq\";");
                //            rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS
                // \"component_variable_id_seq\";");
                //            rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"reference_id_seq\";");
                //            rowsAffected += stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"variable_id_seq\";");

                if (rowsAffected > 0) {
                    ARLogger.getInstance(PerformDataBase.class)
                            .warning(String.format("Migration DB Scripts - RowsUpdated - %s", rowsAffected));
                } else {
                    ARLogger.getInstance(PerformDataBase.class).info("Migration DB Scripts - No Rows were updated");
                }
                return null;

            } catch (SQLException error) {
                ARLogger.getInstance(PerformDataBase.class)
                        .warning("Migration DB Scripts - Error: " + error.getMessage());
                return new ErrorMessage(
                        "Error Drop Tables Migration 2.7f", "Error dropping OLD objects", error.getMessage());
            }

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to dropPostGresSequences.");
            return new ErrorMessage("Connection Error", "Could not connect to Postgres DB", error.getMessage());
        }
    }

    private void insertOrNull(PreparedStatement stmt, int parameterIndex, ResultSet rs, String columnName)
            throws SQLException {
        Object value = rs.getObject(columnName);
        if (value == null) {
            stmt.setNull(parameterIndex, Types.INTEGER);
        } else {
            stmt.setInt(parameterIndex, rs.getInt(columnName));
        }
    }

    // CREATE SAVED COMPONENTS
    public ErrorMessage createCompBlock(BlockDetailsDTO blockDetailsDTO) {

        final int BATCH_SIZE = 100;
        String checkExistsSQL =
                "SELECT id, block_order_number, name, description, type_id, export_file, active, wait, bot_job_id "
                        + "FROM block WHERE bot_job_id = ? AND id = ? ORDER BY id";

        String selectComponentIdsSQL = "SELECT id FROM component_block WHERE home_banking_id = "
                + blockDetailsDTO.getHomeBankingId() + " ORDER BY id";

        try (Connection conn = getConnection();
                PreparedStatement selectStmt = conn.prepareStatement(checkExistsSQL);
                Statement idStmtBefore = conn.createStatement();
                Statement idStmtAfter = conn.createStatement()) {

            conn.setAutoCommit(false);

            // Step 1: get current component_block ids before insert
            List<Integer> blockIdsBefore = new ArrayList<>();
            try (ResultSet rsIdsBefore = idStmtBefore.executeQuery(selectComponentIdsSQL)) {
                while (rsIdsBefore.next()) {
                    blockIdsBefore.add(rsIdsBefore.getInt("id"));
                }
            }

            // Step 2: prepare and execute insert
            selectStmt.setInt(1, blockDetailsDTO.getBotJobId());
            selectStmt.setInt(2, blockDetailsDTO.getBlockId());

            try (ResultSet rs = selectStmt.executeQuery()) {

                String insertSQL = "INSERT INTO component_block "
                        + "(block_order_number, name, description, type_id, export_file, active, wait, home_banking_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

                blockMap.clear();

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {

                    int count = 0;

                    while (rs.next()) {
                        int id = rs.getInt("id");
                        int blockOrderNumber = rs.getInt("block_order_number");
                        //                        String name = rs.getString("name");
                        //                        String description = rs.getString("description");
                        Integer typeId = rs.getObject("type_id") != null ? rs.getInt("type_id") : null;
                        String exportFile = rs.getString("export_file");
                        int active = rs.getInt("active");
                        Integer wait = rs.getObject("wait") != null ? rs.getInt("wait") : null;

                        Integer newHomeBankId = blockDetailsDTO.getHomeBankingId();
                        if (newHomeBankId == null) {
                            System.out.println("Skipped component_block with unknown home_banking_id");
                            continue;
                        }

                        blockMap.put(id, -1);

                        insertStmt.setInt(1, blockOrderNumber);
                        insertStmt.setString(2, blockDetailsDTO.getBlockName());
                        insertStmt.setString(3, blockDetailsDTO.getBlockDescription());
                        if (typeId != null) {
                            insertStmt.setInt(4, typeId);
                        } else {
                            insertStmt.setNull(4, Types.INTEGER);
                        }
                        insertStmt.setString(5, exportFile);
                        insertStmt.setInt(6, active);
                        if (wait != null) {
                            insertStmt.setInt(7, wait);
                        } else {
                            insertStmt.setNull(7, Types.INTEGER);
                        }
                        insertStmt.setInt(8, newHomeBankId);

                        insertStmt.addBatch();
                        count++;

                        if (count % BATCH_SIZE == 0) {
                            insertStmt.executeBatch();
                            conn.commit();
                            System.out.println("Inserted batch of " + BATCH_SIZE);
                        }
                    }

                    // Final batch
                    if (count % BATCH_SIZE != 0) {
                        insertStmt.executeBatch();
                        conn.commit();
                        System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                    }
                }
            }

            // Step 3: get component_block ids after insert
            List<Integer> blockIdsAfter = new ArrayList<>();
            try (ResultSet rsIdsAfter = idStmtAfter.executeQuery(selectComponentIdsSQL)) {
                while (rsIdsAfter.next()) {
                    blockIdsAfter.add(rsIdsAfter.getInt("id"));
                }
            }

            // Step 4: compute new IDs
            List<Integer> newComponentIds = new ArrayList<>(blockIdsAfter);
            newComponentIds.removeAll(blockIdsBefore);

            // You can now use `newComponentIds` as needed
            System.out.println("Newly inserted component_block IDs: " + newComponentIds);

            // Step 5: update blockMap with new IDs
            List<Integer> keys = new ArrayList<>(blockMap.keySet());

            if (keys.size() != newComponentIds.size()) {
                System.err.println(
                        "Mismatch in size: expected " + keys.size() + " new IDs, but got " + newComponentIds.size());
            } else {
                for (int i = 0; i < keys.size(); i++) {
                    Integer oldId = keys.get(i);
                    Integer newId = newComponentIds.get(i);
                    blockMap.put(oldId, newId);
                }
            }

            return null;

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to create saved component_block");
            return new ErrorMessage(
                    "Failed to create saved component_block", "component_block Insertion Failure", error.getMessage());
        }
    }

    public ErrorMessage createCompInstructions(BlockDetailsDTO blockDetailsDTO) {

        final int BATCH_SIZE = 100;

        String checkExistsSQL = "SELECT * FROM instruction WHERE bot_job_id = ? AND block_id = ? order by id";

        String selectComponentIdsSQL = "SELECT id FROM component_instruction WHERE home_banking_id = "
                + blockDetailsDTO.getHomeBankingId() + " ORDER BY id";

        try (Connection conn = getConnection();
                PreparedStatement selectStmt = conn.prepareStatement(checkExistsSQL);
                Statement idStmtBefore = conn.createStatement();
                Statement idStmtAfter = conn.createStatement()) {

            conn.setAutoCommit(false);

            // Step 1: get current component_block ids before insert
            List<Integer> blockIdsBefore = new ArrayList<>();
            try (ResultSet rsIdsBefore = idStmtBefore.executeQuery(selectComponentIdsSQL)) {
                while (rsIdsBefore.next()) {
                    blockIdsBefore.add(rsIdsBefore.getInt("id"));
                }
            }

            // Step 2: prepare and execute insert
            selectStmt.setInt(1, blockDetailsDTO.getBotJobId());
            selectStmt.setInt(2, blockDetailsDTO.getBlockId());

            try (ResultSet rsInstruction = selectStmt.executeQuery()) {

                String insertSQL = "INSERT INTO component_instruction ("
                        + "instruction_order_number, actions, name, xpath, coordinates, force_coordinates, iframe_xpath, tag_name, shadow_host, shadow_root, css_selector, description, operation, optional, block_marked, default_value, action_custom_max_wait_sec, on_hold_seconds, codified, export_to_abr, active, block_id, variable_id, parent_id, home_banking_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                instructionMap.clear();
                instrVariablesMap.clear();

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {

                    int count = 0;

                    while (rsInstruction.next()) {
                        int id = rsInstruction.getInt("id");
                        Integer newHomeBankId = blockDetailsDTO.getHomeBankingId();
                        if (newHomeBankId == null) {
                            System.out.println("Skipped component_instruction with unknown home_banking_id");
                            continue;
                        }

                        int oldBlockId = rsInstruction.getInt("block_id");
                        Integer newBlockId = blockMap.get(oldBlockId);
                        if (newBlockId == null) {
                            System.out.println("Skipped component_instruction with unknown block_id: " + oldBlockId);
                            continue;
                        }

                        instructionMap.put(id, -1);

                        insertStmt.setInt(1, rsInstruction.getInt("instruction_order_number"));
                        insertStmt.setString(2, rsInstruction.getString("actions"));
                        insertStmt.setString(3, rsInstruction.getString("name"));
                        insertStmt.setString(4, rsInstruction.getString("xpath"));
                        insertStmt.setString(5, rsInstruction.getString("coordinates"));

                        insertOrNull(insertStmt, 6, rsInstruction, "force_coordinates");
                        insertStmt.setString(7, rsInstruction.getString("iframe_xpath"));
                        insertStmt.setString(8, rsInstruction.getString("tag_name"));
                        insertStmt.setString(9, rsInstruction.getString("shadow_host"));
                        insertStmt.setString(10, rsInstruction.getString("shadow_root"));
                        insertStmt.setString(11, rsInstruction.getString("css_selector"));
                        insertStmt.setString(12, rsInstruction.getString("description"));
                        insertStmt.setString(13, rsInstruction.getString("operation"));

                        insertOrNull(insertStmt, 14, rsInstruction, "optional");
                        insertOrNull(insertStmt, 15, rsInstruction, "block_marked");
                        insertStmt.setString(16, rsInstruction.getString("default_value"));
                        insertOrNull(insertStmt, 17, rsInstruction, "action_custom_max_wait_sec");
                        insertOrNull(insertStmt, 18, rsInstruction, "on_hold_seconds");
                        insertOrNull(insertStmt, 19, rsInstruction, "codified");
                        insertOrNull(insertStmt, 20, rsInstruction, "export_to_abr");

                        insertStmt.setInt(21, rsInstruction.getInt("active"));
                        insertStmt.setInt(22, newBlockId);

                        // variable_id + tracking
                        Integer variableId = rsInstruction.getInt("variable_id");
                        if (rsInstruction.wasNull()) {
                            variableId = null;
                        }

                        if (variableId != null) {
                            instrVariablesMap.put(id, variableId);
                        }
                        insertStmt.setNull(23, Types.INTEGER); // TO BE SOLVED LATER

                        insertOrNull(insertStmt, 24, rsInstruction, "parent_id");

                        insertStmt.setInt(25, newHomeBankId);

                        insertStmt.addBatch();
                        count++;
                        if (count % BATCH_SIZE == 0) {
                            insertStmt.executeBatch();
                            conn.commit();
                            System.out.println("Inserted batch of " + BATCH_SIZE);
                        }
                    }

                    // Final batch
                    if (count % BATCH_SIZE != 0) {
                        insertStmt.executeBatch();
                        conn.commit();
                        System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                    }
                }
            }

            // Step 3: get component_block ids after insert
            List<Integer> blockIdsAfter = new ArrayList<>();
            try (ResultSet rsIdsAfter = idStmtAfter.executeQuery(selectComponentIdsSQL)) {
                while (rsIdsAfter.next()) {
                    blockIdsAfter.add(rsIdsAfter.getInt("id"));
                }
            }

            // Step 4: compute new IDs
            List<Integer> newComponentIds = new ArrayList<>(blockIdsAfter);
            newComponentIds.removeAll(blockIdsBefore);

            // You can now use `newComponentIds` as needed
            System.out.println("Newly inserted component_instruction IDs: " + newComponentIds);

            // Step 5: update blockMap with new IDs
            List<Integer> keys = new ArrayList<>(instructionMap.keySet());

            if (keys.size() != newComponentIds.size()) {
                System.err.println(
                        "Mismatch in size: expected " + keys.size() + " new IDs, but got " + newComponentIds.size());
            } else {
                for (int i = 0; i < keys.size(); i++) {
                    Integer oldId = keys.get(i);
                    Integer newId = newComponentIds.get(i);
                    instructionMap.put(oldId, newId);
                }
            }

            return null;

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe("Failed to create saved component_instruction: " + error.getMessage());
            return new ErrorMessage(
                    "Failed to create saved component_instruction",
                    "component_instruction Insertion Failure",
                    error.getMessage());
        }
    }

    public ErrorMessage createCompVariables(BlockDetailsDTO blockDetailsDTO) {

        final int BATCH_SIZE = 100;

        String idsInstruction =
                instructionMap.keySet().stream().map(String::valueOf).collect(Collectors.joining(","));

        String checkExistsSQL = "SELECT * FROM variable " + "WHERE  bot_job_id = ? AND instruction_id IN ("
                + idsInstruction + ") ORDER BY id";

        String selectComponentIdsSQL = "SELECT id FROM component_variable WHERE home_banking_id = "
                + blockDetailsDTO.getHomeBankingId() + " ORDER BY id";

        try (Connection conn = getConnection();
                PreparedStatement selectStmt = conn.prepareStatement(checkExistsSQL);
                Statement idStmtBefore = conn.createStatement();
                Statement idStmtAfter = conn.createStatement()) {

            conn.setAutoCommit(false);

            // Step 1: get current component_block ids before insert
            List<Integer> blockIdsBefore = new ArrayList<>();
            try (ResultSet rsIdsBefore = idStmtBefore.executeQuery(selectComponentIdsSQL)) {
                while (rsIdsBefore.next()) {
                    blockIdsBefore.add(rsIdsBefore.getInt("id"));
                }
            }

            // Step 2: prepare and execute insert
            selectStmt.setInt(1, blockDetailsDTO.getBotJobId());

            try (ResultSet rsVariable = selectStmt.executeQuery()) {

                String insertSQL =
                        "INSERT INTO component_variable (type, name, value, local_format, delimiter, instruction_id, home_banking_id) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

                variableMap.clear();

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {

                    int count = 0;

                    while (rsVariable.next()) {
                        int id = rsVariable.getInt("id");

                        Integer newHomeBankId = blockDetailsDTO.getHomeBankingId();
                        if (newHomeBankId == null) {
                            System.out.println("Skipped component_variable with unknown home_banking_id");
                            continue;
                        }

                        Integer instructionId = rsVariable.getObject("instruction_id") != null
                                ? rsVariable.getInt("instruction_id")
                                : null;

                        Integer newInstructionId = null;

                        if (instructionId != null) {
                            newInstructionId = instructionMap.get(instructionId);
                            if (newInstructionId == null) {
                                System.out.println(
                                        "Skipped component_variable with unknown instruction_id: " + instructionId);
                                continue;
                            }
                        }

                        variableMap.put(id, -1);

                        insertStmt.setString(1, rsVariable.getString("type"));
                        insertStmt.setString(2, rsVariable.getString("name"));
                        insertStmt.setString(3, rsVariable.getString("value"));
                        insertStmt.setString(4, rsVariable.getString("local_format"));
                        insertStmt.setString(5, rsVariable.getString("delimiter"));

                        if (newInstructionId != null) {
                            insertStmt.setInt(6, newInstructionId);
                        } else {
                            insertStmt.setNull(6, Types.INTEGER);
                        }

                        insertStmt.setInt(7, newHomeBankId);

                        insertStmt.addBatch();
                        count++;

                        if (count % BATCH_SIZE == 0) {
                            insertStmt.executeBatch();
                            conn.commit();
                            System.out.println("Inserted batch of " + BATCH_SIZE);
                        }
                    }

                    // Final batch
                    if (count % BATCH_SIZE != 0) {
                        insertStmt.executeBatch();
                        conn.commit();
                        System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                    }
                }
            }

            // Step 3: get component_block ids after insert
            List<Integer> idsAfter = new ArrayList<>();
            try (ResultSet rsIdsAfter = idStmtAfter.executeQuery(selectComponentIdsSQL)) {
                while (rsIdsAfter.next()) {
                    idsAfter.add(rsIdsAfter.getInt("id"));
                }
            }

            // Step 4: compute new IDs
            List<Integer> newComponentIds = new ArrayList<>(idsAfter);
            newComponentIds.removeAll(blockIdsBefore);

            // You can now use `newComponentIds` as needed
            System.out.println("Newly inserted component_variable IDs: " + newComponentIds);

            // Step 5: update blockMap with new IDs
            List<Integer> keys = new ArrayList<>(variableMap.keySet());

            if (keys.size() != newComponentIds.size()) {
                System.err.println(
                        "Mismatch in size: expected " + keys.size() + " new IDs, but got " + newComponentIds.size());
            } else {
                for (int i = 0; i < keys.size(); i++) {
                    Integer oldId = keys.get(i);
                    Integer newId = newComponentIds.get(i);
                    variableMap.put(oldId, newId);
                }
            }

            return null;

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to create saved component_variable");
            return new ErrorMessage(
                    "Failed to create saved component_variable",
                    "component_variable Insertion Failure",
                    error.getMessage());
        }
    }

    public ErrorMessage createUpdateCompInstruction(BlockDetailsDTO blockDetailsDTO) {
        final int BATCH_SIZE = 100;

        instrNewInverted.clear();

        for (Map.Entry<Integer, Integer> entry : instructionMap.entrySet()) {
            instrNewInverted.put(entry.getValue(), entry.getKey());
        }

        try (Connection conn = getConnection();
                Statement postgresStmt = conn.createStatement()) {
            conn.setAutoCommit(false);

            String idsInstruction =
                    instructionMap.values().stream().map(String::valueOf).collect(Collectors.joining(","));

            String idsBlock = blockMap.values().stream().map(String::valueOf).collect(Collectors.joining(","));

            String selectAccessSQL =
                    "SELECT id, name, parent_id, parent_block_id, variable_id " + "FROM component_instruction "
                            + "WHERE (parent_id IS NOT NULL OR variable_id IS NOT NULL) "
                            + "AND home_banking_id = "
                            + blockDetailsDTO.getHomeBankingId() + " AND id IN ("
                            + idsInstruction + ") " + "AND block_id IN ("
                            + idsBlock + ") " + "ORDER BY id";

            try (ResultSet rsInstruction = postgresStmt.executeQuery(selectAccessSQL)) {

                String updateSQL =
                        "UPDATE component_instruction SET variable_id = ?, parent_block_id = ?, parent_id = ? WHERE id = ?";

                try (PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {
                    int count = 0;

                    while (rsInstruction.next()) {
                        int id = rsInstruction.getInt("id");
                        String name = rsInstruction.getString("name");

                        // ---- variable_id ----
                        Integer originalOldID = instrNewInverted.get(id);
                        Integer originalVarId = null;

                        if (originalOldID != null) {
                            originalVarId = instrVariablesMap.get(originalOldID);
                        }

                        Integer newVariableId = null;
                        if (originalVarId != null) {
                            newVariableId = variableMap.get(originalVarId);
                        }

                        if (newVariableId == null) {
                            System.out.println("Skipped variable_id column with unknown variable_id: " + newVariableId);
                            updateStmt.setNull(1, Types.INTEGER);
                        } else {
                            updateStmt.setInt(1, newVariableId);
                        }

                        // Handle parent_id or parent_block_id based on name
                        if ("GOTO".equalsIgnoreCase(name) || "EXCEL GOTO".equalsIgnoreCase(name)) {
                            int parentBlockId = rsInstruction.getInt("parent_block_id");
                            if (rsInstruction.wasNull() || !blockMap.containsKey(parentBlockId)) {
                                updateStmt.setNull(2, Types.INTEGER);
                            } else {
                                updateStmt.setInt(2, blockMap.get(parentBlockId));
                            }
                            updateStmt.setNull(3, Types.INTEGER); // Skip parent_id
                        } else {
                            int parentInstructionId = rsInstruction.getInt("parent_id");
                            if (rsInstruction.wasNull() || !instructionMap.containsKey(parentInstructionId)) {
                                updateStmt.setNull(3, Types.INTEGER);
                            } else {
                                updateStmt.setInt(3, instructionMap.get(parentInstructionId));
                            }
                            updateStmt.setNull(2, Types.INTEGER); // Skip parent_block_id
                        }

                        updateStmt.setInt(4, id); // WHERE clause: name = ?

                        updateStmt.addBatch();
                        count++;

                        if (count % BATCH_SIZE == 0) {
                            updateStmt.executeBatch();
                            conn.commit();
                            System.out.println("Updated batch of " + BATCH_SIZE);
                        }
                    }

                    if (count % BATCH_SIZE != 0) {
                        updateStmt.executeBatch();
                        conn.commit();
                        System.out.println("Updated final batch of " + (count % BATCH_SIZE));
                    }

                    ARLogger.getInstance(PerformDataBase.class).info("Updated component_instruction records: " + count);
                }
            }

            return null;

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to update component_instruction");
            return new ErrorMessage(
                    "Failed to update component_instruction",
                    "component_instruction Update Failure",
                    error.getMessage());
        }
    }

    public ErrorMessage createCompReferences(BlockDetailsDTO blockDetailsDTO) {

        final int BATCH_SIZE = 100;

        String idsInstruction =
                instructionMap.keySet().stream().map(String::valueOf).collect(Collectors.joining(","));

        String checkExistsSQL = "SELECT * FROM reference " + "WHERE  bot_job_id = ? AND instruction_id IN ("
                + idsInstruction + ") ORDER BY id";

        String selectComponentIdsSQL = "SELECT id FROM component_reference WHERE home_banking_id = "
                + blockDetailsDTO.getHomeBankingId() + " ORDER BY id";

        try (Connection conn = getConnection();
                PreparedStatement selectStmt = conn.prepareStatement(checkExistsSQL);
                Statement idStmtBefore = conn.createStatement();
                Statement idStmtAfter = conn.createStatement()) {

            conn.setAutoCommit(false);

            // Step 1: get current component_block ids before insert
            List<Integer> blockIdsBefore = new ArrayList<>();
            try (ResultSet rsIdsBefore = idStmtBefore.executeQuery(selectComponentIdsSQL)) {
                while (rsIdsBefore.next()) {
                    blockIdsBefore.add(rsIdsBefore.getInt("id"));
                }
            }

            // Step 2: prepare and execute insert
            selectStmt.setInt(1, blockDetailsDTO.getBotJobId());

            try (ResultSet rsReference = selectStmt.executeQuery()) {

                String insertSQL =
                        "INSERT INTO component_reference ( reference_type, value, instruction_id, home_banking_id) VALUES (?, ?, ?, ?)";

                referenceMap.clear();

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {

                    int count = 0;

                    while (rsReference.next()) {
                        int id = rsReference.getInt("id");

                        int oldInstructionId = rsReference.getInt("instruction_id");
                        Integer newInstructionId = instructionMap.get(oldInstructionId);
                        if (newInstructionId == null) {
                            System.out.println(
                                    "Skipped component_reference with unknown instruction_id: " + oldInstructionId);
                            continue;
                        }

                        Integer newHomeBankId = blockDetailsDTO.getHomeBankingId();
                        if (newHomeBankId == null) {
                            System.out.println(
                                    "Skipped component_reference with unknown home_banking_id: " + newHomeBankId);
                            continue;
                        }

                        referenceMap.put(id, -1);

                        insertStmt.setString(1, rsReference.getString("reference_type"));
                        insertStmt.setString(2, rsReference.getString("value"));
                        insertStmt.setInt(3, newInstructionId);

                        if (newHomeBankId != null) {
                            insertStmt.setInt(4, newHomeBankId);
                        } else {
                            insertStmt.setNull(4, Types.INTEGER);
                        }

                        insertStmt.addBatch();
                        count++;

                        if (count % BATCH_SIZE == 0) {
                            insertStmt.executeBatch();
                            conn.commit();
                            System.out.println("Inserted batch of " + BATCH_SIZE);
                        }
                    }

                    // Final batch
                    if (count % BATCH_SIZE != 0) {
                        insertStmt.executeBatch();
                        conn.commit();
                        System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                    }
                }
            }

            // Step 3: get component_block ids after insert
            List<Integer> idsAfter = new ArrayList<>();
            try (ResultSet rsIdsAfter = idStmtAfter.executeQuery(selectComponentIdsSQL)) {
                while (rsIdsAfter.next()) {
                    idsAfter.add(rsIdsAfter.getInt("id"));
                }
            }

            // Step 4: compute new IDs
            List<Integer> newComponentIds = new ArrayList<>(idsAfter);
            newComponentIds.removeAll(blockIdsBefore);

            // You can now use `newComponentIds` as needed
            System.out.println("Newly inserted component_reference IDs: " + newComponentIds);

            // Step 5: update blockMap with new IDs
            List<Integer> keys = new ArrayList<>(referenceMap.keySet());

            if (keys.size() != newComponentIds.size()) {
                System.err.println(
                        "Mismatch in size: expected " + keys.size() + " new IDs, but got " + newComponentIds.size());
            } else {
                for (int i = 0; i < keys.size(); i++) {
                    Integer oldId = keys.get(i);
                    Integer newId = newComponentIds.get(i);
                    referenceMap.put(oldId, newId);
                }
            }

            return null;

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to create saved component_reference");
            return new ErrorMessage(
                    "Failed to create saved component_reference",
                    "component_reference Insertion Failure",
                    error.getMessage());
        }
    }

    // INJECT BACK SAVED COMPONENTS
    public ErrorMessage createInjectBlock(BlockDetailsDTO blockDetailsDTO) {

        final int BATCH_SIZE = 100;
        String checkExistsSQL =
                "SELECT id, block_order_number, name, description, type_id, export_file, active, wait, home_banking_id "
                        + "FROM component_block WHERE home_banking_id = ? AND id = ? ORDER BY id";

        String selectComponentIdsSQL =
                "SELECT id FROM block WHERE bot_job_id = " + blockDetailsDTO.getBotJobId() + " ORDER BY id";

        try (Connection conn = getConnection();
                PreparedStatement selectStmt = conn.prepareStatement(checkExistsSQL);
                Statement idStmtBefore = conn.createStatement();
                Statement idStmtAfter = conn.createStatement()) {

            conn.setAutoCommit(false);

            // Step 1: get current block ids before insert
            List<Integer> blockIdsBefore = new ArrayList<>();
            try (ResultSet rsIdsBefore = idStmtBefore.executeQuery(selectComponentIdsSQL)) {
                while (rsIdsBefore.next()) {
                    blockIdsBefore.add(rsIdsBefore.getInt("id"));
                }
            }

            // Step 2: prepare and execute insert
            selectStmt.setInt(1, blockDetailsDTO.getHomeBankingId());
            selectStmt.setInt(2, blockDetailsDTO.getBlockId());

            try (ResultSet rs = selectStmt.executeQuery()) {

                String insertSQL = "INSERT INTO block "
                        + "(block_order_number, name, description, type_id, export_file, active, wait, bot_job_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

                blockMap.clear();

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {

                    int count = 0;

                    while (rs.next()) {
                        int id = rs.getInt("id");
                        int blockOrderNumber = rs.getInt("block_order_number");
                        Integer typeId = rs.getObject("type_id") != null ? rs.getInt("type_id") : null;
                        String exportFile = rs.getString("export_file");
                        int active = rs.getInt("active");
                        Integer wait = rs.getObject("wait") != null ? rs.getInt("wait") : null;

                        Integer newBotJobId = blockDetailsDTO.getBotJobId();
                        if (newBotJobId == null) {
                            System.out.println("Skipped block with unknown bot_job_id");
                            continue;
                        }

                        blockMap.put(id, -1);

                        insertStmt.setInt(1, blockOrderNumber);
                        insertStmt.setString(2, blockDetailsDTO.getBlockName());
                        insertStmt.setString(3, blockDetailsDTO.getBlockDescription());
                        if (typeId != null) {
                            insertStmt.setInt(4, typeId);
                        } else {
                            insertStmt.setNull(4, Types.INTEGER);
                        }
                        insertStmt.setString(5, exportFile);
                        insertStmt.setInt(6, active);
                        if (wait != null) {
                            insertStmt.setInt(7, wait);
                        } else {
                            insertStmt.setNull(7, Types.INTEGER);
                        }
                        insertStmt.setInt(8, newBotJobId);

                        insertStmt.addBatch();
                        count++;

                        if (count % BATCH_SIZE == 0) {
                            insertStmt.executeBatch();
                            conn.commit();
                            System.out.println("Inserted batch of " + BATCH_SIZE);
                        }
                    }

                    // Final batch
                    if (count % BATCH_SIZE != 0) {
                        insertStmt.executeBatch();
                        conn.commit();
                        System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                    }
                }
            }

            // Step 3: get block ids after insert
            idsBlockAfter.clear();
            try (ResultSet rsIdsAfter = idStmtAfter.executeQuery(selectComponentIdsSQL)) {
                while (rsIdsAfter.next()) {
                    idsBlockAfter.add(rsIdsAfter.getInt("id"));
                }
            }

            // Step 4: compute new IDs
            List<Integer> newComponentIds = new ArrayList<>(idsBlockAfter);
            newComponentIds.removeAll(blockIdsBefore);

            // You can now use `newComponentIds` as needed
            System.out.println("Newly inserted block IDs: " + newComponentIds);

            // Step 5: update blockMap with new IDs
            List<Integer> keys = new ArrayList<>(blockMap.keySet());

            if (keys.size() != newComponentIds.size()) {
                System.err.println(
                        "Mismatch in size: expected " + keys.size() + " new IDs, but got " + newComponentIds.size());
            } else {
                for (int i = 0; i < keys.size(); i++) {
                    Integer oldId = keys.get(i);
                    Integer newId = newComponentIds.get(i);
                    blockMap.put(oldId, newId);
                }
            }

            return null;

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to create saved block");
            return new ErrorMessage("Failed to create saved block", "block Insertion Failure", error.getMessage());
        }
    }

    public ErrorMessage createInjectInstructions(BlockDetailsDTO blockDetailsDTO) {

        final int BATCH_SIZE = 100;

        String checkExistsSQL =
                "SELECT * FROM component_instruction WHERE home_banking_id = ? AND block_id = ? order by id";

        String selectComponentIdsSQL =
                "SELECT id FROM instruction WHERE bot_job_id = " + blockDetailsDTO.getBotJobId() + " ORDER BY id";

        try (Connection conn = getConnection();
                PreparedStatement selectStmt = conn.prepareStatement(checkExistsSQL);
                Statement idStmtBefore = conn.createStatement();
                Statement idStmtAfter = conn.createStatement()) {

            conn.setAutoCommit(false);

            // Step 1: get current component_block ids before insert
            List<Integer> blockIdsBefore = new ArrayList<>();
            try (ResultSet rsIdsBefore = idStmtBefore.executeQuery(selectComponentIdsSQL)) {
                while (rsIdsBefore.next()) {
                    blockIdsBefore.add(rsIdsBefore.getInt("id"));
                }
            }

            // Step 2: prepare and execute insert
            selectStmt.setInt(1, blockDetailsDTO.getHomeBankingId());
            selectStmt.setInt(2, blockDetailsDTO.getBlockId());

            try (ResultSet rsInstruction = selectStmt.executeQuery()) {

                String insertSQL = "INSERT INTO instruction ("
                        + "instruction_order_number, actions, name, xpath, coordinates, force_coordinates, iframe_xpath, tag_name, shadow_host, shadow_root, css_selector, description, operation, optional, block_marked, default_value, action_custom_max_wait_sec, on_hold_seconds, codified, export_to_abr, active, block_id, variable_id, parent_id, bot_job_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                instructionMap.clear();
                instrVariablesMap.clear();

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {

                    int count = 0;

                    while (rsInstruction.next()) {
                        int id = rsInstruction.getInt("id");
                        Integer newBotJobId = blockDetailsDTO.getBotJobId();
                        if (newBotJobId == null) {
                            System.out.println("Skipped instruction with unknown bot_job_id");
                            continue;
                        }

                        int oldBlockId = rsInstruction.getInt("block_id");
                        Integer newBlockId = blockMap.get(oldBlockId);
                        if (newBlockId == null) {
                            System.out.println("Skipped instruction with unknown block_id: " + oldBlockId);
                            continue;
                        }

                        instructionMap.put(id, -1);

                        insertStmt.setInt(1, rsInstruction.getInt("instruction_order_number"));
                        insertStmt.setString(2, rsInstruction.getString("actions"));
                        insertStmt.setString(3, rsInstruction.getString("name"));
                        insertStmt.setString(4, rsInstruction.getString("xpath"));
                        insertStmt.setString(5, rsInstruction.getString("coordinates"));

                        insertOrNull(insertStmt, 6, rsInstruction, "force_coordinates");
                        insertStmt.setString(7, rsInstruction.getString("iframe_xpath"));
                        insertStmt.setString(8, rsInstruction.getString("tag_name"));
                        insertStmt.setString(9, rsInstruction.getString("shadow_host"));
                        insertStmt.setString(10, rsInstruction.getString("shadow_root"));
                        insertStmt.setString(11, rsInstruction.getString("css_selector"));
                        insertStmt.setString(12, rsInstruction.getString("description"));
                        insertStmt.setString(13, rsInstruction.getString("operation"));

                        insertOrNull(insertStmt, 14, rsInstruction, "optional");
                        insertOrNull(insertStmt, 15, rsInstruction, "block_marked");
                        insertStmt.setString(16, rsInstruction.getString("default_value"));
                        insertOrNull(insertStmt, 17, rsInstruction, "action_custom_max_wait_sec");
                        insertOrNull(insertStmt, 18, rsInstruction, "on_hold_seconds");
                        insertOrNull(insertStmt, 19, rsInstruction, "codified");
                        insertOrNull(insertStmt, 20, rsInstruction, "export_to_abr");

                        insertStmt.setInt(21, rsInstruction.getInt("active"));
                        insertStmt.setInt(22, newBlockId);

                        // variable_id + tracking
                        Integer variableId = rsInstruction.getInt("variable_id");
                        if (rsInstruction.wasNull()) {
                            variableId = null;
                        }

                        if (variableId != null) {
                            instrVariablesMap.put(id, variableId);
                        }
                        insertStmt.setNull(23, Types.INTEGER); // TO BE SOLVED LATER

                        insertOrNull(insertStmt, 24, rsInstruction, "parent_id");

                        insertStmt.setInt(25, newBotJobId);

                        insertStmt.addBatch();
                        count++;

                        if (count % BATCH_SIZE == 0) {
                            insertStmt.executeBatch();
                            conn.commit();
                            System.out.println("Inserted batch of " + BATCH_SIZE);
                        }
                    }

                    // Final batch
                    if (count % BATCH_SIZE != 0) {
                        insertStmt.executeBatch();
                        conn.commit();
                        System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                    }
                }
            }

            // Step 3: get component_block ids after insert
            List<Integer> blockIdsAfter = new ArrayList<>();
            try (ResultSet rsIdsAfter = idStmtAfter.executeQuery(selectComponentIdsSQL)) {
                while (rsIdsAfter.next()) {
                    blockIdsAfter.add(rsIdsAfter.getInt("id"));
                }
            }

            // Step 4: compute new IDs
            List<Integer> newComponentIds = new ArrayList<>(blockIdsAfter);
            newComponentIds.removeAll(blockIdsBefore);

            // You can now use `newComponentIds` as needed
            System.out.println("Newly inserted instruction IDs: " + newComponentIds);

            // Step 5: update blockMap with new IDs
            List<Integer> keys = new ArrayList<>(instructionMap.keySet());

            if (keys.size() != newComponentIds.size()) {
                System.err.println(
                        "Mismatch in size: expected " + keys.size() + " new IDs, but got " + newComponentIds.size());
            } else {
                for (int i = 0; i < keys.size(); i++) {
                    Integer oldId = keys.get(i);
                    Integer newId = newComponentIds.get(i);
                    instructionMap.put(oldId, newId);
                }
            }

            return null;

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to create saved instruction");
            return new ErrorMessage(
                    "Failed to create saved instruction", "instruction Insertion Failure", error.getMessage());
        }
    }

    public ErrorMessage createInjectVariables(BlockDetailsDTO blockDetailsDTO) {

        final int BATCH_SIZE = 100;

        String idsInstruction =
                instructionMap.keySet().stream().map(String::valueOf).collect(Collectors.joining(","));

        String checkExistsSQL = "SELECT * FROM component_variable "
                + "WHERE  home_banking_id = ? AND instruction_id IN (" + idsInstruction + ") ORDER BY id";

        String selectComponentIdsSQL =
                "SELECT id FROM variable WHERE bot_job_id = " + blockDetailsDTO.getBotJobId() + " ORDER BY id";

        try (Connection conn = getConnection();
                PreparedStatement selectStmt = conn.prepareStatement(checkExistsSQL);
                Statement idStmtBefore = conn.createStatement();
                Statement idStmtAfter = conn.createStatement()) {

            conn.setAutoCommit(false);

            // Step 1: get current component_block ids before insert
            List<Integer> blockIdsBefore = new ArrayList<>();
            try (ResultSet rsIdsBefore = idStmtBefore.executeQuery(selectComponentIdsSQL)) {
                while (rsIdsBefore.next()) {
                    blockIdsBefore.add(rsIdsBefore.getInt("id"));
                }
            }

            // Step 2: prepare and execute insert
            selectStmt.setInt(1, blockDetailsDTO.getHomeBankingId());

            try (ResultSet rsVariable = selectStmt.executeQuery()) {

                String insertSQL =
                        "INSERT INTO variable (type, name, value, local_format, delimiter, instruction_id, bot_job_id) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

                variableMap.clear();

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {

                    int count = 0;

                    while (rsVariable.next()) {
                        int id = rsVariable.getInt("id");

                        Integer newBotJobId = blockDetailsDTO.getBotJobId();
                        if (newBotJobId == null) {
                            System.out.println("Skipped variable with unknown bot_job_id");
                            continue;
                        }

                        Integer instructionId = rsVariable.getObject("instruction_id") != null
                                ? rsVariable.getInt("instruction_id")
                                : null;

                        Integer newInstructionId = null;

                        if (instructionId != null) {
                            newInstructionId = instructionMap.get(instructionId);
                            if (newInstructionId == null) {
                                System.out.println("Skipped variable with unknown instruction_id: " + instructionId);
                                continue;
                            }
                        }

                        variableMap.put(id, -1);

                        insertStmt.setString(1, rsVariable.getString("type"));
                        insertStmt.setString(2, rsVariable.getString("name"));
                        insertStmt.setString(3, rsVariable.getString("value"));
                        insertStmt.setString(4, rsVariable.getString("local_format"));
                        insertStmt.setString(5, rsVariable.getString("delimiter"));

                        if (newInstructionId != null) {
                            insertStmt.setInt(6, newInstructionId);
                        } else {
                            insertStmt.setNull(6, Types.INTEGER);
                        }

                        insertStmt.setInt(7, newBotJobId);

                        insertStmt.addBatch();
                        count++;

                        if (count % BATCH_SIZE == 0) {
                            insertStmt.executeBatch();
                            conn.commit();
                            System.out.println("Inserted batch of " + BATCH_SIZE);
                        }
                    }

                    // Final batch
                    if (count % BATCH_SIZE != 0) {
                        insertStmt.executeBatch();
                        conn.commit();
                        System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                    }
                }
            }

            // Step 3: get component_block ids after insert
            List<Integer> idsAfter = new ArrayList<>();
            try (ResultSet rsIdsAfter = idStmtAfter.executeQuery(selectComponentIdsSQL)) {
                while (rsIdsAfter.next()) {
                    idsAfter.add(rsIdsAfter.getInt("id"));
                }
            }

            // Step 4: compute new IDs
            List<Integer> newComponentIds = new ArrayList<>(idsAfter);
            newComponentIds.removeAll(blockIdsBefore);

            // You can now use `newComponentIds` as needed
            System.out.println("Newly inserted variable IDs: " + newComponentIds);

            // Step 5: update blockMap with new IDs
            List<Integer> keys = new ArrayList<>(variableMap.keySet());

            if (keys.size() != newComponentIds.size()) {
                System.err.println(
                        "Mismatch in size: expected " + keys.size() + " new IDs, but got " + newComponentIds.size());
            } else {
                for (int i = 0; i < keys.size(); i++) {
                    Integer oldId = keys.get(i);
                    Integer newId = newComponentIds.get(i);
                    variableMap.put(oldId, newId);
                }
            }

            return null;

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to create saved variable");
            return new ErrorMessage(
                    "Failed to create saved variable", "variable Insertion Failure", error.getMessage());
        }
    }

    public ErrorMessage createUpdateInjectInstruction(BlockDetailsDTO blockDetailsDTO) {
        final int BATCH_SIZE = 100;

        instrNewInverted.clear();

        for (Map.Entry<Integer, Integer> entry : instructionMap.entrySet()) {
            instrNewInverted.put(entry.getValue(), entry.getKey());
        }

        try (Connection conn = getConnection();
                Statement postgresStmt = conn.createStatement()) {
            conn.setAutoCommit(false);

            String idsInstruction =
                    instructionMap.values().stream().map(String::valueOf).collect(Collectors.joining(","));

            String idsBlock = blockMap.values().stream().map(String::valueOf).collect(Collectors.joining(","));

            String selectAccessSQL = "SELECT id, name, parent_id, parent_block_id, variable_id " + "FROM instruction "
                    + "WHERE (parent_block_id IS NOT NULL OR parent_id IS NOT NULL OR variable_id IS NOT NULL) "
                    + "AND bot_job_id = "
                    + blockDetailsDTO.getBotJobId() + " AND id IN ("
                    + idsInstruction + ") " + "AND block_id IN ("
                    + idsBlock + ") " + "ORDER BY id";

            try (ResultSet rsInstruction = postgresStmt.executeQuery(selectAccessSQL)) {

                String updateSQL =
                        "UPDATE instruction SET variable_id = ?, parent_block_id = ?, parent_id = ? WHERE id = ?";

                try (PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {
                    int count = 0;

                    while (rsInstruction.next()) {
                        int id = rsInstruction.getInt("id");
                        String name = rsInstruction.getString("name");

                        // ---- variable_id ----
                        Integer originalOldID = instrNewInverted.get(id);
                        Integer originalVarId = null;

                        if (originalOldID != null) {
                            originalVarId = instrVariablesMap.get(originalOldID);
                        }

                        Integer newVariableId = null;
                        if (originalVarId != null) {
                            newVariableId = variableMap.get(originalVarId);
                        }

                        if (newVariableId == null) {
                            System.out.println("Skipped variable_id column with unknown variable_id: " + newVariableId);
                            updateStmt.setNull(1, Types.INTEGER);
                        } else {
                            updateStmt.setInt(1, newVariableId);
                        }

                        // Handle parent_id or parent_block_id based on name
                        if ("GOTO".equalsIgnoreCase(name) || "EXCEL GOTO".equalsIgnoreCase(name)) {
                            int parentBlockId = rsInstruction.getInt("parent_block_id");
                            if (rsInstruction.wasNull() || !blockMap.containsKey(parentBlockId)) {
                                updateStmt.setNull(2, Types.INTEGER);
                            } else {
                                updateStmt.setInt(2, blockMap.get(parentBlockId));
                            }
                            updateStmt.setNull(3, Types.INTEGER); // Skip parent_id
                        } else {
                            int parentInstructionId = rsInstruction.getInt("parent_id");
                            if (rsInstruction.wasNull() || !instructionMap.containsKey(parentInstructionId)) {
                                updateStmt.setNull(3, Types.INTEGER);
                            } else {
                                updateStmt.setInt(3, instructionMap.get(parentInstructionId));
                            }
                            updateStmt.setNull(2, Types.INTEGER); // Skip parent_block_id
                        }

                        updateStmt.setInt(4, id); // WHERE clause: name = ?

                        updateStmt.addBatch();
                        count++;

                        if (count % BATCH_SIZE == 0) {
                            updateStmt.executeBatch();
                            conn.commit();
                            System.out.println("Updated batch of " + BATCH_SIZE);
                        }
                    }

                    if (count % BATCH_SIZE != 0) {
                        updateStmt.executeBatch();
                        conn.commit();
                        System.out.println("Updated final batch of " + (count % BATCH_SIZE));
                    }

                    ARLogger.getInstance(PerformDataBase.class).info("Updated instruction records: " + count);
                }
            }

            return null;

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to update instruction");
            return new ErrorMessage("Failed to update instruction", "instruction Update Failure", error.getMessage());
        }
    }

    public ErrorMessage createInjectReferences(BlockDetailsDTO blockDetailsDTO) {

        final int BATCH_SIZE = 100;

        String idsInstruction =
                instructionMap.keySet().stream().map(String::valueOf).collect(Collectors.joining(","));

        String checkExistsSQL = "SELECT * FROM component_reference "
                + "WHERE  home_banking_id = ? AND instruction_id IN (" + idsInstruction + ") ORDER BY id";

        String selectComponentIdsSQL =
                "SELECT id FROM reference WHERE bot_job_id = " + blockDetailsDTO.getBotJobId() + " ORDER BY id";

        try (Connection conn = getConnection();
                PreparedStatement selectStmt = conn.prepareStatement(checkExistsSQL);
                Statement idStmtBefore = conn.createStatement();
                Statement idStmtAfter = conn.createStatement()) {

            conn.setAutoCommit(false);

            // Step 1: get current component_block ids before insert
            List<Integer> blockIdsBefore = new ArrayList<>();
            try (ResultSet rsIdsBefore = idStmtBefore.executeQuery(selectComponentIdsSQL)) {
                while (rsIdsBefore.next()) {
                    blockIdsBefore.add(rsIdsBefore.getInt("id"));
                }
            }

            // Step 2: prepare and execute insert
            selectStmt.setInt(1, blockDetailsDTO.getHomeBankingId());

            try (ResultSet rsReference = selectStmt.executeQuery()) {

                String insertSQL =
                        "INSERT INTO reference (reference_type, value, instruction_id, bot_job_id) VALUES (?, ?, ?, ?)";

                referenceMap.clear();

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {

                    int count = 0;

                    while (rsReference.next()) {
                        int id = rsReference.getInt("id");

                        int oldInstructionId = rsReference.getInt("instruction_id");
                        Integer newInstructionId = instructionMap.get(oldInstructionId);
                        if (newInstructionId == null) {
                            System.out.println("Skipped reference with unknown instruction_id: " + oldInstructionId);
                            continue;
                        }

                        Integer newBotJobId = blockDetailsDTO.getBotJobId();
                        if (newBotJobId == null) {
                            System.out.println("Skipped reference with unknown bot_job_id: " + newBotJobId);
                            continue;
                        }

                        referenceMap.put(id, -1);

                        insertStmt.setString(1, rsReference.getString("reference_type"));
                        insertStmt.setString(2, rsReference.getString("value"));
                        insertStmt.setInt(3, newInstructionId);

                        if (newBotJobId != null) {
                            insertStmt.setInt(4, newBotJobId);
                        } else {
                            insertStmt.setNull(4, Types.INTEGER);
                        }

                        insertStmt.addBatch();
                        count++;

                        if (count % BATCH_SIZE == 0) {
                            insertStmt.executeBatch();
                            conn.commit();
                            System.out.println("Inserted batch of " + BATCH_SIZE);
                        }
                    }

                    // Final batch
                    if (count % BATCH_SIZE != 0) {
                        insertStmt.executeBatch();
                        conn.commit();
                        System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                    }
                }
            }

            // Step 3: get component_block ids after insert
            List<Integer> idsAfter = new ArrayList<>();
            try (ResultSet rsIdsAfter = idStmtAfter.executeQuery(selectComponentIdsSQL)) {
                while (rsIdsAfter.next()) {
                    idsAfter.add(rsIdsAfter.getInt("id"));
                }
            }

            // Step 4: compute new IDs
            List<Integer> newComponentIds = new ArrayList<>(idsAfter);
            newComponentIds.removeAll(blockIdsBefore);

            // You can now use `newComponentIds` as needed
            System.out.println("Newly inserted reference IDs: " + newComponentIds);

            // Step 5: update blockMap with new IDs
            List<Integer> keys = new ArrayList<>(referenceMap.keySet());

            if (keys.size() != newComponentIds.size()) {
                System.err.println(
                        "Mismatch in size: expected " + keys.size() + " new IDs, but got " + newComponentIds.size());
            } else {
                for (int i = 0; i < keys.size(); i++) {
                    Integer oldId = keys.get(i);
                    Integer newId = newComponentIds.get(i);
                    referenceMap.put(oldId, newId);
                }
            }

            return null;

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to create saved reference");
            return new ErrorMessage(
                    "Failed to create saved reference", "reference Insertion Failure", error.getMessage());
        }
    }

    // CLONE BOT JOB
    public Integer getNewBotBojId(int previousBotJob) {
        return botJobMap.get(previousBotJob);
    }

    public ErrorMessage cloneBotJob(
            HomeUrlDTO homeUrlDTO, int previousBotJob, String newBotJobName, String newDescription) {

        String selectComponentIdsSQL =
                "SELECT id FROM bot_job WHERE home_banking_id = " + homeUrlDTO.getHomeBankingId() + " ORDER BY id";

        try (Connection conn = getConnection();
                Statement idStmtBefore = conn.createStatement();
                Statement idStmtAfter = conn.createStatement()) {

            conn.setAutoCommit(false);

            // Step 1: get current component_block ids before insert
            List<Integer> blockIdsBefore = new ArrayList<>();
            try (ResultSet rsIdsBefore = idStmtBefore.executeQuery(selectComponentIdsSQL)) {
                while (rsIdsBefore.next()) {
                    blockIdsBefore.add(rsIdsBefore.getInt("id"));
                }
            }

            String insertSQL =
                    "INSERT INTO bot_job (name, description, priority, home_banking_id, home_url_id, active) "
                            + "SELECT ?, ?, priority, home_banking_id, ?, ? FROM bot_job WHERE id = ?";

            botJobMap.clear();
            botJobMap.put(previousBotJob, -1);

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {

                insertStmt.setString(1, newBotJobName);
                insertStmt.setString(2, newDescription);
                insertStmt.setInt(3, homeUrlDTO.getId());
                insertStmt.setInt(4, 1);
                insertStmt.setInt(5, previousBotJob);

                insertStmt.addBatch();
                insertStmt.executeBatch();
                conn.commit();
                ARLogger.getInstance(PerformDataBase.class).info("Inserted bot job record: 1");
            }

            // Step 3: get component_block ids after insert
            List<Integer> blockIdsAfter = new ArrayList<>();
            try (ResultSet rsIdsAfter = idStmtAfter.executeQuery(selectComponentIdsSQL)) {
                while (rsIdsAfter.next()) {
                    blockIdsAfter.add(rsIdsAfter.getInt("id"));
                }
            }

            // Step 4: compute new IDs
            List<Integer> newComponentIds = new ArrayList<>(blockIdsAfter);
            newComponentIds.removeAll(blockIdsBefore);

            // You can now use `newComponentIds` as needed
            System.out.println("Newly inserted bot_job IDs: " + newComponentIds);

            // Step 5: update blockMap with new IDs
            List<Integer> keys = new ArrayList<>(botJobMap.keySet());

            if (keys.size() != newComponentIds.size()) {
                System.err.println(
                        "Mismatch in size: expected " + keys.size() + " new IDs, but got " + newComponentIds.size());
            } else {
                for (int i = 0; i < keys.size(); i++) {
                    Integer oldId = keys.get(i);
                    Integer newId = newComponentIds.get(i);
                    botJobMap.put(oldId, newId);
                }
            }

            return null;

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to clone bot_job");
            return new ErrorMessage("Failed to clone bot_job", "bot_job Insertion Failure", error.getMessage());
        }
    }

    public ErrorMessage cloneBlock(int previousBotJob) {

        final int BATCH_SIZE = 100;
        String checkExistsSQL =
                "SELECT id, block_order_number, name, description, type_id, export_file, active, wait, bot_job_id "
                        + "FROM block WHERE bot_job_id = ? ORDER BY id";

        String selectComponentIdsSQL = "SELECT id FROM block ORDER BY id";

        try (Connection conn = getConnection();
                PreparedStatement selectStmt = conn.prepareStatement(checkExistsSQL);
                Statement idStmtBefore = conn.createStatement();
                Statement idStmtAfter = conn.createStatement()) {

            conn.setAutoCommit(false);

            // Step 1: get current component_block ids before insert
            List<Integer> blockIdsBefore = new ArrayList<>();
            try (ResultSet rsIdsBefore = idStmtBefore.executeQuery(selectComponentIdsSQL)) {
                while (rsIdsBefore.next()) {
                    blockIdsBefore.add(rsIdsBefore.getInt("id"));
                }
            }

            // Step 2: prepare and execute insert
            selectStmt.setInt(1, previousBotJob);
            try (ResultSet rs = selectStmt.executeQuery()) {

                String insertSQL = "INSERT INTO block "
                        + "( block_order_number, name, description, type_id, export_file, active, wait, bot_job_id) "
                        + "VALUES ( ?, ?, ?, ?, ?, ?, ?, ?)";

                blockMap.clear();

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {

                    int count = 0;

                    while (rs.next()) {
                        int id = rs.getInt("id");
                        int blockOrderNumber = rs.getInt("block_order_number");
                        String name = rs.getString("name");
                        String description = rs.getString("description");
                        Integer typeId = rs.getObject("type_id") != null ? rs.getInt("type_id") : null;
                        String exportFile = rs.getString("export_file");
                        int active = rs.getInt("active");
                        Integer wait = rs.getObject("wait") != null ? rs.getInt("wait") : null;

                        int oldBotJobId = rs.getInt("bot_job_id");

                        // Map old bot_job_id to new
                        Integer newBotJobId = botJobMap.get(oldBotJobId);
                        if (newBotJobId == null) {
                            System.out.println("Skipped block with unknown bot_job_id: " + newBotJobId);
                            continue;
                        }

                        blockMap.put(id, -1);

                        insertStmt.setInt(1, blockOrderNumber);
                        insertStmt.setString(2, name);
                        insertStmt.setString(3, description);
                        if (typeId != null) {
                            insertStmt.setInt(4, typeId);
                        } else {
                            insertStmt.setNull(4, Types.INTEGER);
                        }
                        insertStmt.setString(5, exportFile);
                        insertStmt.setInt(6, active);
                        if (wait != null) {
                            insertStmt.setInt(7, wait);
                        } else {
                            insertStmt.setNull(7, Types.INTEGER);
                        }
                        insertStmt.setInt(8, newBotJobId);

                        insertStmt.addBatch();
                        count++;

                        if (count % BATCH_SIZE == 0) {
                            insertStmt.executeBatch();
                            conn.commit();
                            System.out.println("Inserted batch of " + BATCH_SIZE);
                        }
                    }

                    // Final batch
                    if (count % BATCH_SIZE != 0) {
                        insertStmt.executeBatch();
                        conn.commit();
                        System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                    }
                }
            }

            // Step 3: get component_block ids after insert
            List<Integer> blockIdsAfter = new ArrayList<>();
            try (ResultSet rsIdsAfter = idStmtAfter.executeQuery(selectComponentIdsSQL)) {
                while (rsIdsAfter.next()) {
                    blockIdsAfter.add(rsIdsAfter.getInt("id"));
                }
            }

            // Step 4: compute new IDs
            List<Integer> newComponentIds = new ArrayList<>(blockIdsAfter);
            newComponentIds.removeAll(blockIdsBefore);

            // You can now use `newComponentIds` as needed
            System.out.println("Newly inserted block IDs: " + newComponentIds);

            // Step 5: update blockMap with new IDs
            List<Integer> keys = new ArrayList<>(blockMap.keySet());

            if (keys.size() != newComponentIds.size()) {
                System.err.println(
                        "Mismatch in size: expected " + keys.size() + " new IDs, but got " + newComponentIds.size());
            } else {
                for (int i = 0; i < keys.size(); i++) {
                    Integer oldId = keys.get(i);
                    Integer newId = newComponentIds.get(i);
                    blockMap.put(oldId, newId);
                }
            }

            return null;

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to clone block");
            return new ErrorMessage("Failed to clone block", "block Insertion Failure", error.getMessage());
        }
    }

    public ErrorMessage cloneInstructions(int previousBotJob) {

        final int BATCH_SIZE = 100;

        String checkExistsSQL = "SELECT * FROM instruction WHERE bot_job_id = ? order by id";

        String selectComponentIdsSQL = "SELECT id FROM instruction ORDER BY id";

        try (Connection conn = getConnection();
                PreparedStatement selectStmt = conn.prepareStatement(checkExistsSQL);
                Statement idStmtBefore = conn.createStatement();
                Statement idStmtAfter = conn.createStatement()) {

            conn.setAutoCommit(false);

            // Step 1: get current component_block ids before insert
            List<Integer> blockIdsBefore = new ArrayList<>();
            try (ResultSet rsIdsBefore = idStmtBefore.executeQuery(selectComponentIdsSQL)) {
                while (rsIdsBefore.next()) {
                    blockIdsBefore.add(rsIdsBefore.getInt("id"));
                }
            }

            // Step 2: prepare and execute insert
            selectStmt.setInt(1, previousBotJob);

            try (ResultSet rsInstruction = selectStmt.executeQuery()) {

                String insertSQL = "INSERT INTO instruction ("
                        + " instruction_order_number, actions, name, xpath, coordinates, force_coordinates, iframe_xpath, tag_name, shadow_host, shadow_root, css_selector, description, operation, optional, block_marked, default_value, action_custom_max_wait_sec, on_hold_seconds, codified, export_to_abr, active, block_id, variable_id, parent_id, bot_job_id) "
                        + "VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                instructionMap.clear();
                instrVariablesMap.clear();

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {

                    int count = 0;

                    while (rsInstruction.next()) {
                        int id = rsInstruction.getInt("id");

                        int oldBlockId = rsInstruction.getInt("block_id");
                        Integer newBlockId = blockMap.get(oldBlockId);
                        if (newBlockId == null) {
                            System.out.println("Skipped instruction with unknown block_id: " + oldBlockId);
                            continue;
                        }

                        int oldBotJobId = rsInstruction.getInt("bot_job_id");
                        Integer newBotJobId = botJobMap.get(oldBotJobId);
                        if (newBotJobId == null) {
                            System.out.println("Skipped instruction with unknown bot_job_id: " + newBotJobId);
                            continue;
                        }

                        instructionMap.put(id, -1);

                        insertStmt.setInt(1, rsInstruction.getInt("instruction_order_number"));
                        insertStmt.setString(2, rsInstruction.getString("actions"));
                        insertStmt.setString(3, rsInstruction.getString("name"));
                        insertStmt.setString(4, rsInstruction.getString("xpath"));
                        insertStmt.setString(5, rsInstruction.getString("coordinates"));

                        insertOrNull(insertStmt, 6, rsInstruction, "force_coordinates");
                        insertStmt.setString(7, rsInstruction.getString("iframe_xpath"));
                        insertStmt.setString(8, rsInstruction.getString("tag_name"));
                        insertStmt.setString(9, rsInstruction.getString("shadow_host"));
                        insertStmt.setString(10, rsInstruction.getString("shadow_root"));
                        insertStmt.setString(11, rsInstruction.getString("css_selector"));
                        insertStmt.setString(12, rsInstruction.getString("description"));
                        insertStmt.setString(13, rsInstruction.getString("operation"));

                        insertOrNull(insertStmt, 14, rsInstruction, "optional");
                        insertOrNull(insertStmt, 15, rsInstruction, "block_marked");
                        insertStmt.setString(16, rsInstruction.getString("default_value"));
                        insertOrNull(insertStmt, 17, rsInstruction, "action_custom_max_wait_sec");
                        insertOrNull(insertStmt, 18, rsInstruction, "on_hold_seconds");
                        insertOrNull(insertStmt, 19, rsInstruction, "codified");
                        insertOrNull(insertStmt, 20, rsInstruction, "export_to_abr");

                        insertStmt.setInt(21, rsInstruction.getInt("active"));
                        insertStmt.setInt(22, newBlockId);

                        // variable_id + tracking
                        Integer variableId = rsInstruction.getInt("variable_id");
                        if (rsInstruction.wasNull()) {
                            variableId = null;
                        }

                        if (variableId != null) {
                            instrVariablesMap.put(id, variableId);
                        }
                        insertStmt.setNull(23, Types.INTEGER); // TO BE SOLVED LATER

                        insertOrNull(insertStmt, 24, rsInstruction, "parent_id");

                        insertStmt.setInt(25, newBotJobId);

                        insertStmt.addBatch();
                        count++;

                        if (count % BATCH_SIZE == 0) {
                            insertStmt.executeBatch();
                            conn.commit();
                            System.out.println("Inserted batch of " + BATCH_SIZE);
                        }
                    }

                    // Final batch
                    if (count % BATCH_SIZE != 0) {
                        insertStmt.executeBatch();
                        conn.commit();
                        System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                    }
                }
            }

            // Step 3: get component_block ids after insert
            List<Integer> blockIdsAfter = new ArrayList<>();
            try (ResultSet rsIdsAfter = idStmtAfter.executeQuery(selectComponentIdsSQL)) {
                while (rsIdsAfter.next()) {
                    blockIdsAfter.add(rsIdsAfter.getInt("id"));
                }
            }

            // Step 4: compute new IDs
            List<Integer> newComponentIds = new ArrayList<>(blockIdsAfter);
            newComponentIds.removeAll(blockIdsBefore);

            // You can now use `newComponentIds` as needed
            System.out.println("Newly inserted instruction IDs: " + newComponentIds);

            // Step 5: update blockMap with new IDs
            List<Integer> keys = new ArrayList<>(instructionMap.keySet());

            if (keys.size() != newComponentIds.size()) {
                System.err.println(
                        "Mismatch in size: expected " + keys.size() + " new IDs, but got " + newComponentIds.size());
            } else {
                for (int i = 0; i < keys.size(); i++) {
                    Integer oldId = keys.get(i);
                    Integer newId = newComponentIds.get(i);
                    instructionMap.put(oldId, newId);
                }
            }

            return null;

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to clono instruction");
            return new ErrorMessage("Failed to clone instruction", "instruction Insertion Failure", error.getMessage());
        }
    }

    public ErrorMessage cloneVariables(int previousBotJob) {

        final int BATCH_SIZE = 100;

        String checkExistsSQL = "SELECT * FROM variable WHERE bot_job_id = ? order by id";

        String selectComponentIdsSQL = "SELECT id FROM variable ORDER BY id";

        try (Connection conn = getConnection();
                PreparedStatement selectStmt = conn.prepareStatement(checkExistsSQL);
                Statement idStmtBefore = conn.createStatement();
                Statement idStmtAfter = conn.createStatement()) {

            conn.setAutoCommit(false);

            // Step 1: get current component_block ids before insert
            List<Integer> blockIdsBefore = new ArrayList<>();
            try (ResultSet rsIdsBefore = idStmtBefore.executeQuery(selectComponentIdsSQL)) {
                while (rsIdsBefore.next()) {
                    blockIdsBefore.add(rsIdsBefore.getInt("id"));
                }
            }

            // Step 2: prepare and execute insert
            selectStmt.setInt(1, previousBotJob);

            try (ResultSet rsVariable = selectStmt.executeQuery()) {

                String insertSQL =
                        "INSERT INTO variable ( type, name, value, local_format, delimiter, instruction_id, bot_job_id) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

                variableMap.clear();

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {

                    int count = 0;

                    while (rsVariable.next()) {
                        int id = rsVariable.getInt("id");

                        int oldBotJobId = rsVariable.getInt("bot_job_id");
                        Integer newBotJobId = botJobMap.get(oldBotJobId);
                        if (newBotJobId == null) {
                            System.out.println("Skipped variable with unknown bot_job_id: " + newBotJobId);
                            continue;
                        }

                        Integer instructionId = rsVariable.getObject("instruction_id") != null
                                ? rsVariable.getInt("instruction_id")
                                : null;

                        Integer newInstructionId = null;

                        if (instructionId != null) {
                            newInstructionId = instructionMap.get(instructionId);
                            if (newInstructionId == null) {
                                System.out.println("Skipped variable with unknown instruction_id: " + instructionId);
                                continue;
                            }
                        }

                        variableMap.put(id, -1);

                        insertStmt.setString(1, rsVariable.getString("type"));
                        insertStmt.setString(2, rsVariable.getString("name"));
                        insertStmt.setString(3, rsVariable.getString("value"));
                        insertStmt.setString(4, rsVariable.getString("local_format"));
                        insertStmt.setString(5, rsVariable.getString("delimiter"));

                        if (newInstructionId != null) {
                            insertStmt.setInt(6, newInstructionId);
                        } else {
                            insertStmt.setNull(6, Types.INTEGER);
                        }

                        insertStmt.setInt(7, newBotJobId);

                        insertStmt.addBatch();
                        count++;

                        if (count % BATCH_SIZE == 0) {
                            insertStmt.executeBatch();
                            conn.commit();
                            System.out.println("Inserted batch of " + BATCH_SIZE);
                        }
                    }

                    // Final batch
                    if (count % BATCH_SIZE != 0) {
                        insertStmt.executeBatch();
                        conn.commit();
                        System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                    }
                }
            }

            // Step 3: get component_block ids after insert
            List<Integer> idsAfter = new ArrayList<>();
            try (ResultSet rsIdsAfter = idStmtAfter.executeQuery(selectComponentIdsSQL)) {
                while (rsIdsAfter.next()) {
                    idsAfter.add(rsIdsAfter.getInt("id"));
                }
            }

            // Step 4: compute new IDs
            List<Integer> newComponentIds = new ArrayList<>(idsAfter);
            newComponentIds.removeAll(blockIdsBefore);

            // You can now use `newComponentIds` as needed
            System.out.println("Newly inserted variable IDs: " + newComponentIds);

            // Step 5: update blockMap with new IDs
            List<Integer> keys = new ArrayList<>(variableMap.keySet());

            if (keys.size() != newComponentIds.size()) {
                System.err.println(
                        "Mismatch in size: expected " + keys.size() + " new IDs, but got " + newComponentIds.size());
            } else {
                for (int i = 0; i < keys.size(); i++) {
                    Integer oldId = keys.get(i);
                    Integer newId = newComponentIds.get(i);
                    variableMap.put(oldId, newId);
                }
            }

            return null;

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to clone variable");
            return new ErrorMessage("Failed to clone variable", "variable Insertion Failure", error.getMessage());
        }
    }

    public ErrorMessage cloneUpdateInstruction(int previosBotJob) {
        final int BATCH_SIZE = 100;

        instrNewInverted.clear();

        for (Map.Entry<Integer, Integer> entry : instructionMap.entrySet()) {
            instrNewInverted.put(entry.getValue(), entry.getKey());
        }

        try (Connection conn = getConnection();
                Statement postgresStmt = conn.createStatement()) {
            conn.setAutoCommit(false);

            String idsInstruction =
                    instructionMap.values().stream().map(String::valueOf).collect(Collectors.joining(","));

            String idsBlock = blockMap.values().stream().map(String::valueOf).collect(Collectors.joining(","));

            int newBotJobId = botJobMap.get(previosBotJob);

            String selectAccessSQL = "SELECT id, name, parent_id, parent_block_id, variable_id " + "FROM instruction "
                    + "WHERE (parent_block_id IS NOT NULL OR parent_id IS NOT NULL OR variable_id IS NOT NULL) "
                    + "AND bot_job_id = "
                    + newBotJobId + " AND id IN ("
                    + idsInstruction + ") " + "AND block_id IN ("
                    + idsBlock + ") " + "ORDER BY id";

            try (ResultSet rsInstruction = postgresStmt.executeQuery(selectAccessSQL)) {

                String updateSQL =
                        "UPDATE instruction SET variable_id = ?, parent_block_id = ?, parent_id = ? WHERE id = ?";

                try (PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {
                    int count = 0;

                    while (rsInstruction.next()) {
                        int id = rsInstruction.getInt("id");
                        String name = rsInstruction.getString("name");

                        // ---- variable_id ----
                        Integer originalOldID = instrNewInverted.get(id);
                        Integer originalVarId = null;

                        if (originalOldID != null) {
                            originalVarId = instrVariablesMap.get(originalOldID);
                        }

                        Integer newVariableId = null;
                        if (originalVarId != null) {
                            newVariableId = variableMap.get(originalVarId);
                        }

                        if (newVariableId == null) {
                            System.out.println("Skipped variable_id column with unknown variable_id: " + newVariableId);
                            updateStmt.setNull(1, Types.INTEGER);
                        } else {
                            updateStmt.setInt(1, newVariableId);
                        }

                        // Handle parent_id or parent_block_id based on name
                        if ("GOTO".equalsIgnoreCase(name) || "EXCEL GOTO".equalsIgnoreCase(name)) {
                            int parentBlockId = rsInstruction.getInt("parent_block_id");
                            if (rsInstruction.wasNull() || !blockMap.containsKey(parentBlockId)) {
                                updateStmt.setNull(2, Types.INTEGER);
                            } else {
                                updateStmt.setInt(2, blockMap.get(parentBlockId));
                            }
                            updateStmt.setNull(3, Types.INTEGER); // Skip parent_id
                        } else {
                            int parentInstructionId = rsInstruction.getInt("parent_id");
                            if (rsInstruction.wasNull() || !instructionMap.containsKey(parentInstructionId)) {
                                updateStmt.setNull(3, Types.INTEGER);
                            } else {
                                updateStmt.setInt(3, instructionMap.get(parentInstructionId));
                            }
                            updateStmt.setNull(2, Types.INTEGER); // Skip parent_block_id
                        }

                        updateStmt.setInt(4, id); // WHERE clause: name = ?

                        updateStmt.addBatch();
                        count++;

                        if (count % BATCH_SIZE == 0) {
                            updateStmt.executeBatch();
                            conn.commit();
                            System.out.println("Updated batch of " + BATCH_SIZE);
                        }
                    }

                    if (count % BATCH_SIZE != 0) {
                        updateStmt.executeBatch();
                        conn.commit();
                        System.out.println("Updated final batch of " + (count % BATCH_SIZE));
                    }

                    ARLogger.getInstance(PerformDataBase.class).info("Updated instruction records: " + count);
                }
            }

            return null;

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to update cloned instruction");
            return new ErrorMessage(
                    "Failed to update cloned instruction", "cloned instruction Update Failure", error.getMessage());
        }
    }

    public ErrorMessage cloneReferences(int previousBotJob) {

        final int BATCH_SIZE = 100;

        String idsInstruction =
                instructionMap.keySet().stream().map(String::valueOf).collect(Collectors.joining(","));

        String checkExistsSQL = "SELECT * FROM reference " + "WHERE  bot_job_id = ? AND instruction_id IN ("
                + idsInstruction + ") ORDER BY id";

        String selectComponentIdsSQL = "SELECT id FROM reference ORDER BY id";

        try (Connection conn = getConnection();
                PreparedStatement selectStmt = conn.prepareStatement(checkExistsSQL);
                Statement idStmtBefore = conn.createStatement();
                Statement idStmtAfter = conn.createStatement()) {

            conn.setAutoCommit(false);

            // Step 1: get current component_block ids before insert
            List<Integer> blockIdsBefore = new ArrayList<>();
            try (ResultSet rsIdsBefore = idStmtBefore.executeQuery(selectComponentIdsSQL)) {
                while (rsIdsBefore.next()) {
                    blockIdsBefore.add(rsIdsBefore.getInt("id"));
                }
            }

            // Step 2: prepare and execute insert
            selectStmt.setInt(1, previousBotJob);

            try (ResultSet rsReference = selectStmt.executeQuery()) {

                String insertSQL =
                        "INSERT INTO reference (reference_type, value, instruction_id, bot_job_id) VALUES (?, ?, ?, ?)";

                referenceMap.clear();

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {

                    int count = 0;

                    while (rsReference.next()) {
                        int id = rsReference.getInt("id");

                        int oldInstructionId = rsReference.getInt("instruction_id");
                        Integer newInstructionId = instructionMap.get(oldInstructionId);
                        if (newInstructionId == null) {
                            System.out.println("Skipped reference with unknown instruction_id: " + oldInstructionId);
                            continue;
                        }

                        int oldBotJobId = rsReference.getInt("bot_job_id");
                        Integer newBotJobId = botJobMap.get(oldBotJobId);
                        if (newBotJobId == null) {
                            System.out.println("Skipped reference with unknown bot_job_id: " + newBotJobId);
                            continue;
                        }

                        referenceMap.put(id, -1);

                        insertStmt.setString(1, rsReference.getString("reference_type"));
                        insertStmt.setString(2, rsReference.getString("value"));
                        insertStmt.setInt(3, newInstructionId);

                        if (newBotJobId != null) {
                            insertStmt.setInt(4, newBotJobId);
                        } else {
                            insertStmt.setNull(4, Types.INTEGER);
                        }

                        insertStmt.addBatch();
                        count++;

                        if (count % BATCH_SIZE == 0) {
                            insertStmt.executeBatch();
                            conn.commit();
                            System.out.println("Inserted batch of " + BATCH_SIZE);
                        }
                    }

                    // Final batch
                    if (count % BATCH_SIZE != 0) {
                        insertStmt.executeBatch();
                        conn.commit();
                        System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                    }
                }
            }

            // Step 3: get component_block ids after insert
            List<Integer> idsAfter = new ArrayList<>();
            try (ResultSet rsIdsAfter = idStmtAfter.executeQuery(selectComponentIdsSQL)) {
                while (rsIdsAfter.next()) {
                    idsAfter.add(rsIdsAfter.getInt("id"));
                }
            }

            // Step 4: compute new IDs
            List<Integer> newComponentIds = new ArrayList<>(idsAfter);
            newComponentIds.removeAll(blockIdsBefore);

            // You can now use `newComponentIds` as needed
            System.out.println("Newly inserted reference IDs: " + newComponentIds);

            // Step 5: update blockMap with new IDs
            List<Integer> keys = new ArrayList<>(referenceMap.keySet());

            if (keys.size() != newComponentIds.size()) {
                System.err.println(
                        "Mismatch in size: expected " + keys.size() + " new IDs, but got " + newComponentIds.size());
            } else {
                for (int i = 0; i < keys.size(); i++) {
                    Integer oldId = keys.get(i);
                    Integer newId = newComponentIds.get(i);
                    referenceMap.put(oldId, newId);
                }
            }

            return null;

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to clone reference");
            return new ErrorMessage("Failed to clone reference", "reference Insertion Failure", error.getMessage());
        }
    }

    public void createTableLLama2AIVector() {

        try (Connection conn = getConnection()) {
            try (Statement stmt = conn.createStatement()) {

                String createTableVectorOpenAI =
                        """
                        CREATE TABLE web_elements_llama2 (
                          id SERIAL PRIMARY KEY,
                          element_name TEXT,
                          element_type TEXT,
                          embedding VECTOR(4096) -- size of OpenAI embedding vector
                        );
                        """;
                stmt.executeUpdate(createTableVectorOpenAI);
            }
            System.out.println("Database %s has been created!");
        } catch (SQLException error) {
            System.out.println("initializeDatabase\nError: " + error.getMessage());
        }
    }

    public void createTableOpenAIVector() {

        try (Connection conn = getConnection()) {
            try (Statement stmt = conn.createStatement()) {

                String createTableVectorOpenAI =
                        """
                        CREATE TABLE web_elements_openai (
                          id SERIAL PRIMARY KEY,
                          element_name TEXT,
                          element_type TEXT,
                          embedding vector(1536) -- size of OpenAI embedding vector
                        );
                        """;
                stmt.executeUpdate(createTableVectorOpenAI);
            }
            System.out.println("Database %s has been created!");
        } catch (SQLException error) {
            System.out.println("initializeDatabase\nError: " + error.getMessage());
        }
    }

    public boolean deleteAllJobDetails(String dataBaseType) {
        // Build the SQL delete statement
        try (Statement stmt = getConnection().createStatement()) {

            // Execute each statement individually
            stmt.executeUpdate("DELETE FROM variable;");
            stmt.executeUpdate("DELETE FROM reference;");
            stmt.executeUpdate("DELETE FROM instruction;");
            stmt.executeUpdate("DELETE FROM block;");
            stmt.executeUpdate("DELETE FROM bot_job;");

            stmt.executeUpdate("DELETE FROM component_variable;");
            stmt.executeUpdate("DELETE FROM component_reference;");
            stmt.executeUpdate("DELETE FROM component_instruction;");
            stmt.executeUpdate("DELETE FROM component_block;");

            // Drop sequences if they exist
            //            if (!dataBaseType.equalsIgnoreCase("ACCESS")) {
            //                stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"blockLoopInstructionSeq\";");
            //                stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"blockSeq\";");
            //                stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"botJobSeq\";");
            //                stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"variableSeq\";");
            //                stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"instructionReferenceSeq\";");
            //                stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"savedInstructionReferenceSeq\";");
            //                stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"excelReportSeq\";");
            //                stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"savedInstructionReferenceSeq\";");
            //                stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"savedBlockLoopInstructionSeq\";");
            //                stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"complexInstructionSeq\";");
            //                stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"configurationSeq\";");
            //                stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"homeBankingSeq\";");
            //                stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"savedBlockSeq\";");
            //                stmt.executeUpdate("DROP SEQUENCE IF EXISTS \"idgen\";");
            //            }
            ARLogger.getInstance(PerformDataBase.class)
                    .info("All Rows DELETED for:\n"
                            + "Variables;\n"
                            + "Instructions References;\n"
                            + "Instructions;\n"
                            + "Blocks;\n"
                            + "Bot Jobs;\n"
                            + "Saved Components;");

            return true;

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(dataBaseType + " Problems:\n"
                            + "Not Possible delete the  Rows was for these tables:\n"
                            + "ExcelReportDTO;\n"
                            + "Variables;\n"
                            + "Instructions References;\n"
                            + "Instructions;\n"
                            + "Blocks;\n"
                            + "Bot Jobs;\n"
                            + "Saved Components;\n"
                            + "Sequences Not dropped\n"
                            + e.getMessage());
        }
        return false;
    }

    public boolean deleteHomeUrl(String dataBaseType) {
        // Build the SQL delete statement
        try (Statement stmt = getConnection().createStatement()) {

            // Execute each statement individually
            stmt.executeUpdate("DELETE FROM home_url;");

            ARLogger.getInstance(PerformDataBase.class).info("All Rows DELETED for:\n" + "HomeUrl;");

            return true;

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(dataBaseType + " Problems:\n"
                            + "Not Possible delete the  Rows was for these tables:\n"
                            + "HomeUrl;\n"
                            + e.getMessage());
        }
        return false;
    }

    public void loadAllVariablesByCriteria(String tableName, int whereId, int parentId) {
        performLists.getListVariablesUser().clear();

        // Determine related table and columns based on tableName
        String joinTable;
        String joinTableVarId;
        String filterColumn;

        if ("component_variable".equalsIgnoreCase(tableName)) {
            joinTable = "component_instruction";
            joinTableVarId = "variable_id"; // assuming the join column name in component_instruction is variable_id
            filterColumn = "home_banking_id";
        } else {
            // default to "variable"
            joinTable = "instruction";
            joinTableVarId = "variable_id";
            filterColumn = "bot_job_id";
        }

        StringBuilder selectSQL = new StringBuilder(
                "SELECT vars.id, vars.type, vars.name, vars.value, vars.local_format, vars.delimiter, COUNT(blk."
                        + joinTableVarId + ") AS UsedVars "
                        + "FROM " + tableName + " vars "
                        + "LEFT JOIN " + joinTable + " blk ON blk." + joinTableVarId + " = vars.id "
                        + "WHERE vars." + filterColumn + " = ? ");

        if (parentId != -1) {
            selectSQL.append(" AND instruction_id = ? ");
        }

        selectSQL.append(" GROUP BY vars.id, vars.type, vars.name, vars.value, vars.local_format, vars.delimiter ");
        selectSQL.append(" ORDER BY vars.id ");

        try (PreparedStatement pstmt = getConnection().prepareStatement(selectSQL.toString())) {
            pstmt.setInt(1, whereId);
            if (parentId != -1) {
                pstmt.setInt(2, parentId);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String type = rs.getString("type");
                    String name = rs.getString("name");
                    String value = rs.getString("value");
                    String localFormat = rs.getString("local_format");
                    String delimiter = rs.getString("delimiter");
                    String usedVars = rs.getString("UsedVars");

                    performLists
                            .getListVariablesUser()
                            .add(new VariableUserDTO(
                                    id, type, name, value, whereId, parentId, localFormat, delimiter, usedVars));
                }
            }
        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("loadAllVariablesByCriteria. Error: " + e.getMessage());
        }
    }

    public void loadAllVariables(int botJobId) {
        performLists.getListVariable().clear();
        String selectSQL =
                "SELECT vars.id, instruction_id, vars.type, vars.name, vars.value, vars.local_format, vars.delimiter, COUNT(blk.variable_id) UsedVars "
                        + "FROM variable vars "
                        + "LEFT JOIN instruction blk ON blk.variable_id = vars.id "
                        + "WHERE vars.bot_job_id = " + botJobId;

        selectSQL += " GROUP BY vars.id, vars.type, vars.Name, vars.value";

        selectSQL += " ORDER BY vars.id";

        try (Statement stmt = getConnection().createStatement(); // Assuming you have getConnection() method
                ResultSet rs = stmt.executeQuery(selectSQL)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                Integer instructionId = rs.getInt("instruction_id");
                String type = rs.getString("type");
                String name = rs.getString("name");
                String value = rs.getString("value");
                String localFormat = rs.getString("local_format");
                String delimiter = rs.getString("delimiter");
                Integer usedVars = rs.getInt("UsedVars");
                performLists
                        .getListVariable()
                        .add(new VariableLoadDTO(
                                id, -1, botJobId, instructionId, type, name, value, localFormat, delimiter, usedVars));
            }
        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class).severe("loadAllVariables. Error: " + error.getMessage());
        }
    }

    public List<InstructionLoadDTO> loadExcelGotoBlock(int whereId, String tableName) {
        List<InstructionLoadDTO> instructionLoadDTOList = new ArrayList<>();

        // Build base SQL
        String sql = "SELECT * FROM " + tableName + " WHERE actions = 'EXCEL GOTO'";

        // Add condition depending on table
        if ("instruction".equalsIgnoreCase(tableName)) {
            sql += " AND bot_job_id = ?";
        } else if ("component_instruction".equalsIgnoreCase(tableName)) {
            sql += " AND home_banking_id = ?";
        } else {
            throw new IllegalArgumentException("Unsupported table: " + tableName);
        }

        sql += " ORDER BY instruction_order_number";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, whereId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    InstructionLoadDTO instructionLoadDTO = new InstructionLoadDTO();
                    instructionLoadDTO.setId(rs.getInt("id"));
                    instructionLoadDTO.setActionCustomMaxWaitSec(rs.getInt("action_custom_max_wait_sec"));
                    instructionLoadDTO.setActions(rs.getString("actions"));
                    instructionLoadDTO.setInstructionActive(rs.getBoolean("active"));
                    instructionLoadDTO.setBlockMarked(rs.getBoolean("block_marked"));
                    instructionLoadDTO.setCodified(rs.getBoolean("codified"));
                    instructionLoadDTO.setDefaultValue(rs.getString("default_value"));
                    instructionLoadDTO.setDescription(rs.getString("description"));
                    instructionLoadDTO.setExportToABR(rs.getBoolean("export_to_abr"));
                    instructionLoadDTO.setInstructionOrderNumber(rs.getInt("instruction_order_number"));
                    instructionLoadDTO.setInstructionName(rs.getString("name"));
                    instructionLoadDTO.setOnHoldSeconds(rs.getInt("on_hold_seconds"));
                    instructionLoadDTO.setOperation(rs.getString("operation"));
                    instructionLoadDTO.setOptional(rs.getBoolean("optional"));
                    instructionLoadDTO.setXpath(rs.getString("xpath"));
                    instructionLoadDTO.setCoordinates(rs.getString("coordinates"));
                    instructionLoadDTO.setForceCoordinates(rs.getBoolean("force_coordinates"));
                    instructionLoadDTO.setIFrameXPath(rs.getString("iframe_xpath"));

                    instructionLoadDTO.setTagName(rs.getString("tag_name"));
                    instructionLoadDTO.setShadowHost(rs.getString("shadow_host"));
                    instructionLoadDTO.setShadowRoot(rs.getString("shadow_root"));
                    instructionLoadDTO.setCssSelector(rs.getString("css_selector"));

                    instructionLoadDTO.setVariableId(rs.getInt("variable_id"));
                    instructionLoadDTO.setParentBlockId(rs.getInt("parent_block_id"));
                    instructionLoadDTO.setParentId(rs.getInt("parent_id"));
                    instructionLoadDTO.setBlockId(rs.getInt("block_id"));

                    // Conditional mapping for IDs
                    if ("instruction".equalsIgnoreCase(tableName)) {
                        instructionLoadDTO.setBotJobId(rs.getInt("bot_job_id"));
                    } else {
                        instructionLoadDTO.setHomeBankingId(rs.getInt("home_banking_id"));
                    }

                    instructionLoadDTOList.add(instructionLoadDTO);
                }
            }
        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error getting All Excel data GOTO from table %s whereId %d. Error: %s",
                            tableName, whereId, error.getMessage()));
        }

        return instructionLoadDTOList;
    }

    public ErrorMessage createVariable(VariableUserDTO user) {
        String tableName = "variable";
        String insertSQL = "INSERT INTO " + tableName
                + " (type, Name, Value, bot_job_id, instruction_id, local_format, delimiter) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
                Statement idStmtBefore = conn.createStatement();
                Statement idStmtAfter = conn.createStatement();
                PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            conn.setAutoCommit(false); // Disable auto-commit

            // Step 1: Get IDs before insertion
            List<Integer> idsBefore = new ArrayList<>();
            try (ResultSet rsBefore = idStmtBefore.executeQuery("SELECT ID FROM " + tableName + " ORDER BY ID")) {
                while (rsBefore.next()) {
                    idsBefore.add(rsBefore.getInt("ID"));
                }
            }

            // Step 2: Insert new variable record
            pstmt.setString(1, user.getType());
            pstmt.setString(2, user.getName());
            pstmt.setString(3, user.getValue());
            pstmt.setInt(4, user.getBotJobId());
            pstmt.setInt(5, user.getParentId());
            pstmt.setString(6, user.getLocalFormat());
            pstmt.setString(7, user.getDelimiter());

            pstmt.addBatch();
            pstmt.executeBatch();

            // Step 3: Get IDs after insertion
            idsVariableAfter.clear();
            try (ResultSet rsAfter = idStmtAfter.executeQuery("SELECT ID FROM " + tableName + " ORDER BY ID")) {
                while (rsAfter.next()) {
                    idsVariableAfter.add(rsAfter.getInt("ID"));
                }
            }

            // Step 4: Keep only the new IDs
            idsVariableAfter.removeAll(idsBefore);

            ARLogger.getInstance(PerformDataBase.class)
                    .info(String.format("Variable inserted successfully. New IDs: %s", idsVariableAfter));

            conn.commit(); // Commit transaction
            return null; // Success

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("createVariable - Error: %s", error.getMessage()));

            return new ErrorMessage("Variable Insertion Error", "Error inserting a new variable.", error.getMessage());
        }
    }

    public void updateUserData(Integer userId, VariableUserDTO user) {
        //        try {
        String updateSQL = "UPDATE variable SET Name = '" + user.getName() + "', "
                + " type = '" + user.getType() + "', "
                + " value = '" + user.getValue() + "', "
                + " local_format = '" + user.getLocalFormat() + "', "
                + " delimiter = '" + user.getDelimiter() + "' "
                + " WHERE ID = " + userId;
        try (Statement stmt = getConnection().createStatement()) {
            int rowsAffected = stmt.executeUpdate(updateSQL);
            if (rowsAffected > 0) {
                System.out.println("Data updated successfully.");
            } else {
                System.out.println("No matching record found to update.");
            }
        } catch (SQLException error) {
            System.out.println(error.getMessage());
        }
    }

    public void deleteUserData(String Id) {
        try {
            int variableId = Integer.parseInt(Id);
            String deleteSQL = "DELETE FROM variable WHERE ID = " + variableId;
            try (Statement stmt = getConnection().createStatement()) {
                int rowsAffected = stmt.executeUpdate(deleteSQL);
                if (rowsAffected > 0) {
                    System.out.println("Data deleted successfully.");
                } else {
                    System.out.println("No matching record found to delete.");
                }
            } catch (SQLException error) {
                System.out.println(error.getMessage());
            }
        } catch (NumberFormatException error) {
            System.out.println(error.getMessage());
        }
    }

    private InstructionOperationDTO buildFromLoadDTO(InstructionOperationDTO dto) {
        return InstructionOperationDTO.builder()
                .id(dto.getId())
                .blockId(dto.getBlockId())
                .botJobId(dto.getBotJobId())
                .homeBankingId(dto.getHomeBankingId())
                .name(dto.getName())
                .description(dto.getDescription())
                .actions(dto.getActions())
                .operation(dto.getOperation())
                .instructionOrderNumber(dto.getInstructionOrderNumber())
                .actionCustomMaxWaitSec(dto.getActionCustomMaxWaitSec())
                .onHoldSeconds(dto.getOnHoldSeconds())
                .variableId(dto.getVariableId())
                .parentBlockId(dto.getParentBlockId())
                .parentId(dto.getParentId())
                .instructionActive(dto.getInstructionActive())
                .blockActive(dto.getBlockActive())
                .refreshLoop(dto.getRefreshLoop())
                .loopOnly(dto.getLoopOnly())
                .build();
    }

    public ErrorMessage loadInstructions(int whereID, int blockId, int instrucId, String tableName) {
        List<InstructionLoadDTO> instructions = new ArrayList<>();

        // Validate table name to avoid SQL injection
        List<String> allowedTables = Arrays.asList("instruction", "component_instruction");
        if (!allowedTables.contains(tableName)) {
            return new ErrorMessage(
                    "Invalid Table Name", "tableName must be 'instruction' or 'component_instruction'", tableName);
        }

        // Determine filter column
        String whereColumn = tableName.equals("component_instruction") ? "home_banking_id" : "bot_job_id";

        // Build query dynamically
        StringBuilder querySQL = new StringBuilder("SELECT * FROM ")
                .append(tableName)
                .append(" WHERE ")
                .append(whereColumn)
                .append(" = ?");
        if (blockId > 0) {
            querySQL.append(" AND block_id = ?");
        }
        if (instrucId > -1) {
            querySQL.append(" AND id = ?");
        }
        querySQL.append(" ORDER BY instruction_order_number ASC");

        // Choose target list depending on tableName
        List<InstructionLoadDTO> targetList;
        if ("instruction".equals(tableName)) {
            performLists.getListInstruction().clear();
            targetList = performLists.getListInstruction();
        } else {
            performLists.getListInstructionComp().clear();
            targetList = performLists.getListInstructionComp();
        }

        try (PreparedStatement pstmt = getConnection().prepareStatement(querySQL.toString())) {
            int paramIndex = 1;
            pstmt.setInt(paramIndex++, whereID);

            if (blockId > 0) {
                pstmt.setInt(paramIndex++, blockId);
            }
            if (instrucId > -1) {
                pstmt.setInt(paramIndex++, instrucId);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    InstructionLoadDTO instruction = new InstructionLoadDTO();

                    instruction.setInstructionId(rs.getInt("id"));

                    if (tableName.equals("component_instruction")) {
                        instruction.setHomeBankingId(rs.getInt("home_banking_id"));
                    } else {
                        instruction.setBotJobId(rs.getInt("bot_job_id"));
                    }

                    instruction.setBlockId(rs.getInt("block_id"));
                    instruction.setInstructionName(rs.getString("name"));
                    instruction.setInstructionOrderNumber(rs.getInt("instruction_order_number"));

                    instruction.setActions(rs.getString("actions"));
                    instruction.setXpath(rs.getString("xpath"));
                    instruction.setCoordinates(rs.getString("coordinates"));
                    instruction.setForceCoordinates(rs.getBoolean("force_coordinates"));
                    instruction.setIFrameXPath(rs.getString("iframe_xpath"));
                    instruction.setTagName(rs.getString("tag_name"));
                    instruction.setShadowHost(rs.getString("shadow_host"));
                    instruction.setShadowRoot(rs.getString("shadow_root"));
                    instruction.setCssSelector(rs.getString("css_selector"));
                    instruction.setDescription(rs.getString("description"));
                    instruction.setOptional(rs.getBoolean("optional"));
                    instruction.setActionCustomMaxWaitSec(rs.getInt("action_custom_max_wait_sec"));
                    instruction.setOnHoldSeconds(rs.getInt("on_hold_seconds"));
                    instruction.setCodified(rs.getBoolean("codified"));
                    instruction.setExportToABR(rs.getBoolean("export_to_abr"));
                    instruction.setInstructionActive(rs.getBoolean("active"));

                    if (hasColumn(rs, "variable_id")) {
                        instruction.setVariableId(rs.getInt("variable_id"));
                    }
                    if (hasColumn(rs, "parent_id")) {
                        instruction.setParentId(rs.getInt("parent_id"));
                    }

                    instructions.add(instruction);
                    targetList.add(instruction);
                }

                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format(
                                "Fetched %d instructions from table %s%s%s",
                                instructions.size(),
                                tableName,
                                blockId > 0 ? " for Block ID " + blockId : "",
                                instrucId > -1 ? " and Instruction ID " + instrucId : ""));
            }
            return null;
        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error fetching instructions from table %s%s%s. Error: %s",
                            tableName,
                            blockId > 0 ? " for Block ID " + blockId : "",
                            instrucId > -1 ? " and Instruction ID " + instrucId : "",
                            e.getMessage()));

            return new ErrorMessage(
                    "Load Instructions Error", "Failed to load instructions from table: " + tableName, e.getMessage());
        }
    }

    public List<ReferenceLoadDTO> getReferencesList(
            int whereID, List<InstructionLoadDTO> lstInstruc, String tableName) {
        List<ReferenceLoadDTO> references = new ArrayList<>();

        // Validate table name to avoid SQL injection
        List<String> allowedTables = Arrays.asList("reference", "component_reference");
        if (!allowedTables.contains(tableName)) {
            throw new IllegalArgumentException("Invalid table name: " + tableName);
        }

        // Determine filter column
        String whereColumn = tableName.equals("component_reference") ? "home_banking_id" : "bot_job_id";

        // Collect instruction IDs
        List<Integer> instrucIds = lstInstruc.stream()
                .map(InstructionLoadDTO::getId) // adjust if your DTO uses another method name for ID
                .filter(id -> id != null && id > -1)
                .toList();

        // Build query dynamically
        StringBuilder querySQL = new StringBuilder("SELECT * FROM ")
                .append(tableName)
                .append(" WHERE ")
                .append(whereColumn)
                .append(" = ?");

        if (!instrucIds.isEmpty()) {
            querySQL.append(" AND block_loop_instruction_id IN (")
                    .append(instrucIds.stream().map(i -> "?").collect(Collectors.joining(",")))
                    .append(")");
        }

        querySQL.append(" ORDER BY id ASC");

        try (PreparedStatement pstmt = getConnection().prepareStatement(querySQL.toString())) {
            int paramIndex = 1;
            pstmt.setInt(paramIndex++, whereID);

            // Bind instruction IDs if present
            for (Integer id : instrucIds) {
                pstmt.setInt(paramIndex++, id);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ReferenceLoadDTO reference = new ReferenceLoadDTO();

                    reference.setId(rs.getInt("id"));

                    if (tableName.equals("component_reference")) {
                        reference.setHomeBankingId(rs.getInt("home_banking_id"));
                    } else {
                        reference.setBotJobId(rs.getInt("bot_job_id"));
                    }

                    reference.setReferenceType(rs.getString("reference_type"));
                    reference.setValue(rs.getString("value"));
                    reference.setBlockLoopInstructionId(rs.getInt("block_loop_instruction_id"));

                    references.add(reference);
                }

                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format(
                                "Fetched %d references from table %s where %s=%d and instrucIds=%s",
                                references.size(), tableName, whereColumn, whereID, instrucIds));
            }
        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error fetching references from table %s where %s=%d. Error: %s",
                            tableName, whereColumn, whereID, e.getMessage()));
        }

        return references;
    }

    /**
     * Utility to check if a column exists in ResultSet
     */
    private boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        ResultSetMetaData rsMeta = rs.getMetaData();
        for (int i = 1; i <= rsMeta.getColumnCount(); i++) {
            if (columnName.equalsIgnoreCase(rsMeta.getColumnName(i))) {
                return true;
            }
        }
        return false;
    }

    public ErrorMessage updateColumns() {
        try (Connection conn = getConnection()) {

            // Check and add parent_block_id column if missing in both tables
            addColumnIfNotExists(conn, "instruction", "parent_block_id", "INTEGER");
            addColumnIfNotExists(conn, "component_instruction", "parent_block_id", "INTEGER");

            return null;

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe("Error ensuring parent_block_id column: " + e.getMessage());
            return new ErrorMessage(
                    "Database Column Error", "Error ensuring parent_block_id column exists in tables", e.getMessage());
        }
    }

    /** Helper method to add column if it does not exist */
    private void addColumnIfNotExists(Connection conn, String tableName, String columnName, String columnType)
            throws SQLException {
        boolean columnExists = false;
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, tableName, columnName)) {
            if (rs.next()) {
                columnExists = true;
            }
        }
        if (!columnExists) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType);
            }
        }
    }

    public void preDeleteNullBlocks(String blockTable, int whereId, String instTable) {
        loadBlocks(whereId, "", blockTable);
        loadInstructions(whereId, -1, -1, instTable);

        // Snapshot of previous IDs
        List<Integer> previousIds = (blockTable.equals("block")
                        ? performLists.getListBlock()
                        : performLists.getListBlockComp())
                .stream().map(BlockLoadDTO::getId).filter(Objects::nonNull).toList();

        // Snapshot of previous IDs
        List<Integer> currentIds = (instTable.equals("instruction")
                        ? performLists.getListInstruction()
                        : performLists.getListInstructionComp())
                .stream()
                        .map(InstructionLoadDTO::getBlockId)
                        .filter(Objects::nonNull)
                        .toList();

        List<Integer> restToDeleteIds =
                previousIds.stream().filter(id -> !currentIds.contains(id)).collect(Collectors.toList());

        System.out.println("Blocks To Delete: " + restToDeleteIds.size());
        // Keep at least One for BLOCK TABLE
        List<BlockLoadDTO> listBlocks =
                instTable.equals("instruction") ? performLists.getListBlock() : performLists.getListBlockComp();
        ErrorMessage errorMessage = null;
        if (!restToDeleteIds.isEmpty() && (blockTable.equals("block") && listBlocks.size() > 1)) {
            errorMessage = deleteNullBlocks(blockTable, whereId, restToDeleteIds);
        } else if (errorMessage == null && !restToDeleteIds.isEmpty() && (blockTable.equals("component_block"))) {
            errorMessage = deleteNullBlocks(blockTable, whereId, restToDeleteIds);
        }
        if (errorMessage != null) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe("Error Deleting Nulls Blocks Table:" + blockTable + " Error: "
                            + errorMessage.getErrorMessage());
        }
    }

    public ErrorMessage deleteNullBlocks(String tableName, int whereId, List<Integer> restIds) {
        if (restIds == null || restIds.isEmpty()) {
            return null; // Nothing to delete
        }

        // Determine foreign key column
        String foreignKeyColumn = "block".equalsIgnoreCase(tableName) ? "bot_job_id" : "home_banking_id";

        // Build dynamic delete SQL
        String deleteSQL = "DELETE FROM " + tableName + " WHERE id = ? AND " + foreignKeyColumn + " = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {
                for (Integer id : restIds) {
                    pstmt.setInt(1, id);
                    pstmt.setInt(2, whereId);
                    pstmt.addBatch();
                }

                int[] results = pstmt.executeBatch();
                conn.commit();

                int totalDeleted = Arrays.stream(results).sum();

                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format(
                                "Deleted %d blocks from table %s where %s = %d (IDs: %s)",
                                totalDeleted, tableName, foreignKeyColumn, whereId, restIds));

                return null; // success
            } catch (SQLException e) {
                conn.rollback();

                ARLogger.getInstance(PerformDataBase.class)
                        .severe(String.format(
                                "Error deleting blocks from table %s where %s = %d. IDs: %s. Error: %s",
                                tableName, foreignKeyColumn, whereId, restIds, e.getMessage()));

                return new ErrorMessage(
                        "Delete Blocks Error",
                        "Failed to delete blocks from table " + tableName + " where " + foreignKeyColumn + " = "
                                + whereId,
                        e.getMessage());
            }
        } catch (SQLException ex) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Connection error while deleting blocks from table %s where %s = %d. IDs: %s. Error: %s",
                            tableName, foreignKeyColumn, whereId, restIds, ex.getMessage()));

            return new ErrorMessage("Database Connection Error", "Could not connect to database", ex.getMessage());
        }
    }
}
