package com.allinweb.ch.dto;

import java.util.List;
import lombok.Data;

@Data
public class BlockLoopInstructionLoadDTO {
    private int id;
    private int instructionOrderNumber;
    private String actions;
    private String name;
    private String path;
    private String description;
    private int optional;
    private boolean blockMarked;
    private String default_val;
    private Integer actionCustomMaxWaitSec;
    private Integer onHoldSeconds;
    private Integer encrypted;
    private Integer exportToABR;
    private Boolean executed;
    private String priority;
    private String operation;
    private int parentId;

    public String getDefaultValue() {
        return default_val;
    }

    public void setDefaultValue(String default_val) {
        this.default_val = default_val;
    }

    public boolean isOptional() {
        return optional >= 1;
    }

    public boolean isEncrypted() {
        return encrypted >= 1;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public int getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    private List<ComplexInstructionLoadDTO> complexInstructionLoadDTOList;
    private List<InstructionReferenceLoadDTO> instructionReferenceLoadDTOList;

    @Override
    public String toString() {
        return "BlockLoopInstructionLoadDTO{" + "id="
                + id + ", instructionOrderNumber="
                + instructionOrderNumber + ", actions='"
                + actions + '\'' + ", name='"
                + name + '\'' + ", path='"
                + path + '\'' + ", description='"
                + description + '\'' + ", optional="
                + optional + ", blockMarked="
                + blockMarked + ", default_val='"
                + default_val + '\'' + ", actionCustomMaxWaitSec="
                + actionCustomMaxWaitSec + ", onHoldSeconds="
                + onHoldSeconds + ", encrypted="
                + encrypted + ", exportToABR="
                + exportToABR + ", executed="
                + executed + ", priority='"
                + priority + '\'' + ", complexInstructionLoadDTOList="
                + complexInstructionLoadDTOList + ", instructionReferenceLoadDTOList="
                + instructionReferenceLoadDTOList + '}';
    }
}
