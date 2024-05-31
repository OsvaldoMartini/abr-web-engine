package com.allinweb.ch.dto;

import java.util.List;
import javax.persistence.*;

/**
 * actions syntax: A -> type of action (in this case 'A' is a placeholder)
 *                  if A == I -> insert -> I:X
 *                                           X -> name of field
 */
@Entity
@Table(name = "block_loop_instruction")
@SequenceGenerator(initialValue = 1, name = "idgen", sequenceName = "blockLoopInstructionSeq", allocationSize = 1)
public class BlockLoopInstructionDTO extends BaseDTO {

    @Column(name = "instruction_order_number")
    private int instructionOrderNumber;

    @Column(name = "actions")
    private String actions;

    @Column(name = "name")
    private String name;

    @Column(name = "path", length = 1000)
    private String path;

    @Column(name = "description")
    private String description;

    @Column(name = "optional")
    private int optional;

    @Column(name = "default_val")
    private String default_val;

    @Column(name = "action_custom_max_wait_sec")
    private Integer actionCustomMaxWaitSec;

    @Column(name = "on_hold_seconds")
    private Integer onHoldSeconds;

    @Column(name = "encrypted")
    private Integer encrypted;

    @Column(name = "export_to_abr")
    private Integer exportToABR;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id")
    private BlockDTO blockDTO;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @OrderBy("orderNumber ASC")
    @JoinColumn(name = "block_loop_instruction_id")
    private List<ComplexInstructionDTO> complexInstructionDTOList;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "block_loop_instruction_id")
    private List<InstructionReferenceDTO> instructionReferenceDTOList;

    public BlockLoopInstructionDTO() {
        super();
    }

    public BlockLoopInstructionDTO(int id) {
        super(id);
    }

    public BlockLoopInstructionDTO(BlockDTO blockDTO) {
        super();
        this.blockDTO = blockDTO;
    }

    public int getInstructionOrderNumber() {
        return instructionOrderNumber;
    }

    public void setInstructionOrderNumber(int instructionOrderNumber) {
        this.instructionOrderNumber = instructionOrderNumber;
    }

    public Integer getOnHoldSeconds() {
        return onHoldSeconds;
    }

    public void setOnHoldSeconds(Integer onHoldSeconds) {
        this.onHoldSeconds = onHoldSeconds;
    }

    public String getActions() {
        return actions;
    }

    public void setActions(String actions) {
        this.actions = actions;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BlockDTO getBlock() {
        return blockDTO;
    }

    public void setBlock(BlockDTO blockDTO) {
        this.blockDTO = blockDTO;
    }

    public String getDefaultValue() {
        return default_val;
    }

    public void setDefaultValue(String default_val) {
        this.default_val = default_val;
    }

    public Integer getActionCustomMaxWaitSec() {
        return actionCustomMaxWaitSec;
    }

    public void setActionCustomMaxWaitSec(Integer actionCustomMaxWaitSec) {
        this.actionCustomMaxWaitSec = actionCustomMaxWaitSec;
    }

    public boolean isOptional() {
        return optional >= 1;
    }

    public void setOptional(boolean optional) {
        this.optional = optional ? 1 : 0;
    }

    public boolean isEncrypted() {
        return encrypted >= 1;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted ? 1 : 0;
    }

    public boolean getExportToABR() {
        return exportToABR >= 1;
    }

    public void setExportToABR(boolean exportToABR) {
        this.exportToABR = exportToABR ? 1 : 0;
    }

    public List<ComplexInstructionDTO> getComplexInstrucions() {
        return complexInstructionDTOList;
    }

    public void setComplexInstrucions(List<ComplexInstructionDTO> complexInstructionDTOList) {
        this.complexInstructionDTOList = complexInstructionDTOList;
    }

    public List<InstructionReferenceDTO> getInstructionReferenceDTOList() {
        return instructionReferenceDTOList;
    }

    public void setInstructionReferenceDTOList(List<InstructionReferenceDTO> instructionReferenceDTOList) {
        this.instructionReferenceDTOList = instructionReferenceDTOList;
    }
}
