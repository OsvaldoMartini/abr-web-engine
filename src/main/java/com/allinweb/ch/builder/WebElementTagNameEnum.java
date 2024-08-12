package com.allinweb.ch.builder;

import java.util.List;

public enum WebElementTagNameEnum {
    ALL("*"),
    INPUT("input"),
    BUTTON("button"),
    FORM("form"),
    TEXT_AREA("textarea"),
    DIV("div"),
    IMAGE("image"),
    PARAGRAPH("p"),
    ANCHOR("a"),
    SELECT("select"),
    OPTION("option"),
    MAT_SELECT("mat-select"),
    MAT_OPTION("mat-option"),
    MAT_EXPANSION_PANEL("mat-expansion-panel"),
    SET("set"),
    GET("get"),
    CK("ck");

    private String value;

    WebElementTagNameEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public static List<WebElementTagNameEnum> insertableTags() {
        return List.of(INPUT, TEXT_AREA);
    }

    public static List<WebElementTagNameEnum> clickableTags() {
        return List.of(INPUT, BUTTON, MAT_SELECT, MAT_OPTION, MAT_EXPANSION_PANEL, ANCHOR, SELECT, OPTION);
    }
}
