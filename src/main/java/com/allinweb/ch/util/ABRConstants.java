package com.allinweb.ch.util;

public class ABRConstants {

    public enum ConditionStatus {
        NONE, // No active condition
        IF_PASSED, // IF condition was met
        IF_FAILED, // IF condition failed
        ELSEIF_PASSED, // ELSEIF condition was met
        ELSEIF_FAILED, // ELSEIF condition failed
        ELSE_PASSED,
        ELSE_FAILED,
        IF, // ELSE block is active
        ELSEIF,
        ELSE, // ELSE block is active
        ENDIF
    }

    public enum DialogModal {
        NONE,
        OK,
        STOP,
        EXIT
    }

    // Labels & Properties
    public static final String LABELS_FILE_NAME_COMMON = "lang/labels.";
    public static final String PROPERTIES_FILE_EXTENSION = ".properties";
    public static final String CONFIG_FILE_NAME = "config";

    // BROWSERS
    public static final String CHROME = "chrome";
    public static final String EDGE = "edge";
    public static final String FIREFOX = "firefox";
    public static final String SAFARI = "safari";

    // column names
    public static final String NAME = "name";
    public static final String ID = "id";

    // DEFAULT VALUES
    public static final String DEFAULT_NAME_ENGINE = "\\ABR_Web_Engine.jar";
    public static final String DEFAULT_NAME_SCANNER = "\\ABR_Web_Scanner.jar";
    public static final String DEFAULT_NAME_LAUNCHER = "\\ABR_Web_Launcher.jar";
    public static final String DEFAULT_NAME_JAVA_EXECUTABLE = "\\java.exe";

    public static final String DEFAULT_PATH_EXCEL = "\\excel";
    public static final String DEFAULT_PATH_LOG = "\\log";
    public static final String DEFAULT_PATH_JAVA = "\\java";
    public static final String DEFAULT_PATH_DB = "\\db";
    public static final String DEFAULT_PATH_REPORT = "\\report";
    public static final String DEFAULT_PATH_JAVA_FX = "\\javaFX";

    public static final String DEFAULT_FILENAME_FOR_ABR = "_filtered_for_ABR";

    public static final String DEFAULT_VALUE_NO_IDENTIFICATION = "No significant identification found";
    public static final String TO_IGNORE = "TO IGNORE";

    public static final String FOLDER_BIN = "\\bin";
    public static final String FOLDER_LIB = "\\lib";
    // DIMENSIONS
    public static final Double SPACE_ZERO = 0D;
    public static final Double SPACE_XS = 5D;
    public static final Double SPACE_SM = 10D;
    public static final Double SPACE_M = 20D;
    public static final Double SPACE_L = 30D;
    public static final Double SPACE_XL = 40D;
    public static final Double SPACE_XXS = 2.5D;

    // IMPORTANT FILE NAMES
    public static final String FILE_NAME_ENGINE_BASE_LOG = "\\abr_web_engine_base.log";
    public static final String FILE_NAME_ENGINE_RUN_LOG = "\\abr_web_engine_run.log";
    public static final String FILE_NAME_ENGINE_OUTPUT_LOG = "\\abr_web_engine_output.log";
    public static final String FILE_NAME_CONFIGURATION = "\\config\\configuration.properties";
    public static final String FILE_NAME_PRIORITIES = "\\priorities.properties";
    public static final String FILE_NAME_DB = "\\database.mdb";
    public static final String CURRENT_PATH = System.getProperty("user.dir");

    // ICON FILE NAMES
    public static final String ICON_APPLICATION = "/ABR_icon.png";
    public static final String ICON_DIRECTORY = "/directory.png";
    public static final String ICON_REFRESH = "/refresh.png";
    public static final String ICON_REFRESH_ONLY = "/refresh-only.png";
    public static final String ICON_REFRESH_LOOP = "/refresh-loop.png";
    public static final String ICON_NEW = "/new_document.png";
    public static final String ICON_LIST = "/list.png";
    public static final String ICON_INFO = "/info.png";
    public static final String ICON_CONFIG = "/cogwheel.png";
    public static final String ICON_PLAY = "/play.png";
    public static final String ICON_CHROME = "/open_browser.png";
    public static final String ICON_EXCEL = "/excel.png";
    public static final String ICON_EXCEL2 = "/excel2.png";
    public static final String ICON_EXCEL3 = "/excel3.png";
    public static final String ICON_EDIT = "/edit.png";
    public static final String ICON_BLOCK = "/brick.png";
    public static final String ICON_COPY = "/copy.png";
    public static final String ICON_PRINT = "/print.png";
    public static final String ICON_SAVE = "/save.png";
    public static final String ICON_SEARCH = "/search.png";
    public static final String ICON_WAIT = "/wait.png";
    public static final String ICON_PAUSE = "/pause3.png";
    public static final String ICON_CLICK = "/click.png";
    public static final String ICON_OUTPUT = "/output1.png";
    public static final String ICON_HIDDEN = "/hidden-black.png";
    public static final String ICON_INSERT = "/input_field.png";
    public static final String ICON_TEXT = "/text.png";
    public static final String ICON_UP = "/up.png";
    public static final String ICON_DOWN = "/down.png";
    public static final String ICON_CROSS = "/cross.png";
    public static final String ICON_CROSS2 = "/cross2.png";
    public static final String ICON_BIN = "/Bin.png";
    public static final String ICON_SET_VALUE = "/setValue1.png";
    public static final String ICON_GET_VALUE = "/getValue1.png";
    public static final String ICON_SET_VALUE_BTN = "/setValueBtn2.png";
    public static final String ICON_VARIABLES = "/variables.png";
    public static final String ICON_GET_VALUE_BTN = "/getValueBtn2.png";
    public static final String ICON_CHECK = "/check3.png";
    public static final String ICON_IF_ELSE = "/ifElse.png";
    public static final String ICON_GOTO = "/goto8.png";
    public static final String ICON_STEP = "/step.png";
    public static final String ICON_PLUS = "/plus4.png";
    public static final String ICON_BLANK = "/blank.png";
    public static final String ICON_MOVE = "/move.png";
    public static final String ICON_ARROWLEFT = "/ArrowLeft.png";
    public static final String ICON_ARROWRIGHT = "/ArrowRight.png";
    public static final String ICON_LEFT = "/left.png";
    public static final String ICON_RIGHT = "/right.png";

    public static final String ICON_DOCS = "/docs.png";
    public static final String ICON_CUBES = "/Cubes.png";
    public static final String ICON_SCREEN = "/screen.png";
    public static final String ICON_TICK = "/tick.png";
    public static final String ICON_EQUAL = "/equal.png";
    public static final String ICON_GREATER = "/greater.png";
    public static final String ICON_DIFFERENT = "/different.png";

    // ACTION CODES
    public static final String EXIT = "EXIT";
    public static final String IGNORE = "IGNORE";
    public static final String BY_PASS = "BY_PASS";
    public static final String CLEAR = "CLEAR";
    public static final String TAB = "TAB";
    public static final String SEND_KEYS = "SEND_KEYS";
    public static final String INSERT = "I";
    public static final String CLICK = "C";
    public static final String FOCUS = "FOCUS";
    public static final String SELECT = "SELECT";

    // actions codes
    public static final String COORD_VISUALIZA = "COORD_VISUALIZA";
    public static final String COORD_CLICK = "COORD_CLICK";
    public static final String COORD_INSERT = "COORD_INSERT";
    public static final String VISUALIZE = "V";
    public static final String OUTPUT = "O";
    public static final String HIDDEN = "hidden";
    public static final String OTHER = "W";
    public static final String SEARCH = "S";
    public static final String HOLD = "H";
    public static final String LOOP = "LOOP";
    public static final String REFRESH_ONLY = "REFRESH";
    public static final String REFRESH_HOLD = "REFRESH_HOLD";
    public static final String REFRESH_LOOP = "REFRESH_LOOP";
    public static final String LIST_OPERATION = "L";
    public static final String QUIT = "Q";
    public static final String SCREEN = "P";
    public static final String PAUSE = "PAUSE";
    public static final String EXTRACT_FIELD = "E";
    public static final String SET_VALUE = "SET";
    public static final String GET_VALUE = "GET";
    public static final String CHECK_VALUE = "CK";
    public static final String IF = "IF";
    public static final String ELSEIF = "ELSEIF";
    public static final String ELSE = "ELSE";
    public static final String ENDIF = "ENDIF";
    public static final String GOTO = "GOTO";
    public static final String FIND_ALL_CHILD_ELEMENTS = ".//*";
    public static final String NO_VALUE = "NULL";

    // ACTION SYNTAX
    public static final String SUBSTITUTE_FIELD_VALUE = "<#value>";
    public static final String PATH_FIELD_SUBSTITUTION = "#";
    public static final String ACTIONS_AND_PATHS_SPLITTER = ";";
    public static final String ACTION_SPECIFICATIONS_SPLITTER = ":";
    public static final String NULL_PATH_AND_TYPE_ID_SEPARATOR = "-";
    public static final String RND_ID_SEPARATOR = "-";
    public static final String FIELDS_SEPARATOR = ",";
    public static final String BLANK_STRING = " ";
    public static final String SUCCESS = "OK";
    public static final String FAIL = "KO";
    public static final String PATH_SEPARATOR = "\\";
    public static final String COMPLEX_INSTRUCTION_SEPARATOR = "\\|\\|";

    // actions specifications and various
    public static final String ABSOLUT_XPATH = "ABSOLUT_XPATH"; // BECAUSE OS LIMITATION OF ACCESS DB 255 CHARACTER
    public static final String REGULAR_XPATH = "REGULAR_XPATH"; // BECAUSE OS LIMITATION OF ACCESS DB 255 CHARACTER

    // FILE FORMATS
    public static final String FILE_FORMAT_EXCEL = ".xlsx";
    public static final String FILE_FORMAT_CSV = ".csv";
    public static final String FILE_FORMAT_LOG = ".log";

    // DATABASES
    public static final String POSTGRES = "PostGres";
    public static final String ACCESS = "Access";
    public static final String SQLSERVER = "SQLServer";

    public static final String XPATH_SCRIPT = "" + "window.addEventListener('click', onClick);"
            + "const onClick = (event) => {\n"
            + "  return event.srcElement.id;\n"
            + "}";
}
