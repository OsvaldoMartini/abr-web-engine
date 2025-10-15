package com.allinweb.ch.model;

import java.util.List;

public class BankingDTO {

    private Integer id;
    private String name;
    private int totalJobs;
    private String url;
    private String priority;
    private List<JobDTO> botJobs;

    public BankingDTO(Integer id, String name, String url, String priority, Integer totalJobs, List<JobDTO> botJobs) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.totalJobs = totalJobs;
        this.priority = priority;
        this.botJobs = botJobs;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getTotalJobs() {
        return totalJobs;
    }

    public void setTotalJobs(int totalJobs) {
        this.totalJobs = totalJobs;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public List<JobDTO> getBotJobs() {
        return botJobs;
    }

    public void setBotJobs(List<JobDTO> botJobs) {
        this.botJobs = botJobs;
    }
}
