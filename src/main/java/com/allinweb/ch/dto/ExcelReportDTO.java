package com.allinweb.ch.dto;

import java.time.LocalDateTime;
import javax.persistence.*;

@Entity
@Table(name = "job_run_report")
public class ExcelReportDTO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TR_ID")
    private long id;

    @Column(name = "TR_BR_ID")
    private int batchJobId; // id del batch se fa parte di un run in batch, altrimenti 0 TODO: per il momento 0

    @Column(name = "TR_START_TIME")
    private LocalDateTime startDate;

    @Column(name = "TR_RUN_TIME")
    private long duration;

    @Column(name = "TR_STATUS")
    private short status;

    @Column(name = "TR_ORDER")
    private short order; // ordine di esecuzione TODO: per il momento 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TR_TS_ID")
    private BotJobDTO botJobDTO;

    public ExcelReportDTO() {}

    public ExcelReportDTO(BotJobDTO botJobDTO) {
        this.botJobDTO = botJobDTO;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public BotJobDTO getBotJobDTO() {
        return botJobDTO;
    }

    public void setBotJobDTO(BotJobDTO botJobDTO) {
        this.botJobDTO = botJobDTO;
    }
}
