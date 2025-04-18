package com.allinweb.ch;

import com.allinweb.ch.component.model.BlockLoadDTO;
import com.allinweb.ch.component.model.BotJobLoadDTO;
import com.allinweb.ch.component.model.HomeBankingLoadDTO;
import com.allinweb.ch.component.model.InstructionLoadDTO;
import com.allinweb.ch.component.model.InstructionReferenceLoadDTO;
import com.allinweb.ch.component.model.RowStatus;
import com.allinweb.ch.component.model.VariableLoadDTO;
import com.allinweb.ch.driver.ARWebDriver;
import com.allinweb.ch.facade.PerformActions;
import com.allinweb.ch.facade.PerformDataBase;
import com.allinweb.ch.facade.PerformMessage;
import com.allinweb.ch.readersAndWriters.ExcelReader;
import com.allinweb.ch.readersAndWriters.ExcelWriter;
import com.allinweb.ch.util.*;
import com.google.common.base.Strings;
import io.opentelemetry.api.internal.StringUtils;
import java.io.*;
import java.text.SimpleDateFormat;
import java.time.Duration;
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
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Engine {
    private static SimpleDateFormat dateFormatter;
    static final String EXECUTE_JOB = "execute/j";

    private static final String language = "en";
    private static File baseLogFile = null;

    private static Map<String, String> mapOperators;
    private static Map<String, String> mapExport;
    private static List<VariableLoadDTO> variablesLoaded;

    private static RowStatus rowStatus = new RowStatus();

    private static List<BotJobLoadDTO> botLoadJobs = new ArrayList<>();

    private static final ARPropertyManager arPropertyManager;
    private static final PerformMessage performMessage;
    private static final PerformDataBase performDataBase;
    private static final PerformActions performActions;
    private static ARPriorities abrPriorities;
    private static ARWebDriver currentARWebDriver;

    // Static block to initialize
    static {
        arPropertyManager = ARPropertyManager.getInstance();
        performMessage = PerformMessage.getInstance();
        performDataBase = PerformDataBase.getInstance();
        performActions = PerformActions.getInstance();
        abrPriorities = ARPriorities.getInstance();
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
            System.setProperty("ARWebConfig", configurationValue);
            arPropertyManager.loadProperties();
        }

        Labels.initializeLabelsInSpecLang(language);

        List<String> missingProperties = checkProperties(arPropertyManager.getProperties());

        if (!missingProperties.isEmpty()) {

            int totalMissing = missingProperties.size();
            int partSize = (int) Math.ceil((double) totalMissing / 3); // Divide into 3 parts

            String part1 = String.join(", ", missingProperties.subList(0, Math.min(partSize, totalMissing)));
            String part2 = totalMissing > partSize
                    ? String.join(", ", missingProperties.subList(partSize, Math.min(2 * partSize, totalMissing)))
                    : "";
            String part3 = totalMissing > 2 * partSize
                    ? String.join(", ", missingProperties.subList(2 * partSize, totalMissing))
                    : "";

            performMessage.errorMessage(
                    "I cannot Execute Engine", "Missing required properties: ", part1, part2, part3, 0);
            return;
        }

        //        for (String arg : arguments) {
        //            ARLogger.getInstance(Engine.class).fine("Argument: " + arg);
        //        }

        //        repository = new Repository(sessionFactory);

        try {
            baseLogFile = new File(arPropertyManager.getProperty(ARPropertyEnum.FOLDER_PATH_LOG)
                    + ARConstants.FILE_NAME_ENGINE_BASE_LOG);
        } catch (Exception e) {
            ARLogger.getInstance(Engine.class).severe("baseLogFile Error: " + e.getMessage());
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

        String dataBaseType = arPropertyManager.getProperty(ARPropertyEnum.DATABASE_TYPE);
        performDataBase.initialize(dataBaseType);
        performDataBase.changeDbConnection();

        try {
            startParametersInterpreter(args);
        } catch (Exception e) {
            ARLogger.getInstance(Engine.class).severe("Main class Start Error: " + e.getMessage());
        }

        //        repository.closeSession();
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

        HomeBankingLoadDTO homeBanking = performDataBase.loadHomeBanking(homeBankingId);

        if (homeBanking == null || StringUtils.isNullOrEmpty(homeBanking.getUrl())) {
            ARLogger.getInstance(Engine.class).severe("Cannot find Home Banking Environment Id:" + homeBankingId);
            return false;
        }

        botLoadJobs = performDataBase.loadCompleteJobs(botJobId);

        if (botLoadJobs.size() < 1) {
            ARLogger.getInstance(Engine.class).severe("Cannot find Bot Jobs with this Id:" + botJobId);
            return false;
        }

        List<BlockLoadDTO> blocksLoaded = botLoadJobs.get(0).getBlockLoadDTOList();

        String excelPath = idsAndPaths[2];

        // Assuming blocksLoaded is your List<BlockLoadDTO>
        List<String> allActions = blocksLoaded.stream()
                .flatMap(blockLoadDTO ->
                        blockLoadDTO.getInstructionLoadDTOS().stream()) // Flatten the stream of InstructionLoadDTO
                .map(InstructionLoadDTO::getActions) // Extract the actions
                .collect(Collectors.toList()); // Collect all actions into a List

        ExcelReader excelReader = new ExcelReader();
        ExtractedData extractedData = null;
        try {
            extractedData = excelReader.extractData(excelPath, allActions);
        } catch (Exception e) {

            performMessage.errorMessage(
                    "Excel Error",
                    "Could Not Execute Excel File",
                    "Check All Excel Columns and Values!",
                    null,
                    null,
                    0);
            //            Platform.exit();
        }

        try {

            //            String browser = arPropertyManager.getProperty(ARPropertyEnum.BROWSER);
            //            WebPage webPage = new WebPage(
            //                    browser,
            //                    homeBankingDTO.getUrl(),
            //                    homeBankingDTO.getPriority(),
            //                    homeBankingDTO.getOptionsConfig(),
            //                    mapOperators);

            currentARWebDriver = new ARWebDriver();

            String browserType = arPropertyManager.getProperty(ARPropertyEnum.BROWSER);
            String webDriverPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_WEBDRIVER);

            currentARWebDriver.openDriver(
                    browserType, webDriverPath, homeBanking.getUrl(), homeBanking.getOptionsConfig(), null, false, 0);

            // Ensure botJob and abrPriorities are not null before accessing their methods
            if (botLoadJobs.get(0) != null && abrPriorities != null) {
                // Check if we need to update abrPriorities
                if (abrPriorities.getJobId() == null
                        || !abrPriorities.getJobId().equals(botLoadJobs.get(0).getId())) {
                    // Set Job ID in abrPriorities
                    abrPriorities.setJobId(botLoadJobs.get(0).getId());

                    // Check for non-null HomeBanking and Priority
                    if (homeBanking != null) {
                        String priorityValue = homeBanking.getPriority();
                        String searchConfig = homeBanking.getSearchConfig();

                        if (priorityValue != null) {
                            abrPriorities.loadPrioritiesFromString(priorityValue);
                        } else {
                            abrPriorities.loadPriorities();
                        }

                        abrPriorities.loadSearchElementsConfig(searchConfig);
                    }

                    // Initialize performAction with abrPriorities and arWebDriver
                    performActions.initialize(abrPriorities);
                    performActions.setCurrentDriver(currentARWebDriver.getCurrentDriver());
                }
            }
            if (performActions.waitForPage == null) {
                String updateTimeout = arPropertyManager.getProperty(ARPropertyEnum.WEBDRIVER_PAGE_UPDATE_TIMEOUT_SEC);
                String interactionTimeout =
                        arPropertyManager.getProperty(ARPropertyEnum.WEBDRIVER_PAGE_UPDATE_TIMEOUT_SEC);
                performActions.waitForPage = new WebDriverWait(
                        currentARWebDriver.getCurrentDriver(), Duration.ofSeconds(Integer.parseInt(updateTimeout)));
                performActions.waitForAction = new WebDriverWait(
                        currentARWebDriver.getCurrentDriver(),
                        Duration.ofSeconds(Integer.parseInt(interactionTimeout)));
            }

            String botJobName = botLoadJobs.get(0).getName();

            String baseLogString = blocksLoaded.get(0).getBotJobName()
                    + ARConstants.FIELDS_SEPARATOR
                    + labelsValue.getProperty(Labels.START);

            printBaseLog(baseLogFile, generateTimestamp(), baseLogString);

            ExcelWriter.ExcelChain writerReport = new ExcelWriter(
                            botLoadJobs.get(0).getName(), currentARWebDriver.getCurrentDriver(), false)
                    .withPurpose("report");
            writerReport.insertReportHead();

            ExcelWriter.ExcelChain writerExport = null;
            //                new ExcelWriter(blocksLoaded.get(0).getName(),
            // arWebDriver.getCurrentDriver()).withPurpose("export");
            boolean excelExportOnceCreation = true;
            //        writerExport.insertReportHead();

            Set<String> mapIgnore = new HashSet<>();

            String mainMsg = "";
            boolean byPassNotFound = false;
            boolean byPassFlagLoop;
            boolean success = true;
            boolean stopAll = false;
            long botJobStartTime = System.nanoTime();
            long totalExecutionTime = 0;
            String resultActions = "No instruction executed yet";
            String failedMessage = "";
            Map<String, String> dataExcel = null;

            // clearFields();

            // Execute All Blocks starting from executeSpecificBlock if Defined
            // int executeSpecificBlock = comboBoxBlocks.getValue().getVarId();
            String sessionRowStatus = "botJobTasks-" + botJobId;

            mapOperators = new HashMap<>();
            mapExport = new LinkedHashMap<>();
            variablesLoaded = performDataBase.loadAllVariables(botJobId);
            Map<String, String> mapSavedLocators = new HashMap<>();

            Set<Integer> parentIdsForLoop = null;
            Map<String, List<Integer>> mapConditional = new HashMap<>(); // <parentId:Limit Loops> -> <1|5 Times>
            Map<String, Integer> mapLoops = new HashMap<>(); // <parentId:Limit Loops> -> <1|5 Times>
            Map<String, Integer> mapRefresh = new HashMap<>(); // <parentId:Limit Loops> -> <1|5 Times>
            Set<String> loopBlockActive = new HashSet<>();
            Map<String, Integer> loopBlockLimits = new HashMap<>();

            ARConstants.ConditionStatus currentCondition = ARConstants.ConditionStatus.NONE;
            ARConstants.ConditionStatus previousCondition;
            ARConstants.ConditionStatus progressCondition;
            ARConstants.DialogModal respModal = ARConstants.DialogModal.NONE;

            int exportIndex = 1;
            boolean webElementWork = false;

            if (extractedData.getNumberOfDataRows() > 0) {

                // Execute All Blocks starting from executeSpecificBlock if Defined
                int currentBlock = (executeSpecificBlock > -1) ? executeSpecificBlock - 1 : 0;

                blockLoop:
                while (currentBlock <= blocksLoaded.size() - 1 && !blocksLoaded.isEmpty() && !stopAll) {
                    long blockStartTime = System.nanoTime();

                    currentCondition = ARConstants.ConditionStatus.NONE;
                    previousCondition = ARConstants.ConditionStatus.NONE;
                    progressCondition = ARConstants.ConditionStatus.NONE;

                    respModal = ARConstants.DialogModal.NONE;

                    int parentBlockCondition = -1;

                    BlockLoadDTO blockLoad = blocksLoaded.get(currentBlock);

                    String excelFieldName = blockLoad.getExportFile();

                    String blockName = blocksLoaded.get(currentBlock).getName();
                    int blockOrder = blocksLoaded.get(currentBlock).getBlockOrderNumber();
                    String blockReportName = "#" + blockOrder + " " + blockName;

                    int blockWait = blocksLoaded.get(currentBlock).getWait() > 0
                            ? blocksLoaded.get(currentBlock).getWait()
                            : 2;
                    boolean blockActive = blocksLoaded.get(currentBlock).getActive();

                    // It Searches the Block That have finished the Loops to Avoid recursivity
                    if (loopBlockActive.size() > 0) {
                        for (String blocLoopKey : loopBlockActive) {
                            if (mapLoops.containsKey(blocLoopKey)) {
                                if (mapLoops.get(blocLoopKey) == 0) {
                                    stopAll = true;
                                    int limit = loopBlockLimits.get(blocLoopKey);

                                    Pair<String, String> msgBlock = new Pair(blocLoopKey, "0");

                                    // Excel Report and Log
                                    performActions.logAndReport(
                                            currentCondition,
                                            true,
                                            true,
                                            blockStartTime,
                                            blockReportName,
                                            success,
                                            new String[] {ARConstants.GOTO},
                                            msgBlock,
                                            dataExcel,
                                            writerReport,
                                            "GOTO Limit Reached",
                                            blocLoopKey + " Reached: 0");

                                    msgBlock = new Pair(
                                            String.format("Exit at Block Name: \"%s\"", blockLoad.getName()),
                                            ARConstants.EXIT);

                                    // Excel Report and Log
                                    performActions.logAndReport(
                                            currentCondition,
                                            true,
                                            true,
                                            blockStartTime,
                                            blockReportName,
                                            success,
                                            new String[] {ARConstants.EXIT},
                                            msgBlock,
                                            dataExcel,
                                            writerReport,
                                            "Stopping App",
                                            String.format("Exit at Block Name: \"%s\"", blockName));

                                    performActions.gotoLimitExecution(limit, resultActions);

                                    continue blockLoop;
                                }
                            }
                        }
                    }

                    if (!blockActive) {
                        currentBlock++;

                        Pair<String, String> msgBlock =
                                new Pair(String.format("Ignore: \"%s\"", blockLoad.getName()), ARConstants.IGNORE);

                        // Excel Report and Log
                        performActions.logAndReport(
                                currentCondition,
                                true,
                                true,
                                blockStartTime,
                                blockReportName,
                                success,
                                new String[] {ARConstants.IGNORE},
                                msgBlock,
                                dataExcel,
                                writerReport,
                                "BLOCK IGNORED",
                                String.format("Block: \"%s\" is Inactive: ", blockName));

                        continue;
                    }

                    try {

                        Pair<String, String> msgBlock = new Pair(blockLoad.getName(), ARConstants.EXCEL_BLOCK_HEADER);

                        // Block Header Format
                        performActions.logAndReport(
                                currentCondition,
                                true,
                                false,
                                blockStartTime,
                                blockReportName,
                                success,
                                new String[] {ARConstants.EXCEL_BLOCK_HEADER},
                                msgBlock,
                                null,
                                writerReport,
                                null,
                                null);

                        performActions.onHoldInSeconds(blockWait);

                        msgBlock = new Pair(
                                String.format("Default Wait: \"%s\" ->  %d Seconds", blockLoad.getName(), blockWait),
                                ARConstants.HOLD);

                        // Excel Report and Log
                        performActions.logAndReport(
                                currentCondition,
                                true,
                                true,
                                blockStartTime,
                                blockReportName,
                                success,
                                new String[] {ARConstants.HOLD},
                                msgBlock,
                                dataExcel,
                                writerReport,
                                "BLOCK DEFAULT WAIT",
                                String.format("Block: \"%s\" Wait %s Seconds: ", blockName, blockWait));

                    } catch (Exception ex) {
                        ARLogger.getInstance(Engine.class)
                                .severe(String.format("Error Wait Block for :\"%s\"", blockLoad.getName()));
                    }

                    // Step 1: Get all ParentIds For LOOPs Filter rows where actions = "REFRESH_LOOP" or "LOOP" on
                    // current
                    // Block
                    parentIdsForLoop = performActions.getParentIdsForLoop(
                            blocksLoaded.get(currentBlock).getInstructionLoadDTOS());

                    // Step 2: Get all Conditional By parentId for Index Locator on current Block Relocate "IF",
                    // "ELSEIF",
                    // "ELSE", and "ENDIF"
                    mapConditional = performActions.getConditionIndexMapByParentId(blockLoad);

                    // Step 3: Get all Instructions Ids on current Block
                    int[] instructionIds = blockLoad.getInstructionLoadDTOS().stream()
                            .mapToInt(InstructionLoadDTO::getId)
                            .toArray();

                    // Step 2: Filter rows where actions = "REFRESH_LOOP" or "LOOP" and collect into the map

                    //                mapLoops = performActions.getLoopAndRefreshLoops(
                    //                        blocksLoaded.get(currentBlock).getBlockLoopInstructionLoadDTOS());

                    //                executionTimes++;
                    boolean jumpGoto = false;
                    boolean jumpLoop = false;
                    boolean jumpGotoError = false;
                    boolean jumpLoopError = false;
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

                            //                            stopAll = isInterceptBotJob();
                            if (stopAll) {
                                break;
                            }

                            success = true;
                            webElementWork = false;

                            long currentInstructionStartTime = System.nanoTime();

                            InstructionLoadDTO currentInstruction =
                                    blockLoad.getInstructionLoadDTOS().get(currentIndex);

                            byPassFlagLoop = parentIdsForLoop.contains(currentInstruction.getId());

                            mainMsg =
                                    currentInstruction.getOptional() ? "OPTIONAL INSTRUCTION" : "MANDATORY INSTRUCTION";

                            if (!currentInstruction.getInstructionActive()) {

                                String nameInstruc =
                                        "(" + currentInstruction.getId() + ") " + currentInstruction.getName();
                                Pair<String, String> msgBlock =
                                        new Pair(String.format("Ignore: \"%s\"", nameInstruc), ARConstants.IGNORE);

                                // Excel Report and Log
                                performActions.logAndReport(
                                        currentCondition,
                                        true,
                                        true,
                                        blockStartTime,
                                        blockReportName,
                                        success,
                                        new String[] {ARConstants.IGNORE},
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
                            boolean execGetOrSet = false;
                            boolean execCheckValue = false;
                            boolean execOutPut = false;
                            boolean excelWriteOperation = false;
                            boolean pauseOperation = false;

                            String xPathOperation = null;
                            String[] parentActions = null;
                            String parentField = null;
                            String parentFieldLoop = null;
                            String variableField = null;
                            String fieldName = null;
                            int parentId = currentInstruction.getParentId();

                            if (mapIgnore.contains(currentInstruction.getId() + "-" + currentInstruction.getName())) {
                                continue;
                            }

                            // sendMessageJson(int homeBankingId, String sessionId, String msg1, String msg2)
                            String jsonStatus;
                            if (rowStatus.getInstructionId() == null) {
                                rowStatus.setInstructionId(currentInstruction.getId());
                                rowStatus.setColor("#fcba03"); // deep carmine yellow
                                //                                jsonStatus = gson.toJson(rowStatus);
                                //                                sendMessageJson(homeBanking.getId(), sessionRowStatus,
                                // jsonStatus, "rowStatus");
                            } else {
                                // Previous
                                rowStatus.setColor("#1d9c06"); // green
                                //                                jsonStatus = gson.toJson(rowStatus);
                                //                                sendMessageJson(homeBanking.getId(), sessionRowStatus,
                                // jsonStatus, "rowStatus");
                                try {
                                    Thread.sleep(300);
                                } catch (Exception e) {
                                }
                                // Current
                                rowStatus.setInstructionId(currentInstruction.getId());
                                rowStatus.setColor("#fcba03"); // deep carmine green
                                //                                jsonStatus = gson.toJson(rowStatus);
                                //                                sendMessageJson(homeBanking.getId(), sessionRowStatus,
                                // jsonStatus, "rowStatus");
                            }

                            //                        String[] operation =
                            // UtilsMethods.splitIfContains(instruction.getOperation(),
                            // ARConstants.ACTION_SPECIFICATIONS_SPLITTER);
                            String[] actions =
                                    currentInstruction.getActions().split(ARConstants.ACTION_SPECIFICATIONS_SPLITTER);
                            String[] operations = currentInstruction.getOperation() != null
                                    ? currentInstruction
                                            .getOperation()
                                            .split(ARConstants.ACTION_SPECIFICATIONS_SPLITTER)
                                    : null;

                            if (actions[0].equalsIgnoreCase(ARConstants.IF)
                                    || actions[0].equalsIgnoreCase(ARConstants.ELSEIF)
                                    || actions[0].equalsIgnoreCase(ARConstants.ELSE)
                                    || actions[0].equalsIgnoreCase(ARConstants.ENDIF)) {
                                currentCondition = ARConstants.ConditionStatus.valueOf(actions[0]);
                                if (previousCondition.equals(ARConstants.ConditionStatus.NONE)) {
                                    previousCondition = currentCondition;
                                    parentBlockCondition = parentId;
                                } else if (!previousCondition.equals(
                                        currentCondition)) { // To Reset the Progress to the Next Block
                                    previousCondition = currentCondition;
                                }

                                // Conditions When Pass to any of then
                                if (progressCondition.equals(ARConstants.ConditionStatus.IF_PASSED)
                                        || progressCondition.equals(ARConstants.ConditionStatus.ELSEIF_PASSED)) {
                                    int jumpPassed = performActions.checkActionToJump(
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
                                        currentCondition = ARConstants.ConditionStatus.NONE;
                                        progressCondition = ARConstants.ConditionStatus.NONE;
                                        continue instructionLoop;
                                    }
                                } else if (currentCondition.equals(ARConstants.ConditionStatus.ENDIF)) {
                                    currentCondition = ARConstants.ConditionStatus.NONE;
                                    previousCondition = ARConstants.ConditionStatus.NONE;
                                    progressCondition = ARConstants.ConditionStatus.NONE;
                                    parentBlockCondition = -1;
                                }
                                continue;
                            }

                            // Case for Inputs
                            String valueInsert = "No Data Found";
                            if (actions[0].equals(ARConstants.INSERT) && actions[1].equals(ARConstants.ENTER)) {
                                String reference = actions[2];
                                valueInsert = dataExcel.get(reference);
                            } else if (actions[0].equals(ARConstants.INSERT)) {
                                String reference = actions[1];
                                valueInsert = dataExcel.get(reference);
                            }

                            Pair<String, String> msgInstruction = null;
                            if (actions[0].equalsIgnoreCase(ARConstants.GOTO)) {
                                // <currentId:blockId:blockOrderNumber:bockName>
                                msgInstruction = performActions.getBlockDetailsById(blocksLoaded, currentInstruction);
                                if (msgInstruction == null) {
                                    msgInstruction = new Pair("GO TO Block \"Unknown\"", "Unknown");
                                    success = false;
                                    jumpGotoError = true;
                                    jumpGoto = true;
                                } else if (!mapLoops.containsKey(msgInstruction.getKey())) {
                                    jumpGoto = true;
                                    jumpGotoError = false;
                                    mapLoops.put(
                                            msgInstruction.getKey(),
                                            Integer.valueOf(msgInstruction.getValue())); // <id:orderId:blockName>
                                } else if (mapLoops.containsKey(msgInstruction.getKey())) {
                                    // Updates the msgInstruction
                                    jumpGoto = true;
                                    msgInstruction = new Pair<>(
                                            msgInstruction.getKey(),
                                            String.valueOf(mapLoops.get(msgInstruction.getKey())));
                                }

                            } else if (actions[0].equalsIgnoreCase(ARConstants.LOOP)) {
                                // <currentId:parentId:parentName>
                                msgInstruction = performActions.getInstructionDetailsById(
                                        blocksLoaded.get(currentBlock).getInstructionLoadDTOS(), currentInstruction);

                                if (msgInstruction == null) {
                                    msgInstruction = new Pair("Jump To Parent \"Unknown\"", "Unknown");
                                    success = false;
                                } else if (!mapLoops.containsKey(msgInstruction.getKey())) {
                                    jumpLoopError = false;
                                    String[] parts = msgInstruction.getValue().split(":"); // Split by ':'
                                    mapLoops.put(msgInstruction.getKey(), Integer.valueOf(parts[1])); // Loop Times
                                    mapRefresh.put(msgInstruction.getKey(), Integer.valueOf(parts[0])); // Wait Time
                                } else if (mapLoops.containsKey(msgInstruction.getKey())) {
                                    // Updates the msgInstruction
                                    msgInstruction = new Pair<>(
                                            msgInstruction.getKey(),
                                            String.valueOf(mapLoops.get(msgInstruction.getKey())));
                                }
                            } else if (actions[0].equalsIgnoreCase(ARConstants.REFRESH_LOOP)) {
                                msgInstruction = performActions.getInstructionDetailsById(
                                        blocksLoaded.get(currentBlock).getInstructionLoadDTOS(), currentInstruction);
                                if (msgInstruction == null) {
                                    msgInstruction = new Pair("Jump To Parent \"Unknown\"", "Unknown");
                                    success = false;
                                } else if (!mapLoops.containsKey(msgInstruction.getKey())) {
                                    jumpLoopError = false;
                                    String[] parts = msgInstruction.getValue().split(":"); // Split by ':'
                                    mapLoops.put(msgInstruction.getKey(), Integer.valueOf(parts[1])); // Loop Times
                                    mapRefresh.put(msgInstruction.getKey(), Integer.valueOf(parts[0])); // Wait Time
                                } else if (mapLoops.containsKey(msgInstruction.getKey())) {
                                    // Updates the msgInstruction
                                    // Refresh Loop  <5:5> <WAIT:LOOP>
                                    String updMsg = mapRefresh.get(msgInstruction.getKey()) + ":"
                                            + mapLoops.get(msgInstruction.getKey());
                                    msgInstruction = new Pair<>(msgInstruction.getKey(), updMsg);
                                }
                            } else if (actions[0].equalsIgnoreCase(ARConstants.SET_VALUE)
                                    || (actions[0].equalsIgnoreCase(ARConstants.GET_VALUE))) {
                                msgInstruction = new Pair(
                                        currentInstruction.getName(),
                                        (currentInstruction.getOperation() != null
                                                ? "(" + parentId + ")-" + operations[0] + ":" + operations[1]
                                                : (actions[0].equalsIgnoreCase(ARConstants.INSERT))
                                                        ? valueInsert
                                                        : ""));
                            } else {
                                msgInstruction = new Pair(
                                        "(" + currentInstruction.getId() + ")-" + currentInstruction.getName(),
                                        (currentInstruction.getOperation() != null
                                                ? currentInstruction.getOperation()
                                                : (actions[0].equalsIgnoreCase(ARConstants.INSERT))
                                                        ? valueInsert
                                                        : ""));
                            }

                            resultActions = performActions.actionResultMessage(blockName, actions, msgInstruction);

                            if (actions[0].equalsIgnoreCase(ARConstants.PAUSE)) {
                                pauseOperation = true;

                                respModal = performMessage.showCustomModalDialogDragWin11(
                                        "PAUSE BOT JOB",
                                        "PAUSED at Block Name",
                                        blockLoad.getName(),
                                        " Please click OK to continue!",
                                        null,
                                        false,
                                        "Continue",
                                        "Stop all",
                                        0);
                            }

                            if (actions[0].equalsIgnoreCase(ARConstants.LOOP)) {
                                parentFieldLoop =
                                        performActions.getInstructionParentField(currentInstruction, blockLoad);
                                if (parentField == null && parentFieldLoop == null) {
                                    parentFieldLoop = "Unknown parent";
                                    parentField = parentFieldLoop;
                                } else {
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

                                        continue;
                                    }

                                } else {
                                    jumpLoopError = true;
                                }

                            } else if (actions[0].equalsIgnoreCase(ARConstants.REFRESH_ONLY)) {
                                refreshOnly = true;
                            } else if (actions[0].equalsIgnoreCase(ARConstants.REFRESH_LOOP)) {
                                parentFieldLoop =
                                        performActions.getInstructionParentField(currentInstruction, blockLoad);
                                if (parentField == null && parentFieldLoop == null) {
                                    parentFieldLoop = "Unknown parent";
                                    parentField = parentFieldLoop;
                                } else {
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

                                        continue;
                                    }

                                } else {
                                    jumpLoopError = true;
                                }
                            } else if (actions[0].equalsIgnoreCase(ARConstants.GET_VALUE)
                                    || actions[0].equalsIgnoreCase(ARConstants.SET_VALUE)) {

                                execGetOrSet = true;

                                xPathOperation = performActions.getXPathInstruction(currentInstruction, blockLoad);
                                String actionsParent =
                                        performActions.getInstructionParentActions(currentInstruction, blockLoad);
                                parentActions = actionsParent != null
                                        ? actionsParent.split(ARConstants.ACTION_SPECIFICATIONS_SPLITTER)
                                        : null;

                                parentField = performActions.getInstructionParentField(currentInstruction, blockLoad);
                                variableField =
                                        performActions.getInstructionVariableField(currentInstruction, variablesLoaded);
                                if (variableField == null) {
                                    variableField = "Not Variable defined";
                                }

                            } else if (actions[0].equalsIgnoreCase(ARConstants.OUTPUT)) {
                                execOutPut = true;
                                fieldName = currentInstruction.getId() + "-" + currentInstruction.getName();
                            } else if (actions[0].equalsIgnoreCase(ARConstants.CHECK_VALUE)) {
                                execCheckValue = true;
                                parentField = performActions.getInstructionParentField(currentInstruction, blockLoad);
                                variableField =
                                        performActions.getInstructionVariableField(currentInstruction, variablesLoaded);
                                if (variableField == null) {
                                    variableField = "Not Variable defined";
                                }
                            } else if (actions[0].equalsIgnoreCase(ARConstants.EXTRACT_FIELD)) {
                                excelWriteOperation = true;
                                parentField = performActions.getInstructionParentField(currentInstruction, blockLoad);
                                variableField =
                                        performActions.getInstructionVariableField(currentInstruction, variablesLoaded);
                                if (variableField == null) {
                                    variableField = "Not Variable defined";
                                }
                            }

                            File logFileForSingleExcel = excelReader.createLogFile(excelPath);

                            try {
                                if (jumpGoto) {

                                    if (jumpGotoError) {
                                        success = false;
                                        failedMessage = "Failed: GO TO";
                                        resultActions = performActions.blockGotoFailed(resultActions);
                                    } else {
                                        if (!loopBlockActive.contains(msgInstruction.getKey())) {
                                            loopBlockActive.add(msgInstruction.getKey());
                                            loopBlockLimits.put(
                                                    msgInstruction.getKey(),
                                                    Integer.valueOf(msgInstruction.getValue()));
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

                                                success = true;

                                            } catch (Exception ex) {
                                                failedMessage = "Failed: GO TO";
                                                msgInstruction = updateMSGInstruction(msgInstruction, failedMessage);

                                                success = false;

                                                resultActions = performActions.blockGotoFailed(resultActions);
                                            }

                                            Pair<String, String> currentPair = new Pair(
                                                    msgInstruction.getKey(),
                                                    String.valueOf(mapLoops.get(msgInstruction.getKey())));

                                            // Excel Report and Log
                                            performActions.logAndReport(
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
                                                    finalLogMessage(failedMessage, resultActions));

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
                                    }

                                } else if (jumpLoop) {

                                    if (mapRefresh.containsKey(parentFieldLoop)) {
                                        int timerLoop = mapRefresh.get(parentFieldLoop);
                                        performActions.onHoldInSeconds(timerLoop);
                                    }

                                    if (mapLoops.containsKey(parentFieldLoop)) {

                                        int repeat = mapLoops.get(parentFieldLoop) - 1;
                                        String[] parts = parentFieldLoop.split(":");
                                        if (repeat > 0) {
                                            mapLoops.put(parentFieldLoop, repeat);

                                            ARLogger.getInstance(Engine.class)
                                                    .info(String.format(
                                                            "Loop to Parent :\"%s\" - %d Times",
                                                            parts[0] + "-(" + parts[1] + ") " + parts[2],
                                                            mapLoops.get(parentFieldLoop)));

                                            if (refreshLoop) {

                                                String extraLog = performActions.actionResultMessage(
                                                        blockName,
                                                        new String[] {ARConstants.REFRESH_HOLD},
                                                        msgInstruction);

                                                performActions.performOtherActions(
                                                        byPassNotFound,
                                                        currentInstruction,
                                                        new String[] {ARConstants.REFRESH_HOLD});

                                                // Excel Report and Log
                                                performActions.logAndReport(
                                                        currentCondition,
                                                        true,
                                                        true,
                                                        currentInstructionStartTime,
                                                        blockReportName,
                                                        success,
                                                        new String[] {ARConstants.REFRESH_HOLD},
                                                        msgInstruction,
                                                        dataExcel,
                                                        writerReport,
                                                        mainMsg,
                                                        extraLog);

                                                // Refresh For REFRESH_LOOP
                                                extraLog = performActions.actionResultMessage(
                                                        blockName,
                                                        new String[] {ARConstants.REFRESH_ONLY},
                                                        msgInstruction);

                                                performActions.performOtherActions(
                                                        byPassNotFound,
                                                        currentInstruction,
                                                        new String[] {ARConstants.REFRESH_ONLY});

                                                // Excel Report and Log
                                                performActions.logAndReport(
                                                        currentCondition,
                                                        true,
                                                        true,
                                                        currentInstructionStartTime,
                                                        blockReportName,
                                                        success,
                                                        new String[] {ARConstants.REFRESH_ONLY},
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
                                            performActions.logAndReport(
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
                                                    finalLogMessage(failedMessage, resultActions));

                                        } else {
                                            mapLoops.put(parentFieldLoop, repeat);
                                        }

                                        jumpLoop = false;
                                        refreshLoop = false;

                                        if (repeat > 0) {
                                            continue instructionLoop;
                                        } else {
                                            ARLogger.getInstance(Engine.class)
                                                    .info(String.format(
                                                            "IGNORING Loop to Parent :\"%s\" - %d Times",
                                                            parts[0] + "-(" + parts[1] + ") " + parts[2],
                                                            mapLoops.get(parentFieldLoop)));
                                            continue;
                                        }

                                    } else {
                                        resultActions = performActions.parentValueIsNotDefined(
                                                currentInstruction.getName(),
                                                "(" + parentId + ")-" + parentField,
                                                resultActions);

                                        success = false;
                                    }

                                } else if (refreshOnly) {

                                    performActions.performOtherActions(byPassNotFound, currentInstruction, actions);

                                    resultActions = "Refresh Current Web Page ->  inside Block :\""
                                            + blockLoad.getName() + "\"";

                                    refreshOnly = false;

                                } else if (actions[0].equals(ARConstants.HOLD)
                                        || actions[0].equals(ARConstants.QUIT)
                                        || actions[0].equals(ARConstants.SCREEN)
                                        || actions[0].equals(ARConstants.REFRESH_ONLY)) {

                                    performActions.performOtherActions(byPassNotFound, currentInstruction, actions);

                                    if (actions[0].equals(ARConstants.QUIT)) {
                                        stopAll = true;
                                        success = true;
                                    }

                                } else if (!jumpGotoError
                                        && !jumpLoopError
                                        && !execGetOrSet
                                        && !execCheckValue
                                        && !excelWriteOperation
                                        && !pauseOperation) {

                                    webElementWork = true;

                                    // Extract dataFieldName and dataFieldValue using a separate method
                                    Pair<String, String> fieldData = performActions.extractFieldData(
                                            dataExcel,
                                            actions,
                                            currentInstruction.getDefaultValue(),
                                            currentInstruction.getCodified());

                                    WebElement webElementFound = null;
                                    boolean forceCoordinates = currentInstruction.getForceCoordinates() != null
                                            && currentInstruction.getForceCoordinates();
                                    try {
                                        webElementFound = performActions.searchElement(
                                                currentInstruction, botJobId, forceCoordinates);
                                    } catch (Exception ex) {
                                        success = false;
                                    }

                                    if (webElementFound == null && forceCoordinates) {

                                        Boolean pressEnterAfter = false;
                                        if (actions[0].equals(ARConstants.INSERT)
                                                && actions[1].equals(ARConstants.ENTER)) {
                                            pressEnterAfter = true;
                                        }
                                        if (actions[0].equalsIgnoreCase(ARConstants.VISUALIZE)
                                                || actions[0].equalsIgnoreCase(ARConstants.CLICK)
                                                || actions[0].equalsIgnoreCase(ARConstants.INSERT)) {
                                            success = performActions.executeActionsAtCoordinates(
                                                    mapSavedLocators.get("coordinates"),
                                                    fieldData,
                                                    actions[0],
                                                    pressEnterAfter);
                                        }
                                    }

                                    byPassNotFound = byPassFlagLoop
                                            || !currentCondition.equals(ARConstants.ConditionStatus.NONE);

                                    if (webElementFound != null && success) {

                                        success = performActions.performWebActions(
                                                byPassNotFound,
                                                mapSavedLocators.get("coordinates"),
                                                fieldData,
                                                currentInstruction,
                                                mapOperators,
                                                webElementFound,
                                                actions);

                                        if (execOutPut) {
                                            if (mapOperators.containsKey(fieldName)) {
                                                msgInstruction = new Pair(fieldName, mapOperators.get(fieldName));
                                            } else {
                                                msgInstruction = new Pair(fieldName, "TEXT OUTPUT NOT FOUND");
                                            }
                                        }
                                    }
                                    // Special Cases for Select Responses
                                    // It could be Improved the case
                                    if (resultActions.contains("Error:")
                                            || (webElementFound == null && !forceCoordinates)) {
                                        failedMessage = "Failed execution Web Element";
                                        msgInstruction = updateMSGInstruction(msgInstruction, failedMessage);
                                        success = false;
                                    } else if (resultActions != null && success) {
                                        currentInstruction.setExecuted(true);
                                    }

                                } else if (execGetOrSet) {
                                    // GET && SET Special Operators

                                    if (parentField != null && parentId != 0) {
                                        parentField = parentId + "-" + parentField;
                                    }
                                    // Mandatory for GET_VALUE
                                    if (xPathOperation == null && actions[0].equalsIgnoreCase(ARConstants.GET_VALUE)) {
                                        failedMessage = "Parent Id in Wrong Block";
                                        msgInstruction = updateMSGInstruction(msgInstruction, failedMessage);
                                        resultActions = performActions.parentIdWrongBlock(
                                                currentInstruction, blockLoad, resultActions, currentCondition);
                                        success = false;
                                    } else if (parentField == null) {
                                        failedMessage = "Parent Id in Wrong Block";
                                        msgInstruction = updateMSGInstruction(msgInstruction, failedMessage);
                                        resultActions = performActions.parentIdWrongBlock(
                                                currentInstruction, blockLoad, resultActions, currentCondition);
                                        success = false;
                                    } else {

                                        resultActions = performActions.performOperatorActions(
                                                byPassNotFound,
                                                currentInstruction,
                                                xPathOperation,
                                                parentActions,
                                                actions[0],
                                                operations,
                                                parentField,
                                                variableField,
                                                mapOperators);

                                        if (resultActions.contains("Error:")) {
                                            failedMessage = "Failed: Operation (GetValue / SetValue)";
                                            msgInstruction = updateMSGInstruction(msgInstruction, failedMessage);
                                            success = false;
                                        } else {
                                            success = true;
                                        }
                                    }

                                } else if (execCheckValue) {
                                    // Check Validation Operator

                                    if (!mapOperators.containsKey(variableField)) {
                                        failedMessage = "Get Value Is Not Defined";
                                        msgInstruction = updateMSGInstruction(msgInstruction, failedMessage);
                                        resultActions = performActions.getValueIsNotDefined(
                                                actions[0],
                                                currentInstruction,
                                                resultActions,
                                                ARConstants.ConditionStatus
                                                        .NONE, // NOT  currentCondition to Force Message,
                                                parentField,
                                                variableField);

                                        success = false;
                                    } else {
                                        //                                    fieldName = parentField;

                                        resultActions = "Check Value for " + String.join(" ", operations);
                                        boolean isOperationValid = false;
                                        String invalidValues = null;

                                        if (operations[1].equalsIgnoreCase("=")) {
                                            isOperationValid = mapOperators
                                                    .get(variableField)
                                                    .trim()
                                                    .equalsIgnoreCase(operations[2].trim());

                                        } else if (operations[1].equalsIgnoreCase(">")) {
                                            int resp = handleGreaterThan(
                                                    mapOperators
                                                            .get(variableField)
                                                            .trim(),
                                                    operations[2].trim());
                                            if (resp == 1) {
                                                isOperationValid = true;
                                            } else if (resp == 0) {
                                                isOperationValid = false;
                                            } else {
                                                isOperationValid = false;
                                                invalidValues = "Invalid Numbers";
                                            }
                                        } else if (operations[1].equalsIgnoreCase("!=")) {
                                            isOperationValid = !mapOperators
                                                    .get(variableField)
                                                    .trim()
                                                    .equalsIgnoreCase(operations[2].trim());
                                        } else if (operations[1].equalsIgnoreCase("<")) {
                                            int resp = handleLessThan(
                                                    mapOperators
                                                            .get(variableField)
                                                            .trim(),
                                                    operations[2].trim());
                                            if (resp == 1) {
                                                isOperationValid = true;
                                            } else if (resp == 0) {
                                                isOperationValid = false;
                                            } else {
                                                isOperationValid = false;
                                                invalidValues = "Invalid Numbers";
                                            }
                                        }

                                        if (isOperationValid) {

                                            currentInstruction.setExecuted(true);
                                            success = true;
                                        } else {
                                            failedMessage = "Failed: Check Validation";
                                            msgInstruction = updateMSGInstruction(msgInstruction, failedMessage);
                                            resultActions = performActions.checkValidationFailed(
                                                    invalidValues,
                                                    parentField,
                                                    mapOperators.get(variableField),
                                                    resultActions,
                                                    operations,
                                                    currentCondition,
                                                    byPassNotFound);

                                            success = false;
                                        }
                                    }

                                } else if (excelWriteOperation) {
                                    // Excel Write Operator

                                    if (parentField == null) {
                                        failedMessage = "Parent Id in Wrong Block";
                                        msgInstruction = updateMSGInstruction(msgInstruction, failedMessage);
                                        resultActions = performActions.parentIdWrongBlock(
                                                currentInstruction, blockLoad, resultActions, currentCondition);

                                        success = false;

                                    } else if (!mapOperators.containsKey(variableField)) {
                                        failedMessage = "Get Value Is Not Defined";
                                        msgInstruction = updateMSGInstruction(msgInstruction, failedMessage);
                                        resultActions = performActions.getValueIsNotDefined(
                                                actions[0],
                                                currentInstruction,
                                                resultActions,
                                                ARConstants.ConditionStatus
                                                        .NONE, // NOT  currentCondition to Force Message,
                                                parentField,
                                                variableField);

                                        success = false;
                                    } else {

                                        if (excelExportOnceCreation) {
                                            //
                                            // writerExport.insertReportHead();
                                            excelExportOnceCreation = false;
                                        }

                                        if (!Strings.isNullOrEmpty(excelFieldName)) {
                                            writerExport = new ExcelWriter(
                                                            excelFieldName, performActions.getCurrentDriver(), true)
                                                    .withPurpose("export");
                                        }

                                        if (writerExport != null) {

                                            resultActions = "insertValueFieldNameInExcel -> " + variableField + "-"
                                                    + mapOperators.get(variableField);
                                        } else {
                                            resultActions = "NO Export Excel File defined -> " + variableField + "-"
                                                    + mapOperators.get(variableField);
                                        }

                                        if (mapExport.size() == 0) {
                                            //
                                            // writerExport.insertBlockSeparation(blockLoad.getName());
                                            //                                            exportIndex *= 2;
                                        }

                                        // Insert the updated mapExport into the Excel after each instruction
                                        if (writerExport != null) {
                                            mapExport.put("KEY", "EXTERNAL");
                                            mapExport.put(
                                                    parentField.trim(),
                                                    mapOperators
                                                            .get(variableField)
                                                            .trim());
                                            if (excelFieldName != null
                                                    && excelFieldName
                                                            .toLowerCase()
                                                            .endsWith(".csv")) {
                                                writerExport.writeMapToCSV(mapExport, excelFieldName);
                                            } else {
                                                writerExport.insertFieldNameAndValueLastColumn(
                                                        mapExport, exportIndex - 1);
                                            }
                                        }
                                        performActions.onHoldForSeconds(null);

                                        if (resultActions != null) {
                                            currentInstruction.setExecuted(true);
                                            success = true;
                                        } else {
                                            failedMessage = "Failed: Generate File -> Excel/CSV";
                                            msgInstruction = updateMSGInstruction(msgInstruction, failedMessage);

                                            success = false;
                                        }
                                    }
                                }

                            } catch (Throwable t) {
                                success = false;

                                String[] lines = t.getMessage().split("\n");
                                String msg1 = "";
                                String msg2 = "";

                                for (String line : lines) {
                                    if (Strings.isNullOrEmpty(msg1)) {
                                        msg1 = line;
                                    } else if (Strings.isNullOrEmpty(msg2)) {
                                        msg2 = line;
                                    }
                                }

                                String msg3 = resultActions;

                                if (Strings.isNullOrEmpty(failedMessage)) {
                                    failedMessage = "Failed: General Execution";
                                    msgInstruction = updateMSGInstruction(msgInstruction, failedMessage);
                                }

                                performMessage.errorMessage(resultActions, msg1, msg2, msg3, null, 260);
                                //                            throw new RuntimeException(t);
                            }

                            printLog(
                                    generateTimestamp(),
                                    logFileForSingleExcel,
                                    finalLogMessage(failedMessage, resultActions),
                                    success);

                            // Here mark the Status of a progress Condition Fail or Success at the end of each Kind
                            // of Execution
                            if (!jumpGotoError
                                    && !jumpLoopError
                                    && !currentCondition.equals(ARConstants.ConditionStatus.NONE)) {
                                progressCondition = performActions.updateProgressSuccess(success, currentCondition);
                                //                                continue instructionLoop;
                            } else {
                                progressCondition = ARConstants.ConditionStatus.NONE;
                            }

                            // Excel Report and Log
                            performActions.logAndReport(
                                    !byPassFlagLoop ? progressCondition : ARConstants.ConditionStatus.BY_PASS,
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
                                    finalLogMessage(failedMessage, resultActions));

                            if (pauseOperation && respModal.equals(ARConstants.DialogModal.STOP)) {

                                String nameInstruc =
                                        "(" + currentInstruction.getId() + ") " + currentInstruction.getName();

                                resultActions = String.format("STOP ALL PROCESSES: \"%s\"", nameInstruc);

                                Pair<String, String> msgBlock = new Pair(resultActions, ARConstants.PAUSE);

                                // Excel Report and Log
                                performActions.logAndReport(
                                        currentCondition,
                                        true,
                                        true,
                                        blockStartTime,
                                        blockReportName,
                                        success,
                                        new String[] {ARConstants.PAUSE},
                                        msgBlock,
                                        dataExcel,
                                        writerReport,
                                        "PAUSE -> STOP",
                                        String.format("STOP ALL CALLED AT: \"%s\" : ", nameInstruc));

                                respModal = ARConstants.DialogModal.NONE;
                                stopAll = true;
                                break;
                            }

                            // It decides Here if ByPass as per Loop or Per IF-ELSEIF-ELSE-ENDIF blocks
                            if (!success
                                    && !byPassFlagLoop
                                    && currentCondition.equals(ARConstants.ConditionStatus.NONE)) {
                                stopAll = true;
                                break;
                            }

                            // It decides Here if ByPass as per Loop or Per IF-ELSEIF-ELSE-ENDIF blocks
                            if (jumpGotoError || jumpLoopError) {
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
                            if (progressCondition.equals(ARConstants.ConditionStatus.IF_PASSED)
                                    || progressCondition.equals(ARConstants.ConditionStatus.ELSEIF_PASSED)) {
                                int jumpPassed = performActions.checkActionToJump(
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
                                    currentCondition = ARConstants.ConditionStatus.NONE;
                                    progressCondition = ARConstants.ConditionStatus.NONE;
                                    continue instructionLoop;
                                }
                            }

                            // Conditions When Fails to any of then and Look for the next Correct Block
                            if (progressCondition.equals(ARConstants.ConditionStatus.IF_FAILED)
                                    || progressCondition.equals(ARConstants.ConditionStatus.ELSEIF_FAILED)) {

                                // Goes to the next ELSEIF IF EXIST (ELSEIF index + 1);
                                int index = performActions.searchMapConditional(
                                        mapConditional,
                                        parentBlockCondition,
                                        ARConstants.ConditionStatus.ELSEIF,
                                        currentIndex,
                                        false);

                                // Goes to the next ELSE IF ELSEIF  DOES NOT EXIST  (ELSE index + 1);
                                if (index < 0) {
                                    index = performActions.searchMapConditional(
                                            mapConditional,
                                            parentBlockCondition,
                                            ARConstants.ConditionStatus.ELSE,
                                            currentIndex,
                                            true);
                                }
                                if (index < 0) {
                                    stopAll = true;
                                    continue blockLoop;
                                }
                                currentIndex = index;
                                currentCondition = ARConstants.ConditionStatus.NONE;
                                progressCondition = ARConstants.ConditionStatus.NONE;
                                continue instructionLoop;

                            } else if (progressCondition.equals(ARConstants.ConditionStatus.ELSE_FAILED)) {
                                // Goes to the ENDIF (ENDIF index + 1);
                                int index = performActions.searchMapConditional(
                                        mapConditional,
                                        parentBlockCondition,
                                        ARConstants.ConditionStatus.ENDIF,
                                        currentIndex,
                                        true);

                                if (index < 0) {
                                    stopAll = true;
                                    continue blockLoop;
                                }
                                currentIndex = index;
                                currentCondition = ARConstants.ConditionStatus.NONE;
                                progressCondition = ARConstants.ConditionStatus.NONE;
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

                    for (InstructionLoadDTO currentInstruction :
                            blocksLoaded.get(j).getInstructionLoadDTOS()) {
                        if (currentInstruction.getDefaultValue() == null) {
                            String[] arr = UtilsMethods.splitIfContains(
                                    currentInstruction.getActions(), ARConstants.ACTION_SPECIFICATIONS_SPLITTER);
                            if (arr.length > 1) {
                                String dataFieldName = arr[1].split(ARConstants.PATH_FIELD_SUBSTITUTION)[0];
                                PerformActions.insertRandomName(dataFieldName);
                            }
                        }
                    }
                }
                for (int j = 0; success && j < blocksLoaded.size(); j++) {

                    String blockName = blocksLoaded.get(j).getName();
                    int blockOrder = blocksLoaded.get(j).getBlockOrderNumber();
                    String blockReportName = "#" + blockOrder + " " + blockName;

                    for (InstructionLoadDTO currentInstruction :
                            blocksLoaded.get(j).getInstructionLoadDTOS()) {

                        long currentInstructionStartTime = System.nanoTime();
                        File logFileForSingleExcel = excelReader.createLogFile(excelPath);

                        String[] actions =
                                currentInstruction.getActions().split(ARConstants.ACTIONS_AND_PATHS_SPLITTER);

                        // Case for Inputs
                        String valueInsert = "No Data Found";
                        if (actions[0].equals(ARConstants.INSERT) && actions[1].equals(ARConstants.ENTER)) {
                            String reference = actions[2];
                            valueInsert = dataExcel.get(reference);
                        } else if (actions[0].equals(ARConstants.INSERT)) {
                            String reference = actions[1];
                            valueInsert = dataExcel.get(reference);
                        }

                        Pair<String, String> msgInstruction = new Pair(
                                currentInstruction.getName(),
                                (currentInstruction.getOperation() != null
                                        ? currentInstruction.getOperation()
                                        : (actions[0].equalsIgnoreCase(ARConstants.INSERT)) ? valueInsert : ""));

                        resultActions = performActions.actionResultMessage(blockName, actions, msgInstruction);

                        try {

                            if (actions[0].equals(ARConstants.HOLD)
                                    || actions[0].equals(ARConstants.QUIT)
                                    || actions[0].equals(ARConstants.SCREEN)
                                    || actions[0].equals(ARConstants.REFRESH_ONLY)) {
                                performActions.performOtherActions(byPassNotFound, currentInstruction, actions);

                                if (actions[0].equals(ARConstants.QUIT)) {
                                    stopAll = true;
                                    success = true;
                                }

                                // Excel Report and Log
                                performActions.logAndReport(
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
                                        finalLogMessage(failedMessage, resultActions));

                                continue;
                            }

                            WebElement webElementFound = null;
                            boolean forceCoordinates = currentInstruction.getForceCoordinates() != null
                                    && currentInstruction.getForceCoordinates();

                            try {
                                webElementFound =
                                        performActions.searchElement(currentInstruction, botJobId, forceCoordinates);
                            } catch (Exception ex) {
                            }

                            success = performActions.performWebActions(
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
                                failedMessage = "Failed: Execution";
                                msgInstruction = updateMSGInstruction(msgInstruction, failedMessage);

                                resultActions = currentInstruction.getName();
                                success = false;
                            }

                            // Excel Report and Log
                            performActions.logAndReport(
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
                                    finalLogMessage(failedMessage, resultActions));

                        } catch (Throwable t) {
                            success = false;
                            currentInstruction.setExecuted(false);

                            failedMessage = "Failed: ";
                            msgInstruction = updateMSGInstruction(msgInstruction, failedMessage);

                            // Excel Report and Log
                            performActions.logAndReport(
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
                                    finalLogMessage(failedMessage, resultActions));

                            //                        throw new RuntimeException(t);
                        }
                        printLog(
                                generateTimestamp(),
                                logFileForSingleExcel,
                                finalLogMessage(failedMessage, resultActions),
                                success);
                    }
                }
            }
            //            launchBotJobButton.setDisable(false);

            totalExecutionTime = performActions.getTotalExecutionTime();

            if (totalExecutionTime == 0) {
                writerReport.insertTotalExecutionTimes(botJobStartTime, botJobStartTime);
            } else {
                writerReport.insertTotalExecutionTimes(botJobStartTime, System.nanoTime());
            }

            // PRINT END BASE LOG//
            if (success) {
                baseLogString = botLoadJobs.get(0).getName()
                        + ARConstants.FIELDS_SEPARATOR
                        + labelsValue.getProperty(Labels.END)
                        + ARConstants.FIELDS_SEPARATOR
                        + labelsValue.getProperty(Labels.OK);

                System.out.println(String.format("Success: %s Last Execution: %s", botJobName, resultActions));

                respModal = performMessage.showCustomModalDialogDragWin11(
                        "Bot-Job Finished - successfully",
                        botJobName,
                        "Last Execution:",
                        resultActions,
                        null,
                        false,
                        "OK",
                        "Close Browser",
                        300);

            } else {
                baseLogString = botLoadJobs.get(0).getName()
                        + ARConstants.FIELDS_SEPARATOR
                        + labelsValue.getProperty(Labels.END)
                        + ARConstants.FIELDS_SEPARATOR
                        + labelsValue.getProperty(Labels.KO)
                        + resultActions;

                if (webElementWork) {
                    respModal = performMessage.showCustomModalDialogDragWin11(
                            "Failed finding element (5 attempts).",
                            "Use \"Force Coordinates\" in some cases.",
                            !Strings.isNullOrEmpty(failedMessage) ? failedMessage : "Failed:",
                            "Last Execution:",
                            resultActions,
                            true,
                            "OK",
                            "Close Browser",
                            350);
                } else {

                    respModal = performMessage.showCustomModalDialogDragWin11(
                            "Process Execution Terminated",
                            !Strings.isNullOrEmpty(failedMessage) ? failedMessage : "Failed:",
                            "Last Execution:",
                            resultActions,
                            null,
                            true,
                            "OK",
                            "Close Browser",
                            350);
                }

                System.out.println(String.format("Failed: %s Last Execution: %s", botJobName, resultActions));
                System.out.println("Failed to locate the element after 10 attempts");

                //                performMessage.errorMessage(
                //                        "Failed to locate the element after 10 attempts.",
                //                        "Try rescanning the element,",
                //                        "or change the action to \"Force Coordinates\".",
                //                        "Last Execution:",
                //                        resultActions,
                //                        260);

            }
            printBaseLog(baseLogFile, generateTimestamp(), baseLogString);
            printBaseLog(baseLogFile, generateTimestamp(), baseLogString);

            if (resultActions.equalsIgnoreCase("Close Browser") || respModal.equals(ARConstants.DialogModal.STOP)) {
                currentARWebDriver.getCurrentDriver().quit();
            }

            return true;
        } catch (Throwable t) {
            //            ABRLogger.getInstance(Engine.class).severe("Error Executing JOB \n" + t.getMessage());

            if (t.getMessage().contains("Current browser version")) {
                String[] lines = t.getMessage().split("\n");
                String msg1 = "";
                String msg2 = "";

                for (String line : lines) {
                    int indexMessage = line.indexOf("Message: ");
                    if (indexMessage != -1) {
                        msg1 = line.substring(indexMessage + "Message: ".length());
                    }

                    int indexBrowserVersion = line.indexOf("Current browser version");
                    if (indexBrowserVersion != -1) {
                        msg2 = line.substring(indexBrowserVersion);
                    }
                }

                ARLogger.getInstance(Engine.class).severe("Error Open URL: \n" + msg1 + "\n" + msg2);

                performMessage.errorMessage("Error WebDriver Version", msg1, msg2, null, null, 260);
            } else {
                String errorMessage = t.getMessage();
                String msg1 = "";
                String msg2 = "";
                String searchWord = "because";
                int index = errorMessage.indexOf(searchWord);

                if (index != -1) {
                    msg1 = errorMessage.substring(0, index).trim();
                    msg2 = errorMessage.substring(index).trim(); // Includes "because"
                } else {
                    // If "because" is not found, put the whole message in msg1 and leave msg2 empty
                    msg1 = errorMessage.trim();
                }

                ARLogger.getInstance(Engine.class)
                        .severe("Error Open URL: \n" + msg1 + "\n--- " + searchWord + " ---\n" + msg2);

                performMessage.errorMessage("Error Details:", msg1, searchWord + " " + msg2, null, null, 0);
            }

            return false;
        }
    }

    private static String generateTimestamp() {
        Date date = new Date();
        dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        return dateFormatter.format(date);
    }

    private static void printLog(String timeStamp, File logFile, String resultActions, boolean result) {
        String resultMsg = result ? ARConstants.SUCCESS : ARConstants.FAIL;
        String log = String.join(ARConstants.FIELDS_SEPARATOR, timeStamp, resultMsg, resultActions);

        try {
            FileWriter fileWriter = new FileWriter(logFile, true);
            fileWriter.write(log + System.lineSeparator());
            fileWriter.close();
        } catch (Exception e) {
            ARLogger.getInstance(Engine.class).severe("printLog Error: " + e.getMessage());
        }
    }

    private static void printBaseLog(File logFile, String timeStamp, String msg) {
        String resultMsg;
        String log = String.join(ARConstants.FIELDS_SEPARATOR, timeStamp, msg);

        try {
            FileWriter fileWriter = new FileWriter(logFile, true);
            fileWriter.write(log + System.lineSeparator());
            fileWriter.close();
        } catch (Exception e) {
            ARLogger.getInstance(Engine.class).severe("printBaseLog Error: " + e.getMessage());
        }
    }

    private static void printLogExcel(String timeStamp, File logExcel, Map<String, String> data, boolean result) {
        String resultMsg = result ? ARConstants.SUCCESS : ARConstants.FAIL;

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
            ARLogger.getInstance(Engine.class).severe("printLogExcel Error: " + e.getMessage());
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

    public static List<InstructionLoadDTO> getUnexecutedInstructions(
            List<InstructionLoadDTO> instructionsExecuted, List<InstructionLoadDTO> otherList) {
        // Create a set of instructionOrderNumbers from instructionsExecuted
        Set<Integer> executedInstructionOrderNumbers = instructionsExecuted.stream()
                .map(InstructionLoadDTO::getInstructionOrderNumber)
                .collect(Collectors.toSet());

        // Filter the otherList to get instructions where executed is false and not in executedInstructionOrderNumbers
        return otherList.stream()
                //                .filter(instruction -> instruction.getExecuted() != null &&
                // !instruction.getExecuted())
                .filter(instruction ->
                        !executedInstructionOrderNumbers.contains(instruction.getInstructionOrderNumber()))
                .collect(Collectors.toList());
    }

    public static List<String> checkProperties(Properties properties) {
        String[] requiredProperties = {
            "data_base",
            "path_excel",
            "path_log",
            "path_java",
            "path_java_fx",
            "path_db",
            "path_report",
            "path_priority",
            "path_engine",
            "path_web_driver",
            "log_level",
            "browser"
        };

        List<String> missingPropertiesList = new ArrayList<>();

        for (String propertyName : requiredProperties) {
            if (!properties.containsKey(propertyName)) {
                missingPropertiesList.add(propertyName);
            }
        }

        return missingPropertiesList;
    }

    private static int handleGreaterThan(String value1, String value2) {
        try {
            double num1 = Double.parseDouble(value1);
            double num2 = Double.parseDouble(value2);
            return num1 > num2 ? 1 : 0;
        } catch (NumberFormatException e) {
            // Handle non-numeric values (e.g., log an error, return false)
            return -1; // Or throw an exception
        }
    }

    private static int handleLessThan(String value1, String value2) {
        try {
            double num1 = Double.parseDouble(value1);
            double num2 = Double.parseDouble(value2);
            return num1 < num2 ? 1 : 0;
        } catch (NumberFormatException e) {
            // Handle non-numeric values
            return -1; // Or throw an exception
        }
    }

    private static String finalLogMessage(String failedMessage, String resultActions) {
        if (!Strings.isNullOrEmpty(failedMessage)) {
            return failedMessage + resultActions;
        }
        return resultActions;
    }

    private static Pair<String, String> updateMSGInstruction(
            Pair<String, String> msgInstruction, String failedMessage) {
        String currentKey = msgInstruction.getKey();
        String updatedKey = failedMessage + " - " + currentKey;
        return new Pair<>(updatedKey, msgInstruction.getValue());
    }
}
