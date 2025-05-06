package com.allinweb.ch.util;

public enum ARPropertyEnum {
    PATH_LICENSE("path_license"),
    DATABASE_TYPE("data_base"),
    PORT_SOCKET("port_socket"),
    PATH_EXCEL("path_excel"),
    PATH_EXPORT("path_export"),
    //    FILE_NAME_EXPORT("file_name_export"),
    PATH_LOG("path_log"),
    PATH_JAVA("path_java"),
    PATH_JAVA_FX("path_java_fx"),
    PATH_DB("path_db"),
    PATH_REPORT("path_report"),
    PATH_PRIORITY("path_priority"),
    PATH_ENGINE("path_engine"),
    PATH_WEBDRIVER("path_web_driver"),
    LOG_LEVEL("log_level"),
    MAX_LOG_SIZE("max_log_size"),
    BROWSER("browser"),
    VERSION("version"),
    BUILD("build"),
    EXPIRATION("expiration"),
    WEBDRIVER_PAGE_UPDATE_TIMEOUT_SEC("page_update_timeout_sec"),
    WEBDRIVER_INTERACTION_TIMEOUT_SEC("interaction_timeout_sec"),
    WEBDRIVER_EXT_REFERENCE("ext_reference"),
    INSTRUCTION_STOP_SECONDS("default_instruction_stop_seconds");

    private String value; // this must not be final even if suggested doing so

    ARPropertyEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
