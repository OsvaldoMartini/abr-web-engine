package com.allinweb.ch.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class CsvRow {
    private Map<String, String> values = new LinkedHashMap<>();

    public void set(String column, String value) {
        values.put(column, value);
    }

    public String get(String column) {
        return values.getOrDefault(column, "");
    }

    public Map<String, String> getValues() {
        return values;
    }
}
