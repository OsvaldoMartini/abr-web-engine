package com.allinweb.ch;

import com.allinweb.ch.builder.WebElementTagNameEnum;
import com.allinweb.ch.dto.*;
import com.allinweb.ch.readersAndWriters.ExcelReader;
import com.allinweb.ch.readersAndWriters.ExcelWriter;
import com.allinweb.ch.supportTypes.ExtractedData;
import com.allinweb.ch.supportTypes.WebPage;
import com.allinweb.ch.util.*;
import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
import org.openqa.selenium.WebDriver;

public class Engine {
    private static SimpleDateFormat dateFormatter;
    static final String EXECUTE_JOB = "execute/j";
    static final String FORM_RECOGNITION = "form/r";
    static final String TEST = "test";
    public static Repository repository;
    private static final String language = "en";
    private static File baseLogFile = null;

    private static Map<String, String> mapOperators;
    private static WebDriver abrWebDriver;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 30;
    private static final Random RANDOM = new Random();

    private static final DateTimeFormatter FORMAT_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static Connection conn = null;

    private static final String CONNECTION_TYPE = "jdbc:ucanaccess://";
    private static final String CONNECTION_PARAMETERS = ";memory=false;newDatabaseVersion=V2010";

    // Postgres
    private static final boolean POSTGRES_DB = false;
    private static final String CONNECTION_POSTGRES = "jdbc:postgresql://";
    private static final String DB_HOST = "localhost"; // or your PostgreSQL server address
    private static final String DB_PORT = "5432"; // default PostgreSQL port
    private static final String DB_NAME = "abr_web"; // your database name
    private static final String USERNAME = "postgres"; // your database username
    private static final String PASSWORD = "martini"; // your database password

    private static List<BotJobLoadDTO> botLoadJobs = new ArrayList<>();
    private static List<BlockLoopInstructionLoadDTO> instructionsExecuted = new ArrayList<>();

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
                    + ABRConstants.FILE_NAME_ENGINE_BASE_LOG);
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

    private static boolean executeJob(String[] idsAndPaths) throws Exception {

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

            mapOperators = new HashMap<>();

            String browser = ABRPropertyManager.getInstance().getProperty(ABRPropertyEnum.BROWSER);
            WebPage webPage = new WebPage(
                    browser,
                    homeBankingDTO.getUrl(),
                    homeBankingDTO.getPriority(),
                    homeBankingDTO.getOptionsConfig(),
                    mapOperators);

            abrWebDriver = webPage.getDriver();

            String baseLogString =
                    selectedJob.getName() + Constants.FIELDS_SEPARATOR + labelsValue.getProperty(Labels.START);
            printBaseLog(baseLogFile, generateTimestamp(), baseLogString);
            ExcelReportDTO report = new ExcelReportDTO(selectedJob);
            report.setOrder((short) 0);
            report.setStartDate(LocalDateTime.now());
            report.setBatchJobId(0);
            report.setStatus((short) ExcelReportStatusEnum.NOT_RUN.ordinal());
            ExcelWriter.ExcelChain writer = new ExcelWriter(selectedJob.getName(), abrWebDriver).withPurpose("report");
            writer.insertReportHead();
            boolean success = true;
            boolean stopAll = false;
            long botJobStartTime = System.nanoTime();
            long totalExecutionTime = 0;
            String lastInstructionExecuted = "No instruction executed yet";
            String resultAcions = "";
            short status = (short) ExcelReportStatusEnum.ERROR.ordinal();
            Map<String, String> dataExcel = null;

            loadBlockAll(botJobId);

            List<BlockLoadDTO> blocksLoaded = botLoadJobs.get(0).getBlockLoadDTOList();
            if (extractedData.getNumberOfDataRows() > 0) {
                for (int i = 0; success && i < extractedData.getNumberOfDataRows(); i++) {
                    List<BlockLoadDTO> blockList = blocksLoaded;

                    if (stopAll) {
                        break;
                    }
                    for (int j = 0; success && j < blockList.size(); j++) {
                        if (stopAll) {
                            break;
                        }

                        writer.insertBlockSeparation(blockList.get(j).getName());

                        // Call the method to get the filtered list
                        List<BlockLoopInstructionLoadDTO> unexecutedInstructions = getUnexecutedInstructions(
                                instructionsExecuted, blockList.get(j).getBlockLoopInstructionLoadDTOS());

                        for (BlockLoopInstructionLoadDTO currentInstruction : unexecutedInstructions) {

                            if (stopAll) {
                                break;
                            }
                            if (currentInstruction.getExecuted() == null || !currentInstruction.getExecuted()) {
                                boolean execOperation = false;
                                boolean checkOperation = false;
                                String xPathOperation = null;
                                String parentField = null;

                                String[] actions =
                                        currentInstruction.getActions().split(Constants.ACTIONS_AND_PATHS_SPLITTER);
                                String[] operations = currentInstruction.getOperation() != null
                                        ? currentInstruction
                                                .getOperation()
                                                .split(Constants.ACTION_SPECIFICATIONS_SPLITTER)
                                        : null;

                                if (actions[0].equalsIgnoreCase(WebElementTagNameEnum.GET.getValue())
                                        || actions[0].equalsIgnoreCase(WebElementTagNameEnum.SET.getValue())) {

                                    execOperation = true;
                                    xPathOperation = blockList.get(j).getBlockLoopInstructionLoadDTOS().stream()
                                            .filter(f -> f.getId() == currentInstruction.getParentId())
                                            .findFirst()
                                            .get()
                                            .getPath();
                                    parentField = blockList.get(j).getBlockLoopInstructionLoadDTOS().stream()
                                            .filter(f -> f.getId() == currentInstruction.getParentId())
                                            .findFirst()
                                            .get()
                                            .getName();
                                } else if (actions[0].equalsIgnoreCase(WebElementTagNameEnum.CK.getValue())) {
                                    parentField = blockList.get(j).getBlockLoopInstructionLoadDTOS().stream()
                                            .filter(f -> f.getId() == currentInstruction.getParentId())
                                            .findFirst()
                                            .get()
                                            .getName();
                                    checkOperation = true;
                                }

                                long currentInstructionStartTime = System.nanoTime();
                                File logFileForSingleExcel = excelReader.createLogFile(excelPath);

                                //                                fillUpCurretLocators(currentInstruction);

                                try {
                                    if (!execOperation && !checkOperation) {
                                        dataExcel = extractedData.getRowFieldValues(i);

                                        lastInstructionExecuted = currentInstruction.getName()
                                                + Constants.BLANK_STRING
                                                + currentInstruction.getPath();
                                        resultAcions = webPage.performActions(
                                                dataExcel,
                                                currentInstruction,
                                                botJobId,
                                                blockList.get(j).getName());
                                        long currentInstructionEndTime = System.nanoTime();
                                        totalExecutionTime += currentInstructionEndTime - currentInstructionStartTime;

                                        if (resultAcions != null) {

                                            ABRLogger.getInstance(WebPage.class)
                                                    .fine("SUCCESSFUL INSTRUCTION on element: " + resultAcions
                                                            + " Cmd: " + lastInstructionExecuted);

                                            currentInstruction.setExecuted(true);

                                            // Assuming currentInstruction and instructionsExecuted are already defined
                                            if (currentInstruction != null
                                                    && instructionsExecuted.stream()
                                                            .noneMatch(instruction ->
                                                                    instruction.getInstructionOrderNumber()
                                                                            == currentInstruction
                                                                                    .getInstructionOrderNumber())) {
                                                instructionsExecuted.add(currentInstruction);
                                            }
                                            success = true;
                                        } else {
                                            resultAcions = "Failed to Execute -> " + currentInstruction.getName();
                                            success = false;
                                        }

                                        writer.insertInstructionResult(
                                                currentInstruction,
                                                dataExcel,
                                                LocalTime.ofNanoOfDay(
                                                        currentInstructionEndTime - currentInstructionStartTime),
                                                success ? "success" : "failed");

                                    } else if (execOperation) {
                                        // Special Operators
                                        lastInstructionExecuted = currentInstruction.getName()
                                                + Constants.BLANK_STRING
                                                + currentInstruction.getActions()
                                                + Constants.BLANK_STRING
                                                + currentInstruction.getOperation();

                                        if (operations.length == 2) {
                                            resultAcions = webPage.performActionOperator(
                                                    currentInstruction,
                                                    xPathOperation,
                                                    actions[0],
                                                    operations,
                                                    parentField);

                                            long currentInstructionEndTime = System.nanoTime();
                                            totalExecutionTime +=
                                                    currentInstructionEndTime - currentInstructionStartTime;

                                            if (resultAcions != null) {

                                                ABRLogger.getInstance(WebPage.class)
                                                        .fine("SUCCESSFUL INSTRUCTION on element: " + resultAcions
                                                                + " Cmd: " + lastInstructionExecuted);

                                                currentInstruction.setExecuted(true);

                                                // Assuming currentInstruction and instructionsExecuted are already
                                                // defined
                                                if (currentInstruction != null
                                                        && instructionsExecuted.stream()
                                                                .noneMatch(instruction ->
                                                                        instruction.getInstructionOrderNumber()
                                                                                == currentInstruction
                                                                                        .getInstructionOrderNumber())) {
                                                    instructionsExecuted.add(currentInstruction);
                                                }
                                                success = true;
                                            } else {
                                                resultAcions = "Failed to Execute Cmd: " + lastInstructionExecuted;
                                                success = false;
                                            }
                                        } else {
                                            resultAcions = "Failed to Execute Cmd: " + lastInstructionExecuted;
                                            success = false;
                                        }
                                    } else if (checkOperation) {
                                        // Special Operators
                                        lastInstructionExecuted = currentInstruction.getName()
                                                + Constants.BLANK_STRING
                                                + currentInstruction.getActions()
                                                + Constants.BLANK_STRING
                                                + currentInstruction.getOperation();

                                        if (operations.length == 3) {
                                            if (mapOperators.containsKey(parentField)) {

                                                //                                        mapOperators =
                                                // performActionOperator(currentInstruction, xPathOperation,
                                                // mapOperators,
                                                // actions[0],operations[1]);
                                                resultAcions = "(" + parentField + ")" + String.join(":", operations);
                                                boolean isOperationValid = false;
                                                if (operations[1].equalsIgnoreCase("=")) {
                                                    isOperationValid = mapOperators
                                                            .get(parentField)
                                                            .equalsIgnoreCase(operations[2]);

                                                } else if (operations[1].equalsIgnoreCase(">")) {
                                                    isOperationValid = mapOperators
                                                            .get(parentField)
                                                            .equalsIgnoreCase(operations[2]);
                                                }

                                                long currentInstructionEndTime = System.nanoTime();
                                                totalExecutionTime +=
                                                        currentInstructionEndTime - currentInstructionStartTime;

                                                if (isOperationValid) {

                                                    ABRLogger.getInstance(WebPage.class)
                                                            .fine("SUCCESSFUL INSTRUCTION on element: " + resultAcions
                                                                    + " Cmd: " + lastInstructionExecuted);

                                                    currentInstruction.setExecuted(true);

                                                    // Assuming currentInstruction and instructionsExecuted are already
                                                    // defined
                                                    if (currentInstruction != null
                                                            && instructionsExecuted.stream()
                                                                    .noneMatch(
                                                                            instruction ->
                                                                                    instruction
                                                                                                    .getInstructionOrderNumber()
                                                                                            == currentInstruction
                                                                                                    .getInstructionOrderNumber())) {
                                                        instructionsExecuted.add(currentInstruction);
                                                    }
                                                    success = true;
                                                } else {

                                                    String message = "The Value: <b style='color:red;'>" + operations[2]
                                                            + "</b> is not "
                                                            + "<b>" + operations[1] + " "
                                                            + mapOperators.get(parentField)
                                                            + "</b> Length: (<b>"
                                                            + mapOperators
                                                                    .get(parentField)
                                                                    .length() + "</b>)"
                                                            + "<br>----------------------------------------------<br>"
                                                            + "Check the SET/GET of <b style='color:red;'>"
                                                            + operations[0]
                                                            + "</b> for <b style='color:red;'>"
                                                            + parentField + "</b>"
                                                            + "<br>Current value: <b style='color:red;'>"
                                                            + operations[2]
                                                            + "</b> Length: (<b>"
                                                            + operations[2].length() + "</b>)"
                                                            + "<br>Expected value: <b style='color:green;'>"
                                                            + mapOperators.get(parentField) + "</b> Length: (<b>"
                                                            + mapOperators
                                                                    .get(parentField)
                                                                    .length() + "</b>)";

                                                    webPage.alertMessage(message);
                                                    stopAll = true;

                                                    resultAcions = "Failed to Execute Cmd: " + lastInstructionExecuted;
                                                    success = false;
                                                }
                                            } else {
                                                String message = "GET Value is Not Defined"
                                                        + "<br>----------------------------------------------<br>"
                                                        + "Validation Error: <b style='color:red;'>" + parentField
                                                        + "</b>"
                                                        + "<br>----------------------------------------------<br>"
                                                        + "Check the GET Value for <b style='color:red;'>"
                                                        + parentField + "</b>";

                                                webPage.alertMessage(message);
                                                stopAll = true;

                                                resultAcions = "Failed to Execute Cmd: " + lastInstructionExecuted;
                                                success = false;
                                            }

                                        } else {
                                            resultAcions = "Failed to Execute Cmd: " + lastInstructionExecuted;
                                            success = false;
                                        }
                                    }

                                } catch (Throwable t) {
                                    success = false;
                                    currentInstruction.setExecuted(false);
                                    if (currentInstruction.isOptional()) {
                                        long currentInstructionEndTime = System.nanoTime();
                                        long duration = currentInstructionEndTime - botJobStartTime;
                                        ABRLogger.getInstance(WebPage.class)
                                                .fine("FAILED OPTIONAL INSTRUCTION on element: " + resultAcions
                                                        + " Cmd: "
                                                        + lastInstructionExecuted + "- Duration: "
                                                        + LocalTime.ofNanoOfDay(duration)
                                                                .format(FORMAT_TIME));
                                        writer.insertInstructionResult(
                                                currentInstruction,
                                                dataExcel,
                                                LocalTime.ofNanoOfDay(
                                                        currentInstructionEndTime - currentInstructionStartTime),
                                                "optional skipped");
                                        status = (short) ExcelReportStatusEnum.WARNING.ordinal();
                                    } else {
                                        long currentInstructionEndTime = System.nanoTime();
                                        long duration = currentInstructionEndTime - botJobStartTime;
                                        ABRLogger.getInstance(WebPage.class)
                                                .fine("FAILED MANDATORY INSTRUCTION on element: " + resultAcions
                                                        + " Cmd: "
                                                        + lastInstructionExecuted + "- Duration: "
                                                        + LocalTime.ofNanoOfDay(duration)
                                                                .format(FORMAT_TIME));
                                        writer.insertInstructionResult(
                                                currentInstruction,
                                                null,
                                                LocalTime.ofNanoOfDay(
                                                        currentInstructionEndTime - currentInstructionStartTime),
                                                "failed");
                                        status = (short) ExcelReportStatusEnum.ERROR.ordinal();
                                    }
                                    //                            throw new RuntimeException(t);
                                }
                                printLog(generateTimestamp(), logFileForSingleExcel, resultAcions, success);
                                if (!success) {
                                    //                                    countdownTextField.setStyle("-fx-font-size:
                                    // 16px; -fx-text-fill: red;");
                                    //                                    countdownTextField.setText(resultAcions);
                                    return false;
                                }
                            }
                        }
                    }
                }
            } else { //  if dataExel is NULL
                List<BlockLoadDTO> blockList = blocksLoaded;

                // Creating Dynamic Data if Default is Null
                Map<String, String> dataDynamic = new HashMap<>();
                for (int j = 0; success && j < blockList.size(); j++) {

                    // Call the method to get the filtered list
                    List<BlockLoopInstructionLoadDTO> unexecutedInstructions = getUnexecutedInstructions(
                            instructionsExecuted, blockList.get(j).getBlockLoopInstructionLoadDTOS());

                    for (BlockLoopInstructionLoadDTO currentInstruction : unexecutedInstructions) {
                        if (currentInstruction.getDefaultValue() == null) {
                            String[] arr = UtilsMethods.splitIfContains(
                                    currentInstruction.getActions(), Constants.ACTION_SPECIFICATIONS_SPLITTER);
                            if (arr.length > 1) {
                                String dataFieldName = arr[1].split(Constants.PATH_FIELD_SUBSTITUTION)[0];
                                insertRandomName(dataDynamic, dataFieldName);
                            }
                        }
                    }
                }
                for (int j = 0; success && j < blockList.size(); j++) {

                    // Call the method to get the filtered list
                    List<BlockLoopInstructionLoadDTO> unexecutedInstructions = getUnexecutedInstructions(
                            instructionsExecuted, blockList.get(j).getBlockLoopInstructionLoadDTOS());

                    for (BlockLoopInstructionLoadDTO currentInstruction : unexecutedInstructions) {
                        long currentInstructionStartTime = System.nanoTime();
                        File logFileForSingleExcel = excelReader.createLogFile(excelPath);
                        try {
                            lastInstructionExecuted = currentInstruction.getName()
                                    + Constants.BLANK_STRING
                                    + currentInstruction.getPath();
                            resultAcions = webPage.performActions(
                                    dataDynamic,
                                    currentInstruction,
                                    botJobId,
                                    blockList.get(j).getName());
                            long currentInstructionEndTime = System.nanoTime();
                            totalExecutionTime += currentInstructionEndTime - currentInstructionStartTime;
                            if (resultAcions != null) {

                                ABRLogger.getInstance(WebPage.class)
                                        .fine("SUCCESSFUL INSTRUCTION on element: " + resultAcions + " Cmd: "
                                                + lastInstructionExecuted);

                                currentInstruction.setExecuted(true);
                                success = true;
                            } else {
                                resultAcions = "Failed to Execute -> " + currentInstruction.getName();
                                success = false;
                            }
                            writer.insertInstructionResult(
                                    currentInstruction,
                                    dataDynamic,
                                    LocalTime.ofNanoOfDay(currentInstructionEndTime - currentInstructionStartTime),
                                    success ? "success" : "failed");
                        } catch (Throwable t) {
                            success = false;
                            currentInstruction.setExecuted(false);
                            if (currentInstruction.isOptional()) {
                                long currentInstructionEndTime = System.nanoTime();
                                long duration = currentInstructionEndTime - botJobStartTime;
                                ABRLogger.getInstance(WebPage.class)
                                        .fine("FAILED OPTIONAL INSTRUCTION on element: " + resultAcions
                                                + " Cmd: "
                                                + lastInstructionExecuted + "- Duration: "
                                                + LocalTime.ofNanoOfDay(duration)
                                                        .format(FORMAT_TIME));
                                writer.insertInstructionResult(
                                        currentInstruction,
                                        dataDynamic,
                                        LocalTime.ofNanoOfDay(currentInstructionEndTime - currentInstructionStartTime),
                                        "optional skipped");
                                status = (short) ExcelReportStatusEnum.WARNING.ordinal();
                            } else {
                                long currentInstructionEndTime = System.nanoTime();
                                long duration = currentInstructionEndTime - botJobStartTime;
                                ABRLogger.getInstance(WebPage.class)
                                        .fine("FAILED MANDATORY INSTRUCTION on element: " + resultAcions
                                                + " Cmd: "
                                                + lastInstructionExecuted + "- Duration: "
                                                + LocalTime.ofNanoOfDay(duration)
                                                        .format(FORMAT_TIME));
                                writer.insertInstructionResult(
                                        currentInstruction,
                                        null,
                                        LocalTime.ofNanoOfDay(currentInstructionEndTime - currentInstructionStartTime),
                                        "failed");
                                status = (short) ExcelReportStatusEnum.ERROR.ordinal();
                            }
                            //                            throw new RuntimeException(t);
                        }
                        printLog(generateTimestamp(), logFileForSingleExcel, resultAcions, success);
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
                report.setStatus(status);
                report.setDuration(totalExecutionTime / 100);
                writer.insertTotalExecutionTimes(botJobStartTime, System.nanoTime());
                repository.write(report);
            }
            printBaseLog(baseLogFile, generateTimestamp(), baseLogString);
            return true;
        } catch (Throwable t) {
            //            t.printStackTrace();
            ABRLogger.getInstance(Engine.class).severe("Error Executing JOB \n" + t.getMessage());
            return false;
        }
    }

    private static String generateTimestamp() {
        Date date = new Date();
        dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        return dateFormatter.format(date);
    }

    private static void printLog(String timeStamp, File logFile, String resultAcions, boolean result) {
        String resultMsg = result ? Constants.SUCCESS : Constants.FAIL;
        String log = String.join(Constants.FIELDS_SEPARATOR, timeStamp, resultMsg, resultAcions);

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

    public static String generateRandomName() {
        int length = RANDOM.nextInt(MAX_LENGTH - MIN_LENGTH + 1) + MIN_LENGTH;
        StringBuilder nameBuilder = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            char randomChar = CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length()));
            nameBuilder.append(randomChar);
        }

        return nameBuilder.toString();
    }

    public static void insertRandomName(Map<String, String> map, String key) {
        String randomName = generateRandomName();
        map.put(key, randomName);
    }

    private static void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static List<BlockLoopInstructionLoadDTO> getUnexecutedInstructions(
            List<BlockLoopInstructionLoadDTO> instructionsExecuted, List<BlockLoopInstructionLoadDTO> otherList) {
        // Create a set of instructionOrderNumbers from instructionsExecuted
        Set<Integer> executedInstructionOrderNumbers = instructionsExecuted.stream()
                .map(BlockLoopInstructionLoadDTO::getInstructionOrderNumber)
                .collect(Collectors.toSet());

        // Filter the otherList to get instructions where executed is false and not in executedInstructionOrderNumbers
        return otherList.stream()
                //                .filter(instruction -> instruction.getExecuted() != null &&
                // !instruction.getExecuted())
                .filter(instruction ->
                        !executedInstructionOrderNumbers.contains(instruction.getInstructionOrderNumber()))
                .collect(Collectors.toList());
    }

    private static Connection getConnection() {
        if (!POSTGRES_DB) {
            if (conn == null) {
                String dbPath = ABRPropertyManager.getInstance().getProperty(ABRPropertyEnum.FOLDER_PATH_DB);
                String dbUrl = CONNECTION_TYPE + dbPath + ABRConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;
                try {
                    conn = DriverManager.getConnection(dbUrl);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            return conn;
        } else {

            if (conn == null) {
                String dbUrl = CONNECTION_POSTGRES + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;
                try {
                    conn = DriverManager.getConnection(dbUrl, USERNAME, PASSWORD);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            return conn;
        }
    }

    private static void loadBlockAll(int botJobId) {
        String query = "SELECT bj.id AS bot_job_id, bj.name AS bot_job_name, "
                + " b.id AS block_id, b.block_order_number, b.name AS block_name, "
                + " b.description AS block_description, b.type_id, "
                + " bli.id AS block_loop_instruction_id, bli.instruction_order_number, "
                + " bli.actions, bli.name AS instruction_name, bli.path, bli.description AS instruction_description, "
                + " bli.optional, bli.block_marked, bli.default_val, bli.action_custom_max_wait_sec, "
                + " bli.on_hold_seconds, bli.encrypted, bli.export_to_abr, "
                + " irl.reference_type, irl.value, "
                + "  bli.operation, bli.parent_id "
                + " FROM bot_job bj "
                + " LEFT JOIN block b ON b.bot_job_id = bj.id "
                + " LEFT JOIN block_loop_instruction bli ON bli.block_id = b.id "
                + " LEFT JOIN instruction_reference irl ON irl.block_loop_instruction_id = bli.id "
                + " where bot_job_id = " + botJobId
                + "  ORDER BY bj.id, b.block_order_number, bli.instruction_order_number, irl.id ASC";

        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            Map<Integer, BotJobLoadDTO> botJobMap = new HashMap<>();
            Map<Integer, BlockLoadDTO> blockMap = new HashMap<>();
            Map<Integer, BlockLoopInstructionLoadDTO> instructionMap = new HashMap<>();

            botLoadJobs.clear();

            while (rs.next()) {
                botJobId = rs.getInt("bot_job_id");
                BotJobLoadDTO botJobDTO = botJobMap.get(botJobId);

                if (botJobDTO == null) {
                    botJobDTO = new BotJobLoadDTO();
                    botJobDTO.setId(botJobId);
                    botJobDTO.setName(rs.getString("bot_job_name"));
                    botJobDTO.setBlockLoadDTOList(new ArrayList<>());
                    botJobMap.put(botJobId, botJobDTO);
                    botLoadJobs.add(botJobDTO);
                }

                int blockId = rs.getInt("block_id");
                BlockLoadDTO blockDTO = blockMap.get(blockId);

                if (blockDTO == null) {
                    blockDTO = new BlockLoadDTO();
                    blockDTO.setId(blockId);
                    blockDTO.setBlockOrderNumber(rs.getInt("block_order_number"));
                    blockDTO.setName(rs.getString("block_name"));
                    blockDTO.setDescription(rs.getString("block_description"));
                    blockDTO.setTypeId(rs.getInt("type_id"));
                    blockDTO.setBotJobLoadDTO(botJobDTO);

                    blockDTO.setBlockLoopInstructionLoadDTOS(new ArrayList<>());
                    botJobDTO.getBlockLoadDTOList().add(blockDTO);
                    blockMap.put(blockId, blockDTO);
                }

                int instructionId = rs.getInt("block_loop_instruction_id");
                BlockLoopInstructionLoadDTO instruction = instructionMap.get(instructionId);

                if (instruction == null) {
                    instruction = new BlockLoopInstructionLoadDTO();
                    instruction.setId(instructionId);
                    instruction.setInstructionOrderNumber(rs.getInt("instruction_order_number"));
                    instruction.setActions(rs.getString("actions"));
                    instruction.setName(rs.getString("instruction_name"));
                    instruction.setPath(rs.getString("path"));
                    instruction.setDescription(rs.getString("instruction_description"));
                    instruction.setOptional(rs.getInt("optional"));
                    instruction.setBlockMarked(rs.getBoolean("block_marked"));
                    instruction.setDefault_val(rs.getString("default_val"));
                    instruction.setActionCustomMaxWaitSec(rs.getInt("action_custom_max_wait_sec"));
                    instruction.setOnHoldSeconds(rs.getInt("on_hold_seconds"));
                    instruction.setEncrypted(rs.getInt("encrypted"));
                    instruction.setExportToABR(rs.getInt("export_to_abr"));
                    instruction.setOperation(rs.getString("operation"));
                    instruction.setParentId(rs.getInt("parent_id"));

                    instruction.setInstructionReferenceLoadDTOList(new ArrayList<>());
                    blockDTO.getBlockLoopInstructionLoadDTOS().add(instruction);
                    instructionMap.put(instructionId, instruction);
                }

                String referenceType = rs.getString("reference_type");
                if (referenceType != null) {
                    InstructionReferenceLoadDTO reference = new InstructionReferenceLoadDTO();
                    reference.setReferenceType(referenceType);
                    reference.setValue(rs.getString("value"));
                    instruction.getInstructionReferenceLoadDTOList().add(reference);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //    private void fillUpCurretLocators(BlockLoopInstructionLoadDTO currentInstruction) {
    //        for (InstructionReferenceLoadDTO reference : currentInstruction.getInstructionReferenceLoadDTOList()) {
    //            switch (reference.getReferenceType()) {
    //                case "absolutXPath":
    //                    absolutXPathTextField.setText(reference.getValue());
    //                    break;
    //                case "currentXPath":
    //                    currentXPathTextField.setText(reference.getValue());
    //                    break;
    //                case "coords":
    //                    coordsTextField.setText(reference.getValue());
    //                    break;
    //                case "customXPath":
    //                    customXPathTextField.setText(reference.getValue());
    //                    break;
    //                default:
    //                    System.out.println("Unknown reference type: " + reference.getReferenceType());
    //            }
    //        }
    //    }

}
