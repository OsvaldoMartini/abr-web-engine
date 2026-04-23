package com.allinweb.ch.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstructionLoad {
    private Integer homeBankingId;
    private Integer id;
    private Integer botJobId;
    private String botJobName;
    private Integer instructionOrderNumber;
    private String actions;
    private String name;
    private String xpath;
    private String coordinates;
    // Combinable post-input flags (any subset, any order). The engines split this
    // down to individual bits (via InputFlags) only at execution time.
    //   F = force coordinates (use elementFromPoint even when XPath matches)
    //   E = press ENTER after input
    //   T = press TAB after input
    //   N = press NEXT after input (mobile IME "Next"; cascades N-T-E when solo)
    //   S = scroll target into view before typing
    // Examples: "", "S", "FE", "FETN", "ETNFS".
    private String forceCoordinates;
    private String iFrameXPath;
    private String tagName;
    private String shadowHost;
    private String shadowRoot;
    private String cssSelector;
    private String description;
    private Boolean optional;
    private Boolean blockMarked;
    private String defaultValue;
    private Integer actionCustomMaxWaitSec;
    private Integer onHoldSeconds;
    private Boolean codified;
    private Boolean exportToABR;
    private Boolean executed;
    private String priority;
    private String operation;
    private String exportFile;
    private Integer parentId;
    private Integer blockId;
    private Integer blockOrderNumber;
    private String blockName;
    private Boolean blockActive;
    private Boolean instructionActive;
    private Integer blockWait;
    private Boolean editMode = false; // Add an editMode flag
    private Boolean refreshLoop;
    private Boolean loopOnly;
    private Integer variableId;
    private String type;
    private String sessionId;
    private Integer parentBlockId;

    private List<ReferenceLoadDTO> referenceLoadDTOList;

    // Custom constructor
    public InstructionLoad(
            Integer homeBankingId,
            Integer botJobId,
            String botJobName,
            Integer id,
            Integer instructionOrderNumber,
            String name,
            String description,
            Integer blockId,
            Integer blockOrderNumber,
            String blockName,
            Boolean blockActive,
            Boolean instructionActive,
            Integer blockWait,
            String actions,
            Integer parentBlockId,
            Integer parentId,
            Integer variableId,
            String operation,
            String defaultValue,
            String exportFile,
            String tagName,
            String forceCoordinates) {
        this.homeBankingId = homeBankingId;
        this.botJobId = botJobId;
        this.botJobName = botJobName;
        this.id = id;
        this.instructionOrderNumber = instructionOrderNumber;
        this.name = name;
        this.description = description;
        this.blockId = blockId;
        this.blockOrderNumber = blockOrderNumber;
        this.blockName = blockName;
        this.blockActive = blockActive;
        this.instructionActive = instructionActive;
        this.blockWait = blockWait;
        this.actions = actions;
        this.parentBlockId = parentBlockId;
        this.parentId = parentId;
        this.variableId = variableId;
        this.operation = operation;
        this.defaultValue = defaultValue;
        this.exportFile = exportFile;
        this.tagName = tagName;
        this.forceCoordinates = forceCoordinates;
    }
}
