package com.allinweb.ch.persistence;

import java.util.*;
import javax.persistence.*;

@Entity
@Table(name = "block")
// @SequenceGenerator(initialValue = 1, name = "idgen", sequenceName = "blockSeq", allocationSize = 1)
public class BlockDTO extends BaseDTO {

    @Column(name = "block_order_number")
    private int blockOrderNumber;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "type_id")
    private Integer typeId;

    @Column(name = "export_file")
    private String exportFile;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "wait")
    private Integer wait;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bot_job_id")
    private BotJobDTO botJobDTO;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @OrderBy("instruction_order_number ASC")
    @JoinColumn(name = "block_id")
    private List<BlockLoopInstructionDTO> blockLoopInstructionDTOS = new ArrayList<>();

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

    public String getExportFile() {
        return exportFile;
    }

    public void setExportFile(String exportFile) {
        this.exportFile = exportFile;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Integer getWait() {
        return wait;
    }

    public void setWait(Integer wait) {
        this.wait = wait;
    }

    public BotJobDTO getBotJobDTO() {
        return botJobDTO;
    }

    public void setBotJob(BotJobDTO botJobDTO) {
        this.botJobDTO = botJobDTO;
    }

    public void setBotJobDTO(BotJobDTO botJobDTO) {
        this.botJobDTO = botJobDTO;
    }

    public List<BlockLoopInstructionDTO> getBlockLoopInstructionDTOS() {
        return blockLoopInstructionDTOS;
    }

    public void setBlockLoopInstructionDTOS(List<BlockLoopInstructionDTO> blockLoopInstructionDTOS) {
        this.blockLoopInstructionDTOS = blockLoopInstructionDTOS;
    }
}
