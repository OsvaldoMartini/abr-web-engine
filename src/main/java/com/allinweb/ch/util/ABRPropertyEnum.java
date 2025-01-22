package com.allinweb.ch.util;

public enum ABRPropertyEnum {
    DATABASE_TYPE("data_base"),
    PORT_SOCKET("port_socket"),
    BLOCK_EXEC_LIMIT("block_exec_limit"),
    FOLDER_PATH_EXCEL("path_excel"),
    FOLDER_PATH_EXPORT("path_export"),
    //    FILE_NAME_EXPORT("file_name_export"),
    FOLDER_PATH_LOG("path_log"),
    FOLDER_PATH_JAVA("path_java"),
    FOLDER_PATH_JAVA_FX("path_java_fx"),
    FOLDER_PATH_DB("path_db"),
    FOLDER_PATH_REPORT("path_report"),
    FOLDER_PATH_PRIORITY("path_priority"),
    PATH_ENGINE("path_engine"),
    PATH_WEBDRIVER("path_web_driver"),
    LOG_LEVEL("log_level"),
    MAX_LOG_SIZE("max_log_size"),
    REDUCE_SEARCH_CRITERIA("reduce_search_criteria"),
    BROWSER("browser"),
    WEBDRIVER_PAGE_UPDATE_TIMEOUT_SEC("page_update_timeout_sec"),
    WEBDRIVER_INTERACTION_TIMEOUT_SEC("interaction_timeout_sec"),
    WEBDRIVER_EXT_REFERENCE("ext_reference"),
    DEFAULT_INSTRUCTION_STOP_SECONDS("default_instruction_stop_seconds");

    private String value; // this must not be final even if suggested doing so

    ABRPropertyEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
