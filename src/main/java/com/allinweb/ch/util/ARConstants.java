package com.allinweb.ch.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ARConstants {

    // Labels & Properties
    public static final String LABELS_FILE_NAME_COMMON = "lang/labels.";
    public static final String PROPERTIES_FILE_EXTENSION = ".properties";
    // BROWSERS
    public static final String CHROME = "chrome";
    public static final String EDGE = "edge";
    public static final String FIREFOX = "firefox";
    // column names
    public static final String NAME = "name";
    public static final String ID = "id";
    // DEFAULT VALUES
    public static final String VALUE_NO_IDENTIFICATION = "No significant identification found";
    // DIMENSIONS
    public static final Double SPACE_ZERO = 0D;
    public static final Double SPACE_XS = 5D;
    public static final Double SPACE_SM = 10D;
    public static final Double SPACE_S = 24D;
    public static final Double SPACE_M = 20D;
    public static final Double SPACE_L = 30D;
    public static final Double SPACE_XL = 40D;
    public static final Double SPACE_XXS = 2.5D;
    // IMPORTANT FILE NAMES
    public static final String FILE_DEFAULT_CONFIG = "\\config\\configuration.properties";
    public static final String FILE_AR_WEB_CONFIG = "\\config\\ARWeb.config";
    public static final String FILE_NAME_PRIORITIES = "\\priorities.properties";
    public static final String FILE_NAME_ACCESS = "/database.mdb";
    public static final String FILE_NAME_SQLITE = "/database.db";
    public static final String USER_PATH = System.getProperty("user.dir");
    // ICON FILE NAMES
    public static final String ICON_APPLICATION = "/AR_icon.png";
    public static final String ICON_DIRECTORY = "/directory.png";
    public static final String ICON_DOWNLOAD = "/download-3.png";
    public static final String ICON_BURN = "/burn.png";
    public static final String ICON_REFRESH = "/refresh.png";
    public static final String ICON_REFRESH_ONLY = "/refresh-only.png";
    public static final String ICON_REFRESH_LOOP = "/refresh-loop.png";
    public static final String ICON_NEW = "/new_document.png";
    public static final String ICON_LIST = "/list.png";
    public static final String ICON_INFO = "/info.png";
    public static final String ICON_CONFIG = "/cogwheel.png";
    public static final String ICON_PLAY = "/play.png";
    public static final String ICON_BROWSER = "/open_browser.png";
    public static final String ICON_EXCEL = "/excel.png";
    public static final String ICON_EXCEL2 = "/excel2.png";
    public static final String ICON_EXCEL3 = "/excel3.png";
    public static final String ICON_EXCEL_GOTO = "/excel_goto.png";
    public static final String ICON_EDIT = "/edit.png";
    public static final String ICON_BLOCK = "/brick.png";
    public static final String ICON_COPY = "/copy.png";
    public static final String ICON_PRINT = "/print.png";
    public static final String ICON_SAVE = "/save.png";
    public static final String ICON_SEARCH = "/search.png";
    public static final String ICON_WAIT = "/wait.png";
    public static final String ICON_PAUSE = "/pause3.png";
    public static final String ICON_CLICK = "/click.png";
    public static final String ICON_LINK = "/links-icon.png";
    public static final String ICON_OUTPUT = "/output1.png";
    public static final String ICON_HIDDEN = "/hidden-black.png";
    public static final String ICON_INSERT = "/input_field.png";
    public static final String ICON_TEXT = "/text.png";
    public static final String ICON_iFRAME1 = "/iFrame1.png";
    public static final String ICON_iFRAME2 = "/iFrame2.png";
    public static final String ICON_UP = "/up.png";
    public static final String ICON_DOWN = "/down.png";
    public static final String ICON_AI = "/AI-1.png";
    public static final String ICON_CROSS = "/cross.png";
    public static final String ICON_CROSS2 = "/cross2.png";
    public static final String ICON_BIN = "/Bin.png";
    public static final String ICON_SET_VALUE = "/setValue1.png";
    public static final String ICON_GET_VALUE = "/getValue1.png";
    public static final String ICON_SET_VALUE_BTN = "/setValueBtn2.png";
    public static final String ICON_VARIABLES = "/variables.png";
    public static final String ICON_GET_VALUE_BTN = "/getValueBtn2.png";
    public static final String ICON_CHECK = "/check3.png";
    public static final String ICON_NEXT_ENTER = "/next_enter.png";
    public static final String ICON_SWIPE_UP = "/swipe_up.png";
    public static final String ICON_SWIPE_DOWN = "/swipe_down.png";
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
    public static final String ICON_ORGS = "/organization-6.png";
    public static final String ICON_SCREEN = "/screen.png";
    public static final String ICON_TICK = "/tick.png";
    public static final String ICON_EQUAL = "/equal.png";
    public static final String ICON_GREATER = "/greater.png";
    public static final String ICON_LESS = "/less.png";
    public static final String ICON_DIFFERENT = "/different.png";
    public static final String ICON_CONTAINS = "/contains.png";
    // ACTION CODES
    public static final String EXIT = "EXIT";
    public static final String IGNORE = "IGNORE";
    public static final String BY_PASS = "BY_PASS";
    public static final String CLEAR = "CLEAR";
    public static final String TAB = "TAB";
    public static final String SEND_KEYS = "SEND_KEYS";
    public static final String INSERT = "I";
    public static final String ENTER = "E";
    public static final String CLICK = "C";
    public static final String FOCUS = "FOCUS";
    public static final String SELECT = "SELECT";
    // actions codes
    public static final String COORD_VISUALIZA = "COORD_VISUALIZA";
    public static final String COORD_CLICK = "COORD_CLICK";
    public static final String COORD_INSERT = "COORD_INSERT";
    public static final String COORD_MOVE_CLICK_RED = "COORD_MOVE_CLICK_RED";
    public static final String VISUALIZE = "V";
    public static final String GET_ELEMENT = "G";
    public static final String IFRAME = "IFRAME";
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
    public static final String NEXT_ENTER = "NEXT_ENTER";
    public static final String SWIPE_UP = "SWIPE_UP";
    public static final String SWIPE_DOWN = "SWIPE_DOWN";
    public static final String EXTRACT_FIELD = "E";
    public static final String SET_VALUE = "SET";
    public static final String GET_VALUE = "GET";
    public static final String CHECK_VALUE = "CK";
    public static final String PDF_CHECK = "PDF CHECK";
    public static final String CSV_CHECK = "CSV CHECK";
    public static final String IF = "IF";
    public static final String ELSEIF = "ELSEIF";
    public static final String ELSE = "ELSE";
    public static final String ENDIF = "ENDIF";
    public static final String GOTO = "GOTO";
    public static final String EXCEL_GOTO = "EXCEL GOTO";
    public static final String NEXT_ROW = "NEXT ROW";
    public static final String FIND_ALL_CHILD_ELEMENTS = ".//*";
    public static final String NO_VALUE = "NULL";
    // ACTION SYNTAX
    public static final String EXCEL_BLOCK_HEADER = "EXCEL_BLOCK_HEADER";
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
    public static final String REGULAR_XPATH = "REGULAR_XPATH"; // BECAUSE OS LIMITATION OF ACCESS DB 255 CHARACTER
    public static final String CUSTOM_XPATH = "CUSTOM_XPATH"; // BECAUSE OS LIMITATION OF ACCESS DB 255 CHARACTER
    public static final String ATTRIBUTE_ID = "ATTRIBUTE_ID";
    public static final String ATTRIBUTE_NAME = "ATTRIBUTE_NAME";
    public static final String SEARCH_COORD = "SEARCH_COORD";
    public static final String SHADOW_DOM = "SHADOW_DOM";
    // FILE FORMATS
    public static final String FILE_FORMAT_EXCEL = ".xlsx";
    public static final String FILE_FORMAT_CSV = ".csv";
    public static final String FILE_FORMAT_LOG = ".log";
    // DATABASES
    public static final String POSTGRES = "PostGres";
    public static final String ACCESS = "Access";
    public static final String SQLSERVER = "SQLServer";
    public static final String SQLITE = "TEXT";
    public static final String XPATH_SCRIPT = "" + "window.addEventListener('click', onClick);"
            + "const onClick = (event) => {\n"
            + "  return event.srcElement.id;\n"
            + "}";
}
