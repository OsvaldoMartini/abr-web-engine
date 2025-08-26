package com.allinweb.ch.facade;

import com.allinweb.ch.util.*;
import java.io.File;
import java.sql.*;
import lombok.Getter;
import lombok.Setter;

/**
 * PerformActions.
 *
 * @author Osvaldo Martini
 * @version 1.0
 */
public class PerformInitializer {
    protected static volatile PerformInitializer instance;

    @Getter
    @Setter
    public Connection conn = null;

    // Private constructor to prevent instantiation
    private PerformInitializer() {}

    public static PerformInitializer getInstance() {
        if (instance == null) {
            synchronized (PerformInitializer.class) {
                if (instance == null) {
                    instance = new PerformInitializer();
                }
            }
        }
        return instance;
    }

    public final String CONNECTION_TYPE = "jdbc:ucanaccess://";
    public final String CONNECTION_PARAMETERS = ";memory=false;newDatabaseVersion=V2010";
    public final String CONNECTION_TYPE_SQLITE = "jdbc:sqlite:"; // no parameters needed

    private static final ARPropertyManager arPropertyManager = ARPropertyManager.getInstance();
    private static final PerformDataBase performDataBase = PerformDataBase.getInstance();
    private static final PerformMessage performMessage = PerformMessage.getInstance();

    public void initialize(Connection conn) {
        this.conn = conn;
    }

    public ErrorMessage initializeMainDatabasePostgres() {
        try (Connection conn = performDataBase.getConnection()) {
            try (Statement stmt = conn.createStatement();
                    Statement dropTableStmt = conn.createStatement()) {

                dropTableStmt.executeUpdate("DROP TABLE IF EXISTS component_reference CASCADE");
                dropTableStmt.executeUpdate("DROP TABLE IF EXISTS component_variable CASCADE");
                dropTableStmt.executeUpdate("DROP TABLE IF EXISTS component_instruction CASCADE");
                dropTableStmt.executeUpdate("DROP TABLE IF EXISTS component_block CASCADE");

                dropTableStmt.executeUpdate("DROP TABLE IF EXISTS reference CASCADE");
                dropTableStmt.executeUpdate("DROP TABLE IF EXISTS variable CASCADE");
                dropTableStmt.executeUpdate("DROP TABLE IF EXISTS instruction CASCADE");
                dropTableStmt.executeUpdate("DROP TABLE IF EXISTS block CASCADE");
                dropTableStmt.executeUpdate("DROP TABLE IF EXISTS bot_job CASCADE");
                dropTableStmt.executeUpdate("DROP TABLE IF EXISTS home_url CASCADE");
                dropTableStmt.executeUpdate("DROP TABLE IF EXISTS home_banking CASCADE");

                // Create home_banking table
                String createHomeBankingTableSQL = "CREATE TABLE home_banking ("
                        + "ID INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, "
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
                        + "ID INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, "
                        + "url TEXT, "
                        + "home_banking_id INTEGER REFERENCES home_banking(id) ON DELETE CASCADE)";
                stmt.executeUpdate(createURLTableSQL);

                // Create bot_job table with a foreign key reference to home_banking
                String createBotJobTableSQL = "CREATE TABLE bot_job ("
                        + "id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, "
                        + "name TEXT UNIQUE, "
                        + "description TEXT, "
                        + "priority TEXT, "
                        + "active INTEGER NOT NULL, "
                        + "home_banking_id INTEGER REFERENCES home_banking(id) ON DELETE CASCADE, "
                        + "home_url_id INTEGER REFERENCES home_url(id) ON DELETE CASCADE)";
                stmt.executeUpdate(createBotJobTableSQL);

                // Create block table with a foreign key reference to bot_job
                String createBlockTableSQL = "CREATE TABLE block ("
                        + "id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, "
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
                        + "id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, "
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
                        + "parent_block_id INTEGER REFERENCES block(id) ON DELETE CASCADE, "
                        + "parent_id INTEGER, "
                        + "bot_job_id INTEGER REFERENCES bot_job(id) ON DELETE CASCADE)";
                stmt.executeUpdate(createInstructionTableSQL);

                String createReferenceTableSQL = "CREATE TABLE reference ("
                        + "id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, "
                        + "reference_type TEXT, "
                        + "value TEXT, "
                        + "instruction_id INTEGER NOT NULL REFERENCES instruction(id) ON DELETE CASCADE, "
                        + "bot_job_id INTEGER REFERENCES bot_job(id) ON DELETE CASCADE)";
                stmt.executeUpdate(createReferenceTableSQL);

                String createVariableTableSQL = "CREATE TABLE variable ("
                        + "id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, "
                        + "type TEXT, "
                        + "name TEXT, "
                        + "value TEXT, "
                        + "local_format TEXT,"
                        + "delimiter TEXT,"
                        + "instruction_id INTEGER REFERENCES instruction(id) ON DELETE CASCADE, "
                        + "bot_job_id INTEGER REFERENCES bot_job(id) ON DELETE CASCADE)";
                stmt.executeUpdate(createVariableTableSQL);

                String createComponentBlockTableSQL = "CREATE TABLE component_block ("
                        + "id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, "
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
                        + "id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, "
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
                        + "parent_block_id INTEGER REFERENCES component_block(id) ON DELETE CASCADE, "
                        + "parent_id INTEGER, "
                        + "home_banking_id INTEGER REFERENCES home_banking(id) ON DELETE CASCADE)";
                stmt.executeUpdate(createComponentInstructionTableSQL);

                String createComponentReferenceTableSQL = "CREATE TABLE component_reference ("
                        + "id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, "
                        + "reference_type TEXT, "
                        + "value TEXT, "
                        + "instruction_id INTEGER NOT NULL REFERENCES component_instruction(id) ON DELETE CASCADE, "
                        + "home_banking_id INTEGER REFERENCES home_banking(id) ON DELETE CASCADE)";
                stmt.executeUpdate(createComponentReferenceTableSQL);

                String createComponentVariableTableSQL = "CREATE TABLE component_variable ("
                        + "id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, "
                        + "type TEXT, "
                        + "name TEXT, "
                        + "value TEXT, "
                        + "local_format TEXT,"
                        + "delimiter TEXT,"
                        + "instruction_id INTEGER REFERENCES component_instruction(id) ON DELETE CASCADE, "
                        + "home_banking_id INTEGER REFERENCES home_banking(id) ON DELETE CASCADE)";
                stmt.executeUpdate(createComponentVariableTableSQL);

                // Foreign Keys with ON DELETE CASCADE
                stmt.executeUpdate(
                        "ALTER TABLE variable ADD CONSTRAINT fk_variable_instruction FOREIGN KEY (instruction_id) REFERENCES instruction(id) ON DELETE CASCADE");
                //                stmt.executeUpdate(
                //                        "ALTER TABLE instruction ADD CONSTRAINT fk_instruction_variable FOREIGN KEY
                // (variable_id) REFERENCES variable(id) ON DELETE CASCADE");

                stmt.executeUpdate(
                        "ALTER TABLE component_variable ADD CONSTRAINT fk_component_variable_instruction FOREIGN KEY (instruction_id) REFERENCES component_instruction(id) ON DELETE CASCADE");

                //                stmt.executeUpdate(
                //                        "ALTER TABLE component_instruction ADD CONSTRAINT
                // fk_component_instruction_variable FOREIGN KEY (variable_id) REFERENCES component_variable(id) ON
                // DELETE CASCADE");
            }
            return null;
        } catch (SQLException error) {
            return new ErrorMessage(
                    "Postgres Database Creation Error",
                    "An error occurred while creating a new database",
                    error.getMessage());
        }
    }

    private void dropExistingTablesAccess(Statement stmt) throws SQLException {
        String[] tables = {
            "component_reference", "component_variable", "component_instruction", "component_block",
            "reference", "variable", "instruction", "block",
            "bot_job", "home_url", "home_banking"
        };
        for (String table : tables) {
            try {
                stmt.executeUpdate("DROP TABLE " + table);
            } catch (SQLException e) {
                // Ignore errors if table doesn't exist
            }
        }
    }

    public ErrorMessage initializeMainDatabaseAccess(File dbFile) {
        try (Connection conn = performDataBase.getConnection()) {
            try (Statement stmt = conn.createStatement()) {

                // HOME BANKING
                stmt.executeUpdate("CREATE TABLE home_banking (" + "ID AUTOINCREMENT PRIMARY KEY, "
                        + "url MEMO, "
                        + "name TEXT, "
                        + "priority MEMO, "
                        + "search_config MEMO, "
                        + "options_config MEMO, "
                        + "cookies MEMO, "
                        + "driver_session MEMO, "
                        + "username TEXT, "
                        + "password TEXT)");

                // HOME URL
                stmt.executeUpdate("CREATE TABLE home_url (" + "ID AUTOINCREMENT PRIMARY KEY, "
                        + "url MEMO, "
                        + "home_banking_id INTEGER)");

                // BOT JOB
                stmt.executeUpdate("CREATE TABLE bot_job (" + "id AUTOINCREMENT PRIMARY KEY, "
                        + "name TEXT UNIQUE, "
                        + "description TEXT, "
                        + "priority MEMO, "
                        + "active YESNO NOT NULL, "
                        + "home_banking_id INTEGER, "
                        + "home_url_id INTEGER)");

                // BLOCK
                stmt.executeUpdate("CREATE TABLE block (" + "id AUTOINCREMENT PRIMARY KEY, "
                        + "block_order_number INTEGER NOT NULL, "
                        + "name TEXT NOT NULL, "
                        + "description TEXT, "
                        + "type_id INTEGER, "
                        + "export_file TEXT, "
                        + "active YESNO NOT NULL, "
                        + "wait INTEGER, "
                        + "bot_job_id INTEGER)");

                // INSTRUCTION
                stmt.executeUpdate("CREATE TABLE instruction (" + "id AUTOINCREMENT PRIMARY KEY, "
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
                        + "parent_block_id INTEGER, "
                        + "parent_id INTEGER, "
                        + "bot_job_id INTEGER)");

                // REFERENCE
                stmt.executeUpdate("CREATE TABLE reference (" + "id AUTOINCREMENT PRIMARY KEY, "
                        + "reference_type TEXT, "
                        + "value MEMO, "
                        + "instruction_id INTEGER NOT NULL, "
                        + "bot_job_id INTEGER)");

                // VARIABLE
                stmt.executeUpdate("CREATE TABLE variable (" + "id AUTOINCREMENT PRIMARY KEY, "
                        + "type TEXT, "
                        + "name TEXT, "
                        + "value MEMO, "
                        + "instruction_id INTEGER, "
                        + "bot_job_id INTEGER, "
                        + "local_format TEXT, "
                        + "delimiter TEXT)");

                // COMPONENT BLOCK
                stmt.executeUpdate("CREATE TABLE component_block (" + "id AUTOINCREMENT PRIMARY KEY, "
                        + "home_banking_id INTEGER, "
                        + "block_order_number INTEGER NOT NULL, "
                        + "name TEXT NOT NULL, "
                        + "description TEXT, "
                        + "type_id INTEGER, "
                        + "export_file TEXT, "
                        + "active YESNO, "
                        + "wait INTEGER)");

                // COMPONENT INSTRUCTION
                stmt.executeUpdate("CREATE TABLE component_instruction (" + "id AUTOINCREMENT PRIMARY KEY, "
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
                        + "parent_block_id INTEGER, "
                        + "parent_id INTEGER, "
                        + "home_banking_id INTEGER)");

                // COMPONENT REFERENCE
                stmt.executeUpdate("CREATE TABLE component_reference (" + "id AUTOINCREMENT PRIMARY KEY, "
                        + "reference_type TEXT, "
                        + "value MEMO, "
                        + "instruction_id INTEGER NOT NULL, "
                        + "home_banking_id INTEGER)");

                // COMPONENT VARIABLE
                stmt.executeUpdate("CREATE TABLE component_variable (" + "id AUTOINCREMENT PRIMARY KEY, "
                        + "type TEXT, "
                        + "name TEXT, "
                        + "value MEMO, "
                        + "instruction_id INTEGER, "
                        + "home_banking_id INTEGER, "
                        + "local_format TEXT, "
                        + "delimiter TEXT)");

                // Foreign Keys with ON DELETE CASCADE
                stmt.executeUpdate(
                        "ALTER TABLE home_url ADD CONSTRAINT fk_home_url_home_banking FOREIGN KEY (home_banking_id) REFERENCES home_banking(ID) ON DELETE CASCADE");
                stmt.executeUpdate(
                        "ALTER TABLE bot_job ADD CONSTRAINT fk_bot_job_home_banking FOREIGN KEY (home_banking_id) REFERENCES home_banking(ID) ON DELETE CASCADE");
                stmt.executeUpdate(
                        "ALTER TABLE bot_job ADD CONSTRAINT fk_bot_job_home_url FOREIGN KEY (home_url_id) REFERENCES home_url(ID) ON DELETE CASCADE");
                stmt.executeUpdate(
                        "ALTER TABLE block ADD CONSTRAINT fk_block_bot_job FOREIGN KEY (bot_job_id) REFERENCES bot_job(id) ON DELETE CASCADE");
                stmt.executeUpdate(
                        "ALTER TABLE instruction ADD CONSTRAINT fk_instruction_block FOREIGN KEY (block_id) REFERENCES block(id) ON DELETE CASCADE");

                //                stmt.executeUpdate(
                //                        "ALTER TABLE instruction ADD CONSTRAINT fk_instruction_variable FOREIGN KEY
                // (variable_id) REFERENCES variable(id) ON DELETE CASCADE");
                stmt.executeUpdate(
                        "ALTER TABLE instruction ADD CONSTRAINT fk_instruction_parent_block FOREIGN KEY (parent_block_id) REFERENCES block(id) ON DELETE CASCADE");

                //                // NOT POSSIBLE SELF REFERENCE IN ACCESS
                //                stmt.executeUpdate(
                //                        "ALTER TABLE instruction ADD CONSTRAINT fk_instruction_parent FOREIGN KEY
                // (parent_id) REFERENCES instruction(id)");

                stmt.executeUpdate(
                        "ALTER TABLE instruction ADD CONSTRAINT fk_instruction_bot_job FOREIGN KEY (bot_job_id) REFERENCES bot_job(id) ON DELETE CASCADE");

                stmt.executeUpdate(
                        "ALTER TABLE reference ADD CONSTRAINT fk_reference_instruction FOREIGN KEY (instruction_id) REFERENCES instruction(id) ON DELETE CASCADE");

                stmt.executeUpdate(
                        "ALTER TABLE reference ADD CONSTRAINT fk_reference_bot_job FOREIGN KEY (bot_job_id) REFERENCES bot_job(id) ON DELETE CASCADE");

                stmt.executeUpdate(
                        "ALTER TABLE variable ADD CONSTRAINT fk_variable_instruction FOREIGN KEY (instruction_id) REFERENCES instruction(id) ON DELETE CASCADE");

                stmt.executeUpdate(
                        "ALTER TABLE variable ADD CONSTRAINT fk_variable_bot_job FOREIGN KEY (bot_job_id) REFERENCES bot_job(id) ON DELETE CASCADE");

                stmt.executeUpdate(
                        "ALTER TABLE component_block ADD CONSTRAINT fk_component_block_home_banking FOREIGN KEY (home_banking_id) REFERENCES home_banking(ID) ON DELETE CASCADE");

                stmt.executeUpdate(
                        "ALTER TABLE component_instruction ADD CONSTRAINT fk_component_instruction_block FOREIGN KEY (block_id) REFERENCES component_block(id) ON DELETE CASCADE");

                //                stmt.executeUpdate(
                //                        "ALTER TABLE component_instruction ADD CONSTRAINT
                // fk_component_instruction_variable FOREIGN KEY (variable_id) REFERENCES component_variable(id) ON
                // DELETE CASCADE");

                stmt.executeUpdate(
                        "ALTER TABLE component_instruction ADD CONSTRAINT fk_component_instruction_parent_block FOREIGN KEY (parent_block_id) REFERENCES component_block(id) ON DELETE CASCADE");

                // NOT POSSIBLE SELF REFERENCE IN ACCESS
                //                stmt.executeUpdate(
                //                        "ALTER TABLE component_instruction ADD CONSTRAINT
                // fk_component_instruction_parent FOREIGN KEY (parent_id) REFERENCES component_instruction(id)");

                stmt.executeUpdate(
                        "ALTER TABLE component_instruction ADD CONSTRAINT fk_component_instruction_home_banking FOREIGN KEY (home_banking_id) REFERENCES home_banking(ID) ON DELETE CASCADE");

                stmt.executeUpdate(
                        "ALTER TABLE component_reference ADD CONSTRAINT fk_component_reference_instruction FOREIGN KEY (instruction_id) REFERENCES component_instruction(id) ON DELETE CASCADE");

                stmt.executeUpdate(
                        "ALTER TABLE component_reference ADD CONSTRAINT fk_component_reference_home_banking FOREIGN KEY (home_banking_id) REFERENCES home_banking(ID) ON DELETE CASCADE");

                stmt.executeUpdate(
                        "ALTER TABLE component_variable ADD CONSTRAINT fk_component_variable_instruction FOREIGN KEY (instruction_id) REFERENCES component_instruction(id) ON DELETE CASCADE");

                stmt.executeUpdate(
                        "ALTER TABLE component_variable ADD CONSTRAINT fk_component_variable_home_banking FOREIGN KEY (home_banking_id) REFERENCES home_banking(ID) ON DELETE CASCADE");
            }
            System.out.printf("Database %s has been created with cascading deletes!%n", dbFile.getName());

            return null;
        } catch (SQLException error) {
            return new ErrorMessage(
                    "Access Database Creation Error",
                    "An error occurred while creating a new database",
                    error.getMessage());
        }
    }

    public ErrorMessage initializeMainDatabaseSQLite(File dbFile) {
        try (Connection conn = performDataBase.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                // Enable foreign keys in SQLite
                stmt.execute("PRAGMA foreign_keys = ON;");

                String createHomeBankingTableSQL = "CREATE TABLE home_banking ("
                        + "ID INTEGER PRIMARY KEY AUTOINCREMENT, "
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

                String createURLTableSQL = "CREATE TABLE home_url ("
                        + "ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "url TEXT, "
                        + "home_banking_id INTEGER, "
                        + "FOREIGN KEY(home_banking_id) REFERENCES home_banking(id) ON DELETE CASCADE)";
                stmt.executeUpdate(createURLTableSQL);

                String createBotJobTableSQL = "CREATE TABLE bot_job ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "name TEXT UNIQUE, "
                        + "description TEXT, "
                        + "priority TEXT, "
                        + "active INTEGER NOT NULL, "
                        + "home_banking_id INTEGER, "
                        + "home_url_id INTEGER, "
                        + "FOREIGN KEY(home_banking_id) REFERENCES home_banking(id) ON DELETE CASCADE, "
                        + "FOREIGN KEY(home_url_id) REFERENCES home_url(id) ON DELETE CASCADE)";
                stmt.executeUpdate(createBotJobTableSQL);

                String createBlockTableSQL = "CREATE TABLE block ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "block_order_number INTEGER NOT NULL, "
                        + "name TEXT NOT NULL, "
                        + "description TEXT, "
                        + "type_id INTEGER, "
                        + "export_file TEXT, "
                        + "active INTEGER NOT NULL, "
                        + "wait INTEGER, "
                        + "bot_job_id INTEGER, "
                        + "FOREIGN KEY(bot_job_id) REFERENCES bot_job(id) ON DELETE CASCADE)";
                stmt.executeUpdate(createBlockTableSQL);

                String createInstructionTableSQL = "CREATE TABLE instruction ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
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
                        + "block_id INTEGER, "
                        + "variable_id INTEGER, "
                        + "parent_block_id INTEGER, "
                        + "parent_id INTEGER, "
                        + "bot_job_id INTEGER, "
                        //                        + "FOREIGN KEY(variable_id) REFERENCES variable(id) ON DELETE CASCADE,
                        // "
                        + "FOREIGN KEY(parent_block_id) REFERENCES block(id) ON DELETE CASCADE, "
                        + "FOREIGN KEY(block_id) REFERENCES block(id) ON DELETE CASCADE, "
                        + "FOREIGN KEY(bot_job_id) REFERENCES bot_job(id) ON DELETE CASCADE)";
                stmt.executeUpdate(createInstructionTableSQL);

                String createReferenceTableSQL = "CREATE TABLE reference ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "reference_type TEXT, "
                        + "value TEXT, "
                        + "instruction_id INTEGER NOT NULL, "
                        + "bot_job_id INTEGER, "
                        + "FOREIGN KEY(instruction_id) REFERENCES instruction(id) ON DELETE CASCADE, "
                        + "FOREIGN KEY(bot_job_id) REFERENCES bot_job(id) ON DELETE CASCADE)";
                stmt.executeUpdate(createReferenceTableSQL);

                String createVariableTableSQL = "CREATE TABLE variable ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "type TEXT, "
                        + "name TEXT, "
                        + "value TEXT, "
                        + "local_format TEXT, "
                        + "delimiter TEXT, "
                        + "instruction_id INTEGER, "
                        + "bot_job_id INTEGER, "
                        + "FOREIGN KEY(instruction_id) REFERENCES instruction(id) ON DELETE CASCADE, "
                        + "FOREIGN KEY(bot_job_id) REFERENCES bot_job(id) ON DELETE CASCADE)";
                stmt.executeUpdate(createVariableTableSQL);

                String createComponentBlockTableSQL = "CREATE TABLE component_block ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "home_banking_id INTEGER, "
                        + "block_order_number INTEGER NOT NULL, "
                        + "name TEXT NOT NULL, "
                        + "description TEXT, "
                        + "type_id INTEGER, "
                        + "export_file TEXT, "
                        + "active INTEGER, "
                        + "wait INTEGER, "
                        + "FOREIGN KEY(home_banking_id) REFERENCES home_banking(id) ON DELETE CASCADE)";
                stmt.executeUpdate(createComponentBlockTableSQL);

                String createComponentInstructionTableSQL = "CREATE TABLE component_instruction ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
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
                        + "block_id INTEGER, "
                        + "variable_id INTEGER, "
                        + "parent_block_id INTEGER, "
                        + "parent_id INTEGER, "
                        + "home_banking_id INTEGER, "
                        //                        + "FOREIGN KEY(variable_id) REFERENCES variable(id) ON DELETE CASCADE,
                        // "
                        + "FOREIGN KEY(parent_block_id) REFERENCES block(id) ON DELETE CASCADE, "
                        + "FOREIGN KEY(block_id) REFERENCES component_block(id) ON DELETE CASCADE, "
                        + "FOREIGN KEY(home_banking_id) REFERENCES home_banking(id) ON DELETE CASCADE)";
                stmt.executeUpdate(createComponentInstructionTableSQL);

                String createComponentReferenceTableSQL = "CREATE TABLE component_reference ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "reference_type TEXT, "
                        + "value TEXT, "
                        + "instruction_id INTEGER NOT NULL, "
                        + "home_banking_id INTEGER, "
                        + "FOREIGN KEY(instruction_id) REFERENCES component_instruction(id) ON DELETE CASCADE, "
                        + "FOREIGN KEY(home_banking_id) REFERENCES home_banking(id) ON DELETE CASCADE)";
                stmt.executeUpdate(createComponentReferenceTableSQL);

                String createComponentVariableTableSQL = "CREATE TABLE component_variable ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "type TEXT, "
                        + "name TEXT, "
                        + "value TEXT, "
                        + "local_format TEXT, "
                        + "delimiter TEXT, "
                        + "instruction_id INTEGER, "
                        + "home_banking_id INTEGER, "
                        + "FOREIGN KEY(instruction_id) REFERENCES component_instruction(id) ON DELETE CASCADE, "
                        + "FOREIGN KEY(home_banking_id) REFERENCES home_banking(id) ON DELETE CASCADE)";
                stmt.executeUpdate(createComponentVariableTableSQL);
            }

            return null;
        } catch (SQLException error) {
            return new ErrorMessage(
                    "SQLite Database Creation Error",
                    "An error occurred while creating a new database",
                    error.getMessage());
        }
    }

    public boolean doesNotInstructionTableExistAccess(Connection conn) throws SQLException {
        try {
            if (conn != null && conn.getMetaData() != null) {
                setConn(conn);
                try (ResultSet rs = conn.getMetaData().getTables(null, null, "instruction", null)) {
                    return !rs.next(); // true if the table does NOT exist
                }
            }
        } catch (SQLException error) {
            System.out.println("Error checking table existence: " + error.getMessage());
            throw error; // rethrow so caller handles it
        } catch (Exception error) {
            System.out.println("Unexpected error while checking table existence: " + error.getMessage());
            throw new SQLException("Unexpected error while checking table existence", error);
        }

        return true; // Assume table does not exist if no valid connection or metadata
    }

    public boolean doesNotInstructionTableExist(Connection conn) throws SQLException {
        try {
            if (conn != null && conn.getMetaData() != null) {
                setConn(conn);
                try (ResultSet rs = conn.getMetaData().getTables(null, null, "instruction", null)) {
                    return !rs.next(); // true if the table does NOT exist
                }
            }
        } catch (SQLException error) {
            System.out.println("SQL error while checking table existence: " + error.getMessage());
            throw error;
        } catch (Exception error) {
            System.out.println("Unexpected error while checking table existence: " + error.getMessage());
            throw new SQLException("Unexpected error while checking table existence", error);
        }

        return true; // assume table does not exist if no connection or metadata
    }

    public boolean doesNotInstructionTableExistSQLITE(Connection conn) throws SQLException {
        try {
            if (conn != null && conn.getMetaData() != null) {
                try (ResultSet rs = conn.getMetaData().getTables(null, null, "instruction", null)) {
                    return !rs.next(); // true if the table does NOT exist
                }
            }
        } catch (SQLException error) {
            System.out.println("SQL error while checking SQLite table existence: " + error.getMessage());
            throw error;
        } catch (Exception error) {
            System.out.println("Unexpected error while checking SQLite table existence: " + error.getMessage());
            throw new SQLException("Unexpected error while checking SQLite table existence", error);
        }

        return true;
    }

    public void testConnection(String dataBaseType, String dbUrlPath, String dbUrl, String userDB, String userPwd)
            throws SQLException, ClassNotFoundException {

        if (dataBaseType == null) {
            throw new IllegalArgumentException("Database type is required.");
        }

        Connection conn = null;

        try {
            if ("Postgres".equalsIgnoreCase(dataBaseType)) {
                // PostgreSQL
                ARLogger.getInstance(PerformDataBase.class).info("Postgres URL: " + dbUrl);
                Class.forName("org.postgresql.Driver");
                conn = DriverManager.getConnection(dbUrl, userDB, userPwd);

            } else if ("SQLServer".equalsIgnoreCase(dataBaseType)) {
                // SQLite
                ARLogger.getInstance(PerformDataBase.class).info("SQLServer URL: " + dbUrl);
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                conn = DriverManager.getConnection(dbUrl, userDB, userPwd);
            } else if ("SQLite".equalsIgnoreCase(dataBaseType)) {
                // SQLite
                String dbSQLiteUrl = CONNECTION_TYPE_SQLITE + dbUrlPath + ARConstants.FILE_NAME_SQLITE;
                ARLogger.getInstance(PerformDataBase.class).info("SQLite connection URL: " + dbSQLiteUrl);
                Class.forName("org.sqlite.JDBC");
                conn = DriverManager.getConnection(dbSQLiteUrl);

            } else {
                // Default to Access
                String dbAccessUrl = CONNECTION_TYPE + dbUrlPath + ARConstants.FILE_NAME_ACCESS + CONNECTION_PARAMETERS;
                ARLogger.getInstance(PerformDataBase.class).info("Access connection URL: " + dbAccessUrl);
                Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
                conn = DriverManager.getConnection(dbAccessUrl);
            }

            // Wait for 3 seconds to simulate holding the connection
            try {
                Thread.sleep(3000); // 3000 ms = 3 seconds
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt(); // Restore interrupt status
            }

        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    System.out.println("Error closing connection: " + e.getMessage());
                }
            }
        }
    }

    public void initializeDBS() {
        if (performDataBase.POSTGRES_DB) {
            try {
                if (doesNotInstructionTableExist(performDataBase.getConnection())) {
                    if (getConn() != null) {
                        ErrorMessage errorMessage = initializeMainDatabasePostgres();
                        if (errorMessage != null) {
                            ARLogger.getInstance(PerformInitializer.class)
                                    .severe("Database Creation Error: " + errorMessage.getErrorMessage());

                            performMessage.errorMessage(
                                    errorMessage.getErrorTitle(),
                                    "<span style='color: #D32F2F; font-weight: bold; font-size: 1.1em;'>Operation Failed!</span>",
                                    "<span style='color: #E65100; font-weight: bold;'>Error Type:</span> Database Creation Error",
                                    "<span style='color: #2E7D32; font-weight: bold;'>" + errorMessage.getErrorHeader()
                                            + "</span>",
                                    "<span style='font-style: italic;'>Detail:</span> "
                                            + errorMessage.getErrorMessage(),
                                    0);
                        }
                    }
                }
            } catch (Exception error) {
                ARLogger.getInstance(PerformInitializer.class)
                        .severe("Error connection with Postgres: " + error.getMessage());
            }
        } else if (performDataBase.SQLITE_DB) {
            String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
            File dbFile = new File(dbPath + ARConstants.FILE_NAME_SQLITE);

            try {
                if (doesNotInstructionTableExistSQLITE(performDataBase.getConnection())) {
                    if (getConn() != null) {
                        ErrorMessage errorMessage = initializeMainDatabaseSQLite(dbFile);

                        if (errorMessage != null) {
                            ARLogger.getInstance(PerformInitializer.class)
                                    .severe("Database Creation Error: " + errorMessage.getErrorMessage());

                            performMessage.errorMessage(
                                    errorMessage.getErrorTitle(),
                                    "<span style='color: #D32F2F; font-weight: bold; font-size: 1.1em;'>Operation Failed!</span>",
                                    "<span style='color: #E65100; font-weight: bold;'>Error Type:</span> Database Creation Error",
                                    "<span style='color: #2E7D32; font-weight: bold;'>" + errorMessage.getErrorHeader()
                                            + "</span>",
                                    "<span style='font-style: italic;'>Detail:</span> "
                                            + errorMessage.getErrorMessage(),
                                    0);
                        }
                    }
                }
            } catch (Exception error) {
                ARLogger.getInstance(PerformInitializer.class)
                        .severe("Error connection with SQLite: " + error.getMessage());
            }
        } else if (performDataBase.ACCESS_DB) {
            String dbPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_DB);
            File dbFile = new File(dbPath + ARConstants.FILE_NAME_ACCESS);

            try {
                if (doesNotInstructionTableExistAccess(performDataBase.getConnection())) {
                    if (getConn() != null) {
                        ErrorMessage errorMessage = initializeMainDatabaseAccess(dbFile);

                        if (errorMessage != null) {
                            ARLogger.getInstance(PerformInitializer.class)
                                    .severe("Database Creation Error: " + errorMessage.getErrorMessage());

                            performMessage.errorMessage(
                                    errorMessage.getErrorTitle(),
                                    "<span style='color: #D32F2F; font-weight: bold; font-size: 1.1em;'>Operation Failed!</span>",
                                    "<span style='color: #E65100; font-weight: bold;'>Error Type:</span> Database Creation Error",
                                    "<span style='color: #2E7D32; font-weight: bold;'>" + errorMessage.getErrorHeader()
                                            + "</span>",
                                    "<span style='font-style: italic;'>Detail:</span> "
                                            + errorMessage.getErrorMessage(),
                                    0);
                        }
                    }
                }
            } catch (Exception error) {
                ARLogger.getInstance(PerformInitializer.class)
                        .severe("Error connection with Access: " + error.getMessage());
            }
        }
    }
}
