package com.allinweb.ch.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VariableLoadDTO {
    private Integer id;
    private Integer homeBankingId;
    private Integer botJobId;
    private Integer InstructionId;
    private String type;
    private String name;
    private String value;
    private String localFormat;
    private String delimiter;
    private Integer usedVars;
}
