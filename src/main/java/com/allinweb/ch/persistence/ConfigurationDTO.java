package com.allinweb.ch.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "configuration")
@SequenceGenerator(initialValue = 1, name = "idgen", sequenceName = "configurationSeq", allocationSize = 1)
public class ConfigurationDTO extends BaseDTO {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "idgen")
    @Column(name = "id")
    private int id;

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

    @Column(name = "pageUpdateTimeoutSec")
    private String pageUpdateTimeoutSec;

    @Column(name = "pathPriority")
    private String pathPriority;

    @Column(name = "pathEngine")
    private String pathEngine;

    @Column(name = "pathExcel")
    private String pathExcel;

    @Column(name = "pathJavaFx")
    private String pathJavaFx;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public String getPathJava() {
        return pathJava;
    }

    public void setPathJava(String pathJava) {
        this.pathJava = pathJava;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public String getPathDB() {
        return pathDB;
    }

    public void setPathDB(String pathDB) {
        this.pathDB = pathDB;
    }

    public String getInteractionTimeoutSec() {
        return interactionTimeoutSec;
    }

    public void setInteractionTimeoutSec(String interactionTimeoutSec) {
        this.interactionTimeoutSec = interactionTimeoutSec;
    }

    public String getPathLog() {
        return pathLog;
    }

    public void setPathLog(String pathLog) {
        this.pathLog = pathLog;
    }

    public String getDefaultInstructionStopSeconds() {
        return defaultInstructionStopSeconds;
    }

    public void setDefaultInstructionStopSeconds(String defaultInstructionStopSeconds) {
        this.defaultInstructionStopSeconds = defaultInstructionStopSeconds;
    }

    public String getPathReport() {
        return pathReport;
    }

    public void setPathReport(String pathReport) {
        this.pathReport = pathReport;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getPageUpdateTimeoutSec() {
        return pageUpdateTimeoutSec;
    }

    public void setPageUpdateTimeoutSec(String pageUpdateTimeoutSec) {
        this.pageUpdateTimeoutSec = pageUpdateTimeoutSec;
    }

    public String getPathPriority() {
        return pathPriority;
    }

    public void setPathPriority(String pathPriority) {
        this.pathPriority = pathPriority;
    }

    public String getPathEngine() {
        return pathEngine;
    }

    public void setPathEngine(String pathEngine) {
        this.pathEngine = pathEngine;
    }

    public String getPathExcel() {
        return pathExcel;
    }

    public void setPathExcel(String pathExcel) {
        this.pathExcel = pathExcel;
    }

    public String getPathJavaFx() {
        return pathJavaFx;
    }

    public void setPathJavaFx(String pathJavaFx) {
        this.pathJavaFx = pathJavaFx;
    }
}
