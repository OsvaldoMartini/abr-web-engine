package com.allinweb.ch;

import com.allinweb.ch.dto.*;
import com.allinweb.ch.readersAndWriters.ExcelReader;
import com.allinweb.ch.readersAndWriters.ExcelWriter;
import com.allinweb.ch.supportTypes.ExtractedData;
import com.allinweb.ch.supportTypes.WebPage;
import com.allinweb.ch.util.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javafx.scene.control.Alert;
import javax.swing.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public class Engine {
    private static SimpleDateFormat dateFormatter;
    static final String EXECUTE_JOB = "execute/j";
    static final String FORM_RECOGNITION = "form/r";
    static final String TEST = "test";
    public static Repository repository;
    private static final String language = "en";
    private static File baseLogFile = null;

    public static void main(String[] args) {

        System.out.println("ENGINE STARTED");
        for (int i = 0; i < args.length; i++) {
            System.out.println("PARAM " + i + ">> " + args[i]);
        }

        List<String> arguments = Arrays.asList(args);
        if (arguments.contains("-c")) {
            int configurationValueIndex = arguments.indexOf("-c") + 1;
            String configurationValue = arguments.get(configurationValueIndex);
            ABRPropertyManager.setConfigurationFileName(configurationValue);
        }

        Labels.initializeLabelsInSpecLang(language);
        repository = new Repository();

        try {
            baseLogFile = new File(ABRPropertyManager.getInstance().getProperty(ABRPropertyEnum.FOLDER_PATH_LOG)
                    + ABRConstants.FILE_NAME_ENGINE_LOG);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (args.length == 0) {
            System.out.println("No parameters, please read documentation.");
            System.exit(0);
        }

        dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("LookAndFeel setting failed.");
        }

        try {
            startParametersInterpreter(args);
        } catch (Exception e) {
            e.printStackTrace();
        }

        repository.closeSession();
    }

    private static void startParametersInterpreter(String[] args) throws Exception {

        String[] idsAndPaths = null;
        if (args.length > 1) {
            idsAndPaths = Arrays.copyOfRange(args, 1, args.length);
        }

        Boolean executeJob = Arrays.stream(args).anyMatch(EXECUTE_JOB::equals);

        idsAndPaths = removeElementsBefore(args, EXECUTE_JOB);

        if (executeJob) {
            executeJob(idsAndPaths);
        }
    }

    public static String[] removeElementsBefore(String[] array, String target) {
        OptionalInt indexOpt = IntStream.range(0, array.length)
                .filter(i -> array[i].equals(EXECUTE_JOB))
                .findFirst();

        // If the element is not found, return the original array
        if (!indexOpt.isPresent()) {
            return array;
        }

        int index = indexOpt.getAsInt();

        // Use Arrays.copyOfRange to create a new array with elements after the found index
        return Arrays.copyOfRange(array, index + 1, array.length);
    }

    private static void executeJob(String[] idsAndPaths) throws Exception {

        Properties labelsValue = Labels.labelsValue;

        if (idsAndPaths == null || idsAndPaths.length < 3) {
            throw new Exception("not enough parameters for job");
        }

        int homeBankingId;
        int botJobId;
        try {
            homeBankingId = Integer.parseInt(idsAndPaths[0]);
            botJobId = Integer.parseInt(idsAndPaths[1]);
        } catch (Exception e) {
            throw new Exception("no reference (id) for home banking or bot job");
        }

        try {
            HomeBankingDTO homeBankingDTO = repository.retrieveHomeBankingDTOById(homeBankingId);

            BotJobDTO selectedJob = null;

            if (homeBankingDTO == null) {
                throw new Exception("Home banking not found");
            }
            if (homeBankingDTO.getBotJobs() == null) {
                throw new Exception("No bot jobs found");
            }

            for (BotJobDTO job : homeBankingDTO.getBotJobs()) {
                if (job.getId() == botJobId) {
                    selectedJob = job;
                    break;
                }
            }
            if (selectedJob == null) {
                throw new Exception("bot job not found");
            }

            // path must be: "path1;path2;-;path4;..." path or '-' for each block
            //            String excelPath = idsAndPaths[2] + selectedJob.getName() + ".xlsx";
            String excelPath = idsAndPaths[2];

            ExcelReader excelReader = new ExcelReader();
            ExtractedData extractedData = excelReader.extractData(excelPath, repository, botJobId);
            if (extractedData.getErrorMessage() != null) {
                //				showAlert("Excel Data File", "Warning: Excel File exist" , "Fields in the excel not matching the
                // botjob requirements");
                System.out.println("Fields in the excel not matching the botjob requirements");
            }

            Set<String> blockClickables = selectedJob.getBlocks().stream()
                    .map(BlockDTO::getBlockLoopInstructions)
                    .reduce((identity, accumulated) -> {
                        accumulated.addAll(identity);
                        return accumulated;
                    })
                    .get()
                    .stream()
                    .map(BlockLoopInstructionDTO::getActions)
                    .filter(action -> action.contains(Constants.CLICK))
                    .collect(Collectors.toSet());

            String browser = ABRPropertyManager.getInstance().getProperty(ABRPropertyEnum.BROWSER);
            WebPage webPage = new WebPage(browser, homeBankingDTO.getUrl());

            String baseLogString =
                    selectedJob.getName() + Constants.FIELDS_SEPARATOR + labelsValue.getProperty(Labels.START);
            printBaseLog(baseLogFile, generateTimestamp(), baseLogString);
            ExcelReportDTO report = new ExcelReportDTO(selectedJob);
            report.setOrder((short) 0);
            report.setStartDate(LocalDateTime.now());
            report.setBatchJobId(0);
            report.setStatus((short) ExcelReportStatusEnum.NOT_RUN.ordinal());
            ExcelWriter.ExcelChain writer = new ExcelWriter(selectedJob).withPurpose("report");
            writer.insertReportHead();
            boolean success = true;
            long botJobStartTime = System.nanoTime();
            long totalExecutionTime = 0;
            String lastInstructionExecuted = "No istruction executed yet";
            short status;

            if (extractedData.getNumberOfDataRows() > 0) {
                for (int i = 0; success && i < extractedData.getNumberOfDataRows(); i++) {
                    List<BlockDTO> blockList = selectedJob.getBlocks();
                    for (int j = 0; success && j < blockList.size(); j++) {
                        writer.insertBlockSeparation(blockList.get(j).getName());
                        for (BlockLoopInstructionDTO currentInstruction :
                                blockList.get(j).getBlockLoopInstructions()) {
                            long currentInstructionStartTime = System.nanoTime();
                            File logFileForSingleExcel = excelReader.createLogFile(excelPath);
                            Map<String, String> data = extractedData.getRowFieldValues(i);
                            try {
                                lastInstructionExecuted = currentInstruction.getName()
                                        + Constants.BLANK_STRING
                                        + currentInstruction.getPath();
                                webPage.performActions(data, currentInstruction);
                                long currentInstructionEndTime = System.nanoTime();
                                writer.insertInstructionResult(
                                        currentInstruction,
                                        data,
                                        LocalTime.ofNanoOfDay(currentInstructionEndTime - currentInstructionStartTime),
                                        "success");
                                totalExecutionTime += currentInstructionEndTime - currentInstructionStartTime;
                                System.out.println("SUCCESSFUL INSTRUCTION on element: " + lastInstructionExecuted);

                            } catch (Throwable t) {
                                success = false;
                                if (currentInstruction.isOptional()) {
                                    long currentInstructionEndTime = System.nanoTime();
                                    writer.insertInstructionResult(
                                            currentInstruction,
                                            data,
                                            LocalTime.ofNanoOfDay(
                                                    currentInstructionEndTime - currentInstructionStartTime),
                                            "optional skipped");
                                    System.err.println(
                                            "FAILED OPTIONAL INSTRUCTION on element: " + lastInstructionExecuted);
                                    status = (short) ExcelReportStatusEnum.WARNING.ordinal();
                                } else {
                                    long currentInstructionEndTime = System.nanoTime();
                                    writer.insertInstructionResult(
                                            currentInstruction,
                                            data,
                                            LocalTime.ofNanoOfDay(
                                                    currentInstructionEndTime - currentInstructionStartTime),
                                            "failed");
                                    System.out.println(
                                            "FAILED MANDATORY INSTRUCTION on element: " + lastInstructionExecuted);
                                    status = (short) ExcelReportStatusEnum.ERROR.ordinal();
                                }
                                report.setStatus(status);
                                report.setDuration(totalExecutionTime / 100);
                                writer.insertTotalExecutionTimes(botJobStartTime, System.nanoTime());
                                repository.write(report);
                                throw new RuntimeException(t);
                            }
                            printLog(generateTimestamp(), logFileForSingleExcel, data, success);
                        }
                    }
                }
            } else {
                List<BlockDTO> blockList = selectedJob.getBlocks();
                for (int j = 0; success && j < blockList.size(); j++) {
                    writer.insertBlockSeparation(blockList.get(j).getName());
                    for (BlockLoopInstructionDTO currentInstruction :
                            blockList.get(j).getBlockLoopInstructions()) {
                        long currentInstructionStartTime = System.nanoTime();
                        File logFileForSingleExcel = excelReader.createLogFile(excelPath);
                        try {
                            lastInstructionExecuted = currentInstruction.getName()
                                    + Constants.BLANK_STRING
                                    + currentInstruction.getPath();
                            webPage.performActions(null, currentInstruction);
                            long currentInstructionEndTime = System.nanoTime();
                            writer.insertInstructionResult(
                                    currentInstruction,
                                    null,
                                    LocalTime.ofNanoOfDay(currentInstructionEndTime - currentInstructionStartTime),
                                    "success");
                            totalExecutionTime += currentInstructionEndTime - currentInstructionStartTime;
                            System.out.println("SUCCESSFUL INSTRUCTION on element: " + lastInstructionExecuted);

                        } catch (Throwable t) {
                            success = false;
                            if (currentInstruction.isOptional()) {
                                long currentInstructionEndTime = System.nanoTime();
                                writer.insertInstructionResult(
                                        currentInstruction,
                                        null,
                                        LocalTime.ofNanoOfDay(currentInstructionEndTime - currentInstructionStartTime),
                                        "optional skipped");
                                System.err.println(
                                        "FAILED OPTIONAL INSTRUCTION on element: " + lastInstructionExecuted);
                                status = (short) ExcelReportStatusEnum.WARNING.ordinal();
                            } else {
                                long currentInstructionEndTime = System.nanoTime();
                                writer.insertInstructionResult(
                                        currentInstruction,
                                        null,
                                        LocalTime.ofNanoOfDay(currentInstructionEndTime - currentInstructionStartTime),
                                        "failed");
                                System.out.println(
                                        "FAILED MANDATORY INSTRUCTION on element: " + lastInstructionExecuted);
                                status = (short) ExcelReportStatusEnum.ERROR.ordinal();
                            }
                            report.setStatus(status);
                            report.setDuration(totalExecutionTime / 100);
                            writer.insertTotalExecutionTimes(botJobStartTime, System.nanoTime());
                            repository.write(report);
                            throw new RuntimeException(t);
                        }
                        printLog(generateTimestamp(), logFileForSingleExcel, null, success);
                    }
                }
            }

            if (totalExecutionTime == 0) {
                report.setDuration(0);
                writer.insertTotalExecutionTimes(botJobStartTime, botJobStartTime);
                repository.write(report);
            }

            // PRINT END BASE LOG//
            if (success) {
                report.setStatus((short) ExcelReportStatusEnum.SUCCESS.ordinal());
                report.setDuration(totalExecutionTime / 100);
                writer.insertTotalExecutionTimes(botJobStartTime, System.nanoTime());
                repository.write(report);
                baseLogString = selectedJob.getName()
                        + Constants.FIELDS_SEPARATOR
                        + labelsValue.getProperty(Labels.END)
                        + Constants.FIELDS_SEPARATOR
                        + labelsValue.getProperty(Labels.OK);
            } else {
                baseLogString = selectedJob.getName()
                        + Constants.FIELDS_SEPARATOR
                        + labelsValue.getProperty(Labels.END)
                        + Constants.FIELDS_SEPARATOR
                        + labelsValue.getProperty(Labels.KO)
                        + lastInstructionExecuted;
            }
            printBaseLog(baseLogFile, generateTimestamp(), baseLogString);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static String generateTimestamp() {
        Date date = new Date();
        dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        return dateFormatter.format(date);
    }

    private static void printLog(String timeStamp, File logFile, Map<String, String> data, boolean result) {
        String resultMsg = result ? Constants.SUCCESS : Constants.FAIL;
        String log = String.join(Constants.FIELDS_SEPARATOR, data.toString(), timeStamp, resultMsg);

        try {
            FileWriter fileWriter = new FileWriter(logFile, true);
            fileWriter.write(log + System.lineSeparator());
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printBaseLog(File logFile, String timeStamp, String msg) {
        String resultMsg;
        String log = String.join(Constants.FIELDS_SEPARATOR, timeStamp, msg);

        try {
            FileWriter fileWriter = new FileWriter(logFile, true);
            fileWriter.write(log + System.lineSeparator());
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printLogExcel(String timeStamp, File logExcel, Map<String, String> data, boolean result) {
        String resultMsg = result ? Constants.SUCCESS : Constants.FAIL;

        try {
            Workbook logExcelWorkbook = WorkbookFactory.create(logExcel);
            Sheet logSheet = logExcelWorkbook.getSheetAt(0);
            int maxColumn = logSheet.getRow(0).getLastCellNum();
            int newRowIndex = logSheet.getLastRowNum() + 1;

            Row newLogRow = logSheet.createRow(newRowIndex);

            String[] paymentToArray = data.values().toArray(String[]::new);
            for (int i = 0; i < maxColumn; i++) {
                newLogRow.createCell(i).setCellValue(paymentToArray[i]);
            }

            FileOutputStream outputStream = new FileOutputStream(logExcel);
            logExcelWorkbook.write(outputStream);
            logExcelWorkbook.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
