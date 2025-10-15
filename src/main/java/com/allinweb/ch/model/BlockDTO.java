package com.allinweb.ch.model;

public class BlockDTO {
    private int blockOrderNumber;
    private String name;
    private String description;
    private Integer typeId;
    private boolean active;
    private int defaultWait;

    public BlockDTO(int blockOrderNumber, String name, String description, Integer typeId) {
        this.blockOrderNumber = blockOrderNumber;
        this.name = name;
        this.description = description;
        this.typeId = typeId;
    }

    public int getBlockOrderNumber() {
        return blockOrderNumber;
    }

    public void setBlockOrderNumber(int blockOrderNumber) {
        this.blockOrderNumber = blockOrderNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getTypeId() {
        return typeId;
    }

    public void setTypeId(Integer typeId) {
        this.typeId = typeId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getDefaultWait() {
        return defaultWait;
    }

    public void setDefaultWait(int defaultWait) {
        this.defaultWait = defaultWait;
    }
}
