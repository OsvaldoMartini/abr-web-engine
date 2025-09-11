package com.allinweb.ch.component.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseUserDTO {
    private String id;
    private String jobs = "0"; // default value
    private String name;
    private String url;
    private String priority = "";
    private String searchConfig = "";
    private String optionsConfig = "";
    private String username = "";
    private String password = "";

    /**
     * Shorter constructor (subset of fields)
     */
    public DatabaseUserDTO(
            String id, String name, String url, String priority, String searchConfig, String optionsConfig) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.priority = priority;
        this.searchConfig = searchConfig;
        this.optionsConfig = optionsConfig;
    }
}
