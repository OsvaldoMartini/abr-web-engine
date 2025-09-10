package com.allinweb.ch.util;

import com.google.common.base.Strings;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import javax.swing.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ARPriorities {

    private static final ARPropertyManager arPropertyManager;
    public static Properties properties;
    public static List<Priority> priorityList;
    public static List<SearchConfig> searchList;
    // Static final variable to hold the singleton instance
    protected static volatile ARPriorities instance;
    private static String searchConfigTemplate =
            "#numero priorità, categoria, identificativo\n" + "1,ByXPath,//a[@href],a[href]\n"
                    + "2,ByLabels,label,spam,div,p\n"
                    + "3,attribute,martini-id";

    static {
        arPropertyManager = ARPropertyManager.getInstance();
    }

    private Integer jobId;

    // Private constructor to prevent instantiation
    private ARPriorities() {}

    // Public method to access the singleton instance
    public static ARPriorities getInstance() {
        if (instance == null) {
            synchronized (ARPriorities.class) {
                if (instance == null) {
                    instance = new ARPriorities();
                }
            }
        }
        return instance;
    }

    // Public method to access the singleton instance
    public static void destroyInstance() {
        instance = null;
    }

    public static List<Priority> getAllPriorityList() {
        return priorityList;
    }

    public static List<SearchConfig> getSearchConfigList() {
        return searchList;
    }

    public static void loadPrioritiesFromString(String text) {
        List<Priority> priorities = new ArrayList<>();

        // Split the text into lines
        String[] lines = text.split("\\r?\\n");

        // Process each line
        for (String line : lines) {
            String[] parts = line.split(",");
            if (isFirstCharacterHash(parts[0])) {
                continue;
            }
            if (parts.length >= 3) {
                // Extract values from parts
                int priorityNumber = Integer.parseInt(parts[0]);
                String priorityType = parts[1];
                List<String> name = Arrays.stream(Arrays.copyOfRange(parts, 2, parts.length))
                        .toList();

                // Create and add Priority object
                priorities.add(new Priority(priorityNumber, priorityType, name));
            } else {
                // Handle invalid lines
                log.error("Invalid line: " + line);
            }
        }
        priorityList = priorities;

        priorityList.sort(Comparator.comparingInt(Priority::getPriorityNumber));
    }

    public static void loadSearchElementsConfig(String text) {
        List<SearchConfig> searchConfigs = new ArrayList<>();

        if (Strings.isNullOrEmpty(text)) {
            text = searchConfigTemplate;
        }

        // Split the text into lines
        String[] lines = text.split("\\r?\\n");

        // Process each line
        for (String line : lines) {
            String[] parts = line.split(",");
            if (isFirstCharacterHash(parts[0])) {
                continue;
            }
            if (parts.length >= 3) {
                // Extract values from parts
                int searchNumber = Integer.parseInt(parts[0]);
                String searchType = parts[1];
                List<String> name = Arrays.stream(Arrays.copyOfRange(parts, 2, parts.length))
                        .toList();

                // Create and add Priority object
                searchConfigs.add(new SearchConfig(searchNumber, searchType, name));
            } else {
                // Handle invalid lines
                log.error("Invalid line: " + line);
            }
        }
        searchList = searchConfigs;

        searchList.sort(Comparator.comparingInt(SearchConfig::getSearchNumber));
    }

    public static boolean isFirstCharacterHash(String str) {
        return str != null && str.startsWith("#");
    }

    public void loadPriorities() {
        String priorityPath = arPropertyManager.getProperty(ARPropertyEnum.PATH_PRIORITY);
        if (priorityPath == null || priorityPath.isBlank()) {
            JOptionPane.showMessageDialog(
                    null,
                    "Priority configuration folder is not set. Please set the folder of priority configuration file "
                            + ARConstantsEngine.FILE_NAME_PRIORITIES,
                    "Priority configuration folder not set",
                    JOptionPane.WARNING_MESSAGE);
            log.warn("Priority configuration folder not set");
            throw new RuntimeException("Priority configuration not set");
        }
        String prioritiesFileName = priorityPath + ARConstantsEngine.FILE_NAME_PRIORITIES;
        priorityList = new ArrayList<>();
        File prioritiesFile = new File(prioritiesFileName);
        if (!prioritiesFile.exists()) {
            JOptionPane.showMessageDialog(
                    null,
                    "Priority configuration file is missing. Please check that the priority configuration is "
                            + "set correctly or create the file: " + prioritiesFileName,
                    "Priority configuration file missing",
                    JOptionPane.WARNING_MESSAGE);

            log.warn("Priority configuration file missing" + prioritiesFileName);
            throw new RuntimeException("Priority configuration file missing");
        }
        try (FileInputStream priorities = new FileInputStream(prioritiesFile)) {
            properties = new Properties();
            properties.load(priorities);
            if (properties.size() == 0) {
                log.warn("The file " + prioritiesFileName + "is empty");
            }
            properties.keySet().forEach(keyObj -> {
                String[] params = String.valueOf(keyObj).split(ARConstantsEngine.FIELDS_SEPARATOR);
                Priority priority = new Priority(
                        Integer.parseInt(params[0]),
                        params[1],
                        Arrays.stream(Arrays.copyOfRange(params, 2, params.length))
                                .toList());
                priorityList.add(priority);
            });
            priorityList.sort(Comparator.comparingInt(Priority::getPriorityNumber));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Integer getJobId() {
        return jobId;
    }

    public void setJobId(Integer jobId) {
        this.jobId = jobId;
    }
}
