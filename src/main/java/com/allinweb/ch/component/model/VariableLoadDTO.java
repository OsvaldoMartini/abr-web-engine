package com.allinweb.ch.component.model;

import lombok.Data;

@Data
public class VariableLoadDTO {
    private Integer id;
    private String type;
    private String name;
    private String value;
    private Integer InstructionId;
    private Integer botJobId;
}
