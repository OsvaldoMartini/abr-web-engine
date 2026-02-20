package com.allinweb.ch.model;

import lombok.Data;

@Data
public class ParentOperations {
    private Integer id;
    private String name;
    private String actions;
    private String operations;
    private Integer instructionId;
    private Integer parentId;
    private String parentName;
    private String tagName;
}
