package com.allinweb.ch.persistence;

import javax.persistence.*;

@Entity
@Table(name = "complex_instruction")
// @SequenceGenerator(initialValue = 1, name = "idgen", sequenceName = "complexInstructionSeq", allocationSize = 1)
public class ComplexInstructionDTO extends BaseDTO {

    @ManyToOne
    @JoinColumn(name = "instruction_id")
    private InstructionDTO instructionDTO;

    @Column(name = "order_number")
    private int orderNumber;

    @Column(name = "instruction")
    private String instruction;

    @Column(name = "way")
    private String way;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "bot_job_id")
    private BotJobDTO botJobDTO;

    public BotJobDTO getBotJobDTO() {
        return botJobDTO;
    }

    public void setBotJobDTO(BotJobDTO botJobDTO) {
        this.botJobDTO = botJobDTO;
    }

    public ComplexInstructionDTO() {
        super();
    }

    public InstructionDTO getBlockLoopInstructionDTO() {
        return instructionDTO;
    }

    public void setBlockLoopInstructionDTO(InstructionDTO instructionDTO) {
        this.instructionDTO = instructionDTO;
    }

    public int getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(int orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }
}
