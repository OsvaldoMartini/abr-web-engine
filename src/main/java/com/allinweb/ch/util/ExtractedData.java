package com.allinweb.ch.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * Block scoped Excel data store. Indexed as blockName then fieldName then rowIdx then value
 * so that two INPUT instructions with the same canonical name in different blocks can each
 * own their own column in the Excel file without colliding. Field names are whatever the
 * Excel column header holds, which is clientNamed when set and the canonical instruction
 * name otherwise.
 *
 * <p>The legacy no block API addField, getRowFieldValues and friends is kept because report
 * rendering and the empty data fallback do not carry block context. Those methods operate on
 * a flat view across all blocks where the first non null value wins on duplicate field names.
 */
@Slf4j
public class ExtractedData {
    private static final String NO_BLOCK = ""; // bucket for legacy no block calls

    // blockName then fieldName then rowIdx then value
    private final Map<String, Map<String, Map<Integer, String>>> extractedData = new LinkedHashMap<>();

    private String errorTitle;
    private String errorMessage;
    private String missingFields;

    public ExtractedData() {}

    public String getErrorTitle() {
        return errorTitle;
    }

    public void setErrorTitle(String errorTitle) {
        this.errorTitle = errorTitle;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getMissingFields() {
        return missingFields;
    }

    public void setMissingFields(String missingFields) {
        this.missingFields = missingFields;
    }

    // Block scoped API preferred

    public void addField(String blockName, String fieldName) {
        String b = normalizeBlock(blockName);
        extractedData.computeIfAbsent(b, k -> new LinkedHashMap<>()).computeIfAbsent(fieldName, k -> new HashMap<>());
    }

    public boolean containsField(String blockName, String fieldName) {
        return findField(findBlock(blockName), fieldName) != null;
    }

    public void addFieldValue(String blockName, String fieldName, String value, Integer row) {
        addField(blockName, fieldName);
        extractedData.get(normalizeBlock(blockName)).get(fieldName).put(row, value);
    }

    public String getFieldValue(String blockName, String fieldName, Integer row) {
        Map<Integer, String> field = findField(findBlock(blockName), fieldName);
        return field == null ? null : field.get(row);
    }

    public Map<String, String> getRowFieldValues(String blockName, Integer row) {
        Map<String, String> map = new HashMap<>();
        Map<String, Map<Integer, String>> block = findBlock(blockName);
        if (block == null) return map;
        for (Map.Entry<String, Map<Integer, String>> e : block.entrySet()) {
            map.put(e.getKey(), e.getValue().get(row));
        }
        return map;
    }

    public Set<String> getExtractedFields(String blockName) {
        Map<String, Map<Integer, String>> block = findBlock(blockName);
        return block == null ? Collections.emptySet() : block.keySet();
    }

    /**
     * Locate a stored block by name. Tries exact match first, falls back to
     * trim and case insensitive comparison so the executeJob lookup survives
     * cosmetic differences between {@code blockLoad.getName()} and the block
     * label persisted in the Excel file. For example an extra space the user
     * typed directly into the spreadsheet, or a hash prefix variant.
     */
    private Map<String, Map<Integer, String>> findBlock(String blockName) {
        String key = normalizeBlock(blockName);
        Map<String, Map<Integer, String>> exact = extractedData.get(key);
        if (exact != null) return exact;
        String needle = key.trim().toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Map<String, Map<Integer, String>>> e : extractedData.entrySet()) {
            String stored = e.getKey() == null ? "" : e.getKey();
            if (stored.trim().toLowerCase(Locale.ROOT).equals(needle)) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * Locate a field within a block. Same lenient policy as findBlock so a
     * column header typed with extra whitespace or different casing still
     * resolves. This is what makes the block displayKey lookup robust
     * against the Excel file being hand edited.
     */
    private Map<Integer, String> findField(Map<String, Map<Integer, String>> block, String fieldName) {
        if (block == null || fieldName == null) return null;
        Map<Integer, String> exact = block.get(fieldName);
        if (exact != null) return exact;
        String needle = fieldName.trim().toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Map<Integer, String>> e : block.entrySet()) {
            String stored = e.getKey() == null ? "" : e.getKey();
            if (stored.trim().toLowerCase(Locale.ROOT).equals(needle)) {
                return e.getValue();
            }
        }
        return null;
    }

    /** @return the set of block names that have at least one field, in insertion order. */
    public Set<String> getBlocks() {
        return extractedData.keySet();
    }

    // Legacy no block API delegates to default bucket

    public void addField(String fieldName) {
        addField(NO_BLOCK, fieldName);
    }

    public boolean containsField(String fieldName) {
        return containsField(NO_BLOCK, fieldName);
    }

    public void addFieldValue(String fieldName, String value, Integer row) {
        addFieldValue(NO_BLOCK, fieldName, value, row);
    }

    public String getFieldValue(String fieldName, Integer row) {
        return getFieldValue(NO_BLOCK, fieldName, row);
    }

    // Cross block aggregations

    /**
     * Flat row dump used for reporting and logging. Merges values across all blocks. On
     * duplicate field names, the first non null value wins in insertion order.
     */
    public Map<String, String> getRowFieldValues(Integer row) {
        Map<String, String> map = new HashMap<>();
        for (Map<String, Map<Integer, String>> block : extractedData.values()) {
            for (Map.Entry<String, Map<Integer, String>> e : block.entrySet()) {
                String value = e.getValue().get(row);
                if (!map.containsKey(e.getKey()) || (map.get(e.getKey()) == null && value != null)) {
                    map.put(e.getKey(), value);
                }
            }
        }
        return map;
    }

    public Integer getNumberOfDataRows() {
        int max = 0;
        for (Map<String, Map<Integer, String>> block : extractedData.values()) {
            for (Map<Integer, String> field : block.values()) {
                int size = field.size();
                if (size > max) {
                    max = size;
                }
            }
        }
        return max;
    }

    /** @return all field names across all blocks, deduped. */
    public Set<String> getExtractedFields() {
        Set<String> all = new LinkedHashSet<>();
        for (Map<String, Map<Integer, String>> block : extractedData.values()) {
            all.addAll(block.keySet());
        }
        return all;
    }

    private static String normalizeBlock(String blockName) {
        return blockName == null ? NO_BLOCK : blockName;
    }
}
