package com.allinweb.ch.persistence;

import javax.persistence.*;

@Entity
@Table(name = "variable")
// @SequenceGenerator(initialValue = 1, name = "idgen", sequenceName = "variableSeq", allocationSize = 1)
public class VariableDTO extends BaseDTO {
    @Column(name = "type")
    private String type;

    @Column(name = "name")
    private String name;

    @Column(name = "value")
    private String value;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instruction_id")
    private InstructionDTO instructionDTO;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "bot_job_id")
    private BotJobDTO botJobDTO;

    public InstructionDTO getBlockLoopInstructionDTO() {
        return instructionDTO;
    }

    public void setBlockLoopInstructionDTO(InstructionDTO instructionDTO) {
        this.instructionDTO = instructionDTO;
    }

    public BotJobDTO getBotJobDTO() {
        return botJobDTO;
    }

    public void setBotJobDTO(BotJobDTO botJobDTO) {
        this.botJobDTO = botJobDTO;
    }

    public VariableDTO() {
        super();
    }

    public VariableDTO(int id) {
        super(id);
    }

    public VariableDTO(BotJobDTO botJobDTO) {
        super();
        this.botJobDTO = botJobDTO;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public BotJobDTO getBotJob() {
        return botJobDTO;
    }

    public void setBotJob(BotJobDTO botJobDTO) {
        this.botJobDTO = botJobDTO;
    }
}
