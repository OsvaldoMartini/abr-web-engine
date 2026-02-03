package com.allinweb.ch.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ARConstantsEngine {

    // Labels & Properties
    public static final String LABELS_FILE_NAME_COMMON = "lang/labels.";
    public static final String PROPERTIES_FILE_EXTENSION = ".properties";
    // BROWSERS
    public static final String CHROME = "chrome";
    public static final String EDGE = "edge";
    public static final String FIREFOX = "firefox";

    public static final String VALUE_NO_IDENTIFICATION = "No significant identification found";
    // IMPORTANT FILE NAMES
    public static final String FILE_NAME_CONFIGURATION = "\\config\\configuration.properties";
    public static final String FILE_NAME_PRIORITIES = "\\priorities.properties";
    public static final String FILE_NAME_ACCESS = "/database.mdb";
    public static final String FILE_NAME_SQLITE = "/database.db";
    public static final String USER_PATH = System.getProperty("user.dir");

    // ACTION CODES
    public static final String EXIT = "EXIT";
    public static final String IGNORE = "IGNORE";
    public static final String BY_PASS = "BY_PASS";
    public static final String CLEAR = "CLEAR";
    public static final String TAB = "TAB";
    public static final String SEND_KEYS = "SEND_KEYS";
    public static final String INSERT = "I";
    public static final String ENTER = "E";
    public static final String INSERT_ENTER = "I:E";
    public static final String CLICK = "C";
    public static final String FOCUS = "FOCUS";
    public static final String SELECT = "SELECT";
    // actions codes
    public static final String COORD_VISUALIZA = "COORD_VISUALIZA";
    public static final String COORD_CLICK = "COORD_CLICK";
    public static final String COORD_INSERT = "COORD_INSERT";
    public static final String COORD_MOVE_CLICK_RED = "COORD_MOVE_CLICK_RED";
    public static final String SEARCH_COORD = "SEARCH_COORD";
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
    public static final String SHADOW_DOM = "SHADOW_DOM";

    // FILE FORMATS
    public static final String FILE_FORMAT_EXCEL = ".xlsx";
}
