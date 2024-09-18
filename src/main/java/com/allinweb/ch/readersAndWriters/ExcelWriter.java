package com.allinweb.ch.readersAndWriters;

import com.allinweb.ch.dto.BlockLoopInstructionLoadDTO;
import com.allinweb.ch.util.*;
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

    public ExcelWriter(String botJobName, WebDriver abrWebDriver) {
        this.botJobName = botJobName;
        this.abrWebDriver = abrWebDriver;
        boolean exist = ManagedExcel.checkIfExcelExist(botJobName, "excel");
        String now = LocalDateTime.now().format(FORMAT_DATE_AND_TIME);
        managedExcelMap.put("excel", new ManagedExcel(botJobName, "excel", !exist));
        managedExcelMap.put("report", new ManagedExcel(botJobName + " (" + now + ")", "report", true));
    }

    public ExcelChain withPurpose(String purpose) {
        return new ExcelChain(managedExcelMap.get(purpose), botJobName);
    }

    public record ExcelChain(ManagedExcel managedExcel, String botJobName) {

        public void insertValueFieldName(String fieldName, String value) {
            managedExcel
                    .onSheet(0)
                    .insertValueAfterLastColumnOfRow(fieldName, INSTRUCTION_FIELDS_ROW_INDEX)
                    .insertValueAfterLastColumnOfRow(value, INSTRUCTION_FIELDS_ROW_INDEX + 1);
            managedExcel.save();
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

        public void insertInstructionResult(
                BlockLoopInstructionLoadDTO instruction, Map<String, String> data, LocalTime time, String status) {
            String[] splittedAction =
                    UtilsMethods.splitIfContains(instruction.getActions(), ABRConstants.ACTION_SPECIFICATIONS_SPLITTER);
            String action =
                    switch (splittedAction[0]) {
                        case ABRConstants.CLICK -> "CLICK";
                        case ABRConstants.INSERT -> "INSERT";
                        case ABRConstants.EXTRACT -> "EXTRACT";
                        case ABRConstants.QUIT -> "QUIT";
                        case ABRConstants.HOLD -> "WAIT";
                        case ABRConstants.REFRESH -> "REFRESH";
                        case ABRConstants.VISUALIZE -> "VISUALIZE";
                        case ABRConstants.SEARCH -> "SEARCH";
                        case ABRConstants.SCREEN -> "SCREEN";
                        default -> "Unsupported action";
                    };
            String value = "";
            if (splittedAction.length > 1) {
                String reference = splittedAction[1];
                value = data.get(reference);
            }
            if (!action.equals("SCREEN")) {
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
                    act.fillRowBackgroundColorOfLastRow(color).insertScreenshotAfterLastRowOfColumn(0, abrWebDriver);
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
                    act.fillRowBackgroundColorOfLastRow(color).insertScreenshotAfterLastRowOfColumn(0, abrWebDriver);
                }
            }
            managedExcel.save();
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

        public ManagedExcelAction insertValueAfterLastRowOfColumn(String value, int columnIndex) {
            int afterLastRowIndex = sheet.getLastRowNum() + 1;
            return insertValueAtCoordinates(value, afterLastRowIndex, columnIndex);
        }

        public ManagedExcelAction insertValueOnLastRowAfterLastColumn(String value) {
            int lastRowIndex = sheet.getLastRowNum();
            int lastColumnIndex = sheet.getRow(lastRowIndex).getLastCellNum();
            return insertValueAtCoordinates(value, lastRowIndex, lastColumnIndex);
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
