package com.allinweb.ch.socket;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Simulates a CSV file with dynamic columns, missing values, and row numbering.
 */
public class CsvSimulatorWithColumnsMap {
    private List<String> columnsCSV; // Column headers
    private List<List<String>> rowsCSV; // Data rows
    private static final String END_OF_FILE_MARKER = "END OF FILE";

    public CsvSimulatorWithColumnsMap() {
        this.columnsCSV = new ArrayList<>();
        this.rowsCSV = new ArrayList<>();
    }

    /**
     * Adds a row with values matching the columns.
     * Missing values are filled with empty strings.
     * @param values Array of values; may be less than columns.
     */
    public void addRow(String... values) {
        if (columnsCSV.isEmpty()) {
            throw new IllegalStateException("Columns must be initialized before adding a row using values.");
        }

        List<String> row = new ArrayList<>();
        int maxCols = columnsCSV.size();

        for (int i = 0; i < maxCols; i++) {
            if (i < values.length) {
                row.add(values[i]);
            } else {
                row.add(""); // fill missing with empty string
            }
        }
        rowsCSV.add(row);
    }

    /**
     * Adds a row using a Map<String, String>. If this is the first row added,
     * it sets the column order based on the map's keys.
     */
    public void addRowFromMap(Map<String, String> map) {
        // Initialize column order on first insert
        if (columnsCSV.isEmpty()) {
            if (map instanceof LinkedHashMap) {
                columnsCSV.addAll(map.keySet()); // preserve order
            } else {
                // Default to alphabetical if insertion order is unknown
                List<String> sortedKeys = new ArrayList<>(map.keySet());
                Collections.sort(sortedKeys);
                columnsCSV.addAll(sortedKeys);
            }
        }

        List<String> row = new ArrayList<>();
        for (String column : columnsCSV) {
            row.add(map.getOrDefault(column, ""));
        }
        rowsCSV.add(row);
    }

    public String getCsvContent() {
        StringBuilder sb = new StringBuilder();
        sb.append("0: ").append(String.join(",", columnsCSV)).append("\n");

        int rowNumber = 1;
        for (List<String> row : rowsCSV) {
            sb.append(rowNumber).append(": ").append(String.join(",", row)).append("\n");
            rowNumber++;
        }
        sb.append(END_OF_FILE_MARKER);
        return sb.toString();
    }

    public String getBancaStatoCsvContent(String delimiter) {
        StringBuilder sb = new StringBuilder();
        sb.append("KEY")
                .append(delimiter)
                .append(String.join(delimiter, columnsCSV))
                .append("\n");

        for (List<String> row : rowsCSV) {
            sb.append("EXTERNAL")
                    .append(delimiter)
                    .append(String.join(delimiter, row))
                    .append("\n");
        }

        sb.append(END_OF_FILE_MARKER);
        return sb.toString();
    }

    public void printCsv() {
        System.out.println(getCsvContent());
    }

    public static void main(String[] args) {
        CsvSimulatorWithColumnsMap csv = new CsvSimulatorWithColumnsMap();

        Map<String, String> row1 = new LinkedHashMap<>();
        row1.put("Name", "Alice");
        row1.put("Age", "30");
        row1.put("Country", "USA");
        row1.put("Email", "alice@example.com");

        Map<String, String> row2 = new LinkedHashMap<>();
        row2.put("Name", "Bob");
        row2.put("Age", "25");

        csv.addRowFromMap(row1);
        csv.addRowFromMap(row2);

        String csvContent = csv.getBancaStatoCsvContent("|");
        csv.writeToFile("output.csv", csvContent);
    }

    public void writeToFile(String filename, String content) {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(content);
            System.out.println("CSV written to file: " + filename);
        } catch (IOException e) {
            System.err.println("Error writing file: " + e.getMessage());
        }
    }
}
