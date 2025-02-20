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
    LABEL("label"),
    FOR_LABEL("for"),
    INNER_HTML("innerHTML"),
    HREF("href"),
    DATA_TEST_ID("data-testid"), // Assuming 'data-testid' is used for testing IDs
    CLASS("class"),
    STYLE("style"),
    TITLE("title"),
    DISABLED("disabled"),
    MAT_LABEL("mat-label"),
    MAT_INPUT("mat-input"),
    INPUT("input");

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
