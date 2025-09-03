package com.allinweb.ch.component.model;

import java.util.List;
import lombok.Data;

@Data
public class RowMoveDTO {
    private String type;
    private String sessionId;
    private String operationId;
    private Integer botJobId;
    private String botJobName;
    private Integer blockId;
    private Integer blockOrderNumber;
    private Integer deleteBlockId;
    private String blockName;
    private Boolean blockActive;
    private Boolean instructionActive;
    private Boolean isBetween;
    private Integer homeBankingId;
    private Integer parentBlockId;
    private List<InstructionLoad> updatedRows;
}
