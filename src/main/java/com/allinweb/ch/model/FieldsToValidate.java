package com.allinweb.ch.model;

import lombok.Data;

@Data
public class FieldsToValidate {
    private String value;
    private Double confidence;

    public FieldsToValidate() {}

    public FieldsToValidate(String value, Double confidence) {
        this.value = value;
        this.confidence = confidence;
    }
}
