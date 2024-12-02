package com.allinweb.ch.util;

public class ABRConstants {

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

    public static final String FOLDER_BIN = "\\bin";
    public static final String FOLDER_LIB = "\\lib";
    // DIMENSIONS
    public static final Double SPACE_ZERO = 0D;
    public static final Double SPACE_XS = 5D;
    public static final Double SPACE_SM = 10D;
    public static final Double SPACE_M = 20D;
    public static final Double SPACE_L = 30D;
    public static final Double SPACE_XL = 40D;

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
    public static final String ICON_NEW = "/new_document.png";
    public static final String ICON_LIST = "/list.png";
    public static final String ICON_INFO = "/info.png";
    public static final String ICON_CONFIG = "/cogwheel.png";
    public static final String ICON_PLAY = "/play.png";
    public static final String ICON_CHROME = "/open_browser.png";
    public static final String ICON_EXCEL = "/excel.png";
    public static final String ICON_EDIT = "/edit.png";
    public static final String ICON_COPY = "/copy.png";
    public static final String ICON_PRINT = "/print.png";
    public static final String ICON_SAVE = "/save.png";
    public static final String ICON_SEARCH = "/search.png";
    public static final String ICON_WAIT = "/wait.png";
    public static final String ICON_CLICK = "/click.png";
    public static final String ICON_INSERT = "/input_field.png";
    public static final String ICON_UP = "/up.png";
    public static final String ICON_DOWN = "/down.png";
    public static final String ICON_CROSS = "/cross.png";

    // ACTION CODES
    public static final String VISUALIZE = "V";
    public static final String OUTPUT = "O";
    public static final String OTHER = "W";
    public static final String CLICK = "C";
    public static final String SEARCH = "S";
    public static final String INSERT = "I";
    public static final String HOLD = "H";
    public static final String PAUSE = "PAUSE";
    public static final String REFRESH_ONLY = "REFRESH";
    public static final String REFRESH_LOOP = "REFRESH_LOOP";
    public static final String LIST_OPERATION = "L";
    public static final String QUIT = "Q";
    public static final String SCREEN = "P";
    public static final String EXTRACT_FIELD = "E";
    public static final String SET_VALUE = "SET";
    public static final String GET_VALUE = "GET";
    public static final String CHECK_VALUE = "CK";
    public static final String IF = "IF";
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

    // FILE FORMATS
    public static final String FILE_FORMAT_EXCEL = ".xlsx";
    public static final String FILE_FORMAT_LOG = ".log";

    // BROWSERS
    public static final String CHROME = "chrome";
    public static final String EDGE = "edge";
    public static final String FIREFOX = "firefox";
    public static final String SAFARI = "safari";
}
