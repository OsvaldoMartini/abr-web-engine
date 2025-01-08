package com.allinweb.ch.component.model;

import lombok.Data;

@Data
public class InstructionReferenceLoadDTO {
    private Integer id;
    private String referenceType;
    private String value;
    private Integer blockLoopInstructionId;
    private Integer botJobId;

    private BlockLoopInstructionLoadDTO blockLoopInstructionLoadDTO;

    @Override
    public String toString() {
        return "InstructionReferenceLoadDTO{" + "referenceType='"
                + referenceType + '\'' + ", value='"
                + value + '\'' + '}';
    }
}
