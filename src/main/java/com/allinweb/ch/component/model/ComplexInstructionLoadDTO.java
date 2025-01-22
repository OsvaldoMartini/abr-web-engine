package com.allinweb.ch.component.model;

import lombok.Data;

@Data
public class ComplexInstructionLoadDTO {
    private Integer id;
    private Integer instructionId;
    private Integer botJobId;
    private Integer orderNumber;
    private String instruction;
    private String way;
}
