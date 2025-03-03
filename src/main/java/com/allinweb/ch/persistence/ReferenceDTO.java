package com.allinweb.ch.persistence;

import javax.persistence.*;

@Entity
@Table(name = "reference")
public class ReferenceDTO extends BaseDTO {
    @Column(name = "reference_type")
    private String referenceType;

    @Column(name = "value", columnDefinition = "TEXT")
    private String value;

    @Column(name = "instruction_id", nullable = false) // Adds non-null constraint on the foreign key
    private Integer instructionId;

    @Column(name = "bot_job_id")
    private Integer botJobId;
}
