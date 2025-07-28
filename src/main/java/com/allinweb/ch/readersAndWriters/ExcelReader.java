package com.allinweb.ch.readersAndWriters;

import com.allinweb.ch.util.ARConstants;
import com.allinweb.ch.util.ARPropertyEnum;
import com.allinweb.ch.util.ARPropertyManager;
import com.allinweb.ch.util.ExtractedData;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelReader {

    private static final ARPropertyManager arPropertyManager;

    static {
        arPropertyManager = ARPropertyManager.getInstance();
    }

    private static final DataFormatter formatter = new DataFormatter();
    private static final int EXCEL_DATA_COLUMN_INTESTATION_ROW = 1;

    private static String executed = "EXECUTED";

    private static String OUTCOME = "outcome";

    public ExcelReader() {}

    public ExtractedData extractData(String paymentsFilePath, List<String> allActions) throws Exception {
        // Initialize the extracted data
        ExtractedData extractedDataWithMissingFields = new ExtractedData();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new File(paymentsFilePath))) {
            Sheet firstSheet = workbook.getSheetAt(0);

            if (allActions == null || allActions.isEmpty()) {
                String errorMessage = "File Exist but No actions were provided";
                extractedDataWithMissingFields.setErrorTitle("No Actions Provided");
                extractedDataWithMissingFields.setErrorMessage(errorMessage);
                throw new Exception(errorMessage);
            }

            Row fieldNamesRow = firstSheet.getRow(EXCEL_DATA_COLUMN_INTESTATION_ROW);
            if (fieldNamesRow == null) {
                String errorMessage = "Field names row is missing in the Excel sheet";
                extractedDataWithMissingFields.setErrorTitle("Missing Field Names Row");
                extractedDataWithMissingFields.setErrorMessage(errorMessage);
                throw new Exception(errorMessage); // Throwing exception if field row is missing
            }

            // Extract block fields from actions
            // Initialize a Set to hold block fields
            Set<String> blockFields = new HashSet<>();

            // Iterate over each action in allActions
            for (String action : allActions) {
                // Check if action contains ARConstants.INSERT and ARConstants.ACTION_SPECIFICATIONS_SPLITTER
                if (action.contains(ARConstants.INSERT)
                        && action.contains(ARConstants.ACTION_SPECIFICATIONS_SPLITTER)) {

                    String[] parts = action.split(ARConstants.ACTION_SPECIFICATIONS_SPLITTER);
                    if (parts.length == 3
                            && parts[0].equals(ARConstants.INSERT)
                            && parts[1].equals(ARConstants.ENTER)) {
                        blockFields.add(parts[2]);
                    } else if (parts.length == 2 && parts[0].equals(ARConstants.INSERT)) {
                        blockFields.add(parts[1]);
                    }
                }
            }

            // Cache field names and values from extractedData
            for (int i = fieldNamesRow.getFirstCellNum(); i < fieldNamesRow.getLastCellNum(); i++) {
                String fieldName = getCellValue(fieldNamesRow.getCell(i));
                extractedDataWithMissingFields.addField(fieldName);
            }

            // Cache existing field values from extractedData and add them to extractedDataWithMissingFields
            for (int currentRowIndex = EXCEL_DATA_COLUMN_INTESTATION_ROW + 1;
                    currentRowIndex <= firstSheet.getLastRowNum();
                    currentRowIndex++) {
                Row currentRow = firstSheet.getRow(currentRowIndex);
                if (currentRow == null) {
                    continue;
                }

                for (int currentCellIndex = currentRow.getFirstCellNum();
                        currentCellIndex < currentRow.getLastCellNum();
                        currentCellIndex++) {
                    String fieldName = getCellValue(fieldNamesRow.getCell(currentCellIndex));
                    String value = getCellValue(currentRow.getCell(currentCellIndex));
                    extractedDataWithMissingFields.addFieldValue(
                            fieldName, value, currentRowIndex - EXCEL_DATA_COLUMN_INTESTATION_ROW - 1);
                }
            }

            // Extract missing fields from actions
            Set<String> missingFields = new HashSet<>();
            for (String blockField : blockFields) {
                boolean found = false;
                for (String extractedField : extractedDataWithMissingFields.getExtractedFields()) {
                    if (blockField.equalsIgnoreCase(extractedField)) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    missingFields.add(blockField);
                    extractedDataWithMissingFields.addField(blockField);
                    extractedDataWithMissingFields.addFieldValue(
                            blockField, "CHANGE ME", extractedDataWithMissingFields.getNumberOfDataRows());
                }
            }

            // Set the error message for missing fields if any
            if (!missingFields.isEmpty()) {
                extractedDataWithMissingFields.setMissingFields(
                        "Fields in the Excel do not match the Bot Job requirements. Missing fields: "
                                + String.join(", ", missingFields));
            }

            return extractedDataWithMissingFields;

        } catch (FileNotFoundException e) {
            // Handle FileNotFoundException and set an appropriate error message
            if (isFileInUse(e)) {
                extractedDataWithMissingFields.setErrorTitle("File In Use");
                extractedDataWithMissingFields.setErrorMessage("The file is currently in use by another process.");
            } else {
                extractedDataWithMissingFields.setErrorTitle("File Not Found");
                extractedDataWithMissingFields.setErrorMessage("The file does not exist.");
            }
        } catch (IOException e) {
            // Handle IOException and set an appropriate error message
            extractedDataWithMissingFields.setErrorTitle("IOException");
            extractedDataWithMissingFields.setErrorMessage(
                    "An unexpected error occurred while processing the file: " + e.getMessage());
        } catch (Exception e) {
            // Handle other exceptions
            // extractedDataWithMissingFields.setErrorTitle("An error occurred");
            // extractedDataWithMissingFields.setErrorMessage(e.getMessage());
        }

        return extractedDataWithMissingFields;
    }

    // Helper method to check if the file is in use
    private boolean isFileInUse(FileNotFoundException e) {
        // Check if the exception message contains 'being used by another process'
        return e.getMessage().contains("being used by another process");
    }

    public File createLogFile(String filePath) {

        File paymentsFile = new File(filePath);
        String paymentsFileName = paymentsFile.getName();
        int lastPeriodPos = paymentsFileName.lastIndexOf('.');
        paymentsFileName = paymentsFileName.substring(0, lastPeriodPos);
        String logDirectory = arPropertyManager.getProperty(ARPropertyEnum.PATH_LOG);
        String logFilePath = logDirectory + "\\" + paymentsFileName + ARConstants.FILE_FORMAT_LOG;

        File logFile = null;
        try {
            logFile = new File(logFilePath);
            logFile.createNewFile();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }

        return logFile;
    }

    /* TODO: To be removed
    public File createPaymentsExcelLogFile(String paymentsFilePath) {


    	File paymentsFile = new File(paymentsFilePath);
    	File logDirectory = new File(paymentsFile.getParent() + logFolderName);
    	logDirectory.mkdir();

    	String paymentsFileName = paymentsFile.getName();
    	int lastPeriodPos = paymentsFileName.lastIndexOf('.');
    	paymentsFileName = paymentsFileName.substring(0, lastPeriodPos);
    	String logExcelFilePath = logDirectory + "\\" + paymentsFileName + executed + executedExcelFileExtension;


    	try {
    		Workbook workbookPayments = WorkbookFactory.create(paymentsFile);
    		Row intestationRow = workbookPayments.getSheetAt(0).getRow(0);

    		Workbook logExcelWorkbook = new XSSFWorkbook();
    		Sheet page0 = logExcelWorkbook.createSheet();
    		Row intestationRowLog = page0.createRow(0);

    		Cell logCell;
    		int maxColumn = 0;
    		for (int i = 0; i < intestationRow.getLastCellNum(); i++) {
    			logCell = intestationRowLog.createCell(i);
    			logCell.setCellValue(intestationRow.getCell(i).getStringCellValue());
    			maxColumn = i;
    		}
    		logCell = intestationRowLog.createCell(maxColumn);
    		logCell.setCellValue(OUTCOME);

    		FileOutputStream outputStream = new FileOutputStream(logExcelFilePath);
    		logExcelWorkbook.write(outputStream);
    		outputStream.close();
    		logExcelWorkbook.close();

    	} catch (Exception e) {
    		System.out.println(e.getMessage());
    		System.exit(0);
    	}
    	return new File(logExcelFilePath);
    }
    */

    // Basically, it mirrors exactly what Excel displays, without altering formats.
    private static String getCellValue(Cell cell) {
        if (cell == null) return null;
        return formatter.formatCellValue(cell);

        //        String cellValue = null;
        //        CellType type = cell.getCellType();
        //        switch (type) {
        //            case STRING -> {
        //                String val = cell.getStringCellValue();
        //                if (!val.isBlank()) {
        //                    cellValue = val;
        //                }
        //            }
        //            case NUMERIC -> {
        //                cellValue = String.valueOf(cell.getNumericCellValue());
        //                //                if (cellValue.contains(".0")) {
        //                //                    cellValue = cellValue.substring(0, cellValue.indexOf(".0"));
        //                //                }
        //                return cellValue;
        //            }
        //            case BOOLEAN, BLANK, FORMULA, ERROR -> {}
        //        }
        //        return cellValue;
    }
}
