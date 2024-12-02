package com.allinweb.ch.util;

public class Constants {

    public static final String LABELS_FILE_NAME_COMMON = "lang/labels.";
    public static final String PROPERTIES_FILE_EXTENSION = ".properties";
    public static final String CONFIG_FILE_NAME = "config";
    public static final String CHROME = "chrome";
    public static final String EDGE = "edge";
    public static final String FIREFOX = "firefox";
    public static final String SAFARI = "safari";

    // column names
    public static final String NAME = "name";
    public static final String ID = "id";

    // actions codes
    public static final String VISUALIZE = "V";
    public static final String CLICK = "C";
    public static final String OUTPUT = "O"; // For Label and Texts
    public static final String OTHER = "W"; // For Other Kind OF
    public static final String SEARCH = "S";
    public static final String INSERT = "I";
    public static final String HOLD = "H";
    public static final String PAUSE = "PAUSE";
    public static final String REFRESH_ONLY = "REFRESH";
    public static final String REFRESH_LOOP = "REFRESH_LOOP";
    public static final String LIST_OPERATION = "L";
    public static final String QUIT = "Q";
    public static final String SCREEN = "P";
    public static final String EXTRACT = "E";
    public static final String FIND_ALL_CHILD_ELEMENTS = ".//*";

    // actions specifications and various

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
    public static final String ABSOLUT_XPATH = "ABSOLUT_XPATH"; // BECAUSE OS LIMITATION OF ACCESS DB 255 CHARACTER
    public static final String REGULAR_XPATH = "REGULAR_XPATH"; // BECAUSE OS LIMITATION OF ACCESS DB 255 CHARACTER

    public static final String FILE_FORMAT = ".xlsx";

    public static final String XPATH_SCRIPT = "" + "window.addEventListener('click', onClick);"
            + "const onClick = (event) => {\n"
            + "  return event.srcElement.id;\n"
            + "}";
}
