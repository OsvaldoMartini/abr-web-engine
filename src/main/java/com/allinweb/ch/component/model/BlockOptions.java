package com.allinweb.ch.component.model;

import lombok.Getter;

@Getter
public class BlockOptions {
    private final String text;
    private final String value;
    private final Integer instructionId;
    private final Integer blockId;

    public BlockOptions(String text, String value, Integer instructionId, Integer blockId) {
        this.text = text;
        this.value = value;
        this.instructionId = instructionId;
        this.blockId = blockId;
    }
}
