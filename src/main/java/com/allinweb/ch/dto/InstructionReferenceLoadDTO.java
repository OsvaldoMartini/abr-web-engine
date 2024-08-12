package com.allinweb.ch.dto;

import lombok.Data;

@Data
public class InstructionReferenceLoadDTO {
    private String referenceType;
    private String value;

    @Override
    public String toString() {
        return "InstructionReferenceLoadDTO{" + "referenceType='"
                + referenceType + '\'' + ", value='"
                + value + '\'' + '}';
    }
}
