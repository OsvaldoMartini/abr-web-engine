package com.allinweb.ch.persistence;

import javax.persistence.*;
import lombok.Data;

@Entity
@Table(name = "variable")
@Data
public class VariableDTO extends BaseDTO {
    @Column(name = "type")
    private String type;

    @Column(name = "name")
    private String name;

    @Column(name = "value", columnDefinition = "TEXT")
    private String value;

    @Column(name = "instruction_id")
    private Integer instructionId;

    @Column(name = "bot_job_id")
    private Integer botJobId;
}
