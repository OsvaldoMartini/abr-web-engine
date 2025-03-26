package com.allinweb.ch.persistence;

import javax.persistence.*;
import lombok.Data;

@Entity
@Table(name = "component_variable")
@Data
public class ComponentVariableDTO extends BaseDTO {
    @Column(name = "type")
    private String type;

    @Column(name = "name")
    private String name;

    @Column(name = "value", columnDefinition = "TEXT")
    private String value;

    @Column(name = "instruction_id")
    private Integer instructionId;

    @Column(name = "home_banking_id")
    private Integer homeBankingId;
}
