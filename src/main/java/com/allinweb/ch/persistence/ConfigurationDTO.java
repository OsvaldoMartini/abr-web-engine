package com.allinweb.ch.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "configuration")
@Data
public class ConfigurationDTO extends BaseDTO {

    @Column(name = "pathJava")
    private String pathJava;

    @Column(name = "logLevel")
    private String logLevel;

    @Column(name = "pathDB")
    private String pathDB;

    @Column(name = "interactionTimeoutSec")
    private String interactionTimeoutSec;

    @Column(name = "pathLog")
    private String pathLog;

    @Column(name = "defaultInstructionStopSeconds")
    private String defaultInstructionStopSeconds;

    @Column(name = "pathReport")
    private String pathReport;

    @Column(name = "browser")
    private String browser;

    @Column(name = "dataBaseType")
    private String dataBaseType;

    @Column(name = "pageUpdateTimeoutSec")
    private String pageUpdateTimeoutSec;

    @Column(name = "pathPriority")
    private String pathPriority;

    @Column(name = "pathEngine")
    private String pathEngine;

    @Column(name = "pathExcel")
    private String pathExcel;

    @Column(name = "pathExport")
    private String pathExport;

    @Column(name = "socketPort")
    private String socketPort;

    @Column(name = "blockLimit")
    private String blockLimit;

    @Column(name = "pathJavaFx")
    private String pathJavaFx;
}
