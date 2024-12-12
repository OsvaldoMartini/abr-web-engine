package com.allinweb.ch;

import com.allinweb.ch.driver.ABRWebDriver;
import com.allinweb.ch.dto.*;
import com.allinweb.ch.facade.PerformActions;
import com.allinweb.ch.readersAndWriters.ExcelReader;
import com.allinweb.ch.readersAndWriters.ExcelWriter;
import com.allinweb.ch.supportTypes.ExtractedData;
import com.allinweb.ch.supportTypes.WebPage;
import com.allinweb.ch.util.*;
import com.google.common.base.Strings;
import io.opentelemetry.api.internal.StringUtils;
import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javafx.scene.control.Alert;
import javafx.util.Pair;
import javax.swing.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Engine {
    private static SimpleDateFormat dateFormatter;
    static final String EXECUTE_JOB = "execute/j";
    static final String FORM_RECOGNITION = "form/r";
    static final String TEST = "test";
    public static Repository repository;
    private static final String language = "en";
    private static File baseLogFile = null;

    private static Map<String, String> mapOperators;
    private static Map<String, String> mapExport;

    private static final DateTimeFormatter FORMAT_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static Connection conn = null;

    private static final String CONNECTION_TYPE = "jdbc:ucanaccess://";
    private static final String CONNECTION_PARAMETERS = ";memory=false;newDatabaseVersion=V2010";

    // Postgres
    private static boolean POSTGRES_DB = false;
    private static final String CONNECTION_POSTGRES = "jdbc:postgresql://";
    private static final String DB_HOST = "localhost"; // or your PostgreSQL server address
    private static final String DB_PORT = "5432"; // default PostgreSQL port
    private static final String DB_NAME = "abr_web"; // your database name
    private static final String USERNAME = "postgres"; // your database username
    private static final String PASSWORD = "martini"; // your database password

    private static SessionFactory sessionFactory = null;
    private static Session session = null;

    private static List<BotJobLoadDTO> botLoadJobs = new ArrayList<>();
    static List<BlockLoopInstructionLoadDTO> instructionsExecuted = new ArrayList<>();
    static List<Integer> executedSuccess = new ArrayList<>();
    Map<String, WebElement> mapAdvanced = new HashMap<>();

    private static final PerformActions performAction;
    private static ABRPriorities abrPriorities;

    // Static block to initialize
    static {
        performAction = PerformActions.getInstance();
        abrPriorities = ABRPriorities.getInstance();
    }

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

        changeDbConnection();

        repository = new Repository(sessionFactory);

        try {
            baseLogFile = new File(ABRPropertyManager.getInstance().getProperty(ABRPropertyEnum.FOLDER_PATH_LOG)
                    + ABRConstants.FILE_NAME_ENGINE_BASE_LOG);
        } catch (Exception e) {
            ABRLogger.getInstance(WebPage.class).severe("baseLogFile Error: " + e.getMessage());
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
            ABRLogger.getInstance(WebPage.class).severe("Main class Start Error: " + e.getMessage());
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
        int executeSpecificBlock = -1;

        try {
            homeBankingId = Integer.parseInt(idsAndPaths[0]);
            botJobId = Integer.parseInt(idsAndPaths[1]);
        } catch (Exception e) {
            throw new Exception("no reference (id) for home banking or bot job");
        }

        //  Specific Selected Block to Execute If Have
        try {
            executeSpecificBlock = Integer.parseInt(idsAndPaths[2]);
            System.out.println("Running Block Id: " + executeSpecificBlock);
        } catch (Exception e) {
            System.out.println("Running All Blocks");
        }

        HomeBankingDTO homeBankingDTO = loadHomeBanking(homeBankingId);

        if (homeBankingDTO == null || StringUtils.isNullOrEmpty(homeBankingDTO.getUrl())) {
            ABRLogger.getInstance(Engine.class).severe("Cannot find Home Banking Environment Id:" + homeBankingId);
            return false;
        }

        loadBlockAll(botJobId);

        if (botLoadJobs.size() < 1) {
            ABRLogger.getInstance(Engine.class).severe("Cannot find Bot Jobs with this Id:" + botJobId);
            return false;
        }

        List<BlockLoadDTO> blocksLoaded = botLoadJobs.get(0).getBlockLoadDTOList();

        String excelPath = idsAndPaths[2];

        // Assuming blocksLoaded is your List<BlockLoadDTO>
        List<String> allActions = blocksLoaded.stream()
                .flatMap(
                        blockLoadDTO -> blockLoadDTO
                                .getBlockLoopInstructionLoadDTOS()
                                .stream()) // Flatten the stream of BlockLoopInstructionLoadDTO
                .map(BlockLoopInstructionLoadDTO::getActions) // Extract the actions
                .collect(Collectors.toList()); // Collect all actions into a List

        ExcelReader excelReader = new ExcelReader();
        ExtractedData extractedData = null;
        try {
            extractedData = excelReader.extractData(excelPath, allActions);
        } catch (Exception e) {

            performAction.errorMessage(
                    "Excel Error", "Could Not Execute Excel File", "Check All Excel Columns and Values!", null, null);

            //            Platform.exit();
        }

        try {

            //            String browser = ABRPropertyManager.getInstance().getProperty(ABRPropertyEnum.BROWSER);
            //            WebPage webPage = new WebPage(
            //                    browser,
            //                    homeBankingDTO.getUrl(),
            //                    homeBankingDTO.getPriority(),
            //                    homeBankingDTO.getOptionsConfig(),
            //                    mapOperators);

            ABRWebDriver abrWebDriver = new ABRWebDriver();
            abrWebDriver.openDriver(homeBankingDTO.getUrl(), homeBankingDTO.getOptionsConfig());

            // Ensure botJob and abrPriorities are not null before accessing their methods
            if (botLoadJobs.get(0) != null && abrPriorities != null) {
                // Check if we need to update abrPriorities
                if (abrPriorities.getJobId() == null
                        || !abrPriorities.getJobId().equals(botLoadJobs.get(0).getId())) {
                    // Set Job ID in abrPriorities
                    abrPriorities.setJobId(botLoadJobs.get(0).getId());

                    // Check for non-null HomeBanking and Priority
                    if (homeBankingDTO != null) {
                        String priorityValue = homeBankingDTO.getPriority();
                        String searchConfig = homeBankingDTO.getSearchConfig();

                        if (priorityValue != null) {
                            abrPriorities.loadPrioritiesFromString(priorityValue);
                        } else {
                            abrPriorities.loadPriorities();
                        }

                        abrPriorities.loadSearchElementsConfig(searchConfig);
                    }

                    // Initialize performAction with abrPriorities and abrWebDriver
                    performAction.initializePerformActions(abrPriorities, abrWebDriver);
                }
            }
            if (performAction.waitForPage == null) {
                String updateTimeout =
                        ABRPropertyManager.getInstance().getProperty(ABRPropertyEnum.WEBDRIVER_PAGE_UPDATE_TIMEOUT_SEC);
                String interactionTimeout =
                        ABRPropertyManager.getInstance().getProperty(ABRPropertyEnum.WEBDRIVER_PAGE_UPDATE_TIMEOUT_SEC);
                performAction.waitForPage = new WebDriverWait(
                        abrWebDriver.getDriver(), Duration.ofSeconds(Integer.parseInt(updateTimeout)));
                performAction.waitForAction = new WebDriverWait(
                        abrWebDriver.getDriver(), Duration.ofSeconds(Integer.parseInt(interactionTimeout)));
            }

            String botJobName = botLoadJobs.get(0).getName();

            String baseLogString = blocksLoaded.get(0).getBotJobName()
                    + Constants.FIELDS_SEPARATOR
                    + labelsValue.getProperty(Labels.START);

            printBaseLog(baseLogFile, generateTimestamp(), baseLogString);

            ExcelWriter.ExcelChain writerReport = new ExcelWriter(
                            botLoadJobs.get(0).getName(), abrWebDriver.getDriver(), false)
                    .withPurpose("report");
            writerReport.insertReportHead();

            ExcelWriter.ExcelChain writerExport = null;
            boolean excelExportOnceCreation = true;
            //            writerExport.insertReportHead();

            boolean refreshOnly = false;
            boolean refreshLoopExecuted = false;
            boolean ignoreRefreshLoop = false;
            Set<String> mapIgnore = new HashSet<>();
            int[] refreshLoopArray = null; // new int[] {0, 0, 0};

            boolean searchByJavaScript = false;

            boolean byPassNotFound = false;
            boolean success = true;
            boolean stopAll = false;
            long botJobStartTime = System.nanoTime();
            long totalExecutionTime = 0;
            String resultActions = "No instruction executed yet";
            String extraMsg = "";
            short status = (short) ExcelReportStatusEnum.ERROR.ordinal();
            Map<String, String> dataExcel = null;

            //            ExcelReportDTO report = new ExcelReportDTO();
            //            report.setOrder((short) botLoadJobs.get(0).getId());
            //            report.setStartDate(LocalDateTime.now());
            //            report.setBatchJobId(selectedJob.getId());
            //            report.setBotJobDTO(selectedJob);
            //            report.setStatus((short) ExcelReportStatusEnum.NOT_RUN.ordinal());

            mapOperators = new HashMap<>();
            mapExport = new LinkedHashMap<>();
            int executionTimes = 0;
            int execLimitReach = 0;
            String limitReach = ABRPropertyManager.getInstance().getProperty(ABRPropertyEnum.BLOCK_EXEC_LIMIT);
            if (limitReach != null) {
                execLimitReach = Integer.parseInt(limitReach);
            }

            Map<String, String> mapSavedLocators = new HashMap<>();

            Set<Integer> parentIdsForRefreshLoop = null;
            int exportIndex = 1;
            if (extractedData.getNumberOfDataRows() > 0) {

                // Execute All Blocks starting from executeSpecificBlock if Defined
                int currentBlock = (executeSpecificBlock > -1) ? executeSpecificBlock - 1 : 0;

                //            while ((executeSpecificBlock > -1
                //                    && currentBlock == executeSpecificBlock - 1) // Execute specific block only
                //                    || (executeSpecificBlock == -1 && currentBlock <= blocksLoaded.size() - 1) //
                // Execute
                // all blocks
                //                    && blocksLoaded.size() > 0
                //                    && !stopAll
                //                    && executionTimes < execLimitReach) {

                outerLoop:
                while (currentBlock <= blocksLoaded.size() - 1
                        && blocksLoaded.size() > 0
                        && !stopAll
                        && executionTimes < execLimitReach) {
                    instructionsExecuted.clear();
                    BlockLoadDTO blockLoad = blocksLoaded.get(currentBlock);
                    String excelFieldName = blockLoad.getExportFile();
                    String blockName = blocksLoaded.get(currentBlock).getName();
                    int blockWait = blocksLoaded.get(currentBlock).getWait() > 0
                            ? blocksLoaded.get(currentBlock).getWait()
                            : 2;
                    boolean blockActive = blocksLoaded.get(currentBlock).isActive();

                    long blockStartTime = System.nanoTime();

                    if (!blockActive) {
                        currentBlock++;

                        Pair<String, String> msgBlock =
                                new Pair(String.format("Ignore: \"%s\"", blockLoad.getName()), ABRConstants.IGNORE);
                        long duration = performAction.duration(blockStartTime);
                        performAction.excelReportWrite(
                                success,
                                new String[] {ABRConstants.IGNORE},
                                msgBlock,
                                duration,
                                dataExcel,
                                writerReport);
                        totalExecutionTime += duration;

                        status = performAction.operationLog(
                                success,
                                "BLOCK IGNORED",
                                String.format("Block: \"%s\" is Inactive: ", blockName),
                                duration);

                        continue;
                    }

                    try {

                        performAction.onHoldInSeconds(blockWait);
                        ABRLogger.getInstance(Engine.class)
                                .info(String.format(
                                        "Default Wait for Block: \"%s\" ->  %d Seconds",
                                        blockLoad.getName(), blockWait));

                        long duration = performAction.duration(blockStartTime);
                        Pair<String, String> msgBlock = new Pair(
                                String.format("Default Wait: \"%s\" ->  %d Seconds", blockLoad.getName(), blockWait),
                                ABRConstants.HOLD);
                        performAction.excelReportWrite(
                                success, new String[] {ABRConstants.HOLD}, msgBlock, duration, dataExcel, writerReport);
                        totalExecutionTime += duration;

                        status = performAction.operationLog(
                                success,
                                "BLOCK DEFAULT WAIT",
                                "Block Default Wait " + blockWait + " Seconds",
                                duration);
                    } catch (Exception ex) {
                        ABRLogger.getInstance(Engine.class)
                                .severe(String.format("Error Wait Block for :\"%s\"", blockLoad.getName()));
                    }

                    // Step 1: Filter rows where actions = "REFRESH_LOOP" and collect their parent IDs
                    parentIdsForRefreshLoop = blocksLoaded.get(currentBlock).getBlockLoopInstructionLoadDTOS().stream()
                            .filter(instruction -> "REFRESH_LOOP".equalsIgnoreCase(instruction.getActions()))
                            .map(BlockLoopInstructionLoadDTO::getParentId)
                            .collect(Collectors.toSet());

                    executionTimes++;
                    boolean jumpGoto = false;

                    for (int i = 0; success && i < extractedData.getNumberOfDataRows() && !stopAll; i++) {
                        boolean ifClause = false;
                        boolean ifFailed = false;
                        boolean ifIsDone = false;
                        boolean elseClause = false;
                        boolean elseFailed = false;
                        boolean byPassFlagLoop = false;
                        mapExport.clear();
                        //                    writerReport.insertBlockSeparation(blockLoad.getName());

                        dataExcel = extractedData.getRowFieldValues(i);

                        int[] instructionIds = blockLoad.getBlockLoopInstructionLoadDTOS().stream()
                                .mapToInt(BlockLoopInstructionLoadDTO::getId)
                                .toArray();

                        int currentIndex = 0;

                        while (currentIndex < instructionIds.length && !stopAll) {

                            BlockLoopInstructionLoadDTO currentInstruction =
                                    blockLoad.getBlockLoopInstructionLoadDTOS().get(currentIndex);

                            mapSavedLocators.clear();

                            // Loop through the instructionReferenceLoadDTOList
                            if (currentInstruction.getInstructionReferenceLoadDTOList() != null) {
                                for (InstructionReferenceLoadDTO reference :
                                        currentInstruction.getInstructionReferenceLoadDTOList()) {
                                    // Populate the map with referenceType as the key and value as the value
                                    mapSavedLocators.put(reference.getReferenceType(), reference.getValue());
                                }
                            }

                            currentIndex++;

                            // Allow Re-Execute Instructions in Previous Blocks
                            //                        if (currentInstruction.getExecuted() == null ||
                            // !currentInstruction.getExecuted()) {
                            boolean execOperation = false;
                            boolean checkOperation = false;
                            boolean excelWriteOperation = false;

                            String xPathOperation = null;
                            String parentField = null;
                            String fieldName = null;
                            int parentId = currentInstruction.getParentId();

                            if (mapIgnore.contains(currentInstruction.getId() + "-" + currentInstruction.getName())) {
                                continue;
                            }

                            //                        String[] operation =
                            // UtilsMethods.splitIfContains(instruction.getOperation(),
                            // Constants.ACTION_SPECIFICATIONS_SPLITTER);
                            String[] actions =
                                    currentInstruction.getActions().split(Constants.ACTION_SPECIFICATIONS_SPLITTER);
                            String[] operations = currentInstruction.getOperation() != null
                                    ? currentInstruction.getOperation().split(Constants.ACTION_SPECIFICATIONS_SPLITTER)
                                    : null;

                            // Case for Inputs
                            String valueInsert = "No Data Found";
                            if (actions[0].equalsIgnoreCase(ABRConstants.INSERT)) {
                                String reference = actions[1];
                                valueInsert = dataExcel.get(reference);
                            }

                            Pair<String, String> msgInitial = new Pair(
                                    currentInstruction.getName(),
                                    (currentInstruction.getOperation() != null
                                            ? currentInstruction.getOperation()
                                            : (actions[0].equalsIgnoreCase(ABRConstants.INSERT)) ? valueInsert : ""));

                            resultActions = performAction.actionResultMessage(blockName, actions, msgInitial);

                            extraMsg = "";

                            boolean isEndIf = actions[0].equalsIgnoreCase(ABRConstants.ENDIF);

                            if ((ifIsDone && !isEndIf) || isEndIf) {

                                if (isEndIf) {
                                    ifIsDone = false;
                                }
                                continue;
                            }

                            if (actions[0].equalsIgnoreCase(ABRConstants.PAUSE)) {

                                ABRLogger.getInstance(Engine.class)
                                        .info(String.format("PAUSE BOT JOB at Block Name:\"%s\"", blockLoad.getName()));

                                long currentInstructionStartTime = System.nanoTime();

                                //                                SwingUtilities.invokeLater(() ->
                                performAction.showCustomModalDialog(
                                        "PAUSE BOT JOB",
                                        String.format("PAUSE BOT JOB at Block Name:\"%s\"", blockLoad.getName()),
                                        " Please click OK to continue!");
                                //
                                long duration = performAction.duration(currentInstructionStartTime);

                                performAction.excelReportWrite(
                                        success, actions, msgInitial, duration, dataExcel, writerReport);

                                totalExecutionTime += duration;

                                continue;

                            } else if (actions[0].equalsIgnoreCase(ABRConstants.IF)) {

                                ABRLogger.getInstance(Engine.class)
                                        .info("Initial Execution { IF -> ELSE} ->  inside Block :\""
                                                + blockLoad.getName() + "\"");

                                ifClause = true;
                                ifFailed = false; // Reset failure status for this IF clause
                                ifIsDone = false;
                                continue;
                            }

                            if (ifClause && ifFailed && !ifIsDone) {

                                if (actions[0].equalsIgnoreCase(ABRConstants.ELSE)) {
                                    ABRLogger.getInstance(Engine.class)
                                            .warning("Closing Block { IF -> ELSE} -> Failed Execution inside Block :\""
                                                    + blockLoad.getName() + "\"");

                                    ifClause = false;
                                    ifFailed = false;
                                    elseClause = true;
                                    elseFailed = false; // Reset failure status for this ELSE clause
                                    continue;
                                } else {
                                    // Skip until ELSE is found
                                    continue;
                                }
                            } else if (elseClause && elseFailed) {
                                if (actions[0].equalsIgnoreCase(ABRConstants.ENDIF)) {
                                    ABRLogger.getInstance(Engine.class)
                                            .warning(
                                                    "Closing Block { ELSE -> ENDIF } -> Failed Execution inside Block :\""
                                                            + blockLoad.getName() + "\"");

                                    elseClause = false;
                                    elseFailed = false; // Reset failure status for this ELSE clause
                                    continue;
                                } else {
                                    // Skip until ELSE is found
                                    continue;
                                }
                            }

                            // AND SOME RESON JUMPED INTO A FIELD INSIDE OF THE IF STATEMENT
                            if ((ifClause && !ifFailed && !ifIsDone && actions[0].equalsIgnoreCase(ABRConstants.ELSE))
                                    || (refreshLoopArray != null
                                            && !ifClause
                                            && !ifFailed
                                            && !ifIsDone
                                            && actions[0].equalsIgnoreCase(ABRConstants.ELSE))) {

                                ABRLogger.getInstance(Engine.class)
                                        .info("Closing Block { IF -> ELSE } -> Success Execution inside Block :\""
                                                + blockLoad.getName() + "\"");

                                ifClause = false;
                                if (!ifFailed) {
                                    ifFailed = false;
                                    elseClause = false;
                                    ifIsDone = true;
                                } else {
                                    ifFailed = false;
                                    elseClause = true;
                                }
                                elseFailed = false; // Reset failure status for this ELSE clause
                                continue;
                            }

                            // Process ENDIF to reset flags and resume normal flow after IF-ELSE blocks
                            if (elseClause && !elseFailed && actions[0].equalsIgnoreCase(ABRConstants.ENDIF)) {

                                ABRLogger.getInstance(Engine.class)
                                        .info("Closing Block { ELSE -> ENDIF } -> Success Execution inside Block :\""
                                                + blockLoad.getName() + "\"");

                                elseClause = false;
                                elseFailed = false;
                                continue;
                            }
                            //                        else if (ifIsDone &&
                            // !actions[0].equalsIgnoreCase(ABRConstants.ENDIF))
                            // {
                            //                            continue;
                            //                        }

                            // Process ENDIF to reset flags and resume normal flow after IF-ELSE blocks
                            if (!ifClause && !ifFailed && !elseClause && !elseFailed && !isEndIf && ifIsDone) {
                                ifIsDone = false;
                                ABRLogger.getInstance(Engine.class)
                                        .info("Skipping { ENDIF } -> Success Skipping inside Block :\""
                                                + blockLoad.getName() + "\"");

                                continue;
                            }

                            if (actions[0].equalsIgnoreCase(ABRConstants.GOTO)) {
                                jumpGoto = true;

                            } else if (actions[0].equalsIgnoreCase(ABRConstants.GET_VALUE)
                                    || actions[0].equalsIgnoreCase(ABRConstants.SET_VALUE)) {

                                execOperation = true;
                                xPathOperation = performAction.getXPathInstruction(currentInstruction, blockLoad);

                                parentField = performAction.getInstructionParentField(currentInstruction, blockLoad);

                                if (xPathOperation != null && parentField != null) {
                                    fieldName = parentField;
                                    parentField = parentId + "-" + parentField;

                                } else {
                                    resultActions = performAction.parentIdWrongBlock(
                                            currentInstruction, blockLoad, ifClause, elseClause);

                                    if (!ifClause && !elseClause) {
                                        stopAll = true;
                                        success = false;
                                    } else if (ifClause) {
                                        ifFailed = true;
                                    } else if (elseClause) {
                                        elseFailed = true;
                                    }

                                    if (stopAll) {
                                        break;
                                    }
                                }

                            } else if (actions[0].equalsIgnoreCase(ABRConstants.CHECK_VALUE)) {

                                checkOperation = true;
                                parentField = performAction.getInstructionParentField(currentInstruction, blockLoad);

                                if (parentField != null) {
                                    fieldName = parentField;
                                    parentField = parentId + "-" + parentField;

                                } else {
                                    resultActions = performAction.getValueIsNotDefinedEngine(
                                            currentInstruction, resultActions, ifClause, elseClause);

                                    if (!ifClause && !elseClause) {
                                        stopAll = true;
                                        success = false;
                                    } else if (ifClause) {
                                        ifFailed = true;
                                    } else if (elseClause) {
                                        elseFailed = true;
                                    }

                                    if (stopAll) {
                                        break;
                                    }
                                }
                            } else if (actions[0].equalsIgnoreCase(ABRConstants.EXTRACT_FIELD)) {

                                excelWriteOperation = true;

                                parentField = performAction.getInstructionParentField(currentInstruction, blockLoad);
                                if (parentField != null) {

                                    fieldName = parentField;
                                    parentField = parentId + "-" + parentField;
                                } else {
                                    resultActions = performAction.getValueIsNotDefinedEngine(
                                            currentInstruction, resultActions, ifClause, elseClause);

                                    if (!ifClause && !elseClause) {
                                        stopAll = true;
                                        success = false;
                                    } else if (ifClause) {
                                        ifFailed = true;
                                    } else if (elseClause) {
                                        elseFailed = true;
                                    }

                                    if (stopAll) {
                                        break;
                                    }
                                }

                            } else if (actions[0].equalsIgnoreCase(ABRConstants.REFRESH_ONLY)) {

                                ABRLogger.getInstance(Engine.class)
                                        .info("Refresh Current Web Page ->  inside Block :\"" + blockLoad.getName()
                                                + "\"");

                                refreshOnly = true;
                            } else if (!ignoreRefreshLoop
                                    && refreshLoopArray == null
                                    && actions[0].equalsIgnoreCase(ABRConstants.REFRESH_LOOP)) {

                                ABRLogger.getInstance(Engine.class)
                                        .info("Refresh Loop Current Web Page ->  inside Block :\"" + blockLoad.getName()
                                                + "\"");

                                parentField = performAction.getInstructionParentField(currentInstruction, blockLoad);

                                if (parentField != null) {
                                    fieldName = parentField;
                                    parentField = parentId + "-" + parentField;

                                    String[] splitArray =
                                            currentInstruction.getOperation().split(":");
                                    refreshLoopArray = Arrays.stream(splitArray)
                                            .mapToInt(Integer::parseInt)
                                            .toArray();

                                    refreshLoopArray = performAction.addElementToArray(refreshLoopArray, parentId);

                                    if (refreshLoopArray != null && refreshLoopArray.length > 2) {

                                        int index = -1; // Initialize to -1 to indicate not found

                                        for (int x = 0; x < instructionIds.length; x++) {
                                            if (instructionIds[x] == refreshLoopArray[2]) {
                                                index = x;
                                                break; // Exit the loop once the value is found
                                            }
                                        }

                                        if (index != -1) {
                                            refreshLoopArray = performAction.addElementToArray(refreshLoopArray, index);
                                            //                                        currentIndex = index;
                                        } else {
                                            refreshLoopArray = null;

                                            resultActions = performAction.parentValueIsNotDefined(
                                                    "REFRESH_LOOP", refreshLoopArray[2], resultActions);

                                            ABRLogger.getInstance(Engine.class)
                                                    .severe(String.format("Error: \"%s\"", resultActions));

                                            stopAll = true;
                                            success = false;

                                            if (stopAll) {
                                                break;
                                            }
                                        }
                                    }

                                } else {
                                    resultActions = performAction.parentValueIsNotDefined(
                                            currentInstruction.getName(),
                                            currentInstruction.getParentId(),
                                            resultActions);

                                    refreshLoopArray = null;

                                    if (!ifClause && !elseClause) {
                                        stopAll = true;
                                        success = false;
                                    } else if (ifClause) {
                                        ifFailed = true;
                                    } else if (elseClause) {
                                        elseFailed = true;
                                    }

                                    if (stopAll) {
                                        break;
                                    }
                                }
                            } else if (ignoreRefreshLoop && actions[0].equalsIgnoreCase(ABRConstants.REFRESH_LOOP)) {
                                mapIgnore.add(currentInstruction.getId() + "-" + currentInstruction.getName());
                                ignoreRefreshLoop = false;
                                continue;
                            } else if (!ignoreRefreshLoop
                                    && refreshLoopArray != null
                                    && refreshLoopArray[1] > -1
                                    && actions[0].equalsIgnoreCase(ABRConstants.REFRESH_LOOP)) {

                                refreshLoopArray[1] = refreshLoopArray[1] - 1;

                                if (refreshLoopArray[1] > -1) {
                                    currentIndex = refreshLoopArray[3];
                                    continue;
                                }
                            }
                            long currentInstructionStartTime = System.nanoTime();
                            File logFileForSingleExcel = excelReader.createLogFile(excelPath);

                            // fillUpCurretLocators(currentInstruction);

                            try {
                                if (jumpGoto) {

                                    try {
                                        int blockOrderNumber = blocksLoaded.stream()
                                                .filter(block -> block.getId()
                                                        == currentInstruction.getParentId()) // Filter by blockId
                                                .findFirst() // Get the first matching block
                                                .map(BlockLoadDTO::getBlockOrderNumber) // Map to blockOrderNumber
                                                .orElseThrow(() -> new NoSuchElementException(
                                                        "No block found with the given blockId")); // Handle if no
                                        // block is found
                                        currentBlock = blockOrderNumber - 1;
                                        currentInstruction.setExecuted(true);

                                        // Assuming currentInstruction and instructionsExecuted are already defined
                                        if (currentInstruction != null
                                                && instructionsExecuted.stream()
                                                        .noneMatch(
                                                                instruction -> instruction.getInstructionOrderNumber()
                                                                        == currentInstruction
                                                                                .getInstructionOrderNumber())) {
                                            instructionsExecuted.add(currentInstruction);
                                        }

                                        executedSuccess.add(currentInstruction.getId());
                                        success = true;
                                    } catch (Exception ex) {
                                        resultActions = "Failed " + resultActions;

                                        success = false;

                                        resultActions = performAction.blockGotoFailed(resultActions);
                                    }

                                    long duration = performAction.duration(currentInstructionStartTime);
                                    performAction.excelReportWrite(
                                            success, actions, msgInitial, duration, dataExcel, writerReport);
                                    totalExecutionTime += duration;

                                    status = performAction.operationLog(
                                            success,
                                            currentInstruction.isOptional()
                                                    ? "OPTIONAL INSTRUCTION"
                                                    : "MANDATORY INSTRUCTION",
                                            resultActions,
                                            duration);
                                    if (success) {
                                        continue outerLoop;
                                    } else {

                                        if (!ifClause && !elseClause) {
                                            stopAll = true;
                                        } else if (ifClause) {
                                            ifFailed = true;
                                        } else if (elseClause) {
                                            elseFailed = true;
                                        }
                                        if (stopAll) {
                                            break;
                                        }
                                    }

                                } else if (refreshOnly) {

                                    performAction.performOtherActions(byPassNotFound, currentInstruction, actions);

                                    long duration = performAction.duration(currentInstructionStartTime);
                                    performAction.excelReportWrite(
                                            success, actions, msgInitial, duration, dataExcel, writerReport);
                                    totalExecutionTime += duration;

                                    status = performAction.operationLog(
                                            success,
                                            currentInstruction.isOptional()
                                                    ? "OPTIONAL INSTRUCTION"
                                                    : "MANDATORY INSTRUCTION",
                                            resultActions,
                                            duration);

                                    refreshOnly = false;

                                    continue;

                                } else if (refreshLoopArray != null && !refreshLoopExecuted && !ignoreRefreshLoop) {

                                    if (!refreshLoopExecuted && actions[0].equals(Constants.REFRESH_LOOP)) {
                                        performAction.performOtherActions(byPassNotFound, currentInstruction, actions);
                                    }

                                    refreshLoopExecuted = true;

                                    Pair<String, String> msgLoop = new Pair(
                                            parentField,
                                            (refreshLoopArray != null
                                                    ? Arrays.stream(refreshLoopArray)
                                                            .mapToObj(String::valueOf) // Convert each int to String
                                                            .collect(Collectors.joining(":"))
                                                    : "Empty:Empty"));

                                    resultActions = performAction.actionResultMessage(blockName, actions, msgLoop);
                                    long duration = performAction.duration(currentInstructionStartTime);

                                    boolean excelSuccess = performAction.excelReportWrite(
                                            success, actions, msgLoop, duration, dataExcel, writerReport);

                                    if (!excelSuccess) {
                                        resultActions = "Failed " + resultActions;
                                        success = false;
                                    }

                                    totalExecutionTime += duration;

                                    status = performAction.operationLog(
                                            success,
                                            currentInstruction.isOptional()
                                                    ? "OPTIONAL INSTRUCTION"
                                                    : "MANDATORY INSTRUCTION",
                                            resultActions,
                                            duration);

                                    refreshLoopArray[1] = refreshLoopArray[1] - 1;
                                    currentIndex = refreshLoopArray[3];
                                    continue;

                                } else if (!execOperation && !checkOperation && !excelWriteOperation) {

                                    Pair<String, String> msgLoop = null;
                                    if (refreshLoopExecuted && refreshLoopArray != null) {

                                        boolean pauseParentLoop =
                                                parentIdsForRefreshLoop.contains(currentInstruction.getId());
                                        if (pauseParentLoop) {
                                            performAction.onHoldInSeconds(refreshLoopArray[0]);

                                            //                                        if (refreshLoopExecuted) {
                                            //
                                            // performAction.performOtherActions(
                                            //                                                    byPassNotFound,
                                            //                                                    currentInstruction,
                                            //                                                    new String[]
                                            // {Constants.REFRESH_ONLY});
                                            //                                        }
                                        }

                                        String currentField =
                                                currentInstruction.getId() + "-" + currentInstruction.getName();
                                        msgLoop = new Pair(
                                                currentField,
                                                (refreshLoopArray != null
                                                        ? Arrays.stream(refreshLoopArray)
                                                                .mapToObj(String::valueOf) // Convert each int to String
                                                                .collect(Collectors.joining(":"))
                                                        : "Empty:Empty"));

                                        resultActions = performAction.actionResultMessage(
                                                blockName, new String[] {ABRConstants.HOLD}, msgLoop);
                                        long duration = performAction.duration(currentInstructionStartTime);

                                        boolean excelSuccess = performAction.excelReportWrite(
                                                pauseParentLoop,
                                                new String[] {ABRConstants.HOLD},
                                                msgLoop,
                                                duration,
                                                dataExcel,
                                                writerReport);

                                        if (!excelSuccess) {
                                            resultActions = "Failed " + resultActions;
                                            success = false;
                                        }

                                        totalExecutionTime += duration;

                                        status = performAction.operationLog(
                                                pauseParentLoop,
                                                currentInstruction.isOptional()
                                                        ? "OPTIONAL INSTRUCTION"
                                                        : "MANDATORY INSTRUCTION",
                                                "(REFRESH_LOOP)-HOLD TIME" + refreshLoopArray[0] + " Seconds",
                                                duration);
                                    }

                                    if (actions[0].equals(Constants.HOLD)
                                            || actions[0].equals(Constants.QUIT)
                                            || actions[0].equals(Constants.SCREEN)
                                            || actions[0].equals(Constants.REFRESH_ONLY)) {
                                        performAction.performOtherActions(byPassNotFound, currentInstruction, actions);

                                        if (actions[0].equals(Constants.QUIT)) {
                                            stopAll = true;
                                            success = true;
                                        }

                                        long duration = performAction.duration(currentInstructionStartTime);

                                        performAction.excelReportWrite(
                                                success, actions, msgInitial, duration, dataExcel, writerReport);

                                        totalExecutionTime += duration;

                                        status = performAction.operationLog(
                                                success,
                                                currentInstruction.isOptional()
                                                        ? "OPTIONAL INSTRUCTION"
                                                        : "MANDATORY INSTRUCTION",
                                                resultActions,
                                                duration);

                                        continue;
                                    }

                                    // Extract dataFieldName and dataFieldValue using a separate method
                                    Pair<String, String> fieldData = performAction.extractFieldData(
                                            dataExcel,
                                            actions,
                                            currentInstruction.getDefaultValue(),
                                            currentInstruction.getEncrypted() > 0);

                                    WebElement webElementFound = null;
                                    try {
                                        webElementFound = performAction.searchElement(currentInstruction, botJobId);
                                    } catch (Exception ex) {
                                        extraMsg = "Element not found. Please try rescanning.!";
                                        success = false;
                                    }

                                    if (webElementFound == null && searchByJavaScript) {
                                        if (actions[0].equalsIgnoreCase(ABRConstants.VISUALIZE)
                                                || actions[0].equalsIgnoreCase(ABRConstants.CLICK)
                                                || actions[0].equalsIgnoreCase(ABRConstants.INSERT)) {
                                            success = performAction.executeActionsAtCoordinates(
                                                    mapSavedLocators.get("coordinates"), fieldData, actions[0]);
                                        }
                                    }

                                    byPassNotFound = byPassFlagLoop || ifClause || elseClause;

                                    if (webElementFound != null && success) {

                                        success = performAction.performWebActions(
                                                byPassNotFound,
                                                mapSavedLocators.get("coordinates"),
                                                fieldData,
                                                currentInstruction,
                                                mapOperators,
                                                webElementFound,
                                                actions);

                                        if (actions[0].equalsIgnoreCase(ABRConstants.OUTPUT)) {
                                            fieldName = currentInstruction.getId() + "-" + currentInstruction.getName();
                                            if (mapOperators.containsKey(fieldName)) {
                                                msgInitial = new Pair(fieldName, mapOperators.get(fieldName));
                                            } else {
                                                msgInitial = new Pair(fieldName, "TEXT OUTPUT NOT FOUND");
                                            }
                                        }
                                    }
                                    // Special Cases for Select Responses
                                    // It could be Improved the case
                                    if (resultActions.contains("Error:") || webElementFound == null || !success) {
                                        resultActions = "Failed " + resultActions;
                                        success = false;
                                    } else if (resultActions != null && success) {
                                        currentInstruction.setExecuted(true);
                                        // Assuming currentInstruction and instructionsExecuted are already defined
                                        if (currentInstruction != null
                                                && instructionsExecuted.stream()
                                                        .noneMatch(
                                                                instruction -> instruction.getInstructionOrderNumber()
                                                                        == currentInstruction
                                                                                .getInstructionOrderNumber())) {
                                            instructionsExecuted.add(currentInstruction);
                                        }

                                        executedSuccess.add(currentInstruction.getId());
                                        success = true;
                                    }

                                    long duration = performAction.duration(currentInstructionStartTime);

                                    if (!success && refreshLoopExecuted && refreshLoopArray != null) {
                                        byPassFlagLoop = parentIdsForRefreshLoop.contains(currentInstruction.getId());
                                        success = byPassFlagLoop;
                                    }

                                    if (!success && !ifClause && !elseClause && !byPassFlagLoop) {
                                        stopAll = true;
                                        success = false;
                                    } else if (ifClause && !success) {
                                        ifFailed = true;
                                    } else if (elseClause && !success) {
                                        elseFailed = true;
                                    }

                                    if (byPassFlagLoop) {

                                        resultActions = "By Passing Loop Flag "
                                                + performAction.actionResultMessage(blockName, actions, fieldData);

                                        performAction.excelReportWrite(
                                                success,
                                                new String[] {ABRConstants.BY_PASS},
                                                msgInitial,
                                                duration,
                                                dataExcel,
                                                writerReport);

                                    } else {
                                        performAction.excelReportWrite(
                                                success, actions, msgInitial, duration, dataExcel, writerReport);
                                    }

                                    totalExecutionTime += duration;

                                    status = performAction.operationLog(
                                            success,
                                            currentInstruction.isOptional()
                                                    ? "OPTIONAL INSTRUCTION"
                                                    : "MANDATORY INSTRUCTION",
                                            resultActions,
                                            duration);

                                    if (stopAll) {
                                        break;
                                    }

                                } else if (execOperation) {

                                    resultActions = currentInstruction.getName()
                                            + Constants.BLANK_STRING
                                            + currentInstruction.getActions()
                                            + Constants.BLANK_STRING
                                            + currentInstruction.getOperation();

                                    // Special Operators
                                    if (operations.length == 2) {
                                        resultActions = performAction.performOperatorActions(
                                                byPassNotFound,
                                                currentInstruction,
                                                xPathOperation,
                                                actions[0],
                                                operations,
                                                parentField,
                                                mapOperators);

                                        if (resultActions != null) {
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

                                            executedSuccess.add(currentInstruction.getId());
                                            success = true;
                                        } else {
                                            resultActions = "Failed: " + resultActions;
                                            success = false;
                                        }

                                    } else {
                                        resultActions = "Failed: " + resultActions;
                                        success = false;
                                    }

                                    long duration = performAction.duration(currentInstructionStartTime);
                                    performAction.excelReportWrite(
                                            success, actions, msgInitial, duration, dataExcel, writerReport);
                                    totalExecutionTime += duration;

                                    status = performAction.operationLog(
                                            success,
                                            currentInstruction.isOptional()
                                                    ? "OPTIONAL INSTRUCTION"
                                                    : "MANDATORY INSTRUCTION",
                                            resultActions,
                                            duration);

                                } else if (checkOperation) {
                                    // Check Validation Operator
                                    resultActions = currentInstruction.getName()
                                            + Constants.BLANK_STRING
                                            + currentInstruction.getActions()
                                            + Constants.BLANK_STRING
                                            + currentInstruction.getOperation();

                                    if (operations.length == 3) {
                                        if (mapOperators.containsKey(parentField)) {

                                            byPassFlagLoop = parentIdsForRefreshLoop.contains(parentId);
                                            success = byPassFlagLoop;

                                            resultActions = "CHECK_VALUE for (Parent: " + parentField + ")"
                                                    + String.join(" ", operations);
                                            boolean isOperationValid = false;
                                            if (operations[1].equalsIgnoreCase("=")) {
                                                isOperationValid = mapOperators
                                                        .get(parentField)
                                                        .trim()
                                                        .equalsIgnoreCase(operations[2]);

                                            } else if (operations[1].equalsIgnoreCase(">")) {
                                                isOperationValid = mapOperators
                                                        .get(parentField)
                                                        .trim()
                                                        .equalsIgnoreCase(operations[2]);
                                            } else if (operations[1].equalsIgnoreCase("!=")) {
                                                isOperationValid = !mapOperators
                                                        .get(parentField)
                                                        .trim()
                                                        .equalsIgnoreCase(operations[2]);
                                            }

                                            if (isOperationValid) {

                                                if (byPassFlagLoop) {
                                                    refreshLoopExecuted = false;
                                                    ignoreRefreshLoop = true;
                                                    refreshLoopArray = null;
                                                    ignoreRefreshLoop = true;
                                                }

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

                                                executedSuccess.add(currentInstruction.getId());
                                                success = true;
                                            } else {
                                                resultActions = performAction.checkValidationFailedEngine(
                                                        parentField,
                                                        mapOperators.get(parentField),
                                                        resultActions,
                                                        operations,
                                                        ifClause,
                                                        elseClause,
                                                        byPassFlagLoop);

                                                if (!ifClause && !elseClause && !byPassFlagLoop) {
                                                    stopAll = true;
                                                    success = false;
                                                } else if (ifClause) {
                                                    ifFailed = true;
                                                } else if (elseClause) {
                                                    elseFailed = true;
                                                }

                                                if (stopAll) {
                                                    break;
                                                }
                                            }

                                        } else {
                                            resultActions = performAction.getValueIsNotDefinedEngine(
                                                    currentInstruction, resultActions, ifClause, elseClause);
                                            if (!ifClause && !elseClause) {
                                                stopAll = true;
                                                success = false;
                                            } else if (ifClause) {
                                                ifFailed = true;
                                            } else if (elseClause) {
                                                elseFailed = true;
                                            }

                                            if (stopAll) {
                                                break;
                                            }
                                        }

                                    } else {
                                        resultActions = "Failed: " + resultActions;
                                        success = false;
                                    }

                                    long duration = performAction.duration(currentInstructionStartTime);
                                    performAction.excelReportWrite(
                                            success, actions, msgInitial, duration, dataExcel, writerReport);
                                    totalExecutionTime += duration;

                                    status = performAction.operationLog(
                                            success,
                                            currentInstruction.isOptional()
                                                    ? "OPTIONAL INSTRUCTION"
                                                    : "MANDATORY INSTRUCTION",
                                            resultActions,
                                            duration);

                                } else if (excelWriteOperation) {
                                    // Excel Write Operator
                                    resultActions = currentInstruction.getName()
                                            + Constants.BLANK_STRING
                                            + currentInstruction.getActions()
                                            + Constants.BLANK_STRING
                                            + currentInstruction.getOperation();

                                    if (operations.length == 2) {
                                        if (mapOperators.containsKey(parentField)) {

                                            if (excelExportOnceCreation) {
                                                //
                                                // writerExport.insertReportHead();
                                                excelExportOnceCreation = false;
                                            }

                                            if (!Strings.isNullOrEmpty(excelFieldName)) {
                                                writerExport = new ExcelWriter(
                                                                excelFieldName, abrWebDriver.getDriver(), true)
                                                        .withPurpose("export");
                                            }

                                            if (writerExport != null) {

                                                resultActions = "insertValueFieldNameInExcel-->" + parentField + "-"
                                                        + mapOperators.get(parentField);
                                            } else {
                                                resultActions = "NO Export Excel File defined -->" + parentField + "-"
                                                        + mapOperators.get(parentField);
                                            }

                                            if (mapExport.size() == 0) {
                                                //
                                                // writerExport.insertBlockSeparation(blockLoad.getName());
                                                //                                            exportIndex *= 2;
                                            }

                                            // Insert the updated mapExport into the Excel after each instruction
                                            if (writerExport != null) {
                                                mapExport.put("KEY", "EXTERNAL");
                                                mapExport.put(fieldName, mapOperators.get(parentField));

                                                writerExport.insertFieldNameAndValueLastColumn(
                                                        mapExport, exportIndex - 1);
                                            }
                                            performAction.onHoldForSeconds(null);

                                            if (resultActions != null) {
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

                                                executedSuccess.add(currentInstruction.getId());
                                                success = true;
                                            } else {
                                                resultActions = "Failed: " + resultActions;
                                                success = false;
                                            }

                                        } else {
                                            resultActions = performAction.getValueIsNotDefinedEngine(
                                                    currentInstruction, resultActions, ifClause, elseClause);
                                            if (!ifClause && !elseClause) {
                                                stopAll = true;
                                                success = false;
                                            } else if (ifClause) {
                                                ifFailed = true;
                                            } else if (elseClause) {
                                                elseFailed = true;
                                            }
                                            if (stopAll) {
                                                break;
                                            }
                                        }

                                    } else {
                                        resultActions = "Failed: " + resultActions;
                                        success = false;
                                    }

                                    long duration = performAction.duration(currentInstructionStartTime);
                                    performAction.excelReportWrite(
                                            success, actions, msgInitial, duration, dataExcel, writerReport);
                                    totalExecutionTime += duration;

                                    status = performAction.operationLog(
                                            success,
                                            currentInstruction.isOptional()
                                                    ? "OPTIONAL INSTRUCTION"
                                                    : "MANDATORY INSTRUCTION",
                                            resultActions,
                                            duration);
                                }

                            } catch (Throwable t) {
                                if (!ifClause && !elseClause) {
                                    stopAll = true;
                                    success = false;
                                } else if (ifClause) {
                                    ifFailed = true;
                                } else if (elseClause) {
                                    elseFailed = true;
                                }
                                currentInstruction.setExecuted(false);

                                long duration = performAction.duration(currentInstructionStartTime);
                                performAction.excelReportWrite(
                                        false, actions, msgInitial, duration, dataExcel, writerReport);
                                totalExecutionTime += duration;

                                status = performAction.operationLog(
                                        false,
                                        currentInstruction.isOptional()
                                                ? "OPTIONAL INSTRUCTION"
                                                : "MANDATORY INSTRUCTION",
                                        resultActions,
                                        duration);

                                if (stopAll) {
                                    break;
                                }

                                //                            throw new RuntimeException(t);
                            }

                            printLog(generateTimestamp(), logFileForSingleExcel, resultActions, success);

                            if (!success) {
                                //                                countdownTextField.setStyle("-fx-font-size: 16px;
                                // -fx-text-fill: red;");
                                //                                countdownTextField.setText(resultActions);
                                if (!ifClause && !elseClause) {
                                    stopAll = true;
                                    success = false;
                                    break;
                                }
                                //                                return false;
                            }

                            if (resultActions.equalsIgnoreCase("Close Browser")) {
                                stopAll = true;
                                break;
                            }
                        }
                    }
                    // Increment currentBlock only if executing all blocks
                    //                if (executeSpecificBlock == -1) {
                    //                    currentBlock++;
                    //                } else {
                    //                    break; // Exit loop after executing the specific block
                    //                }

                    // Increment currentBlock only if executing all blocks
                    currentBlock++;
                }

                if (executionTimes >= execLimitReach) {
                    performAction.alertExecutionTimes(executionTimes, resultActions);
                }

            } else { //  if dataExel is NULL
                // Creating Dynamic Data if Default is Null
                Pair<String, String> dataDynamic = null;
                for (int j = 0; success && j < blocksLoaded.size(); j++) {

                    // Call the method to get the filtered list
                    List<BlockLoopInstructionLoadDTO> unexecutedInstructions = getUnexecutedInstructions(
                            instructionsExecuted, blocksLoaded.get(j).getBlockLoopInstructionLoadDTOS());

                    for (BlockLoopInstructionLoadDTO currentInstruction : unexecutedInstructions) {
                        if (currentInstruction.getDefaultValue() == null) {
                            String[] arr = UtilsMethods.splitIfContains(
                                    currentInstruction.getActions(), Constants.ACTION_SPECIFICATIONS_SPLITTER);
                            if (arr.length > 1) {
                                String dataFieldName = arr[1].split(Constants.PATH_FIELD_SUBSTITUTION)[0];
                                performAction.insertRandomName(dataFieldName);
                            }
                        }
                    }
                }
                for (int j = 0; success && j < blocksLoaded.size(); j++) {

                    String blockName = blocksLoaded.get(j).getName();

                    // Call the method to get the filtered list
                    List<BlockLoopInstructionLoadDTO> unexecutedInstructions = getUnexecutedInstructions(
                            instructionsExecuted, blocksLoaded.get(j).getBlockLoopInstructionLoadDTOS());

                    for (BlockLoopInstructionLoadDTO currentInstruction : unexecutedInstructions) {

                        long currentInstructionStartTime = System.nanoTime();
                        File logFileForSingleExcel = excelReader.createLogFile(excelPath);

                        String[] actions = currentInstruction.getActions().split(Constants.ACTIONS_AND_PATHS_SPLITTER);

                        // Case for Inputs
                        String valueInsert = "No Data Found";
                        if (actions[0].equalsIgnoreCase(ABRConstants.INSERT)) {

                            String reference = actions[1];
                            valueInsert = dataExcel.get(reference);
                        }

                        Pair<String, String> msgInitial = new Pair(
                                currentInstruction.getName(),
                                (currentInstruction.getOperation() != null
                                        ? currentInstruction.getOperation()
                                        : (actions[0].equalsIgnoreCase(ABRConstants.INSERT)) ? valueInsert : ""));

                        resultActions = performAction.actionResultMessage(blockName, actions, msgInitial);

                        try {

                            if (actions[0].equals(Constants.HOLD)
                                    || actions[0].equals(Constants.QUIT)
                                    || actions[0].equals(Constants.SCREEN)
                                    || actions[0].equals(Constants.REFRESH_ONLY)) {
                                performAction.performOtherActions(byPassNotFound, currentInstruction, actions);

                                if (actions[0].equals(Constants.QUIT)) {
                                    stopAll = true;
                                    success = true;
                                }

                                long duration = performAction.duration(currentInstructionStartTime);

                                performAction.excelReportWrite(
                                        success, actions, msgInitial, duration, dataExcel, writerReport);

                                totalExecutionTime += duration;

                                status = performAction.operationLog(
                                        success,
                                        currentInstruction.isOptional()
                                                ? "OPTIONAL INSTRUCTION"
                                                : "MANDATORY INSTRUCTION",
                                        resultActions,
                                        duration);

                                continue;
                            }

                            WebElement webElementFound = null;
                            try {
                                webElementFound = performAction.searchElement(currentInstruction, botJobId);
                            } catch (Exception ex) {
                                extraMsg = "Element not found. Please try rescanning.!";
                            }

                            success = performAction.performWebActions(
                                    byPassNotFound,
                                    mapSavedLocators.get("coordinates"),
                                    dataDynamic,
                                    currentInstruction,
                                    mapOperators,
                                    webElementFound,
                                    actions);

                            // Special Cases for Select Responses
                            // It could be Improved the case
                            if (resultActions.contains("Error:")) {
                                success = false;
                            } else if (resultActions != null) {
                                currentInstruction.setExecuted(true);
                                success = true;
                            } else {
                                resultActions = "Failed to Execute -> " + currentInstruction.getName();
                                success = false;
                            }

                            long duration = performAction.duration(currentInstructionStartTime);
                            performAction.excelReportWrite(success, actions, msgInitial, duration, null, writerReport);
                            totalExecutionTime += duration;

                            status = performAction.operationLog(
                                    success,
                                    currentInstruction.isOptional() ? "OPTIONAL INSTRUCTION" : "MANDATORY INSTRUCTION",
                                    resultActions,
                                    duration);

                        } catch (Throwable t) {
                            success = false;
                            currentInstruction.setExecuted(false);

                            long duration = performAction.duration(currentInstructionStartTime);
                            performAction.excelReportWrite(false, actions, msgInitial, duration, null, writerReport);
                            totalExecutionTime += duration;

                            status = performAction.operationLog(
                                    false,
                                    currentInstruction.isOptional() ? "OPTIONAL INSTRUCTION" : "MANDATORY INSTRUCTION",
                                    resultActions,
                                    duration);
                            //                        throw new RuntimeException(t);
                        }
                        printLog(generateTimestamp(), logFileForSingleExcel, resultActions, success);
                    }
                }
            }

            if (totalExecutionTime == 0) {
                //                report.setDuration(0);
                writerReport.insertTotalExecutionTimes(botJobStartTime, botJobStartTime);
                //                try {
                //                    repository.write(report);
                //                } catch (Exception ex) {
                //                    ABRLogger.getInstance(Engine.class).warning("Repository.write(report) Error:\n" +
                // ex.getMessage());
                //                }
            }

            // PRINT END BASE LOG//
            if (success) {
                //                report.setStatus((short) ExcelReportStatusEnum.SUCCESS.ordinal());
                //                report.setDuration(totalExecutionTime / 100);
                writerReport.insertTotalExecutionTimes(botJobStartTime, System.nanoTime());
                //                try {
                //                    repository.write(report);
                //                } catch (Exception ex) {
                //                    ABRLogger.getInstance(Engine.class).warning("Repository.write(report) Error:\n" +
                // ex.getMessage());
                //                }
                baseLogString = botLoadJobs.get(0).getName()
                        + Constants.FIELDS_SEPARATOR
                        + labelsValue.getProperty(Labels.END)
                        + Constants.FIELDS_SEPARATOR
                        + labelsValue.getProperty(Labels.OK);

                System.out.println(String.format("Success: %s Last Execution: %s", botJobName, resultActions));

            } else {
                baseLogString = botLoadJobs.get(0).getName()
                        + Constants.FIELDS_SEPARATOR
                        + labelsValue.getProperty(Labels.END)
                        + Constants.FIELDS_SEPARATOR
                        + labelsValue.getProperty(Labels.KO)
                        + resultActions;
                //                report.setStatus(status);
                //                report.setDuration(totalExecutionTime / 100);
                writerReport.insertTotalExecutionTimes(botJobStartTime, System.nanoTime());
                //                try {
                //                    repository.write(report);
                //                } catch (Exception ex) {
                //                    ABRLogger.getInstance(Engine.class).warning("Repository.write(report) Error:\n" +
                // ex.getMessage());
                //                }

                System.out.println(String.format("Failed: %s Last Execution: %s", botJobName, resultActions));
            }
            printBaseLog(baseLogFile, generateTimestamp(), baseLogString);

            if (resultActions.equalsIgnoreCase("Close Browser")) {
                abrWebDriver.getDriver().quit();
            }

            return true;
        } catch (Throwable t) {
            ABRLogger.getInstance(Engine.class).severe("Error Executing JOB \n" + t.getMessage());
            return false;
        }
    }

    private static String generateTimestamp() {
        Date date = new Date();
        dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        return dateFormatter.format(date);
    }

    private static void printLog(String timeStamp, File logFile, String resultActions, boolean result) {
        String resultMsg = result ? Constants.SUCCESS : Constants.FAIL;
        String log = String.join(Constants.FIELDS_SEPARATOR, timeStamp, resultMsg, resultActions);

        try {
            FileWriter fileWriter = new FileWriter(logFile, true);
            fileWriter.write(log + System.lineSeparator());
            fileWriter.close();
        } catch (Exception e) {
            ABRLogger.getInstance(WebPage.class).severe("printLog Error: " + e.getMessage());
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
            ABRLogger.getInstance(WebPage.class).severe("printBaseLog Error: " + e.getMessage());
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
            ABRLogger.getInstance(WebPage.class).severe("printLogExcel Error: " + e.getMessage());
        }
    }

    private static void showAlertInfo(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showAlertError(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
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

    public static void changeDbConnection() {
        String priorityPath = ABRPropertyManager.getInstance().getProperty(ABRPropertyEnum.FOLDER_PATH_PRIORITY);
        String dataBaseType = ABRPropertyManager.getInstance().getProperty(ABRPropertyEnum.DATABASE_TYPE);

        if (dataBaseType != null && dataBaseType.equalsIgnoreCase("POSTGRES")) {
            POSTGRES_DB = true;
        } else {
            POSTGRES_DB = false;
        }

        if (priorityPath != null) {

            //            if (priorityPath != null && !priorityPath.isBlank()) {
            //                abrPriorities.loadPriorities();
            //            }

            if (POSTGRES_DB) {
                String dbUrl = CONNECTION_POSTGRES + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;
                sessionFactory = new Configuration()
                        .configure()
                        .setProperty("hibernate.connection.url", dbUrl)
                        .setProperty("hibernate.connection.username", USERNAME)
                        .setProperty("hibernate.connection.password", PASSWORD)
                        .setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                        .setProperty("hibernate.connection.driver_class", "org.postgresql.Driver")
                        .buildSessionFactory();
                session = sessionFactory.openSession();
                //                cacheEntitiesFromDB();
            } else {

                String dbPath = ABRPropertyManager.getInstance().getProperty(ABRPropertyEnum.FOLDER_PATH_DB);
                if (!dbPath.isBlank()) {
                    File dbFolder = new File(dbPath);
                    dbFolder.mkdirs();
                    String dbUrl = CONNECTION_TYPE + dbPath + ABRConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;
                    sessionFactory = new Configuration()
                            .configure()
                            .setProperty("hibernate.connection.url", dbUrl)
                            .buildSessionFactory();
                    session = sessionFactory.openSession();
                    //                    cacheEntitiesFromDB();
                }
            }
        }
    }

    private static Connection getConnection() {
        String dataBaseType = ABRPropertyManager.getInstance().getProperty(ABRPropertyEnum.DATABASE_TYPE);

        if (dataBaseType != null && dataBaseType.equalsIgnoreCase("POSTGRES")) {
            POSTGRES_DB = true;
        } else {
            POSTGRES_DB = false;
        }

        if (!POSTGRES_DB) {
            if (conn == null) {
                String dbPath = ABRPropertyManager.getInstance().getProperty(ABRPropertyEnum.FOLDER_PATH_DB);
                String dbUrl = CONNECTION_TYPE + dbPath + ABRConstants.FILE_NAME_DB + CONNECTION_PARAMETERS;
                try {
                    conn = DriverManager.getConnection(dbUrl);
                } catch (SQLException e) {
                    ABRLogger.getInstance(WebPage.class).severe("getConnection Error: " + e.getMessage());
                }
            }
            return conn;
        } else {

            if (conn == null) {
                String dbUrl = CONNECTION_POSTGRES + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;
                try {
                    conn = DriverManager.getConnection(dbUrl, USERNAME, PASSWORD);
                } catch (SQLException e) {
                    ABRLogger.getInstance(WebPage.class).severe("Get DB connection Error: " + e.getMessage());
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
                + "  bli.operation, bli.parent_id, "
                + "  b.export_file, b.active, b.wait "
                + " FROM bot_job bj "
                + " LEFT JOIN block b ON b.bot_job_id = bj.id "
                + "  JOIN block_loop_instruction bli ON bli.block_id = b.id "
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
                    blockDTO.setActive(rs.getBoolean("active"));
                    blockDTO.setWait(rs.getInt("wait"));
                    blockDTO.setExportFile(rs.getString("export_file"));
                    blockDTO.setBotJobId(botJobDTO.getId());
                    blockDTO.setBotJobName(botJobDTO.getName());

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
            ABRLogger.getInstance(WebPage.class).severe("loadBlockAll Error: " + e.getMessage());
        }
    }

    private static HomeBankingDTO loadHomeBanking(int homeBankingId) {
        String query =
                "SELECT id, cookies, driver_session, name, options_config, password, priority, search_config, url, username "
                        + "FROM home_banking "
                        + "WHERE id = " + homeBankingId;
        HomeBankingDTO homeBanking = null;

        try (Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                homeBanking = new HomeBankingDTO();
                homeBanking.setId(rs.getInt("id"));
                homeBanking.setCookies(rs.getString("cookies"));
                homeBanking.setDriverSession(rs.getString("driver_session"));
                homeBanking.setName(rs.getString("name"));
                homeBanking.setOptionsConfig(rs.getString("options_config"));
                homeBanking.setPassword(rs.getString("password"));
                homeBanking.setPriority(rs.getString("priority"));
                homeBanking.setSearchConfig(rs.getString("search_config"));
                homeBanking.setUrl(rs.getString("url"));
                homeBanking.setUsername(rs.getString("username"));
            }

        } catch (SQLException e) {
            ABRLogger.getInstance(WebPage.class).severe("loadHomeBanking Error: " + e.getMessage());
        }

        return homeBanking;
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
