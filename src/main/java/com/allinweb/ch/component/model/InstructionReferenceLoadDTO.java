package com.allinweb.ch.component.model;

import lombok.Data;

@Data
public class InstructionReferenceLoadDTO {
    private Integer id;
    private Integer homeBankingId;
    private Integer botJobId;
    private String referenceType;
    private String value;
    private Integer blockLoopInstructionId;

    private InstructionLoadDTO instructionLoadDTO;
}
