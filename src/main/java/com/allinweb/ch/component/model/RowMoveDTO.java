package com.allinweb.ch.component.model;

import java.util.List;
import lombok.Data;

@Data
public class RowMoveDTO {
    private String type;
    private int botJobId;
    private String botJobName;
    private int blockId;
    private int deleteBlockId;
    private String blockName;
    private boolean blockActive;
    private boolean instructionActive;
    private boolean isBetween;
    private List<InstructionDTO> updatedRows;
}
