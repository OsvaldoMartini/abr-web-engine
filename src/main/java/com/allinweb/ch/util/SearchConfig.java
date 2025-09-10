package com.allinweb.ch.util;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SearchConfig {
    private List<String> name;
    private String searchType;
    private int searchNumber;

    public SearchConfig() {}

    public SearchConfig(int searchNumber, String searchType, List<String> name) {
        this.name = name;
        this.searchType = searchType;
        this.searchNumber = searchNumber;
    }

    public List<String> getName() {
        return name;
    }

    public void setName(List<String> name) {
        this.name = name;
    }

    public String getSearchType() {
        return searchType;
    }

    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }

    public int getSearchNumber() {
        return searchNumber;
    }

    public void setSearchNumber(int searchNumber) {
        this.searchNumber = searchNumber;
    }
}
