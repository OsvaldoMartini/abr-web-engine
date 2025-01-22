package com.allinweb.ch.persistence;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
@Table(name = "bot_job")
// @SequenceGenerator(initialValue = 1, name = "idgen", sequenceName = "botJobSeq", allocationSize = 1)
public class BotJobDTO extends BaseDTO implements Serializable {

    @Column(name = "name", unique = true)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "priority")
    private String priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_banking_id")
    private HomeBankingDTO homeBankingDTO;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @OrderBy("block_order_number ASC")
    @JoinColumn(name = "bot_job_id")
    @Fetch(FetchMode.SUBSELECT)
    private List<BlockDTO> blockDTOS = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "bot_job_id")
    @Fetch(FetchMode.SUBSELECT)
    private List<VariableDTO> variableDTOS = new ArrayList<>();

    public BotJobDTO() {
        super();
    }

    public BotJobDTO(HomeBankingDTO homeBankingDTO) {
        super();
        this.homeBankingDTO = homeBankingDTO;
    }

    public BotJobDTO(int id) {
        super(id);
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

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public HomeBankingDTO getHomeBanking() {
        return homeBankingDTO;
    }

    public void setHomeBanking(HomeBankingDTO homeBankingDTO) {
        this.homeBankingDTO = homeBankingDTO;
    }

    public List<BlockDTO> getBlocks() {
        return blockDTOS;
    }

    public void setBlocks(List<BlockDTO> blockDTOS) {
        this.blockDTOS = blockDTOS;
    }

    public HomeBankingDTO getHomeBankingDTO() {
        return homeBankingDTO;
    }

    public void setHomeBankingDTO(HomeBankingDTO homeBankingDTO) {
        this.homeBankingDTO = homeBankingDTO;
    }

    public List<BlockDTO> getBlockDTOS() {
        return blockDTOS;
    }

    public void setBlockDTOS(List<BlockDTO> blockDTOS) {
        this.blockDTOS = blockDTOS;
    }

    public List<VariableDTO> getVariableDTOS() {
        return variableDTOS;
    }

    public void setVariableDTOS(List<VariableDTO> variableDTOS) {
        this.variableDTOS = variableDTOS;
    }
}
