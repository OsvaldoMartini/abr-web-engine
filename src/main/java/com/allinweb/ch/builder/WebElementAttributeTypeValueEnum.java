package com.allinweb.ch.builder;

import java.util.ArrayList;
import java.util.List;

public enum WebElementAttributeTypeValueEnum {
    SUBMIT("submit"),
    BUTTON("button"),
    CHECKBOX("checkbox"),
    RADIO("radio");

    String value;

    WebElementAttributeTypeValueEnum(String value) {
        this.value = value;
    }

    public static List<WebElementAttributeTypeValueEnum> getClickableValues() {
        List<WebElementAttributeTypeValueEnum> clickableList = new ArrayList<>();
        clickableList.add(WebElementAttributeTypeValueEnum.SUBMIT);
        clickableList.add(WebElementAttributeTypeValueEnum.BUTTON);
        clickableList.add(WebElementAttributeTypeValueEnum.CHECKBOX);
        clickableList.add(WebElementAttributeTypeValueEnum.RADIO);
        return clickableList;
    }

    public String getValue() {
        return value;
    }
}
