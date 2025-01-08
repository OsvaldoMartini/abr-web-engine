package com.allinweb.ch.util;

// Helper class to hold text and varID
public class ComboBoxVars {
    private final String text;
    private final String value;
    private final Integer varId;
    private final Integer instructionId;

    public ComboBoxVars(String text, String value, Integer varId, Integer instructionId) {
        this.text = text;
        this.value = value;
        this.varId = varId;
        this.instructionId = instructionId;
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

    public Integer getInstructionId() {
        return instructionId;
    }
}
