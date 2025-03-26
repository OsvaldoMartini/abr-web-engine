package com.allinweb.ch.persistence;

import java.util.*;
import javax.persistence.*;
import lombok.Data;

@Entity
@Table(name = "component_reference")
@Data
public class ComponentReferenceDTO extends BaseDTO {
    @Column(name = "reference_type")
    private String referenceType;

    @Column(name = "value", columnDefinition = "TEXT")
    private String value;

    @Column(name = "instruction_id", nullable = false) // Adds non-null constraint on the foreign key
    private Integer instructionId;

    @Column(name = "home_banking_id")
    private Integer homeBankingId;
}
