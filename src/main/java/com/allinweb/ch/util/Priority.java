package com.allinweb.ch.util;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Priority {
    private List<String> name;
    private String priorityType;
    private int priorityNumber;

    public Priority() {}

    public Priority(int priorityNumber, String priorityType, List<String> name) {
        this.name = name;
        this.priorityType = priorityType;
        this.priorityNumber = priorityNumber;
    }

    public List<String> getName() {
        return name;
    }

    public void setName(List<String> name) {
        this.name = name;
    }

    public int getPriorityNumber() {
        return priorityNumber;
    }

    public void setPriorityNumber(int priorityNumber) {
        this.priorityNumber = priorityNumber;
    }

    public PriorityTypeEnum getPriorityType() {
        return PriorityTypeEnum.getPriorityType(priorityType);
    }

    public void setPriorityType(String priorityType) {
        this.priorityType = priorityType;
    }
}
