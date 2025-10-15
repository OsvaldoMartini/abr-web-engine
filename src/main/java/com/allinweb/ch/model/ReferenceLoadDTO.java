package com.allinweb.ch.model;

import lombok.Data;

@Data
public class ReferenceLoadDTO {
    private Integer id;
    private Integer homeBankingId;
    private Integer botJobId;
    private String referenceType;
    private String value;
    private Integer instructionId;
}
