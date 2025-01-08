package com.allinweb.ch.component.model;

public class PayloadJson {
    private int id;
    private String name;
    private int instructionId;

    public PayloadJson(int botJobId, String name, int instructionId) {
        this.id = botJobId;
        this.name = name;
        this.instructionId = instructionId;
    }
}
