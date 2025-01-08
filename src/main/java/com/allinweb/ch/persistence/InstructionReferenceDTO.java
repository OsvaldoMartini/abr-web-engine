package com.allinweb.ch.persistence;

import javax.persistence.*;

@Entity
@Table(name = "instruction_reference")
@SequenceGenerator(initialValue = 1, name = "idgen", sequenceName = "instructionReferenceSeq", allocationSize = 1)
public class InstructionReferenceDTO extends BaseDTO {

    @Column(name = "reference_type")
    private String referenceType;

    @Column(name = "value", length = 1000)
    private String value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_loop_instruction_id")
    private BlockLoopInstructionDTO blockLoopInstructionDTO;

    public InstructionReferenceDTO() {
        super();
    }

    public InstructionReferenceDTO(int id) {
        super(id);
    }

    public InstructionReferenceDTO(BlockLoopInstructionDTO instruction) {
        blockLoopInstructionDTO = instruction;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public BlockLoopInstructionDTO getBlockLoopInstructionDTO() {
        return blockLoopInstructionDTO;
    }

    public void setBlockLoopInstructionDTO(BlockLoopInstructionDTO blockLoopInstructionDTO) {
        this.blockLoopInstructionDTO = blockLoopInstructionDTO;
    }
}
