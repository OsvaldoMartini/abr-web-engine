package com.allinweb.ch.model;

import java.util.*;

public class CsvTable {

    private final String fileName;
    private final String fullPath;
    private final String delimiter;

    // unique + preserves insertion order
    private final LinkedHashSet<String> columns = new LinkedHashSet<>();

    private final List<CsvRow> rows = new ArrayList<>();

    public CsvTable(String fileName, String fullPath, String delimiter) {
        this.fileName = Objects.requireNonNull(fileName, "fileName");
        this.fullPath = Objects.requireNonNull(fullPath, "fullPath");
        this.delimiter = Objects.requireNonNull(delimiter, "delimiter");
    }

    public String getFileName() {
        return fileName;
    }

    public String getFullPath() {
        return fullPath;
    }

    public String getDelimiter() {
        return delimiter;
    }

    /** Snapshot list in insertion order */
    public List<String> getColumns() {
        return new ArrayList<>(columns);
    }

    public List<CsvRow> getRows() {
        return rows;
    }

    /** Add many columns (deduped, keeps first-seen order) */
    public void addColumns(Collection<String> newColumns) {
        if (newColumns == null) return;
        for (String c : newColumns) {
            String col = normalizeColumnNullable(c);
            if (col != null) columns.add(col);
        }
    }

    private CsvRow ensureRow(int rowIndex) {
        if (rowIndex < 0) {
            throw new IllegalArgumentException("rowIndex must be >= 0");
        }
        while (rows.size() <= rowIndex) {
            rows.add(new CsvRow());
        }
        return rows.get(rowIndex);
    }

    /** Writes value, and grows columns if needed */
    public void put(int rowIndex, String column, String value) {
        String col = normalizeColumnRequired(column);

        // grow columns dynamically
        columns.add(col);

        CsvRow row = ensureRow(rowIndex);
        row.set(col, value);
    }

    public String get(int rowIndex, String column) {
        if (rowIndex < 0 || rowIndex >= rows.size()) return "";
        String col = normalizeColumnRequired(column);
        return rows.get(rowIndex).get(col);
    }

    private static String normalizeColumnRequired(String column) {
        if (column == null) throw new IllegalArgumentException("column must not be null");
        String col = column.trim();
        if (col.isEmpty()) throw new IllegalArgumentException("column must not be blank");
        return col;
    }

    /** returns null if blank/null */
    private static String normalizeColumnNullable(String column) {
        if (column == null) return null;
        String col = column.trim();
        return col.isEmpty() ? null : col;
    }
}
