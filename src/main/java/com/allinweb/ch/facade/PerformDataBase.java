package com.allinweb.ch.facade;

import com.allinweb.ch.component.model.BlockDetailsDTO;
import com.allinweb.ch.component.model.BlockLoadDTO;
import com.allinweb.ch.component.model.BlockOrderDetailDTO;
import com.allinweb.ch.component.model.BotJobLoadDTO;
import com.allinweb.ch.component.model.ComplexInstructionLoadDTO;
import com.allinweb.ch.component.model.DeleteBlockDTO;
import com.allinweb.ch.component.model.HomeBankingLoadDTO;
import com.allinweb.ch.component.model.HomeUrlDTO;
import com.allinweb.ch.component.model.InstructionLoadDTO;
import com.allinweb.ch.component.model.InstructionReferenceLoadDTO;
import com.allinweb.ch.component.model.ParentOperations;
import com.allinweb.ch.component.model.RollBackBlocksDTO;
import com.allinweb.ch.component.model.RowMoveDTO;
import com.allinweb.ch.component.model.VariableLoadDTO;
import com.allinweb.ch.component.model.VariableUserDTO;
import com.allinweb.ch.persistence.DatabaseUserDTO;
import com.allinweb.ch.util.ARConstants;
import com.allinweb.ch.util.ARLogger;
import com.allinweb.ch.util.ARPropertyEnum;
import com.allinweb.ch.util.ARPropertyManager;
import com.allinweb.ch.util.ComboBoxVars;
import com.allinweb.ch.util.ErrorMessage;
import com.google.common.base.Strings;
import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.Setter;

public class PerformDataBase {

    // Static final variable to hold the singleton instance
    protected static volatile PerformDataBase instance;

    public final DateTimeFormatter FORMAT_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    // Private constructor to prevent instantiation
    private PerformDataBase() {
        // Initialize if necessary
    }

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

    static {
        performMessage = PerformMessage.getInstance();
        arPropertyManager = ARPropertyManager.getInstance();
    }

    public String previousDB;

    @Getter
    @Setter
    public Connection conn = null;

    // Open connection counter
    public int openConnections = 0;

    public final String CONNECTION_TYPE = "jdbc:ucanaccess://";
    public final String CONNECTION_PARAMETERS = ";memory=false;newDatabaseVersion=V2010";

    private TreeMap<Integer, Integer> homeBankMap = new TreeMap<>();
    private TreeMap<Integer, Integer> homeUrlMap = new TreeMap<>();
    private TreeMap<Integer, Integer> botJobMap = new TreeMap<>();
    private TreeMap<Integer, Integer> blockMap = new TreeMap<>();
    private TreeMap<Integer, Integer> instructionMap = new TreeMap<>();
    private TreeMap<Integer, Integer> variableMap = new TreeMap<>();
    private TreeMap<Integer, Integer> referenceMap = new TreeMap<>();

    // Postgres
    public boolean POSTGRES_DB = false;

    @Getter
    private List<HomeBankingLoadDTO> databaseUpds;

    @Getter
    private ObservableList<DatabaseUserDTO> databaseList = FXCollections.observableArrayList();

    @Getter
    private List<HomeUrlDTO> homeURLList = new ArrayList<>();

    @Getter
    private ObservableList<VariableUserDTO> variablesList = FXCollections.observableArrayList();

    @Getter
    private ObservableList<ComboBoxVars> webPageItems = FXCollections.observableArrayList();

    private List<BotJobLoadDTO> botJobLoadList = new ArrayList<>();
    private List<BotJobLoadDTO> botJobLoadCompList = new ArrayList<>();
    private List<BlockLoadDTO> blockLoadList = new ArrayList<>();

    public void initialize(String databaseType) {
        this.previousDB = databaseType;
    }

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

    public void testConnection(String dataBaseType, String dbAccessPath, String dbUrl, String userDB, String userPwd)
            throws SQLException, ClassNotFoundException {
        if (dataBaseType != null && !dataBaseType.equalsIgnoreCase("POSTGRES")) {

            String dbAccessUrl = CONNECTION_TYPE + dbAccessPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;
            ARLogger.getInstance(PerformDataBase.class).info("ACCESS connection URL: " + dbUrl);
            Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
            DriverManager.getConnection(dbAccessUrl);
        } else {
            Class.forName("org.postgresql.Driver");
            DriverManager.getConnection(dbUrl, userDB, userPwd);
        }
    }

    public void changeDbConnection() throws SQLException {
        String dataBaseType = arPropertyManager.getProperty(ARPropertyEnum.DATABASE_TYPE);
        //        if (Strings.isNullOrEmpty(previousDB) || (previousDB != null && !previousDB.equals(dataBaseType))) {
        closeConnection();

        previousDB = dataBaseType;

        if (dataBaseType != null && dataBaseType.equalsIgnoreCase("POSTGRES")) {
            POSTGRES_DB = true;

            //            createTableOpenAIVector();
            //            createTableLLama2AIVector();
            if (doesNotInstructionTableExist()) {
                if (getConn() != null) {
                    initializeMainDatabasePostgres();
                }
            }

        } else {
            POSTGRES_DB = false;

            String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
            String dbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;

            File dbFile = new File(dbPath + ARConstants.FILE_NAME_DB);
            if (!dbFile.exists()) {
                initializeMainDatabaseAccess(dbFile);
            } else {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format("Database '%s' detected!", dbFile.getName()));
            }
        }
        //        }
    }

    public Connection getConnection() throws SQLException {
        //        ARLogger.getInstance(PerformDataBase.class).info("Open Connections Count() : " +
        // getOpenConnectionsCount());

        String dataBaseType = arPropertyManager.getProperty(ARPropertyEnum.DATABASE_TYPE);

        if (dataBaseType != null && dataBaseType.equalsIgnoreCase("POSTGRES")) {
            POSTGRES_DB = true;
        } else {
            POSTGRES_DB = false;
        }

        try {

            // Close previous connection if already exists and is open
            //            if (conn != null && !conn.isClosed()) {
            //                closeConnection(); // Close previous connection before establishing a new one
            //            }

            if (conn == null || conn.isClosed()) {
                if (!POSTGRES_DB) {
                    String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
                    String dbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;
                    ARLogger.getInstance(PerformDataBase.class).info("ACCESS connection URL: " + dbUrl);
                    Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
                    conn = DriverManager.getConnection(dbUrl);
                    conn.setReadOnly(false);
                } else {
                    String dbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
                    String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
                    String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

                    String userData = userDB + " - " + userPwd;

                    ARLogger.getInstance(PerformDataBase.class).info("POSTGRES connection URL: " + dbUrl);
                    ARLogger.getInstance(PerformDataBase.class).info("User Details: " + userData);
                    Class.forName("org.postgresql.Driver");
                    conn = DriverManager.getConnection(dbUrl, userDB, userPwd);
                    conn.setReadOnly(false);
                }
                // Increment the open connection counter
                if (getOpenConnectionsCount() > 10) {
                    this.openConnections = 0;
                }

                incrementOpenConnections();
            }
        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class).severe("getConnection Error: " + error.getMessage());
            String database = POSTGRES_DB ? "Postgress" : "Access";
            performMessage.errorMessage(
                    "Database connection Failed",
                    "<span style='color: #D32F2F; font-weight: bold; font-size: 1.1em;'>An error occurred during the Database connection.</span>",
                    "<span style='font-weight: bold;'>" + database + "</span>.",
                    "<span style='color: #E65100; font-weight: bold;'>Please ensure the Database connections are correct.</span>",
                    "<span style='font-style: italic;'>Details: " + error.getMessage() + "</span>",
                    0);

            throw error;
        } catch (ClassNotFoundException error) {
            ARLogger.getInstance(PerformDataBase.class).severe("Drive DB Class not Found Error: " + error.getMessage());
        }

        //        changeDbConnection(previousDB);

        return conn;
    }

    // Handle DELETE_INSTRUCTION message
    public void deleteInstruction(int botJobId, InstructionLoadDTO deleteInstructionLoadDTO, boolean blockDeletion) {

        if (deleteInstructionLoadDTO.getParentId() != null) {
            List<ParentOperations> listParents = loadParents(
                    botJobId, deleteInstructionLoadDTO.getInstructionId(), deleteInstructionLoadDTO.getParentId());
            if (!listParents.isEmpty()) {

                if (!blockDeletion) {

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
                        return;
                    }
                }

                deleteRowParents(
                        deleteInstructionLoadDTO.getBotJobId(),
                        deleteInstructionLoadDTO.getBlockId(),
                        deleteInstructionLoadDTO.getInstructionId());
            }
        }

        if (deleteInstructionLoadDTO.getInstructionId() > 0) {
            if (deleteVariable(botJobId, deleteInstructionLoadDTO.getInstructionId()))
                if (deleteReferences(botJobId, deleteInstructionLoadDTO.getInstructionId()))
                    if (deleteRow(deleteInstructionLoadDTO)) {
                        deleteNullBlocks(botJobId);
                        updateBlockOrderNumber(selectAllBlocks(deleteInstructionLoadDTO.getBlockId()), true);
                    }
        }
    }

    public boolean deleteVariable(int bot_job_id, int instructionId) {
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

    public List<ParentOperations> loadAllParents(int bot_job_id, int instructionId) {
        List<ParentOperations> parentList = new ArrayList<>();

        try (Statement stmt = getConnection().createStatement()) {

            String selectSQL = MessageFormat.format(
                    """
                    SELECT
                        parent.name as parent_name,
                        child.actions,
                        child.operation,
                        child.name as child_name,
                        child.id
                    FROM instruction AS child
                    LEFT JOIN instruction AS parent ON child.parent_id = parent.id
                    WHERE child.parent_id = {0}
                      AND child.bot_job_id = {1}
                    ORDER BY child.id;
            """,
                    instructionId, bot_job_id);

            try (ResultSet rs = stmt.executeQuery(selectSQL)) {
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

    public List<ParentOperations> loadParents(int bot_job_id, int instructionId, int parentId) {
        List<ParentOperations> parentList = new ArrayList<>();

        try (Statement stmt = getConnection().createStatement()) {

            String selectSQL = MessageFormat.format(
                    """
                    SELECT
                        parent.name as parent_name,
                        child.name as child_name,
                        child.parent_id
                    FROM instruction AS child
                    LEFT JOIN instruction AS parent ON child.parent_id = parent.id
                    WHERE child.id != {0}
                      AND child.parent_id = {1}
                      AND child.bot_job_id = {2}
                    ORDER BY child.id;
            """,
                    parentId, instructionId, bot_job_id);

            try (ResultSet rs = stmt.executeQuery(selectSQL)) {
                while (rs.next()) {
                    String name = (rs.getString("child_name") + " --> (" + rs.getString("parent_id") + ")-"
                            + rs.getString("parent_name"));

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

    public List<ParentOperations> loadParentsComp(int homeBankId, int blockId, int instructionId, int parentId) {
        List<ParentOperations> parentList = new ArrayList<>();

        try (Statement stmt = getConnection().createStatement()) {

            String selectSQL = MessageFormat.format(
                    """
                    SELECT
                        parent.name as parent_name,
                        child.name as child_name,
                        child.parent_id
                    FROM component_instruction AS child
                    LEFT JOIN component_instruction AS parent ON child.parent_id = parent.id
                    WHERE child.id != {0}
                      AND child.parent_id = {1}
                      AND child.home_banking_id = {2}
                      AND child.block_id = {3}
                    ORDER BY child.id;
            """,
                    parentId, instructionId, homeBankId, blockId);

            try (ResultSet rs = stmt.executeQuery(selectSQL)) {
                while (rs.next()) {
                    String name = (rs.getString("child_name") + " --> (" + rs.getString("parent_id") + ")-"
                            + rs.getString("parent_name"));

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
                                "Loaded parents for instruction ID %d from homeBankId %d and blockId %d",
                                instructionId, homeBankId, blockId));
            } else {
                ARLogger.getInstance(PerformDataBase.class)
                        .warning(String.format(
                                "No parents found for instruction ID %d in homeBankId %d and blockId %d.",
                                instructionId, homeBankId, blockId));
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error loading component parents for instruction ID %d from homeBankId %d and blockId %d. Error: %s",
                            instructionId, homeBankId, blockId, e.getMessage()));
        }

        return parentList;
    }

    public boolean deleteCompVariable(InstructionLoadDTO deleteInstructionLoadDTO) {
        // Validate input
        if (deleteInstructionLoadDTO == null || deleteInstructionLoadDTO.getInstructionId() <= 0) {
            ARLogger.getInstance(PerformDataBase.class)
                    .warning("Invalid Instruction provided. Skipping reference deletion.");
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

    public boolean deleteReferences(int botJobId, int instructionId) {
        String deleteSQL =
                "DELETE FROM reference WHERE " + " bot_job_id = " + botJobId + " and instruction_id = " + instructionId;

        try (Statement stmt = getConnection().createStatement()) {
            int rowsAffected = stmt.executeUpdate(deleteSQL);
            if (rowsAffected > 0) {
                System.out.println("Data deleted successfully.");
            } else {
                System.out.println("No matching record found to delete.");
            }
            return true;
        } catch (SQLException error) {
            //            dataBaseInUse(error.getMessage());
            return true;
        }
    }

    public boolean deleteCompReferences(InstructionLoadDTO deleteInstructionLoadDTO) {
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
                                "Deleted all component_references for Instruction ID %d.",
                                deleteInstructionLoadDTO.getInstructionId()));
            } else {
                ARLogger.getInstance(PerformDataBase.class)
                        .warning(String.format(
                                "No component_references found for Instruction ID %d.",
                                deleteInstructionLoadDTO.getInstructionId()));
            }
            return true;

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error deleting component_references for Instruction ID %d. Error: %s",
                            deleteInstructionLoadDTO.getInstructionId(), e.getMessage()));
        }
        return false;
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

    public boolean deleteRowParents(int botJobId, int blockId, int parentId) {
        // Build the SQL delete statement
        String deleteSQL = MessageFormat.format(
                """
                DELETE FROM instruction
                 WHERE parent_id = {0}
                 AND bot_job_id = {1}
                 AND block_id = {2}
                 """,
                parentId, botJobId, blockId);

        try (Statement stmt = getConnection().createStatement()) {
            int rowsAffected = stmt.executeUpdate(deleteSQL);
            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format(
                                "Deleted %d parents - parent with ID %d - bot job %d - block %d. ",
                                rowsAffected, parentId, botJobId, blockId));
            }
            return true;

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error deleting parent ID %d from bot job %d block %d. Error: %s",
                            parentId, botJobId, blockId, e.getMessage()));
        }
        return false;
    }

    public boolean deleteCompRowParents(int homeBankId, int blockId, int parentId) {
        // Build the SQL delete statement
        String deleteSQL = MessageFormat.format(
                """
                DELETE FROM component_instruction
                 WHERE parent_id = {0}
                 AND home_banking_id = {1}
                 AND block_id = {2}
                 """,
                parentId, homeBankId, blockId);

        try (Statement stmt = getConnection().createStatement()) {
            int rowsAffected = stmt.executeUpdate(deleteSQL);
            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format(
                                "Deleted %d parents - parent with ID %d - home bank %d - block %d.",
                                rowsAffected, parentId, homeBankId, blockId));
            }
            return true;

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error deleting parent ID %d from home bank %d - block %d. Error: %s",
                            parentId, homeBankId, blockId, e.getMessage()));
        }
        return false;
    }

    public boolean deleteCompInstruction(InstructionLoadDTO deleteInstructionLoadDTO) {
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
                    "DELETE FROM component_instruction WHERE (id = ? OR parent_id = ?) AND block_id = ? AND home_banking_id = ?";
        } else {
            // Simple deletion by instruction ID
            deleteSQL = "DELETE FROM component_instruction WHERE id = ?";
        }

        try (PreparedStatement stmt = getConnection().prepareStatement(deleteSQL)) {
            stmt.setInt(1, deleteInstructionLoadDTO.getInstructionId());

            if (isConditional) {
                stmt.setInt(2, deleteInstructionLoadDTO.getParentId());
                stmt.setInt(3, deleteInstructionLoadDTO.getBlockId());
                stmt.setInt(4, deleteInstructionLoadDTO.getHomeBankingId());
            }

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format(
                                "Deleted component_instruction ID %d%s.",
                                deleteInstructionLoadDTO.getInstructionId(),
                                isConditional ? " and related conditional component_instruction" : ""));
            } else {
                ARLogger.getInstance(PerformDataBase.class)
                        .warning(String.format(
                                "No matching component_instruction found for ID %d.",
                                deleteInstructionLoadDTO.getInstructionId()));
            }

            return true;

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error deleting component_instruction ID %d. Error: %s",
                            deleteInstructionLoadDTO.getInstructionId(), e.getMessage()));
        }
        return false;
    }

    public void deleteNullBlocks(int botJobId) {
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
                //                ARLogger.getInstance(PerformDataBase.class)
                //                        .info(String.format(
                //                                "The %d Nulls Blocks successfully deleted from botJobId %d.",
                // rowsAffected, botJobId));
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error deleting Null Blocks with BotJobId ID %d. Error: %s", botJobId, e.getMessage()));
        }
    }

    public void deleteCompNullBlocks(int homeBanking) {
        // Validate input
        if (homeBanking <= 0) {
            ARLogger.getInstance(PerformDataBase.class)
                    .warning("Invalid home Banking Id provided. Skipping block deletion.");
            return;
        }

        String deleteSQL = "DELETE FROM component_block b " + "WHERE b.home_banking_id = ? "
                + "AND NOT EXISTS ( "
                + "    SELECT 1 FROM component_instruction bli "
                + "    WHERE bli.block_id = b.id );";

        try (PreparedStatement stmt = getConnection().prepareStatement(deleteSQL)) {
            stmt.setInt(1, homeBanking);

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

    public void updateBlockOrderNumber(List<BlockOrderDetailDTO> blockOrderDetailDTOList, boolean reorderAll) {
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
                    //                    ARLogger.getInstance(PerformDataBase.class)
                    //                            .info(String.format(
                    //                                    "Block Order Number updated blockId: %s, newBlockOrderNumber:
                    // %s",
                    //                                    blockOrderDetailDTO.getBlockId(), newOrderNumber));
                } else {
                    //                    ARLogger.getInstance(PerformDataBase.class)
                    //                            .warning(String.format(
                    //                                    "UpdateBlockOrderNumber - No matching record found to update
                    // botJobId: %d blockId: %d",
                    //                                    blockOrderDetailDTO.getBotJobId(),
                    // blockOrderDetailDTO.getBlockId()));
                }

                newOrderNumber++; // Increment the new order number for the next block
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("Error UpdateBlockOrderNumber. Error: %s", e.getMessage()));
        }
    }

    public void updateCompBlockOrderNumber(List<BlockOrderDetailDTO> blockOrderDetailDTOList, boolean reorderAll) {
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
                        + " AND home_banking_id = " + blockOrderDetailDTO.getHomeBankId();

                int rowsAffected = stmt.executeUpdate(updateSQL);

                if (rowsAffected > 0) {
                    //                    ARLogger.getInstance(PerformDataBase.class)
                    //                            .info(String.format(
                    //                                    "Block Order Number updated blockId: %s, newBlockOrderNumber:
                    // %s",
                    //                                    blockOrderDetailDTO.getBlockId(), newOrderNumber));
                } else {
                    //                    ARLogger.getInstance(PerformDataBase.class)
                    //                            .warning(String.format(
                    //                                    "UpdateBlockOrderNumber - No matching record found to update
                    // botJobId: %d blockId: %d",
                    //                                    blockOrderDetailDTO.getBotJobId(),
                    // blockOrderDetailDTO.getBlockId()));
                }

                newOrderNumber++; // Increment the new order number for the next block
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("Error UpdateCompBlockOrderNumber. Error: %s", e.getMessage()));
        }
    }

    //    public void updateCompBlockOrderNumber(List<BlockOrderDetailDTO> blockOrderDetailDTOList, boolean reorderAll)
    // {
    //        //         Sort the blockOrderDetailDTOList based on the previous blockOrderNumber in ascending order
    //        blockOrderDetailDTOList.sort(Comparator.comparingInt(BlockOrderDetailDTO::getBlockOrderNumber));
    //
    //        try (Statement stmt = getConnection().createStatement()) {
    //            int newOrderNumber = 1; // Start reordering from 1
    //
    //            for (BlockOrderDetailDTO blockOrderDetailDTO : blockOrderDetailDTOList) {
    //                // Update each block's block_order_number starting from 1
    //                String updateSQL = "UPDATE component_block SET block_order_number = "
    //                        + (reorderAll ? newOrderNumber : blockOrderDetailDTO.getBlockOrderNumber())
    //                        + " WHERE id = "
    //                        + blockOrderDetailDTO.getBlockId()
    //                        + " AND bot_job_id = " + blockOrderDetailDTO.getBotJobId();
    //
    //                int rowsAffected = stmt.executeUpdate(updateSQL);
    //
    //                if (rowsAffected > 0) {
    //                    ARLogger.getInstance(PerformDataBase.class)
    //                            .info(String.format(
    //                                    "Block Order Number updated blockId: %s, newBlockOrderNumber: %s",
    //                                    blockOrderDetailDTO.getBlockId(), newOrderNumber));
    //                } else {
    //                    ARLogger.getInstance(PerformDataBase.class)
    //                            .warning(String.format(
    //                                    "updateCompBlockOrderNumber - No matching record found to update botJobId: %d
    // blockId: %d",
    //                                    blockOrderDetailDTO.getBotJobId(), blockOrderDetailDTO.getBlockId()));
    //                }
    //
    //                newOrderNumber++; // Increment the new order number for the next block
    //            }
    //
    //        } catch (SQLException e) {
    //            ARLogger.getInstance(PerformDataBase.class)
    //                    .severe(String.format("Error updateCompBlockOrderNumber. Error: %s", e.getMessage()));
    //        }
    //    }

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
                deleteInstruction(deleteBlockDTO.getBotJobId(), deleteDTO, true);
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
                getCompInstructionsByBlockId(deleteBlockDTO.getHomeBankingId(), deleteBlockDTO.getBlockId());
        if (deleteList.size() > 0) {
            for (InstructionLoadDTO deleteDTO : deleteList) {
                deleteDTO.setHomeBankingId(deleteBlockDTO.getHomeBankingId());
                deleteComponent(deleteBlockDTO.getHomeBankingId(), deleteBlockDTO.getBlockId(), deleteDTO, true);
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
                "INSERT INTO bot_job (id, name, description, home_banking_id, home_url_id, active) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = getConnection().prepareStatement(insertSQL)) {
            pstmt.setInt(1, nextId);
            pstmt.setString(2, createdBotJob.getName());
            pstmt.setString(3, createdBotJob.getName() + " description");
            pstmt.setInt(4, createdBotJob.getHomeBankingId());
            pstmt.setInt(5, createdBotJob.getHomeUrlId());
            pstmt.setInt(6, 1); // Setting "active" as true

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

    public boolean updateInstructionsSplitter(List<InstructionLoadDTO> instructions, int oldBlockId, int newBlockId) {
        // Build the SQL update statement

        try (Statement stmt = getConnection().createStatement()) {
            for (InstructionLoadDTO instruction : instructions) {

                String updateSQL = "UPDATE instruction SET  "
                        + " instruction_order_number = " + instruction.getInstructionOrderNumber() + ","
                        + " block_id = " + newBlockId
                        + " WHERE id = " + instruction.getInstructionId()
                        + " and block_id = " + oldBlockId;

                int rowsAffected = stmt.executeUpdate(updateSQL);
                if (rowsAffected > 0) {
                } else {
                    ARLogger.getInstance(PerformDataBase.class)
                            .warning(String.format(
                                    "updateInstructionsSplitter - No matching record found to update blockId: ",
                                    oldBlockId));
                }
            }
            return true;
        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("This '%s' \n cannot be updated.\nError: %s", oldBlockId, e.getMessage()));
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
        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "This BlockId '%d' \n cannot be updated.\nError: %s",
                            rollBackBlocksDTO.getBlockId(), error.getMessage()));
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

    public Integer loadNexHomeUrlData() {
        //        String selectSQL = "SELECT NEXT_VAL fROM homeBankingSeq";
        String selectSQL = "SELECT MAX(ID) AS max_id FROM home_url";
        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(selectSQL)) {
            while (rs.next()) {
                return rs.getInt("max_id");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
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
                + "  bli.variable_id "
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

            botJobLoadList.clear();

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
                    botJobLoadList.add(botJobDTO);
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
                    instruction.setParentId(rs.getInt("parent_id"));
                    instruction.setVariableId(rs.getInt("variable_id"));

                    instruction.setInstructionActive(rs.getBoolean("instruction_active"));

                    instruction.setInstructionReferenceLoadDTOList(new ArrayList<>());
                    blockDTO.getInstructionLoadDTOS().add(instruction);
                    instructionMapDTO.put(instructionId, instruction);
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
            botJobLoadList.clear();
        }

        return botJobLoadList;
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
                + "    bli.variable_id \n"
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

            botJobLoadCompList.clear();

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
                    botJobLoadCompList.add(botJobDTO);
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
                    instruction.setParentId(rs.getInt("parent_id"));
                    instruction.setVariableId(rs.getInt("variable_id"));

                    instruction.setInstructionActive(rs.getBoolean("instruction_active"));

                    instruction.setInstructionReferenceLoadDTOList(new ArrayList<>());
                    blockDTO.getInstructionLoadDTOS().add(instruction);
                    instructionMapDTO.put(instructionId, instruction);
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
            botJobLoadCompList.clear();
        }

        return botJobLoadCompList;
    }

    public boolean reorderInstructions(List<InstructionLoadDTO> rowList, String tableName) {
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
                String updateSQL = "UPDATE " + tableName + " SET  "
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

    public List<InstructionLoadDTO> getInstructionsByBlockId(int botJobId, int blockId, String tableName) {
        // List to store the fetched instructions
        List<InstructionLoadDTO> instructions = new ArrayList<>();

        // Build the SQL query statement
        String querySQL = "SELECT * FROM " + tableName + " WHERE block_id = " + blockId
                + " order by instruction_order_number ASC";

        // Execute the query and process the result set
        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(querySQL)) {

            while (rs.next()) {
                // Assuming you have an Instruction class, populate it with data from the ResultSet
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
                instruction.setBlockId(rs.getInt("block_id"));
                instruction.setBlockOrderNumber(instruction.getBlockOrderNumber());
                instruction.setBotJobId(botJobId);

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

    public List<InstructionLoadDTO> getCompInstructionsByBlockId(int homeBankId, int blockId) {
        // List to store the fetched instructions
        List<InstructionLoadDTO> instructions = new ArrayList<>();

        // Build the SQL query statement
        String querySQL = "SELECT * FROM component_instruction WHERE home_banking_id = " + homeBankId
                + " and block_id = " + blockId + " order by instruction_order_number ASC";

        // Execute the query and process the result set
        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(querySQL)) {

            while (rs.next()) {
                // Assuming you have an Instruction class, populate it with data from the ResultSet
                InstructionLoadDTO instruction = new InstructionLoadDTO();
                instruction.setInstructionId(rs.getInt("id"));

                instruction.setBlockId(rs.getInt("block_id"));

                instruction.setInstructionName(rs.getString("name"));
                instruction.setInstructionOrderNumber(rs.getInt("instruction_order_number"));
                instruction.setBlockOrderNumber(instruction.getBlockOrderNumber());
                instruction.setHomeBankingId(homeBankId);

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

                instruction.setVariableId(rs.getInt("variable_id"));
                instruction.setParentId(rs.getInt("parent_id"));

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

    public List<InstructionLoadDTO> getInstructionsByBlockId(int botJobId, int blockId) {
        // List to store the fetched instructions
        List<InstructionLoadDTO> instructions = new ArrayList<>();

        // Build the SQL query statement
        String querySQL = "SELECT * FROM instruction WHERE bot_job_id = " + botJobId + " and block_id = " + blockId
                + " order by instruction_order_number ASC";

        // Execute the query and process the result set
        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(querySQL)) {

            while (rs.next()) {
                // Assuming you have an Instruction class, populate it with data from the ResultSet
                InstructionLoadDTO instruction = new InstructionLoadDTO();
                instruction.setInstructionId(rs.getInt("id"));

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

                instruction.setVariableId(rs.getInt("variable_id"));
                instruction.setParentId(rs.getInt("parent_id"));

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

    public InstructionLoadDTO getInstructionById(int botJobId, int instructionId) {
        // List to store the fetched instructions

        // Build the SQL query statement
        String querySQL = "SELECT * FROM instruction WHERE bot_job_id = " + botJobId + " and id = " + instructionId;
        InstructionLoadDTO instruction = null;

        // Execute the query and process the result set
        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(querySQL)) {

            while (rs.next()) {
                // Assuming you have an Instruction class, populate it with data from the ResultSet
                instruction = new InstructionLoadDTO();
                instruction.setId(rs.getInt("id"));

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

                instruction.setVariableId(rs.getInt("variable_id"));
                instruction.setParentId(rs.getInt("parent_id"));
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error fetching Instruction Bot Job ID %d - Instruc %d . Error: %s: ",
                            botJobId, instructionId, e.getMessage()));
        }

        return instruction;
    }

    public List<BotJobLoadDTO> loadAllBotJobs(Connection conn) {
        this.botJobLoadList.clear();
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
bot.active
FROM bot_job bot
LEFT JOIN home_banking hb ON bot.home_banking_id = hb.id
LEFT JOIN home_url hu ON bot.home_url_id = hu.id and hu.home_banking_id = hb.id
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
                    botJobDTO.setHomeUrlId(rs.getInt("home_url_id"));
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
                    .severe(String.format("Error loadAllBotJobs Error: %s", error.getMessage()));
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

            Map<Integer, BotJobLoadDTO> botJobMapDTO = new HashMap<>();
            Map<Integer, BlockLoadDTO> blockMapDTO = new HashMap<>();

            botJobLoadList.clear();

            while (rs.next()) {
                botJobId = rs.getInt("bot_job_id");
                BotJobLoadDTO botJobDTO = botJobMapDTO.get(botJobId);

                if (botJobDTO == null) {
                    botJobDTO = new BotJobLoadDTO();
                    botJobDTO.setId(botJobId);
                    botJobDTO.setName(rs.getString("bot_job_name"));
                    botJobDTO.setBlockLoadDTOList(new ArrayList<>());
                    botJobMapDTO.put(botJobId, botJobDTO);
                    botJobLoadList.add(botJobDTO);
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
                    blockDTO.setActive(rs.getBoolean("active"));
                    blockDTO.setWait(rs.getInt("wait"));

                    blockDTO.setBotJobId(botJobDTO.getId());
                    blockDTO.setBotJobName(botJobDTO.getName());

                    blockDTO.setInstructionLoadDTOS(new ArrayList<>());
                    botJobDTO.getBlockLoadDTOList().add(blockDTO);
                    blockMapDTO.put(blockId, blockDTO);
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
                botJobLoadDTO.setHomeUrlId(rs.getInt("home_url_id"));
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

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format("Error updating Active = 1 all botjobs,  Error: %s", error.getMessage()));
        }

        return false;
    }

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
                    blockDTO.setId(blockId);
                    blockDTO.setBlockOrderNumber(rs.getInt("block_order_number"));
                    blockDTO.setName(rs.getString("block_name"));
                    blockDTO.setDescription(rs.getString("block_description"));
                    blockDTO.setTypeId(rs.getInt("type_id"));
                    blockDTO.setBotJobId(rs.getInt("bot_job_id"));
                    blockDTO.setBotJobName(rs.getString("bot_job_name"));

                    blockMapDTO.put(blockId, blockDTO);
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

    public List<BlockLoadDTO> loadCompBlocksByHomeId(int homeBankingId, int botJobId, String botJobName) {
        // SQL query to get the blocks for a specific bot job
        String query = "SELECT " + "b.id AS block_id, "
                + "b.block_order_number, "
                + "b.name AS block_name, "
                + "b.description AS block_description, "
                + "b.type_id, "
                + "hb.id AS home_banking_id "
                + "FROM home_banking hb "
                + "JOIN component_block b ON b.home_banking_id = hb.id "
                + "JOIN component_instruction ci ON ci.home_banking_id = hb.id and ci.block_id = b.id "
                + "WHERE  hb.id = "
                + homeBankingId + " " + "ORDER BY b.block_order_number ASC";

        // Initialize the necessary data structures
        blockLoadList.clear();
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
                    blockDTO.setId(blockId);
                    blockDTO.setBlockOrderNumber(rs.getInt("block_order_number"));
                    blockDTO.setName(rs.getString("block_name"));
                    blockDTO.setDescription(rs.getString("block_description"));
                    blockDTO.setTypeId(rs.getInt("type_id"));
                    blockDTO.setHomeBankingId(rs.getInt("home_banking_id"));
                    blockDTO.setBotJobId(botJobId);
                    blockDTO.setBotJobName(botJobName);

                    blockMapDTO.put(blockId, blockDTO);
                    blockLoadList.add(blockDTO);
                }
            }
        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error loadBlocksForBotJob for botJobId %d\nError: %s", botJobId, error.getMessage()));
        }

        return blockLoadList;
    }

    public int insertInstruction(
            String typeTask,
            InstructionLoadDTO instructionLoad,
            Integer currentBotJobId,
            Integer currentBlockId,
            Integer homeBankingId) {

        String tableName = "instruction";
        if (typeTask.equals("componentTasks")) {
            tableName = "component_instruction";
        }

        try (Statement stmt = getConnection().createStatement()) {
            Integer nextId = instructionLoad.getId();

            if (typeTask.equals("componentTasks") && nextId == null) {
                nextId = loadNextIdComponentInstruc() + 1;
                instructionLoad.setId(nextId);
            } else if (nextId == null) {
                nextId = loadNextIdInstructionData() + 1;
                instructionLoad.setId(nextId);
            }

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
            addColumnValue.accept("parent_id", instructionLoad.getParentId());
            addColumnValue.accept("variable_id", instructionLoad.getVariableId());
            addColumnValue.accept("block_id", currentBlockId);

            if (typeTask.equals("componentTasks")) {
                addColumnValue.accept("home_banking_id", homeBankingId);
            } else {
                addColumnValue.accept("bot_job_id", currentBotJobId);
            }
            // Add boolean fields with conditional logic
            if (instructionLoad.getBlockMarked() != null) {
                addColumnValue.accept("block_marked", instructionLoad.getBlockMarked() ? 1 : 0);
            }

            if (instructionLoad.getCodified() != null) {
                addColumnValue.accept("codified", instructionLoad.getCodified() ? 1 : 0);
            }

            if (instructionLoad.getExportToABR() != null) {
                addColumnValue.accept("export_to_abr", instructionLoad.getExportToABR() ? 1 : 0);
            }

            if (instructionLoad.getOptional() != null) {
                addColumnValue.accept("optional", instructionLoad.getOptional() ? 1 : 0);
            }

            if (instructionLoad.getInstructionActive() != null) {
                addColumnValue.accept("active", instructionLoad.getInstructionActive() ? 1 : 0);
            }

            if (instructionLoad.getExecuted() != null) {
                addColumnValue.accept("executed", instructionLoad.getExecuted() ? 1 : 0);
            }

            if (instructionLoad.getBlockActive() != null) {
                addColumnValue.accept("block_active", instructionLoad.getBlockActive() ? 1 : 0);
            }

            if (instructionLoad.getRefreshLoop() != null) {
                addColumnValue.accept("refresh_loop", instructionLoad.getRefreshLoop() ? 1 : 0);
            }

            if (instructionLoad.getLoopOnly() != null) {
                addColumnValue.accept("loop_only", instructionLoad.getLoopOnly() ? 1 : 0);
            }

            if (instructionLoad.getForceCoordinates() != null) {
                addColumnValue.accept("force_coordinates", instructionLoad.getForceCoordinates() ? 1 : 0);
            }

            // Uncomment if needed
            // if (instructionLoad.getEditMode() != null) {
            //     addColumnValue.accept("edit_mode", instructionLoad.getEditMode() ? 1 : 0);
            // }

            // Construct final SQL query
            String insertSQL = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columns, values);

            int rowsAffected = stmt.executeUpdate(insertSQL);
            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format(
                                "New %s SAVED SUCCESSFULLY id: %d Name: %s Actions: %s Operation: %s",
                                tableName.toUpperCase(),
                                instructionLoad.getId(),
                                instructionLoad.getName(),
                                instructionLoad.getActions(),
                                instructionLoad.getOperation()));
                return nextId;

            } else {
                ARLogger.getInstance(PerformDataBase.class)
                        .warning(String.format(
                                "%s NOT SAVED\nid: %d Name: %s Actions: %s Operations: %s",
                                tableName.toUpperCase(),
                                instructionLoad.getId(),
                                instructionLoad.getName(),
                                instructionLoad.getActions(),
                                instructionLoad.getOperation()));
                return -1;
            }

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class)
                    .warning(String.format(
                            "%s NOT SAVED id: %d Name: %s Actions: %s Operations: %s",
                            tableName.toUpperCase(),
                            instructionLoad.getId(),
                            instructionLoad.getName(),
                            instructionLoad.getActions(),
                            instructionLoad.getOperation()));
            ARLogger.getInstance(PerformDataBase.class).warning(error.getMessage());
            return -1;
        }
    }

    public int updateInstruction(
            String typeTask,
            InstructionLoadDTO instructionLoadDTO,
            Integer currentBotJobId,
            Integer currentBlockId,
            Integer homeBankingId) {

        String tableName = "instruction";
        if (typeTask.equals("componentTasks")) {
            tableName = "component_instruction";
        }

        try (Statement stmt = getConnection().createStatement()) {
            if (instructionLoadDTO.getId() == null) {
                ARLogger.getInstance(PerformDataBase.class).warning("Instruction ID is null. Update failed.");
                return -1;
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
            addColumnValue.accept("coordinates", instructionLoadDTO.getCoordinates());
            addColumnValue.accept("iframe_xpath", instructionLoadDTO.getIFrameXPath());
            addColumnValue.accept("tag_name", instructionLoadDTO.getTagName());
            addColumnValue.accept("shadow_host", instructionLoadDTO.getShadowHost());
            addColumnValue.accept("shadow_root", instructionLoadDTO.getShadowRoot());
            addColumnValue.accept("css_selector", instructionLoadDTO.getCssSelector());
            addColumnValue.accept("xpath", instructionLoadDTO.getXpath());
            addColumnValue.accept("action_custom_max_wait_sec", instructionLoadDTO.getActionCustomMaxWaitSec());
            addColumnValue.accept("actions", instructionLoadDTO.getActions());
            addColumnValue.accept("default_value", instructionLoadDTO.getDefaultValue());
            addColumnValue.accept("description", instructionLoadDTO.getDescription());
            addColumnValue.accept("instruction_order_number", instructionLoadDTO.getInstructionOrderNumber());
            addColumnValue.accept("name", instructionLoadDTO.getName());
            addColumnValue.accept("on_hold_seconds", instructionLoadDTO.getOnHoldSeconds());
            addColumnValue.accept("operation", instructionLoadDTO.getOperation());
            addColumnValue.accept("parent_id", instructionLoadDTO.getParentId());
            addColumnValue.accept("variable_id", instructionLoadDTO.getVariableId());
            addColumnValue.accept("block_id", currentBlockId);

            if (typeTask.equals("componentTasks")) {
                addColumnValue.accept("home_banking_id", homeBankingId);
            } else {
                addColumnValue.accept("bot_job_id", currentBotJobId);
            }

            // Boolean fields
            addColumnValue.accept(
                    "block_marked",
                    instructionLoadDTO.getBlockMarked() != null ? (instructionLoadDTO.getBlockMarked() ? 1 : 0) : null);
            addColumnValue.accept(
                    "codified",
                    instructionLoadDTO.getCodified() != null ? (instructionLoadDTO.getCodified() ? 1 : 0) : null);
            addColumnValue.accept(
                    "export_to_abr",
                    instructionLoadDTO.getExportToABR() != null ? (instructionLoadDTO.getExportToABR() ? 1 : 0) : null);
            addColumnValue.accept(
                    "optional",
                    instructionLoadDTO.getOptional() != null ? (instructionLoadDTO.getOptional() ? 1 : 0) : null);
            addColumnValue.accept(
                    "active",
                    instructionLoadDTO.getInstructionActive() != null
                            ? (instructionLoadDTO.getInstructionActive() ? 1 : 0)
                            : null);
            addColumnValue.accept(
                    "executed",
                    instructionLoadDTO.getExecuted() != null ? (instructionLoadDTO.getExecuted() ? 1 : 0) : null);
            addColumnValue.accept(
                    "block_active",
                    instructionLoadDTO.getBlockActive() != null ? (instructionLoadDTO.getBlockActive() ? 1 : 0) : null);
            addColumnValue.accept(
                    "refresh_loop",
                    instructionLoadDTO.getRefreshLoop() != null ? (instructionLoadDTO.getRefreshLoop() ? 1 : 0) : null);
            addColumnValue.accept(
                    "loop_only",
                    instructionLoadDTO.getLoopOnly() != null ? (instructionLoadDTO.getLoopOnly() ? 1 : 0) : null);
            addColumnValue.accept(
                    "force_coordinates",
                    instructionLoadDTO.getForceCoordinates() != null
                            ? (instructionLoadDTO.getForceCoordinates() ? 1 : 0)
                            : null);

            if (setClause.isEmpty()) {
                ARLogger.getInstance(PerformDataBase.class)
                        .warning("No fields to update for instruction ID: " + instructionLoadDTO.getId());
                return -1;
            }

            // Construct final SQL query
            String updateSQL =
                    String.format("UPDATE %s SET %s WHERE id = %d", tableName, setClause, instructionLoadDTO.getId());

            int rowsAffected = stmt.executeUpdate(updateSQL);
            if (rowsAffected > 0) {
                ARLogger.getInstance(PerformDataBase.class)
                        .info(String.format(
                                "%s UPDATED SUCCESSFULLY id: %d Name: %s Actions: %s Operation: %s",
                                tableName.toUpperCase(),
                                instructionLoadDTO.getId(),
                                instructionLoadDTO.getName(),
                                instructionLoadDTO.getActions(),
                                instructionLoadDTO.getOperation()));
                return rowsAffected;
            } else {
                ARLogger.getInstance(PerformDataBase.class)
                        .warning(String.format(
                                "%s NOT UPDATED id: %d Name: %s Actions: %s Operations: %s",
                                tableName.toUpperCase(),
                                instructionLoadDTO.getId(),
                                instructionLoadDTO.getName(),
                                instructionLoadDTO.getActions(),
                                instructionLoadDTO.getOperation()));
                return 0;
            }

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class)
                    .warning(String.format(
                            "%s UPDATE FAILED id: %d Name: %s Actions: %s Operations: %s",
                            tableName.toUpperCase(),
                            instructionLoadDTO.getId(),
                            instructionLoadDTO.getName(),
                            instructionLoadDTO.getActions(),
                            instructionLoadDTO.getOperation()));
            return -1;
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

    private Integer loadNextIdComponentInstruc() {
        //        String selectSQL = "SELECT NEXT_VAL fROM homeBankingSeq;
        String selectSQL = "SELECT MAX(ID) AS max_id FROM component_instruction";
        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(selectSQL)) {
            while (rs.next()) {
                return rs.getInt("max_id");
            }
        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("loadNextIdCompInstruc  \nError: " + e.getMessage());
        }
        return null;
    }

    public boolean preInsertStep(RowMoveDTO rowMoveDTO, List<InstructionLoadDTO> rowList, String tableName) {
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
                        String updateSQL = "UPDATE " + tableName + " SET  "
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
            boolean isShowAlert,
            boolean updateRow,
            boolean blockIdChanged) {

        //        this.botJobLoadDTO = loadBotJobById(rowMoveDTO.getBotJobId());

        if (!updateRow || blockIdChanged) {
            List<InstructionLoadDTO> rowList = null;
            String tableName = "instruction";
            if (rowMoveDTO.getSessionId().equals("componentTasks")) {
                tableName = "component_instruction";
            }

            rowList = getInstructionsByBlockId(rowMoveDTO.getBotJobId(), rowMoveDTO.getBlockId(), tableName);

            reorderInstructions(rowList, tableName);

            preInsertStep(rowMoveDTO, rowList, tableName);
        }

        List<BlockLoadDTO> matchingBlocks = null;

        this.botJobLoadList = loadBotJobAndBlocks(rowMoveDTO.getBotJobId());
        this.blockLoadList = loadBlocksByBotJobId(rowMoveDTO.getBotJobId());

        if (!rowMoveDTO.getUpdatedRows().isEmpty()) {

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
        //        List<InstructionLoadDTO> finalInstructionList = rowList;

        InstructionLoadDTO instruction = new InstructionLoadDTO();

        instruction.setName(name);

        instruction.setCodified(false);
        instruction.setExportToABR(false);
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
        instruction.setOptional(false);

        instruction.setOperation(operation);
        instruction.setActions(actions);
        instruction.setDescription(description);

        instruction.setVariableId(varId);

        Integer nextId = -1;
        if (!updateRow) {
            if (rowMoveDTO.getSessionId().equals("componentTasks")) {
                nextId = loadNextIdComponentInstruc() + 1;
            } else {
                nextId = loadNextIdInstructionData() + 1;
            }
        } else {
            nextId = rowMoveDTO.getUpdatedRows().get(0).getInstructionId();
        }

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

        // Define where to get the BlockId
        //        instruction.setBlockId(rowMoveDTO.getBotJobId());
        if (!rowMoveDTO.getSessionId().equals("componentTasks")) {
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

                int newBlockId = createNewBlock(newBlockDetails);

                if (newBlockId > 0) {

                    // IT SETS THE NEW TARGET IN CASE TO ADD MORE INSTRUCTIONS
                    rowMoveDTO.setBlockId(newBlockId);

                    this.blockLoadList = loadBlocksByBotJobId(rowMoveDTO.getBotJobId());

                    instruction.setBlockId(newBlockId);
                }
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
            if (!updateRow) {
                response = insertInstruction(
                        rowMoveDTO.getSessionId(),
                        instruction,
                        rowMoveDTO.getBotJobId(),
                        currentBlockId,
                        rowMoveDTO.getHomeBankingId());
            } else {
                response = updateInstruction(
                        rowMoveDTO.getSessionId(),
                        instruction,
                        rowMoveDTO.getBotJobId(),
                        currentBlockId,
                        rowMoveDTO.getHomeBankingId());
            }

            if (!updateRow || blockIdChanged) {
                int targetOrderNumber = rowMoveDTO.getUpdatedRows().get(0).getInstructionOrderNumber();
                rowMoveDTO.getUpdatedRows().get(0).setInstructionOrderNumber(targetOrderNumber + 1);
            }

            int finalResponse = response;
            if (isShowAlert) {
                if (finalResponse > -1) {

                    ARLogger.getInstance(PerformDataBase.class)
                            .info(String.format(
                                    "\"Component\" Instruction: \"%s\" has been added successfully!",
                                    instruction.getName()));
                } else {

                    ARLogger.getInstance(PerformDataBase.class)
                            .severe(String.format(
                                    "Error Add New \"Component\" Instruction: \"%s\" Cannot be saved!",
                                    instruction.getName()));
                }
            }

            if (response > -1) {
                return response;
            }

        } catch (Exception e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Cannot Insert Instruction\nError: " + e.getMessage());
        }

        return -1;
    }

    public List<HomeBankingLoadDTO> loadHomeBanking(Integer homeBankingId) {
        Map<Integer, HomeBankingLoadDTO> homeBankingMap = new HashMap<>();

        StringBuilder selectSQLBuilder = new StringBuilder();
        selectSQLBuilder.append("SELECT hb.id AS hb_id, hb.cookies, hb.driver_session, hb.name, hb.options_config, ");
        selectSQLBuilder.append("hb.password, hb.priority, hb.search_config, hb.url AS hb_url, hb.username, ");
        selectSQLBuilder.append("hu.id AS hu_id, hu.url AS hu_url, hu.home_banking_id ");
        selectSQLBuilder.append("FROM home_banking hb ");
        selectSQLBuilder.append("LEFT JOIN home_url hu ON hb.id = hu.home_banking_id ");

        if (homeBankingId != null) {
            selectSQLBuilder.append("WHERE hb.id = ? "); // Add a space before WHERE
        }
        selectSQLBuilder.append("ORDER BY hb.id, hu.id");

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(selectSQLBuilder.toString())) {

            if (homeBankingId != null) {
                pstmt.setInt(1, homeBankingId);
            }

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Integer currentHomeBankingId = rs.getInt("hb_id");

                HomeBankingLoadDTO homeBanking = homeBankingMap.get(currentHomeBankingId);
                if (homeBanking == null) {
                    homeBanking =
                            new HomeBankingLoadDTO(); // This will use the @NoArgsConstructor and the list will be null
                    // OR, if you implement the full constructor, use it:
                    // new HomeBankingLoadDTO(id, url, name, etc.)
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
                    homeBanking.setHomeUrlDTOs(
                            new ArrayList<>()); // <--- IMPORTANT: Initialize the list here if not done in constructor
                    homeBankingMap.put(currentHomeBankingId, homeBanking);
                }

                int homeUrlId = rs.getInt("hu_id");
                if (!rs.wasNull()) {
                    String homeUrlUrl = rs.getString("hu_url");
                    int homeUrlHomeBankingId = rs.getInt("home_banking_id");
                    HomeUrlDTO urlDTO = new HomeUrlDTO(homeUrlId, homeUrlUrl, homeUrlHomeBankingId);
                    homeBanking.getHomeUrlDTOs().add(urlDTO); // Access the list via getter and add
                    // OR if you added the addHomeUrlDTO method in HomeBankingLoadDTO:
                    // homeBanking.addHomeUrlDTO(urlDTO);
                }
            }

        } catch (SQLException e) {
            String message = homeBankingId != null
                    ? String.format(
                            "Error selecting home banking record with ID %d. Error: %s", homeBankingId, e.getMessage())
                    : "Error selecting ALL home banking records";
            ARLogger.getInstance(PerformDataBase.class).severe(message);
        }

        return new ArrayList<>(homeBankingMap.values());
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
                + "  bli.operation,      "
                + "  bli.tag_name      "
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
                String tagName = rs.getString("tag_name").trim();
                Integer orderNumber = rs.getInt("instruction_order_number");

                // Filter out "SET", "GET", "CK", adn "H"
                if (actions != null
                        && !actions.equalsIgnoreCase(ARConstants.SET_VALUE)
                        && !actions.equalsIgnoreCase(ARConstants.GET_VALUE)
                        && !actions.equalsIgnoreCase(ARConstants.CHECK_VALUE)
                        && !actions.equalsIgnoreCase(ARConstants.HOLD)) {
                    webPageItems.add(new ComboBoxVars(
                            "(" + id + ")" + name, name, id, blockId, -1, -1, tagName, orderNumber, null));
                }
            }
        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "loadWebPageFields - Error selecting Web Page Fields. Error: %s", e.getMessage()));
        }
        return webPageItems;
    }

    public ObservableList<ComboBoxVars> loadCompWebPageFields(int homeBankingId) {
        webPageItems.clear();
        String selectSQL = " SELECT  "
                + "  hb.id AS home_banking_id,  "
                + "  b.id AS block_id,  "
                + "  bli.id AS instruction_id,  "
                + "  bli.instruction_order_number,  "
                + "  bli.actions,  "
                + "  bli.name AS instruction_name,  "
                + "  bli.xpath,  "
                + "  bli.operation,      "
                + "  bli.tag_name      "
                + " FROM home_banking hb  "
                + " LEFT JOIN component_block b ON b.home_banking_id = hb.id  "
                + " JOIN component_instruction bli ON bli.block_id = b.id  "
                + " where hb.id = " + homeBankingId
                + "   and operation is null  "
                + "  ORDER BY hb.id, b.block_order_number, bli.instruction_order_number ASC;";

        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(selectSQL)) {
            while (rs.next()) {
                int id = rs.getInt("instruction_id");
                String name = rs.getString("instruction_name").trim();
                String actions = rs.getString("actions").trim();
                Integer blockId = rs.getInt("block_id");
                String tagName = rs.getString("tag_name").trim();
                Integer orderNumber = rs.getInt("instruction_order_number");

                // Filter out "SET", "GET", "CK", adn "H"
                if (actions != null
                        && !actions.equalsIgnoreCase(ARConstants.SET_VALUE)
                        && !actions.equalsIgnoreCase(ARConstants.GET_VALUE)
                        && !actions.equalsIgnoreCase(ARConstants.CHECK_VALUE)
                        && !actions.equalsIgnoreCase(ARConstants.HOLD)) {
                    webPageItems.add(new ComboBoxVars(
                            "(" + id + ")" + name, name, id, blockId, -1, -1, tagName, orderNumber, null));
                }
            }
        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "loadCompWebPageFields - Error selecting Component Web Page Fields. Error: %s",
                            e.getMessage()));
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

    public List<VariableLoadDTO> instVariablesToDuplicateNEW(
            Connection conn, int homeBankingId, int oldBotJobId, int oldBlockId, String targetTable)
            throws SQLException {

        // Determine if we need to use home_banking_id instead of bot_job_id
        String idColumn = targetTable.equals("component_variable") ? "home_banking_id" : "bot_job_id";

        // Build the base query
        String query =
                "SELECT var.id, var.name, var.type, var.value, var.instruction_id, var.local_format, var.delimiter, var."
                        + idColumn;

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
                if (targetTable.equals("component_variable")) {
                    stmt.setInt(1, homeBankingId);
                    stmt.setInt(2, oldBlockId);
                } else if (targetTable.equals("variable")) {
                    stmt.setInt(1, oldBotJobId);
                    stmt.setInt(2, oldBlockId);
                }
            } else {
                if (targetTable.equals("component_variable")) {
                    stmt.setInt(1, homeBankingId);
                } else if (targetTable.equals("variable")) {
                    stmt.setInt(1, oldBotJobId);
                }
            }

            // Execute the query and process results
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {

                int id = rs.getInt("id");
                int botJobId = -1;
                homeBankingId = -1;
                if (targetTable.equalsIgnoreCase("instruction")) {
                    botJobId = rs.getInt("bot_job_id");
                } else if (targetTable.equalsIgnoreCase("component_instruction")) {
                    homeBankingId = rs.getInt("home_banking_id");
                }

                int instructionId = rs.getInt("instruction_id");
                String type = rs.getString("type");
                String name = rs.getString("name");
                String value = rs.getString("value");
                String localFormat = rs.getString("local_format");
                String delimiter = rs.getString("delimiter");
                //                int usedVars = rs.getInt("UsedVars");
                variableDTOList.add(new VariableLoadDTO(
                        id, homeBankingId, botJobId, instructionId, type, name, value, localFormat, delimiter, 0));
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

    public List<InstructionReferenceLoadDTO> referenceToDuplicateNew(
            Connection conn, int homeBankingId, int oldBotJobId, int oldBlockId, String table1, String table2)
            throws SQLException {

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
                if (table1.equals("component_reference")) {
                    stmt.setInt(1, homeBankingId);
                    stmt.setInt(2, oldBlockId);
                } else if (table1.equals("reference")) {
                    stmt.setInt(1, oldBotJobId);
                    stmt.setInt(2, oldBlockId);
                }
            } else {
                if (table1.equals("component_reference")) {
                    stmt.setInt(1, homeBankingId);
                } else if (table1.equals("reference")) {
                    stmt.setInt(1, oldBotJobId);
                }
            }

            // Execute the query and process the results
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                InstructionReferenceLoadDTO referenceDTO = new InstructionReferenceLoadDTO();
                referenceDTO.setId(rs.getInt("id"));
                referenceDTO.setReferenceType(rs.getString("reference_type"));
                referenceDTO.setValue(rs.getString("value"));
                referenceDTO.setBlockLoopInstructionId(rs.getInt("instruction_id"));

                if (table1.equalsIgnoreCase("reference")) {
                    referenceDTO.setBotJobId(rs.getInt("bot_job_id"));
                } else if (table1.equalsIgnoreCase("component_reference")) {
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
        String botJobIdColumn =
                table2.equalsIgnoreCase("component_block") ? "" : "and blk.bot_job_id = bli.bot_job_id ";

        // Build the query
        String query = "SELECT bli.id, bli.action_custom_max_wait_sec, bli.actions, bli.active, bli.block_marked, "
                + "bli.codified, bli.default_value, bli.description, bli.export_to_abr, bli.instruction_order_number, "
                + "bli.name, bli.on_hold_seconds, bli.operation, bli.optional, bli.parent_id, bli.xpath, bli.coordinates, "
                + "bli.iframe_xpath,  bli.tag_name, bli.shadow_host, bli.shadow_root, bli.css_selector, bli.force_coordinates, "
                + "bli.variable_id, bli.block_id, blk.block_order_number, bli."
                + idColumn;

        query += " FROM " + table1 + " bli "
                + " JOIN " + table2 + " blk ON bli.block_id = blk.id " + botJobIdColumn
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
                if (table1.equals("component_instruction")) {
                    stmt.setInt(1, homeBankingId);
                    stmt.setInt(2, oldBlockId);
                } else if (table1.equals("instruction")) {
                    stmt.setInt(1, oldBotJobId);
                    stmt.setInt(2, oldBlockId);
                }
            } else {
                if (table1.equals("component_instruction")) {
                    stmt.setInt(1, homeBankingId);
                } else if (table1.equals("instruction")) {
                    stmt.setInt(1, oldBotJobId);
                }
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

                InstructionLoadDTO.setTagName(rs.getString("tag_name"));
                InstructionLoadDTO.setShadowHost(rs.getString("shadow_host"));
                InstructionLoadDTO.setShadowRoot(rs.getString("shadow_root"));
                InstructionLoadDTO.setCssSelector(rs.getString("css_selector"));

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
            int homeUrlId,
            int oldBotJobId,
            int newBotJobId,
            String newName,
            String newDescription,
            String[] arrayTables) {

        String botJobInsertQuery =
                "INSERT INTO bot_job (id, name, description, priority, home_banking_id, home_url_id, active) "
                        + "SELECT ?, ?, ?, priority, home_banking_id, ?, ? FROM bot_job WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(botJobInsertQuery)) {
            stmt.setInt(1, newBotJobId); // Set new name
            stmt.setString(2, newName); // Set new name
            stmt.setString(3, newDescription); // Set new description
            stmt.setInt(4, homeUrlId); //
            stmt.setInt(5, 1); //
            stmt.setInt(6, oldBotJobId); // Set original botJobId for the SELECT query
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
                instVariablesToDuplicateNEW(conn, homeBankId, newBotJobId, oldBlockId, arrayTables[6]); // variable

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
            ErrorMessage errorMessage = duplicateInstructions(
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
            List<InstructionReferenceLoadDTO> refersList = referenceToDuplicateNew(
                    conn, homeBankId, newBotJobId, oldBlockId, arrayTables[4], arrayTables[2]); // reference
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
                errorMessage = duplicateReferences(conn, refersList, arrayTables[5]); // component_reference
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

        List<VariableLoadDTO> varsList = instVariablesToDuplicateNEW(conn, homeBankId, oldBotJobId, -1, arrayTables[4]);

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
            ErrorMessage errorMessage = duplicateInstructions(
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
                    referenceToDuplicateNew(conn, homeBankId, oldBotJobId, -1, arrayTables[2], arrayTables[1]);
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
                errorMessage = duplicateReferences(conn, refersList, arrayTables[2]);
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

    private ErrorMessage duplicateInstructions(
            Connection conn,
            List<InstructionLoadDTO> instList,
            Map<Integer, Integer> parentOlderAndNewId,
            Map<Integer, Integer> variableOlderAndNewId,
            Map<Integer, Integer> blocksOlderAndNewId,
            String targetTable)
            throws SQLException {

        // Dynamically construct the INSERT query
        String blockLoopInstructionInsertQuery = "INSERT INTO " + targetTable
                + " (id, action_custom_max_wait_sec, actions, active, block_marked, codified, "
                + "default_value, description, export_to_abr, instruction_order_number, name, on_hold_seconds, operation, optional, "
                + "parent_id, xpath, coordinates, force_coordinates, iframe_xpath, variable_id, block_id, tag_name, shadow_host, shadow_root, css_selector";

        if (targetTable.equalsIgnoreCase("instruction")) {
            blockLoopInstructionInsertQuery += ", bot_job_id";
        } else if (targetTable.equalsIgnoreCase("component_instruction")) {
            blockLoopInstructionInsertQuery += ", home_banking_id";
        }

        blockLoopInstructionInsertQuery +=
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?";

        if (targetTable.equalsIgnoreCase("instruction") || targetTable.equalsIgnoreCase("component_instruction")) {
            blockLoopInstructionInsertQuery += ", ?";
        }

        blockLoopInstructionInsertQuery += ")";

        final int BATCH_SIZE = 100;
        int count = 0;

        conn.setAutoCommit(false);

        try (PreparedStatement insertStmt = conn.prepareStatement(blockLoopInstructionInsertQuery)) {
            for (InstructionLoadDTO instruction : instList) {
                Integer newParentId = instruction.getActions().equals(ARConstants.GOTO)
                        ? blocksOlderAndNewId.get(instruction.getParentId())
                        : parentOlderAndNewId.get(instruction.getParentId());

                Integer newVariableId = variableOlderAndNewId.get(instruction.getVariableId());

                insertStmt.setInt(1, instruction.getInstructionId());
                insertStmt.setInt(2, instruction.getActionCustomMaxWaitSec());
                insertStmt.setString(3, instruction.getActions());
                insertStmt.setInt(4, instruction.getInstructionActive() ? 1 : 0);
                insertStmt.setInt(5, instruction.getBlockMarked() ? 1 : 0);
                insertStmt.setInt(6, instruction.getCodified() ? 1 : 0);

                if (instruction.getDefaultValue() != null) {
                    insertStmt.setString(7, instruction.getDefaultValue());
                } else {
                    insertStmt.setNull(7, Types.VARCHAR);
                }

                insertStmt.setString(8, instruction.getDescription());
                insertStmt.setInt(9, instruction.getExportToABR() ? 1 : 0);
                insertStmt.setInt(10, instruction.getInstructionOrderNumber());
                insertStmt.setString(11, instruction.getInstructionName());
                insertStmt.setInt(12, instruction.getOnHoldSeconds());

                if (instruction.getOperation() != null) {
                    insertStmt.setString(13, instruction.getOperation());
                } else {
                    insertStmt.setNull(13, Types.VARCHAR);
                }

                insertStmt.setInt(14, instruction.getOptional() ? 1 : 0);

                if (instruction.getParentId() != null && instruction.getParentId() > 0) {
                    insertStmt.setInt(15, newParentId != null ? newParentId : instruction.getParentId());
                } else {
                    insertStmt.setNull(15, Types.INTEGER);
                }

                if (instruction.getXpath() != null) {
                    insertStmt.setString(16, instruction.getXpath());
                } else {
                    insertStmt.setNull(16, Types.VARCHAR);
                }

                if (!Strings.isNullOrEmpty(instruction.getCoordinates())) {
                    insertStmt.setString(17, instruction.getCoordinates());
                } else {
                    insertStmt.setNull(17, Types.VARCHAR);
                }

                insertStmt.setInt(18, instruction.getForceCoordinates() ? 1 : 0);

                if (!Strings.isNullOrEmpty(instruction.getIFrameXPath())) {
                    insertStmt.setString(19, instruction.getIFrameXPath());
                } else {
                    insertStmt.setNull(19, Types.VARCHAR);
                }

                if (instruction.getVariableId() != null && instruction.getVariableId() > 0) {
                    insertStmt.setInt(20, newVariableId != null ? newVariableId : instruction.getVariableId());
                } else {
                    insertStmt.setNull(20, Types.INTEGER);
                }

                insertStmt.setInt(21, instruction.getBlockId());

                insertStmt.setString(22, instruction.getTagName());
                insertStmt.setString(23, instruction.getShadowHost());
                insertStmt.setString(24, instruction.getShadowRoot());
                insertStmt.setString(25, instruction.getCssSelector());

                int paramIndex = 26;
                if (targetTable.equalsIgnoreCase("instruction")) {
                    insertStmt.setInt(paramIndex, instruction.getBotJobId());
                } else if (targetTable.equalsIgnoreCase("component_instruction")) {
                    insertStmt.setInt(paramIndex, instruction.getHomeBankingId());
                }

                insertStmt.addBatch();
                count++;

                if (count % BATCH_SIZE == 0) {
                    insertStmt.executeBatch();
                    conn.commit(); // ensure autoCommit is false
                    System.out.println("Inserted batch of " + BATCH_SIZE);
                }
            }

            // Final batch
            if (count % BATCH_SIZE != 0) {
                insertStmt.executeBatch();
                conn.commit();
                System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
            }
            return null;
        } catch (SQLException error) {
            System.out.println(error.getMessage());
            return new ErrorMessage("Error Duplicating Instructions", "Block Insertion Failure", error.getMessage());
        }
    }

    private ErrorMessage duplicateReferences(
            Connection conn, List<InstructionReferenceLoadDTO> refersList, String targetTable) {

        String insertQuery = "INSERT INTO " + targetTable + " (id, reference_type, value, instruction_id";

        if (targetTable.equalsIgnoreCase("reference")) {
            insertQuery += ", bot_job_id";
        } else if (targetTable.equalsIgnoreCase("component_reference")) {
            insertQuery += ", home_banking_id";
        }

        insertQuery += ") VALUES (?, ?, ?, ?";

        if (targetTable.equalsIgnoreCase("reference") || targetTable.equalsIgnoreCase("component_reference")) {
            insertQuery += ", ?";
        }

        insertQuery += ")";

        final int BATCH_SIZE = 100;
        int count = 0;

        try {
            conn.setAutoCommit(false);

            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                for (InstructionReferenceLoadDTO reference : refersList) {
                    insertStmt.setInt(1, reference.getId());
                    insertStmt.setString(2, reference.getReferenceType());
                    insertStmt.setString(3, reference.getValue());
                    insertStmt.setInt(4, reference.getBlockLoopInstructionId());

                    int paramIndex = 5;
                    if (targetTable.equalsIgnoreCase("reference")) {
                        insertStmt.setInt(paramIndex, reference.getBotJobId());
                    } else if (targetTable.equalsIgnoreCase("component_reference")) {
                        insertStmt.setInt(paramIndex, reference.getHomeBankingId());
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

                return null;

            } catch (SQLException e) {
                conn.rollback(); // rollback changes on failure
                ARLogger.getInstance(PerformDataBase.class).severe("Failed to duplicateReferences");
                return new ErrorMessage("Error Duplicating References", "Reference Insertion Failure", e.getMessage());
            } finally {
                try {
                    conn.setAutoCommit(true); // restore autocommit
                } catch (SQLException e) {
                    ARLogger.getInstance(PerformDataBase.class).severe("Failed to duplicateReferences");
                    return new ErrorMessage(
                            "Error Duplicating References", "Reference Insertion Failure", e.getMessage());
                }
            }

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to duplicateReferences");
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

    private ErrorMessage duplicateVariables(Connection conn, List<VariableLoadDTO> varsList, String targetTable) {
        final int BATCH_SIZE = 100;
        int count = 0;

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

        try {
            conn.setAutoCommit(false); // ensure autocommit is OFF

            try (PreparedStatement varStmt = conn.prepareStatement(variableInsertQuery)) {
                for (VariableLoadDTO variableDTO : varsList) {
                    varStmt.setInt(1, variableDTO.getId());
                    varStmt.setString(2, variableDTO.getName());
                    varStmt.setString(3, variableDTO.getType());
                    varStmt.setString(4, variableDTO.getValue());
                    varStmt.setInt(5, variableDTO.getInstructionId());

                    if (targetTable.equalsIgnoreCase("variable")) {
                        varStmt.setInt(6, variableDTO.getBotJobId());
                    } else if (targetTable.equalsIgnoreCase("component_variable")) {
                        varStmt.setInt(6, variableDTO.getHomeBankingId());
                    }

                    varStmt.addBatch();
                    count++;

                    if (count % BATCH_SIZE == 0) {
                        varStmt.executeBatch();
                        conn.commit(); // commit after batch
                        System.out.println("Inserted batch of " + BATCH_SIZE);
                    }
                }

                // Final batch
                if (count % BATCH_SIZE != 0) {
                    varStmt.executeBatch();
                    conn.commit(); // commit final batch
                    System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                }

                return null;
            } catch (SQLException e) {
                conn.rollback(); // rollback on error
                ARLogger.getInstance(PerformDataBase.class).severe("Failed to Duplicating Variables");
                return new ErrorMessage("Error Duplicating Variables", "Variable Insertion Failure", e.getMessage());
            } finally {
                conn.setAutoCommit(true); // restore autocommit
            }
        } catch (SQLException outerEx) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe("Database Error.Could not configure transaction: " + outerEx.getMessage());
            return new ErrorMessage("Database Error", "Could not configure transaction", outerEx.getMessage());
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

    public List<InstructionLoadDTO> buildJsonViewData(List<BotJobLoadDTO> botJobLoadList, String tableName) {
        if (!botJobLoadList.isEmpty()
                && !botJobLoadList.get(0).getBlockLoadDTOList().isEmpty()) {
            List<InstructionLoadDTO> rowList = null;
            try {

                for (BlockLoadDTO block : botJobLoadList.get(0).getBlockLoadDTOList()) {
                    rowList = getInstructionsByBlockId(botJobLoadList.get(0).getId(), block.getId(), tableName);
                    reorderInstructions(rowList, tableName);
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
        List<BlockLoadDTO> savedBlockLoadList = new ArrayList<>();

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

    public boolean insertReferences(List<InstructionReferenceLoadDTO> queue, int instructionId) throws SQLException {
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
    public void deleteComponent(
            int homeBankId, int blockId, InstructionLoadDTO deleteInstructionLoad, boolean blockDeletion) {

        if (deleteInstructionLoad.getParentId() != null) {
            List<ParentOperations> listParents = loadParentsComp(
                    homeBankId, blockId, deleteInstructionLoad.getInstructionId(), deleteInstructionLoad.getParentId());
            if (!listParents.isEmpty()) {

                if (!blockDeletion) {
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
                        return;
                    }
                }

                deleteCompRowParents(homeBankId, blockId, deleteInstructionLoad.getInstructionId());
            }
        }

        if (deleteCompVariable(deleteInstructionLoad))
            if (deleteCompReferences(deleteInstructionLoad))
                if (deleteCompInstruction(deleteInstructionLoad)) {
                    deleteCompNullBlocks(deleteInstructionLoad.getHomeBankingId());
                    updateCompBlockOrderNumber(selectCompAllBlocks(homeBankId, blockId), true);
                }
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

    public void updateTableAccess(String dbUrl, File dbFile) {
        //        try {
        //            String url = "jdbc:odbc:Driver={Microsoft Access Driver (*.mdb,
        // *.accdb)};DBQ=path_to_your_access_db.accdb";
        //            Connection conn = DriverManager.getConnection(url);
        //        } catch (SQLException error) {
        //            System.out.println("initializeDatabase\nError: " + error.getMessage());
        //        }

        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            try (Statement stmt = conn.createStatement()) {
                DatabaseMetaData dbMeta = conn.getMetaData();
                ResultSet rs = dbMeta.getColumns(null, null, "variable", "local_format");

                if (!rs.next()) {
                    // Column does not exist, so add it
                    //                    String addLocalFormatColumnSQL = "ALTER TABLE variable ADD COLUMN local_format
                    // TEXT;";
                    //                    String addLocalFormatColumnSQL = "ALTER TABLE variable ADD COLUMN local_format
                    // MEMO;";
                    String addLocalFormatColumnSQL = "ALTER TABLE variable ADD COLUMN local_format VARCHAR(255);";
                    stmt.executeUpdate(addLocalFormatColumnSQL);

                    System.out.println(String.format("Database %s has been updated!", dbFile.getName()));
                    System.out.println(String.format("Updates %s", "variable ADD COLUMN local_format"));
                } else {
                    //                    System.out.println(String.format("Database %s no need updates!",
                    // dbFile.getName()));
                }

                rs = dbMeta.getColumns(null, null, "component_variable", "local_format");

                if (!rs.next()) {
                    // Column does not exist, so add it
                    //                    String addLocalFormatColumnSQL = "ALTER TABLE variable ADD COLUMN local_format
                    // TEXT;";
                    //                    String addLocalFormatColumnSQL = "ALTER TABLE variable ADD COLUMN local_format
                    // MEMO;";
                    String addLocalFormatColumnSQL =
                            "ALTER TABLE component_variable ADD COLUMN local_format VARCHAR(255);";
                    stmt.executeUpdate(addLocalFormatColumnSQL);

                    System.out.println(String.format("Database %s has been updated!", dbFile.getName()));
                    System.out.println(String.format("Updates %s", "component_variable ADD COLUMN local_format"));
                } else {
                    //                    System.out.println(String.format("Database %s no need updates!",
                    // dbFile.getName()));
                }

                // ADD DELIMITER COLUMN
                rs = dbMeta.getColumns(null, null, "variable", "delimiter");

                if (!rs.next()) {
                    String addLocalFormatColumnSQL = "ALTER TABLE variable ADD COLUMN delimiter VARCHAR(255);";
                    stmt.executeUpdate(addLocalFormatColumnSQL);

                    System.out.println(String.format("Database %s has been updated!", dbFile.getName()));
                    System.out.println(String.format("Updates %s", "variable ADD COLUMN delimiter"));
                } else {
                    //                    System.out.println(String.format("Database %s no need updates!",
                    // dbFile.getName()));
                }

                rs = dbMeta.getColumns(null, null, "component_variable", "delimiter");

                if (!rs.next()) {
                    String addLocalFormatColumnSQL =
                            "ALTER TABLE component_variable ADD COLUMN delimiter VARCHAR(255);";
                    stmt.executeUpdate(addLocalFormatColumnSQL);

                    System.out.println(String.format("Database %s has been updated!", dbFile.getName()));
                    System.out.println(String.format("Updates %s", "component_variable ADD COLUMN delimiter"));
                } else {
                    //                    System.out.println(String.format("Database %s no need updates!",
                    // dbFile.getName()));
                }

                boolean homeUrlExists = false;
                rs = dbMeta.getTables(null, null, null, new String[] {"TABLE"});

                ResultSet tables = dbMeta.getTables(null, null, null, new String[] {"TABLE"});
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    if ("home_url".equalsIgnoreCase(tableName)) {
                        System.out.println("Table 'home_url' exists.");
                        homeUrlExists = true;
                        break;
                    }
                }

                if (!homeUrlExists) {
                    System.out.println("Table 'home_url' does NOT exist. Creating it...");
                    createHomeURLTable(dbUrl, dbFile); // Pass the connection if needed
                }

                // ADD HOME_URL_ID COLUMN
                rs = dbMeta.getColumns(null, null, "bot_job", "home_url_id");

                if (!rs.next()) {
                    String addHomeUrlIdColumnSQL = "ALTER TABLE bot_job ADD COLUMN home_url_id INTEGER;";
                    stmt.executeUpdate(addHomeUrlIdColumnSQL);

                    //                    String addHomrURLForeignKeySQL = "ALTER TABLE bot_job "
                    //                            + "ADD CONSTRAINT FK_NewHomeURL FOREIGN KEY (home_url_id) "
                    //                            + "REFERENCES home_url(id)";
                    //                    stmt.executeUpdate(addHomrURLForeignKeySQL);

                    //                    String upDateSQL = "UPDATE bot_job "
                    //                            + "SET home_url_id = home_banking_id";
                    //                    stmt.executeUpdate(upDateSQL);

                    System.out.println(String.format("Database %s has been updated!", dbFile.getName()));
                    System.out.println(String.format("Updates %s", "bot_job ADD COLUMN home_url_id"));
                } else {
                    //                    System.out.println(String.format("Database %s no need updates!",
                    // dbFile.getName()));
                }

                //                // TEST FOR DROPPING COLUMNS
                //                rs = dbMeta.getColumns(null, null, "variable", "local_format");
                //                if (rs.next()) {
                //                    // Column exists, so drop it
                //                    String dropColumnSQL = "ALTER TABLE variable DROP COLUMN local_format;";
                //                    stmt.executeUpdate(dropColumnSQL);
                //                }

                //                rs = dbMeta.getTables(null, null, "home_url", new String[]{"TABLE"});

                //                deleteHomeUrl(dbUrl);
                this.databaseUpds = loadHomeBanking(null);
                this.homeURLList = loadAllHomeURL();

                insertUpdateHomeUrl();

                // Update bot_jobs home_url_id
                this.homeURLList = loadAllHomeURL();

                this.botJobLoadList = loadAllBotJobs(getConn());

                if (botJobLoadList != null
                        && botJobLoadList.size() > 0
                        && botJobLoadList.get(0).getHomeUrlId() == 0) {
                    updateBotJobHomeUrlId(homeURLList);
                }

                rs.close();
            }
        } catch (SQLException error) {
            System.out.println("initializeDatabase\nError: " + error.getMessage());
        }
    }

    private void updateBotJobHomeUrlId(List<HomeUrlDTO> homeURLList) {
        try (Connection conn = getConnection()) {

            ErrorMessage errorMessage = updateBotJobHomeUrlIds(conn, homeURLList);

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

    private ErrorMessage updateBotJobHomeUrlIds(Connection conn, List<HomeUrlDTO> homeURLList) {
        // Update bot_job only if the home_url_id to be set exists in the home_url table
        String blockInsertQuery = "UPDATE bot_job AS bj " + "SET bj.home_url_id = ? "
                + "WHERE bj.home_banking_id = ? "
                + "AND EXISTS (SELECT 1 FROM home_url WHERE id = ?);"; // Parameter for home_url.id check

        try (PreparedStatement blockStmt = conn.prepareStatement(blockInsertQuery)) {
            boolean batchModeEnabled = false;
            for (HomeUrlDTO homeUrl : homeURLList) {
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

    public void createHomeURLTable(String dbUrl, File dbFile) {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            try (Statement stmt = conn.createStatement()) {

                // Create bot_job table with a foreign key reference to home_banking
                String createURLTableSQL = "CREATE TABLE home_url ("
                        + "ID INTEGER PRIMARY KEY, "
                        + "url MEMO, "
                        + "home_banking_id INTEGER);";
                stmt.executeUpdate(createURLTableSQL);

                //                String addURLForeignKeySQL = "ALTER TABLE home_url "
                //                        + "ADD CONSTRAINT FK_UrlNew FOREIGN KEY (home_banking_id) "
                //                        + "REFERENCES home_banking(id) ";
                //                stmt.executeUpdate(addURLForeignKeySQL);
            }
            System.out.println(String.format("Database %s has been created!", dbFile.getName()));
        } catch (SQLException error) {
            System.out.println("initializeDatabase\nError: " + error.getMessage());
        }
    }

    private void insertUpdateHomeUrl() {
        try (Connection conn = getConnection()) {

            int newHomeUrlId = loadNexHomeUrlData() + 1;

            if (newHomeUrlId > -1) {

                ErrorMessage errorMessage = insertIntoHomeUrl(conn, newHomeUrlId);

                if (errorMessage != null) {
                    performMessage.errorMessage(
                            "Updating Home URLs Error",
                            "<span style='color: #2E7D32; font-weight: bold; font-size: 1.1em;'>Home URLs error!</span>",
                            null,
                            null,
                            null,
                            0);
                }
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private ErrorMessage insertIntoHomeUrl(Connection conn, int newHomeUrlId) throws SQLException {
        String blockInsertQuery = "INSERT INTO home_url " + "(id,  url, home_banking_id) ";
        String strValues = "VALUES (?, ?, ?)";

        blockInsertQuery = blockInsertQuery + strValues;

        try (PreparedStatement blockStmt = conn.prepareStatement(blockInsertQuery)) {

            boolean batchModeEnabled = false;
            for (HomeBankingLoadDTO dbUser : this.databaseUpds) {
                Integer dbUserId = dbUser.getId() != null ? dbUser.getId() : null;
                String dbUserUrl = dbUser.getUrl();

                // Check if homeURLList contains a HomeUrlDTO with matching id and url
                boolean exists = homeURLList.stream()
                        .anyMatch(homeUrl -> homeUrl.getId() != null
                                && dbUserId.equals(homeUrl.getHomeBankingId())
                                && dbUserUrl.equals(homeUrl.getUrl()));

                if (!exists) {
                    System.out.println("Insert in home_url: Home Banking ID = " + dbUserId + ", url = " + dbUserUrl);
                    int index = 1;
                    blockStmt.setInt(index++, newHomeUrlId);
                    blockStmt.setString(index++, dbUserUrl);
                    blockStmt.setInt(index++, dbUserId);

                    newHomeUrlId++;

                    blockStmt.addBatch(); // Add the current block to the batch
                    batchModeEnabled = true;
                }
            }
            if (batchModeEnabled) {
                blockStmt.executeBatch(); // Execute the batch insert
            }
            return null;
        } catch (SQLException error) {
            System.out.println(error.getMessage());
            return new ErrorMessage("Error Duplicating Blocks", "Block Insertion Failure", error.getMessage());
        }
    }

    public ErrorMessage insertHomeUrlChild(Connection conn, int homeBankId, String newUrl, int newHomeUrlId)
            throws SQLException {
        String blockInsertQuery = "INSERT INTO home_url " + "(id,  url, home_banking_id) ";
        String strValues = "VALUES (?, ?, ?)";

        blockInsertQuery = blockInsertQuery + strValues;

        try (PreparedStatement blockStmt = conn.prepareStatement(blockInsertQuery)) {

            int index = 1;
            blockStmt.setInt(index++, newHomeUrlId);
            blockStmt.setString(index++, newUrl);
            blockStmt.setInt(index++, homeBankId);

            blockStmt.addBatch(); // Add the current block to the batch
            blockStmt.executeBatch(); // Execute the batch insert
            return null;
        } catch (SQLException error) {
            System.out.println(error.getMessage());
            return new ErrorMessage("Error Duplicating Blocks", "Block Insertion Failure", error.getMessage());
        }
    }

    public ErrorMessage insertNewHomeUrl(int homeBankId, String newUrl) throws SQLException {
        String checkQuery = "SELECT COUNT(*) FROM home_url WHERE url = ? AND home_banking_id = ?";
        String blockInsertQuery = "INSERT INTO home_url (id, url, home_banking_id) VALUES (?, ?, ?)";

        try (Connection conn = getConnection()) {
            // Check if the URL already exists for the given home_banking_id
            try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                checkStmt.setString(1, newUrl);
                checkStmt.setInt(2, homeBankId);

                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return new ErrorMessage(
                                "Error: URL already exists for this organization.",
                                "Duplicate URL Entry",
                                "The URL '" + newUrl + "' is already assigned to this home_banking_id: " + homeBankId);
                    }
                }
            }

            // Proceed with insertion
            try (PreparedStatement insertStmt = conn.prepareStatement(blockInsertQuery)) {
                int newHomeUrlId = getMaxId(conn, "home_url") + 1;

                int index = 1;
                insertStmt.setInt(index++, newHomeUrlId);
                insertStmt.setString(index++, newUrl);
                insertStmt.setInt(index++, homeBankId);

                insertStmt.executeUpdate(); // Use executeUpdate for single insert
                return null;
            }

        } catch (SQLException error) {
            System.out.println(error.getMessage());
            return new ErrorMessage("Error inserting URL", "Org URL Insertion Failure", error.getMessage());
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

    public ObservableList<DatabaseUserDTO> loadAllHomeBankingBotJob() {
        databaseList.clear();
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
  bank.password;
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

                databaseList.add(new DatabaseUserDTO(
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

        return databaseList;
        //        jobUserList.clear();
        //        loadBotJobData();
    }

    public List<HomeUrlDTO> loadAllHomeURL() {
        homeURLList.clear();
        String selectSQL = " SELECT *  FROM home_url bank ";
        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(selectSQL)) {
            while (rs.next()) {
                Integer id = rs.getInt("ID");
                String url = rs.getString("url");
                Integer homeBankingId = rs.getInt("home_banking_id");

                homeURLList.add(new HomeUrlDTO(id, url, homeBankingId));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return homeURLList;
    }

    public List<HomeUrlDTO> loadAllHomeURLByHomeId(int homeBankingId) {
        homeURLList.clear();
        String selectSQL =
                " SELECT *  FROM home_url bank " + " where home_banking_id = " + homeBankingId + " order by id";

        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(selectSQL)) {
            while (rs.next()) {
                Integer id = rs.getInt("ID");
                String url = rs.getString("url");

                homeURLList.add(new HomeUrlDTO(id, url, homeBankingId));
            }
            return homeURLList;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return homeURLList;
    }

    public void selectHomeBankinOneRow() {
        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
        String accessDbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;
        ARLogger.getInstance(PerformDataBase.class).info("ACCESS connection URL: " + accessDbUrl);

        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        String userData = userDB + " - " + userPwd;

        ARLogger.getInstance(PerformDataBase.class).info("POSTGRES connection URL: " + postgresDbUrl);
        ARLogger.getInstance(PerformDataBase.class).info("User Details: " + userData);

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

    public void exportHomeBankingAccess() {
        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
        String accessDbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;
        ARLogger.getInstance(PerformDataBase.class).info("ACCESS connection URL: " + accessDbUrl);

        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        String userData = userDB + " - " + userPwd;

        ARLogger.getInstance(PerformDataBase.class).info("POSTGRES connection URL: " + postgresDbUrl);
        ARLogger.getInstance(PerformDataBase.class).info("User Details: " + userData);

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

    public void exportHomeBanking() {
        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
        String accessDbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;
        ARLogger.getInstance(PerformDataBase.class).info("ACCESS connection URL: " + accessDbUrl);

        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        String userData = userDB + " - " + userPwd;

        ARLogger.getInstance(PerformDataBase.class).info("POSTGRES connection URL: " + postgresDbUrl);
        ARLogger.getInstance(PerformDataBase.class).info("User Details: " + userData);

        final int BATCH_SIZE = 100;

        try (Connection accessConn = DriverManager.getConnection(accessDbUrl);
                Connection postgresConn = DriverManager.getConnection(postgresDbUrl, userDB, userPwd);
                Statement accessStmt = accessConn.createStatement(); ) {
            postgresConn.setAutoCommit(false); // Use manual commit for batch performance

            String selectAccessSQL =
                    "SELECT ID, url, name, priority, search_config, options_config, cookies, driver_session, username, password FROM home_banking order by id";
            ResultSet rsHomeBank = accessStmt.executeQuery(selectAccessSQL);

            String checkSQL = "SELECT id FROM home_banking WHERE url = ?";
            String insertSQL =
                    "INSERT INTO home_banking (url, name, priority, search_config, options_config, cookies, driver_session, username, password) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            homeBankMap.clear();
            try (PreparedStatement checkStmt = postgresConn.prepareStatement(checkSQL);
                    PreparedStatement insertStmt = postgresConn.prepareStatement(insertSQL)) {
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
                            postgresConn.commit();
                            System.out.println("Inserted batch of " + BATCH_SIZE);
                        }
                    } else {
                        System.out.println("Skipped (exists): " + url);
                    }
                }

                // Final batch
                if (count % BATCH_SIZE != 0) {
                    insertStmt.executeBatch();
                    postgresConn.commit();
                    System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                }
            }

            System.out.println("Sync completed.");
        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class).severe("Error export HomeBanking");
        }
    }

    public void getNewIdsHomeBank() {

        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        try (Connection postgresConn = DriverManager.getConnection(postgresDbUrl, userDB, userPwd);
                PreparedStatement stmt = postgresConn.prepareStatement("SELECT id, url FROM home_banking order by id");
                ResultSet rs = stmt.executeQuery()) {

            List<Integer> keys = new ArrayList<>(homeBankMap.keySet());
            int index = 0;
            while (rs.next()) {
                int id = rs.getInt("id");
                //                String url = rs.getString("url");
                int keyAtIndex0 = keys.get(index);
                homeBankMap.put(keyAtIndex0, id);
                index++;
            }

            ARLogger.getInstance(PerformDataBase.class)
                    .info("Loaded home_banking map with " + homeBankMap.size() + " entries.");

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to load home_banking map from PostgreSQL.");
        }
    }

    public void getNewIdsHomeBankAccess() {

        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
        String accessDbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;

        try (Connection accessConn = DriverManager.getConnection(accessDbUrl);
                PreparedStatement stmt = accessConn.prepareStatement("SELECT id, url FROM home_banking order by id");
                ResultSet rs = stmt.executeQuery()) {

            List<Integer> keys = new ArrayList<>(homeBankMap.keySet());
            int index = 0;
            while (rs.next()) {
                int id = rs.getInt("id");
                //                String url = rs.getString("url");
                int keyAtIndex0 = keys.get(index);
                homeBankMap.put(keyAtIndex0, id);
                index++;
            }

            ARLogger.getInstance(PerformDataBase.class)
                    .info("Loaded home_banking map with " + homeBankMap.size() + " entries.");

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to load home_banking map from PostgreSQL.");
        }
    }

    public void getNewIdsHomeUrl() {

        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        try (Connection postgresConn = DriverManager.getConnection(postgresDbUrl, userDB, userPwd);
                PreparedStatement stmt = postgresConn.prepareStatement("SELECT id, url FROM home_url order by id");
                ResultSet rs = stmt.executeQuery()) {

            List<Integer> keys = new ArrayList<>(homeUrlMap.keySet());
            int index = 0;
            while (rs.next()) {
                int id = rs.getInt("id");
                //                String url = rs.getString("url");
                int keyAtIndex0 = keys.get(index);
                homeUrlMap.put(keyAtIndex0, id);
                index++;
            }

            ARLogger.getInstance(PerformDataBase.class)
                    .info("Loaded home_url map with " + homeUrlMap.size() + " entries.");

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to load home_url map from PostgreSQL.");
        }
    }

    public void getNewIdsHomeUrlAccess() {

        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
        String accessDbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;

        try (Connection accessConn = DriverManager.getConnection(accessDbUrl);
                PreparedStatement stmt = accessConn.prepareStatement("SELECT id, url FROM home_url order by id");
                ResultSet rs = stmt.executeQuery()) {

            List<Integer> keys = new ArrayList<>(homeUrlMap.keySet());
            int index = 0;
            while (rs.next()) {
                int id = rs.getInt("id");
                //                String url = rs.getString("url");
                int keyAtIndex0 = keys.get(index);
                homeUrlMap.put(keyAtIndex0, id);
                index++;
            }

            ARLogger.getInstance(PerformDataBase.class)
                    .info("Loaded home_url map with " + homeUrlMap.size() + " entries.");

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to load home_url map from PostgreSQL.");
        }
    }

    public void getNewIdsBotJob() {

        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        try (Connection postgresConn = DriverManager.getConnection(postgresDbUrl, userDB, userPwd);
                PreparedStatement stmt = postgresConn.prepareStatement("SELECT id  FROM bot_job order by id");
                ResultSet rs = stmt.executeQuery()) {

            List<Integer> keys = new ArrayList<>(botJobMap.keySet());
            int index = 0;
            while (rs.next()) {
                int id = rs.getInt("id");
                //                String url = rs.getString("url");
                int keyAtIndex0 = keys.get(index);
                botJobMap.put(keyAtIndex0, id);
                index++;
            }

            ARLogger.getInstance(PerformDataBase.class)
                    .info("Loaded bot_job map with " + botJobMap.size() + " entries.");

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to load bot_job map from PostgreSQL.");
        }
    }

    public void getNewIdsBotJobAccess() {

        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
        String accessDbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;

        try (Connection accessConn = DriverManager.getConnection(accessDbUrl);
                PreparedStatement stmt = accessConn.prepareStatement("SELECT id  FROM bot_job order by id");
                ResultSet rs = stmt.executeQuery()) {

            List<Integer> keys = new ArrayList<>(botJobMap.keySet());
            int index = 0;
            while (rs.next()) {
                int id = rs.getInt("id");
                //                String url = rs.getString("url");
                int keyAtIndex0 = keys.get(index);
                botJobMap.put(keyAtIndex0, id);
                index++;
            }

            ARLogger.getInstance(PerformDataBase.class)
                    .info("Loaded bot_job map with " + botJobMap.size() + " entries.");

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to load bot_job map from PostgreSQL.");
        }
    }

    public void getNewIdsBlock() {

        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        try (Connection postgresConn = DriverManager.getConnection(postgresDbUrl, userDB, userPwd);
                PreparedStatement stmt = postgresConn.prepareStatement("SELECT id  FROM block order by id");
                ResultSet rs = stmt.executeQuery()) {

            List<Integer> keys = new ArrayList<>(blockMap.keySet());
            int index = 0;
            while (rs.next()) {
                int id = rs.getInt("id");
                //                String url = rs.getString("url");
                int keyAtIndex0 = keys.get(index);
                blockMap.put(keyAtIndex0, id);
                index++;
            }

            ARLogger.getInstance(PerformDataBase.class).info("Loaded block map with " + blockMap.size() + " entries.");

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to load block map from PostgreSQL.");
        }
    }

    public void getNewIdsBlockAccess() {

        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
        String accessDbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;

        try (Connection accessConn = DriverManager.getConnection(accessDbUrl);
                PreparedStatement stmt = accessConn.prepareStatement("SELECT id  FROM block order by id");
                ResultSet rs = stmt.executeQuery()) {

            List<Integer> keys = new ArrayList<>(blockMap.keySet());
            int index = 0;
            while (rs.next()) {
                int id = rs.getInt("id");
                //                String url = rs.getString("url");
                int keyAtIndex0 = keys.get(index);
                blockMap.put(keyAtIndex0, id);
                index++;
            }

            ARLogger.getInstance(PerformDataBase.class).info("Loaded block map with " + blockMap.size() + " entries.");

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to load block map from PostgreSQL.");
        }
    }

    public void getNewIdsInstruc() {

        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        try (Connection postgresConn = DriverManager.getConnection(postgresDbUrl, userDB, userPwd);
                PreparedStatement stmt = postgresConn.prepareStatement("SELECT id  FROM instruction order by id");
                ResultSet rs = stmt.executeQuery()) {

            List<Integer> keys = new ArrayList<>(instructionMap.keySet());
            int index = 0;
            while (rs.next()) {
                int id = rs.getInt("id");
                //                String url = rs.getString("url");
                int keyAtIndex0 = keys.get(index);
                instructionMap.put(keyAtIndex0, id);
                index++;
            }

            ARLogger.getInstance(PerformDataBase.class)
                    .info("Loaded instruction map with " + instructionMap.size() + " entries.");

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to load instruction map from PostgreSQL.");
        }
    }

    public void getNewIdsInstrucAccess() {

        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
        String accessDbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;

        try (Connection accessConn = DriverManager.getConnection(accessDbUrl);
                PreparedStatement stmt = accessConn.prepareStatement("SELECT id  FROM instruction order by id");
                ResultSet rs = stmt.executeQuery()) {

            List<Integer> keys = new ArrayList<>(instructionMap.keySet());
            int index = 0;
            while (rs.next()) {
                int id = rs.getInt("id");
                //                String url = rs.getString("url");
                int keyAtIndex0 = keys.get(index);
                instructionMap.put(keyAtIndex0, id);
                index++;
            }

            ARLogger.getInstance(PerformDataBase.class)
                    .info("Loaded instruction map with " + instructionMap.size() + " entries.");

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to load instruction map from PostgreSQL.");
        }
    }

    public void getNewIdsVariable() {

        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        try (Connection postgresConn = DriverManager.getConnection(postgresDbUrl, userDB, userPwd);
                PreparedStatement stmt = postgresConn.prepareStatement("SELECT id  FROM variable order by id");
                ResultSet rs = stmt.executeQuery()) {

            List<Integer> keys = new ArrayList<>(variableMap.keySet());
            int index = 0;
            while (rs.next()) {
                int id = rs.getInt("id");
                //                String url = rs.getString("url");
                int keyAtIndex0 = keys.get(index);
                variableMap.put(keyAtIndex0, id);
                index++;
            }

            ARLogger.getInstance(PerformDataBase.class)
                    .info("Loaded variable map with " + variableMap.size() + " entries.");

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to load variable map from PostgreSQL.");
        }
    }

    public void getNewIdsVariableAccess() {

        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
        String accessDbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;

        try (Connection accessConn = DriverManager.getConnection(accessDbUrl);
                PreparedStatement stmt = accessConn.prepareStatement("SELECT id  FROM variable order by id");
                ResultSet rs = stmt.executeQuery()) {

            List<Integer> keys = new ArrayList<>(variableMap.keySet());
            int index = 0;
            while (rs.next()) {
                int id = rs.getInt("id");
                //                String url = rs.getString("url");
                int keyAtIndex0 = keys.get(index);
                variableMap.put(keyAtIndex0, id);
                index++;
            }

            ARLogger.getInstance(PerformDataBase.class)
                    .info("Loaded variable map with " + variableMap.size() + " entries.");

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to load variable map from PostgreSQL.");
        }
    }

    // SAVED COMPONENTS
    public void getNewIdsCompBlock() {

        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        try (Connection postgresConn = DriverManager.getConnection(postgresDbUrl, userDB, userPwd);
                PreparedStatement stmt = postgresConn.prepareStatement("SELECT id  FROM component_block order by id");
                ResultSet rs = stmt.executeQuery()) {

            List<Integer> keys = new ArrayList<>(blockMap.keySet());
            int index = 0;
            while (rs.next()) {
                int id = rs.getInt("id");
                //                String url = rs.getString("url");
                int keyAtIndex0 = keys.get(index);
                blockMap.put(keyAtIndex0, id);
                index++;
            }

            ARLogger.getInstance(PerformDataBase.class)
                    .info("Loaded component_block map with " + blockMap.size() + " entries.");

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to load component_block map from PostgreSQL.");
        }
    }

    public void getNewIdsCompBlockAccess() {

        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
        String accessDbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;

        try (Connection accessConn = DriverManager.getConnection(accessDbUrl);
                PreparedStatement stmt = accessConn.prepareStatement("SELECT id  FROM component_block order by id");
                ResultSet rs = stmt.executeQuery()) {

            List<Integer> keys = new ArrayList<>(blockMap.keySet());
            int index = 0;
            while (rs.next()) {
                int id = rs.getInt("id");
                //                String url = rs.getString("url");
                int keyAtIndex0 = keys.get(index);
                blockMap.put(keyAtIndex0, id);
                index++;
            }

            ARLogger.getInstance(PerformDataBase.class)
                    .info("Loaded component_block map with " + blockMap.size() + " entries.");

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to load component_block map from PostgreSQL.");
        }
    }

    public void getNewIdsCompInstruc() {

        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        try (Connection postgresConn = DriverManager.getConnection(postgresDbUrl, userDB, userPwd);
                PreparedStatement stmt =
                        postgresConn.prepareStatement("SELECT id  FROM component_instruction order by id");
                ResultSet rs = stmt.executeQuery()) {

            List<Integer> keys = new ArrayList<>(instructionMap.keySet());
            int index = 0;
            while (rs.next()) {
                int id = rs.getInt("id");
                //                String url = rs.getString("url");
                int keyAtIndex0 = keys.get(index);
                instructionMap.put(keyAtIndex0, id);
                index++;
            }

            ARLogger.getInstance(PerformDataBase.class)
                    .info("Loaded component_instruction map with " + instructionMap.size() + " entries.");

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe("Failed to load component_instruction map from PostgreSQL.");
        }
    }

    public void getNewIdsCompInstrucAccess() {

        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
        String accessDbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;

        try (Connection accessConn = DriverManager.getConnection(accessDbUrl);
                PreparedStatement stmt =
                        accessConn.prepareStatement("SELECT id  FROM component_instruction order by id");
                ResultSet rs = stmt.executeQuery()) {

            List<Integer> keys = new ArrayList<>(instructionMap.keySet());
            int index = 0;
            while (rs.next()) {
                int id = rs.getInt("id");
                //                String url = rs.getString("url");
                int keyAtIndex0 = keys.get(index);
                instructionMap.put(keyAtIndex0, id);
                index++;
            }

            ARLogger.getInstance(PerformDataBase.class)
                    .info("Loaded component_instruction map with " + instructionMap.size() + " entries.");

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe("Failed to load component_instruction map from PostgreSQL.");
        }
    }

    public void getNewIdsCompVariable() {

        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        try (Connection postgresConn = DriverManager.getConnection(postgresDbUrl, userDB, userPwd);
                PreparedStatement stmt =
                        postgresConn.prepareStatement("SELECT id  FROM component_variable order by id");
                ResultSet rs = stmt.executeQuery()) {

            List<Integer> keys = new ArrayList<>(variableMap.keySet());
            int index = 0;
            while (rs.next()) {
                int id = rs.getInt("id");
                //                String url = rs.getString("url");
                int keyAtIndex0 = keys.get(index);
                variableMap.put(keyAtIndex0, id);
                index++;
            }

            ARLogger.getInstance(PerformDataBase.class)
                    .info("Loaded component_variable map with " + variableMap.size() + " entries.");

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe("Failed to load component_variable map from PostgreSQL.");
        }
    }

    public void getNewIdsCompVariableAccess() {

        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
        String accessDbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;

        try (Connection accessConn = DriverManager.getConnection(accessDbUrl);
                PreparedStatement stmt = accessConn.prepareStatement("SELECT id  FROM component_variable order by id");
                ResultSet rs = stmt.executeQuery()) {

            List<Integer> keys = new ArrayList<>(variableMap.keySet());
            int index = 0;
            while (rs.next()) {
                int id = rs.getInt("id");
                //                String url = rs.getString("url");
                int keyAtIndex0 = keys.get(index);
                variableMap.put(keyAtIndex0, id);
                index++;
            }

            ARLogger.getInstance(PerformDataBase.class)
                    .info("Loaded component_variable map with " + variableMap.size() + " entries.");

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe("Failed to load component_variable map from PostgreSQL.");
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

    public void exportHomeUrl() {
        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
        String accessDbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;
        ARLogger.getInstance(PerformDataBase.class).info("ACCESS connection URL: " + accessDbUrl);

        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        String userData = userDB + " - " + userPwd;

        ARLogger.getInstance(PerformDataBase.class).info("POSTGRES connection URL: " + postgresDbUrl);
        ARLogger.getInstance(PerformDataBase.class).info("User Details: " + userData);
        final int BATCH_SIZE = 100;

        try (Connection accessConn = DriverManager.getConnection(accessDbUrl);
                Connection postgresConn = DriverManager.getConnection(postgresDbUrl, userDB, userPwd);
                Statement accessStmt = accessConn.createStatement()) {

            postgresConn.setAutoCommit(false); // Enable manual commit for batch performance

            String selectAccessSQL = "SELECT id, home_banking_id, url FROM home_url order by id";
            try (ResultSet rsHomeUrl = accessStmt.executeQuery(selectAccessSQL)) {

                String insertHomeUrlSQL = "INSERT INTO home_url (url, home_banking_id) VALUES (?, ?)";

                try (PreparedStatement insertStmt = postgresConn.prepareStatement(insertHomeUrlSQL)) {

                    int count = 0;
                    homeUrlMap.clear();

                    while (rsHomeUrl.next()) {

                        int id = rsHomeUrl.getInt("id");
                        String url = rsHomeUrl.getString("url");

                        int oldHomeBankId = rsHomeUrl.getInt("home_banking_id");
                        // Map old bot_job_id to new
                        Integer newHomeBankId = homeBankMap.get(oldHomeBankId);
                        if (newHomeBankId == null) {
                            System.out.println(
                                    "Skipped component_block with unknown home_banking_id: " + newHomeBankId);
                            continue;
                        }

                        if (url != null && !url.trim().isEmpty()) {
                            homeUrlMap.put(id, -1);
                        }

                        insertStmt.setString(1, url);
                        insertStmt.setInt(2, newHomeBankId);
                        insertStmt.addBatch();
                        count++;

                        if (count % BATCH_SIZE == 0) {
                            insertStmt.executeBatch();
                            postgresConn.commit();
                            System.out.println("Inserted batch of " + BATCH_SIZE);
                        }
                    }

                    // Final batch
                    if (count % BATCH_SIZE != 0) {
                        insertStmt.executeBatch();
                        postgresConn.commit();
                        System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                    }
                }
            }

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to export home_url");
        }
    }

    public void exportHomeUrlAccess() {
        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
        String accessDbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;
        ARLogger.getInstance(PerformDataBase.class).info("ACCESS connection URL: " + accessDbUrl);

        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        String userData = userDB + " - " + userPwd;

        ARLogger.getInstance(PerformDataBase.class).info("POSTGRES connection URL: " + postgresDbUrl);
        ARLogger.getInstance(PerformDataBase.class).info("User Details: " + userData);
        final int BATCH_SIZE = 100;

        try (Connection accessConn = DriverManager.getConnection(accessDbUrl);
                Connection postgresConn = DriverManager.getConnection(postgresDbUrl, userDB, userPwd);
                Statement postgresStmt = postgresConn.createStatement()) {

            accessConn.setAutoCommit(false); // Enable manual commit for batch performance

            String selectPostgresSQL = "SELECT id, url, home_banking_id FROM home_url order by id";
            try (ResultSet rsHomeUrl = postgresStmt.executeQuery(selectPostgresSQL)) {

                String findHomeBankingIdSQL = "SELECT id FROM home_banking WHERE url = ?";
                String checkHomeUrlExistsSQL = "SELECT id FROM home_url WHERE url = ? AND home_banking_id = ?";
                String insertHomeUrlSQL = "INSERT INTO home_url (url, home_banking_id, id) VALUES (?, ?, ?)";

                try (PreparedStatement findHomeBankingStmt = accessConn.prepareStatement(findHomeBankingIdSQL);
                        PreparedStatement checkStmt = accessConn.prepareStatement(checkHomeUrlExistsSQL);
                        PreparedStatement insertStmt = accessConn.prepareStatement(insertHomeUrlSQL)) {

                    int count = 0;
                    homeUrlMap.clear();

                    while (rsHomeUrl.next()) {

                        int idHomeUrl = rsHomeUrl.getInt("id");
                        String url = rsHomeUrl.getString("url");

                        int oldHomeBankId = rsHomeUrl.getInt("home_banking_id");
                        // Map old bot_job_id to new
                        Integer newHomeBankId = homeBankMap.get(oldHomeBankId);
                        if (newHomeBankId == null) {
                            System.out.println(
                                    "Skipped component_block with unknown home_banking_id: " + newHomeBankId);
                            continue;
                        }

                        if (url != null && !url.trim().isEmpty()) {
                            homeUrlMap.put(idHomeUrl, -1);
                        }

                        // Get home_banking.id from PostgreSQL using url
                        //                        findHomeBankingStmt.setString(1, url);
                        //                        try (ResultSet homeBankingRs = findHomeBankingStmt.executeQuery()) {
                        //
                        //                            if (homeBankingRs.next()) {
                        //                                int homeBankingId = homeBankingRs.getInt("id");
                        //
                        //                                // Check if home_url with the same url and home_banking_id
                        // already exists
                        //                                checkStmt.setString(1, url);
                        //                                checkStmt.setInt(2, homeBankingId);
                        //                                try (ResultSet checkRs = checkStmt.executeQuery()) {
                        //
                        //                                    if (!checkRs.next()) {
                        insertStmt.setString(1, url);
                        insertStmt.setInt(2, newHomeBankId);

                        insertStmt.setInt(3, idHomeUrl);

                        insertStmt.addBatch();
                        count++;

                        if (count % BATCH_SIZE == 0) {
                            insertStmt.executeBatch();
                            accessConn.commit();
                            System.out.println("Inserted batch of " + BATCH_SIZE);
                            //                                        }
                            //                                    } else {
                            //                                        System.out.println("Skipped (already exists): " +
                            // url + " / " + homeBankingId);
                            //                                    }
                            //                                }
                            //
                            //                            } else {
                            //                                System.out.println("No matching home_banking entry for
                            // url: " + url);
                            //                            }
                        }
                    }

                    // Final batch
                    if (count % BATCH_SIZE != 0) {
                        insertStmt.executeBatch();
                        accessConn.commit();
                        System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                    }
                }
            }

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to export home_url");
        }
    }

    public void exportBotJob() {
        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
        String accessDbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;
        ARLogger.getInstance(PerformDataBase.class).info("ACCESS connection URL: " + accessDbUrl);

        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        final int BATCH_SIZE = 100;

        try (Connection accessConn = DriverManager.getConnection(accessDbUrl);
                Connection postgresConn = DriverManager.getConnection(postgresDbUrl, userDB, userPwd);
                Statement accessStmt = accessConn.createStatement()) {

            postgresConn.setAutoCommit(false);

            //            String selectAccessSQL =
            //                    "SELECT bot.id, bot.name, bot.description, bot.priority, bot.active,
            // bot.home_banking_id, hu.url FROM bot_job bot left join home_url hu on hu.id = bot.home_url_id order by
            // bot.id";
            String selectAccessFabrizio =
                    "SELECT bot.id, bot.name, bot.description, bot.priority, bot.active, bot.home_banking_id, bot.home_url_id FROM bot_job bot order by bot.id";
            try (ResultSet rsBotJob = accessStmt.executeQuery(selectAccessFabrizio)) {

                String findHomeUrlSQL = "SELECT id FROM home_banking WHERE url = ? and name = ?";
                String checkExistsSQL = "SELECT id FROM bot_job WHERE name = ?";
                String insertSQL =
                        "INSERT INTO bot_job (name, description, priority, active, home_url_id, home_banking_id) "
                                + "VALUES (?, ?, ?, ?, ?, ?)";

                try (PreparedStatement insertStmt = postgresConn.prepareStatement(insertSQL)) {

                    int count = 0;

                    botJobMap.clear();

                    while (rsBotJob.next()) {
                        int id = rsBotJob.getInt("id");
                        String name = rsBotJob.getString("name");
                        String description = rsBotJob.getString("description");
                        String priority = rsBotJob.getString("priority");
                        int active = rsBotJob.getInt("active");

                        int oldHomeBankId = rsBotJob.getInt("home_banking_id");
                        // Map old bot_job_id to new
                        Integer newHomeBankId = homeBankMap.get(oldHomeBankId);
                        if (newHomeBankId == null) {
                            System.out.println(
                                    "Skipped component_block with unknown home_banking_id: " + newHomeBankId);
                            continue;
                        }

                        //                        int oldHomeUrlId = rsBotJob.getInt("home_url_id");
                        //                        Integer newHomeUrlId = homeBankMap.get(oldHomeUrlId);
                        //                        if (newHomeUrlId == null) {
                        //                            System.out.println("Skipped component_block with unknown
                        // home_banking_id: " + oldHomeUrlId);
                        //                            continue;
                        //                        }

                        botJobMap.put(id, -1);

                        insertStmt.setString(1, name);
                        insertStmt.setString(2, description);
                        insertStmt.setString(3, priority);
                        insertStmt.setInt(4, active);
                        insertStmt.setInt(5, newHomeBankId);
                        insertStmt.setInt(6, newHomeBankId);
                        insertStmt.addBatch();
                        count++;

                        if (count % BATCH_SIZE == 0) {
                            insertStmt.executeBatch();
                            postgresConn.commit();
                            System.out.println("Inserted batch of " + BATCH_SIZE);
                        }
                    }

                    // Final batch
                    if (count % BATCH_SIZE != 0) {
                        insertStmt.executeBatch();
                        postgresConn.commit();
                        System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                    }
                }
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to export bot_job");
        }
    }

    public void exportBotJobAccess() {
        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
        String accessDbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;
        ARLogger.getInstance(PerformDataBase.class).info("ACCESS connection URL: " + accessDbUrl);

        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        final int BATCH_SIZE = 100;

        try (Connection accessConn = DriverManager.getConnection(accessDbUrl);
                Connection postgresConn = DriverManager.getConnection(postgresDbUrl, userDB, userPwd);
                Statement postgresStmt = postgresConn.createStatement()) {

            accessConn.setAutoCommit(false);

            String selectPostgresSQL =
                    "SELECT bot.id, bot.name, bot.description, bot.priority, bot.active, bot.home_banking_id, bot.home_url_id FROM bot_job bot order by bot.id";
            try (ResultSet rsBotJob = postgresStmt.executeQuery(selectPostgresSQL)) {

                String insertSQL =
                        "INSERT INTO bot_job (name, description, priority, active, home_url_id, home_banking_id, id) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement insertStmt = accessConn.prepareStatement(insertSQL)) {

                    int count = 0;

                    botJobMap.clear();

                    while (rsBotJob.next()) {
                        int idBotJob = rsBotJob.getInt("id");
                        String name = rsBotJob.getString("name");
                        String description = rsBotJob.getString("description");
                        String priority = rsBotJob.getString("priority");
                        int active = rsBotJob.getInt("active");

                        int oldHomeBankId = rsBotJob.getInt("home_banking_id");
                        // Map old bot_job_id to new
                        Integer newHomeBankId = homeBankMap.get(oldHomeBankId);
                        if (newHomeBankId == null) {
                            System.out.println(
                                    "Skipped component_block with unknown home_banking_id: " + newHomeBankId);
                            continue;
                        }

                        int oldHomeUrlId = rsBotJob.getInt("home_url_id");
                        // Map old bot_job_id to new
                        Integer newHomeUrlId = homeBankMap.get(oldHomeUrlId);
                        if (newHomeUrlId == null) {
                            System.out.println("Skipped component_block with unknown home_banking_id: " + oldHomeUrlId);
                            continue;
                        }

                        botJobMap.put(idBotJob, -1);

                        insertStmt.setString(1, name);
                        insertStmt.setString(2, description);
                        insertStmt.setString(3, priority);
                        insertStmt.setInt(4, active);
                        insertStmt.setInt(5, newHomeUrlId);
                        insertStmt.setInt(6, newHomeBankId);

                        insertStmt.setInt(7, idBotJob);

                        insertStmt.addBatch();
                        count++;

                        if (count % BATCH_SIZE == 0) {
                            insertStmt.executeBatch();
                            accessConn.commit();
                            System.out.println("Inserted batch of " + BATCH_SIZE);
                        }
                    }

                    // Final batch
                    if (count % BATCH_SIZE != 0) {
                        insertStmt.executeBatch();
                        accessConn.commit();
                        System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                    }
                }
            }

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to export bot_job");
        }
    }

    public void exportBlock() {
        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
        String accessDbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;
        ARLogger.getInstance(PerformDataBase.class).info("ACCESS connection URL: " + accessDbUrl);

        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        final int BATCH_SIZE = 100;

        try (Connection accessConn = DriverManager.getConnection(accessDbUrl);
                Connection postgresConn = DriverManager.getConnection(postgresDbUrl, userDB, userPwd);
                Statement accessStmt = accessConn.createStatement()) {
            postgresConn.setAutoCommit(false);

            String selectAccessSQL =
                    "SELECT id, block_order_number, name, description, type_id, export_file, active, wait, bot_job_id FROM block ORDER BY id";

            try (ResultSet rs = accessStmt.executeQuery(selectAccessSQL)) {

                String checkExistsSQL =
                        "SELECT id FROM block WHERE block_order_number = ? AND name = ? AND bot_job_id = ?";
                String insertSQL =
                        "INSERT INTO block (block_order_number, name, description, type_id, export_file, active, wait, bot_job_id) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

                blockMap = new TreeMap<>();

                try (PreparedStatement insertStmt = postgresConn.prepareStatement(insertSQL);
                        PreparedStatement checkStmt = postgresConn.prepareStatement(checkExistsSQL)) {

                    int count = 0;
                    blockMap.clear();

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

                        blockMap.put(id, -1); // initialize with -1

                        // Map old bot_job_id to new
                        Integer newBotJobId = botJobMap.get(oldBotJobId);
                        if (newBotJobId == null) {
                            System.out.println("Skipped block with unknown bot_job_id: " + oldBotJobId);
                            continue;
                        }

                        checkStmt.setInt(1, blockOrderNumber);
                        checkStmt.setString(2, name);
                        checkStmt.setInt(3, newBotJobId);

                        try (ResultSet checkRs = checkStmt.executeQuery()) {
                            if (!checkRs.next()) {
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
                            } else {
                                System.out.println("Skipped existing block: " + name);
                            }
                        }

                        if (count % BATCH_SIZE == 0) {
                            insertStmt.executeBatch();
                            postgresConn.commit();
                            System.out.println("Inserted batch of " + BATCH_SIZE);
                        }
                    }

                    // Final batch
                    if (count % BATCH_SIZE != 0) {
                        insertStmt.executeBatch();
                        postgresConn.commit();
                        System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                    }
                }
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to export block");
        }
    }

    public void exportBlockAccess() {
        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
        String accessDbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;
        ARLogger.getInstance(PerformDataBase.class).info("ACCESS connection URL: " + accessDbUrl);

        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        final int BATCH_SIZE = 100;

        try (Connection accessConn = DriverManager.getConnection(accessDbUrl);
                Connection postgresConn = DriverManager.getConnection(postgresDbUrl, userDB, userPwd);
                Statement postgresStmt = postgresConn.createStatement()) {

            accessConn.setAutoCommit(false);

            String selectPostgresSQL =
                    "SELECT id, block_order_number, name, description, type_id, export_file, active, wait, bot_job_id FROM block ORDER BY id";

            try (ResultSet rs = postgresStmt.executeQuery(selectPostgresSQL)) {

                String checkExistsSQL =
                        "SELECT id FROM block WHERE block_order_number = ? AND name = ? AND bot_job_id = ?";
                String insertSQL =
                        "INSERT INTO block (block_order_number, name, description, type_id, export_file, active, wait, bot_job_id, id) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

                blockMap = new TreeMap<>();

                try (PreparedStatement insertStmt = accessConn.prepareStatement(insertSQL);
                        PreparedStatement checkStmt = accessConn.prepareStatement(checkExistsSQL)) {

                    int count = 0;
                    blockMap.clear();

                    while (rs.next()) {
                        int idBlock = rs.getInt("id");
                        int blockOrderNumber = rs.getInt("block_order_number");
                        String name = rs.getString("name");
                        String description = rs.getString("description");
                        Integer typeId = rs.getObject("type_id") != null ? rs.getInt("type_id") : null;
                        String exportFile = rs.getString("export_file");
                        int active = rs.getInt("active");
                        Integer wait = rs.getObject("wait") != null ? rs.getInt("wait") : null;
                        int oldBotJobId = rs.getInt("bot_job_id");

                        blockMap.put(idBlock, -1); // initialize with -1

                        // Map old bot_job_id to new
                        Integer newBotJobId = botJobMap.get(oldBotJobId);
                        if (newBotJobId == null) {
                            System.out.println("Skipped block with unknown bot_job_id: " + oldBotJobId);
                            continue;
                        }

                        checkStmt.setInt(1, blockOrderNumber);
                        checkStmt.setString(2, name);
                        checkStmt.setInt(3, newBotJobId);

                        try (ResultSet checkRs = checkStmt.executeQuery()) {
                            if (!checkRs.next()) {
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

                                insertStmt.setInt(9, idBlock);

                                insertStmt.addBatch();
                                count++;
                            } else {
                                System.out.println("Skipped existing block: " + name);
                            }
                        }

                        if (count % BATCH_SIZE == 0) {
                            insertStmt.executeBatch();
                            accessConn.commit();
                            System.out.println("Inserted batch of " + BATCH_SIZE);
                        }
                    }

                    // Final batch
                    if (count % BATCH_SIZE != 0) {
                        insertStmt.executeBatch();
                        accessConn.commit();
                        System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                    }
                }
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to export block");
        }
    }

    public void exportInstructions() {
        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
        String accessDbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;
        ARLogger.getInstance(PerformDataBase.class).info("ACCESS connection URL: " + accessDbUrl);

        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        final int BATCH_SIZE = 100;

        try (Connection accessConn = DriverManager.getConnection(accessDbUrl);
                Connection postgresConn = DriverManager.getConnection(postgresDbUrl, userDB, userPwd);
                Statement accessStmt = accessConn.createStatement()) {

            postgresConn.setAutoCommit(false);

            String selectAccessSQL = "SELECT * FROM instruction order by id";
            try (ResultSet rsInstruction = accessStmt.executeQuery(selectAccessSQL)) {

                String checkExistsSQL =
                        "SELECT id FROM instruction WHERE instruction_order_number = ? AND name = ? AND bot_job_id = ? AND block_id = ?";
                String insertSQL = "INSERT INTO instruction ("
                        + "instruction_order_number, actions, name, xpath, coordinates, force_coordinates, iframe_xpath, tag_name, shadow_host, shadow_root, css_selector, description, operation, optional, block_marked, default_value, action_custom_max_wait_sec, on_hold_seconds, codified, export_to_abr, active, block_id, variable_id, parent_id, bot_job_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement insertStmt = postgresConn.prepareStatement(insertSQL);
                        PreparedStatement checkStmt = postgresConn.prepareStatement(checkExistsSQL)) {

                    int count = 0;
                    instructionMap.clear();

                    while (rsInstruction.next()) {

                        int id = rsInstruction.getInt("id");
                        int oldBotJobId = rsInstruction.getInt("bot_job_id");
                        Integer newBotJobId = botJobMap.get(oldBotJobId);
                        if (newBotJobId == null) {
                            System.out.println("Skipped instruction with unknown bot_job_id: " + oldBotJobId);
                            continue;
                        }

                        int oldBlockId = rsInstruction.getInt("block_id");
                        Integer newBlockId = blockMap.get(oldBlockId);
                        if (newBlockId == null) {
                            System.out.println("Skipped instruction with unknown block_id: " + oldBlockId);
                            continue;
                        }

                        int instructionOrderNumber = rsInstruction.getInt("instruction_order_number");
                        String name = rsInstruction.getString("name");

                        instructionMap.put(id, -1); // initialize with -1

                        checkStmt.setInt(1, instructionOrderNumber);
                        checkStmt.setString(2, name);
                        checkStmt.setInt(3, newBotJobId);
                        checkStmt.setInt(4, newBlockId);

                        try (ResultSet checkRs = checkStmt.executeQuery()) {
                            if (!checkRs.next()) {

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

                                insertOrNull(insertStmt, 23, rsInstruction, "variable_id");
                                insertOrNull(insertStmt, 24, rsInstruction, "parent_id");

                                insertStmt.setInt(25, newBotJobId);

                                insertStmt.addBatch();
                                count++;
                            } else {
                                System.out.println("Skipped existing instruction: " + name);
                            }
                        }
                        if (count % BATCH_SIZE == 0) {
                            insertStmt.executeBatch();
                            postgresConn.commit();
                            System.out.println("Inserted batch of " + BATCH_SIZE);
                        }
                    }

                    // Final batch
                    if (count % BATCH_SIZE != 0) {
                        insertStmt.executeBatch();
                        postgresConn.commit();
                        System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                    }
                }
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to export instructions");
        }
    }

    public void exportInstructionsAccess() {
        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
        String accessDbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;
        ARLogger.getInstance(PerformDataBase.class).info("ACCESS connection URL: " + accessDbUrl);

        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        final int BATCH_SIZE = 100;

        try (Connection accessConn = DriverManager.getConnection(accessDbUrl);
                Connection postgresConn = DriverManager.getConnection(postgresDbUrl, userDB, userPwd);
                Statement postgresStmt = postgresConn.createStatement()) {

            accessConn.setAutoCommit(false);

            String selectPostgresSQL = "SELECT * FROM instruction order by id";
            try (ResultSet rsInstruction = postgresStmt.executeQuery(selectPostgresSQL)) {

                String checkExistsSQL =
                        "SELECT id FROM instruction WHERE instruction_order_number = ? AND name = ? AND bot_job_id = ? AND block_id = ?";
                String insertSQL = "INSERT INTO instruction ("
                        + "instruction_order_number, actions, name, xpath, coordinates, force_coordinates, iframe_xpath, tag_name, shadow_host, shadow_root, css_selector, description, operation, optional, block_marked, default_value, action_custom_max_wait_sec, on_hold_seconds, codified, export_to_abr, active, block_id, variable_id, parent_id, bot_job_id, id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement insertStmt = accessConn.prepareStatement(insertSQL);
                        PreparedStatement checkStmt = accessConn.prepareStatement(checkExistsSQL)) {

                    int count = 0;
                    instructionMap.clear();

                    while (rsInstruction.next()) {

                        int idInstruc = rsInstruction.getInt("id");
                        int oldBotJobId = rsInstruction.getInt("bot_job_id");
                        Integer newBotJobId = botJobMap.get(oldBotJobId);
                        if (newBotJobId == null) {
                            System.out.println("Skipped instruction with unknown bot_job_id: " + oldBotJobId);
                            continue;
                        }

                        int oldBlockId = rsInstruction.getInt("block_id");
                        Integer newBlockId = blockMap.get(oldBlockId);
                        if (newBlockId == null) {
                            System.out.println("Skipped instruction with unknown block_id: " + oldBlockId);
                            continue;
                        }

                        int instructionOrderNumber = rsInstruction.getInt("instruction_order_number");
                        String name = rsInstruction.getString("name");

                        instructionMap.put(idInstruc, -1); // initialize with -1

                        checkStmt.setInt(1, instructionOrderNumber);
                        checkStmt.setString(2, name);
                        checkStmt.setInt(3, newBotJobId);
                        checkStmt.setInt(4, newBlockId);

                        try (ResultSet checkRs = checkStmt.executeQuery()) {
                            if (!checkRs.next()) {

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

                                insertOrNull(insertStmt, 23, rsInstruction, "variable_id");
                                insertOrNull(insertStmt, 24, rsInstruction, "parent_id");

                                insertStmt.setInt(25, newBotJobId);

                                insertStmt.setInt(26, idInstruc);

                                insertStmt.addBatch();
                                count++;
                            } else {
                                System.out.println("Skipped existing instruction: " + name);
                            }
                        }
                        if (count % BATCH_SIZE == 0) {
                            insertStmt.executeBatch();
                            accessConn.commit();
                            System.out.println("Inserted batch of " + BATCH_SIZE);
                        }
                    }

                    if (count % BATCH_SIZE != 0) {
                        insertStmt.executeBatch();
                        accessConn.commit();
                        System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                    }
                }
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to export instructions");
        }
    }

    public void exportVariables() {
        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
        String accessDbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;
        ARLogger.getInstance(PerformDataBase.class).info("ACCESS connection URL: " + accessDbUrl);

        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        final int BATCH_SIZE = 100;

        try (Connection accessConn = DriverManager.getConnection(accessDbUrl);
                Connection postgresConn = DriverManager.getConnection(postgresDbUrl, userDB, userPwd);
                Statement accessStmt = accessConn.createStatement()) {

            postgresConn.setAutoCommit(false);

            String selectAccessSQL = "SELECT * FROM variable order by id";
            try (ResultSet rsVariable = accessStmt.executeQuery(selectAccessSQL)) {

                String checkExistsSQL =
                        "SELECT id FROM variable WHERE type = ? AND name = ? AND instruction_id = ? AND bot_job_id = ?";
                String insertSQL =
                        "INSERT INTO variable (type, name, value, local_format, delimiter, instruction_id, bot_job_id) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement insertStmt = postgresConn.prepareStatement(insertSQL);
                        PreparedStatement checkStmt = postgresConn.prepareStatement(checkExistsSQL)) {

                    int count = 0;
                    variableMap.clear();

                    while (rsVariable.next()) {
                        int id = rsVariable.getInt("id");
                        String type = rsVariable.getString("type");
                        String name = rsVariable.getString("name");

                        int oldBotJobId = rsVariable.getInt("bot_job_id");
                        Integer newBotJobId = botJobMap.get(oldBotJobId);
                        if (newBotJobId == null) {
                            System.out.println("Skipped variable with unknown bot_job_id: " + oldBotJobId);
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

                        variableMap.put(id, -1); // initialize with -1

                        checkStmt.setString(1, type);
                        checkStmt.setString(2, name);
                        checkStmt.setInt(3, newInstructionId);
                        checkStmt.setInt(4, newBotJobId);

                        try (ResultSet checkRs = checkStmt.executeQuery()) {
                            if (!checkRs.next()) {

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
                            } else {
                                System.out.println("Skipped existing variable: " + name);
                            }
                        }

                        if (count % BATCH_SIZE == 0) {
                            insertStmt.executeBatch();
                            postgresConn.commit();
                            System.out.println("Inserted batch of " + BATCH_SIZE);
                        }
                    }

                    // Final batch
                    if (count % BATCH_SIZE != 0) {
                        insertStmt.executeBatch();
                        postgresConn.commit();
                        System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                    }
                }
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to export variables");
        }
    }

    public void exportVariablesAccess() {
        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
        String accessDbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;
        ARLogger.getInstance(PerformDataBase.class).info("ACCESS connection URL: " + accessDbUrl);

        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        final int BATCH_SIZE = 100;

        try (Connection accessConn = DriverManager.getConnection(accessDbUrl);
                Connection postgresConn = DriverManager.getConnection(postgresDbUrl, userDB, userPwd);
                Statement postgresStmt = postgresConn.createStatement()) {

            accessConn.setAutoCommit(false);

            String selectPostgresSQL = "SELECT * FROM variable order by id";
            try (ResultSet rsVariable = postgresStmt.executeQuery(selectPostgresSQL)) {

                String checkExistsSQL =
                        "SELECT id FROM variable WHERE type = ? AND name = ? AND instruction_id = ? AND bot_job_id = ?";
                String insertSQL =
                        "INSERT INTO variable (type, name, value, local_format, delimiter, instruction_id, bot_job_id, id) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement insertStmt = accessConn.prepareStatement(insertSQL);
                        PreparedStatement checkStmt = accessConn.prepareStatement(checkExistsSQL)) {

                    int count = 0;
                    variableMap.clear();

                    while (rsVariable.next()) {
                        int idVariable = rsVariable.getInt("id");
                        String type = rsVariable.getString("type");
                        String name = rsVariable.getString("name");

                        int oldBotJobId = rsVariable.getInt("bot_job_id");
                        Integer newBotJobId = botJobMap.get(oldBotJobId);
                        if (newBotJobId == null) {
                            System.out.println("Skipped variable with unknown bot_job_id: " + oldBotJobId);
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

                        variableMap.put(idVariable, -1); // initialize with -1

                        checkStmt.setString(1, type);
                        checkStmt.setString(2, name);
                        checkStmt.setInt(3, newInstructionId);
                        checkStmt.setInt(4, newBotJobId);

                        try (ResultSet checkRs = checkStmt.executeQuery()) {
                            if (!checkRs.next()) {

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

                                insertStmt.setInt(8, idVariable);

                                insertStmt.addBatch();
                                count++;
                            } else {
                                System.out.println("Skipped existing variable: " + name);
                            }
                        }

                        if (count % BATCH_SIZE == 0) {
                            insertStmt.executeBatch();
                            accessConn.commit();
                            System.out.println("Inserted batch of " + BATCH_SIZE);
                        }
                    }

                    // Final batch
                    if (count % BATCH_SIZE != 0) {
                        insertStmt.executeBatch();
                        accessConn.commit();
                        System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                    }
                }
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to export variables");
        }
    }

    public void exportUpdateInstruction() {
        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        final int BATCH_SIZE = 100;

        try (Connection postgresConn = DriverManager.getConnection(postgresDbUrl, userDB, userPwd);
                Statement postgresStmt = postgresConn.createStatement()) {
            postgresConn.setAutoCommit(false);

            String selectAccessSQL =
                    "SELECT id, name, parent_id, variable_id FROM instruction WHERE parent_id IS NOT NULL OR variable_id IS NOT NULL ORDER BY id";
            try (ResultSet rsInstruction = postgresStmt.executeQuery(selectAccessSQL)) {

                String updateSQL = "UPDATE instruction SET variable_id = ?, parent_id = ? WHERE id = ? ";

                try (PreparedStatement updateStmt = postgresConn.prepareStatement(updateSQL)) {
                    int count = 0;

                    while (rsInstruction.next()) {
                        int id = rsInstruction.getInt("id");
                        String name = rsInstruction.getString("name");

                        // Set variable_id directly from Access
                        int originalVariableId = rsInstruction.getInt("variable_id");
                        // Map old bot_job_id to new
                        Integer newVariableId = variableMap.get(originalVariableId);
                        if (newVariableId == null) {
                            System.out.println(
                                    "Skipped update variable column with unknown variable_id: " + newVariableId);
                            updateStmt.setNull(1, Types.INTEGER);
                        } else {
                            updateStmt.setInt(1, newVariableId);
                        }

                        // Handle parent_id based on name
                        if ("GOTO".equalsIgnoreCase(name) || "EXCEL GOTO".equalsIgnoreCase(name)) {
                            int parentBlockId = rsInstruction.getInt("parent_id");
                            Integer newParentBlockId = blockMap.get(parentBlockId);
                            if (newParentBlockId != null) {
                                updateStmt.setInt(2, newParentBlockId);
                            } else {
                                updateStmt.setNull(2, Types.INTEGER);
                            }
                        } else {
                            int parentInstructionId = rsInstruction.getInt("parent_id");
                            Integer newParentInstructionId = instructionMap.get(parentInstructionId);
                            if (newParentInstructionId != null) {
                                updateStmt.setInt(2, newParentInstructionId);
                            } else {
                                updateStmt.setNull(2, Types.INTEGER);
                            }
                        }

                        updateStmt.setInt(3, id); // WHERE clause: name = ?

                        updateStmt.addBatch();
                        count++;

                        if (count % BATCH_SIZE == 0) {
                            updateStmt.executeBatch();
                            postgresConn.commit();
                            System.out.println("Updated batch of " + BATCH_SIZE);
                        }
                    }

                    if (count % BATCH_SIZE != 0) {
                        updateStmt.executeBatch();
                        postgresConn.commit();
                        System.out.println("Updated final batch of " + (count % BATCH_SIZE));
                    }

                    ARLogger.getInstance(PerformDataBase.class).info("Updated instruction records: " + count);
                }
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to update instructions");
        }
    }

    public void exportUpdateInstructionAccess() {

        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
        String accessDbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;

        final int BATCH_SIZE = 100;

        try (Connection accessConn = DriverManager.getConnection(accessDbUrl);
                Statement accessStmt = accessConn.createStatement()) {

            accessConn.setAutoCommit(false);

            String selectAccessSQL =
                    "SELECT id, name, parent_id, variable_id FROM instruction WHERE parent_id IS NOT NULL OR variable_id IS NOT NULL ORDER BY id";
            try (ResultSet rsInstruction = accessStmt.executeQuery(selectAccessSQL)) {

                String updateSQL = "UPDATE instruction SET variable_id = ?, parent_id = ? WHERE id = ? ";

                try (PreparedStatement updateStmt = accessConn.prepareStatement(updateSQL)) {
                    int count = 0;

                    while (rsInstruction.next()) {
                        int id = rsInstruction.getInt("id");
                        String name = rsInstruction.getString("name");

                        // Set variable_id directly from Access
                        int originalVariableId = rsInstruction.getInt("variable_id");
                        // Map old bot_job_id to new
                        Integer newVariableId = variableMap.get(originalVariableId);
                        if (newVariableId == null) {
                            System.out.println(
                                    "Skipped update variable column with unknown variable_id: " + newVariableId);
                            updateStmt.setNull(1, Types.INTEGER);
                        } else {
                            updateStmt.setInt(1, newVariableId);
                        }

                        // Handle parent_id based on name
                        if ("GOTO".equalsIgnoreCase(name) || "EXCEL GOTO".equalsIgnoreCase(name)) {
                            int parentBlockId = rsInstruction.getInt("parent_id");
                            Integer newParentBlockId = blockMap.get(parentBlockId);
                            if (newParentBlockId != null) {
                                updateStmt.setInt(2, newParentBlockId);
                            } else {
                                updateStmt.setNull(2, Types.INTEGER);
                            }
                        } else {
                            int parentInstructionId = rsInstruction.getInt("parent_id");
                            Integer newParentInstructionId = instructionMap.get(parentInstructionId);
                            if (newParentInstructionId != null) {
                                updateStmt.setInt(2, newParentInstructionId);
                            } else {
                                updateStmt.setNull(2, Types.INTEGER);
                            }
                        }

                        updateStmt.setInt(3, id); // WHERE clause: name = ?

                        updateStmt.addBatch();
                        count++;

                        if (count % BATCH_SIZE == 0) {
                            updateStmt.executeBatch();
                            accessConn.commit();
                            System.out.println("Updated batch of " + BATCH_SIZE);
                        }
                    }

                    if (count % BATCH_SIZE != 0) {
                        updateStmt.executeBatch();
                        accessConn.commit();
                        System.out.println("Updated final batch of " + (count % BATCH_SIZE));
                    }

                    ARLogger.getInstance(PerformDataBase.class).info("Updated instruction records: " + count);
                }
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to update instructions");
        }
    }

    public void exportReferences() {
        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
        String accessDbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;
        ARLogger.getInstance(PerformDataBase.class).info("ACCESS connection URL: " + accessDbUrl);

        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        final int BATCH_SIZE = 100;

        try (Connection accessConn = DriverManager.getConnection(accessDbUrl);
                Connection postgresConn = DriverManager.getConnection(postgresDbUrl, userDB, userPwd);
                Statement accessStmt = accessConn.createStatement()) {

            postgresConn.setAutoCommit(false);

            String selectAccessSQL = "SELECT * FROM reference order by id";
            try (ResultSet rs = accessStmt.executeQuery(selectAccessSQL)) {

                String checkExistsSQL =
                        "SELECT id FROM reference WHERE reference_type = ? AND value = ? AND instruction_id = ? AND bot_job_id = ?";
                String insertSQL =
                        "INSERT INTO reference (reference_type, value, instruction_id, bot_job_id) VALUES (?, ?, ?, ?)";

                try (PreparedStatement insertStmt = postgresConn.prepareStatement(insertSQL);
                        PreparedStatement checkStmt = postgresConn.prepareStatement(checkExistsSQL)) {
                    int count = 0;
                    referenceMap.clear();

                    while (rs.next()) {
                        int id = rs.getInt("id");
                        String referenceType = rs.getString("reference_type");
                        String value = rs.getString("value");

                        int oldInstructionId = rs.getInt("instruction_id");
                        Integer newInstructionId = instructionMap.get(oldInstructionId);
                        if (newInstructionId == null) {
                            System.out.println("Skipped reference with unknown instruction_id: " + oldInstructionId);
                            continue;
                        }

                        int oldBotJobId = rs.getInt("bot_job_id");
                        Integer newBotJobId = botJobMap.get(oldBotJobId);

                        referenceMap.put(id, -1); // initialize with -1

                        checkStmt.setString(1, referenceType);
                        checkStmt.setString(2, value);
                        checkStmt.setInt(3, newInstructionId);
                        checkStmt.setInt(4, newBotJobId);

                        try (ResultSet checkRs = checkStmt.executeQuery()) {
                            if (!checkRs.next()) {

                                insertStmt.setString(1, rs.getString("reference_type"));
                                insertStmt.setString(2, rs.getString("value"));
                                insertStmt.setInt(3, newInstructionId);

                                if (newBotJobId != null) {
                                    insertStmt.setInt(4, newBotJobId);
                                } else {
                                    insertStmt.setNull(4, Types.INTEGER);
                                }

                                insertStmt.addBatch();
                                count++;
                            } else {
                                System.out.println("Skipped existing reference: " + referenceType);
                            }
                        }

                        if (count % BATCH_SIZE == 0) {
                            insertStmt.executeBatch();
                            postgresConn.commit();
                            System.out.println("Inserted batch of " + BATCH_SIZE);
                        }
                    }

                    // Final batch
                    if (count % BATCH_SIZE != 0) {
                        insertStmt.executeBatch();
                        postgresConn.commit();
                        System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                    }
                }
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to export reference table");
        }
    }

    public void exportReferencesAccess() {
        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
        String accessDbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;
        ARLogger.getInstance(PerformDataBase.class).info("ACCESS connection URL: " + accessDbUrl);

        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        final int BATCH_SIZE = 100;

        try (Connection accessConn = DriverManager.getConnection(accessDbUrl);
                Connection postgresConn = DriverManager.getConnection(postgresDbUrl, userDB, userPwd);
                Statement postgresStmt = postgresConn.createStatement()) {

            accessConn.setAutoCommit(false);

            String selectPostgresSQL = "SELECT * FROM reference order by id";
            try (ResultSet rs = postgresStmt.executeQuery(selectPostgresSQL)) {

                String checkExistsSQL =
                        "SELECT id FROM reference WHERE reference_type = ? AND value = ? AND instruction_id = ? AND bot_job_id = ?";
                String insertSQL =
                        "INSERT INTO reference (reference_type, value, instruction_id, bot_job_id, id) VALUES (?, ?, ?, ?, ?)";

                try (PreparedStatement insertStmt = accessConn.prepareStatement(insertSQL);
                        PreparedStatement checkStmt = accessConn.prepareStatement(checkExistsSQL)) {
                    int count = 0;
                    referenceMap.clear();

                    while (rs.next()) {
                        int idRefer = rs.getInt("id");
                        String referenceType = rs.getString("reference_type");
                        String value = rs.getString("value");

                        int oldInstructionId = rs.getInt("instruction_id");
                        Integer newInstructionId = instructionMap.get(oldInstructionId);
                        if (newInstructionId == null) {
                            System.out.println("Skipped reference with unknown instruction_id: " + oldInstructionId);
                            continue;
                        }

                        int oldBotJobId = rs.getInt("bot_job_id");
                        Integer newBotJobId = botJobMap.get(oldBotJobId);

                        referenceMap.put(idRefer, -1); // initialize with -1

                        checkStmt.setString(1, referenceType);
                        checkStmt.setString(2, value);
                        checkStmt.setInt(3, newInstructionId);
                        checkStmt.setInt(4, newBotJobId);

                        try (ResultSet checkRs = checkStmt.executeQuery()) {
                            if (!checkRs.next()) {

                                insertStmt.setString(1, rs.getString("reference_type"));
                                insertStmt.setString(2, rs.getString("value"));
                                insertStmt.setInt(3, newInstructionId);

                                if (newBotJobId != null) {
                                    insertStmt.setInt(4, newBotJobId);
                                } else {
                                    insertStmt.setNull(4, Types.INTEGER);
                                }

                                insertStmt.setInt(5, idRefer);

                                insertStmt.addBatch();
                                count++;
                            } else {
                                System.out.println("Skipped existing reference: " + referenceType);
                            }
                        }

                        if (count % BATCH_SIZE == 0) {
                            insertStmt.executeBatch();
                            accessConn.commit();
                            System.out.println("Inserted batch of " + BATCH_SIZE);
                        }
                    }

                    // Final batch
                    if (count % BATCH_SIZE != 0) {
                        insertStmt.executeBatch();
                        accessConn.commit();
                        System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                    }
                }
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to export reference table");
        }
    }

    // EXPORT SAVED COMPONENTS
    public void exportCompBlock() {
        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
        String accessDbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;
        ARLogger.getInstance(PerformDataBase.class).info("ACCESS connection URL: " + accessDbUrl);

        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        final int BATCH_SIZE = 100;

        try (Connection accessConn = DriverManager.getConnection(accessDbUrl);
                Connection postgresConn = DriverManager.getConnection(postgresDbUrl, userDB, userPwd);
                Statement accessStmt = accessConn.createStatement()) {
            postgresConn.setAutoCommit(false);

            String selectAccessSQL =
                    "SELECT id, block_order_number, name, description, type_id, export_file, active, wait, home_banking_id FROM component_block ORDER BY id";

            try (ResultSet rs = accessStmt.executeQuery(selectAccessSQL)) {

                String checkExistsSQL =
                        "SELECT id FROM component_block WHERE block_order_number = ? AND name = ? AND home_banking_id = ?";
                String insertSQL =
                        "INSERT INTO component_block (block_order_number, name, description, type_id, export_file, active, wait, home_banking_id) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

                blockMap = new TreeMap<>();

                try (PreparedStatement insertStmt = postgresConn.prepareStatement(insertSQL);
                        PreparedStatement checkStmt = postgresConn.prepareStatement(checkExistsSQL)) {

                    int count = 0;
                    blockMap.clear();

                    while (rs.next()) {
                        int id = rs.getInt("id");
                        int blockOrderNumber = rs.getInt("block_order_number");
                        String name = rs.getString("name");
                        String description = rs.getString("description");
                        Integer typeId = rs.getObject("type_id") != null ? rs.getInt("type_id") : null;
                        String exportFile = rs.getString("export_file");
                        int active = rs.getInt("active");
                        Integer wait = rs.getObject("wait") != null ? rs.getInt("wait") : null;
                        int oldHomeBankId = rs.getInt("home_banking_id");

                        blockMap.put(id, -1); // initialize with -1

                        // Map old bot_job_id to new
                        Integer newHomeBankId = homeBankMap.get(oldHomeBankId);
                        if (newHomeBankId == null) {
                            System.out.println(
                                    "Skipped component_block with unknown home_banking_id: " + newHomeBankId);
                            continue;
                        }

                        checkStmt.setInt(1, blockOrderNumber);
                        checkStmt.setString(2, name);
                        checkStmt.setInt(3, newHomeBankId);

                        try (ResultSet checkRs = checkStmt.executeQuery()) {
                            if (!checkRs.next()) {
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

                                insertStmt.setInt(8, newHomeBankId);

                                insertStmt.addBatch();
                                count++;
                            } else {
                                System.out.println("Skipped existing component_block: " + name);
                            }
                        }

                        if (count % BATCH_SIZE == 0) {
                            insertStmt.executeBatch();
                            postgresConn.commit();
                            System.out.println("Inserted batch of " + BATCH_SIZE);
                        }
                    }

                    // Final batch
                    if (count % BATCH_SIZE != 0) {
                        insertStmt.executeBatch();
                        postgresConn.commit();
                        System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                    }
                }
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to export component_block");
        }
    }

    public void exportCompBlockAccess() {
        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
        String accessDbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;
        ARLogger.getInstance(PerformDataBase.class).info("ACCESS connection URL: " + accessDbUrl);

        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        final int BATCH_SIZE = 100;

        try (Connection accessConn = DriverManager.getConnection(accessDbUrl);
                Connection postgresConn = DriverManager.getConnection(postgresDbUrl, userDB, userPwd);
                Statement postgresStmt = postgresConn.createStatement()) {

            accessConn.setAutoCommit(false);

            String selectPostgresSQL =
                    "SELECT id, block_order_number, name, description, type_id, export_file, active, wait, home_banking_id FROM component_block ORDER BY id";

            try (ResultSet rs = postgresStmt.executeQuery(selectPostgresSQL)) {

                String checkExistsSQL =
                        "SELECT id FROM component_block WHERE block_order_number = ? AND name = ? AND home_banking_id = ?";
                String insertSQL =
                        "INSERT INTO component_block (block_order_number, name, description, type_id, export_file, active, wait, home_banking_id, id) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

                blockMap = new TreeMap<>();

                try (PreparedStatement insertStmt = accessConn.prepareStatement(insertSQL);
                        PreparedStatement checkStmt = accessConn.prepareStatement(checkExistsSQL)) {

                    int count = 0;
                    blockMap.clear();

                    while (rs.next()) {
                        int idCompBlock = rs.getInt("id");
                        int blockOrderNumber = rs.getInt("block_order_number");
                        String name = rs.getString("name");
                        String description = rs.getString("description");
                        Integer typeId = rs.getObject("type_id") != null ? rs.getInt("type_id") : null;
                        String exportFile = rs.getString("export_file");
                        int active = rs.getInt("active");
                        Integer wait = rs.getObject("wait") != null ? rs.getInt("wait") : null;
                        int oldHomeBankId = rs.getInt("home_banking_id");

                        // Map old bot_job_id to new
                        Integer newHomeBankId = homeBankMap.get(oldHomeBankId);
                        if (newHomeBankId == null) {
                            System.out.println(
                                    "Skipped component_block with unknown home_banking_id: " + newHomeBankId);
                            continue;
                        }

                        blockMap.put(idCompBlock, -1); // initialize with -1

                        checkStmt.setInt(1, blockOrderNumber);
                        checkStmt.setString(2, name);
                        checkStmt.setInt(3, newHomeBankId);

                        try (ResultSet checkRs = checkStmt.executeQuery()) {
                            if (!checkRs.next()) {
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

                                insertStmt.setInt(8, newHomeBankId);

                                insertStmt.setInt(9, idCompBlock);

                                insertStmt.addBatch();
                                count++;
                            } else {
                                System.out.println("Skipped existing component_block: " + name);
                            }
                        }

                        if (count % BATCH_SIZE == 0) {
                            insertStmt.executeBatch();
                            accessConn.commit();
                            System.out.println("Inserted batch of " + BATCH_SIZE);
                        }
                    }

                    // Final batch
                    if (count % BATCH_SIZE != 0) {
                        insertStmt.executeBatch();
                        accessConn.commit();
                        System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                    }
                }
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to export component_block");
        }
    }

    public void exportCompInstructions() {
        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
        String accessDbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;
        ARLogger.getInstance(PerformDataBase.class).info("ACCESS connection URL: " + accessDbUrl);

        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        final int BATCH_SIZE = 100;

        try (Connection accessConn = DriverManager.getConnection(accessDbUrl);
                Connection postgresConn = DriverManager.getConnection(postgresDbUrl, userDB, userPwd);
                Statement accessStmt = accessConn.createStatement()) {

            postgresConn.setAutoCommit(false);

            String selectAccessSQL = "SELECT * FROM component_instruction order by id";
            try (ResultSet rsInstruction = accessStmt.executeQuery(selectAccessSQL)) {

                String checkExistsSQL =
                        "SELECT id FROM component_instruction WHERE instruction_order_number = ? AND name = ? AND home_banking_id = ? AND block_id = ?";
                String insertSQL = "INSERT INTO component_instruction ("
                        + "instruction_order_number, actions, name, xpath, coordinates, force_coordinates, iframe_xpath, tag_name, shadow_host, shadow_root, css_selector, description, operation, optional, block_marked, default_value, action_custom_max_wait_sec, on_hold_seconds, codified, export_to_abr, active, block_id, variable_id, parent_id, home_banking_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement insertStmt = postgresConn.prepareStatement(insertSQL);
                        PreparedStatement checkStmt = postgresConn.prepareStatement(checkExistsSQL)) {

                    int count = 0;
                    instructionMap.clear();

                    while (rsInstruction.next()) {

                        int id = rsInstruction.getInt("id");
                        int oldHomeBankId = rsInstruction.getInt("home_banking_id");
                        Integer newHomeBankId = homeBankMap.get(oldHomeBankId);
                        if (newHomeBankId == null) {
                            System.out.println(
                                    "Skipped component_instruction with unknown home_banking_id: " + newHomeBankId);
                            continue;
                        }

                        int oldBlockId = rsInstruction.getInt("block_id");
                        Integer newBlockId = blockMap.get(oldBlockId);
                        if (newBlockId == null) {
                            System.out.println("Skipped component_instruction with unknown block_id: " + oldBlockId);
                            continue;
                        }

                        int instructionOrderNumber = rsInstruction.getInt("instruction_order_number");
                        String name = rsInstruction.getString("name");

                        instructionMap.put(id, -1); // initialize with -1

                        checkStmt.setInt(1, instructionOrderNumber);
                        checkStmt.setString(2, name);
                        checkStmt.setInt(3, newHomeBankId);
                        checkStmt.setInt(4, newBlockId);

                        try (ResultSet checkRs = checkStmt.executeQuery()) {
                            if (!checkRs.next()) {

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

                                insertOrNull(insertStmt, 23, rsInstruction, "variable_id");
                                insertOrNull(insertStmt, 24, rsInstruction, "parent_id");

                                insertStmt.setInt(25, newHomeBankId);

                                insertStmt.addBatch();
                                count++;
                            } else {
                                System.out.println("Skipped existing component_instruction: " + name);
                            }
                        }
                        if (count % BATCH_SIZE == 0) {
                            insertStmt.executeBatch();
                            postgresConn.commit();
                            System.out.println("Inserted batch of " + BATCH_SIZE);
                        }
                    }

                    if (count % BATCH_SIZE != 0) {
                        insertStmt.executeBatch();
                        postgresConn.commit();
                        System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                    }
                }
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to export component_instruction");
        }
    }

    public void exportCompInstructionsAccess() {
        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
        String accessDbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;
        ARLogger.getInstance(PerformDataBase.class).info("ACCESS connection URL: " + accessDbUrl);

        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        final int BATCH_SIZE = 100;

        try (Connection accessConn = DriverManager.getConnection(accessDbUrl);
                Connection postgresConn = DriverManager.getConnection(postgresDbUrl, userDB, userPwd);
                Statement postgresStmt = postgresConn.createStatement()) {

            accessConn.setAutoCommit(false);

            String selectPostgreSQL = "SELECT * FROM component_instruction order by id";
            try (ResultSet rsInstruction = postgresStmt.executeQuery(selectPostgreSQL)) {

                String checkExistsSQL =
                        "SELECT id FROM component_instruction WHERE instruction_order_number = ? AND name = ? AND home_banking_id = ? AND block_id = ?";
                String insertSQL = "INSERT INTO component_instruction ("
                        + "instruction_order_number, actions, name, xpath, coordinates, force_coordinates, iframe_xpath, tag_name, shadow_host, shadow_root, css_selector, description, operation, optional, block_marked, default_value, action_custom_max_wait_sec, on_hold_seconds, codified, export_to_abr, active, block_id, variable_id, parent_id, home_banking_id, id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement insertStmt = accessConn.prepareStatement(insertSQL);
                        PreparedStatement checkStmt = accessConn.prepareStatement(checkExistsSQL)) {

                    int count = 0;
                    instructionMap.clear();

                    while (rsInstruction.next()) {

                        int idCompInstruc = rsInstruction.getInt("id");
                        int oldHomeBankId = rsInstruction.getInt("home_banking_id");
                        Integer newHomeBankId = homeBankMap.get(oldHomeBankId);
                        if (newHomeBankId == null) {
                            System.out.println(
                                    "Skipped component_instruction with unknown home_banking_id: " + newHomeBankId);
                            continue;
                        }

                        int oldBlockId = rsInstruction.getInt("block_id");
                        Integer newBlockId = blockMap.get(oldBlockId);
                        if (newBlockId == null) {
                            System.out.println("Skipped component_instruction with unknown block_id: " + oldBlockId);
                            continue;
                        }

                        int instructionOrderNumber = rsInstruction.getInt("instruction_order_number");
                        String name = rsInstruction.getString("name");

                        instructionMap.put(idCompInstruc, -1); // initialize with -1

                        checkStmt.setInt(1, instructionOrderNumber);
                        checkStmt.setString(2, name);
                        checkStmt.setInt(3, newHomeBankId);
                        checkStmt.setInt(4, newBlockId);

                        try (ResultSet checkRs = checkStmt.executeQuery()) {
                            if (!checkRs.next()) {

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

                                insertOrNull(insertStmt, 23, rsInstruction, "variable_id");
                                insertOrNull(insertStmt, 24, rsInstruction, "parent_id");

                                insertStmt.setInt(25, newHomeBankId);

                                insertStmt.setInt(26, idCompInstruc);

                                insertStmt.addBatch();
                                count++;
                            } else {
                                System.out.println("Skipped existing component_instruction: " + name);
                            }
                        }
                        if (count % BATCH_SIZE == 0) {
                            insertStmt.executeBatch();
                            accessConn.commit();
                            System.out.println("Inserted batch of " + BATCH_SIZE);
                        }
                    }

                    if (count % BATCH_SIZE != 0) {
                        insertStmt.executeBatch();
                        accessConn.commit();
                        System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                    }
                }
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to export component_instruction");
        }
    }

    public void exportCompVariables() {
        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
        String accessDbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;
        ARLogger.getInstance(PerformDataBase.class).info("ACCESS connection URL: " + accessDbUrl);

        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        final int BATCH_SIZE = 100;

        try (Connection accessConn = DriverManager.getConnection(accessDbUrl);
                Connection postgresConn = DriverManager.getConnection(postgresDbUrl, userDB, userPwd);
                Statement accessStmt = accessConn.createStatement()) {

            postgresConn.setAutoCommit(false);

            String selectAccessSQL = "SELECT * FROM component_variable order by id";
            try (ResultSet rsVariable = accessStmt.executeQuery(selectAccessSQL)) {

                String checkExistsSQL =
                        "SELECT id FROM component_variable WHERE type = ? AND name = ? AND instruction_id = ? AND home_banking_id = ?";
                String insertSQL =
                        "INSERT INTO component_variable (type, name, value, local_format, delimiter, instruction_id, home_banking_id) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement insertStmt = postgresConn.prepareStatement(insertSQL);
                        PreparedStatement checkStmt = postgresConn.prepareStatement(checkExistsSQL)) {

                    int count = 0;
                    variableMap.clear();

                    while (rsVariable.next()) {
                        int id = rsVariable.getInt("id");
                        String type = rsVariable.getString("type");
                        String name = rsVariable.getString("name");

                        int oldHomeBankId = rsVariable.getInt("home_banking_id");
                        Integer newHomeBankId = homeBankMap.get(oldHomeBankId);
                        if (newHomeBankId == null) {
                            System.out.println(
                                    "Skipped component_variable with unknown home_banking_id: " + oldHomeBankId);
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

                        variableMap.put(id, -1); // initialize with -1

                        checkStmt.setString(1, type);
                        checkStmt.setString(2, name);
                        checkStmt.setInt(3, newInstructionId);
                        checkStmt.setInt(4, newHomeBankId);

                        try (ResultSet checkRs = checkStmt.executeQuery()) {
                            if (!checkRs.next()) {

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
                            } else {
                                System.out.println("Skipped existing component_variable: " + name);
                            }
                        }

                        if (count % BATCH_SIZE == 0) {
                            insertStmt.executeBatch();
                            postgresConn.commit();
                            System.out.println("Inserted batch of " + BATCH_SIZE);
                        }
                    }

                    // Final batch
                    if (count % BATCH_SIZE != 0) {
                        insertStmt.executeBatch();
                        postgresConn.commit();
                        System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                    }
                }
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to export component_variable");
        }
    }

    public void exportCompVariablesAccess() {
        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
        String accessDbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;
        ARLogger.getInstance(PerformDataBase.class).info("ACCESS connection URL: " + accessDbUrl);

        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        final int BATCH_SIZE = 100;

        try (Connection accessConn = DriverManager.getConnection(accessDbUrl);
                Connection postgresConn = DriverManager.getConnection(postgresDbUrl, userDB, userPwd);
                Statement postgresStmt = postgresConn.createStatement()) {

            accessConn.setAutoCommit(false);

            String selectPostgresSQL = "SELECT * FROM component_variable order by id";
            try (ResultSet rsVariable = postgresStmt.executeQuery(selectPostgresSQL)) {

                String checkExistsSQL =
                        "SELECT id FROM component_variable WHERE type = ? AND name = ? AND instruction_id = ? AND home_banking_id = ?";
                String insertSQL =
                        "INSERT INTO component_variable (type, name, value, local_format, delimiter, instruction_id, home_banking_id, id) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement insertStmt = accessConn.prepareStatement(insertSQL);
                        PreparedStatement checkStmt = accessConn.prepareStatement(checkExistsSQL)) {

                    int count = 0;
                    variableMap.clear();

                    while (rsVariable.next()) {
                        int idVariable = rsVariable.getInt("id");
                        String type = rsVariable.getString("type");
                        String name = rsVariable.getString("name");

                        int oldHomeBankId = rsVariable.getInt("home_banking_id");
                        Integer newHomeBankId = homeBankMap.get(oldHomeBankId);
                        if (newHomeBankId == null) {
                            System.out.println(
                                    "Skipped component_variable with unknown home_banking_id: " + oldHomeBankId);
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

                        variableMap.put(idVariable, -1); // initialize with -1

                        checkStmt.setString(1, type);
                        checkStmt.setString(2, name);
                        checkStmt.setInt(3, newInstructionId);
                        checkStmt.setInt(4, newHomeBankId);

                        try (ResultSet checkRs = checkStmt.executeQuery()) {
                            if (!checkRs.next()) {

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

                                insertStmt.setInt(8, idVariable);

                                insertStmt.addBatch();
                                count++;
                            } else {
                                System.out.println("Skipped existing component_variable: " + name);
                            }
                        }

                        if (count % BATCH_SIZE == 0) {
                            insertStmt.executeBatch();
                            accessConn.commit();
                            System.out.println("Inserted batch of " + BATCH_SIZE);
                        }
                    }

                    // Final batch
                    if (count % BATCH_SIZE != 0) {
                        insertStmt.executeBatch();
                        accessConn.commit();
                        System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                    }
                }
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to export component_variable");
        }
    }

    public void exportUpdateCompInstruction() {
        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        final int BATCH_SIZE = 100;

        try (Connection postgresConn = DriverManager.getConnection(postgresDbUrl, userDB, userPwd);
                Statement postgresStmt = postgresConn.createStatement()) {

            postgresConn.setAutoCommit(false);

            String selectPostgresSQL =
                    "SELECT id, name, parent_id, variable_id FROM component_instruction WHERE parent_id IS NOT NULL OR variable_id IS NOT NULL ORDER BY id";
            try (ResultSet rsInstruction = postgresStmt.executeQuery(selectPostgresSQL)) {

                String updateSQL = "UPDATE instruction SET variable_id = ?, parent_id = ? WHERE id = ? ";

                try (PreparedStatement updateStmt = postgresConn.prepareStatement(updateSQL)) {
                    int count = 0;

                    while (rsInstruction.next()) {
                        int id = rsInstruction.getInt("id");
                        String name = rsInstruction.getString("name");

                        // Set variable_id directly from Access
                        int originalVariableId = rsInstruction.getInt("variable_id");
                        // Map old bot_job_id to new
                        Integer newVariableId = variableMap.get(originalVariableId);
                        if (newVariableId == null) {
                            System.out.println("Skipped variable_id column with unknown variable_id: " + newVariableId);
                            updateStmt.setNull(1, Types.INTEGER);
                        } else {
                            updateStmt.setInt(1, newVariableId);
                        }

                        // Handle parent_id based on name
                        if ("GOTO".equalsIgnoreCase(name) || "EXCEL GOTO".equalsIgnoreCase(name)) {
                            int parentBlockId = rsInstruction.getInt("parent_id");
                            Integer newParentBlockId = blockMap.get(parentBlockId);
                            if (newParentBlockId != null) {
                                updateStmt.setInt(2, newParentBlockId);
                            } else {
                                updateStmt.setNull(2, Types.INTEGER);
                            }
                        } else {
                            int parentInstructionId = rsInstruction.getInt("parent_id");
                            Integer newParentInstructionId = instructionMap.get(parentInstructionId);
                            if (newParentInstructionId != null) {
                                updateStmt.setInt(2, newParentInstructionId);
                            } else {
                                updateStmt.setNull(2, Types.INTEGER);
                            }
                        }

                        updateStmt.setInt(3, id); // WHERE clause: name = ?

                        updateStmt.addBatch();
                        count++;

                        if (count % BATCH_SIZE == 0) {
                            updateStmt.executeBatch();
                            postgresConn.commit();
                            System.out.println("Updated batch of " + BATCH_SIZE);
                        }
                    }

                    if (count % BATCH_SIZE != 0) {
                        updateStmt.executeBatch();
                        postgresConn.commit();
                        System.out.println("Updated final batch of " + (count % BATCH_SIZE));
                    }

                    ARLogger.getInstance(PerformDataBase.class).info("Updated component_instruction records: " + count);
                }
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to update component_instruction");
        }
    }

    public void exportUpdateCompInstructionAccess() {

        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
        String accessDbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;

        final int BATCH_SIZE = 100;

        try (Connection accessConn = DriverManager.getConnection(accessDbUrl);
                Statement accessStmt = accessConn.createStatement()) {

            accessConn.setAutoCommit(false);

            String selectAccessSQL =
                    "SELECT id, name, parent_id, variable_id FROM component_instruction WHERE parent_id IS NOT NULL OR variable_id IS NOT NULL ORDER BY id";
            try (ResultSet rsInstruction = accessStmt.executeQuery(selectAccessSQL)) {

                String updateSQL = "UPDATE instruction SET variable_id = ?, parent_id = ? WHERE id = ? ";

                try (PreparedStatement updateStmt = accessConn.prepareStatement(updateSQL)) {
                    int count = 0;

                    while (rsInstruction.next()) {
                        int id = rsInstruction.getInt("id");
                        String name = rsInstruction.getString("name");

                        // Set variable_id directly from Access
                        int originalVariableId = rsInstruction.getInt("variable_id");
                        // Map old bot_job_id to new
                        Integer newVariableId = variableMap.get(originalVariableId);
                        if (newVariableId == null) {
                            System.out.println("Skipped variable_id column with unknown variable_id: " + newVariableId);
                            updateStmt.setNull(1, Types.INTEGER);
                        } else {
                            updateStmt.setInt(1, newVariableId);
                        }

                        // Handle parent_id based on name
                        if ("GOTO".equalsIgnoreCase(name) || "EXCEL GOTO".equalsIgnoreCase(name)) {
                            int parentBlockId = rsInstruction.getInt("parent_id");
                            Integer newParentBlockId = blockMap.get(parentBlockId);
                            if (newParentBlockId != null) {
                                updateStmt.setInt(2, newParentBlockId);
                            } else {
                                updateStmt.setNull(2, Types.INTEGER);
                            }
                        } else {
                            int parentInstructionId = rsInstruction.getInt("parent_id");
                            Integer newParentInstructionId = instructionMap.get(parentInstructionId);
                            if (newParentInstructionId != null) {
                                updateStmt.setInt(2, newParentInstructionId);
                            } else {
                                updateStmt.setNull(2, Types.INTEGER);
                            }
                        }

                        updateStmt.setInt(3, id); // WHERE clause: name = ?

                        updateStmt.addBatch();
                        count++;

                        if (count % BATCH_SIZE == 0) {
                            updateStmt.executeBatch();
                            accessConn.commit();
                            System.out.println("Updated batch of " + BATCH_SIZE);
                        }
                    }

                    if (count % BATCH_SIZE != 0) {
                        updateStmt.executeBatch();
                        accessConn.commit();
                        System.out.println("Updated final batch of " + (count % BATCH_SIZE));
                    }

                    ARLogger.getInstance(PerformDataBase.class).info("Updated component_instruction records: " + count);
                }
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to update component_instruction");
        }
    }

    public void exportCompReferences() {
        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
        String accessDbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;
        ARLogger.getInstance(PerformDataBase.class).info("ACCESS connection URL: " + accessDbUrl);

        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        final int BATCH_SIZE = 100;

        try (Connection accessConn = DriverManager.getConnection(accessDbUrl);
                Connection postgresConn = DriverManager.getConnection(postgresDbUrl, userDB, userPwd);
                Statement accessStmt = accessConn.createStatement()) {

            postgresConn.setAutoCommit(false);

            String selectAccessSQL = "SELECT * FROM component_reference order by id";
            try (ResultSet rs = accessStmt.executeQuery(selectAccessSQL)) {

                String checkExistsSQL =
                        "SELECT id FROM component_reference WHERE reference_type = ? AND value = ? AND instruction_id = ? AND home_banking_id = ?";
                String insertSQL =
                        "INSERT INTO component_reference (reference_type, value, instruction_id, home_banking_id) VALUES (?, ?, ?, ?)";

                try (PreparedStatement insertStmt = postgresConn.prepareStatement(insertSQL);
                        PreparedStatement checkStmt = postgresConn.prepareStatement(checkExistsSQL)) {
                    int count = 0;
                    referenceMap.clear();

                    while (rs.next()) {
                        int id = rs.getInt("id");
                        String referenceType = rs.getString("reference_type");
                        String value = rs.getString("value");

                        int oldInstructionId = rs.getInt("instruction_id");
                        Integer newInstructionId = instructionMap.get(oldInstructionId);
                        if (newInstructionId == null) {
                            System.out.println(
                                    "Skipped component_reference with unknown instruction_id: " + oldInstructionId);
                            continue;
                        }

                        int oldHomeBankId = rs.getInt("home_banking_id");
                        Integer newHomeBankId = homeBankMap.get(oldHomeBankId);
                        if (newHomeBankId == null) {
                            System.out.println(
                                    "Skipped component_reference with unknown home_banking_id: " + oldHomeBankId);
                            continue;
                        }

                        referenceMap.put(id, -1); // initialize with -1

                        checkStmt.setString(1, referenceType);
                        checkStmt.setString(2, value);
                        checkStmt.setInt(3, newInstructionId);
                        checkStmt.setInt(4, newHomeBankId);

                        try (ResultSet checkRs = checkStmt.executeQuery()) {
                            if (!checkRs.next()) {

                                insertStmt.setString(1, rs.getString("reference_type"));
                                insertStmt.setString(2, rs.getString("value"));
                                insertStmt.setInt(3, newInstructionId);

                                if (newHomeBankId != null) {
                                    insertStmt.setInt(4, newHomeBankId);
                                } else {
                                    insertStmt.setNull(4, Types.INTEGER);
                                }

                                insertStmt.addBatch();
                                count++;
                            } else {
                                System.out.println("Skipped existing component_reference: " + referenceType);
                            }
                        }

                        if (count % BATCH_SIZE == 0) {
                            insertStmt.executeBatch();
                            postgresConn.commit();
                            System.out.println("Inserted batch of " + BATCH_SIZE);
                        }
                    }

                    // Final batch
                    if (count % BATCH_SIZE != 0) {
                        insertStmt.executeBatch();
                        postgresConn.commit();
                        System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                    }
                }
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to export component_reference table");
        }
    }

    public void exportCompReferencesAccess() {
        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
        String accessDbUrl = CONNECTION_TYPE + dbPath + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;
        ARLogger.getInstance(PerformDataBase.class).info("ACCESS connection URL: " + accessDbUrl);

        String postgresDbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
        String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
        String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

        final int BATCH_SIZE = 100;

        try (Connection accessConn = DriverManager.getConnection(accessDbUrl);
                Connection postgresConn = DriverManager.getConnection(postgresDbUrl, userDB, userPwd);
                Statement postgresStmt = postgresConn.createStatement()) {

            accessConn.setAutoCommit(false);

            String selectPostgresSQL = "SELECT * FROM component_reference order by id";
            try (ResultSet rs = postgresStmt.executeQuery(selectPostgresSQL)) {

                String checkExistsSQL =
                        "SELECT id FROM component_reference WHERE reference_type = ? AND value = ? AND instruction_id = ? AND home_banking_id = ?";
                String insertSQL =
                        "INSERT INTO component_reference (reference_type, value, instruction_id, home_banking_id, ?) VALUES (?, ?, ?, ?, ?)";

                try (PreparedStatement insertStmt = accessConn.prepareStatement(insertSQL);
                        PreparedStatement checkStmt = accessConn.prepareStatement(checkExistsSQL)) {
                    int count = 0;
                    referenceMap.clear();

                    while (rs.next()) {
                        int idRefer = rs.getInt("id");
                        String referenceType = rs.getString("reference_type");
                        String value = rs.getString("value");

                        int oldInstructionId = rs.getInt("instruction_id");
                        Integer newInstructionId = instructionMap.get(oldInstructionId);
                        if (newInstructionId == null) {
                            System.out.println(
                                    "Skipped component_reference with unknown instruction_id: " + oldInstructionId);
                            continue;
                        }

                        int oldHomeBankId = rs.getInt("home_banking_id");
                        Integer newHomeBankId = homeBankMap.get(oldHomeBankId);
                        if (newHomeBankId == null) {
                            System.out.println(
                                    "Skipped component_reference with unknown home_banking_id: " + oldHomeBankId);
                            continue;
                        }

                        referenceMap.put(idRefer, -1); // initialize with -1

                        checkStmt.setString(1, referenceType);
                        checkStmt.setString(2, value);
                        checkStmt.setInt(3, newInstructionId);
                        checkStmt.setInt(4, newHomeBankId);

                        try (ResultSet checkRs = checkStmt.executeQuery()) {
                            if (!checkRs.next()) {

                                insertStmt.setString(1, rs.getString("reference_type"));
                                insertStmt.setString(2, rs.getString("value"));
                                insertStmt.setInt(3, newInstructionId);

                                if (newHomeBankId != null) {
                                    insertStmt.setInt(4, newHomeBankId);
                                } else {
                                    insertStmt.setNull(4, Types.INTEGER);
                                }

                                insertStmt.setInt(5, idRefer);

                                insertStmt.addBatch();
                                count++;
                            } else {
                                System.out.println("Skipped existing component_reference: " + referenceType);
                            }
                        }

                        if (count % BATCH_SIZE == 0) {
                            insertStmt.executeBatch();
                            accessConn.commit();
                            System.out.println("Inserted batch of " + BATCH_SIZE);
                        }
                    }

                    // Final batch
                    if (count % BATCH_SIZE != 0) {
                        insertStmt.executeBatch();
                        accessConn.commit();
                        System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                    }
                }
            }

        } catch (SQLException e) {
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to export component_reference table");
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
                        + "(id, block_order_number, name, description, type_id, export_file, active, wait, home_banking_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

                blockMap.clear();

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {

                    int count = 0;
                    int newCompBlock = getMaxId(conn, "component_block") + 1;

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

                        insertStmt.setInt(1, newCompBlock);
                        insertStmt.setInt(2, blockOrderNumber);
                        insertStmt.setString(3, blockDetailsDTO.getBlockName());
                        insertStmt.setString(4, blockDetailsDTO.getBlockDescription());
                        if (typeId != null) {
                            insertStmt.setInt(5, typeId);
                        } else {
                            insertStmt.setNull(5, Types.INTEGER);
                        }
                        insertStmt.setString(6, exportFile);
                        insertStmt.setInt(7, active);
                        if (wait != null) {
                            insertStmt.setInt(8, wait);
                        } else {
                            insertStmt.setNull(8, Types.INTEGER);
                        }
                        insertStmt.setInt(9, newHomeBankId);

                        insertStmt.addBatch();
                        count++;
                        newCompBlock++;

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
                        + "id, instruction_order_number, actions, name, xpath, coordinates, force_coordinates, iframe_xpath, tag_name, shadow_host, shadow_root, css_selector, description, operation, optional, block_marked, default_value, action_custom_max_wait_sec, on_hold_seconds, codified, export_to_abr, active, block_id, variable_id, parent_id, home_banking_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                instructionMap.clear();

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {

                    int count = 0;
                    int newCompInstr = getMaxId(conn, "component_instruction") + 1;

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

                        insertStmt.setInt(1, newCompInstr);
                        insertStmt.setInt(2, rsInstruction.getInt("instruction_order_number"));
                        insertStmt.setString(3, rsInstruction.getString("actions"));
                        insertStmt.setString(4, rsInstruction.getString("name"));
                        insertStmt.setString(5, rsInstruction.getString("xpath"));
                        insertStmt.setString(6, rsInstruction.getString("coordinates"));

                        insertOrNull(insertStmt, 7, rsInstruction, "force_coordinates");
                        insertStmt.setString(8, rsInstruction.getString("iframe_xpath"));
                        insertStmt.setString(9, rsInstruction.getString("tag_name"));
                        insertStmt.setString(10, rsInstruction.getString("shadow_host"));
                        insertStmt.setString(11, rsInstruction.getString("shadow_root"));
                        insertStmt.setString(12, rsInstruction.getString("css_selector"));
                        insertStmt.setString(13, rsInstruction.getString("description"));
                        insertStmt.setString(14, rsInstruction.getString("operation"));

                        insertOrNull(insertStmt, 15, rsInstruction, "optional");
                        insertOrNull(insertStmt, 16, rsInstruction, "block_marked");
                        insertStmt.setString(17, rsInstruction.getString("default_value"));
                        insertOrNull(insertStmt, 18, rsInstruction, "action_custom_max_wait_sec");
                        insertOrNull(insertStmt, 19, rsInstruction, "on_hold_seconds");
                        insertOrNull(insertStmt, 20, rsInstruction, "codified");
                        insertOrNull(insertStmt, 21, rsInstruction, "export_to_abr");

                        insertStmt.setInt(22, rsInstruction.getInt("active"));
                        insertStmt.setInt(23, newBlockId);

                        insertOrNull(insertStmt, 24, rsInstruction, "variable_id");
                        insertOrNull(insertStmt, 25, rsInstruction, "parent_id");

                        insertStmt.setInt(26, newHomeBankId);

                        insertStmt.addBatch();
                        count++;
                        newCompInstr++;
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
            ARLogger.getInstance(PerformDataBase.class).severe("Failed to create saved component_instruction");
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
                        "INSERT INTO component_variable (id, type, name, value, local_format, delimiter, instruction_id, home_banking_id) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

                variableMap.clear();

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {

                    int count = 0;
                    int newCompVariable = getMaxId(conn, "component_variable") + 1;

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

                        insertStmt.setInt(1, newCompVariable);
                        insertStmt.setString(2, rsVariable.getString("type"));
                        insertStmt.setString(3, rsVariable.getString("name"));
                        insertStmt.setString(4, rsVariable.getString("value"));
                        insertStmt.setString(5, rsVariable.getString("local_format"));
                        insertStmt.setString(6, rsVariable.getString("delimiter"));

                        if (newInstructionId != null) {
                            insertStmt.setInt(7, newInstructionId);
                        } else {
                            insertStmt.setNull(7, Types.INTEGER);
                        }

                        insertStmt.setInt(8, newHomeBankId);

                        insertStmt.addBatch();
                        count++;
                        newCompVariable++;

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

        try (Connection conn = getConnection();
                Statement postgresStmt = conn.createStatement()) {
            conn.setAutoCommit(false);

            String idsInstruction =
                    instructionMap.values().stream().map(String::valueOf).collect(Collectors.joining(","));

            String idsBlock = blockMap.values().stream().map(String::valueOf).collect(Collectors.joining(","));

            String selectAccessSQL = "SELECT id, name, parent_id, variable_id " + "FROM component_instruction "
                    + "WHERE (parent_id IS NOT NULL OR variable_id IS NOT NULL) "
                    + "AND home_banking_id = "
                    + blockDetailsDTO.getHomeBankingId() + " AND id IN ("
                    + idsInstruction + ") AND block_id IN ("
                    + idsBlock + ") ORDER BY id";

            try (ResultSet rsInstruction = postgresStmt.executeQuery(selectAccessSQL)) {

                String updateSQL = "UPDATE component_instruction SET variable_id = ?, parent_id = ? WHERE id = ? ";

                try (PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {
                    int count = 0;

                    while (rsInstruction.next()) {
                        int id = rsInstruction.getInt("id");
                        String name = rsInstruction.getString("name");

                        // Set variable_id directly from Access
                        int originalVariableId = rsInstruction.getInt("variable_id");
                        // Map old bot_job_id to new
                        Integer newVariableId = variableMap.get(originalVariableId);
                        if (newVariableId == null) {
                            System.out.println("Skipped variable_id column with unknown variable_id: " + newVariableId);
                            updateStmt.setNull(1, Types.INTEGER);
                        } else {
                            updateStmt.setInt(1, newVariableId);
                        }

                        // Handle parent_id based on name
                        if ("GOTO".equalsIgnoreCase(name) || "EXCEL GOTO".equalsIgnoreCase(name)) {
                            int parentBlockId = rsInstruction.getInt("parent_id");
                            Integer newParentBlockId = blockMap.get(parentBlockId);
                            if (newParentBlockId != null) {
                                updateStmt.setInt(2, newParentBlockId);
                            } else {
                                updateStmt.setNull(2, Types.INTEGER);
                            }
                        } else {
                            int parentInstructionId = rsInstruction.getInt("parent_id");
                            Integer newParentInstructionId = instructionMap.get(parentInstructionId);
                            if (newParentInstructionId != null) {
                                updateStmt.setInt(2, newParentInstructionId);
                            } else {
                                updateStmt.setNull(2, Types.INTEGER);
                            }
                        }

                        updateStmt.setInt(3, id); // WHERE clause: name = ?

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
                        "INSERT INTO component_reference (id, reference_type, value, instruction_id, home_banking_id) VALUES (?, ?, ?, ?, ?)";

                referenceMap.clear();

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {

                    int count = 0;
                    int newCompRefer = getMaxId(conn, "component_reference") + 1;

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

                        insertStmt.setInt(1, newCompRefer);
                        insertStmt.setString(2, rsReference.getString("reference_type"));
                        insertStmt.setString(3, rsReference.getString("value"));
                        insertStmt.setInt(4, newInstructionId);

                        if (newHomeBankId != null) {
                            insertStmt.setInt(5, newHomeBankId);
                        } else {
                            insertStmt.setNull(5, Types.INTEGER);
                        }

                        insertStmt.addBatch();
                        count++;
                        newCompRefer++;

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

            try (ResultSet rs = selectStmt.executeQuery()) {

                String insertSQL = "INSERT INTO block "
                        + "(id, block_order_number, name, description, type_id, export_file, active, wait, bot_job_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

                blockMap.clear();

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {

                    int count = 0;
                    int newCompBlock = getMaxId(conn, "block") + 1;

                    while (rs.next()) {
                        int id = rs.getInt("id");
                        int blockOrderNumber = rs.getInt("block_order_number");
                        //                        String name = rs.getString("name");
                        //                        String description = rs.getString("description");
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

                        insertStmt.setInt(1, newCompBlock);
                        insertStmt.setInt(2, blockOrderNumber);
                        insertStmt.setString(3, blockDetailsDTO.getBlockName());
                        insertStmt.setString(4, blockDetailsDTO.getBlockDescription());
                        if (typeId != null) {
                            insertStmt.setInt(5, typeId);
                        } else {
                            insertStmt.setNull(5, Types.INTEGER);
                        }
                        insertStmt.setString(6, exportFile);
                        insertStmt.setInt(7, active);
                        if (wait != null) {
                            insertStmt.setInt(8, wait);
                        } else {
                            insertStmt.setNull(8, Types.INTEGER);
                        }
                        insertStmt.setInt(9, newBotJobId);

                        insertStmt.addBatch();
                        count++;
                        newCompBlock++;

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
                        + "id, instruction_order_number, actions, name, xpath, coordinates, force_coordinates, iframe_xpath, tag_name, shadow_host, shadow_root, css_selector, description, operation, optional, block_marked, default_value, action_custom_max_wait_sec, on_hold_seconds, codified, export_to_abr, active, block_id, variable_id, parent_id, bot_job_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                instructionMap.clear();

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {

                    int count = 0;
                    int newCompInstr = getMaxId(conn, "instruction") + 1;

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

                        insertStmt.setInt(1, newCompInstr);
                        insertStmt.setInt(2, rsInstruction.getInt("instruction_order_number"));
                        insertStmt.setString(3, rsInstruction.getString("actions"));
                        insertStmt.setString(4, rsInstruction.getString("name"));
                        insertStmt.setString(5, rsInstruction.getString("xpath"));
                        insertStmt.setString(6, rsInstruction.getString("coordinates"));

                        insertOrNull(insertStmt, 7, rsInstruction, "force_coordinates");
                        insertStmt.setString(8, rsInstruction.getString("iframe_xpath"));
                        insertStmt.setString(9, rsInstruction.getString("tag_name"));
                        insertStmt.setString(10, rsInstruction.getString("shadow_host"));
                        insertStmt.setString(11, rsInstruction.getString("shadow_root"));
                        insertStmt.setString(12, rsInstruction.getString("css_selector"));
                        insertStmt.setString(13, rsInstruction.getString("description"));
                        insertStmt.setString(14, rsInstruction.getString("operation"));

                        insertOrNull(insertStmt, 15, rsInstruction, "optional");
                        insertOrNull(insertStmt, 16, rsInstruction, "block_marked");
                        insertStmt.setString(17, rsInstruction.getString("default_value"));
                        insertOrNull(insertStmt, 18, rsInstruction, "action_custom_max_wait_sec");
                        insertOrNull(insertStmt, 19, rsInstruction, "on_hold_seconds");
                        insertOrNull(insertStmt, 20, rsInstruction, "codified");
                        insertOrNull(insertStmt, 21, rsInstruction, "export_to_abr");

                        insertStmt.setInt(22, rsInstruction.getInt("active"));
                        insertStmt.setInt(23, newBlockId);

                        insertOrNull(insertStmt, 24, rsInstruction, "variable_id");
                        insertOrNull(insertStmt, 25, rsInstruction, "parent_id");

                        insertStmt.setInt(26, newBotJobId);

                        insertStmt.addBatch();
                        count++;
                        newCompInstr++;
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
                        "INSERT INTO variable (id, type, name, value, local_format, delimiter, instruction_id, bot_job_id) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

                variableMap.clear();

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {

                    int count = 0;
                    int newCompVariable = getMaxId(conn, "variable") + 1;

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

                        insertStmt.setInt(1, newCompVariable);
                        insertStmt.setString(2, rsVariable.getString("type"));
                        insertStmt.setString(3, rsVariable.getString("name"));
                        insertStmt.setString(4, rsVariable.getString("value"));
                        insertStmt.setString(5, rsVariable.getString("local_format"));
                        insertStmt.setString(6, rsVariable.getString("delimiter"));

                        if (newInstructionId != null) {
                            insertStmt.setInt(7, newInstructionId);
                        } else {
                            insertStmt.setNull(7, Types.INTEGER);
                        }

                        insertStmt.setInt(8, newBotJobId);

                        insertStmt.addBatch();
                        count++;
                        newCompVariable++;

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

        try (Connection conn = getConnection();
                Statement postgresStmt = conn.createStatement()) {
            conn.setAutoCommit(false);

            String idsInstruction =
                    instructionMap.values().stream().map(String::valueOf).collect(Collectors.joining(","));

            String idsBlock = blockMap.values().stream().map(String::valueOf).collect(Collectors.joining(","));

            String selectAccessSQL = "SELECT id, name, parent_id, variable_id " + "FROM instruction "
                    + "WHERE (parent_id IS NOT NULL OR variable_id IS NOT NULL) "
                    + "AND bot_job_id = "
                    + blockDetailsDTO.getBotJobId() + " AND id IN ("
                    + idsInstruction + ") AND block_id IN ("
                    + idsBlock + ") ORDER BY id";

            try (ResultSet rsInstruction = postgresStmt.executeQuery(selectAccessSQL)) {

                String updateSQL = "UPDATE instruction SET variable_id = ?, parent_id = ? WHERE id = ? ";

                try (PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {
                    int count = 0;

                    while (rsInstruction.next()) {
                        int id = rsInstruction.getInt("id");
                        String name = rsInstruction.getString("name");

                        // Set variable_id directly from Access
                        int originalVariableId = rsInstruction.getInt("variable_id");
                        // Map old bot_job_id to new
                        Integer newVariableId = variableMap.get(originalVariableId);
                        if (newVariableId == null) {
                            System.out.println("Skipped variable_id column with unknown variable_id: " + newVariableId);
                            updateStmt.setNull(1, Types.INTEGER);
                        } else {
                            updateStmt.setInt(1, newVariableId);
                        }

                        // Handle parent_id based on name
                        if ("GOTO".equalsIgnoreCase(name) || "EXCEL GOTO".equalsIgnoreCase(name)) {
                            int parentBlockId = rsInstruction.getInt("parent_id");
                            Integer newParentBlockId = blockMap.get(parentBlockId);
                            if (newParentBlockId != null) {
                                updateStmt.setInt(2, newParentBlockId);
                            } else {
                                updateStmt.setNull(2, Types.INTEGER);
                            }
                        } else {
                            int parentInstructionId = rsInstruction.getInt("parent_id");
                            Integer newParentInstructionId = instructionMap.get(parentInstructionId);
                            if (newParentInstructionId != null) {
                                updateStmt.setInt(2, newParentInstructionId);
                            } else {
                                updateStmt.setNull(2, Types.INTEGER);
                            }
                        }

                        updateStmt.setInt(3, id); // WHERE clause: name = ?

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
                        "INSERT INTO reference (id, reference_type, value, instruction_id, bot_job_id) VALUES (?, ?, ?, ?, ?)";

                referenceMap.clear();

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {

                    int count = 0;
                    int newCompRefer = getMaxId(conn, "reference") + 1;

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

                        insertStmt.setInt(1, newCompRefer);
                        insertStmt.setString(2, rsReference.getString("reference_type"));
                        insertStmt.setString(3, rsReference.getString("value"));
                        insertStmt.setInt(4, newInstructionId);

                        if (newBotJobId != null) {
                            insertStmt.setInt(5, newBotJobId);
                        } else {
                            insertStmt.setNull(5, Types.INTEGER);
                        }

                        insertStmt.addBatch();
                        count++;
                        newCompRefer++;

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
            HomeUrlDTO homeUrlDTO, int previousBotJob, String newBotJobName, String newDescription)
            throws SQLException {

        int newBotJobId = getMaxId(conn, "bot_job") + 1;

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
                    "INSERT INTO bot_job (id, name, description, priority, home_banking_id, home_url_id, active) "
                            + "SELECT ?, ?, ?, priority, home_banking_id, ?, ? FROM bot_job WHERE id = ?";

            botJobMap.clear();
            botJobMap.put(previousBotJob, -1);

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {

                insertStmt.setInt(1, newBotJobId);
                insertStmt.setString(2, newBotJobName);
                insertStmt.setString(3, newDescription);
                insertStmt.setInt(4, homeUrlDTO.getId());
                insertStmt.setInt(5, 1);
                insertStmt.setInt(6, previousBotJob);

                insertStmt.addBatch();
                insertStmt.executeBatch();
                conn.commit();
                ARLogger.getInstance(PerformDataBase.class).info("Inserted bot job records: 1");
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
                        + "(id, block_order_number, name, description, type_id, export_file, active, wait, bot_job_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

                blockMap.clear();

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {

                    int count = 0;

                    int newBlockId = getMaxId(conn, "block") + 1;

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

                        insertStmt.setInt(1, newBlockId);
                        insertStmt.setInt(2, blockOrderNumber);
                        insertStmt.setString(3, name);
                        insertStmt.setString(4, description);
                        if (typeId != null) {
                            insertStmt.setInt(5, typeId);
                        } else {
                            insertStmt.setNull(5, Types.INTEGER);
                        }
                        insertStmt.setString(6, exportFile);
                        insertStmt.setInt(7, active);
                        if (wait != null) {
                            insertStmt.setInt(8, wait);
                        } else {
                            insertStmt.setNull(8, Types.INTEGER);
                        }
                        insertStmt.setInt(9, newBotJobId);

                        insertStmt.addBatch();
                        newBlockId++;
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
                        + "id, instruction_order_number, actions, name, xpath, coordinates, force_coordinates, iframe_xpath, tag_name, shadow_host, shadow_root, css_selector, description, operation, optional, block_marked, default_value, action_custom_max_wait_sec, on_hold_seconds, codified, export_to_abr, active, block_id, variable_id, parent_id, bot_job_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                instructionMap.clear();

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {

                    int count = 0;

                    int newInstructionId = getMaxId(conn, "instruction") + 1;

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

                        insertStmt.setInt(1, newInstructionId);
                        insertStmt.setInt(2, rsInstruction.getInt("instruction_order_number"));
                        insertStmt.setString(3, rsInstruction.getString("actions"));
                        insertStmt.setString(4, rsInstruction.getString("name"));
                        insertStmt.setString(5, rsInstruction.getString("xpath"));
                        insertStmt.setString(6, rsInstruction.getString("coordinates"));

                        insertOrNull(insertStmt, 7, rsInstruction, "force_coordinates");
                        insertStmt.setString(8, rsInstruction.getString("iframe_xpath"));
                        insertStmt.setString(9, rsInstruction.getString("tag_name"));
                        insertStmt.setString(10, rsInstruction.getString("shadow_host"));
                        insertStmt.setString(11, rsInstruction.getString("shadow_root"));
                        insertStmt.setString(12, rsInstruction.getString("css_selector"));
                        insertStmt.setString(13, rsInstruction.getString("description"));
                        insertStmt.setString(14, rsInstruction.getString("operation"));

                        insertOrNull(insertStmt, 15, rsInstruction, "optional");
                        insertOrNull(insertStmt, 16, rsInstruction, "block_marked");
                        insertStmt.setString(17, rsInstruction.getString("default_value"));
                        insertOrNull(insertStmt, 18, rsInstruction, "action_custom_max_wait_sec");
                        insertOrNull(insertStmt, 19, rsInstruction, "on_hold_seconds");
                        insertOrNull(insertStmt, 20, rsInstruction, "codified");
                        insertOrNull(insertStmt, 21, rsInstruction, "export_to_abr");

                        insertStmt.setInt(22, rsInstruction.getInt("active"));
                        insertStmt.setInt(23, newBlockId);

                        insertOrNull(insertStmt, 24, rsInstruction, "variable_id");
                        insertOrNull(insertStmt, 25, rsInstruction, "parent_id");

                        insertStmt.setInt(26, newBotJobId);

                        insertStmt.addBatch();
                        count++;
                        newInstructionId++;

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
                        "INSERT INTO variable (id, type, name, value, local_format, delimiter, instruction_id, bot_job_id) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

                variableMap.clear();

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {

                    int count = 0;
                    int newVariableId = getMaxId(conn, "variable") + 1;

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

                        insertStmt.setInt(1, newVariableId);
                        insertStmt.setString(2, rsVariable.getString("type"));
                        insertStmt.setString(3, rsVariable.getString("name"));
                        insertStmt.setString(4, rsVariable.getString("value"));
                        insertStmt.setString(5, rsVariable.getString("local_format"));
                        insertStmt.setString(6, rsVariable.getString("delimiter"));

                        if (newInstructionId != null) {
                            insertStmt.setInt(7, newInstructionId);
                        } else {
                            insertStmt.setNull(7, Types.INTEGER);
                        }

                        insertStmt.setInt(8, newBotJobId);

                        insertStmt.addBatch();
                        count++;
                        newVariableId++;

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

        try (Connection conn = getConnection();
                Statement postgresStmt = conn.createStatement()) {
            conn.setAutoCommit(false);

            String idsInstruction =
                    instructionMap.values().stream().map(String::valueOf).collect(Collectors.joining(","));

            String idsBlock = blockMap.values().stream().map(String::valueOf).collect(Collectors.joining(","));

            int newBotJobId = botJobMap.get(previosBotJob);

            String selectAccessSQL = "SELECT id, name, parent_id, variable_id " + "FROM instruction "
                    + "WHERE (parent_id IS NOT NULL OR variable_id IS NOT NULL) "
                    + "AND bot_job_id = "
                    + newBotJobId + " AND id IN ("
                    + idsInstruction + ") AND block_id IN ("
                    + idsBlock + ") ORDER BY id";

            try (ResultSet rsInstruction = postgresStmt.executeQuery(selectAccessSQL)) {

                String updateSQL = "UPDATE instruction SET variable_id = ?, parent_id = ? WHERE id = ? ";

                try (PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {
                    int count = 0;

                    while (rsInstruction.next()) {
                        int id = rsInstruction.getInt("id");
                        String name = rsInstruction.getString("name");

                        // Set variable_id directly from Access
                        int originalVariableId = rsInstruction.getInt("variable_id");
                        // Map old bot_job_id to new
                        Integer newVariableId = variableMap.get(originalVariableId);
                        if (newVariableId == null) {
                            System.out.println("Skipped variable_id column with unknown variable_id: " + newVariableId);
                            updateStmt.setNull(1, Types.INTEGER);
                        } else {
                            updateStmt.setInt(1, newVariableId);
                        }

                        // Handle parent_id based on name
                        if ("GOTO".equalsIgnoreCase(name) || "EXCEL GOTO".equalsIgnoreCase(name)) {
                            int parentBlockId = rsInstruction.getInt("parent_id");
                            Integer newParentBlockId = blockMap.get(parentBlockId);
                            if (newParentBlockId != null) {
                                updateStmt.setInt(2, newParentBlockId);
                            } else {
                                updateStmt.setNull(2, Types.INTEGER);
                            }
                        } else {
                            int parentInstructionId = rsInstruction.getInt("parent_id");
                            Integer newParentInstructionId = instructionMap.get(parentInstructionId);
                            if (newParentInstructionId != null) {
                                updateStmt.setInt(2, newParentInstructionId);
                            } else {
                                updateStmt.setNull(2, Types.INTEGER);
                            }
                        }

                        updateStmt.setInt(3, id); // WHERE clause: name = ?

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
                        "INSERT INTO reference (id, reference_type, value, instruction_id, bot_job_id) VALUES (?, ?, ?, ?, ?)";

                referenceMap.clear();

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {

                    int count = 0;
                    int newReferId = getMaxId(conn, "reference") + 1;

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

                        insertStmt.setInt(1, newReferId);
                        insertStmt.setString(2, rsReference.getString("reference_type"));
                        insertStmt.setString(3, rsReference.getString("value"));
                        insertStmt.setInt(4, newInstructionId);

                        if (newBotJobId != null) {
                            insertStmt.setInt(5, newBotJobId);
                        } else {
                            insertStmt.setNull(5, Types.INTEGER);
                        }

                        insertStmt.addBatch();
                        count++;
                        newReferId++;

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

    public void initializeMainDatabaseAccess(File dbFile) {

        try (Connection conn = getConnection()) {
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
                String createURLTableSQL = "CREATE TABLE home_url ("
                        + "ID INTEGER PRIMARY KEY, "
                        + "url MEMO, "
                        + "home_banking_id INTEGER);";
                stmt.executeUpdate(createURLTableSQL);

                String addURLForeignKeySQL = "ALTER TABLE home_url "
                        + "ADD CONSTRAINT FK_URL FOREIGN KEY (home_banking_id) "
                        + "REFERENCES home_banking(id) ";
                stmt.executeUpdate(addURLForeignKeySQL);

                // Create bot_job table with a foreign key reference to home_banking
                String createBotJobTableSQL = "CREATE TABLE bot_job ("
                        + "id INTEGER PRIMARY KEY, "
                        + "name TEXT UNIQUE, "
                        + "description TEXT, "
                        + "priority MEMO, "
                        + "active YESNO NOT NULL, "
                        + "home_banking_id INTEGER, "
                        + "home_url_id INTEGER);";
                stmt.executeUpdate(createBotJobTableSQL);

                String addBotJobForeignKeySQL = "ALTER TABLE bot_job "
                        + "ADD CONSTRAINT FK_BotJob FOREIGN KEY (home_banking_id) "
                        + "REFERENCES home_banking(id) ";
                stmt.executeUpdate(addBotJobForeignKeySQL);

                String addHomrURLForeignKeySQL = "ALTER TABLE bot_job "
                        + "ADD CONSTRAINT FK_HomeUrl FOREIGN KEY (home_url_id) "
                        + "REFERENCES home_url(id) ";
                stmt.executeUpdate(addHomrURLForeignKeySQL);

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
                        + "REFERENCES bot_job(id) ";
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
                        + "tag_name TEXT, "
                        + "shadow_host MEMO, "
                        + "shadow_root MEMO, "
                        + "css_selector MEMO, "
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
                        + "REFERENCES block(id) ";
                stmt.executeUpdate(addForeignKeySQL3);

                String addForeignKeySQL4 = "ALTER TABLE instruction "
                        + "ADD CONSTRAINT FK_4 FOREIGN KEY (bot_job_id) "
                        + "REFERENCES bot_job(id) ";
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
                        + "REFERENCES instruction(id) ";
                stmt.executeUpdate(addForeignKeySQL5);

                String addForeignKeySQL6 = "ALTER TABLE reference "
                        + "ADD CONSTRAINT FK_6 FOREIGN KEY (bot_job_id) "
                        + "REFERENCES bot_job(id) ";
                stmt.executeUpdate(addForeignKeySQL6);

                String createVariableTableSQL = "CREATE TABLE variable ("
                        + "id INTEGER PRIMARY KEY, "
                        + "type TEXT, "
                        + "name TEXT, "
                        + "value MEMO, "
                        + "instruction_id INTEGER, "
                        + "bot_job_id INTEGER,"
                        + "local_format TEXT,"
                        + "delimiter TEXT);";
                stmt.executeUpdate(createVariableTableSQL);

                String addForeignKeySQL7 = "ALTER TABLE variable "
                        + "ADD CONSTRAINT FK_7 FOREIGN KEY (instruction_id) "
                        + "REFERENCES instruction(id) ";
                stmt.executeUpdate(addForeignKeySQL7);

                String addForeignKeySQL8 = "ALTER TABLE variable "
                        + "ADD CONSTRAINT FK_8 FOREIGN KEY (bot_job_id) "
                        + "REFERENCES bot_job(id) ";
                stmt.executeUpdate(addForeignKeySQL8);

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
                        + "REFERENCES home_banking(id) ";
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
                        + "tag_name TEXT, "
                        + "shadow_host MEMO, "
                        + "shadow_root MEMO, "
                        + "css_selector MEMO, "
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
                        + "REFERENCES component_block(id) ";
                stmt.executeUpdate(addForeignKeySQL10);

                String addCompBlkHomeForeignKeySQL = "ALTER TABLE component_instruction "
                        + "ADD CONSTRAINT FK_BLKHomeBank FOREIGN KEY (home_banking_id) "
                        + "REFERENCES home_banking(id) ";
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
                        + "REFERENCES component_instruction(id) ";
                stmt.executeUpdate(addForeignKeySQL11);

                String addCompReferForeignKeySQL = "ALTER TABLE component_reference "
                        + "ADD CONSTRAINT FK_CompRefer FOREIGN KEY (home_banking_id) "
                        + "REFERENCES home_banking(id) ";
                stmt.executeUpdate(addCompReferForeignKeySQL);

                String createComponentVariableTableSQL = "CREATE TABLE component_variable ("
                        + "id INTEGER PRIMARY KEY, "
                        + "type TEXT, "
                        + "name TEXT, "
                        + "value MEMO, "
                        + "instruction_id INTEGER, "
                        + "home_banking_id INTEGER,"
                        + "local_format TEXT,"
                        + "delimiter TEXT);";
                stmt.executeUpdate(createComponentVariableTableSQL);

                String addForeignKeySQL12 = "ALTER TABLE component_variable "
                        + "ADD CONSTRAINT FK_12 FOREIGN KEY (instruction_id) "
                        + "REFERENCES component_instruction(id) ";
                stmt.executeUpdate(addForeignKeySQL12);

                String addCompVarForeignKeySQL = "ALTER TABLE component_variable "
                        + "ADD CONSTRAINT FK_CompVar FOREIGN KEY (home_banking_id) "
                        + "REFERENCES home_banking(id) ";
                stmt.executeUpdate(addCompVarForeignKeySQL);
            }
            System.out.println(String.format("Database %s has been created!", dbFile.getName()));
        } catch (SQLException error) {
            System.out.println("initializeDatabase\nError: " + error.getMessage());
        }
    }

    public boolean doesNotInstructionTableExist() throws SQLException {
        try (Connection conn = getConnection()) {
            if (conn != null && conn.getMetaData() != null) {
                setConn(conn);
                try (ResultSet rs = conn.getMetaData().getTables(null, null, "instruction", null)) {
                    return !rs.next(); // Returns true if the table exists
                }
            }
        } catch (SQLException error) {
            System.out.println("Error checking table existence: " + error.getMessage());
            throw error;
        }

        return true; // Default return if an exception occurs or the table does not exist
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

    public void initializeMainDatabasePostgres() {
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
                String createURLTableSQL = "CREATE TABLE home_url ("
                        + "ID SERIAL PRIMARY KEY, "
                        + "url TEXT, "
                        + "home_banking_id INTEGER REFERENCES home_banking(id) ON DELETE CASCADE)";
                stmt.executeUpdate(createURLTableSQL);

                // Create bot_job table with a foreign key reference to home_banking
                String createBotJobTableSQL = "CREATE TABLE bot_job ("
                        + "id SERIAL PRIMARY KEY, "
                        + "name TEXT UNIQUE, "
                        + "description TEXT, "
                        + "priority TEXT, "
                        + "active INTEGER NOT NULL, "
                        + "home_banking_id INTEGER REFERENCES home_banking(id) ON DELETE CASCADE, "
                        + "home_url_id INTEGER REFERENCES home_url(id) ON DELETE CASCADE)";
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
                        + "tag_name TEXT, "
                        + "shadow_host TEXT, "
                        + "shadow_root TEXT, "
                        + "css_selector TEXT, "
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
                        + "local_format TEXT,"
                        + "delimiter TEXT,"
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
                        + "tag_name TEXT, "
                        + "shadow_host TEXT, "
                        + "shadow_root TEXT, "
                        + "css_selector TEXT, "
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
                        + "local_format TEXT,"
                        + "delimiter TEXT,"
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

    public ObservableList<VariableUserDTO> loadAllVariablesByCriteria(int botJobId, int parentId) {
        variablesList.clear();
        String selectSQL =
                "SELECT vars.id, vars.type, vars.name, vars.value, vars.local_format, vars.delimiter, COUNT(blk.variable_id) UsedVars "
                        + "FROM variable vars "
                        + "LEFT JOIN instruction blk ON blk.variable_id = vars.id "
                        + "WHERE vars.bot_job_id = " + botJobId;

        if (parentId != -1) { // Check if instructionId is provided (not -1)
            selectSQL += " AND instruction_id = " + parentId;
        }

        selectSQL += " GROUP BY vars.id, vars.type, vars.Name, vars.value, vars.local_format, vars.delimiter";

        selectSQL += " ORDER BY vars.id";

        try (Statement stmt = getConnection().createStatement(); // Assuming you have getConnection() method
                ResultSet rs = stmt.executeQuery(selectSQL)) {

            while (rs.next()) {
                int id = rs.getInt("ID");
                String type = rs.getString("type");
                String name = rs.getString("name");
                String value = rs.getString("value");
                String localFormat = rs.getString("local_format");
                String delimiter = rs.getString("delimiter");
                String usedVars = rs.getString("UsedVars");
                variablesList.add(new VariableUserDTO(
                        id, type, name, value, botJobId, parentId, localFormat, delimiter, usedVars));
            }
            return variablesList;
        } catch (SQLException e) {
            // Handle the exception properly (log, throw, etc.)
            ARLogger.getInstance(PerformDataBase.class).severe("loadAllVariblesByCriteria. Error: " + e.getMessage());
        }
        return null;
    }

    public List<VariableLoadDTO> loadAllVariables(int botJobId) {
        List<VariableLoadDTO> variablesLoadList = new ArrayList<>();
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
                variablesLoadList.add(new VariableLoadDTO(
                        id, -1, botJobId, instructionId, type, name, value, localFormat, delimiter, usedVars));
            }
            return variablesLoadList;
        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class).severe("loadAllVariables. Error: " + error.getMessage());
        }
        return null;
    }

    public List<InstructionLoadDTO> loadExcelGotoBlock(int homeBankingId, int botJobId) {

        String query = "SELECT * FROM instruction " + " WHERE 1=1 " + " and actions = " + "'EXCEL GOTO'";

        // Add conditional filters based on provided IDs
        if (botJobId > 0) { // Only apply filter if botJobId is a valid, positive ID
            query += " AND bot_job_id = " + botJobId;
        }

        //        if (homeBankingId > 0) { // Only apply filter if homeBankingId is a valid, positive ID
        //            query += " AND home_banking_id = " + homeBankingId;
        //        }

        query += " ORDER BY instruction_order_number"; // Ordering by instruction order, as block_order_number is not
        // available

        List<InstructionLoadDTO> instructionLoadDTOList = new ArrayList<>();

        // Using Statement to execute the query
        try (Statement stmt = getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery(query); // Execute the query

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
                instructionLoadDTO.setParentId(rs.getInt("parent_id"));
                instructionLoadDTO.setXpath(rs.getString("xpath"));
                instructionLoadDTO.setCoordinates(rs.getString("coordinates"));
                instructionLoadDTO.setForceCoordinates(rs.getBoolean("force_coordinates"));
                instructionLoadDTO.setIFrameXPath(rs.getString("iframe_xpath"));

                instructionLoadDTO.setTagName(rs.getString("tag_name"));
                instructionLoadDTO.setShadowHost(rs.getString("shadow_host"));
                instructionLoadDTO.setShadowRoot(rs.getString("shadow_root"));
                instructionLoadDTO.setCssSelector(rs.getString("css_selector"));

                instructionLoadDTO.setVariableId(rs.getInt("variable_id"));
                instructionLoadDTO.setBlockId(rs.getInt("block_id"));
                // Removed: setBlockOrderNumber as 'block_order_number' is not available without joining the 'block'
                // table.

                // Assuming both bot_job_id and home_banking_id exist in the 'instruction' table
                instructionLoadDTO.setBotJobId(rs.getInt("bot_job_id"));

                instructionLoadDTOList.add(instructionLoadDTO);
            }

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class)
                    .severe(String.format(
                            "Error getting All Excel data GOTO Homebanking ID %d BotJob ID %d. Error: %s",
                            homeBankingId, botJobId, error.getMessage()));
        }

        return instructionLoadDTOList;
    }

    private Integer loadNextIdData() {
        //        String selectSQL = "SELECT NEXT_VAL fROM homeBankingSeq";
        String selectSQL = "SELECT MAX(ID) AS max_id FROM variable";
        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(selectSQL)) {
            while (rs.next()) {
                return rs.getInt("max_id");
            }
        } catch (SQLException error) {
            System.out.println(error.getMessage());
        }
        return null;
    }

    public void saveUserData(VariableUserDTO user) {
        // Generate a Unique-ID
        Integer hashCode = loadNextIdData() + 1;
        //        AlterSeq(hashCode);
        //        Integer hashCode = generateID();

        String insertSQL =
                "INSERT INTO variable (ID, type, Name, Value, bot_job_id, instruction_id, local_format, delimiter) VALUES ( "
                        + hashCode + ","
                        + "'" + user.getType() + "', "
                        + "'" + user.getName() + "', "
                        + "'" + user.getValue() + "', "
                        + "'" + user.getBotJobId() + "', "
                        + "'" + user.getParentId() + "', "
                        + "'" + user.getLocalFormat() + "', "
                        + "'" + user.getDelimiter() + "')";
        try (Statement stmt = getConnection().createStatement()) {
            stmt.executeUpdate(insertSQL);
            System.out.println("Data saved successfully.");
        } catch (SQLException error) {
            System.out.println(error.getMessage());
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

    public void migrationAccessToAccess() {
        String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB); // origin
        String dbPathDest = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB); // destination

        String accessOrigin = CONNECTION_TYPE + dbPath + "database-FABRIZIO-2.mdb" + CONNECTION_PARAMETERS;
        ARLogger.getInstance(PerformDataBase.class).info("ACCESS Origin URL: " + accessOrigin);

        String accessDest = CONNECTION_TYPE + dbPathDest + ARConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;
        ARLogger.getInstance(PerformDataBase.class).info("ACCESS Destination URL: " + accessDest);

        final int BATCH_SIZE = 100;

        try (Connection access1 = DriverManager.getConnection(accessOrigin);
                Connection access2 = DriverManager.getConnection(accessDest);
                Statement accessStmt = access1.createStatement()) {

            access2.setAutoCommit(false);

            String selectAccessSQL =
                    "SELECT url, name, priority, search_config, options_config, cookies, driver_session, username, password FROM home_banking";
            ResultSet rs = accessStmt.executeQuery(selectAccessSQL);

            String checkSQL = "SELECT id FROM home_banking WHERE url = ?";
            String insertSQL =
                    "INSERT INTO home_banking (url, name, priority, search_config, options_config, cookies, driver_session, username, password) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement checkStmt = access2.prepareStatement(checkSQL);
                    PreparedStatement insertStmt = access2.prepareStatement(insertSQL)) {

                int count = 0;

                while (rs.next()) {
                    String url = rs.getString("url");

                    checkStmt.setString(1, url);
                    try (ResultSet checkResult = checkStmt.executeQuery()) {
                        if (!checkResult.next()) {
                            insertStmt.setString(1, url);
                            insertStmt.setString(2, rs.getString("name"));
                            insertStmt.setString(3, rs.getString("priority"));
                            insertStmt.setString(4, rs.getString("search_config"));
                            insertStmt.setString(5, rs.getString("options_config"));
                            insertStmt.setString(6, rs.getString("cookies"));
                            insertStmt.setString(7, rs.getString("driver_session"));
                            insertStmt.setString(8, rs.getString("username"));
                            insertStmt.setString(9, rs.getString("password"));
                            insertStmt.addBatch();

                            count++;

                            if (count % BATCH_SIZE == 0) {
                                insertStmt.executeBatch();
                                access2.commit();
                                ARLogger.getInstance(getClass()).info("Inserted batch of " + BATCH_SIZE);
                            }
                        } else {
                            ARLogger.getInstance(getClass()).info("Skipped existing: " + url);
                        }
                    }
                }

                // Final batch
                if (count % BATCH_SIZE != 0) {
                    insertStmt.executeBatch();
                    access2.commit();
                    System.out.println("Inserted final batch of " + (count % BATCH_SIZE));
                }
            }

            ARLogger.getInstance(getClass()).info("Migration completed.");

        } catch (SQLException error) {
            ARLogger.getInstance(PerformDataBase.class).severe("Error during migration: " + error);
        }
    }
}
