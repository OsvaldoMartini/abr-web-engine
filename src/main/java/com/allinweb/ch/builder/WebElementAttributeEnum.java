package com.allinweb.ch.builder;

/***
 *
 */
public enum WebElementAttributeEnum {
    // ATTENTION: ORDER OF DECLARATION IS IMPORTANT TO DETERMINE IDENTIFICATION PRIORITY
    FORM_CONTROL_NAME("formcontrolname"),
    TEST_ID("test-id"),
    ID("id"),
    NAME("name"),
    TYPE("type"),
    VALUE("value"),
    ARIA_LABEL("aria-label"),
    INNER_HTML("innerHTML");

    private String value;

    WebElementAttributeEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
