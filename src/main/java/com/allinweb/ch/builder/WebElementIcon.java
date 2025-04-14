package com.allinweb.ch.builder;

public enum WebElementIcon {
    HIDDEN("hidden"),
    HOLD("hold"),
    OUTPUT("output"),
    CLICK("click"),
    INSERT("insert"),
    TEXT("text"),
    SET_VALUE("SetValue"),
    GET_VALUE("GetValue"),
    CHECK_VALUE("CheckValue"),
    COPY_VAR("CopyVar"),
    IFRAME("iframe"),
    GOTO("GOTO"),
    EXTRACT_FIELD("ExcelWrite"),
    REFRESH_ONLY("Refresh"),
    REFRESH_LOOP("Refresh Loop"),
    LOOP("Loop"),
    NONE("none");

    private String value;

    WebElementIcon(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
