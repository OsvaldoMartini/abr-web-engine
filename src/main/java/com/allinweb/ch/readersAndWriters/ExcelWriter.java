package com.allinweb.ch.readersAndWriters;

import com.allinweb.ch.model.FieldData;
import com.allinweb.ch.util.ARConstantsEngine;
import com.allinweb.ch.util.ARExecution;
import com.allinweb.ch.util.ARPropertyEnum;
import com.allinweb.ch.util.ARPropertyManager;
import com.google.common.base.Strings;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

@Slf4j
public class ExcelWriter {
    private static final ARPropertyManager arPropertyManager;
    private static final int INSTRUCTION_FIELDS_ROW_INDEX = 1;
    private static final int EXECUTION_TIMES_COLUMN_INDEX = 11;
    private static final DateTimeFormatter FORMAT_DATE_AND_TIME = DateTimeFormatter.ofPattern("dd-MM-yyyy HH.mm.ss");
    private static final DateTimeFormatter FORMAT_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static WebDriver webDriver;
    private static int CURRENT_ROW_INDEX = 0;

    static {
        arPropertyManager = ARPropertyManager.getInstance();
    }

    private final Map<String, ManagedExcel> managedExcelMap = new HashMap<>();
    private String botJobName;

    public ExcelWriter(String botJobName, WebDriver webDriver, boolean isFullPath) {
        this.botJobName = botJobName;
        this.webDriver = webDriver;
        boolean exist = ManagedExcel.checkIfExcelExist(botJobName, "excel", isFullPath);
        String now = LocalDateTime.now().format(FORMAT_DATE_AND_TIME);
        try {
            managedExcelMap.put("export", new ManagedExcel(botJobName, "export", true, isFullPath));

            managedExcelMap.put("excel", new ManagedExcel(botJobName, "excel", !exist, isFullPath));
            managedExcelMap.put("report", new ManagedExcel(botJobName + " (" + now + ")", "report", true, isFullPath));
        } catch (Exception ex) {

            log.error(String.format("Excel Folder maybe not configured. %s\nError", botJobName, ex.getMessage()));
        }
    }

    public ExcelChain withPurpose(String purpose) {
        return new ExcelChain(managedExcelMap.get(purpose), botJobName);
    }

    public record ExcelChain(ManagedExcel managedExcel, String botJobName) {

        public static void writeMapToCSV(Map<String, String> mapExport, String filePath, String delimiterCSV) {
            try (FileWriter writer = new FileWriter(filePath)) {
                // Write the keys (first row)
                String keys = String.join(delimiterCSV, mapExport.keySet());
                writer.write(keys + "\n");

                // Write the values (second row, corresponding to the keys)
                StringBuilder valuesBuilder = new StringBuilder();
                boolean first = true;
                for (String key : mapExport.keySet()) {
                    if (!first) {
                        valuesBuilder.append(delimiterCSV);
                    }
                    valuesBuilder.append(mapExport.get(key));
                    first = false;
                }
                writer.write(valuesBuilder.toString() + "\n");

                log.info("CSV file created successfully at: " + filePath);

            } catch (IOException e) {
                log.error("Error writing to CSV file: " + e.getMessage());
            }
        }

        public void insertValueFieldName(String fieldName, String value) {
            try {

                managedExcel
                        .onSheet(0)
                        .insertValueAfterLastColumnOfRow(fieldName, INSTRUCTION_FIELDS_ROW_INDEX)
                        .insertValueAfterLastColumnOfRow(value, INSTRUCTION_FIELDS_ROW_INDEX + 1);
                managedExcel.save();
            } catch (Exception ex) {

                log.error(String.format(
                        "Excel Writer insertValueFieldName.Check if the file exist. File: %s\nError",
                        botJobName, ex.getMessage()));
            }
        }

        public void insertFieldNameAndValueLastColumn(Map<String, String> mapExport, int exportIndex) {
            try {

                managedExcel.onSheet(0).insertFieldNameAndValueLastColumn(mapExport, exportIndex);
                //                        .insertColumValueOnLastRow(value);
                managedExcel.save();
            } catch (Exception ex) {

                log.error(String.format(
                        "Excel Writer insertValueFieldName.Check if the file exist. File: %s\nError",
                        botJobName, ex.getMessage()));
            }
        }

        public void insertCSVContentIntoExcel(List<String> columnsCSV, List<List<String>> rowsCSV, int exportIndex) {
            try {

                managedExcel.onSheet(0).insertCSVContentIntoExcel(columnsCSV, rowsCSV, exportIndex);
                //                        .insertColumValueOnLastRow(value);
                managedExcel.save();
            } catch (Exception ex) {

                log.error(String.format(
                        "Excel Writer insertValueFieldName.Check if the file exist. File: %s\nError",
                        botJobName, ex.getMessage()));
            }
        }

        public void insertReportHead() {
            managedExcel
                    .onSheet(0)
                    .insertValueAtCoordinates("Bot-Job: " + botJobName, 0, 0)
                    .insertValueAtCoordinates(LocalDateTime.now().format(FORMAT_DATE_AND_TIME), 0, 1)
                    .setFontStyleOfLastRow(true, false, false, (short) 14, null, null);
            managedExcel.save();
        }

        public void insertBlockSeparation(String blockName) {
            managedExcel
                    .onSheet(0)
                    .insertValueAfterLastRowOfColumn(blockName, 0)
                    .fillRowBackgroundColorOfLastRow(IndexedColors.ROYAL_BLUE)
                    .setRowTextColorOfLastRow(IndexedColors.WHITE);
            managedExcel.save();
        }

        public void insertBlockSeparationExport(String blockName, int exportIndex) {
            managedExcel
                    .onSheet(0)
                    .insertValueAfterLastRowOfColumn(blockName, exportIndex, 0)
                    .fillRowBackgroundColorOfLastRow(IndexedColors.ROYAL_BLUE)
                    .setRowTextColorOfLastRow(IndexedColors.WHITE);
            managedExcel.save();
        }

        public boolean insertInstructionResult(
                ARExecution.ConditionStatus currentCondition,
                String blockName,
                String[] actions,
                FieldData msgLoop,
                Map<String, String> data,
                LocalTime time,
                boolean success) {
            try {

                //                String[] splittedAction = UtilsMethods.splitIfContains(
                //                        instruction.getActions(), ARConstants.ACTION_SPECIFICATIONS_SPLITTER);
                //                String[] operations = new String[0];
                //                if (instruction.getOperation() != null) {
                //                    operations = UtilsMethods.splitIfContains(
                //                            instruction.getOperation(), ARConstants.ACTION_SPECIFICATIONS_SPLITTER);
                //                }

                String[] operations = !Strings.isNullOrEmpty(msgLoop.getValue())
                        ? msgLoop.getValue().split(":")
                        : new String[] {};

                String action =
                        switch (actions[0]) {
                            case ARConstantsEngine.OTHER -> "OTHER";
                            case ARConstantsEngine.OUTPUT -> "OUTPUT";
                            case ARConstantsEngine.CLICK -> "CLICK";
                            case ARConstantsEngine.INSERT -> "INSERT";
                            case ARConstantsEngine.EXTRACT_FIELD -> "EXTRACT";
                            case ARConstantsEngine.QUIT -> "QUIT";
                            case ARConstantsEngine.HOLD -> "HOLD";
                            case ARConstantsEngine.REFRESH_ONLY -> "REFRESH";
                            case ARConstantsEngine.REFRESH_HOLD -> "WAIT (REFRESH LOOP)";
                            case ARConstantsEngine.REFRESH_LOOP -> "JUMP (REFRESH LOOP)";
                            case ARConstantsEngine.LOOP -> "JUMP TO";
                            case ARConstantsEngine.VISUALIZE -> "VISUALIZE";
                            case ARConstantsEngine.SEARCH -> "SEARCH";
                            case ARConstantsEngine.SET_VALUE -> "SET VALUE";
                            case ARConstantsEngine.GET_VALUE -> "GET VALUE";
                            case ARConstantsEngine.CHECK_VALUE -> "CHECK VALUE";
                            case ARConstantsEngine.GOTO -> "GOTO";
                            case ARConstantsEngine.IF -> "IF";
                            case ARConstantsEngine.ELSE -> "ELSE";
                            case ARConstantsEngine.ENDIF -> "ENDIF";
                            case ARConstantsEngine.SCREEN -> "SCREENSHOT";
                            case ARConstantsEngine.PAUSE -> "PAUSE";
                            case ARConstantsEngine.IGNORE -> "IGNORE";
                            case ARConstantsEngine.EXIT -> "EXIT";
                            case ARConstantsEngine.BY_PASS -> "BY_PASS";
                            case ARConstantsEngine.EXCEL_BLOCK_HEADER -> "EXCEL_BLOCK_HEADER";
                            case ARConstantsEngine.NEXT_ROW -> "EXCEL DATA NEXT ROW";
                            default -> "Unsupported action";
                        };
                String value = "";
                String keyAction = msgLoop.getKey();

                if (actions[0].equals(ARConstantsEngine.INSERT) && actions[1].equals(ARConstantsEngine.ENTER)) {
                    String reference = actions[2];
                    value = data.get(reference);
                } else if (actions[0].equals(ARConstantsEngine.INSERT)) {
                    String reference = actions[1];
                    value = data.get(reference);
                }

                if (actions.length == 2 && actions[0].equalsIgnoreCase(ARConstantsEngine.OUTPUT)) {
                    value = msgLoop.getValue();
                }

                if (actions[0].equalsIgnoreCase(ARConstantsEngine.NEXT_ROW)) {
                    keyAction = msgLoop.getKey();
                    value = msgLoop.getValue();
                } else if (actions[0].equalsIgnoreCase(ARConstantsEngine.GOTO)) {
                    if (msgLoop.getValue().equals("Unknown")) {
                        keyAction = msgLoop.getKey();
                        value = msgLoop.getValue();
                    } else {
                        String[] parts = msgLoop.getKey().split(":");
                        if (!msgLoop.getValue().equals("0")) {
                            keyAction = String.format(
                                    "GO TO Block \"%s\" Id: \"%s\"",
                                    "#" + parts[2] + " " + parts[3], parts[0] + "-(" + parts[1] + ")");
                        } else {
                            keyAction =
                                    String.format("GO TO Limit Reached for \"%s\"", "#" + parts[2] + " " + parts[3]);
                        }
                        value = String.format("GO TO Remains %s times", msgLoop.getValue());
                    }
                } else if (actions[0].equalsIgnoreCase(ARConstantsEngine.LOOP)) {
                    if (msgLoop.getValue().equals("Unknown")) {
                        keyAction = msgLoop.getKey();
                        value = msgLoop.getValue();
                    } else {
                        String[] msgParent = msgLoop.getKey().split(":");
                        keyAction = String.format(
                                "Jump To Parent \"%s\"", msgParent[0] + "-(" + msgParent[1] + ") " + msgParent[2]);
                        value = String.format("Loop Remains %s times", msgLoop.getValue());
                    }
                } else if (actions[0].equalsIgnoreCase(ARConstantsEngine.REFRESH_ONLY)) {
                    String[] msgParent = msgLoop.getKey().split(":");
                    if (msgParent.length == 1) {
                        keyAction = "Refresh for Web Page";
                    } else if (msgParent.length > 2) {
                        action = "REFRESH (REFRESH LOOP)";
                        keyAction = String.format(
                                "Refresh for \"%s\"", msgParent[0] + "-(" + msgParent[1] + ") " + msgParent[2]);
                    }

                } else if (actions[0].equalsIgnoreCase(ARConstantsEngine.REFRESH_HOLD)) {
                    String[] msgParent = msgLoop.getKey().split(":");
                    String[] msgValue = msgLoop.getValue().split(":");
                    keyAction =
                            String.format("Wait for \"%s\"", msgParent[0] + "-(" + msgParent[1] + ") " + msgParent[2]);
                    value = String.format("Wait %s seconds", msgValue[0]);
                } else if (actions[0].equalsIgnoreCase(ARConstantsEngine.REFRESH_LOOP)) {
                    if (msgLoop.getValue().equals("Unknown")) {
                        keyAction = msgLoop.getKey();
                        value = msgLoop.getValue();
                    } else {
                        String[] msgParent = msgLoop.getKey().split(":");
                        keyAction = String.format(
                                "Jump To \"%s\"", msgParent[0] + "-(" + msgParent[1] + ") " + msgParent[2]);
                        value = String.format("Loop Remains %s times", msgLoop.getValue());
                    }
                } else if (actions[0].equalsIgnoreCase(ARConstantsEngine.HOLD)) {
                    value = "";
                } else if (operations.length == 2
                        && (actions[0].equalsIgnoreCase(ARConstantsEngine.SET_VALUE)
                                || actions[0].equalsIgnoreCase(ARConstantsEngine.GET_VALUE))) {
                    keyAction = operations[0];
                    value = operations[1];
                } else if (operations.length == 3) {
                    value = operations[1] + " " + operations[2];
                }

                boolean byPassError = currentCondition.equals(ARExecution.ConditionStatus.IF_FAILED)
                        || currentCondition.equals(ARExecution.ConditionStatus.ELSEIF_FAILED)
                        || currentCondition.equals(ARExecution.ConditionStatus.ELSE_FAILED)
                        || currentCondition.equals(ARExecution.ConditionStatus.BY_PASS);

                String blockCondition = currentCondition.equals(ARExecution.ConditionStatus.IF_FAILED)
                        ? "{IF}"
                        : currentCondition.equals(ARExecution.ConditionStatus.ELSEIF_FAILED)
                                ? "{ELSEIF}"
                                : currentCondition.equals(ARExecution.ConditionStatus.ELSE_FAILED) ? "{ELSE}" : "";

                if (action.equals("EXCEL_BLOCK_HEADER")) {
                    ManagedExcelAction act = managedExcel
                            .onSheet(0)
                            .insertValueAfterLastRowOfColumn(blockName, 0)
                            .setCellFontStyleOfColumnsOfLastRow(
                                    0,
                                    4, // Apply styles to the first 5 columns (0 to 4)
                                    true,
                                    false,
                                    false,
                                    IndexedColors.BLUE_GREY,
                                    IndexedColors.WHITE);
                } else if (!action.equals("SCREENSHOT")) {
                    ManagedExcelAction act = managedExcel
                            .onSheet(0)
                            .insertValueAfterLastRowOfColumn(action, 0)
                            .setCellFontStyleOfColumnOfLastRow(0, true, false, false, null, null)
                            .insertValueOnLastRowAfterLastColumn(keyAction)
                            .insertValueOnLastRowAfterLastColumn(value)
                            .insertValueOnLastRowAfterLastColumn(time.format(FORMAT_TIME))
                            .insertValueOnLastRowAfterLastColumn(success ? "success" : "error")
                            .setCellFontStyleOfColumnOfLastRow(
                                    4, true, false, false, null, !success ? IndexedColors.RED : null);

                    if (!success) {

                        String msgCant = !success ? "Can't " + action : "";
                        keyAction = (blockCondition.length() > 0
                                ? blockCondition + " by passing error " + keyAction
                                : byPassError ? "LOOP by passing error " + keyAction : keyAction);

                        IndexedColors backColor = !byPassError ? IndexedColors.RED : IndexedColors.YELLOW;
                        IndexedColors fontColor = !byPassError ? IndexedColors.WHITE : IndexedColors.BLUE;

                        act.insertValueAfterLastRowOfColumn(msgCant, 0)
                                .insertValueOnLastRowAfterLastColumn(keyAction)
                                .setCellFontStyleOfColumnsOfLastRow(
                                        0, 4, // Apply styles to the first 5 columns (0 to 4)
                                        false, false, false, backColor, fontColor);
                        act.insertScreenshotAfterLastRowOfColumn(0, webDriver);
                    }
                } else { // add screenshot
                    ManagedExcelAction act = managedExcel
                            .onSheet(0)
                            .insertValueAfterLastRowOfColumn(action, 0)
                            .setCellFontStyleOfColumnOfLastRow(0, true, false, false, null, null)
                            .insertValueOnLastRowAfterLastColumn(keyAction)
                            .insertValueOnLastRowAfterLastColumn("")
                            .insertValueOnLastRowAfterLastColumn(time.format(FORMAT_TIME))
                            .insertValueOnLastRowAfterLastColumn(success ? "success" : "error")
                            .setCellFontStyleOfColumnOfLastRow(
                                    4, true, false, false, null, success ? null : IndexedColors.RED)
                            .insertScreenshotAfterLastRowOfColumn(0, webDriver);
                }
                managedExcel.save();
                return true;
            } catch (Exception ex) {

                log.error(
                        String.format("InsertInstructionResult ( %s ) Error: %s ", msgLoop.getKey(), ex.getMessage()));
                return false;
            }
        }

        public void insertTotalExecutionTimes(long startTime, long endTime) {
            long duration = endTime - startTime;
            LocalTime time = LocalTime.now();
            managedExcel
                    .onSheet(0)
                    .insertValueAtCoordinates("Execution Times", 0, EXECUTION_TIMES_COLUMN_INDEX)
                    .insertValueAtCoordinates("Start Time", 1, EXECUTION_TIMES_COLUMN_INDEX)
                    .insertValueAtCoordinates(
                            time.minusNanos(duration).format(FORMAT_TIME), 1, EXECUTION_TIMES_COLUMN_INDEX + 1)
                    .insertValueAtCoordinates("End Time", 2, EXECUTION_TIMES_COLUMN_INDEX)
                    .insertValueAtCoordinates(time.format(FORMAT_TIME), 2, EXECUTION_TIMES_COLUMN_INDEX + 1)
                    .insertValueAtCoordinates("Duration", 3, EXECUTION_TIMES_COLUMN_INDEX)
                    .insertValueAtCoordinates(
                            LocalTime.ofNanoOfDay(duration).format(FORMAT_TIME), 3, EXECUTION_TIMES_COLUMN_INDEX + 1)
                    .setSheetAutoResizable();
            managedExcel.save();
        }
    }

    static class ManagedExcel {
        private final XSSFWorkbook excelWorkbook;
        private final FileManager fileManager;

        public ManagedExcel(String fileName, String purpose, boolean create, boolean isFullPath) {
            ARPropertyEnum property =
                    switch (purpose) {
                        case "report" -> ARPropertyEnum.PATH_REPORT;
                        case "excel" -> ARPropertyEnum.PATH_EXCEL;
                        case "export" -> ARPropertyEnum.PATH_EXPORT;
                        default -> throw new UnsupportedOperationException("Purpose: " + purpose + " not supported");
                    };
            String fileNamePath = "";
            String fullPath = "";

            if (!isFullPath) {
                fileNamePath = "\\" + fileName + ARConstantsEngine.FILE_FORMAT_EXCEL;
                fullPath = arPropertyManager.getProperty(property) + fileNamePath;
            } else {
                fullPath = fileName;
            }
            this.fileManager = new FileManager(fullPath);
            this.excelWorkbook = manageExcelFile(create, purpose);
        }

        public static boolean checkIfExcelExist(String fileName, String purpose, boolean isFullPath) {
            if (!isFullPath) {
                String fullPath = System.getProperty("user.dir") + "\\" + purpose + "\\" + fileName
                        + ARConstantsEngine.FILE_FORMAT_EXCEL;
                return new FileManager(fullPath).getFile().exists();
            } else {
                return new FileManager(fileName).getFile().exists();
            }
        }

        private XSSFWorkbook manageExcelFile(boolean newExcel, String purpose) {
            if (newExcel) {
                XSSFWorkbook workbook = new XSSFWorkbook();
                workbook.createSheet();
                return workbook;
            }
            File excelFile;
            if (purpose.equals("excel")) {
                excelFile = fileManager.getFile();
            } else {
                excelFile = fileManager.deleteFileOnDisk().createFileOnDisk().getFile();
            }
            try {
                return new XSSFWorkbook(excelFile);
            } catch (IOException | InvalidFormatException e) {
                throw new RuntimeException(e);
            }
        }

        public ManagedExcelAction onSheet(int index) {
            return new ManagedExcelAction(excelWorkbook.getSheetAt(index), excelWorkbook);
        }

        public void save() {
            File excelFile = fileManager.deleteFileOnDisk().createFileOnDisk().getFile();
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(excelFile);
                excelWorkbook.write(fileOutputStream);
                fileOutputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    record ManagedExcelAction(Sheet sheet, XSSFWorkbook workbook) {

        private Row getOrCreateRow(int rowIndex) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                row = sheet.createRow(rowIndex);
            }
            return row;
        }

        private Cell getOrCreateColumnCell(Row row, int columnIndex) {
            Cell columnCell = row.getCell(columnIndex);
            if (columnCell == null) {
                columnCell = row.createCell(columnIndex);
            }
            return columnCell;
        }

        public ManagedExcelAction insertValueAtCoordinates(String value, int rowIndex, int columnIndex) {
            Row row = getOrCreateRow(rowIndex);
            Cell columnCell = getOrCreateColumnCell(row, columnIndex);
            columnCell.setCellValue(value);
            return this;
        }

        public ManagedExcelAction insertValueAfterLastColumnOfRow(String value, int rowIndex) {
            int afterLastColumnIndex = sheet.getRow(rowIndex).getLastCellNum();
            return insertValueAtCoordinates(value, rowIndex, afterLastColumnIndex);
        }

        public ManagedExcelAction insertValueAfterLastColumnOfRow(String value) {
            int lastRowIndex = sheet.getLastRowNum();
            int afterLastColumnIndex = sheet.getRow(lastRowIndex).getLastCellNum();
            return insertValueAtCoordinates(value, lastRowIndex, afterLastColumnIndex);
        }

        public ManagedExcelAction insertValueAfterLastRowOfColumn(String value, int columnIndex) {
            int afterLastRowIndex = sheet.getLastRowNum() + 1;
            return insertValueAtCoordinates(value, afterLastRowIndex, columnIndex);
        }

        public ManagedExcelAction insertValueAfterLastRowOfColumn(String value, int exportIndex, int columnIndex) {
            return insertValueAtCoordinates(value, exportIndex, columnIndex);
        }

        public ManagedExcelAction insertValueOnLastRowAfterLastColumn(String value) {
            int lastRowIndex = sheet.getLastRowNum();
            int lastColumnIndex = sheet.getRow(lastRowIndex).getLastCellNum();
            return insertValueAtCoordinates(value, lastRowIndex, lastColumnIndex);
        }

        public void insertFieldNameAndValueLastColumn(Map<String, String> mapExport, int exportIndex) {
            try {
                // Get the current last row index
                int lastRowIndex = sheet.getLastRowNum();
                // Get the row where field names (column names) are stored
                Row headerRow = getOrCreateRow(exportIndex);
                Row valueRow = getOrCreateRow(exportIndex + 1);

                // Iterate over the map and insert/replace values
                for (Map.Entry<String, String> entry : mapExport.entrySet()) {
                    String columnName = entry.getKey();
                    String value = entry.getValue();

                    // Check if column already exists
                    boolean columnExists = false;
                    int columnIndex = 0; // Start at the first column (index 0)

                    for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                        Cell cell = headerRow.getCell(i);
                        if (cell != null && columnName.equals(cell.getStringCellValue())) {
                            columnExists = true;
                            columnIndex = i;
                            break;
                        }
                    }

                    // If column exists, replace the value
                    if (columnExists) {
                        insertValueAtCoordinates(value, exportIndex + 1, columnIndex);
                    }
                    // If column does not exist, add a new column
                    else {
                        // Check if the first column (index 0) is empty and start there
                        if (headerRow.getCell(0) == null
                                || headerRow.getCell(0).getStringCellValue().isEmpty()) {
                            columnIndex = 0; // Start writing in the first column
                        } else {
                            columnIndex = headerRow.getLastCellNum(); // Otherwise, find the next available column
                        }
                        insertValueAtCoordinates(columnName, exportIndex, columnIndex);
                        insertValueAtCoordinates(value, exportIndex + 1, columnIndex);
                    }
                }

                // Save the Excel after modification

            } catch (Exception ex) {

                log.error(String.format("Excel Writer insertFieldNameAndValueLastColumn: \nError", ex.getMessage()));
            }
        }

        public void insertCSVContentIntoExcel(List<String> columnsCSV, List<List<String>> rowsCSV, int exportIndex) {
            try {
                // Create header row: KEY | column1 | column2 | ...
                Row headerRow = getOrCreateRow(exportIndex);
                insertValueAtCoordinates("KEY", exportIndex, 0);
                for (int i = 0; i < columnsCSV.size(); i++) {
                    insertValueAtCoordinates(columnsCSV.get(i), exportIndex, i + 1);
                }

                // Write each row: EXTERNAL_1 | val1 | val2 | ...
                for (int i = 0; i < rowsCSV.size(); i++) {
                    Row row = getOrCreateRow(exportIndex + 1 + i);
                    insertValueAtCoordinates("EXTERNAL_" + (i + 1), exportIndex + 1 + i, 0);
                    List<String> rowData = rowsCSV.get(i);
                    for (int j = 0; j < rowData.size(); j++) {
                        insertValueAtCoordinates(rowData.get(j), exportIndex + 1 + i, j + 1);
                    }
                }

            } catch (Exception ex) {

                log.error(String.format("Excel Writer insertCSVContentIntoExcel: \nError - %s", ex.getMessage()));
            }
        }

        public ManagedExcelAction insertImageAtCoordinates(String value, int rowIndex, int columnIndex) {
            // TODO: to implement if needed
            return this;
        }

        public ManagedExcelAction insertScreenshotAfterLastRowOfColumn(int columnIndex, WebDriver webDriver) {
            int afterLastRowIndex = sheet.getLastRowNum() + 1;
            return insertScreenshotAtCoordinates(afterLastRowIndex, columnIndex, webDriver);
        }

        public ManagedExcelAction insertScreenshotAtCoordinates(int rowIndex, int columnIndex) {
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            try {
                BufferedImage capture = new Robot().createScreenCapture(screenRect);
                File tempFile = File.createTempFile("screenshot", ".png");
                ImageIO.write(capture, "png", tempFile);
                FileInputStream fis = new FileInputStream(tempFile);
                byte[] bytes = IOUtils.toByteArray(fis);
                int pictureIdx = workbook.addPicture(bytes, Workbook.PICTURE_TYPE_PNG);
                fis.close();
                CreationHelper helper = workbook.getCreationHelper();
                ClientAnchor imageAnchor = helper.createClientAnchor();
                imageAnchor.setCol1(columnIndex);
                imageAnchor.setRow1(rowIndex);
                imageAnchor.setCol2(columnIndex + 8);
                imageAnchor.setRow2(rowIndex + 1);
                Drawing drawing = sheet.createDrawingPatriarch();
                Picture pict = drawing.createPicture(imageAnchor, pictureIdx);
                // pict.resize();
                Row row = getOrCreateRow(rowIndex);
                getOrCreateColumnCell(row, columnIndex);
                row.setHeightInPoints(300);
            } catch (IOException | AWTException e) {
                log.info(e.getMessage());
            }
            return this;
        }

        public ManagedExcelAction insertScreenshotAtCoordinates(int rowIndex, int columnIndex, WebDriver driver) {
            try {
                // Capture screenshot using WebDriver's TakesScreenshot interface
                File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);

                // Read the screenshot file into byte array
                byte[] bytes = Files.readAllBytes(screenshot.toPath());

                // Add picture to workbook
                int pictureIdx = workbook.addPicture(bytes, Workbook.PICTURE_TYPE_PNG);
                CreationHelper helper = workbook.getCreationHelper();

                // Set image position in Excel
                ClientAnchor imageAnchor = helper.createClientAnchor();
                imageAnchor.setCol1(columnIndex);
                imageAnchor.setRow1(rowIndex);
                imageAnchor.setCol2(columnIndex + 8);
                imageAnchor.setRow2(rowIndex + 1);

                // Create the drawing and insert the image
                Drawing drawing = sheet.createDrawingPatriarch();
                Picture pict = drawing.createPicture(imageAnchor, pictureIdx);

                // Set row height for better visibility
                Row row = getOrCreateRow(rowIndex);
                getOrCreateColumnCell(row, columnIndex);
                row.setHeightInPoints(300);
            } catch (IOException e) {
                log.info(e.getMessage());
            }
            return this;
        }

        public ManagedExcelAction fillRowBackgroundColorOfLastRow(IndexedColors color) {
            return fillRowBackgroundColor(sheet.getLastRowNum(), color);
        }

        public ManagedExcelAction fillRowBackgroundColor(int rowIndex, IndexedColors color) {
            return setRowBackgroundColorOfColumns(rowIndex, color, 0, 10);
        }

        public ManagedExcelAction setRowBackgroundColorOfColumns(
                int rowIndex, IndexedColors color, int startColumnIndex, int endColumnIndex) {
            Row row = sheet.getRow(rowIndex);
            CellStyle cellStyle = workbook.createCellStyle();
            for (; row != null && startColumnIndex <= endColumnIndex; startColumnIndex++) {
                Cell cell = getOrCreateColumnCell(row, startColumnIndex);
                cellStyle.cloneStyleFrom(cell.getCellStyle());
                cellStyle.setFillBackgroundColor(color.getIndex());
                cellStyle.setFillForegroundColor(color.getIndex());
                cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                cell.setCellStyle(cellStyle);
            }
            return this;
        }

        public ManagedExcelAction setRowTextColorOfLastRow(IndexedColors color) {
            return setRowTextColor(sheet.getLastRowNum(), color);
        }

        public ManagedExcelAction setRowTextColor(int rowIndex, IndexedColors color) {
            return setRowTextColorOfColumns(rowIndex, color, 0, 10);
        }

        public ManagedExcelAction setRowTextColorOfColumns(
                int rowIndex, IndexedColors color, int startColumnIndex, int endColumnIndex) {
            Row row = sheet.getRow(rowIndex);
            for (; row != null && startColumnIndex <= endColumnIndex; startColumnIndex++) {
                Cell cell = getOrCreateColumnCell(row, startColumnIndex);
                Font font = workbook.createFont();
                font.setColor(color.getIndex());
                cell.getCellStyle().setFont(font);
            }
            return this;
        }

        public ManagedExcelAction setFontStyleOfLastRow(
                boolean isBold,
                boolean isItalic,
                boolean isStrikeout,
                short height,
                IndexedColors backgroundColorHex,
                IndexedColors fontColorHex) {
            Row row = getOrCreateRow(sheet.getLastRowNum());
            for (int i = 0; i < row.getLastCellNum(); i++) {
                setCellFontStyleOfColumnOfLastRow(
                        i, isBold, isItalic, isStrikeout, height, backgroundColorHex, fontColorHex);
            }
            return this;
        }

        public ManagedExcelAction setCellFontStyleOfColumnOfLastRow(
                int columnIndex,
                boolean isBold,
                boolean isItalic,
                boolean isStrikeout,
                IndexedColors backgroundColorHex,
                IndexedColors fontColorHex) {
            return setCellFontStyle(
                    sheet.getLastRowNum(),
                    columnIndex,
                    isBold,
                    isItalic,
                    isStrikeout,
                    (short) 0,
                    backgroundColorHex,
                    fontColorHex);
        }

        public ManagedExcelAction setCellFontStyleOfColumnOfLastRow(
                int columnIndex,
                boolean isBold,
                boolean isItalic,
                boolean isStrikeout,
                short height,
                IndexedColors backgroundColorHex,
                IndexedColors fontColorHex) {
            return setCellFontStyle(
                    sheet.getLastRowNum(),
                    columnIndex,
                    isBold,
                    isItalic,
                    isStrikeout,
                    height,
                    backgroundColorHex,
                    fontColorHex);
        }

        public ManagedExcelAction setCellFontStyle(
                int rowIndex,
                int columnIndex,
                boolean isBold,
                boolean isItalic,
                boolean isStrikeout,
                short height,
                IndexedColors backgroundColor,
                IndexedColors fontColor) {

            // Get or create the row and cell
            Row row = getOrCreateRow(rowIndex);
            Cell cell = getOrCreateColumnCell(row, columnIndex);

            // Create a new cell style
            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.cloneStyleFrom(cell.getCellStyle());

            // Create a new font
            Font font = workbook.createFont();
            font.setBold(isBold);
            font.setItalic(isItalic);
            font.setStrikeout(isStrikeout);
            if (height > 0) {
                font.setFontHeightInPoints(height);
            }

            // Set the font color using the Excel IndexedColors
            if (fontColor != null) {
                font.setColor(IndexedColors.valueOf(fontColor.toString()).getIndex());
            }

            // Set background color using the Excel IndexedColors
            if (backgroundColor != null) {
                cellStyle.setFillForegroundColor(
                        IndexedColors.valueOf(backgroundColor.toString()).getIndex());
                cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }

            // Apply the font and cell style to the cell
            cellStyle.setFont(font);
            cell.setCellStyle(cellStyle);

            return this;
        }

        public ManagedExcelAction setSheetAutoResizable() {
            int maximumColumns = 0;
            for (int i = 0; i < sheet.getLastRowNum(); i++) {
                Row row = getOrCreateRow(i);
                if (maximumColumns < row.getLastCellNum()) {
                    maximumColumns = row.getLastCellNum();
                }
            }
            for (int i = 0; i < maximumColumns; i++) {
                sheet.autoSizeColumn(i);
            }
            return this;
        }

        public ManagedExcelAction setCellFontStyleOfColumnsOfLastRow(
                int startColumnIndex,
                int endColumnIndex,
                boolean bold,
                boolean italic,
                boolean underline,
                IndexedColors backgroundColor,
                IndexedColors fontColor) {
            int lastRowIndex = sheet.getLastRowNum();
            Row row = getOrCreateRow(lastRowIndex);
            CellStyle cellStyle = workbook.createCellStyle();
            Font font = workbook.createFont();

            // Set font styles
            font.setBold(bold);
            font.setItalic(italic);
            font.setUnderline(underline ? Font.U_SINGLE : Font.U_NONE);
            font.setColor(fontColor.getIndex());

            // Apply font and background styles
            cellStyle.setFont(font);
            cellStyle.setFillForegroundColor(backgroundColor.getIndex());
            cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Apply styles to each column in the specified range
            for (int i = startColumnIndex; i <= endColumnIndex; i++) {
                Cell cell = getOrCreateColumnCell(row, i);
                cell.setCellStyle(cellStyle);
            }
            return this;
        }
    }
}
