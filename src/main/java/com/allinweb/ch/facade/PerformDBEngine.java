package com.allinweb.ch.facade;

import com.allinweb.ch.model.*;
import com.allinweb.ch.util.ARConstantsEngine;
import com.allinweb.ch.util.ARPropertyEnum;
import com.allinweb.ch.util.ARPropertyManager;
import com.allinweb.ch.util.ErrorMessage;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConfig;

@Slf4j
public class PerformDBEngine {

    private static final Logger logDB = LoggerFactory.getLogger("com.allinweb.database");

    public static final ARPropertyManager arPropertyManager = ARPropertyManager.getInstance();
    public static final PerformMessage performMessage = PerformMessage.getInstance();
    public static final PerformLists performLists = PerformLists.getInstance();
    // Static final variable to hold the singleton instance
    protected static volatile PerformDBEngine instance;
    public final String CONNECTION_TYPE = "jdbc:ucanaccess://";
    public final String CONNECTION_PARAMETERS = ";memory=false;newDatabaseVersion=V2010";
    public final String CONNECTION_TYPE_SQLITE = "jdbc:sqlite:"; // no parameters needed
    // Postgres
    public boolean ACCESS_DB = false;
    public boolean POSTGRES_DB = false;
    public boolean SQLITE_DB = false;
    public boolean connDBWorks = false;
    public boolean dbFailed = false;
    // Private constructor to prevent instantiation
    private PerformDBEngine() {}

    // Public method to access the singleton instance
    public static PerformDBEngine getInstance() {
        if (instance == null) {
            synchronized (PerformDBEngine.class) {
                if (instance == null) {
                    instance = new PerformDBEngine();
                }
            }
        }
        return instance;
    }

    public void initialize(String databaseType) {}

    public Connection getConnection() throws SQLException {
        // Determine DB type from properties
        String dataBaseType = arPropertyManager.getProperty(ARPropertyEnum.DATABASE_TYPE);

        // Booleans to track DB type
        POSTGRES_DB = false;
        SQLITE_DB = false;
        ACCESS_DB = false;

        if (dataBaseType != null) {
            if ("Postgres".equalsIgnoreCase(dataBaseType)) {
                // Postgres-specific logic
                POSTGRES_DB = true;
            } else if ("TEXT".equalsIgnoreCase(dataBaseType)) {
                // SQLite-specific logic
                SQLITE_DB = true;
            } else if ("Access".equalsIgnoreCase(dataBaseType)) {
                // Access-specific logic
                ACCESS_DB = true;
            }
        }

        try {
            if (POSTGRES_DB) {
                // Postgres connection
                String dbUrl = arPropertyManager.getProperty(ARPropertyEnum.DB_URL);
                String userDB = arPropertyManager.getProperty(ARPropertyEnum.DB_USER);
                String userPwd = arPropertyManager.getProperty(ARPropertyEnum.DB_PWD);

                logDB.info("POSTGRES connection URL: " + dbUrl);
                // logDB.info("User Details: " + userDB + " - [PROTECTED]");

                Class.forName("org.postgresql.Driver");
                Connection conn = DriverManager.getConnection(dbUrl, userDB, userPwd);
                conn.setReadOnly(false);
                connDBWorks = true;
                return conn;

                //                // Reset open connections counter if too many
                //                if (getOpenConnectionsCount() > 10) {
                //                    this.openConnections = 0;
                //                }
                //                incrementOpenConnections();

            } else if (SQLITE_DB) {
                // SQLite connection
                String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
                String sqliteUrl = CONNECTION_TYPE_SQLITE
                        + dbPath
                        + ARConstantsEngine.FILE_NAME_SQLITE; // make sure you have FILE_NAME_SQLITE constant

                logDB.info("SQLITE connection URL: " + sqliteUrl);

                Class.forName("org.sqlite.JDBC");

                SQLiteConfig config = new SQLiteConfig();
                config.enforceForeignKeys(true);

                Connection conn = DriverManager.getConnection(sqliteUrl, config.toProperties());
                //                    conn = SQLiteHelper.getConnection(sqliteUrl);
                conn.setReadOnly(false);
                connDBWorks = true;
                return conn;

                //                // Reset open connections counter if too many
                //                if (getOpenConnectionsCount() > 10) {
                //                    this.openConnections = 0;
                //                }
                //                incrementOpenConnections();

            } else {
                // Default to Access connection
                String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
                String dbUrl = CONNECTION_TYPE + dbPath + ARConstantsEngine.FILE_NAME_ACCESS + CONNECTION_PARAMETERS;

                logDB.info("ACCESS connection URL: " + dbUrl);

                Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
                Connection conn = DriverManager.getConnection(dbUrl);
                conn.setReadOnly(false);
                connDBWorks = true;
                return conn;

                //                // Reset open connections counter if too many
                //                if (getOpenConnectionsCount() > 10) {
                //                    this.openConnections = 0;
                //                }
                //                incrementOpenConnections();
            }

        } catch (SQLException error) {
            logDB.error("getConnection Error: " + error.getMessage());

            String database = POSTGRES_DB ? "Postgres" : (SQLITE_DB ? "TEXT" : "Access");
            connDBWorks = false;
            dbFailed = true;
            throw error;
        } catch (ClassNotFoundException error) {

            logDB.error("Driver DB Class not Found Error: " + error.getMessage());
        }

        connDBWorks = false;
        dbFailed = false;
        return null;
    }

    public void changeDbConnection() throws SQLException {
        String dataBaseType = arPropertyManager.getProperty(ARPropertyEnum.DATABASE_TYPE);
        //        if (Strings.isNullOrEmpty(previousDB) || (previousDB != null && !previousDB.equals(dataBaseType))) {
        ErrorMessage errorMessage = null;

        // closeConnection();

        POSTGRES_DB = false;
        SQLITE_DB = false;
        ACCESS_DB = false;

        if (dataBaseType != null) {
            if ("Postgres".equalsIgnoreCase(dataBaseType)) {
                // Postgres-specific logic
                POSTGRES_DB = true;
            } else if ("TEXT".equalsIgnoreCase(dataBaseType)) {
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
    }

    public ErrorMessage loadHomeBanking(Integer homeBankingId) {
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
                + "GROUP BY hb.id "
                + "ORDER BY hb.id";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(selectSQL);
                ResultSet rs = pstmt.executeQuery()) {

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

            performLists.getListHomeBanking().addAll(homeBankingMap.values());
            return null; // ✅ no error

        } catch (SQLException e) {
            String errorDetail = homeBankingId != null
                    ? String.format(
                            "Error selecting home banking record with ID %d. Error: %s", homeBankingId, e.getMessage())
                    : String.format("Error selecting ALL home banking records. Error: %s", e.getMessage());

            logDB.error(errorDetail);

            return new ErrorMessage("Error Load Home Banking", "Failed to load HomeBanking records", errorDetail);
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

        } catch (SQLException error) {
            logDB.error("Failed to load Home URLs");
            return new ErrorMessage("Failed to load Home URLs", "Home URL Load Failure", error.getMessage());
        }

        return null; // success → no error
    }

    public ErrorMessage loadCompleteJobs(int botJobId) {
        String query = "SELECT bot.home_banking_id, bot.home_url_id, bot.id AS bot_job_id, bot.name AS bot_job_name, "
                + " b.id AS block_id, b.block_order_number, b.name AS block_name, "
                + " b.description AS block_description, bot.priority AS bot_job_priority, b.type_id, "
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

        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            Map<Integer, BotJobLoadDTO> botJobMapDTO = new HashMap<>();
            Map<Integer, BlockLoadDTO> blockMapDTO = new HashMap<>();
            Map<Integer, InstructionLoad> instructionMapDTO = new HashMap<>();

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

                    String priority = rs.getString("bot_job_priority");
                    if (priority == null || priority.trim().isEmpty()) {
                        priority = "Web App";
                    }
                    botJobDTO.setPriority(priority);

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

                    blockDTO.setInstructionLoad(new ArrayList<>());
                    botJobDTO.getBlockLoadDTOList().add(blockDTO);
                    blockMapDTO.put(blockId, blockDTO);
                }

                int instructionId = rs.getInt("instruction_id");
                InstructionLoad instruction = instructionMapDTO.get(instructionId);

                if (instruction == null) {
                    instruction = new InstructionLoad();
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
                    instruction.setBlockId(rs.getInt("block_id"));
                    instruction.setParentBlockId(rs.getInt("parent_block_id"));

                    instruction.setParentBlockId(rs.getInt("parent_block_id"));

                    if (instruction.getName().equals("EXCEL GOTO")
                            && ((instruction.getParentBlockId() != null && instruction.getParentBlockId() == 0)
                                    || instruction.getParentBlockId() == null)) {
                        instruction.setParentBlockId(instruction.getBlockId());
                    }

                    instruction.setParentId(rs.getInt("parent_id"));
                    instruction.setVariableId(rs.getInt("variable_id"));

                    instruction.setInstructionActive(rs.getBoolean("instruction_active"));

                    instruction.setReferenceLoadDTOList(new ArrayList<>());
                    blockDTO.getInstructionLoad().add(instruction);
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
        } catch (SQLException error) {

            logDB.error(
                    String.format("Error loadCompleteJobs for Bot Job Id %d. Error: %s", botJobId, error.getMessage()));
            return new ErrorMessage("Error Loading Complete Job", "Error loading complete Job", error.getMessage());
        }

        return null;
    }

    public ErrorMessage loadAllActionsPerBlock(List<BlockLoadDTO> blockLoadDTOList) {
        performLists.getAllActions().clear();

        List<String> actionsList = new ArrayList<>();

        if (blockLoadDTOList == null || blockLoadDTOList.isEmpty()) {
            return null;
        }

        // Build the placeholders for tuples
        String placeholders = blockLoadDTOList.stream().map(b -> "(?, ?)").collect(Collectors.joining(", "));

        String query = "SELECT actions FROM instruction WHERE (block_id, bot_job_id) IN (" + placeholders + ")";

        try (PreparedStatement pstmt = getConnection().prepareStatement(query)) {
            int index = 1;
            for (BlockLoadDTO blockDTO : blockLoadDTOList) {
                pstmt.setInt(index++, blockDTO.getId());
                pstmt.setInt(index++, blockDTO.getBotJobId());
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    actionsList.add(rs.getString("actions"));
                }
            }

            performLists.setAllActions(actionsList);

        } catch (SQLException error) {

            logDB.error(String.format("Error loading actions for blocks. Error: %s", error.getMessage()));
            return new ErrorMessage("Cannot get All Actions", "Error loading actions for blocks", error.getMessage());
        }
        return null;
    }

    public ErrorMessage loadAllVariables(String varTable, int whereId) {
        performLists.getListVariable().clear();

        String instrTable = varTable.equals("variable") ? "instruction" : "component_instruction";
        String whereColumn = varTable.equals("variable") ? "bot_job_id" : "home_banking_id";

        String selectSQL = "SELECT " + "    vars.id, "
                + "    vars.instruction_id, "
                + "    vars.type, "
                + "    vars.name, "
                + "    vars.value, "
                + "    vars.local_format, "
                + "    vars.delimiter, "
                + "    COUNT(blk.variable_id) AS UsedVars "
                + "FROM " + varTable + " vars "
                + "LEFT JOIN "
                + instrTable + " blk ON blk.variable_id = vars.id " + "WHERE vars."
                + whereColumn + " = ? "
                + "GROUP BY vars.id, vars.instruction_id, vars.type, vars.name, vars.value, vars.local_format, vars.delimiter "
                + "ORDER BY vars.id;";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(selectSQL)) {

            conn.setAutoCommit(false);
            stmt.setInt(1, whereId);

            try (ResultSet rs = stmt.executeQuery()) {
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
                                    id,
                                    -1,
                                    whereId, // bot_job_id or home_banking_id
                                    instructionId,
                                    type,
                                    name,
                                    value,
                                    localFormat,
                                    delimiter,
                                    usedVars));
                }
            }

            conn.commit();
            return null; // success
        } catch (SQLException e) {

            logDB.error(String.format(
                    "Error loading variables for %s=%d. Error: %s", whereColumn, whereId, e.getMessage()));

            return new ErrorMessage(
                    "LoadVariables Error",
                    String.format("Failed to load variables for %s=%d", whereColumn, whereId),
                    e.getMessage());
        }
    }

    public List<InstructionLoad> loadExcelGotoBlock(int whereId, String tableName) {
        List<InstructionLoad> InstructionLoadList = new ArrayList<>();

        // Base SQL
        String sql = "SELECT * FROM " + tableName + " WHERE actions = 'EXCEL GOTO'";

        // Decide FK + block table
        final String fkColumn;
        final String blockTable;

        // Add condition depending on table
        if ("instruction".equalsIgnoreCase(tableName)) {
            fkColumn = "bot_job_id";
            blockTable = "block";
            sql += " AND bot_job_id = ?";
        } else if ("component_instruction".equalsIgnoreCase(tableName)) {
            fkColumn = "home_banking_id";
            blockTable = "component_block";
            sql += " AND home_banking_id = ?";
        } else {
            throw new IllegalArgumentException("Unsupported table: " + tableName);
        }

        sql += " ORDER BY instruction_order_number";

        // Block lookup SQL (for missing parent_block_id)
        final String blockSql =
                "SELECT block_order_number FROM " + blockTable + " WHERE id = ? AND " + fkColumn + " = ?";

        // Cache block_id -> block_order_number (avoid N+1 queries)
        Map<Integer, Integer> blockOrderCache = new HashMap<>();

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                PreparedStatement blockStmt = conn.prepareStatement(blockSql)) {

            stmt.setInt(1, whereId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    InstructionLoad instructionLoad = new InstructionLoad();
                    instructionLoad.setId(rs.getInt("id"));
                    instructionLoad.setActionCustomMaxWaitSec(rs.getInt("action_custom_max_wait_sec"));
                    instructionLoad.setActions(rs.getString("actions"));
                    instructionLoad.setInstructionActive(rs.getBoolean("active"));
                    instructionLoad.setBlockMarked(rs.getBoolean("block_marked"));
                    instructionLoad.setCodified(rs.getBoolean("codified"));
                    instructionLoad.setDefaultValue(rs.getString("default_value"));
                    instructionLoad.setDescription(rs.getString("description"));
                    instructionLoad.setExportToABR(rs.getBoolean("export_to_abr"));
                    instructionLoad.setInstructionOrderNumber(rs.getInt("instruction_order_number"));
                    instructionLoad.setName(rs.getString("name"));
                    instructionLoad.setOnHoldSeconds(rs.getInt("on_hold_seconds"));
                    instructionLoad.setOperation(rs.getString("operation"));
                    instructionLoad.setOptional(rs.getBoolean("optional"));
                    instructionLoad.setXpath(rs.getString("xpath"));
                    instructionLoad.setCoordinates(rs.getString("coordinates"));
                    instructionLoad.setForceCoordinates(rs.getBoolean("force_coordinates"));
                    instructionLoad.setIFrameXPath(rs.getString("iframe_xpath"));

                    instructionLoad.setTagName(rs.getString("tag_name"));
                    instructionLoad.setShadowHost(rs.getString("shadow_host"));
                    instructionLoad.setShadowRoot(rs.getString("shadow_root"));
                    instructionLoad.setCssSelector(rs.getString("css_selector"));

                    instructionLoad.setVariableId(rs.getInt("variable_id"));

                    // parent_block_id can be NULL -> use getObject
                    Integer parentBlockId = (Integer) rs.getObject("parent_block_id");
                    instructionLoad.setParentBlockId(parentBlockId != null ? parentBlockId : 0);

                    instructionLoad.setParentId(rs.getInt("parent_id"));

                    int blockId = rs.getInt("block_id");
                    instructionLoad.setBlockId(blockId);

                    // Conditional mapping for IDs
                    if ("instruction".equalsIgnoreCase(tableName)) {
                        instructionLoad.setBotJobId(rs.getInt("bot_job_id"));
                    } else {
                        instructionLoad.setHomeBankingId(rs.getInt("home_banking_id"));
                    }

                    // If parentBlockId is NULL or 0 -> fetch block_order_number from block table using block_id
                    if (parentBlockId == null || parentBlockId == 0 || instructionLoad.getBlockOrderNumber() == null) {
                        Integer cached = blockOrderCache.get(blockId);

                        if (cached == null) {
                            blockStmt.setInt(1, blockId);
                            blockStmt.setInt(2, whereId);

                            try (ResultSet brs = blockStmt.executeQuery()) {
                                if (brs.next()) {
                                    cached = brs.getInt("block_order_number");
                                    blockOrderCache.put(blockId, cached);
                                }
                            }
                        }

                        if (cached != null) {
                            instructionLoad.setBlockOrderNumber(cached); // overwrite/fix using block table
                        }
                    }

                    InstructionLoadList.add(instructionLoad);
                }
            }
        } catch (SQLException error) {

            logDB.error(String.format(
                    "Error getting All Excel data GOTO from table %s whereId %d. Error: %s",
                    tableName, whereId, error.getMessage()));
        }

        return InstructionLoadList;
    }

    public void callSocketLists(String sessionId) {
        performLists.initialize(sessionId);
    }

    public ErrorMessage fixExcelGoto(String tableName, int whereId, int instructionId, int newParentBlockId) {

        // Scope key (same logic as your updateUserData)
        String foreignKeyColumn = "instruction".equalsIgnoreCase(tableName) ? "bot_job_id" : "home_banking_id";

        String updateSQL = "UPDATE " + tableName + " SET "
                + "parent_block_id = ? "
                + "WHERE id = ? "
                + "AND " + foreignKeyColumn + " = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
                pstmt.setInt(1, newParentBlockId);
                pstmt.setInt(2, instructionId);
                pstmt.setInt(3, whereId);

                int rowsAffected = pstmt.executeUpdate();
                conn.commit();

                if (rowsAffected > 0) {
                    logDB.info(String.format(
                            "Updated parent_block_id=%d in %s where id=%d and %s=%d",
                            newParentBlockId, tableName, instructionId, foreignKeyColumn, whereId));
                    return null; // success
                } else {
                    logDB.warn(String.format(
                            "No matching row found in %s where id=%d and %s=%d (no update)",
                            tableName, instructionId, foreignKeyColumn, whereId));
                    return new ErrorMessage(
                            "Update Warning",
                            "No matching row found to update parent_block_id in " + tableName,
                            "No rows affected");
                }

            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException ignore) {
                }

                logDB.error(String.format(
                        "Error updating parent_block_id in %s where id=%d and %s=%d. Error: %s",
                        tableName, instructionId, foreignKeyColumn, whereId, e.getMessage()));

                return new ErrorMessage(
                        "Update Error",
                        "Failed to update parent_block_id in " + tableName
                                + " where id=" + instructionId
                                + " and " + foreignKeyColumn + "=" + whereId,
                        e.getMessage());
            }

        } catch (SQLException ex) {
            logDB.error(String.format(
                    "Connection error while updating parent_block_id in %s where id=%d and %s=%d. Error: %s",
                    tableName, instructionId, foreignKeyColumn, whereId, ex.getMessage()));

            return new ErrorMessage("Database Connection Error", "Could not connect to database", ex.getMessage());
        }
    }
}
