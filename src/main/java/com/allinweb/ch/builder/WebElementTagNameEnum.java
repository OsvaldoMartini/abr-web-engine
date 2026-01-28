package com.allinweb.ch.builder;

import java.util.List;

public enum WebElementTagNameEnum {
    ALL("*"),
    INPUT("input"),
    INPUT_ENTER("I:E"),
    OUTPUT("O"),
    BUTTON("button"),
    FORM("form"),
    HIDDEN("hidden"),
    TEXT_AREA("textarea"),
    DIV("div"),
    SPAN("span"),
    IMAGE("image"),
    HEADER("H"),
    PARAGRAPH("p"),
    ANCHOR("a"),
    LINK("link"),
    SELECT("select"),
    OPTION("option"),
    MAT_SELECT("mat-select"),
    MAT_OPTION("mat-option"),
    MAT_EXPANSION_PANEL("mat-expansion-panel"),
    LABEL("label"),
    FOR_LABEL("for"),
    STRONG("strong"),
    IFRAME("iframe");

    private String value;

    WebElementTagNameEnum(String value) {
        this.value = value;
    }

    public static List<WebElementTagNameEnum> insertableTags() {
        return List.of(INPUT, TEXT_AREA);
    }

    public static List<WebElementTagNameEnum> clickableTags() {
        return List.of(INPUT, BUTTON, MAT_SELECT, MAT_OPTION, MAT_EXPANSION_PANEL, ANCHOR, SELECT, OPTION);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
