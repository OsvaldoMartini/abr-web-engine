package com.allinweb.ch.readersAndWriters;

import com.allinweb.ch.dto.BlockLoopInstructionLoadDTO;
import com.allinweb.ch.util.*;
import com.google.common.base.Strings;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

public class ExcelWriter {
    private static final int INSTRUCTION_FIELDS_ROW_INDEX = 1;
    private static final int EXECUTION_TIMES_COLUMN_INDEX = 11;

    private static final DateTimeFormatter FORMAT_DATE_AND_TIME = DateTimeFormatter.ofPattern("dd-MM-yyyy HH.mm.ss");
    private static final DateTimeFormatter FORMAT_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Map<String, ManagedExcel> managedExcelMap = new HashMap<>();
    private String botJobName;
    private static WebDriver abrWebDriver;

    private static int CURRENT_ROW_INDEX = 0;

    public ExcelWriter(String botJobName, WebDriver abrWebDriver) {
        this.botJobName = botJobName;
        this.abrWebDriver = abrWebDriver;
        boolean exist = ManagedExcel.checkIfExcelExist(botJobName, "excel");
        boolean existExport = ManagedExcel.checkIfExcelExist(botJobName + "_export", "export");
        String now = LocalDateTime.now().format(FORMAT_DATE_AND_TIME);
        String fileNameExport = ABRPropertyManager.getInstance().getProperty(ABRPropertyEnum.FILE_NAME_EXPORT);
        try {
            if (!Strings.isNullOrEmpty(fileNameExport)) {
                managedExcelMap.put("export", new ManagedExcel(fileNameExport, "export", !existExport));
            } else {
                managedExcelMap.put(
                        "export", new ManagedExcel(botJobName + "_export" + " (" + now + ")", "export", !existExport));
            }

            managedExcelMap.put("excel", new ManagedExcel(botJobName, "excel", !exist));
            managedExcelMap.put("report", new ManagedExcel(botJobName + " (" + now + ")", "report", true));
        } catch (Exception ex) {
            ABRLogger.getInstance(ExcelWriter.class)
                    .severe(String.format("Excel Folder maybe not configured. %s\nError", botJobName, ex.getMessage()));
        }
    }

    public ExcelChain withPurpose(String purpose) {
        return new ExcelChain(managedExcelMap.get(purpose), botJobName);
    }

    public record ExcelChain(ManagedExcel managedExcel, String botJobName) {

        public void insertValueFieldName(String fieldName, String value) {
            try {

                managedExcel
                        .onSheet(0)
                        .insertValueAfterLastColumnOfRow(fieldName, INSTRUCTION_FIELDS_ROW_INDEX)
                        .insertValueAfterLastColumnOfRow(value, INSTRUCTION_FIELDS_ROW_INDEX + 1);
                managedExcel.save();
            } catch (Exception ex) {
                ABRLogger.getInstance(ExcelWriter.class)
                        .severe(String.format(
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
                ABRLogger.getInstance(ExcelWriter.class)
                        .severe(String.format(
                                "Excel Writer insertValueFieldName.Check if the file exist. File: %s\nError",
                                botJobName, ex.getMessage()));
            }
        }

        public void insertReportHead() {
            managedExcel
                    .onSheet(0)
                    .insertValueAtCoordinates(botJobName, 0, 0)
                    .insertValueAtCoordinates(LocalDateTime.now().format(FORMAT_DATE_AND_TIME), 0, 1)
                    .setFontStyleOfLastRow(true, false, false, (short) 14);
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

        public void insertInstructionResult(
                BlockLoopInstructionLoadDTO instruction, Map<String, String> data, LocalTime time, String status) {
            try {

                String[] splittedAction = UtilsMethods.splitIfContains(
                        instruction.getActions(), ABRConstants.ACTION_SPECIFICATIONS_SPLITTER);
                String[] operations = UtilsMethods.splitIfContains(
                        instruction.getOperation(), ABRConstants.ACTION_SPECIFICATIONS_SPLITTER);

                String action =
                        switch (splittedAction[0]) {
                            case ABRConstants.OTHER -> "OTHER";
                            case ABRConstants.OUTPUT -> "OUTPUT";
                            case ABRConstants.CLICK -> "CLICK";
                            case ABRConstants.INSERT -> "INSERT";
                            case ABRConstants.EXTRACT_FIELD -> "EXTRACT";
                            case ABRConstants.QUIT -> "QUIT";
                            case ABRConstants.HOLD -> "WAIT";
                            case ABRConstants.REFRESH -> "REFRESH";
                            case ABRConstants.VISUALIZE -> "VISUALIZE";
                            case ABRConstants.SEARCH -> "SEARCH";
                            case ABRConstants.SET_VALUE -> "SET VALUE";
                            case ABRConstants.GET_VALUE -> "GET VALUE";
                            case ABRConstants.CHECK_VALUE -> "CHECK VALUE";
                            case ABRConstants.GOTO -> "GO TO";
                            case ABRConstants.IF -> "IF";
                            case ABRConstants.ELSE -> "ELSE";
                            case ABRConstants.ENDIF -> "ENDIF";
                            case ABRConstants.SCREEN -> "SCREENSHOT";
                            default -> "Unsupported action";
                        };
                String value = "";
                if (splittedAction.length > 1) {
                    String reference = splittedAction[1];
                    value = data.get(reference);
                }

                if (operations.length == 2) {
                    value = operations[1];
                } else if (operations.length == 3) {
                    value = operations[1] + " " + operations[2];
                }

                if (!action.equals("SCREENSHOT")) {
                    ManagedExcelAction act = managedExcel
                            .onSheet(0)
                            .insertValueAfterLastRowOfColumn(action, 0)
                            .setCellFontStyleOfColumnOfLastRow(0, true, false, false)
                            .insertValueOnLastRowAfterLastColumn(instruction.getName())
                            .insertValueOnLastRowAfterLastColumn(value)
                            .insertValueOnLastRowAfterLastColumn(time.format(FORMAT_TIME))
                            .insertValueOnLastRowAfterLastColumn(status);
                    if (!status.equals("success")) {
                        IndexedColors color = status.equals("failed") ? IndexedColors.RED : IndexedColors.YELLOW;
                        act.fillRowBackgroundColorOfLastRow(color)
                                .insertScreenshotAfterLastRowOfColumn(0, abrWebDriver);
                    }
                } else { // add screenshot
                    ManagedExcelAction act = managedExcel
                            .onSheet(0)
                            .insertValueAfterLastRowOfColumn(action, 0)
                            .setCellFontStyleOfColumnOfLastRow(0, true, false, false)
                            .insertValueOnLastRowAfterLastColumn(instruction.getName())
                            .insertValueOnLastRowAfterLastColumn("")
                            .insertValueOnLastRowAfterLastColumn(time.format(FORMAT_TIME))
                            .insertValueOnLastRowAfterLastColumn(status)
                            .insertScreenshotAfterLastRowOfColumn(0, abrWebDriver);
                    if (!status.equals("success")) {
                        IndexedColors color = status.equals("failed") ? IndexedColors.RED : IndexedColors.YELLOW;
                        act.fillRowBackgroundColorOfLastRow(color)
                                .insertScreenshotAfterLastRowOfColumn(0, abrWebDriver);
                    }
                }
                managedExcel.save();
            } catch (Exception ex) {
                ABRLogger.getInstance(ExcelWriter.class)
                        .severe(String.format(
                                "InsertInstructionResult ( %s ) Error: %s ", instruction.getName(), ex.getMessage()));
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

        public static boolean checkIfExcelExist(String fileName, String purpose) {
            String fullPath = System.getProperty("user.dir") + "\\" + purpose + "\\" + fileName + Constants.FILE_FORMAT;
            return new FileManager(fullPath).getFile().exists();
        }

        public ManagedExcel(String fileName, String purpose, boolean create) {
            ABRPropertyEnum property =
                    switch (purpose) {
                        case "report" -> ABRPropertyEnum.FOLDER_PATH_REPORT;
                        case "excel" -> ABRPropertyEnum.FOLDER_PATH_EXCEL;
                        case "export" -> ABRPropertyEnum.FOLDER_PATH_EXPORT;
                        default -> throw new UnsupportedOperationException("Purpose: " + purpose + " not supported");
                    };
            String fileNamePath = "\\" + fileName + Constants.FILE_FORMAT;
            String fullPath = ABRPropertyManager.getInstance().getProperty(property) + fileNamePath;
            this.fileManager = new FileManager(fullPath);
            this.excelWorkbook = manageExcelFile(create, purpose);
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
                ABRLogger.getInstance(ExcelWriter.class)
                        .severe(String.format(
                                "Excel Writer insertFieldNameAndValueLastColumn: \nError", ex.getMessage()));
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
                e.printStackTrace();
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
                e.printStackTrace();
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
                boolean isBold, boolean isItalic, boolean isStrikeout, short height) {
            Row row = getOrCreateRow(sheet.getLastRowNum());
            for (int i = 0; i < row.getLastCellNum(); i++) {
                setCellFontStyleOfColumnOfLastRow(i, isBold, isItalic, isStrikeout, height);
            }
            return this;
        }

        public ManagedExcelAction setCellFontStyleOfColumnOfLastRow(
                int columnIndex, boolean isBold, boolean isItalic, boolean isStrikeout) {
            return setCellFontStyle(sheet.getLastRowNum(), columnIndex, isBold, isItalic, isStrikeout, (short) 0);
        }

        public ManagedExcelAction setCellFontStyleOfColumnOfLastRow(
                int columnIndex, boolean isBold, boolean isItalic, boolean isStrikeout, short height) {
            return setCellFontStyle(sheet.getLastRowNum(), columnIndex, isBold, isItalic, isStrikeout, height);
        }

        public ManagedExcelAction setCellFontStyle(
                int rowIndex, int columnIndex, boolean isBold, boolean isItalic, boolean isStrikeout, short height) {
            Row row = getOrCreateRow(rowIndex);
            Cell cell = getOrCreateColumnCell(row, columnIndex);
            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.cloneStyleFrom(cell.getCellStyle());
            Font font = workbook.createFont();
            font.setBold(isBold);
            font.setItalic(isItalic);
            font.setStrikeout(isStrikeout);
            if (height > 0) {
                font.setFontHeightInPoints(height);
            }
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
    }
}
