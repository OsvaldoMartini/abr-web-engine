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
}
