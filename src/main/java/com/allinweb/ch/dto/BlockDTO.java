package com.allinweb.ch.dto;

import java.util.List;
import javax.persistence.*;

@Entity
@Table(name = "block")
@SequenceGenerator(initialValue = 1, name = "idgen", sequenceName = "blockSeq", allocationSize = 1)
public class BlockDTO extends BaseDTO {

    @Column(name = "block_order_number")
    private int blockOrderNumber;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "type_id")
    private Integer typeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bot_job_id")
    private BotJobDTO botJobDTO;

    @OneToMany(cascade = CascadeType.ALL)
    @OrderBy("instruction_order_number ASC")
    @JoinColumn(name = "block_id")
    private List<BlockLoopInstructionDTO> blockLoopInstructionDTOS;

    public BlockDTO() {
        super();
    }

    public BlockDTO(int id) {
        super(id);
    }

    public BlockDTO(BotJobDTO botJobDTO) {
        super();
        this.botJobDTO = botJobDTO;
    }

    public int getBlockOrderNumber() {
        return blockOrderNumber;
    }

    public void setBlockOrderNumber(int blockOrderNumber) {
        this.blockOrderNumber = blockOrderNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getTypeId() {
        return typeId;
    }

    public void setTypeId(Integer typeId) {
        this.typeId = typeId;
    }

    public BotJobDTO getBotJob() {
        return botJobDTO;
    }

    public void setBotJob(BotJobDTO botJobDTO) {
        this.botJobDTO = botJobDTO;
    }

    public List<BlockLoopInstructionDTO> getBlockLoopInstructions() {
        return blockLoopInstructionDTOS;
    }

    public void setBlockLoopInstructions(List<BlockLoopInstructionDTO> blockLoopInstructionDTOS) {
        this.blockLoopInstructionDTOS = blockLoopInstructionDTOS;
    }
}
