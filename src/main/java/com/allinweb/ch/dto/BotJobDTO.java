package com.allinweb.ch.dto;

import java.util.List;
import javax.persistence.*;

@Entity
@Table(name = "bot_job")
@SequenceGenerator(initialValue = 1, name = "idgen", sequenceName = "botJobSeq", allocationSize = 1)
public class BotJobDTO extends BaseDTO {

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "priority")
    private String priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_banking_id")
    private HomeBankingDTO homeBankingDTO;

    @OneToMany(cascade = CascadeType.ALL)
    @OrderBy("block_order_number ASC")
    @JoinColumn(name = "bot_job_id")
    private List<BlockDTO> blockDTOS;

    @OneToMany(cascade = CascadeType.ALL)
    @OrderBy("order ASC")
    @JoinColumn(name = "TR_TS_ID")
    private List<ExcelReportDTO> excelReportDTOS;

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

    public List<ExcelReportDTO> getExcelReports() {
        return excelReportDTOS;
    }

    public void setExcelReports(List<ExcelReportDTO> excelReportDTOS) {
        this.excelReportDTOS = excelReportDTOS;
    }
}
