package com.allinweb.ch.supportTypes;

import java.util.*;

public class ExtractedData {
    private Map<String, Map<Integer, String>> extractedData = new HashMap<>();

    private String errorMessage;

    public ExtractedData() {}

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void addField(String fieldName) {
        if (!containsField(fieldName)) {
            extractedData.put(fieldName, new HashMap<>());
        }
    }

    public boolean containsField(String fieldName) {
        return extractedData.containsKey(fieldName);
    }

    public void addFieldValue(String fieldName, String value, Integer row) {
        extractedData.get(fieldName).put(row, value);
    }

    public String getFieldValue(String fieldName, Integer row) {
        return extractedData.get(fieldName).get(row);
    }

    public Map<String, String> getRowFieldValues(Integer row) {
        Map<String, String> map = new HashMap<>();
        for (String key : extractedData.keySet()) {
            String value = extractedData.get(key).get(row);
            map.put(key, value);
        }
        return map;
    }

    public Integer getNumberOfDataRows() {
        int max = 0;
        for (String fieldName : extractedData.keySet()) {
            int size = extractedData.get(fieldName).size();
            if (size > max) {
                max = size;
            }
        }
        return max;
    }
}
