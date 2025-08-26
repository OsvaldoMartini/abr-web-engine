package com.allinweb.ch.component.model;

public class PayloadJson {
    private Integer id;
    private Integer blockId;
    private String name;
    private Integer instructionId;

    public PayloadJson(Integer botJobId, Integer blockId, String name, Integer instructionId) {
        this.id = botJobId;
        this.blockId = blockId;
        this.name = name;
        this.instructionId = instructionId;
    }
}
