package com.allinweb.ch.component.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockLoopInstructionLoadDTO {
    private Integer id;
    private Integer botJobId;
    private String botJobName;
    private Integer instructionOrderNumber;
    private String actions;
    private String name;
    private String path;
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

    private List<ComplexInstructionLoadDTO> complexInstructionLoadDTOList;
    private List<InstructionReferenceLoadDTO> instructionReferenceLoadDTOList;

    // Custom constructor
    public BlockLoopInstructionLoadDTO(
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
            Integer parentId,
            String operation,
            String exportFile) {
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
        this.parentId = parentId;
        this.operation = operation;
        this.exportFile = exportFile;
    }
}
