package com.allinweb.ch;

import com.allinweb.ch.component.model.BlockLoadDTO;
import com.allinweb.ch.component.model.BlockLoopInstructionLoadDTO;
import com.allinweb.ch.component.model.BotJobLoadDTO;
import com.allinweb.ch.component.model.HomeBankingLoadDTO;
import com.allinweb.ch.component.model.InstructionReferenceLoadDTO;
import com.allinweb.ch.driver.ABRWebDriver;
import com.allinweb.ch.facade.PerformActions;
import com.allinweb.ch.facade.PerformDataBase;
import com.allinweb.ch.persistence.Repository;
import com.allinweb.ch.readersAndWriters.ExcelReader;
import com.allinweb.ch.readersAndWriters.ExcelWriter;
import com.allinweb.ch.supportTypes.ExtractedData;
import com.allinweb.ch.supportTypes.WebPage;
import com.allinweb.ch.util.*;
import com.google.common.base.Strings;
import io.opentelemetry.api.internal.StringUtils;
import java.io.*;
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

    private static SessionFactory sessionFactory = null;
    private static Session session = null;

    private static List<BotJobLoadDTO> botLoadJobs = new ArrayList<>();
    static List<BlockLoopInstructionLoadDTO> instructionsExecuted = new ArrayList<>();
    static List<Integer> executedSuccess = new ArrayList<>();
    Map<String, WebElement> mapAdvanced = new HashMap<>();

    private static final PerformDataBase performDataBase;
    private static final PerformActions performAction;
    private static ABRPriorities abrPriorities;

    // Static block to initialize
    static {
        performDataBase = PerformDataBase.getInstance();
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

        performDataBase.changeDbConnection();

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

        HomeBankingLoadDTO homeBankingLoad = performDataBase.loadHomeBanking(homeBankingId);

        if (homeBankingLoad == null || StringUtils.isNullOrEmpty(homeBankingLoad.getUrl())) {
            ABRLogger.getInstance(Engine.class).severe("Cannot find Home Banking Environment Id:" + homeBankingId);
            return false;
        }

        botLoadJobs = performDataBase.loadBotJobComplete(botJobId);

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
            abrWebDriver.openDriver(homeBankingLoad.getUrl(), homeBankingLoad.getOptionsConfig());

            // Ensure botJob and abrPriorities are not null before accessing their methods
            if (botLoadJobs.get(0) != null && abrPriorities != null) {
                // Check if we need to update abrPriorities
                if (abrPriorities.getJobId() == null
                        || !abrPriorities.getJobId().equals(botLoadJobs.get(0).getId())) {
                    // Set Job ID in abrPriorities
                    abrPriorities.setJobId(botLoadJobs.get(0).getId());

                    // Check for non-null HomeBanking and Priority
                    if (homeBankingLoad != null) {
                        String priorityValue = homeBankingLoad.getPriority();
                        String searchConfig = homeBankingLoad.getSearchConfig();

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
                    + ABRConstants.FIELDS_SEPARATOR
                    + labelsValue.getProperty(Labels.START);

            printBaseLog(baseLogFile, generateTimestamp(), baseLogString);

            ExcelWriter.ExcelChain writerReport = new ExcelWriter(
                            botLoadJobs.get(0).getName(), abrWebDriver.getDriver(), false)
                    .withPurpose("report");
            writerReport.insertReportHead();

            ExcelWriter.ExcelChain writerExport = null;
            //                new ExcelWriter(blocksLoaded.get(0).getName(),
            // abrWebDriver.getDriver()).withPurpose("export");
            boolean excelExportOnceCreation = true;
            //        writerExport.insertReportHead();

            Set<String> mapIgnore = new HashSet<>();

            boolean searchByJavaScript = false; // checkBoxJavaScript.isSelected();

            String mainMsg = "";
            boolean byPassNotFound = false;
            boolean byPassFlagLoop = false;
            boolean success = true;
            boolean stopAll = false;
            long botJobStartTime = System.nanoTime();
            long totalExecutionTime = 0;
            String resultActions = "No instruction executed yet";
            boolean showAlert = true;
            String extraMsg = "";
            short status = (short) ExcelReportStatusEnum.ERROR.ordinal();
            Map<String, String> dataExcel = null;

            // clearFields();

            //            ExcelReportDTO report = new ExcelReportDTO();
            //            report.setOrder((short) botLoadJobs.get(0).getId());
            //            report.setStartDate(LocalDateTime.now());
            //            report.setBatchJobId(selectedJob.getId());
            //            report.setBotJobDTO(selectedJob);
            //            report.setStatus((short) ExcelReportStatusEnum.NOT_RUN.ordinal());

            // Execute All Blocks starting from executeSpecificBlock if Defined
            //            int executeSpecificBlock = comboBoxBlocks.getValue().getVarId();

            mapOperators = new HashMap<>();
            mapExport = new LinkedHashMap<>();

            Map<String, String> mapSavedLocators = new HashMap<>();

            Set<Integer> parentIdsForLoop = null;
            Map<String, List<Integer>> mapConditional = new HashMap<>(); // <parentId:Limit Loops> -> <1|5 Times>
            Map<String, Integer> mapLoops = new HashMap<>(); // <parentId:Limit Loops> -> <1|5 Times>
            Map<String, Integer> mapRefresh = new HashMap<>(); // <parentId:Limit Loops> -> <1|5 Times>
            Set<String> loopBlockActive = new HashSet<>();
            Map<String, Integer> loopBlockLimits = new HashMap<>();

            ABRConstants.ConditionStatus currentCondition = ABRConstants.ConditionStatus.NONE;

            int exportIndex = 1;
            if (extractedData.getNumberOfDataRows() > 0) {

                // Execute All Blocks starting from executeSpecificBlock if Defined
                int currentBlock = (executeSpecificBlock > -1) ? executeSpecificBlock - 1 : 0;

                blockLoop:
                while (currentBlock <= blocksLoaded.size() - 1 && blocksLoaded.size() > 0 && !stopAll) {
                    long blockStartTime = System.nanoTime();
                    currentCondition = ABRConstants.ConditionStatus.NONE;
                    ABRConstants.ConditionStatus previousCondition = ABRConstants.ConditionStatus.NONE;
                    ABRConstants.ConditionStatus progressCondition = ABRConstants.ConditionStatus.NONE;
                    int parentBlockCondition = -1;

                    instructionsExecuted.clear();

                    BlockLoadDTO blockLoad = blocksLoaded.get(currentBlock);
                    String excelFieldName = blockLoad.getExportFile();

                    String blockName = blocksLoaded.get(currentBlock).getName();
                    int blockOrder = blocksLoaded.get(currentBlock).getBlockOrderNumber();
                    String blockReportName = "#" + blockOrder + " " + blockName;

                    int blockWait = blocksLoaded.get(currentBlock).getWait() > 0
                            ? blocksLoaded.get(currentBlock).getWait()
                            : 2;
                    boolean blockActive = blocksLoaded.get(currentBlock).isActive();

                    // It Searches the Block That have finished the Loops to Avoid recursivity
                    if (loopBlockActive.size() > 0) {
                        for (String blocLoopKey : loopBlockActive) {
                            if (mapLoops.containsKey(blocLoopKey)) {
                                if (mapLoops.get(blocLoopKey) == 0) {
                                    stopAll = true;
                                    int limit = loopBlockLimits.get(blocLoopKey);

                                    Pair<String, String> msgBlock = new Pair(blocLoopKey, "0");

                                    // Excel Report and Log
                                    performAction.logAndReport(
                                            currentCondition,
                                            true,
                                            true,
                                            blockStartTime,
                                            blockReportName,
                                            success,
                                            new String[] {ABRConstants.GOTO},
                                            msgBlock,
                                            dataExcel,
                                            writerReport,
                                            "GOTO Limit Reached",
                                            blocLoopKey + " Reached: 0");

                                    msgBlock = new Pair(
                                            String.format("Exit at Block Name: \"%s\"", blockLoad.getName()),
                                            ABRConstants.EXIT);

                                    // Excel Report and Log
                                    performAction.logAndReport(
                                            currentCondition,
                                            true,
                                            true,
                                            blockStartTime,
                                            blockReportName,
                                            success,
                                            new String[] {ABRConstants.EXIT},
                                            msgBlock,
                                            dataExcel,
                                            writerReport,
                                            "Stopping App",
                                            String.format("Exit at Block Name: \"%s\"", blockName));

                                    performAction.gotoLimitExecution(limit, resultActions);

                                    continue blockLoop;
                                }
                            }
                        }
                    }

                    if (!blockActive) {
                        currentBlock++;

                        Pair<String, String> msgBlock =
                                new Pair(String.format("Ignore: \"%s\"", blockLoad.getName()), ABRConstants.IGNORE);

                        // Excel Report and Log
                        performAction.logAndReport(
                                currentCondition,
                                true,
                                true,
                                blockStartTime,
                                blockReportName,
                                success,
                                new String[] {ABRConstants.IGNORE},
                                msgBlock,
                                dataExcel,
                                writerReport,
                                "BLOCK IGNORED",
                                String.format("Block: \"%s\" is Inactive: ", blockName));

                        continue;
                    }

                    try {

                        performAction.onHoldInSeconds(blockWait);
                        ABRLogger.getInstance(Engine.class)
                                .info(String.format(
                                        "Default Wait for Block: \"%s\" ->  %d Seconds",
                                        blockLoad.getName(), blockWait));

                        Pair<String, String> msgBlock = new Pair(
                                String.format("Default Wait: \"%s\" ->  %d Seconds", blockLoad.getName(), blockWait),
                                ABRConstants.HOLD);

                        // Excel Report and Log
                        performAction.logAndReport(
                                currentCondition,
                                true,
                                true,
                                blockStartTime,
                                blockReportName,
                                success,
                                new String[] {ABRConstants.HOLD},
                                msgBlock,
                                dataExcel,
                                writerReport,
                                "BLOCK DEFAULT WAIT",
                                String.format("Block: \"%s\" Wait %s Seconds: ", blockName, blockWait));

                    } catch (Exception ex) {
                        ABRLogger.getInstance(Engine.class)
                                .severe(String.format("Error Wait Block for :\"%s\"", blockLoad.getName()));
                    }

                    // Step 1: Get all ParentIds For LOOPs Filter rows where actions = "REFRESH_LOOP" or "LOOP" on
                    // current
                    // Block
                    parentIdsForLoop = performAction.getParentIdsForLoop(
                            blocksLoaded.get(currentBlock).getBlockLoopInstructionLoadDTOS());

                    // Step 2: Get all Conditional By parentId for Index Locator on current Block Relocate "IF",
                    // "ELSEIF",
                    // "ELSE", and "ENDIF"
                    mapConditional = performAction.getConditionIndexMapByParentId(blockLoad);

                    // Step 3: Get all Instructions Ids on current Block
                    int[] instructionIds = blockLoad.getBlockLoopInstructionLoadDTOS().stream()
                            .mapToInt(BlockLoopInstructionLoadDTO::getId)
                            .toArray();

                    // Step 2: Filter rows where actions = "REFRESH_LOOP" or "LOOP" and collect into the map

                    //                mapLoops = performAction.getLoopAndRefreshLoops(
                    //                        blocksLoaded.get(currentBlock).getBlockLoopInstructionLoadDTOS());

                    //                executionTimes++;
                    boolean jumpGoto = false;
                    boolean jumpLoop = false;
                    boolean refreshLoop = false;
                    boolean refreshOnly = false;

                    for (int i = 0; success && i < extractedData.getNumberOfDataRows() && !stopAll; i++) {
                        mapExport.clear();
                        //                    writerReport.insertBlockSeparation(blockLoad.getName());

                        dataExcel = extractedData.getRowFieldValues(i);

                        int currentIndex = 0;

                        instructionLoop:
                        while (currentIndex < instructionIds.length && !stopAll) {
                            // Resets the success
                            success = true;

                            long currentInstructionStartTime = System.nanoTime();

                            BlockLoopInstructionLoadDTO currentInstruction =
                                    blockLoad.getBlockLoopInstructionLoadDTOS().get(currentIndex);

                            byPassFlagLoop = parentIdsForLoop.contains(currentInstruction.getId());

                            mainMsg =
                                    currentInstruction.getOptional() ? "OPTIONAL INSTRUCTION" : "MANDATORY INSTRUCTION";

                            if (!currentInstruction.getInstructionActive()) {

                                String nameInstruc =
                                        "(" + currentInstruction.getId() + ") " + currentInstruction.getName();
                                Pair<String, String> msgBlock =
                                        new Pair(String.format("Ignore: \"%s\"", nameInstruc), ABRConstants.IGNORE);

                                // Excel Report and Log
                                performAction.logAndReport(
                                        currentCondition,
                                        true,
                                        true,
                                        blockStartTime,
                                        blockReportName,
                                        success,
                                        new String[] {ABRConstants.IGNORE},
                                        msgBlock,
                                        dataExcel,
                                        writerReport,
                                        "INSTRUCTION IGNORED",
                                        String.format("Instruction: \"%s\" is Inactive: ", nameInstruc));

                                currentIndex++;

                                continue;
                            }

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
                            boolean pauseOperation = false;

                            String xPathOperation = null;
                            String parentField = null;
                            String parentFieldLoop = null;
                            String fieldName = null;
                            int parentId = currentInstruction.getParentId();

                            if (mapIgnore.contains(currentInstruction.getId() + "-" + currentInstruction.getName())) {
                                continue;
                            }

                            //                        String[] operation =
                            // UtilsMethods.splitIfContains(instruction.getOperation(),
                            // ABRConstants.ACTION_SPECIFICATIONS_SPLITTER);
                            String[] actions =
                                    currentInstruction.getActions().split(ABRConstants.ACTION_SPECIFICATIONS_SPLITTER);
                            String[] operations = currentInstruction.getOperation() != null
                                    ? currentInstruction
                                            .getOperation()
                                            .split(ABRConstants.ACTION_SPECIFICATIONS_SPLITTER)
                                    : null;

                            if (actions[0].equalsIgnoreCase(ABRConstants.IF)
                                    || actions[0].equalsIgnoreCase(ABRConstants.ELSEIF)
                                    || actions[0].equalsIgnoreCase(ABRConstants.ELSE)
                                    || actions[0].equalsIgnoreCase(ABRConstants.ENDIF)) {
                                currentCondition = ABRConstants.ConditionStatus.valueOf(actions[0]);
                                if (previousCondition.equals(ABRConstants.ConditionStatus.NONE)) {
                                    previousCondition = currentCondition;
                                    parentBlockCondition = parentId;
                                } else if (!previousCondition.equals(
                                        currentCondition)) { // To Reset the Progress to the Next Block
                                    previousCondition = currentCondition;
                                }

                                // Conditions When Pass to any of then
                                if (progressCondition.equals(ABRConstants.ConditionStatus.IF_PASSED)
                                        || progressCondition.equals(ABRConstants.ConditionStatus.ELSEIF_PASSED)) {
                                    int jumpPassed = performAction.checkActionToJump(
                                            actions[0],
                                            progressCondition,
                                            mapConditional,
                                            parentBlockCondition,
                                            currentIndex);

                                    // Any Error
                                    if (jumpPassed < 0) {
                                        stopAll = true;
                                        continue blockLoop;
                                    }
                                    // Found Next Block
                                    if (jumpPassed > 0) {
                                        currentIndex = jumpPassed;
                                        // reset all Conditional
                                        currentCondition = ABRConstants.ConditionStatus.NONE;
                                        progressCondition = ABRConstants.ConditionStatus.NONE;
                                        continue instructionLoop;
                                    }
                                } else if (currentCondition.equals(ABRConstants.ConditionStatus.ENDIF)) {
                                    currentCondition = ABRConstants.ConditionStatus.NONE;
                                    previousCondition = ABRConstants.ConditionStatus.NONE;
                                    progressCondition = ABRConstants.ConditionStatus.NONE;
                                    parentBlockCondition = -1;
                                }
                                continue;
                            }

                            // Case for Inputs
                            String valueInsert = "No Data Found";
                            if (actions[0].equalsIgnoreCase(ABRConstants.INSERT)) {
                                String reference = actions[1];
                                valueInsert = dataExcel.get(reference);
                            }

                            Pair<String, String> msgInstruction = null;
                            if (actions[0].equalsIgnoreCase(ABRConstants.GOTO)) {
                                // <currentId:blockId:blockOrderNumber:bockName>
                                msgInstruction = performAction.getBlockDetailsById(blocksLoaded, currentInstruction);
                                if (!mapLoops.containsKey(msgInstruction.getKey())) {
                                    mapLoops.put(
                                            msgInstruction.getKey(),
                                            Integer.valueOf(msgInstruction.getValue())); // <id:orderId:blockName>
                                }

                            } else if (actions[0].equalsIgnoreCase(ABRConstants.LOOP)) {
                                // <currentId:parentId:parentName>
                                msgInstruction = performAction.getInstructionDetailsById(
                                        blocksLoaded.get(currentBlock).getBlockLoopInstructionLoadDTOS(),
                                        currentInstruction);
                                if (!mapLoops.containsKey(msgInstruction.getKey())) {
                                    mapLoops.put(msgInstruction.getKey(), Integer.valueOf(msgInstruction.getValue()));
                                }
                            } else if (actions[0].equalsIgnoreCase(ABRConstants.REFRESH_LOOP)) {
                                msgInstruction = performAction.getInstructionDetailsById(
                                        blocksLoaded.get(currentBlock).getBlockLoopInstructionLoadDTOS(),
                                        currentInstruction);
                                if (!mapLoops.containsKey(msgInstruction.getKey())) {
                                    String[] parts = msgInstruction.getValue().split(":"); // Split by ':'
                                    mapLoops.put(msgInstruction.getKey(), Integer.valueOf(parts[1])); // Loop Times
                                    mapRefresh.put(msgInstruction.getKey(), Integer.valueOf(parts[0])); // Wait Time
                                }
                            } else {
                                msgInstruction = new Pair(
                                        currentInstruction.getName(),
                                        (currentInstruction.getOperation() != null
                                                ? currentInstruction.getOperation()
                                                : (actions[0].equalsIgnoreCase(ABRConstants.INSERT))
                                                        ? valueInsert
                                                        : ""));
                            }

                            resultActions = performAction.actionResultMessage(blockName, actions, msgInstruction);

                            extraMsg = "";

                            if (actions[0].equalsIgnoreCase(ABRConstants.PAUSE)) {
                                pauseOperation = true;

                                ABRLogger.getInstance(Engine.class)
                                        .info(String.format("PAUSE BOT JOB at Block Name:\"%s\"", blockLoad.getName()));

                                //                                SwingUtilities.invokeLater(() ->
                                performAction.showCustomModalDialog(
                                        "PAUSE BOT JOB",
                                        String.format("PAUSE BOT JOB at Block Name:\"%s\"", blockLoad.getName()),
                                        " Please click OK to continue!",
                                        null,
                                        null,
                                        false);
                            }

                            if (actions[0].equalsIgnoreCase(ABRConstants.LOOP)) {
                                parentFieldLoop =
                                        performAction.getInstructionParentField(currentInstruction, blockLoad);
                                if (parentField == null) {
                                    parentField = parentFieldLoop;
                                }
                                parentFieldLoop = currentInstruction.getId() + ":" + parentId + ":" + parentFieldLoop;

                                if (mapLoops.containsKey(parentFieldLoop)) {
                                    int currentLoop = mapLoops.get(parentFieldLoop);
                                    if (currentLoop > 0) {
                                        jumpLoop = true;
                                        refreshLoop = false;
                                    } else {

                                        jumpLoop = false;
                                        refreshLoop = false;

                                        //                                    String[] parts =
                                        // parentFieldLoop.split(":");
                                        //
                                        // ABRLogger.getInstance(Engine.class)
                                        //                                            .info(String.format(
                                        //                                                    "IGNORING Loop to Parent
                                        // :\"%s\" - %d Times",
                                        //                                                    parts[0] + "-(" + parts[1]
                                        // +
                                        // ") " + parts[2],
                                        //
                                        // mapLoops.get(parentFieldLoop)));
                                        continue;
                                    }

                                } else {
                                    jumpLoop = true;
                                    refreshLoop = false;
                                }

                            } else if (actions[0].equalsIgnoreCase(ABRConstants.REFRESH_ONLY)) {
                                refreshOnly = true;
                            } else if (actions[0].equalsIgnoreCase(ABRConstants.REFRESH_LOOP)) {
                                parentFieldLoop =
                                        performAction.getInstructionParentField(currentInstruction, blockLoad);
                                if (parentField == null) {
                                    parentField = parentFieldLoop;
                                }
                                parentFieldLoop = currentInstruction.getId() + ":" + parentId + ":" + parentFieldLoop;

                                if (mapLoops.containsKey(parentFieldLoop)) {
                                    int currentLoop = mapLoops.get(parentFieldLoop);
                                    if (currentLoop > 0) {
                                        jumpLoop = true;
                                        refreshLoop = true;
                                    } else {

                                        jumpLoop = false;
                                        refreshLoop = false;

                                        //                                    String[] parts =
                                        // parentFieldLoop.split(":");
                                        //
                                        // ABRLogger.getInstance(Engine.class)
                                        //                                            .info(String.format(
                                        //                                                    "IGNORING Refresh Loop to
                                        // Parent :\"%s\" - %d Times",
                                        //                                                    parts[0] + "-(" + parts[1]
                                        // +
                                        // ") " + parts[2],
                                        //
                                        // mapLoops.get(parentFieldLoop)));
                                        continue;
                                    }

                                } else {
                                    jumpLoop = true;
                                    refreshLoop = true;
                                }
                            } else if (actions[0].equalsIgnoreCase(ABRConstants.GOTO)) {
                                jumpGoto = true;
                            } else if (actions[0].equalsIgnoreCase(ABRConstants.GET_VALUE)
                                    || actions[0].equalsIgnoreCase(ABRConstants.SET_VALUE)) {

                                resultActions = currentInstruction.getName()
                                        + ABRConstants.BLANK_STRING
                                        + currentInstruction.getActions()
                                        + ABRConstants.BLANK_STRING
                                        + currentInstruction.getOperation();

                                execOperation = true;

                                xPathOperation = performAction.getXPathInstruction(currentInstruction, blockLoad);
                                parentField = performAction.getInstructionParentField(currentInstruction, blockLoad);

                            } else if (actions[0].equalsIgnoreCase(ABRConstants.CHECK_VALUE)) {

                                resultActions = currentInstruction.getName()
                                        + ABRConstants.BLANK_STRING
                                        + currentInstruction.getActions()
                                        + ABRConstants.BLANK_STRING
                                        + currentInstruction.getOperation();

                                checkOperation = true;
                                parentField = performAction.getInstructionParentField(currentInstruction, blockLoad);

                            } else if (actions[0].equalsIgnoreCase(ABRConstants.EXTRACT_FIELD)) {

                                resultActions = currentInstruction.getName()
                                        + ABRConstants.BLANK_STRING
                                        + currentInstruction.getActions()
                                        + ABRConstants.BLANK_STRING
                                        + currentInstruction.getOperation();

                                excelWriteOperation = true;

                                parentField = performAction.getInstructionParentField(currentInstruction, blockLoad);
                            }

                            File logFileForSingleExcel = excelReader.createLogFile(excelPath);

                            // fillUpCurretLocators(currentInstruction);

                            try {
                                if (jumpGoto) {

                                    msgInstruction =
                                            performAction.getBlockDetailsById(blocksLoaded, currentInstruction);

                                    if (!loopBlockActive.contains(msgInstruction.getKey())) {
                                        loopBlockActive.add(msgInstruction.getKey());
                                        loopBlockLimits.put(
                                                msgInstruction.getKey(), Integer.valueOf(msgInstruction.getValue()));
                                    }
                                    int repeat = mapLoops.get(msgInstruction.getKey()) - 1;
                                    if (repeat > 0) {
                                        mapLoops.put(msgInstruction.getKey(), repeat);
                                        try {

                                            String[] parts =
                                                    msgInstruction.getKey().split(":");
                                            int blockOrderNumber = Integer.parseInt(parts[2]);

                                            currentBlock = blockOrderNumber - 1;
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

                                            executedSuccess.add(currentInstruction.getId());
                                            success = true;

                                        } catch (Exception ex) {
                                            resultActions = "Failed " + resultActions;

                                            success = false;

                                            resultActions = performAction.blockGotoFailed(resultActions);
                                        }

                                        Pair<String, String> currentPair = new Pair(
                                                msgInstruction.getKey(),
                                                String.valueOf(mapLoops.get(msgInstruction.getKey())));

                                        // Excel Report and Log
                                        performAction.logAndReport(
                                                currentCondition,
                                                true,
                                                true,
                                                currentInstructionStartTime,
                                                blockReportName,
                                                success,
                                                actions,
                                                currentPair,
                                                dataExcel,
                                                writerReport,
                                                mainMsg,
                                                resultActions);

                                        if (success) {
                                            continue blockLoop;
                                        } else {
                                            stopAll = true;
                                            if (stopAll) {
                                                continue blockLoop;
                                            }
                                        }

                                    } else {
                                        mapLoops.put(msgInstruction.getKey(), repeat);
                                        continue blockLoop;
                                    }

                                } else if (jumpLoop) {

                                    if (mapLoops.containsKey(parentFieldLoop)) {

                                        int repeat = mapLoops.get(parentFieldLoop) - 1;
                                        String[] parts = parentFieldLoop.split(":");
                                        if (repeat > 0) {
                                            mapLoops.put(parentFieldLoop, repeat);

                                            ABRLogger.getInstance(Engine.class)
                                                    .info(String.format(
                                                            "Loop to Parent :\"%s\" - %d Times",
                                                            parts[0] + "-(" + parts[1] + ") " + parts[2],
                                                            mapLoops.get(parentFieldLoop)));

                                            if (refreshLoop) {

                                                String extraLog = performAction.actionResultMessage(
                                                        blockName,
                                                        new String[] {ABRConstants.REFRESH_HOLD},
                                                        msgInstruction);

                                                performAction.performOtherActions(
                                                        byPassNotFound,
                                                        currentInstruction,
                                                        new String[] {ABRConstants.REFRESH_HOLD});

                                                // Excel Report and Log
                                                performAction.logAndReport(
                                                        currentCondition,
                                                        true,
                                                        true,
                                                        currentInstructionStartTime,
                                                        blockReportName,
                                                        success,
                                                        new String[] {ABRConstants.REFRESH_HOLD},
                                                        msgInstruction,
                                                        dataExcel,
                                                        writerReport,
                                                        mainMsg,
                                                        extraLog);

                                                // Refresh For REFRESH_LOOP
                                                extraLog = performAction.actionResultMessage(
                                                        blockName,
                                                        new String[] {ABRConstants.REFRESH_ONLY},
                                                        msgInstruction);

                                                performAction.performOtherActions(
                                                        byPassNotFound,
                                                        currentInstruction,
                                                        new String[] {ABRConstants.REFRESH_ONLY});

                                                // Excel Report and Log
                                                performAction.logAndReport(
                                                        currentCondition,
                                                        true,
                                                        true,
                                                        currentInstructionStartTime,
                                                        blockReportName,
                                                        success,
                                                        new String[] {ABRConstants.REFRESH_ONLY},
                                                        msgInstruction,
                                                        dataExcel,
                                                        writerReport,
                                                        mainMsg,
                                                        extraLog);

                                                refreshLoop = false;
                                            }

                                            for (int x = 0; x < instructionIds.length; x++) {
                                                if (instructionIds[x] == parentId) {
                                                    currentIndex = x;
                                                    break; // Exit the loop once the value is found
                                                }
                                            }

                                            // Get Correct Updated Pair for REFRESH_LOOP ACTION
                                            Pair<String, String> currentPair = new Pair(
                                                    msgInstruction.getKey(),
                                                    String.valueOf(mapLoops.get(msgInstruction.getKey())));

                                            // Excel Report and Log
                                            performAction.logAndReport(
                                                    currentCondition,
                                                    true,
                                                    true,
                                                    currentInstructionStartTime,
                                                    blockReportName,
                                                    success,
                                                    actions,
                                                    currentPair,
                                                    dataExcel,
                                                    writerReport,
                                                    mainMsg,
                                                    resultActions);

                                        } else {
                                            mapLoops.put(parentFieldLoop, repeat);
                                        }

                                        jumpLoop = false;
                                        refreshLoop = false;

                                        if (repeat > 0) {
                                            continue instructionLoop;
                                        } else {
                                            ABRLogger.getInstance(Engine.class)
                                                    .info(String.format(
                                                            "IGNORING Loop to Parent :\"%s\" - %d Times",
                                                            parts[0] + "-(" + parts[1] + ") " + parts[2],
                                                            mapLoops.get(parentFieldLoop)));
                                            continue;
                                        }

                                    } else {
                                        resultActions = performAction.parentValueIsNotDefined(
                                                currentInstruction.getName(),
                                                "(" + parentId + ")-" + parentField,
                                                resultActions);

                                        stopAll = true;
                                        success = false;
                                        if (stopAll) {
                                            break;
                                        }
                                    }

                                } else if (refreshOnly) {

                                    ABRLogger.getInstance(Engine.class)
                                            .info("Refresh Current Web Page ->  inside Block :\"" + blockLoad.getName()
                                                    + "\"");

                                    performAction.performOtherActions(byPassNotFound, currentInstruction, actions);

                                    // Excel Report and Log
                                    performAction.logAndReport(
                                            currentCondition,
                                            true,
                                            true,
                                            currentInstructionStartTime,
                                            blockReportName,
                                            success,
                                            actions,
                                            msgInstruction,
                                            dataExcel,
                                            writerReport,
                                            mainMsg,
                                            resultActions);

                                    refreshOnly = false;

                                    continue;

                                } else if (actions[0].equals(ABRConstants.HOLD)
                                        || actions[0].equals(ABRConstants.QUIT)
                                        || actions[0].equals(ABRConstants.SCREEN)
                                        || actions[0].equals(ABRConstants.REFRESH_ONLY)) {

                                    performAction.performOtherActions(byPassNotFound, currentInstruction, actions);

                                    if (actions[0].equals(ABRConstants.QUIT)) {
                                        stopAll = true;
                                        success = true;
                                    }

                                } else if (!execOperation
                                        && !checkOperation
                                        && !excelWriteOperation
                                        && !pauseOperation) {

                                    // Extract dataFieldName and dataFieldValue using a separate method
                                    Pair<String, String> fieldData = performAction.extractFieldData(
                                            dataExcel,
                                            actions,
                                            currentInstruction.getDefaultValue(),
                                            currentInstruction.getCodified());

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

                                    byPassNotFound = byPassFlagLoop
                                            || !currentCondition.equals(ABRConstants.ConditionStatus.NONE);

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
                                                msgInstruction = new Pair(fieldName, mapOperators.get(fieldName));
                                            } else {
                                                msgInstruction = new Pair(fieldName, "TEXT OUTPUT NOT FOUND");
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
                                    }

                                } else if (execOperation) {
                                    // GET && SET Special Operators

                                    if (xPathOperation != null && parentField != null && operations.length == 2) {
                                        //                                    fieldName = parentField;
                                        parentField = parentId + "-" + parentField;

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
                                        resultActions = performAction.parentIdWrongBlock(
                                                currentInstruction, blockLoad, resultActions, currentCondition);

                                        success = false;
                                        if (currentCondition.equals(ABRConstants.ConditionStatus.NONE)) {
                                            stopAll = true;
                                        }

                                        if (stopAll) {
                                            break;
                                        }
                                    }

                                } else if (checkOperation) {
                                    // Check Validation Operator

                                    if (parentField != null) {
                                        parentField = parentId + "-" + parentField;
                                    }

                                    if (parentField == null) {
                                        resultActions = performAction.parentIdWrongBlock(
                                                currentInstruction, blockLoad, resultActions, currentCondition);

                                        resultActions = performAction.getValueIsNotDefined(
                                                currentInstruction, resultActions, currentCondition);

                                        success = false;
                                        if (currentCondition.equals(ABRConstants.ConditionStatus.NONE)) {
                                            stopAll = true;
                                        }

                                        if (stopAll) {
                                            break;
                                        }

                                    } else if (!mapOperators.containsKey(parentField)) {
                                        resultActions = performAction.getValueIsNotDefined(
                                                currentInstruction, resultActions, currentCondition);

                                        success = false;
                                        if (currentCondition.equals(ABRConstants.ConditionStatus.NONE)) {
                                            stopAll = true;
                                        }

                                        if (stopAll) {
                                            break;
                                        }
                                    } else {
                                        //                                    fieldName = parentField;

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
                                            resultActions = performAction.checkValidationFailed(
                                                    parentField,
                                                    mapOperators.get(parentField),
                                                    resultActions,
                                                    operations,
                                                    currentCondition,
                                                    byPassNotFound);

                                            success = false;
                                            if (currentCondition.equals(ABRConstants.ConditionStatus.NONE)) {
                                                stopAll = true;
                                            }

                                            if (stopAll) {
                                                break;
                                            }
                                        }
                                    }

                                } else if (excelWriteOperation && operations.length == 2) {
                                    // Excel Write Operator

                                    if (parentField != null) {
                                        fieldName = parentField;
                                        parentField = parentId + "-" + parentField;
                                    }

                                    if (parentField == null) {

                                        resultActions = performAction.parentIdWrongBlock(
                                                currentInstruction, blockLoad, resultActions, currentCondition);

                                        success = false;
                                        if (currentCondition.equals(ABRConstants.ConditionStatus.NONE)) {
                                            stopAll = true;
                                        }

                                        if (stopAll) {
                                            break;
                                        }
                                    } else if (!mapOperators.containsKey(parentField)) {
                                        resultActions = performAction.getValueIsNotDefined(
                                                currentInstruction, resultActions, currentCondition);

                                        success = false;
                                        if (currentCondition.equals(ABRConstants.ConditionStatus.NONE)) {
                                            stopAll = true;
                                        }

                                        if (stopAll) {}
                                    } else {

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

                                            resultActions = "insertValueFieldNameInExcel -> " + parentField + "-"
                                                    + mapOperators.get(parentField);
                                        } else {
                                            resultActions = "NO Export Excel File defined -> " + parentField + "-"
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

                                            writerExport.insertFieldNameAndValueLastColumn(mapExport, exportIndex - 1);
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
                                    }
                                }

                            } catch (Throwable t) {
                                success = false;
                                if (currentCondition.equals(ABRConstants.ConditionStatus.NONE)) {
                                    stopAll = true;
                                }

                                currentInstruction.setExecuted(false);

                                if (stopAll) {
                                    break;
                                }

                                //                            throw new RuntimeException(t);
                            }

                            printLog(generateTimestamp(), logFileForSingleExcel, resultActions, success);

                            // Here mark the Status of a progress Condition Fail or Success at the end of each Kind
                            // of Execution
                            if (!currentCondition.equals(ABRConstants.ConditionStatus.NONE)) {
                                progressCondition = performAction.updateProgressSuccess(success, currentCondition);
                                //                                continue instructionLoop;
                            }

                            if (byPassFlagLoop) {

                                // Excel Report and Log
                                performAction.logAndReport(
                                        currentCondition,
                                        true,
                                        true,
                                        currentInstructionStartTime,
                                        blockReportName,
                                        success,
                                        new String[] {ABRConstants.BY_PASS},
                                        msgInstruction,
                                        dataExcel,
                                        writerReport,
                                        "By Passing Loop Flag",
                                        resultActions);
                            } else {

                                // Excel Report and Log
                                performAction.logAndReport(
                                        currentCondition,
                                        true,
                                        true,
                                        currentInstructionStartTime,
                                        blockReportName,
                                        success,
                                        actions,
                                        msgInstruction,
                                        dataExcel,
                                        writerReport,
                                        mainMsg,
                                        resultActions);
                            }

                            // It decides Here if ByPass as per Loop or Per IF-ELSEIF-ELSE-ENDIF blocks
                            if (!success
                                    && !byPassFlagLoop
                                    && currentCondition.equals(ABRConstants.ConditionStatus.NONE)) {
                                stopAll = true;
                                break;
                            }

                            // Close Browser Action
                            if (resultActions.equalsIgnoreCase("Close Browser")) {
                                stopAll = true;
                                break;
                            }

                            // Here it Call the next block of IF, ELSIF, ELSE OR ENDIF as Per the Machine State
                            // Conditions When Pass to any of then
                            if (progressCondition.equals(ABRConstants.ConditionStatus.IF_PASSED)
                                    || progressCondition.equals(ABRConstants.ConditionStatus.ELSEIF_PASSED)) {
                                int jumpPassed = performAction.checkActionToJump(
                                        actions[0],
                                        progressCondition,
                                        mapConditional,
                                        parentBlockCondition,
                                        currentIndex);

                                // Any Error
                                if (jumpPassed < 0) {
                                    stopAll = true;
                                    continue blockLoop;
                                }
                                // Found Next Block
                                if (jumpPassed > 0) {
                                    currentIndex = jumpPassed;
                                    // reset all Conditional
                                    currentCondition = ABRConstants.ConditionStatus.NONE;
                                    progressCondition = ABRConstants.ConditionStatus.NONE;
                                    continue instructionLoop;
                                }
                            }

                            // Conditions When Fails to any of then and Look for the next Correct Block
                            if (progressCondition.equals(ABRConstants.ConditionStatus.IF_FAILED)
                                    || progressCondition.equals(ABRConstants.ConditionStatus.ELSEIF_FAILED)) {

                                // Goes to the next ELSEIF IF EXIST (ELSEIF index + 1);
                                int index = performAction.searchMapConditional(
                                        mapConditional,
                                        parentBlockCondition,
                                        ABRConstants.ConditionStatus.ELSEIF,
                                        currentIndex,
                                        false);

                                // Goes to the next ELSE IF ELSEIF  DOES NOT EXIST  (ELSE index + 1);
                                if (index < 0) {
                                    index = performAction.searchMapConditional(
                                            mapConditional,
                                            parentBlockCondition,
                                            ABRConstants.ConditionStatus.ELSE,
                                            currentIndex,
                                            true);
                                }
                                if (index < 0) {
                                    stopAll = true;
                                    continue blockLoop;
                                }
                                currentIndex = index;
                                currentCondition = ABRConstants.ConditionStatus.NONE;
                                progressCondition = ABRConstants.ConditionStatus.NONE;
                                continue instructionLoop;

                            } else if (progressCondition.equals(ABRConstants.ConditionStatus.ELSE_FAILED)) {
                                // Goes to the ENDIF (ENDIF index + 1);
                                int index = performAction.searchMapConditional(
                                        mapConditional,
                                        parentBlockCondition,
                                        ABRConstants.ConditionStatus.ENDIF,
                                        currentIndex,
                                        true);

                                if (index < 0) {
                                    stopAll = true;
                                    continue blockLoop;
                                }
                                currentIndex = index;
                                currentCondition = ABRConstants.ConditionStatus.NONE;
                                progressCondition = ABRConstants.ConditionStatus.NONE;
                                continue instructionLoop;
                            }
                        }
                    }

                    currentBlock++;
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
                                    currentInstruction.getActions(), ABRConstants.ACTION_SPECIFICATIONS_SPLITTER);
                            if (arr.length > 1) {
                                String dataFieldName = arr[1].split(ABRConstants.PATH_FIELD_SUBSTITUTION)[0];
                                performAction.insertRandomName(dataFieldName);
                            }
                        }
                    }
                }
                for (int j = 0; success && j < blocksLoaded.size(); j++) {

                    String blockName = blocksLoaded.get(j).getName();
                    int blockOrder = blocksLoaded.get(j).getBlockOrderNumber();
                    String blockReportName = "#" + blockOrder + " " + blockName;

                    // Call the method to get the filtered list
                    List<BlockLoopInstructionLoadDTO> unexecutedInstructions = getUnexecutedInstructions(
                            instructionsExecuted, blocksLoaded.get(j).getBlockLoopInstructionLoadDTOS());

                    for (BlockLoopInstructionLoadDTO currentInstruction : unexecutedInstructions) {

                        long currentInstructionStartTime = System.nanoTime();
                        File logFileForSingleExcel = excelReader.createLogFile(excelPath);

                        String[] actions =
                                currentInstruction.getActions().split(ABRConstants.ACTIONS_AND_PATHS_SPLITTER);

                        // Case for Inputs
                        String valueInsert = "No Data Found";
                        if (actions[0].equalsIgnoreCase(ABRConstants.INSERT)) {

                            String reference = actions[1];
                            valueInsert = dataExcel.get(reference);
                        }

                        Pair<String, String> msgInstruction = new Pair(
                                currentInstruction.getName(),
                                (currentInstruction.getOperation() != null
                                        ? currentInstruction.getOperation()
                                        : (actions[0].equalsIgnoreCase(ABRConstants.INSERT)) ? valueInsert : ""));

                        resultActions = performAction.actionResultMessage(blockName, actions, msgInstruction);

                        try {

                            if (actions[0].equals(ABRConstants.HOLD)
                                    || actions[0].equals(ABRConstants.QUIT)
                                    || actions[0].equals(ABRConstants.SCREEN)
                                    || actions[0].equals(ABRConstants.REFRESH_ONLY)) {
                                performAction.performOtherActions(byPassNotFound, currentInstruction, actions);

                                if (actions[0].equals(ABRConstants.QUIT)) {
                                    stopAll = true;
                                    success = true;
                                }

                                // Excel Report and Log
                                performAction.logAndReport(
                                        currentCondition,
                                        true,
                                        true,
                                        currentInstructionStartTime,
                                        blockReportName,
                                        success,
                                        actions,
                                        msgInstruction,
                                        dataExcel,
                                        writerReport,
                                        mainMsg,
                                        resultActions);

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

                            // Excel Report and Log
                            performAction.logAndReport(
                                    currentCondition,
                                    true,
                                    true,
                                    currentInstructionStartTime,
                                    blockReportName,
                                    success,
                                    actions,
                                    msgInstruction,
                                    dataExcel,
                                    writerReport,
                                    mainMsg,
                                    resultActions);

                        } catch (Throwable t) {
                            success = false;
                            currentInstruction.setExecuted(false);

                            // Excel Report and Log
                            performAction.logAndReport(
                                    currentCondition,
                                    true,
                                    true,
                                    currentInstructionStartTime,
                                    blockReportName,
                                    success,
                                    actions,
                                    msgInstruction,
                                    dataExcel,
                                    writerReport,
                                    mainMsg,
                                    resultActions);

                            //                        throw new RuntimeException(t);
                        }
                        printLog(generateTimestamp(), logFileForSingleExcel, resultActions, success);
                    }
                }
            }

            totalExecutionTime = performAction.getTotalExecutionTime();

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
                        + ABRConstants.FIELDS_SEPARATOR
                        + labelsValue.getProperty(Labels.END)
                        + ABRConstants.FIELDS_SEPARATOR
                        + labelsValue.getProperty(Labels.OK);

                System.out.println(String.format("Success: %s Last Execution: %s", botJobName, resultActions));

            } else {
                baseLogString = botLoadJobs.get(0).getName()
                        + ABRConstants.FIELDS_SEPARATOR
                        + labelsValue.getProperty(Labels.END)
                        + ABRConstants.FIELDS_SEPARATOR
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
        String resultMsg = result ? ABRConstants.SUCCESS : ABRConstants.FAIL;
        String log = String.join(ABRConstants.FIELDS_SEPARATOR, timeStamp, resultMsg, resultActions);

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
        String log = String.join(ABRConstants.FIELDS_SEPARATOR, timeStamp, msg);

        try {
            FileWriter fileWriter = new FileWriter(logFile, true);
            fileWriter.write(log + System.lineSeparator());
            fileWriter.close();
        } catch (Exception e) {
            ABRLogger.getInstance(WebPage.class).severe("printBaseLog Error: " + e.getMessage());
        }
    }

    private static void printLogExcel(String timeStamp, File logExcel, Map<String, String> data, boolean result) {
        String resultMsg = result ? ABRConstants.SUCCESS : ABRConstants.FAIL;

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
