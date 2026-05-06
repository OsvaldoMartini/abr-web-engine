package com.allinweb.ch.runner;

import com.allinweb.ch.driver.ARWebDriver;
import com.allinweb.ch.facade.*;
import com.allinweb.ch.model.*;
import com.allinweb.ch.readersAndWriters.ExcelReader;
import com.allinweb.ch.readersAndWriters.ExcelWriter;
import com.allinweb.ch.socket.WebSocketSessionManager;
import com.allinweb.ch.util.*;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import io.opentelemetry.api.internal.StringUtils;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public class EngineRunner {

    private static final Logger logLaunch = LoggerFactory.getLogger("com.allinweb.launch");
    private static final Logger logOperations = LoggerFactory.getLogger("com.allinweb.operations");

    private String excelPath;
    private BotJobLoadDTO currentBotJob;
    private String currentBotJobName;
    private int currentBlockOrder;
    private int executeSpecificBlock;
    private Integer lastInstructionIdPushed = null;
    private boolean firstPageLoadDone = false;
    private boolean isMobileApp = false;
    private SplitDTO splitDTO;
    ExtractedData extractedData = null;
    private List<BlockLoadDTO> blocksLoaded;
    private List<InstructionLoad> excelDataGoto = new ArrayList<>();

    private SimpleDateFormat dateFormatter;

    private static final ARPropertyManager arPropertyManager = ARPropertyManager.getInstance();
    private static final WebSocketSessionManager webSocketSessionManager = WebSocketSessionManager.getInstance();
    private static final ARPriorities arPriorities = ARPriorities.getInstance();
    private static final PerformMessage performMessage = PerformMessage.getInstance();
    private static final PerformLists performLists = PerformLists.getInstance();
    private static final PerformDBEngine performDBEngine = PerformDBEngine.getInstance();
    private static final PerformActions performActions = PerformActions.getInstance();
    private static final ARWebDriver currentARWebDriver = ARWebDriver.getInstance();
    private static final PerformListElements performListElements = PerformListElements.getInstance();
    private static final PerformActionExecutorLoad performActionExecutorLoad = PerformActionExecutorLoad.getInstance();
    private static final ActionExecutorClient actionExecutorClient = ActionExecutorClient.getInstance();
    WebDriverWait waitXPath = null;

    static final String EXECUTE_JOB = "execute/j";

    private Map<String, String> mapOperators = new HashMap<>();
    private Map<String, String> mapExportRows;
    private Set<String> headersExport = new LinkedHashSet<>();
    private List<String> currentColumnsCSV = new ArrayList<>(); // set once
    private Map<String, CsvTable> csvTables = new LinkedHashMap<>();
    private final String END_OF_FILE_MARKER = "END OF FILE";
    static String excelFieldName;
    static String delimiterCSV;

    private List<VariableLoadDTO> variablesLoaded;

    private final Gson gson = new Gson();

    private String sessionRowStatus;
    private String jsonStatus;
    private RowStatus rowStatus = new RowStatus();

    public final AtomicBoolean isJobRunning = new AtomicBoolean(false);
    protected static AtomicBoolean interceptBotJob = new AtomicBoolean(false);

    public AtomicBoolean interceptBotJobProperty() {
        return interceptBotJob;
    }

    public boolean isInterceptBotJob() {
        return interceptBotJob.get();
    }

    public void setInterceptBotJob(boolean value) {
        interceptBotJob.set(value);
    }

    private Set<String> windowHandles;

    private ExecutorService executorServicePreLaunch;

    private int portSocketInitial = 54525;
    private boolean searchHiddenFields;
    private String[] defaultSearch;

    public void run(String[] args) {
        dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        String dataBaseType = arPropertyManager.getProperty(ARPropertyEnum.DATABASE_TYPE);
        performDBEngine.initialize(dataBaseType);

        try {
            performDBEngine.changeDbConnection();
        } catch (Exception error) {
            log.error("Error Database Connections: " + error.getMessage());
            System.exit(0);
        }

        try {
            startParametersInterpreter(args);
        } catch (Exception error) {
            log.error("Main class Start Error: " + error.getMessage());
            if (error.getMessage().contains("WebDriver")) {
                String browser = arPropertyManager.getProperty(ARPropertyEnum.BROWSER);
                String webDriverPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_WEBDRIVER);
                int lastSlashIndex = webDriverPath.lastIndexOf('\\');
                String directoryPath = webDriverPath.substring(0, lastSlashIndex + 1); // includes the last backslash
                String fileName = webDriverPath.substring(lastSlashIndex + 1);

                performMessage.errorMessage(
                        "WebDriver Version Incompatibility",
                        "<span style='color: #D32F2F; font-weight: bold; font-size: 1.1em;'>WebDriver version might be incompatible.</span>",
                        "<span style='font-weight: bold;'>Please verify the following:</span>",
                        "<ul>"
                                + "   <li>The installed browser version: <span style='color: #008b8b ; font-weight: bold;'>"
                                + browser + "</span></li>"
                                + "   <li>The WebDriver path:<br><span style='color: #008b8b ; font-weight: bold;'>"
                                + directoryPath + "</span></li>"
                                + "<li>The WebDriver file:<br><span style='color: #008b8b ; font-weight: bold;'>"
                                + fileName + "</span></li>"
                                + "   <li>Ensure the WebDriver version is the correct one for your browser version.</li>"
                                + "</ul>",
                        "<span style='font-style: italic;'>Refer to your browser's documentation or the WebDriver's release notes for compatibility information.</span>",
                        0);
            }

            System.exit(0);
        }
    }

    private void startParametersInterpreter(String[] args) throws Exception {
        if (args == null || args.length < 4) {
            throw new IllegalArgumentException("Incorrect number of parameters for job. "
                    + "Expected at least 3 parameters after the first argument.");
        }

        String[] idsAndPaths = Arrays.copyOfRange(args, 1, args.length);
        boolean executeJob = Arrays.stream(args).anyMatch("execute/j"::equals);
        idsAndPaths = removeElementsBefore(args, "execute/j");

        int homeBankId;
        int botJobId;
        try {
            homeBankId = Integer.parseInt(idsAndPaths[0]);
            botJobId = Integer.parseInt(idsAndPaths[1]);
        } catch (Exception e) {
            throw new Exception("no reference (id) for home banking or bot job");
        }

        executeSpecificBlock = -1;
        try {
            executeSpecificBlock = Integer.parseInt(idsAndPaths[2]) - 1;
            log.info("Running Block Id: " + idsAndPaths[2]);

            if (executeSpecificBlock < 0) {
                log.info("Running All Blocks");
            }
        } catch (Exception e) {
            log.warn("Running All Blocks. Param passed for Block was: \"" + idsAndPaths[2] + "\"");
        }

        ErrorMessage errorMessage = performDBEngine.loadHomeBanking(homeBankId);
        if (errorMessage == null) errorMessage = performDBEngine.loadHomeUrls(homeBankId);
        if (errorMessage == null) errorMessage = performDBEngine.loadCompleteJobs(botJobId);
        if (errorMessage == null) errorMessage = performDBEngine.loadAllVariables("variable", botJobId);
        if (errorMessage == null) excelDataGoto = performDBEngine.loadExcelGotoBlock(botJobId, "instruction");

        if (errorMessage == null && !performLists.getListBotJob().isEmpty()) {
            blocksLoaded = performLists.getListBotJob().get(0).getBlockLoadDTOList();
            errorMessage = performDBEngine.loadAllActionsPerBlock(blocksLoaded);
        } else if (performLists.getListBotJob().isEmpty()) {
            log.warn("I cannot find a Bot Job with this Organization ID: " + homeBankId + " Environment ID: "
                    + botJobId);
        }

        if (errorMessage != null) {
            log.error("Error: " + errorMessage.getErrorMessage());
            performMessage.errorMessage(
                    errorMessage.getErrorTitle(),
                    "<span style='color: #D32F2F; font-weight: bold; font-size: 1.1em;'>Operation Failed!</span> ❌",
                    "<span style='color: #E65100; font-weight: bold;'>Error Type:</span> "
                            + errorMessage.getErrorHeader(),
                    "<span style='font-style: italic;'>Detail:</span> " + errorMessage.getErrorMessage(),
                    null,
                    0);
            System.exit(0);
        }

        if (performLists.getListBotJob().isEmpty()) {
            log.error("Cannot find Bot Jobs with this Id:" + botJobId);
            System.exit(0);
        }

        HomeBankingLoadDTO homeBanking = performLists.getHomeBankingById(homeBankId);
        if (homeBanking == null || StringUtils.isNullOrEmpty(homeBanking.getUrl())) {
            log.error("Cannot find Home Banking Environment Id:" + homeBankId);
            System.exit(0);
        }

        currentBotJob = performLists.getListBotJob().get(0);
        this.currentBotJob.setHomeBankingLoadDTO(homeBanking);
        HomeUrlDTO homeUrlDTO = performLists.getHomeUrlByBankId(
                this.currentBotJob.getHomeBankingId(), this.currentBotJob.getHomeUrlId());

        if (homeUrlDTO != null) {
            this.currentBotJob.setHomeUrlId(homeUrlDTO.getId());
            homeBanking.setUrl(homeUrlDTO.getUrl());
        }

        currentBotJobName = this.currentBotJob.getName();

        try {
            excelPath = idsAndPaths[3];
        } catch (Exception ignore) {
            excelPath = excelPath + "\\" + currentBotJobName + ".xlsx";
            log.warn("Excel data file defined : " + excelPath);
        }

        ExcelReader excelReader = new ExcelReader();
        try {
            extractedData = excelReader.extractData(excelPath, performLists.getAllActions());
        } catch (Exception error) {
            performMessage.errorMessage(
                    "Error Processing Excel File",
                    "<span style='color: #D32F2F; font-weight: bold; font-size: 1.1em;'>Failed to Execute Excel File!</span> ⚠️",
                    "<span style='color: #E65100; font-weight: bold;'>Please carefully review all Excel columns and their values for potential errors.</span>",
                    "<span style='font-style: italic;'>Inconsistent or incorrect data can prevent the application from processing the file.</span>",
                    null,
                    0);
        }

        if (extractedData.getNumberOfDataRows() == 0) {
            extractedData.addField("$EMPTY");
            extractedData.addFieldValue("$EMPTY", "$EMPTY", 0);
        }

        if (extractedData != null && extractedData.getErrorMessage() != null) {
            performMessage.errorMessage(
                    "Excel Error", "Could Not Execute Excel File", extractedData.getErrorMessage(), null, null, 0);
            return;
        }

        if (extractedData.getNumberOfDataRows() > 1 && excelDataGoto.isEmpty()) {
            log.warn("Multiple Excel Rows Detected: each next row will return to first block");
        }

        if (executeJob) {
            initializeWebDriver();
            recallJob();
        }
    }

    private void initializeWebDriver() {
        searchHiddenFields = false;

        defaultSearch = new String[] {"input", "textarea", "button", "a", "select", "label"};

        log.info("Calling Engine");

        // Ensure botJob and arPriorities are not null before accessing their methods
        if (currentBotJob != null && arPriorities != null) {
            // Check if we need to update arPriorities
            if (arPriorities.getJobId() == null || !arPriorities.getJobId().equals(this.currentBotJob.getId())) {
                // Set Job ID in arPriorities
                arPriorities.setJobId(this.currentBotJob.getId());

                // Check for non-null HomeBanking and Priority
                HomeBankingLoadDTO homeBanking = performLists.getHomeBankingById(this.currentBotJob.getHomeBankingId());
                if (homeBanking != null) {
                    String priorityValue = homeBanking.getPriority();
                    String searchConfig = homeBanking.getSearchConfig();

                    if (priorityValue != null) {
                        ARPriorities.loadPrioritiesFromString(priorityValue);
                    } else {
                        arPriorities.loadPriorities();
                    }

                    ARPriorities.loadSearchElementsConfig(searchConfig);
                }

                // Initialize performAction with arPriorities and arWebDriver

                performActions.initialize(arPriorities);
                performActions.setCurrentDriver(currentARWebDriver.getCurrentDriver());
            }
        }

        // Assign instance variables
        performActions.initialize(arPriorities);
        performActions.setCurrentDriver(currentARWebDriver.getCurrentDriver());

        if (!openWebDriver(true)) {
            closeWebDrivers();
            return;
        }
    }

    public String[] removeElementsBefore(String[] array, String target) {
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

    private void recallJob() {
        if (isJobRunning.compareAndSet(false, true)) { // Try to set to true if currently false
            try {
                if (executorServicePreLaunch == null || executorServicePreLaunch.isShutdown()) {
                    executorServicePreLaunch = Executors.newSingleThreadExecutor();
                }

                executorServicePreLaunch.submit(() -> {
                    try {
                        executeJob();
                        System.exit(0);
                    } finally {
                        isJobRunning.set(false);
                    }
                });
            } catch (Exception ignore) {
                // Log the error properly instead of ignoring

                log.error("Error submitting to executorServicePreLaunch: " + ignore.getMessage());
                isJobRunning.set(false); // Ensure flag is reset on submission failure
            }
        } else {
            // Optionally log that a new execution was requested but is already running
            log.info("recallJob() requested while executeJob() was running.");
        }

        if (performActions.getCurrentDriver().getWindowHandles().size() != performActions.windowHandlesList.size()) {
            performActions.updateWindowHandlesList();
        }
    }

    private static void printLog(String resultActions, boolean result) {
        String resultMsg = result ? ARConstantsEngine.SUCCESS : ARConstantsEngine.FAIL;
        String log = String.join(ARConstantsEngine.FIELDS_SEPARATOR, resultMsg, resultActions);
        logLaunch.info(log);
    }

    private int handleGreaterThan(String value1, String value2) {
        double num1 = parseValueGreaterThan(clean(value1), true);
        double num2 = parseValueGreaterThan(clean(value2), false);

        return num1 > num2 ? 1 : 0;
    }

    private double parseValueGreaterThan(String value, boolean isValue1) {
        // Handle EMPTY markers
        if (value == null || "$EMPTY".equalsIgnoreCase(value) || "#EMPTY".equalsIgnoreCase(value)) {
            return Double.MIN_VALUE;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            if (isValue1) {
                logOperations.warn("Invalid numeric value for value1: " + value);
                return Double.MIN_VALUE;
            } else {
                logOperations.warn("Invalid numeric value for value2: " + value);
                return Double.MAX_VALUE;
            }
        }
    }

    private int handleLessThan(String value1, String value2) {
        double num1 = parseValueForLessThan(clean(value1), true);
        double num2 = parseValueForLessThan(clean(value2), false);

        return num1 < num2 ? 1 : 0;
    }

    private double parseValueForLessThan(String value, boolean isValue1) {
        // Handle EMPTY markers
        if (value == null || "$EMPTY".equalsIgnoreCase(value) || "#EMPTY".equalsIgnoreCase(value)) {
            return Double.MAX_VALUE;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            if (isValue1) {
                logOperations.warn("Invalid numeric value for value1: {}", value);
                return Double.MAX_VALUE;
            } else {
                logOperations.warn("Invalid numeric value for value2: {}", value);
                return Double.MIN_VALUE;
            }
        }
    }

    private String finalLogMessage(String failedMessage, String resultActions) {
        if (!Strings.isNullOrEmpty(failedMessage)) {
            return failedMessage + resultActions;
        }
        return resultActions;
    }

    private FieldData updateMSGInstruction(FieldData msgInstruction, String failedMessage) {
        String currentKey = msgInstruction.getKey();
        String updatedKey = failedMessage + " - " + currentKey;
        return new FieldData(updatedKey, msgInstruction.getValue());
    }

    public HomeUrlDTO findMatchingHomeUrlDTO(BotJobLoadDTO botJobLoadDTO) {
        Integer targetHomeUrlId = botJobLoadDTO.getHomeUrlId();
        HomeBankingLoadDTO homeBanking = botJobLoadDTO.getHomeBankingLoadDTO();

        if (homeBanking != null && homeBanking.getHomeUrlDTOs() != null) {
            return homeBanking.getHomeUrlDTOs().stream()
                    .filter(dto -> dto.getId().equals(targetHomeUrlId))
                    .findFirst()
                    .orElse(null);
        }

        return null;
    }

    private boolean openWebDriver(boolean firstLoad) {

        String webDriverPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_WEBDRIVER);
        if (!(new File(webDriverPath)).exists()) {
            performMessage.errorMessage(
                    "Action Required: Missing WebDriver",
                    "<span style='color: #D32F2F; font-weight: bold; font-size: 1.1em;'>Critical: The WebDriver file is missing!</span>",
                    "<span style='color: #2E7D32; font-weight: bold;'>To execute automated browser interactions, the WebDriver is absolutely essential.</span>",
                    "<span style='font-style: italic;'>Please download the correct WebDriver for your browser and ensure it is accessible by the application.</span>",
                    null,
                    0);
            return false;
        }
        String browserType = arPropertyManager.getProperty(ARPropertyEnum.BROWSER);

        if (!firstLoad
                && isBrowserClosed(performActions.getCurrentDriver())
                && performActions.getCurrentDriver() != null) {
            performActions.getCurrentDriver().quit();
            performActions.setCurrentDriver(null);
            currentARWebDriver.getCurrentDriver().quit();
            currentARWebDriver.setCurrentDriver(null);
            firstLoad = true;
        }

        if (firstLoad) {
            HomeUrlDTO homeUrlDTO = performLists.getHomeUrlByBankId(
                    this.currentBotJob.getHomeBankingId(), this.currentBotJob.getHomeUrlId());
            HomeBankingLoadDTO homeBanking = performLists.getHomeBankingById(this.currentBotJob.getHomeBankingId());

            WebDriver returned = currentARWebDriver.openDriver(
                    browserType,
                    webDriverPath,
                    homeUrlDTO.getUrl(),
                    homeBanking.getOptionsConfig(),
                    defaultSearch,
                    searchHiddenFields,
                    portSocketInitial);

            if (returned == null) {
                return false;
            }

            performActions.initialize(arPriorities);
            performActions.setCurrentDriver(currentARWebDriver.getCurrentDriver());
        } else {
            if (currentARWebDriver.getCurrentDriver() != null) {
                HomeUrlDTO homeUrlDTO = performLists.getHomeUrlByBankId(
                        this.currentBotJob.getHomeBankingId(), this.currentBotJob.getHomeUrlId());
                currentARWebDriver.getCurrentDriver().get(homeUrlDTO.getUrl());
            }
        }

        //        try {
        //            performActions.onHoldInSeconds(3);
        //        } catch (Exception ignore) {
        //        }

        return true;
    }

    public boolean isBrowserClosed(WebDriver webDriver) {
        try {
            webDriver.getTitle(); // Try accessing a property
            return false; // If no exception, browser is open
        } catch (Exception e) {
            return true; // If exception occurs, browser is closed
        }
    }

    // Method to close all WebDriver instances
    public void closeWebDrivers() {
        for (WebDriver driver : currentARWebDriver.getWebDriverList()) {
            try {
                driver.quit();
                log.info("WebDriver closed.");
            } catch (Exception e) {
                log.warn("Closing WebDriver: " + e.getMessage());
            }
        }
        currentARWebDriver.getWebDriverList().clear();
        currentARWebDriver.setCurrentDriver(null); // reset current driver

        currentARWebDriver.closeAllDrivers();
    }

    public boolean lastBrowserTab() {
        // Get all window handles (all open tabs/windows)
        try {
            windowHandles = performActions.getCurrentDriver().getWindowHandles();

            // Convert the window handles set to a list
            List<String> windowHandlesList = new ArrayList<>(windowHandles);

            // Switch to the last window (newly opened tab)
            performActions.getCurrentDriver().switchTo().window(windowHandlesList.get(windowHandlesList.size() - 1));

            return true;
        } catch (Exception e) {

            browserNotAttached();

            return false;
        }
    }

    private void browserNotAttached() {
        String webDriverPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_WEBDRIVER);
        performMessage.errorMessage(
                "The Browser attached with this Web Scanner is Not Active",
                "<span style='font-style: italic;'>Session deleted as the browser has closed the connection!</span>",
                "<span style='color: #E65100; font-weight: bold;'>WebDriver path:</span> <span style='font-weight: bold;'>"
                        + webDriverPath + "</span>",
                "<span style='font-style: italic;'>Please close and Re-Open the Scanner Tool.</span>",
                "<span style='font-style: italic;'>Details: " + "Web Browser was closed before the Scanner Tool"
                        + "</span>",
                0);
    }

    private String generateTimestamp() {
        Date date = new Date();
        dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        return dateFormatter.format(date);
    }

    private void shutDownExecutorService(ExecutorService executorService) {
        if (executorService == null || executorService.isShutdown()) {
            return;
        }
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("ExecutorService did not terminate");
                }
            }
        } catch (InterruptedException error) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
            log.warn("ExecutorService did not terminate");
            System.exit(0);
        }
    }

    public void updateHasAnyInput() {
        if (blocksLoaded == null) return;

        blocksLoaded.forEach(block -> {
            boolean hasInput = block.getInstructionLoad() != null
                    && block.getInstructionLoad().stream()
                            .anyMatch(instr -> instr.getActions() != null
                                    && instr.getActions().startsWith("I:"));

            block.setHasAnyInput(hasInput);
        });
    }

    private void updateRowStatusAndNotify(String color) {
        rowStatus.setColor(color);
        jsonStatus = gson.toJson(rowStatus);
        webSocketSessionManager.sendMessageJson(
                this.currentBotJob.getHomeBankingId(), sessionRowStatus, jsonStatus, "rowStatus");
    }

    private boolean executeJob() {
        if (PerformActions.waitForPage == null) {
            String updateTimeout = arPropertyManager.getProperty(ARPropertyEnum.WEBDRIVER_PAGE_UPDATE_TIMEOUT_SEC);
            String interactionTimeout = arPropertyManager.getProperty(ARPropertyEnum.WEBDRIVER_PAGE_UPDATE_TIMEOUT_SEC);
            PerformActions.waitForPage = new WebDriverWait(
                    performActions.getCurrentDriver(), Duration.ofSeconds(Integer.parseInt(updateTimeout)));
            PerformActions.waitForAction = new WebDriverWait(
                    performActions.getCurrentDriver(), Duration.ofSeconds(Integer.parseInt(interactionTimeout)));
        }

        Labels.initializeLabelsInSpecLang("en");
        Properties labelsValue = Labels.labelsValue;

        String baseLogString =
                currentBotJobName + ARConstantsEngine.FIELDS_SEPARATOR + labelsValue.getProperty(Labels.START);

        logLaunch.info(baseLogString);

        ExcelWriter.ExcelChain writerReport =
                new ExcelWriter(currentBotJobName, performActions.getCurrentDriver(), false).withPurpose("report");
        writerReport.insertReportHead();

        ExcelWriter.ExcelChain writerExport = null;
        //                new ExcelWriter(blocksLoaded.get(0).getName(),
        // performActions.getCurrentDriver()).withPurpose("export");
        boolean excelExportOnceCreation = true;
        //        writerExport.insertReportHead();

        Set<String> mapIgnore = new HashSet<>();

        String mainMsg = "";
        boolean byPassNotFound = false;
        boolean byPassFlagLoop = false;
        boolean success = true;
        boolean stopAll = false;
        boolean firstRound = true;
        boolean anyFailure = false;
        boolean alreadyLogged = false;
        long botJobStartTime = System.nanoTime();
        long totalExecutionTime = 0;
        String resultActions = "No instruction executed yet";
        String failedMessage = "";
        Map<String, String> dataExcel = null;
        Integer lastBlockOrderPushed = null;
        TargetElement matchScanned = null;
        TargetElement matchXPath = null;
        WebElement webElementFound = null;
        int navTime = getNavigationTimeSeconds();
        String previousExcelFieldName = "";
        String newExcelFieldName = "";
        String currentExcelFileName = "";
        CsvTable currentTableCSV = null;

        //        List<InputInfo> inputs = new ArrayList<>();

        sessionRowStatus = "engine-perform-bot-job"; // + botJobId;

        variablesLoaded = performLists.getListVariable();
        //        Map<String, String> mapSavedLocators = new HashMap<>();

        Set<Integer> parentIdsForLoop = null;
        Set<Integer> allOutPuts = null;

        Map<String, List<Integer>> mapConditional = new HashMap<>(); // <parentId:Limit Loops> -> <1|5 Times>
        Map<String, Integer> mapLoops = new HashMap<>(); // <parentId:Limit Loops> -> <1|5 Times>
        Map<String, Integer> mapRefresh = new HashMap<>(); // <parentId:Limit Loops> -> <1|5 Times>
        Set<String> loopBlockActive = new HashSet<>();
        Map<String, Integer> loopBlockLimits = new HashMap<>();

        ARExecution.ConditionStatus currentCondition = ARExecution.ConditionStatus.NONE;
        ARExecution.ConditionStatus previousCondition;
        ARExecution.ConditionStatus progressCondition;
        ARExecution.DialogModal respModal = null;

        int exportIndex = 1;
        boolean webElementWork = false;

        if (extractedData.getNumberOfDataRows() > 0) {

            // Execute All Blocks starting from executeSpecificBlock if Defined
            currentBlockOrder = (executeSpecificBlock > -1) ? executeSpecificBlock : 0;
            int blockRecall = currentBlockOrder;
            int blockExcelGoto = blockRecall;

            // BLOCK DEFINED BY "DEFAULT" OR "EXCEL GOTO"
            if (!excelDataGoto.isEmpty() && !blocksLoaded.isEmpty()) {
                Integer parentBlockId =
                        excelDataGoto.get(excelDataGoto.size() - 1).getParentBlockId();
                excelDataGoto
                        .get(0)
                        .setParentBlockId(excelDataGoto.get(0).getBlockId()); // overwrite/fix using block table

                blockExcelGoto = performActions.getBlockOrderNumber(blocksLoaded, parentBlockId) - 1;

                // PREVENTID  LATGER DELETION
                if (blockExcelGoto < 0) {

                    Integer blockOrder = (excelDataGoto != null && !excelDataGoto.isEmpty())
                            ? excelDataGoto.get(0).getBlockOrderNumber()
                            : null;

                    blockExcelGoto = (blockOrder != null && blockOrder > 0) ? blockOrder : 1;
                }
            }

            int xExcelCurrentRow = 0;
            int xExcelDataSize = extractedData.getNumberOfDataRows();
            mapOperators.clear();
            headersExport.clear();
            currentColumnsCSV.clear();
            csvTables.clear();

            while (xExcelCurrentRow <= xExcelDataSize - 1 && !blocksLoaded.isEmpty() && !stopAll) {
                // Clear's Up Any Loop as Per New Line
                mapLoops.clear();
                mapRefresh.clear();

                if (firstRound) {
                    firstRound = false;
                    currentBlockOrder = blockRecall; // start blocks from initial for this row
                } else {
                    currentBlockOrder = blockExcelGoto; // start blocks from initial for this row
                }

                blockLoop:
                while (currentBlockOrder <= blocksLoaded.size() - 1 && !blocksLoaded.isEmpty() && !stopAll) {
                    long blockStartTime = System.nanoTime();
                    failedMessage = "";

                    currentCondition = ARExecution.ConditionStatus.NONE;
                    previousCondition = ARExecution.ConditionStatus.NONE;
                    progressCondition = ARExecution.ConditionStatus.NONE;

                    respModal = ARExecution.DialogModal.NONE;

                    int parentBlockCondition = -1;

                    BlockLoadDTO blockLoad = blocksLoaded.get(currentBlockOrder);

                    String blockName = blocksLoaded.get(currentBlockOrder).getName();
                    int blockOrder = blocksLoaded.get(currentBlockOrder).getBlockOrderNumber();
                    String blockReportName = "#" + blockOrder + " " + blockName;

                    int blockWait = blocksLoaded.get(currentBlockOrder).getWait() > 0
                            ? blocksLoaded.get(currentBlockOrder).getWait()
                            : 2;

                    boolean blockActive = blocksLoaded.get(currentBlockOrder).getActive();

                    if (blockActive) {

                        // Fire only when the block CHANGES, and only for ACTIVE blocks
                        if (lastBlockOrderPushed == null || !lastBlockOrderPushed.equals(currentBlockOrder)) {

                            // RESET instruction-level first-load flag
                            firstPageLoadDone = false;

                            performActions.waitPage();
                            lastBlockOrderPushed = currentBlockOrder;

                            performLists.resetListElements();
                            pushUpdateListElements();

                            // Inject actionExecutor plugin (once per page)
                            injectActionExecutor();

                            logOperations.info("Total Target Elements: "
                                    + performLists.getListTargetElements().size());

                            // Inputs-only list with inferred labels
                            //                            inputs.clear();
                            //                            inputs =
                            //
                            //
                            // DomIntrospectionUtil.listAllRelevantElements(performActions.getCurrentDriver());
                        }

                        newExcelFieldName = blockLoad.getExportFile();
                        // Always loads ExcelWrite columns per block
                        ErrorMessage errorMessage = performDBEngine.loadAllColumnsExcelWrite(
                                "instruction", currentBotJob.getId(), blockLoad.getId());
                        if (errorMessage == null) {
                            setCurrentColumns(performLists.getExcelColumnNames());
                        }

                        if (newExcelFieldName != null && !newExcelFieldName.equals(previousExcelFieldName)) {
                            // Sanitize
                            if (!Strings.isNullOrEmpty(newExcelFieldName)) {
                                String[] parts = newExcelFieldName.split(":");
                                if (parts.length > 2) {
                                    delimiterCSV = parts[2];
                                    newExcelFieldName =
                                            newExcelFieldName.replace(":,", "").replace(":|", "");
                                }

                                // Extract only file name with extension
                                Path path = Paths.get(newExcelFieldName);
                                currentExcelFileName = path.getFileName().toString();
                            }

                            String finalNewExcelFieldName = newExcelFieldName;
                            currentTableCSV = csvTables.computeIfAbsent(finalNewExcelFieldName, f -> {
                                CsvTable t = new CsvTable(f, finalNewExcelFieldName, delimiterCSV);
                                t.addColumns(currentColumnsCSV); // addAll per filename
                                return t;
                            });

                            previousExcelFieldName = newExcelFieldName;
                        }
                    }

                    // It Searches the Block That have finished the Loops to Avoid recursivity
                    if (!loopBlockActive.isEmpty()) {
                        for (String blocLoopKey : loopBlockActive) {
                            if (mapLoops.containsKey(blocLoopKey)) {
                                if (mapLoops.get(blocLoopKey) == 0) {
                                    stopAll = true;
                                    int limit = loopBlockLimits.get(blocLoopKey);

                                    FieldData msgBlock = new FieldData(blocLoopKey, "0");

                                    // Excel Report and Log
                                    performActions.logAndReport(
                                            currentCondition,
                                            true,
                                            true,
                                            blockStartTime,
                                            blockReportName,
                                            success,
                                            new String[] {ARConstantsEngine.GOTO},
                                            msgBlock,
                                            dataExcel,
                                            writerReport,
                                            "GOTO Limit Reached",
                                            blocLoopKey + " Reached: 0");

                                    msgBlock = new FieldData(
                                            String.format("Exit at Block Name: \"%s\"", blockLoad.getName()),
                                            ARConstantsEngine.EXIT);

                                    // Excel Report and Log
                                    performActions.logAndReport(
                                            currentCondition,
                                            true,
                                            true,
                                            blockStartTime,
                                            blockReportName,
                                            success,
                                            new String[] {ARConstantsEngine.EXIT},
                                            msgBlock,
                                            dataExcel,
                                            writerReport,
                                            "Stopping App",
                                            String.format("Exit at Block Name: \"%s\"", blockName));

                                    // performActions.gotoLimitExecution(limit, resultActions);

                                    continue blockLoop;
                                }
                            }
                        }
                    }

                    if (!blockActive) {
                        currentBlockOrder++;

                        FieldData msgBlock = new FieldData(
                                String.format("Ignore: \"%s\"", blockLoad.getName()), ARConstantsEngine.IGNORE);

                        // Excel Report and Log
                        performActions.logAndReport(
                                currentCondition,
                                true,
                                true,
                                blockStartTime,
                                blockReportName,
                                success,
                                new String[] {ARConstantsEngine.IGNORE},
                                msgBlock,
                                dataExcel,
                                writerReport,
                                "BLOCK IGNORED",
                                String.format("Block: \"%s\" is Inactive: ", blockName));

                        continue;
                    }

                    try {

                        FieldData msgBlock = new FieldData(blockLoad.getName(), ARConstantsEngine.EXCEL_BLOCK_HEADER);

                        // Block Header Format
                        performActions.logAndReport(
                                currentCondition,
                                true,
                                false,
                                blockStartTime,
                                blockReportName,
                                success,
                                new String[] {ARConstantsEngine.EXCEL_BLOCK_HEADER},
                                msgBlock,
                                null,
                                writerReport,
                                null,
                                null);

                        performActions.onHoldInSeconds(blockWait);

                        msgBlock = new FieldData(
                                String.format("Default Wait: \"%s\" ->  %d Seconds", blockLoad.getName(), blockWait),
                                ARConstantsEngine.HOLD);

                        // Excel Report and Log
                        performActions.logAndReport(
                                currentCondition,
                                true,
                                true,
                                blockStartTime,
                                blockReportName,
                                success,
                                new String[] {ARConstantsEngine.HOLD},
                                msgBlock,
                                dataExcel,
                                writerReport,
                                "BLOCK DEFAULT WAIT",
                                String.format("Block: \"%s\" Wait %s Seconds: ", blockName, blockWait));

                    } catch (Exception ex) {

                        logOperations.error(String.format("Error Wait Block for :\"%s\"", blockLoad.getName()));
                    }

                    allOutPuts = performActions.getAllOutputsPerBlock(
                            blocksLoaded.get(currentBlockOrder).getInstructionLoad());

                    // Step 1: Get all ParentIds For LOOPs Filter rows where actions = "REFRESH_LOOP" or "LOOP" on
                    // current
                    // Block
                    parentIdsForLoop = performActions.getParentIdsForLoop(
                            blocksLoaded.get(currentBlockOrder).getInstructionLoad());

                    // Step 2: Get all Conditional By parentId for Index Locator on current Block Relocate "IF",
                    // "ELSEIF",
                    // "ELSE", and "ENDIF"
                    mapConditional = performActions.getConditionIndexMapByParentId(blockLoad);

                    // Step 3: Get all Instructions Ids on current Block
                    int[] instructionIds = blockLoad.getInstructionLoad().stream()
                            .mapToInt(InstructionLoad::getId)
                            .toArray();

                    // Step 2: Filter rows where actions = "REFRESH_LOOP" or "LOOP" and collect into the map

                    //                mapLoops = performActions.getLoopAndRefreshLoops(
                    //                        blocksLoaded.get(currentBlockOrder).getBlockLoopInstructionLoadS());

                    //                executionTimes++;
                    boolean jumpGoto = false;
                    boolean jumpLoop = false;
                    boolean jumpGotoError = false;
                    boolean jumpLoopError = false;
                    boolean refreshLoop = false;
                    boolean refreshOnly = false;

                    while (xExcelCurrentRow < extractedData.getNumberOfDataRows() && !stopAll) {
                        failedMessage = "";
                        //                        tableCSV.clear();

                        //                    writerReport.insertBlockSeparation(blockLoad.getName());

                        dataExcel = extractedData.getRowFieldValues(xExcelCurrentRow);

                        int currentIndex = 0;

                        instructionLoop:
                        while (currentIndex < instructionIds.length && !stopAll) {
                            // Resets the success

                            stopAll = isInterceptBotJob();
                            if (stopAll) {
                                break;
                            }

                            success = true;
                            webElementWork = false;

                            long currentInstructionStartTime = System.nanoTime();

                            InstructionLoad currentInstruction =
                                    blockLoad.getInstructionLoad().get(currentIndex);

                            byPassFlagLoop = parentIdsForLoop.contains(currentInstruction.getId());

                            mainMsg =
                                    currentInstruction.getOptional() ? "optional instruction" : "mandatory instruction";

                            if (!currentInstruction.getInstructionActive()) {

                                String nameInstruc =
                                        "(" + currentInstruction.getId() + ") " + currentInstruction.getName();
                                FieldData msgBlock = new FieldData(
                                        String.format("Ignore: \"%s\"", nameInstruc), ARConstantsEngine.IGNORE);

                                // Excel Report and Log
                                performActions.logAndReport(
                                        currentCondition,
                                        true,
                                        true,
                                        blockStartTime,
                                        blockReportName,
                                        success,
                                        new String[] {ARConstantsEngine.IGNORE},
                                        msgBlock,
                                        dataExcel,
                                        writerReport,
                                        "INSTRUCTION IGNORED",
                                        String.format("Instruction: \"%s\" is Inactive: ", nameInstruc));

                                currentIndex++;

                                continue;
                            }

                            // FIRST IMMEDIATE ATTEMPT TO LOCATE
                            if (isWebElementInstruction(currentInstruction)) {
                                webElementFound = immediateXPath(currentInstruction.getXpath());
                            }

                            performActions.waitPage();
                            try {
                                if (navTime > 0) {
                                    performActions.onHoldInSeconds(navTime);
                                    logOperations.info("Navigation Time : {}", navTime);
                                }
                            } catch (Exception ignore) {
                            }

                            // Fire on FIRST page load OR when the INSTRUCTION changes
                            // and only for web-element work (INPUT / OUTPUT / CLICK / GET / SET)
                            if (isWebElementInstruction(currentInstruction) && webElementFound == null) {

                                Integer currentInstructionId = currentInstruction.getId();

                                if (!firstPageLoadDone
                                        || lastInstructionIdPushed == null
                                        || !lastInstructionIdPushed.equals(currentInstructionId)) {

                                    firstPageLoadDone = true;
                                    lastInstructionIdPushed = currentInstructionId;

                                    performLists.resetListElements();
                                    pushUpdateListElements();

                                    logOperations.info("Total Target Elements: "
                                            + performLists
                                                    .getListTargetElements()
                                                    .size());

                                    // runYourScript(currentInstructionId);
                                }
                            }

                            //                            mapSavedLocators.clear();
                            //
                            //                            // Loop through the instructionReferenceLoadDTOList
                            //                            if (currentInstruction.getReferenceLoadDTOList() != null) {
                            //                                for (ReferenceLoadDTO reference :
                            // currentInstruction.getReferenceLoadDTOList()) {
                            //                                    // Populate the map with referenceType as the key and
                            // value as the value
                            //                                    mapSavedLocators.put(reference.getReferenceType(),
                            // reference.getValue());
                            //                                }
                            //                            }

                            currentIndex++;

                            // Allow Re-Execute Instructions in Previous Blocks
                            //                        if (currentInstruction.getExecuted() == null ||
                            // !currentInstruction.getExecuted()) {
                            boolean execGetOrSet = false;
                            boolean execCheckValue = false;
                            boolean execPDFCheck = false;
                            boolean execCSVCheck = false;
                            boolean execOutPut = false;
                            boolean execExcellWrite = false;
                            boolean pauseOperation = false;
                            boolean nextEnter = false;
                            boolean swipeUp = false;
                            boolean swipeDown = false;

                            String xPathOperation = null;
                            String[] parentActions = null;
                            String parentField = null;
                            String parentFieldLoop = null;
                            String variableField = null;
                            String localFormat = null;
                            //                            delimiterCSV = null;
                            String fieldName = null;
                            int parentId = currentInstruction.getParentId();

                            if (mapIgnore.contains(currentInstruction.getId() + "-" + currentInstruction.getName())) {
                                continue;
                            }

                            // webSocketSessionManager.sendMessageJson(int homeBankingId, String sessionId, String msg1,
                            // String msg2)
                            if (rowStatus.getInstructionId() == null) {
                                rowStatus.setInstructionId(currentInstruction.getId());
                                rowStatus.setColor("yellow"); // #fcba03 deep carmine yellow
                                jsonStatus = gson.toJson(rowStatus);
                                webSocketSessionManager.sendMessageJson(
                                        this.currentBotJob.getHomeBankingId(),
                                        sessionRowStatus,
                                        jsonStatus,
                                        "rowStatus");
                            } else {
                                // Previous
                                rowStatus.setColor("green"); // #1d9c06 green
                                jsonStatus = gson.toJson(rowStatus);
                                webSocketSessionManager.sendMessageJson(
                                        this.currentBotJob.getHomeBankingId(),
                                        sessionRowStatus,
                                        jsonStatus,
                                        "rowStatus");
                                try {
                                    Thread.sleep(300);
                                } catch (Exception e) {
                                }
                                // Current
                                rowStatus.setInstructionId(currentInstruction.getId());
                                rowStatus.setColor("yellow"); // #fcba03 deep carmine yellow
                                jsonStatus = gson.toJson(rowStatus);
                                webSocketSessionManager.sendMessageJson(
                                        this.currentBotJob.getHomeBankingId(),
                                        sessionRowStatus,
                                        jsonStatus,
                                        "rowStatus");
                            }

                            //                        String[] operation =
                            // UtilsMethods.splitIfContains(instruction.getOperation(),
                            // ARConstants.ACTION_SPECIFICATIONS_SPLITTER);
                            String[] actions = currentInstruction
                                    .getActions()
                                    .split(ARConstantsEngine.ACTION_SPECIFICATIONS_SPLITTER);
                            String[] operations = currentInstruction.getOperation() != null
                                    ? currentInstruction
                                            .getOperation()
                                            .split(ARConstantsEngine.ACTION_SPECIFICATIONS_SPLITTER)
                                    : null;

                            if (actions[0].equalsIgnoreCase(ARConstantsEngine.IF)
                                    || actions[0].equalsIgnoreCase(ARConstantsEngine.ELSEIF)
                                    || actions[0].equalsIgnoreCase(ARConstantsEngine.ELSE)
                                    || actions[0].equalsIgnoreCase(ARConstantsEngine.ENDIF)) {
                                currentCondition = ARExecution.ConditionStatus.valueOf(actions[0]);
                                if (previousCondition.equals(ARExecution.ConditionStatus.NONE)) {
                                    previousCondition = currentCondition;
                                    parentBlockCondition = parentId;
                                } else if (!previousCondition.equals(
                                        currentCondition)) { // To Reset the Progress to the Next Block
                                    previousCondition = currentCondition;
                                }

                                // Conditions When Pass to any of then
                                if (progressCondition.equals(ARExecution.ConditionStatus.IF_PASSED)
                                        || progressCondition.equals(ARExecution.ConditionStatus.ELSEIF_PASSED)) {
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
                                        currentCondition = ARExecution.ConditionStatus.NONE;
                                        progressCondition = ARExecution.ConditionStatus.NONE;
                                        continue instructionLoop;
                                    }
                                } else if (currentCondition.equals(ARExecution.ConditionStatus.ENDIF)) {
                                    currentCondition = ARExecution.ConditionStatus.NONE;
                                    previousCondition = ARExecution.ConditionStatus.NONE;
                                    progressCondition = ARExecution.ConditionStatus.NONE;
                                    parentBlockCondition = -1;
                                }
                                continue;
                            }

                            // Case for Inputs.
                            // Post-migration 2026-04-26, INSERT actions are always "I:<reference>".
                            // The legacy "I:E:<reference>" shape (ENTER after input) no longer
                            // exists in actions. ENTER is a bit in force_coordinates now.
                            //
                            // Block scoped lookup. Same canonical name in two blocks is now a
                            // valid scenario, each block owns its own column. The lookup key
                            // is displayKey, which is clientNamed when the user set an override
                            // and the canonical name otherwise. It matches what the Excel writer
                            // used as the column header. Falls back to the canonical name for
                            // legacy Excel files written before clientNamed was set.
                            // ExtractedData uses lenient matching, trim and case insensitive,
                            // on both block and field name, so cosmetic Excel edits do not break
                            // the lookup.
                            String valueInsert = "CHANGE ME";
                            if (actions[0].equals(ARConstantsEngine.INSERT)) {
                                String displayKey = currentInstruction.displayKey();
                                String currentBlockName = blockLoad.getName();
                                valueInsert =
                                        extractedData.getFieldValue(currentBlockName, displayKey, xExcelCurrentRow);
                                if (valueInsert == null) {
                                    valueInsert = extractedData.getFieldValue(
                                            currentBlockName, currentInstruction.getName(), xExcelCurrentRow);
                                }
                                if (valueInsert == null) {
                                    log.warn(
                                            "Excel lookup miss block [{}] displayKey [{}] canonical [{}] row {} availableBlocks {} fieldsInBlock {}",
                                            currentBlockName,
                                            displayKey,
                                            currentInstruction.getName(),
                                            xExcelCurrentRow,
                                            extractedData.getBlocks(),
                                            extractedData.getExtractedFields(currentBlockName));
                                }
                            }

                            FieldData msgInstruction = null;
                            if (actions[0].equalsIgnoreCase(ARConstantsEngine.EXCEL_GOTO)) {

                                //                                currentIndex++;
                                continue instructionLoop;

                            } else if (actions[0].equalsIgnoreCase(ARConstantsEngine.NEXT_ROW)) {
                                // <currentId:blockId:blockOrderNumber:bockName>
                                xExcelCurrentRow++;

                                String bodyMsg = "Excel Data Calling Next Row: " + (xExcelCurrentRow + 1);

                                if (xExcelCurrentRow >= xExcelDataSize - 1) {
                                    xExcelCurrentRow = xExcelDataSize - 1;
                                    msgInstruction = new FieldData(
                                            "Excel Data (limit reached) keeping last row",
                                            String.valueOf(xExcelCurrentRow + 1));
                                    bodyMsg = "Excel Data (limit reached) keeping last row: " + xExcelCurrentRow + 1;
                                } else {
                                    msgInstruction =
                                            new FieldData("Excel Data next row", String.valueOf(xExcelCurrentRow + 1));
                                }

                                // Excel Report and Log
                                performActions.logAndReport(
                                        currentCondition,
                                        true,
                                        true,
                                        blockStartTime,
                                        blockReportName,
                                        success,
                                        new String[] {ARConstantsEngine.NEXT_ROW},
                                        msgInstruction,
                                        dataExcel,
                                        writerReport,
                                        "Excel Data Calling Next Row",
                                        bodyMsg);

                                //                                currentIndex++;
                                continue instructionLoop;

                            } else if (actions[0].equalsIgnoreCase(ARConstantsEngine.GOTO)) {
                                // <currentId:blockId:blockOrderNumber:bockName>
                                msgInstruction = performActions.getBlockDetailsById(blocksLoaded, currentInstruction);
                                if (msgInstruction == null) {
                                    msgInstruction = new FieldData("GO TO Block \"Unknown\"", "Unknown");
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
                                    msgInstruction = new FieldData(
                                            msgInstruction.getKey(),
                                            String.valueOf(mapLoops.get(msgInstruction.getKey())));
                                }

                            } else if (actions[0].equalsIgnoreCase(ARConstantsEngine.LOOP)) {
                                // <currentId:parentId:parentName>
                                msgInstruction = performActions.getInstructionDetailsById(
                                        blocksLoaded.get(currentBlockOrder).getInstructionLoad(), currentInstruction);

                                if (msgInstruction == null) {
                                    msgInstruction = new FieldData("Jump To Parent \"Unknown\"", "Unknown");
                                    success = false;
                                } else if (!mapLoops.containsKey(msgInstruction.getKey())) {
                                    jumpLoopError = false;
                                    String[] parts = msgInstruction.getValue().split(":"); // Split by ':'
                                    mapLoops.put(msgInstruction.getKey(), Integer.valueOf(parts[1])); // Loop Times
                                    mapRefresh.put(msgInstruction.getKey(), Integer.valueOf(parts[0])); // Wait Time
                                } else if (mapLoops.containsKey(msgInstruction.getKey())) {
                                    // Updates the msgInstruction
                                    msgInstruction = new FieldData(
                                            msgInstruction.getKey(),
                                            String.valueOf(mapLoops.get(msgInstruction.getKey())));
                                }
                            } else if (actions[0].equalsIgnoreCase(ARConstantsEngine.REFRESH_LOOP)) {
                                msgInstruction = performActions.getInstructionDetailsById(
                                        blocksLoaded.get(currentBlockOrder).getInstructionLoad(), currentInstruction);
                                if (msgInstruction == null) {
                                    msgInstruction = new FieldData("Jump To Parent \"Unknown\"", "Unknown");
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
                                    msgInstruction = new FieldData(msgInstruction.getKey(), updMsg);
                                }
                            } else if (actions[0].equalsIgnoreCase(ARConstantsEngine.SET_VALUE)
                                    || (actions[0].equalsIgnoreCase(ARConstantsEngine.GET_VALUE))) {
                                msgInstruction = new FieldData(
                                        currentInstruction.getName(),
                                        (currentInstruction.getOperation() != null
                                                ? "(" + parentId + ")-" + operations[0] + ":" + operations[1]
                                                : (actions[0].equalsIgnoreCase(ARConstantsEngine.INSERT))
                                                        ? valueInsert
                                                        : ""));
                            } else {
                                msgInstruction = new FieldData(
                                        "(" + currentInstruction.getId() + ")-" + currentInstruction.getName(),
                                        (currentInstruction.getOperation() != null
                                                ? currentInstruction.getOperation()
                                                : (actions[0].equalsIgnoreCase(ARConstantsEngine.INSERT))
                                                        ? valueInsert
                                                        : ""));
                            }

                            resultActions = performActions.actionResultMessage(blockName, actions, msgInstruction);

                            if (actions[0].equalsIgnoreCase(ARConstantsEngine.PAUSE)) {
                                pauseOperation = true;

                                respModal = performMessage.showCustomModalDialogDragWin11(
                                        "PAUSE BOT JOB",
                                        "PAUSED at Block Name",
                                        blockLoad.getName(),
                                        " Please click OK to continue!",
                                        null,
                                        false,
                                        "Continue",
                                        "Stop Run",
                                        0);
                            } else if (actions[0].equalsIgnoreCase(ARConstantsEngine.NEXT_ENTER)) {
                                nextEnter = true;
                            } else if (actions[0].equalsIgnoreCase(ARConstantsEngine.SWIPE_UP)) {
                                swipeUp = true;
                            } else if (actions[0].equalsIgnoreCase(ARConstantsEngine.SWIPE_DOWN)) {
                                swipeDown = true;
                            }

                            if (actions[0].equalsIgnoreCase(ARConstantsEngine.LOOP)) {
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

                            } else if (actions[0].equalsIgnoreCase(ARConstantsEngine.REFRESH_ONLY)) {
                                refreshOnly = true;
                            } else if (actions[0].equalsIgnoreCase(ARConstantsEngine.REFRESH_LOOP)) {
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
                            } else if (actions[0].equalsIgnoreCase(ARConstantsEngine.GET_VALUE)
                                    || actions[0].equalsIgnoreCase(ARConstantsEngine.SET_VALUE)) {

                                execGetOrSet = true;

                                xPathOperation = performActions.getXPathInstruction(currentInstruction, blockLoad);
                                String actionsParent =
                                        performActions.getInstructionParentActions(currentInstruction, blockLoad);
                                parentActions = actionsParent != null
                                        ? actionsParent.split(ARConstantsEngine.ACTION_SPECIFICATIONS_SPLITTER)
                                        : null;

                                parentField = performActions.getInstructionParentField(currentInstruction, blockLoad);
                                variableField =
                                        performActions.getInstructionVariableField(currentInstruction, variablesLoaded);
                                localFormat = performActions.getInstructionVariableFormat(
                                        currentInstruction, variablesLoaded);
                                if (variableField == null) {
                                    variableField = "Not Variable defined";
                                }

                            } else if (actions[0].equalsIgnoreCase(ARConstantsEngine.OUTPUT)) {
                                execOutPut = true;
                                fieldName = currentInstruction.getId() + "-" + currentInstruction.getName();
                            } else if (actions[0].equalsIgnoreCase(ARConstantsEngine.CHECK_VALUE)) {
                                execCheckValue = true;
                                parentField = performActions.getInstructionParentField(currentInstruction, blockLoad);
                                variableField =
                                        performActions.getInstructionVariableField(currentInstruction, variablesLoaded);
                                if (variableField == null) {
                                    variableField = "Not Variable defined";
                                }
                            } else if (actions[0].equalsIgnoreCase(ARConstantsEngine.PDF_CHECK)) {
                                execPDFCheck = true;
                                parentField = performActions.getInstructionParentField(currentInstruction, blockLoad);
                                variableField =
                                        performActions.getInstructionVariableField(currentInstruction, variablesLoaded);
                                if (variableField == null) {
                                    variableField = "Not Variable defined";
                                }
                            } else if (actions[0].equalsIgnoreCase(ARConstantsEngine.CSV_CHECK)) {
                                execCSVCheck = true;
                                parentField = performActions.getInstructionParentField(currentInstruction, blockLoad);
                                variableField =
                                        performActions.getInstructionVariableField(currentInstruction, variablesLoaded);
                                if (variableField == null) {
                                    variableField = "Not Variable defined";
                                }
                            } else if (actions[0].equalsIgnoreCase(ARConstantsEngine.EXTRACT_FIELD)) {
                                execExcellWrite = true;
                                parentField = performActions.getInstructionParentField(currentInstruction, blockLoad);
                                variableField =
                                        performActions.getInstructionVariableField(currentInstruction, variablesLoaded);
                                //                                if (delimiterCSV == null) {
                                //                                    delimiterCSV =
                                // performActions.getInstructionVariableDelimiter(
                                //                                            currentInstruction, variablesLoaded);
                                //                                }
                                if (variableField == null) {
                                    variableField = "Not Variable defined";
                                }
                            }

                            try {
                                if (jumpGoto) {

                                    if (jumpGotoError) {
                                        success = false;
                                        failedMessage = "Failed: GO TO ";
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

                                                currentBlockOrder = blockOrderNumber - 1;
                                                currentInstruction.setExecuted(true);

                                                failedMessage = "";
                                                success = true;

                                            } catch (Exception ex) {
                                                failedMessage = "Failed: GO TO ";
                                                msgInstruction = updateMSGInstruction(msgInstruction, failedMessage);

                                                success = false;

                                                resultActions = performActions.blockGotoFailed(resultActions);
                                            }

                                            FieldData currentPair = new FieldData(
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

                                            logOperations.info(String.format(
                                                    "Loop to Parent :\"%s\" - %d Times",
                                                    parts[0] + "-(" + parts[1] + ") " + parts[2],
                                                    mapLoops.get(parentFieldLoop)));

                                            if (refreshLoop) {

                                                String extraLog = performActions.actionResultMessage(
                                                        blockName,
                                                        new String[] {ARConstantsEngine.REFRESH_HOLD},
                                                        msgInstruction);

                                                performActions.performOtherActions(
                                                        byPassNotFound,
                                                        currentInstruction,
                                                        new String[] {ARConstantsEngine.REFRESH_HOLD});

                                                // Excel Report and Log
                                                performActions.logAndReport(
                                                        currentCondition,
                                                        true,
                                                        true,
                                                        currentInstructionStartTime,
                                                        blockReportName,
                                                        success,
                                                        new String[] {ARConstantsEngine.REFRESH_HOLD},
                                                        msgInstruction,
                                                        dataExcel,
                                                        writerReport,
                                                        mainMsg,
                                                        extraLog);

                                                // Refresh For REFRESH_LOOP
                                                extraLog = performActions.actionResultMessage(
                                                        blockName,
                                                        new String[] {ARConstantsEngine.REFRESH_ONLY},
                                                        msgInstruction);

                                                performActions.performOtherActions(
                                                        byPassNotFound,
                                                        currentInstruction,
                                                        new String[] {ARConstantsEngine.REFRESH_ONLY});

                                                // Excel Report and Log
                                                performActions.logAndReport(
                                                        currentCondition,
                                                        true,
                                                        true,
                                                        currentInstructionStartTime,
                                                        blockReportName,
                                                        success,
                                                        new String[] {ARConstantsEngine.REFRESH_ONLY},
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
                                            FieldData currentPair = new FieldData(
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

                                            logOperations.info(String.format(
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

                                } else if (actions[0].equals(ARConstantsEngine.HOLD)
                                        || actions[0].equals(ARConstantsEngine.QUIT)
                                        || actions[0].equals(ARConstantsEngine.SCREEN)
                                        || actions[0].equals(ARConstantsEngine.REFRESH_ONLY)) {

                                    performActions.performOtherActions(byPassNotFound, currentInstruction, actions);

                                    if (actions[0].equals(ARConstantsEngine.QUIT)) {
                                        stopAll = true;
                                        success = true;
                                    }

                                } else if (!jumpGotoError
                                        && !jumpLoopError
                                        && !execGetOrSet
                                        && !execCheckValue
                                        && !execPDFCheck
                                        && !execCSVCheck
                                        && !execExcellWrite
                                        && !pauseOperation
                                        && !nextEnter
                                        && !swipeUp
                                        && !swipeDown) {

                                    webElementWork = true;

                                    // Block scoped, clientNamed aware extractFieldData. Resolves
                                    // the cell by block displayKey then by block canonical, so
                                    // two blocks with the same column name no longer collide,
                                    // and a renamed field with clientNamed set in React still
                                    // resolves to the right Excel column.
                                    FieldData fieldData = performActions.extractFieldData(
                                            extractedData,
                                            blockLoad.getName(),
                                            xExcelCurrentRow,
                                            currentInstruction,
                                            actions,
                                            currentInstruction.getDefaultValue(),
                                            currentInstruction.getCodified());

                                    //                                    webElementFound = null;
                                    boolean forceCoordinates = InputFlags.of(currentInstruction.getForceCoordinates())
                                            .hasForce();

                                    if (!isMobileApp) {

                                        if (isWebElementInstruction(currentInstruction) && webElementFound == null) {
                                            try {
                                                performActions.waitPage();

                                                matchXPath = InstructionLoadMatcher.findMatchingTargetElementByXPath(
                                                        performLists.getListTargetElements(), currentInstruction);
                                                matchScanned = null;
                                                //                                            InputInfo match =
                                                // findMatchingInput(inputs, currentInstruction);

                                                if (matchXPath == null) {
                                                    matchScanned = InstructionLoadMatcher.findMatchingTargetElement(
                                                            performLists.getListTargetElements(), currentInstruction);

                                                    if (matchScanned != null) {
                                                        InstructionLoadUpdater.applyMatchToInstruction(
                                                                currentInstruction, matchScanned);

                                                        // SECOND IMMEDIATE ATTEMPT TO LOCATE
                                                        webElementFound = immediateXPath(matchScanned.getXPath());
                                                    }
                                                }

                                                // VERY IMPORTANT TO VALIDATE IF THE ELEMENT IS ON TEH PAGE FIRST
                                                //                                            if (matchXPath != null ||
                                                // matchScanned != null || match != null) {
                                                if (webElementFound == null) {
                                                    webElementFound = performActions.searchElement(
                                                            currentInstruction,
                                                            this.currentBotJob.getId(),
                                                            forceCoordinates,
                                                            byPassFlagLoop);
                                                }
                                                // ── Roadmap 3 Phase 3c-iii ────────────────────────────────
                                                // Last-resort fallback: if every existing strategy
                                                // (xpath match, name/text match, priorities ladder) failed,
                                                // try the persisted locator via ElementRecoveryService.
                                                // The recovery service walks its own ladder
                                                // (XPATH_CURRENT > XPATH_ORIGINAL > CSS_SELECTOR >
                                                //  ATTRIB_ID > ATTRIB_NAME > TEXT_FUZZY > COORDS) and
                                                // writes an audit row when a non-direct strategy wins.
                                                if (webElementFound == null
                                                        && currentInstruction.getName() != null
                                                        && !currentInstruction
                                                                .getName()
                                                                .isBlank()) {
                                                    try {
                                                        Integer hbId = this.currentBotJob.getHomeBankingId();
                                                        Integer homeUrlId = this.currentBotJob.getHomeUrlId();
                                                        com.allinweb.ch.model.ElementLocatorEntity loc =
                                                                ElementLocatorRepository.getInstance()
                                                                        .findByKey(
                                                                                hbId,
                                                                                homeUrlId,
                                                                                currentInstruction.getName());
                                                        if (loc != null) {
                                                            ElementRecoveryService.Recovery r =
                                                                    ElementRecoveryService.getInstance()
                                                                            .findOrRecover(
                                                                                    performActions.getCurrentDriver(),
                                                                                    loc);
                                                            if (r.found()) {
                                                                webElementFound = r.element;
                                                                logOperations.info(
                                                                        "ElementRecoveryService recovered '{}'"
                                                                                + " via {} (confidence={})",
                                                                        currentInstruction.getName(),
                                                                        r.strategy,
                                                                        String.format("%.1f", r.confidence));
                                                            }
                                                        }
                                                    } catch (Exception recoveryEx) {
                                                        logOperations.warn(
                                                                "ElementRecoveryService failed (non-fatal): {}",
                                                                recoveryEx.getMessage());
                                                    }
                                                }
                                            } catch (Exception ex) {
                                                success = false;
                                            }
                                        }
                                    } else {
                                        // Safely extract the first element ID (if present)
                                        //                                        Integer elementId =
                                        // Optional.ofNullable(splitDTO.getElementDetails())
                                        //                                                .filter(arr -> arr.length > 0)
                                        //                                                .map(arr -> arr[0])
                                        //                                                .map(ElementDTO::getId)
                                        //                                                .orElse(null);

                                        // Find matching instruction by variableId
                                        //                                        InstructionLoad matchingInstruction =
                                        // Optional.ofNullable(
                                        //
                                        // performLists.getListInstruction())
                                        //
                                        // .orElse(Collections.emptyList())
                                        //                                                .stream()
                                        //                                                .filter(i ->
                                        // Objects.equals(i.getId(), elementId))
                                        //                                                .findFirst()
                                        //                                                .orElse(null);

                                        // 2) Apply only non-empty values into splitDTO and elementDetails[0]
                                        //                                        if (matchingInstruction != null) {
                                        // >>> Add AttrData:* references into elementDetails.attributesData
                                        SplitDTO.applyAttrDataFromReferences(splitDTO, currentInstruction);

                                        SplitDTO.applyInstructionToSplit(splitDTO, currentInstruction);
                                        //                                        }

                                        // Already Maps List<TargetElement>
                                        //
                                        // androidHelper.scanElementsWithCanonicalXmlOnly(
                                        //
                                        // androidDevice.getCurrentDriver());

                                        //                                        webElementFound =
                                        // androidDevice.searchElement(
                                        //                                                splitDTO, actions,
                                        // performLists.getListTargetElements());

                                        if (webElementFound == null) {
                                            appendLog(
                                                    currentInstruction.getName() + "- Not Found- using coordinates",
                                                    "warn");
                                            //
                                            // androidDevice.executeAction(webElementFound, splitDTO, actions);
                                        }
                                    }

                                    if (webElementFound == null && forceCoordinates && !isMobileApp) {

                                        // Enter-after-input flag now lives in force_coordinates,
                                        // not in the actions string. See migration 2026-04-26.
                                        Boolean pressEnterAfter = InputFlags.of(
                                                        currentInstruction.getForceCoordinates())
                                                .hasEnter();
                                        if (actions[0].equalsIgnoreCase(ARConstantsEngine.VISUALIZE)
                                                || actions[0].equalsIgnoreCase(ARConstantsEngine.CLICK)
                                                || actions[0].equalsIgnoreCase(ARConstantsEngine.INSERT)) {

                                            List<WebElement> smartSearch = performActions.findBySmartLocator(
                                                    currentInstruction.getCssSelector());
                                            if (!smartSearch.isEmpty()) {
                                                success = performActions.executeActionsAtCoordinates(
                                                        "coordinates", fieldData, actions[0], pressEnterAfter);
                                            }
                                        }
                                    }

                                    byPassNotFound = byPassFlagLoop
                                            || !currentCondition.equals(ARExecution.ConditionStatus.NONE);

                                    if (webElementFound != null && success) {

                                        success = performActions.performWebActions(
                                                byPassNotFound,
                                                "coordinates",
                                                fieldData,
                                                currentInstruction,
                                                mapOperators,
                                                webElementFound,
                                                actions,
                                                isMobileApp,
                                                splitDTO);

                                        if (execOutPut) {
                                            if (mapOperators.containsKey(fieldName)) {
                                                msgInstruction = new FieldData(fieldName, mapOperators.get(fieldName));
                                            } else {
                                                msgInstruction = new FieldData(fieldName, "TEXT OUTPUT NOT FOUND");
                                            }
                                        }
                                    }
                                    // Special Cases for Select Responses
                                    // It could be Improved the case
                                    if (resultActions.contains("FAIL")
                                            || performLists
                                                    .getListTargetElements()
                                                    .isEmpty()
                                            || (matchXPath == null && matchScanned == null && webElementFound == null)
                                            || (webElementFound == null && !forceCoordinates)) {
                                        failedMessage = "Failed execution Web Element ";
                                        msgInstruction = updateMSGInstruction(msgInstruction, failedMessage);
                                        if (resultActions.contains("PASSED")) {
                                            resultActions = resultActions.replaceAll("PASSED", "FAIL");
                                        }
                                        success = false;

                                        if (performLists.getListTargetElements().isEmpty()) {
                                            String reason = performActions.buildMessageResult(
                                                    success, blockName, "TIME OUT", "Device timeout", "TIME OUT");
                                            appendLog("[TEST]" + reason, "error");
                                            stopAll = true;
                                        }
                                    } else if (resultActions != null && success) {
                                        failedMessage = "";
                                        currentInstruction.setExecuted(true);
                                    }

                                } else if (execGetOrSet) {
                                    // GET && SET Special Operators

                                    if (parentField != null && parentId != 0) {
                                        parentField = parentId + "-" + parentField;
                                    }
                                    // Mandatory for GET_VALUE
                                    if (xPathOperation == null
                                            && actions[0].equalsIgnoreCase(ARConstantsEngine.GET_VALUE)) {
                                        failedMessage = "Parent Id in Wrong Block ";
                                        msgInstruction = updateMSGInstruction(msgInstruction, failedMessage);
                                        resultActions = performActions.parentIdWrongBlock(
                                                currentInstruction, blockLoad, resultActions, currentCondition);
                                        success = false;
                                    } else if (parentField == null) {
                                        failedMessage = "Parent Id in Wrong Block ";
                                        msgInstruction = updateMSGInstruction(msgInstruction, failedMessage);
                                        resultActions = performActions.parentIdWrongBlock(
                                                currentInstruction, blockLoad, resultActions, currentCondition);
                                        success = false;
                                    } else {

                                        webElementFound = null;
                                        if (isMobileApp) {
                                            int index = IntStream.range(0, instructionIds.length)
                                                    .filter(i -> instructionIds[i] == parentId)
                                                    .findFirst()
                                                    .orElse(-1);

                                            InstructionLoad refInstruction = blockLoad
                                                    .getInstructionLoad()
                                                    .get(index);

                                            SplitDTO.applyAttrDataFromReferences(splitDTO, refInstruction);
                                            SplitDTO.applyInstructionToSplit(splitDTO, refInstruction);

                                            //                                            webElementFound =
                                            // androidDevice.searchElement(
                                            //                                                    splitDTO, actions,
                                            // performLists.getListTargetElements());
                                        }

                                        resultActions = performActions.performOperatorActions(
                                                byPassNotFound,
                                                currentInstruction,
                                                xPathOperation,
                                                parentActions,
                                                actions[0],
                                                operations,
                                                parentField,
                                                variableField,
                                                mapOperators,
                                                webElementFound);

                                        if (resultActions.contains("FAIL")) {
                                            failedMessage = "Failed: Operation (GetValue / SetValue) ";
                                            msgInstruction = updateMSGInstruction(msgInstruction, failedMessage);
                                            if (resultActions.contains("PASSED")) {
                                                resultActions = resultActions.replaceAll("PASSED", "FAIL");
                                            }
                                            success = false;
                                        } else {
                                            failedMessage = "";
                                            success = true;
                                            if (!Strings.isNullOrEmpty(localFormat)) {
                                                String valueTo = mapOperators.get(variableField);
                                                valueTo = performActions.removeAllCurrencySymbols(valueTo);
                                                valueTo = performActions.formatLocalNumber(valueTo, localFormat);
                                                mapOperators.put(variableField, valueTo);
                                            }
                                        }
                                    }

                                } else if (execCheckValue) {
                                    // Check Validation Operator

                                    if (!mapOperators.containsKey(variableField)) {
                                        failedMessage = "Get Value Is Not Defined ";
                                        msgInstruction = updateMSGInstruction(msgInstruction, failedMessage);
                                        //                                        resultActions =
                                        // performActions.getValueIsNotDefined(
                                        //                                                actions[0],
                                        //                                                currentInstruction,
                                        //                                                resultActions,
                                        //                                                ARExecution.ConditionStatus
                                        //                                                        .NONE, // NOT
                                        // currentCondition to Force Message,
                                        //                                                parentField,
                                        //                                                variableField);

                                        String reason = performActions.buildGetVariableReason(
                                                actions[0],
                                                currentInstruction,
                                                resultActions,
                                                currentCondition,
                                                parentField,
                                                variableField,
                                                byPassNotFound, // or your bypass flag
                                                blockName,
                                                currentInstruction.getId(),
                                                false);

                                        appendLog("[TEST]" + reason, "error");
                                        alreadyLogged = true;

                                        logOperations.error("{}", reason);

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
                                        } else if (operations[1].equalsIgnoreCase("contains")) {
                                            // Case-insensitive substring match — consistent with "=" / "!="
                                            // which compare via equalsIgnoreCase above.
                                            String actual = mapOperators
                                                    .get(variableField)
                                                    .trim()
                                                    .toLowerCase();
                                            String expected =
                                                    operations[2].trim().toLowerCase();
                                            isOperationValid = actual.contains(expected);
                                        }

                                        if (isOperationValid) {
                                            currentInstruction.setExecuted(true);
                                            failedMessage = "";

                                            resultActions = performActions.buildValidationReason(
                                                    invalidValues,
                                                    parentField,
                                                    mapOperators.get(variableField), // actual/current web value
                                                    operations[2].trim(),
                                                    resultActions, // lastInstructionExecuted
                                                    operations,
                                                    currentCondition,
                                                    byPassNotFound,
                                                    true,
                                                    blockName,
                                                    currentInstruction.getId(),
                                                    isOperationValid);

                                            appendLog("[TEST]" + resultActions, "info");
                                            alreadyLogged = true;

                                            success = true;
                                        } else {
                                            failedMessage = "Failed: Check Validation ";
                                            msgInstruction = updateMSGInstruction(msgInstruction, failedMessage);
                                            //                                            resultActions =
                                            // performActions.checkValidationFailed(
                                            //                                                    invalidValues,
                                            //                                                    parentField,
                                            //
                                            // mapOperators.get(variableField),
                                            //                                                    resultActions,
                                            //                                                    operations,
                                            //                                                    currentCondition,
                                            //                                                    byPassNotFound);

                                            resultActions = performActions.buildValidationReason(
                                                    invalidValues,
                                                    parentField,
                                                    mapOperators.get(variableField), // actual/current web value
                                                    operations[2].trim(),
                                                    resultActions, // lastInstructionExecuted
                                                    operations,
                                                    currentCondition,
                                                    byPassNotFound,
                                                    true,
                                                    blockName,
                                                    currentInstruction.getId(),
                                                    isOperationValid);

                                            appendLog("[TEST]" + resultActions, "error");
                                            alreadyLogged = true;

                                            logOperations.error("Validation failed: {}", resultActions);

                                            success = false;
                                        }
                                    }

                                } else if (execCSVCheck || execPDFCheck) {
                                    String msgCSVPrefix = "CSV ";
                                    if (execPDFCheck) {
                                        msgCSVPrefix = "PDF ";
                                    }

                                    // If fieldsToValidate is null/empty => ignore (no log)
                                    Map<String, FieldsToValidate> fMap = splitDTO.getFieldsToValidate();
                                    if (fMap == null || fMap.isEmpty()) {
                                        // ignore
                                        resultActions = resultActions.replaceAll("PASSED", "IGNORED");
                                    } else {

                                        for (Map.Entry<String, FieldsToValidate> entry : fMap.entrySet()) {

                                            FieldsToValidate expectedField = entry.getValue();

                                            // Only run if parentField exists as a key. If not found => ignore (no log).
                                            if (expectedField == null || expectedField.getValue() == null) {
                                                // ignore
                                            } else {

                                                String parentFieldCSV = entry.getKey();

                                                String foundKey = null;
                                                if (allOutPuts != null && !allOutPuts.isEmpty()) {
                                                    for (Integer outId : allOutPuts) {
                                                        String k = outId + "-" + parentFieldCSV;
                                                        if (mapOperators.containsKey(k)) {
                                                            foundKey = k;
                                                            break;
                                                        }
                                                    }
                                                }

                                                if (foundKey == null) {
                                                    // ignore
                                                } else {

                                                    String actualValue = mapOperators.get(foundKey);

                                                    // You still keep your "Get Value Is Not Defined" behavior
                                                    if (actualValue == null
                                                            || actualValue
                                                                    .trim()
                                                                    .isEmpty()) {
                                                        failedMessage = "Get Value Is Not Defined ";
                                                        msgInstruction =
                                                                updateMSGInstruction(msgInstruction, failedMessage);

                                                        //                                                resultActions
                                                        // =
                                                        // performActions.getValueIsNotDefined(
                                                        //
                                                        // actions[0],
                                                        //
                                                        // currentInstruction,
                                                        //
                                                        // resultActions,
                                                        //
                                                        // ARExecution.ConditionStatus.NONE,
                                                        //
                                                        // parentField,
                                                        //
                                                        // variableField);

                                                        String reason = performActions.buildGetVariableReason(
                                                                actions[0],
                                                                currentInstruction,
                                                                resultActions,
                                                                currentCondition,
                                                                parentField,
                                                                variableField,
                                                                byPassNotFound, // or your bypass flag
                                                                blockName,
                                                                currentInstruction.getId(),
                                                                false);

                                                        appendLog("[TEST]" + reason, "error");
                                                        alreadyLogged = true;

                                                        logOperations.error("{}", reason);

                                                        success = false;

                                                    } else {
                                                        // actual/current value on the web/app side

                                                        // expected value comes from
                                                        // splitDTO.fieldsToValidate[parentField].value
                                                        String expectedValue = expectedField.getValue();

                                                        // operator comes from your parsed operations array
                                                        String operator = operations[1];

                                                        String msgCSV = msgCSVPrefix + parentFieldCSV;
                                                        resultActions = "Check Value for " + parentFieldCSV;
                                                        ValidationResult vr =
                                                                evaluateOperation(actualValue, operator, expectedValue);

                                                        if (vr.valid) {
                                                            currentInstruction.setExecuted(true);
                                                            failedMessage = "";
                                                            success = true;

                                                            resultActions = performActions.buildValidationReason(
                                                                    vr.invalidReason,
                                                                    msgCSV,
                                                                    actualValue, // actual/current web value
                                                                    expectedValue,
                                                                    resultActions,
                                                                    operations,
                                                                    currentCondition,
                                                                    byPassNotFound,
                                                                    true,
                                                                    blockName,
                                                                    currentInstruction.getId(),
                                                                    true);

                                                            appendLog("[TEST]" + resultActions, "info");
                                                            alreadyLogged = true;

                                                            logOperations.info(
                                                                    msgCSVPrefix
                                                                            + " Validation SUCCESS for field '{}': actual='{}' {} expected='{}'",
                                                                    parentFieldCSV,
                                                                    actualValue,
                                                                    operator,
                                                                    expectedValue);

                                                        } else {
                                                            failedMessage = "Failed: Check Validation ";
                                                            msgInstruction =
                                                                    updateMSGInstruction(msgInstruction, failedMessage);

                                                            resultActions = performActions.buildValidationReason(
                                                                    vr.invalidReason,
                                                                    msgCSV,
                                                                    actualValue, // actual/current web value
                                                                    expectedValue,
                                                                    resultActions,
                                                                    operations,
                                                                    currentCondition,
                                                                    byPassNotFound,
                                                                    true,
                                                                    blockName,
                                                                    currentInstruction.getId(),
                                                                    false);

                                                            appendLog("[TEST]" + resultActions, "error");
                                                            alreadyLogged = true;

                                                            logOperations.error(
                                                                    msgCSVPrefix
                                                                            + " Values Validation FAILED for field '{}': actual='{}' {} expected='{}'. Reason: {}",
                                                                    msgCSV,
                                                                    actualValue,
                                                                    operator,
                                                                    expectedValue,
                                                                    resultActions);

                                                            success = false;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else if (execExcellWrite) {
                                    // Excel Write Operator

                                    if (parentField == null) {
                                        failedMessage = "Parent Id in Wrong Block ";
                                        msgInstruction = updateMSGInstruction(msgInstruction, failedMessage);
                                        resultActions = performActions.parentIdWrongBlock(
                                                currentInstruction, blockLoad, resultActions, currentCondition);

                                        success = false;

                                    } else if (!mapOperators.containsKey(variableField)) {
                                        failedMessage = "Get Value Is Not Defined ";
                                        msgInstruction = updateMSGInstruction(msgInstruction, failedMessage);
                                        //                                        resultActions =
                                        // performActions.getValueIsNotDefined(
                                        //                                                actions[0],
                                        //                                                currentInstruction,
                                        //                                                resultActions,
                                        //                                                ARExecution.ConditionStatus
                                        //                                                        .NONE, // NOT
                                        // currentCondition to Force Message,
                                        //                                                parentField,
                                        //                                                variableField);

                                        String reason = performActions.buildGetVariableReason(
                                                actions[0],
                                                currentInstruction,
                                                resultActions,
                                                currentCondition,
                                                parentField,
                                                variableField,
                                                byPassNotFound, // or your bypass flag
                                                blockName,
                                                currentInstruction.getId(),
                                                false);
                                        updateRowStatusAndNotify("red"); // #FF3131 deep carmine red
                                        appendLog("[TEST]" + reason, "error");
                                        alreadyLogged = true;

                                        logOperations.error("{}", reason);

                                        success = false;
                                    } else {

                                        if (excelExportOnceCreation) {
                                            //
                                            // writerExport.insertReportHead();
                                            excelExportOnceCreation = false;
                                        }

                                        if (!Strings.isNullOrEmpty(newExcelFieldName)) {
                                            writerExport = new ExcelWriter(
                                                            newExcelFieldName, performActions.getCurrentDriver(), true)
                                                    .withPurpose("export");

                                            // Only create Columns if Have a file to write
                                            String webData = mapOperators
                                                    .get(variableField)
                                                    .trim();
                                            webData = performActions.sanitizeValue(webData);

                                            currentTableCSV.put(
                                                    xExcelCurrentRow,
                                                    parentField.trim(),
                                                    webData); // may add new columns later too
                                        }

                                        resultActions = performActions.messageExcel(
                                                "Excel Write",
                                                currentInstruction,
                                                parentField,
                                                variableField,
                                                mapOperators.get(variableField),
                                                blockName,
                                                currentInstruction.getId(),
                                                (writerExport != null));

                                        if (currentTableCSV != null
                                                || currentTableCSV.getRows() == null
                                                || currentTableCSV.getRows().isEmpty()) {
                                            //
                                            // writerExport.insertBlockSeparation(blockLoad.getName());
                                            //                                            exportIndex *= 2;
                                        }

                                        // Insert the updated mapExport into the Excel after each instruction
                                        if (writerExport != null) {
                                            headersExport.add(parentField.trim());
                                        }

                                        performActions.onHoldForSeconds(null);

                                        if (resultActions != null && resultActions.contains("PASSED")) {
                                            currentInstruction.setExecuted(true);
                                            failedMessage = "";
                                            success = true;
                                        } else {
                                            failedMessage = "Failed: Generate File -> Excel/CSV ";
                                            msgInstruction = updateMSGInstruction(msgInstruction, failedMessage);
                                            updateRowStatusAndNotify("red"); // #FF3131 deep carmine red
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
                                    failedMessage = "Failed: General Execution ";
                                    msgInstruction = updateMSGInstruction(msgInstruction, failedMessage);
                                }
                                logOperations.error("Error: {} - {} - {} - {}", resultActions, msg1, msg2, msg3);
                                //                                performMessage.errorMessage(resultActions, msg1, msg2,
                                // msg3, null, 260);
                                //                            throw new RuntimeException(t);
                            }

                            if (success && !alreadyLogged) {
                                if (resultActions.contains("IGNORED")) {
                                    appendLog("[TEST]" + resultActions, "warn");
                                } else {
                                    appendLog("[TEST]" + resultActions, "info");
                                }
                            } else if (!alreadyLogged) {
                                appendLog("[TEST]" + resultActions, "error");
                                anyFailure = true;
                            }

                            alreadyLogged = false;

                            printLog(finalLogMessage(failedMessage, resultActions), success);

                            // Here mark the Status of a progress Condition Fail or Success at the end of each Kind
                            // of Execution
                            if (!jumpGotoError
                                    && !jumpLoopError
                                    && !currentCondition.equals(ARExecution.ConditionStatus.NONE)) {
                                progressCondition = performActions.updateProgressSuccess(success, currentCondition);
                                //                                continue instructionLoop;
                            } else {
                                progressCondition = ARExecution.ConditionStatus.NONE;
                            }

                            // Excel Report and Log
                            performActions.logAndReport(
                                    !byPassFlagLoop ? progressCondition : ARExecution.ConditionStatus.BY_PASS,
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

                            failedMessage = "";

                            if (pauseOperation && respModal.equals(ARExecution.DialogModal.STOP)) {

                                String nameInstruc =
                                        "(" + currentInstruction.getId() + ") " + currentInstruction.getName();

                                resultActions = String.format("STOP ALL PROCESSES: \"%s\"", nameInstruc);

                                FieldData msgBlock = new FieldData(resultActions, ARConstantsEngine.PAUSE);

                                // Excel Report and Log
                                performActions.logAndReport(
                                        currentCondition,
                                        true,
                                        true,
                                        blockStartTime,
                                        blockReportName,
                                        success,
                                        new String[] {ARConstantsEngine.PAUSE},
                                        msgBlock,
                                        dataExcel,
                                        writerReport,
                                        "PAUSE -> STOP",
                                        String.format("STOP ALL CALLED AT: \"%s\" : ", nameInstruc));

                                respModal = ARExecution.DialogModal.NONE;
                                stopAll = true;
                                break;
                            }

                            if (nextEnter) {

                                String nameInstruc =
                                        "(" + currentInstruction.getId() + ") " + currentInstruction.getName();

                                resultActions = String.format("Device : \"%s\"", nameInstruc);

                                FieldData msgBlock = new FieldData(resultActions, ARConstantsEngine.NEXT_ENTER);

                                // Excel Report and Log
                                performActions.logAndReport(
                                        currentCondition,
                                        true,
                                        true,
                                        blockStartTime,
                                        blockReportName,
                                        success,
                                        new String[] {ARConstantsEngine.NEXT_ENTER},
                                        msgBlock,
                                        dataExcel,
                                        writerReport,
                                        "DEVICE -> NEXT/ENTER",
                                        String.format("NEXT/ENTER CALLED AT: \"%s\" : ", nameInstruc));

                                respModal = ARExecution.DialogModal.NONE;
                                continue;
                            } else if (swipeUp) {

                                String nameInstruc =
                                        "(" + currentInstruction.getId() + ") " + currentInstruction.getName();

                                resultActions = String.format("Device : \"%s\"", nameInstruc);

                                FieldData msgBlock = new FieldData(resultActions, ARConstantsEngine.SWIPE_UP);

                                int timesSwipe = 1;

                                String operation = currentInstruction.getOperation();
                                if (operation != null && !operation.trim().isEmpty()) {
                                    try {
                                        timesSwipe = Integer.parseInt(operation.trim());
                                    } catch (NumberFormatException ignored) {
                                        appendLog("Invalid swipe count: " + operation + ". Defaulting to 1.", "warn");
                                    }
                                }

                                for (int i = 0; i < timesSwipe; i++) {
                                    //                                    androidDevice.swipeUp();
                                    // androidDevice.swipeVertical(true); // false = down
                                    //                                    androidDevice.swipeADB(splitDTO.getDeviceId(),
                                    // true);
                                }

                                // Excel Report and Log
                                performActions.logAndReport(
                                        currentCondition,
                                        true,
                                        true,
                                        blockStartTime,
                                        blockReportName,
                                        success,
                                        new String[] {ARConstantsEngine.SWIPE_UP},
                                        msgBlock,
                                        dataExcel,
                                        writerReport,
                                        "DEVICE -> SWIPE UP",
                                        String.format("SWIPE UP CALLED AT: \"%s\" : ", nameInstruc));

                                respModal = ARExecution.DialogModal.NONE;
                                continue;
                            } else if (swipeDown) {

                                String nameInstruc =
                                        "(" + currentInstruction.getId() + ") " + currentInstruction.getName();

                                resultActions = String.format("Device : \"%s\"", nameInstruc);

                                FieldData msgBlock = new FieldData(resultActions, ARConstantsEngine.SWIPE_DOWN);

                                int timesSwipe = 1;

                                String operation = currentInstruction.getOperation();
                                if (operation != null && !operation.trim().isEmpty()) {
                                    try {
                                        timesSwipe = Integer.parseInt(operation.trim());
                                    } catch (NumberFormatException ignored) {
                                        appendLog("Invalid swipe count: " + operation + ". Defaulting to 1.", "warn");
                                    }
                                }

                                for (int i = 0; i < timesSwipe; i++) {
                                    //                                    androidDevice.swipeDown();
                                    //                                    androidDevice.swipeVertical(false); // false =
                                    // down
                                    //                                    androidDevice.swipeADB(splitDTO.getDeviceId(),
                                    // false);
                                }
                                // Excel Report and Log
                                performActions.logAndReport(
                                        currentCondition,
                                        true,
                                        true,
                                        blockStartTime,
                                        blockReportName,
                                        success,
                                        new String[] {ARConstantsEngine.SWIPE_DOWN},
                                        msgBlock,
                                        dataExcel,
                                        writerReport,
                                        "DEVICE -> SWIPE DOWN",
                                        String.format("SWIPE DOWN CALLED AT: \"%s\" : ", nameInstruc));

                                respModal = ARExecution.DialogModal.NONE;
                                continue;
                            }

                            // It decides Here if ByPass as per Loop or Per IF-ELSEIF-ELSE-ENDIF blocks
                            // Does not block other executions if it fails for any reason and jumps to the beginning or
                            // Excel GOTO position block
                            if (!success
                                    && !byPassFlagLoop
                                    && currentCondition.equals(ARExecution.ConditionStatus.NONE)) {

                                // Record failure but do NOT alter execution flow
                                anyFailure = true;

                                // Reset success so execution can continue
                                success = true;

                                // Continue with next instruction
                                continue instructionLoop;
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
                            if (progressCondition.equals(ARExecution.ConditionStatus.IF_PASSED)
                                    || progressCondition.equals(ARExecution.ConditionStatus.ELSEIF_PASSED)) {
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
                                    currentCondition = ARExecution.ConditionStatus.NONE;
                                    progressCondition = ARExecution.ConditionStatus.NONE;
                                    continue instructionLoop;
                                }
                            }

                            // Conditions When Fails to any of then and Look for the next Correct Block
                            if (progressCondition.equals(ARExecution.ConditionStatus.IF_FAILED)
                                    || progressCondition.equals(ARExecution.ConditionStatus.ELSEIF_FAILED)) {

                                // Goes to the next ELSEIF IF EXIST (ELSEIF index + 1);
                                int index = performActions.searchMapConditional(
                                        mapConditional,
                                        parentBlockCondition,
                                        ARExecution.ConditionStatus.ELSEIF,
                                        currentIndex,
                                        false);

                                // Goes to the next ELSE IF ELSEIF  DOES NOT EXIST  (ELSE index + 1);
                                if (index < 0) {
                                    index = performActions.searchMapConditional(
                                            mapConditional,
                                            parentBlockCondition,
                                            ARExecution.ConditionStatus.ELSE,
                                            currentIndex,
                                            true);
                                }
                                if (index < 0) {
                                    stopAll = true;
                                    continue blockLoop;
                                }
                                currentIndex = index;
                                currentCondition = ARExecution.ConditionStatus.NONE;
                                progressCondition = ARExecution.ConditionStatus.NONE;
                                continue instructionLoop;

                            } else if (progressCondition.equals(ARExecution.ConditionStatus.ELSE_FAILED)) {
                                // Goes to the ENDIF (ENDIF index + 1);
                                int index = performActions.searchMapConditional(
                                        mapConditional,
                                        parentBlockCondition,
                                        ARExecution.ConditionStatus.ENDIF,
                                        currentIndex,
                                        true);

                                if (index < 0) {
                                    stopAll = true;
                                    continue blockLoop;
                                }
                                currentIndex = index;
                                currentCondition = ARExecution.ConditionStatus.NONE;
                                progressCondition = ARExecution.ConditionStatus.NONE;
                                continue instructionLoop;
                            }
                        }

                        // Has Transversed All Columns in the Block
                        // Way Out from the Current Excel Data Row to another Block keeping the Same Excel Data Row
                        break;
                    }
                    currentBlockOrder++;

                    currentTableCSV = csvTables.get(newExcelFieldName);

                    if (currentTableCSV != null
                            && currentTableCSV.getRows() != null
                            && !currentTableCSV.getRows().isEmpty()) {
                        saveExcelWrite(newExcelFieldName, currentTableCSV, writerExport, exportIndex);
                    }
                }

                currentBlockOrder = blockExcelGoto; // BLOCK DEFINED BY "DEFAULT" OR "EXCEL GOTO"
                xExcelCurrentRow++;
                currentTableCSV = csvTables.get(newExcelFieldName);
                if (currentTableCSV != null
                        && currentTableCSV.getRows() != null
                        && !currentTableCSV.getRows().isEmpty()) {
                    saveExcelWrite(newExcelFieldName, currentTableCSV, writerExport, exportIndex);
                }
            }
        }

        // Last Recall For to avoid Multirow Cycles
        // Iterate over all stored CsvTables
        for (Map.Entry<String, CsvTable> entry : csvTables.entrySet()) {

            currentTableCSV = entry.getValue();

            if (currentTableCSV != null
                    && currentTableCSV.getRows() != null
                    && !currentTableCSV.getRows().isEmpty()) {

                saveExcelWrite(currentTableCSV.getFullPath(), currentTableCSV, writerExport, exportIndex);
            }
        }

        totalExecutionTime = performActions.getTotalExecutionTime();

        if (totalExecutionTime == 0) {
            writerReport.insertTotalExecutionTimes(botJobStartTime, botJobStartTime);
        } else {
            writerReport.insertTotalExecutionTimes(botJobStartTime, System.nanoTime());
        }

        // PRINT END BASE LOG//
        if (success) {
            baseLogString = blocksLoaded.get(0).getName()
                    + ARConstantsEngine.FIELDS_SEPARATOR
                    + labelsValue.getProperty(Labels.END)
                    + ARConstantsEngine.FIELDS_SEPARATOR
                    + labelsValue.getProperty(Labels.OK);

            if (isInterceptBotJob()) {
                updateRowStatusAndNotify("yellow"); // #fcba03 deep carmine yellow
                performMessage.showCustomModalDialogDragWin11TimerAuto(
                        "Bot-Job Interrupted successfully",
                        currentBotJobName,
                        "Last Execution:",
                        resultActions,
                        "Browser will close in",
                        false,
                        "OK",
                        null,
                        300,
                        5);
            } else {
                updateRowStatusAndNotify("green"); // #1d9c06 deep carmine green
                respModal = performMessage.showCustomModalDialogDragWin11TimerAuto(
                        "Bot-Job Finished - successfully",
                        currentBotJobName,
                        "Last Execution:",
                        resultActions,
                        "Browser will close in",
                        false,
                        "OK",
                        "Close Browser",
                        300,
                        5);
            }

            performActions.setInterceptBotJob(false);
            setInterceptBotJob(false);
            isJobRunning.set(false);

        } else {
            baseLogString = blocksLoaded.get(0).getName()
                    + ARConstantsEngine.FIELDS_SEPARATOR
                    + labelsValue.getProperty(Labels.END)
                    + ARConstantsEngine.FIELDS_SEPARATOR
                    + labelsValue.getProperty(Labels.KO)
                    + ARConstantsEngine.FIELDS_SEPARATOR
                    + resultActions;

            if (isInterceptBotJob()) {
                updateRowStatusAndNotify("yellow"); // #fcba03 deep carmine yellow
                performMessage.showCustomModalDialogDragWin11TimerAuto(
                        "Bot-Job Interrupted successfully",
                        currentBotJobName,
                        "Last Execution:",
                        resultActions,
                        "Browser will close in",
                        false,
                        "OK",
                        null,
                        300,
                        5);

            } else {
                updateRowStatusAndNotify("red"); // #FF3131 deep carmine red
                if (webElementWork) {
                    respModal = performMessage.showCustomModalDialogDragWin11TimerAuto(
                            "Bot-Job Finished - successfully",
                            currentBotJobName,
                            "Last Execution:",
                            resultActions,
                            "Browser will close in",
                            false,
                            "OK",
                            "Close Browser",
                            300,
                            5);
                } else {
                    respModal = performMessage.showCustomModalDialogDragWin11TimerAuto(
                            "Process Execution Terminated",
                            !Strings.isNullOrEmpty(failedMessage) ? failedMessage : "Failed:",
                            "Last Execution:",
                            resultActions,
                            "Browser will close in",
                            true,
                            "OK",
                            "Close Browser",
                            350,
                            5);
                }
            }
        }

        logLaunch.info(baseLogString);

        if (resultActions.equalsIgnoreCase("Close Browser") || respModal.equals(ARExecution.DialogModal.STOP)) {
            currentARWebDriver.getCurrentDriver().quit();
        }

        shutDownExecutorService(executorServicePreLaunch);
        performActions.setInterceptBotJob(true);
        setInterceptBotJob(false);
        isJobRunning.set(false);
        return true;
    }

    private static class ValidationResult {
        final boolean valid;
        final String invalidReason; // null if none

        ValidationResult(boolean valid, String invalidReason) {
            this.valid = valid;
            this.invalidReason = invalidReason;
        }
    }

    private ValidationResult evaluateOperation(String actualRaw, String operator, String expectedRaw) {
        if (actualRaw == null || expectedRaw == null || operator == null) {
            return new ValidationResult(false, "Null values");
        }

        String actual = actualRaw.trim();
        String expected = expectedRaw.trim();

        switch (operator.trim()) {
            case "=":
                return new ValidationResult(actual.equalsIgnoreCase(expected), null);

            case "!=":
                return new ValidationResult(!actual.equalsIgnoreCase(expected), null);

            case ">": {
                int resp = handleGreaterThan(actual, expected);
                if (resp == 1) return new ValidationResult(true, null);
                if (resp == 0) return new ValidationResult(false, null);
                return new ValidationResult(false, "Invalid Numbers");
            }

            case "<": {
                int resp = handleLessThan(actual, expected);
                if (resp == 1) return new ValidationResult(true, null);
                if (resp == 0) return new ValidationResult(false, null);
                return new ValidationResult(false, "Invalid Numbers");
            }

            default:
                return new ValidationResult(false, "Unknown operator: " + operator);
        }
    }

    private void appendLog(String message, String style) {}

    public static InputInfo findMatchingInput(List<InputInfo> inputs, InstructionLoad currentInstruction) {
        if (inputs == null || inputs.isEmpty() || currentInstruction == null) {
            return null;
        }

        String instrName = normalize(currentInstruction.getName());
        String instrTag = normalize(currentInstruction.getTagName());

        for (InputInfo info : inputs) {
            if (info == null) continue;

            String inputName = normalize(info.name());
            String inputTag = normalize(info.tag());

            if (instrName.equalsIgnoreCase(inputName) && instrTag.equalsIgnoreCase(inputTag)) {
                return info;
            }
        }

        return null;
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim();
    }

    private WebElement immediateXPath(String xPath) {
        try {
            if (waitXPath == null && performActions.getCurrentDriver() != null) {
                waitXPath = new WebDriverWait(performActions.getCurrentDriver(), Duration.ofSeconds(0));
            }
            waitXPath.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xPath)));
            List<WebElement> foundElementList =
                    performActions.getCurrentDriver().findElements(By.xpath(xPath));
            if (foundElementList.size() > 0) {
                return foundElementList.get(0);
            }
        } catch (TimeoutException ignored) {
        } catch (Exception ignored) {
        }
        return null;
    }

    private void pushUpdateListElements() {
        if (performActions == null || performActions.getCurrentDriver() == null) return;

        int finalPort = portSocketInitial;
        String socketSessionId = "UPDATE_LIST_ELEMENTS";
        String destinationId = "perform-list-data";
        String[] dataArray = new String[] {"input", "textarea", "button", "a", "select", "label"};

        updateListElements(
                performActions.getCurrentDriver(),
                dataArray,
                finalPort,
                socketSessionId,
                destinationId,
                "searchTerms",
                this.currentBotJob.getHomeBankingId(),
                this.currentBotJob.getId());
    }

    public void updateListElements(
            WebDriver driver,
            String[] dataArray,
            int port,
            String sessionId,
            String destinationId,
            String operationId,
            int homeBankingId,
            int botJobId) {
        // "UPDATE_LIST_ELEMENTS", "perform-list-data", "searchTerms"
        ErrorMessage errorMessage = performListElements.dynamicLoadElementsDTO(
                driver,
                dataArray,
                searchHiddenFields,
                port,
                sessionId,
                destinationId,
                operationId,
                homeBankingId,
                botJobId);

        if (errorMessage != null) {
            logOperations.error(
                    "Error: Dynamic Pick One Clone ElementsDTO - {} - {} - {}",
                    errorMessage.getErrorTitle(),
                    errorMessage.getErrorHeader(),
                    errorMessage.getErrorMessage());
        }
    }

    private static boolean isWebElementInstruction(InstructionLoad instr) {
        if (instr == null) return false;

        String actions = instr.getActions();
        if (actions == null) return false;

        String raw = actions.trim();
        if (raw.isEmpty()) return false;

        // split() takes a regex, so quote the splitter to treat it literally
        String[] parts = raw.split(Pattern.quote(ARConstantsEngine.ACTION_SPECIFICATIONS_SPLITTER), 2);
        String first = parts[0].trim();
        if (first.isEmpty()) return false;

        String upper = first.toUpperCase(Locale.ROOT);

        // Required prefixes: "C" (including "C:"), "I:", "O:"
        if (upper.startsWith("C") || upper.startsWith("I") || upper.startsWith("O")) {
            return true;
        }

        // Optional: support plain operation tokens
        return upper.equals("SET") || upper.equals("GET");
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        return value.replace(".", "").replace(",", "");
    }

    private int getNavigationTimeSeconds() {
        String v = arPropertyManager.getProperty(ARPropertyEnum.NAVIGATION_TIME);
        try {
            int s = Integer.parseInt(v);
            if (s < 0) return 0;
            if (s > 10) return 10;
            return s;
        } catch (Exception ignore) {
            return 0;
        }
    }

    public void writeToFileCSV(String filename, String content) {
        try (Writer writer =
                new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.UTF_8))) {

            writer.write(content);
            logOperations.info("CSV written to file: {}", filename);

        } catch (IOException e) {
            logOperations.error("Error writing file: {}", e.getMessage(), e);
        }
    }

    /**
     * Generates the CSV content formatted according to the BancaStato specification.
     *
     * Format rules:
     * - The first line is the header and always starts with "KEY"
     *   followed by the configured delimiter and the ordered column names.
     *
     * - Each data row starts with:
     *     • "EXTERNAL"       if there is only one row
     *     • "EXTERNAL_n"     if multiple rows exist (1-based index)
     *
     * - Column values are written in the exact order defined by {@code tableCSV.columnsCSV}.
     *   Missing values are rendered as empty strings.
     *
     * - The configured end-of-file marker is appended at the end.
     *
     * Example (single row):
     *   KEY|User number
     *   EXTERNAL|434234
     *
     * Example (multiple rows):
     *   KEY|User number
     *   EXTERNAL_1|434234
     *   EXTERNAL_2|353534
     *
     * @param delimiter the column delimiter (e.g. "|")
     * @return the formatted CSV content as a String
     */
    public String getBancaStatoCsvContent(CsvTable tableCSV, String delimiter) {
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("KEY")
                .append(delimiter)
                .append(String.join(delimiter, tableCSV.getColumns()))
                .append("\n");

        int totalRows = (tableCSV == null || tableCSV.getRows() == null)
                ? 0
                : tableCSV.getRows().size();

        // Data rows
        for (int i = 0; i < totalRows; i++) {
            CsvRow row = tableCSV.getRows().get(i);

            String externalKey = (totalRows == 1) ? "EXTERNAL" : "EXTERNAL_" + (i + 1);

            sb.append(externalKey).append(delimiter);

            // values in the same order as columnsCSV
            for (int c = 0; c < tableCSV.getColumns().size(); c++) {
                String col = tableCSV.getColumns().get(c);
                sb.append(row != null ? row.get(col) : "");
                if (c < tableCSV.getColumns().size() - 1) {
                    sb.append(delimiter);
                }
            }

            sb.append("\n");
        }

        //        sb.append(END_OF_FILE_MARKER);
        return sb.toString();
    }

    private void saveExcelWrite(
            String newExcelFieldName, CsvTable tableCSV, ExcelWriter.ExcelChain writerExport, int exportIndex) {
        if (newExcelFieldName != null && newExcelFieldName.toLowerCase().endsWith(".csv")) {
            String delimiter = tableCSV.getDelimiter();
            if (Strings.isNullOrEmpty(tableCSV.getDelimiter())) {
                delimiter = ",";
            }

            String csvContent = getBancaStatoCsvContent(tableCSV, delimiter);
            logOperations.info(csvContent);
            if (csvContent != null) {
                writeToFileCSV(newExcelFieldName, csvContent);
            }
        } else if (newExcelFieldName != null && newExcelFieldName.toLowerCase().endsWith(".xlsx")) {
            //
            //                    writerExport.insertFieldNameAndValueLastColumn(mapExportRows, exportIndex -
            // 1);
            if (writerExport != null) {
                if (tableCSV != null) {
                    writerExport.insertCSVContentIntoExcel(tableCSV.getColumns(), tableCSV, exportIndex - 1);
                }
            }
        }
    }

    public void setCurrentColumns(List<String> columns) {
        currentColumnsCSV.clear();
        currentColumnsCSV.addAll(columns);
    }

    private void injectActionExecutor() {
        if (performActions == null || performActions.getCurrentDriver() == null) return;

        String sessionId = String.valueOf(this.currentBotJob.getHomeBankingId());
        String destination = "engine-perform-bot-job";

        ErrorMessage error = performActionExecutorLoad.injectActionExecutor(
                performActions.getCurrentDriver(),
                portSocketInitial,
                sessionId,
                destination,
                this.currentBotJob.getHomeBankingId(),
                this.currentBotJob.getId());

        if (error != null) {
            logOperations.warn(
                    "actionExecutor injection failed: {} - falling back to Selenium", error.getErrorMessage());
        } else {
            // Configure the client so performWebActions can use it
            actionExecutorClient.configure(this.currentBotJob.getHomeBankingId(), sessionId);
        }

        // Wire callbacks so the plugin is re-injected automatically:
        // 1. After refreshPage() - page reload kills the JS plugin
        performActions.setOnPageRefresh(this::injectActionExecutor);
        // 2. Before any action step - ensureActionExecutor() checks if alive, re-injects if not
        performActions.setActionExecutorInjector(this::injectActionExecutor);
    }
}
