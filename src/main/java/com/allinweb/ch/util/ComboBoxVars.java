package com.allinweb.ch.util;

// Helper class to hold text and varID
public class ComboBoxVars {
    private final String text;
    private final String value;
    private final Integer varId;
    private final Integer extraId;
    private final String tagType;

    public ComboBoxVars(String text, String value, Integer varId, Integer extraId, String tagType) {
        this.text = text;
        this.value = value;
        this.varId = varId;
        this.extraId = extraId;
        this.tagType = tagType;
    }

    public String getText() {
        return text;
    }

    public String getValue() {
        return value;
    }

    public Integer getVarId() {
        return varId;
    }

    public Integer getExtraId() {
        return extraId;
    }

    public String getTagType() {
        return tagType;
    }
}
