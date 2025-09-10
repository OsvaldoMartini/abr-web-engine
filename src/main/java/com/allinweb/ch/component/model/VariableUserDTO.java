package com.allinweb.ch.component.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VariableUserDTO {

    private Integer id;
    private String type;
    private String name;
    private String value;
    private Integer botJobId;
    private Integer parentId;
    private String parentName;
    private String localFormat = "";
    private String delimiter = "";
    private String usedVars = "";

    // Optional: you can add a partial constructor for the shorter version
    public VariableUserDTO(
            Integer id, String type, String name, String value, Integer botJobId, Integer parentId, String parentName) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.value = value;
        this.botJobId = botJobId;
        this.parentId = parentId;
        this.parentName = parentName;
    }
}
