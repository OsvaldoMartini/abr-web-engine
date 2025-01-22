package com.allinweb.ch.persistence;

public class ConfigUserDTO {
    private String id;
    private String pathJava;
    private String logLevel;
    private String pathDB;
    private String interactionTimeoutSec;
    private String pathLog;
    private String defaultInstructionStopSeconds;
    private String pathReport;
    private String browser;
    private String pageUpdateTimeoutSec;
    private String pathPriority;
    private String pathEngine;
    private String pathExcel;
    private String pathJavaFx;

    public ConfigUserDTO(
            String id,
            String pathJava,
            String logLevel,
            String pathDB,
            String interactionTimeoutSec,
            String pathLog,
            String defaultInstructionStopSeconds,
            String pathReport,
            String browser,
            String pageUpdateTimeoutSec,
            String pathPriority,
            String pathEngine,
            String pathExcel,
            String pathJavaFx) {
        this.id = id;
        this.pathJava = pathJava;
        this.logLevel = logLevel;
        this.pathDB = pathDB;
        this.interactionTimeoutSec = interactionTimeoutSec;
        this.pathLog = pathLog;
        this.defaultInstructionStopSeconds = defaultInstructionStopSeconds;
        this.pathReport = pathReport;
        this.browser = browser;
        this.pageUpdateTimeoutSec = pageUpdateTimeoutSec;
        this.pathPriority = pathPriority;
        this.pathEngine = pathEngine;
        this.pathExcel = pathExcel;
        this.pathJavaFx = pathJavaFx;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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
