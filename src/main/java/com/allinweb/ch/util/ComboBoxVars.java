package com.allinweb.ch.util;

import lombok.Getter;

// Helper class to hold text and varID
@Getter
public class ComboBoxVars {
    private final String text;
    private final String value;
    private final Integer instructionId;
    private final Integer blockId;
    private final Integer parentId;
    private final Integer varId;
    private final String tagType;
    private final Integer orderNumber;
    private final String localFormat;

    public ComboBoxVars(
            String text,
            String value,
            Integer instructionId,
            Integer blockId,
            Integer parentId,
            Integer varId,
            String tagType,
            Integer orderNumber,
            String localFormat) {
        this.text = text;
        this.value = value;
        this.instructionId = instructionId;
        this.blockId = blockId;
        this.parentId = parentId;
        this.varId = varId;
        this.tagType = tagType;
        this.orderNumber = orderNumber;
        this.localFormat = localFormat;
    }
}
