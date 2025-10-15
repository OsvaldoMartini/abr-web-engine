package com.allinweb.ch.model;

import lombok.Data;

@Data
public class FieldData {
    private final String key;
    private final String value;

    public FieldData(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
