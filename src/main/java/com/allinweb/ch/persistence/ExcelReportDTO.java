package com.allinweb.ch.persistence;

import java.time.LocalDateTime;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "job_run_report")
@SequenceGenerator(initialValue = 1, name = "idgen", sequenceName = "excelReportSeq", allocationSize = 1)
public class ExcelReportDTO extends BaseDTO {

    @Column(name = "TR_BR_ID")
    private int batchJobId;

    @Column(name = "TR_START_TIME")
    private LocalDateTime startDate;

    @Column(name = "TR_RUN_TIME")
    private long duration;

    @Column(name = "TR_STATUS")
    private short status;

    @Column(name = "TR_ORDER")
    private short order;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "bot_job_id")
    private BotJobDTO botJobDTO;

    public ExcelReportDTO() {}

    public BotJobDTO getBotJobDTO() {
        return botJobDTO;
    }

    public void setBotJobDTO(BotJobDTO botJobDTO) {
        this.botJobDTO = botJobDTO;
    }

    public int getBatchJobId() {
        return batchJobId;
    }

    public void setBatchJobId(int batchJobId) {
        this.batchJobId = batchJobId;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public short getStatus() {
        return status;
    }

    public void setStatus(short status) {
        this.status = status;
    }

    public short getOrder() {
        return order;
    }

    public void setOrder(short order) {
        this.order = order;
    }

    public BotJobDTO getBotJob() {
        return botJobDTO;
    }
}
