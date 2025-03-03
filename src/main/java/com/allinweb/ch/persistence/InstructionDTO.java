package com.allinweb.ch.persistence;

import javax.persistence.*;
import lombok.Data;

@Entity
@Table(name = "instruction")
@Data
public class InstructionDTO extends BaseDTO {

    @Column(name = "bot_job_id")
    private Integer botJobId;

    @Column(name = "instruction_order_number")
    private int instructionOrderNumber;

    @Column(name = "actions", columnDefinition = "TEXT")
    private String actions;

    @Column(name = "name")
    private String name;

    @Column(name = "xpath", columnDefinition = "TEXT")
    private String xpath;

    @Column(name = "coordinates", length = 100)
    private String coordinates;

    @Column(name = "force_coordinates")
    private Boolean forceCoordinates;

    @Column(name = "iframe_xpath", columnDefinition = "TEXT")
    private String iFrameXPath;

    @Column(name = "description")
    private String description;

    @Column(name = "operation", length = 1000)
    private String operation;

    @Column(name = "optional")
    private Boolean optional;

    @Column(name = "block_marked")
    private Boolean blockMarked;

    @Column(name = "default_value")
    private String defaultValue;

    @Column(name = "action_custom_max_wait_sec")
    private Integer actionCustomMaxWaitSec;

    @Column(name = "on_hold_seconds")
    private Integer onHoldSeconds;

    @Column(name = "codified")
    private Boolean codified;

    @Column(name = "export_to_abr")
    private Boolean exportToABR;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "block_id")
    private Integer blockId;

    @Transient
    private Boolean executed;

    @Transient
    private String priority;

    @Column(name = "variable_id")
    private Integer variableId;

    @Column(name = "parent_id")
    private Integer parentId;
}
