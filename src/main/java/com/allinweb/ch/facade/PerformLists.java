package com.allinweb.ch.facade;

import com.allinweb.ch.component.model.*;
import com.allinweb.ch.persistence.DatabaseUserDTO;
import com.allinweb.ch.persistence.ReferenceDTO;
import com.allinweb.ch.util.ComboBoxVars;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerformLists {

    // Static final variable to hold the singleton instance
    protected static volatile PerformLists instance;

    // Private constructor to prevent instantiation
    private PerformLists() {
        // Initialize if necessary
    }

    // Public method to access the singleton instance
    public static PerformLists getInstance() {
        if (instance == null) {
            synchronized (PerformLists.class) {
                if (instance == null) {
                    instance = new PerformLists();
                }
            }
        }
        return instance;
    }

    // Lists for tables
    private ObservableList<HomeBankingLoadDTO> listHomeBanking = FXCollections.observableArrayList();
    private ObservableList<HomeUrlDTO> listHomeUrl = FXCollections.observableArrayList();
    private ObservableList<BotJobLoadDTO> quickBotJobs = FXCollections.observableArrayList();
    private ObservableList<BotJobLoadDTO> listBotJob = FXCollections.observableArrayList();
    private List<BotJobLoadDTO> listBotJobComp = new ArrayList<>();
    private List<BlockLoadDTO> listBlock = new ArrayList<>();
    private List<BlockLoadDTO> listBlockComp = new ArrayList<>();
    private List<InstructionLoadDTO> listInstruction = new ArrayList<>();
    private List<InstructionLoadDTO> listInstructionComp = new ArrayList<>();
    private List<VariableLoadDTO> listVariable = new ArrayList<>();
    private List<VariableLoadDTO> listVariableComp = new ArrayList<>();
    private List<ReferenceDTO> listReference = new ArrayList<>();
    private List<ReferenceDTO> listReferenceComp = new ArrayList<>();

    // Quick Lists
    private List<InstructionOperationDTO> instrucOperList = new ArrayList<>();

    // Observable lists
    private ObservableList<DatabaseUserDTO> listDatabaseUsers = FXCollections.observableArrayList();
    private ObservableList<VariableUserDTO> listVariablesUser = FXCollections.observableArrayList();
    private ObservableList<ComboBoxVars> listWebPageItems = FXCollections.observableArrayList();

    public List<HomeUrlDTO> getHomeUrlsByBankId(Integer homeBankingId) {
        return getListHomeUrl().stream()
                .filter(dto ->
                        dto.getHomeBankingId() != null && dto.getHomeBankingId().equals(homeBankingId))
                .toList(); // Java 16+; use .collect(Collectors.toList()) for older versions
    }

    // Get HomeBankingLoadDTO by homeBankingId
    public HomeBankingLoadDTO getHomeBankingById(Integer homeBankingId) {
        return getListHomeBanking().stream()
                .filter(hb -> Objects.equals(hb.getId(), homeBankingId))
                .findFirst()
                .orElse(null); // null if not found
    }

    // Get the first HomeBankingLoadDTO from the list
    public HomeBankingLoadDTO getFirstHomeBanking() {
        return getListHomeBanking().stream().findFirst().orElse(null); // null if the list is empty
    }

    // Get HomeUrlDTO by homeBankingId and homeUrlId
    public HomeUrlDTO getHomeUrlByBankId(Integer homeBankingId, Integer homeUrlId) {
        return getListHomeUrl().stream()
                .filter(url ->
                        Objects.equals(url.getHomeBankingId(), homeBankingId) && Objects.equals(url.getId(), homeUrlId))
                .findFirst()
                .orElse(null); // null if not found
    }

    // Get BotJobLoadDTO by botJobId
    public BotJobLoadDTO getQuickBotJobById(Integer botJobId) {
        return getQuickBotJobs().stream()
                .filter(job -> Objects.equals(job.getId(), botJobId))
                .findFirst()
                .orElse(null); // null if not found
    }
}
