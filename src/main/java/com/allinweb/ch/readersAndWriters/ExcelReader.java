package com.allinweb.ch.readersAndWriters;

import com.allinweb.ch.util.ARConstantsEngine;
import com.allinweb.ch.util.ARPropertyManager;
import com.allinweb.ch.util.ExtractedData;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;

@Slf4j
public class ExcelReader {

    private static final ARPropertyManager arPropertyManager;
    private static final DataFormatter formatter = new DataFormatter();
    private static final int EXCEL_BLOCK_NAME_ROW = 0;
    private static final int EXCEL_DATA_COLUMN_INTESTATION_ROW = 1;
    private static String executed = "EXECUTED";
    private static String OUTCOME = "outcome";

    static {
        arPropertyManager = ARPropertyManager.getInstance();
    }

    public ExcelReader() {}

    // Basically, it mirrors exactly what Excel displays, without altering formats.
    private static String getCellValue(Cell cell) {
        if (cell == null) return null;
        return formatter.formatCellValue(cell);
    }

    /** Backward-compat wrapper. Prefer the overload that accepts an aliasOfCanonical map. */
    public ExtractedData extractData(String paymentsFilePath, List<String> allActions) throws Exception {
        return extractData(paymentsFilePath, allActions, Collections.emptyMap());
    }

    /**
     * @param aliasOfCanonical map of canonical instruction name -> clientNamed display label
     *     (only entries where clientNamed is set). Used so the missing-fields check accepts an
     *     Excel column header that matches EITHER the canonical name OR the user-set display
     *     label, avoiding false positives after a rename.
     */
    public ExtractedData extractData(
            String paymentsFilePath, List<String> allActions, Map<String, String> aliasOfCanonical) throws Exception {
        ExtractedData extractedDataWithMissingFields = new ExtractedData();
        if (aliasOfCanonical == null) {
            aliasOfCanonical = Collections.emptyMap();
        }

        try (InputStream in = Files.newInputStream(Paths.get(paymentsFilePath));
                Workbook workbook = WorkbookFactory.create(in)) {

            Sheet firstSheet = workbook.getSheetAt(0);

            if (allActions == null || allActions.isEmpty()) {
                String errorMessage = "File Exist but No actions were provided";
                extractedDataWithMissingFields.setErrorTitle("No Actions Provided");
                extractedDataWithMissingFields.setErrorMessage(errorMessage);
                throw new Exception(errorMessage);
            }

            Row blockNamesRow = firstSheet.getRow(EXCEL_BLOCK_NAME_ROW);
            Row fieldNamesRow = firstSheet.getRow(EXCEL_DATA_COLUMN_INTESTATION_ROW);
            if (fieldNamesRow == null) {
                String errorMessage = "Field names row is missing in the Excel sheet";
                extractedDataWithMissingFields.setErrorTitle("Missing Field Names Row");
                extractedDataWithMissingFields.setErrorMessage(errorMessage);
                throw new Exception(errorMessage);
            }

            // Collect canonical INPUT field names from the bot job's actions.
            Set<String> blockFields = new HashSet<>();
            for (String action : allActions) {
                if (action.contains(ARConstantsEngine.INSERT)
                        && action.contains(ARConstantsEngine.ACTION_SPECIFICATIONS_SPLITTER)) {

                    String[] parts = action.split(ARConstantsEngine.ACTION_SPECIFICATIONS_SPLITTER);
                    if (parts.length == 3
                            && parts[0].equals(ARConstantsEngine.INSERT)
                            && parts[1].equals(ARConstantsEngine.ENTER)) {
                        blockFields.add(parts[2]);
                    } else if (parts.length == 2 && parts[0].equals(ARConstantsEngine.INSERT)) {
                        blockFields.add(parts[1]);
                    }
                }
            }

            // Build per-column block context: row 0 holds block names ("#BlockName"), but only
            // at the leftmost column of each block group. Carry the most recent non-empty block
            // name forward across columns so every column knows which block it belongs to.
            // Strip the "#" prefix and trim whitespace so the stored block matches
            // blockLoad.getName() at lookup time.
            int firstCol = fieldNamesRow.getFirstCellNum();
            int lastCol = fieldNamesRow.getLastCellNum();
            String[] columnToBlock = new String[Math.max(0, lastCol)];
            String currentBlock = null;
            for (int i = 0; i < lastCol; i++) {
                String cellBlock = blockNamesRow != null ? getCellValue(blockNamesRow.getCell(i)) : null;
                if (cellBlock != null && !cellBlock.isBlank()) {
                    String trimmed = cellBlock.trim();
                    if (trimmed.startsWith("#")) {
                        trimmed = trimmed.substring(1).trim();
                    }
                    currentBlock = trimmed;
                }
                columnToBlock[i] = currentBlock;
            }
            log.debug("ExcelReader columnToBlock map: {}", java.util.Arrays.toString(columnToBlock));

            // Register all column headers under their block bucket.
            for (int i = firstCol; i < lastCol; i++) {
                String fieldName = getCellValue(fieldNamesRow.getCell(i));
                if (fieldName == null) continue;
                extractedDataWithMissingFields.addField(columnToBlock[i], fieldName);
            }

            // Read data rows. Cell values are bucketed by (block, fieldName) -> row -> value.
            for (int currentRowIndex = EXCEL_DATA_COLUMN_INTESTATION_ROW + 1;
                    currentRowIndex <= firstSheet.getLastRowNum();
                    currentRowIndex++) {
                Row currentRow = firstSheet.getRow(currentRowIndex);
                if (currentRow == null) {
                    continue;
                }

                int rowPosition = currentRowIndex - EXCEL_DATA_COLUMN_INTESTATION_ROW - 1;
                boolean rowHasData = false;
                Map<Integer, String> pendingByCol = new LinkedHashMap<>();

                for (int currentCellIndex = currentRow.getFirstCellNum();
                        currentCellIndex < currentRow.getLastCellNum();
                        currentCellIndex++) {
                    String value = getCellValue(currentRow.getCell(currentCellIndex));
                    if (value != null && !value.trim().isEmpty()) {
                        rowHasData = true;
                    }
                    pendingByCol.put(currentCellIndex, value);
                }

                if (!rowHasData) continue;

                for (Map.Entry<Integer, String> e : pendingByCol.entrySet()) {
                    int col = e.getKey();
                    String fieldName = getCellValue(fieldNamesRow.getCell(col));
                    if (fieldName == null) continue;
                    String blockForCol = (col < columnToBlock.length) ? columnToBlock[col] : null;
                    extractedDataWithMissingFields.addFieldValue(blockForCol, fieldName, e.getValue(), rowPosition);
                }
            }

            // Missing-fields check. Accept either the canonical name or the clientNamed alias
            // as a valid Excel column header. Block scoping isn't enforced here (the writer
            // takes care of producing block-scoped columns); we just verify presence somewhere
            // in the workbook.
            Set<String> allExtracted = extractedDataWithMissingFields.getExtractedFields();
            Set<String> missingFields = new HashSet<>();
            for (String blockField : blockFields) {
                String alias = aliasOfCanonical.get(blockField);
                boolean found = false;
                for (String extractedField : allExtracted) {
                    if (extractedField == null) continue;
                    if (blockField.equalsIgnoreCase(extractedField)
                            || (alias != null && alias.equalsIgnoreCase(extractedField))) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    missingFields.add(blockField);
                    // Register the field name so callers that introspect getExtractedFields()
                    // still see it. Do NOT synthesize a "CHANGE ME" data row at
                    // getNumberOfDataRows(): doing so inflated the row count by one and caused
                    // executeJob to iterate one row past real data.
                    extractedDataWithMissingFields.addField(blockField);
                }
            }

            if (!missingFields.isEmpty()) {
                extractedDataWithMissingFields.setMissingFields(
                        "Fields in the Excel do not match the Bot Job requirements. Missing fields: "
                                + String.join(", ", missingFields));
            }

            return extractedDataWithMissingFields;

        } catch (FileNotFoundException e) {
            if (isFileInUse(e)) {
                extractedDataWithMissingFields.setErrorTitle("File In Use");
                extractedDataWithMissingFields.setErrorMessage("The file is currently in use by another process.");
            } else {
                extractedDataWithMissingFields.setErrorTitle("File Not Found");
                extractedDataWithMissingFields.setErrorMessage("The file does not exist.");
            }
        } catch (IOException e) {
            extractedDataWithMissingFields.setErrorTitle("IOException");
            extractedDataWithMissingFields.setErrorMessage(
                    "An unexpected error occurred while processing the file: " + e.getMessage());
        } catch (Exception e) {
            // swallow: error info already populated where relevant
        }

        return extractedDataWithMissingFields;
    }

    private boolean isFileInUse(FileNotFoundException e) {
        return e.getMessage().contains("being used by another process");
    }
}
