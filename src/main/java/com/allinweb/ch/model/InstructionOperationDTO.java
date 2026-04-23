package com.allinweb.ch.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstructionOperationDTO {
    private Integer id;
    private Integer blockId;
    private Integer botJobId;
    private Integer homeBankingId;
    private String name;
    private String description;
    private String actions;
    private String operation;
    private Integer instructionOrderNumber;
    private Integer actionCustomMaxWaitSec;
    private Integer onHoldSeconds;
    private Integer variableId;
    private Integer parentBlockId;
    private Integer parentId;
    private Boolean instructionActive;
    private Boolean blockActive;
    private Boolean refreshLoop;
    private Boolean loopOnly;
    // Combinable post-input flags (any subset, any order). The engines split this
    // down to individual bits (via InputFlags) only at execution time.
    //   F = force coordinates (use elementFromPoint even when XPath matches)
    //   E = press ENTER after input
    //   T = press TAB after input
    //   N = press NEXT after input (mobile IME "Next"; cascades N-T-E when solo)
    //   S = scroll target into view before typing
    // Examples: "", "S", "FE", "FETN", "ETNFS".
    private String forceCoordinates;
}
