package com.allinweb.ch.model;

import java.util.List;
import lombok.Data;

@Data
public class RollBackBlocksDTO {
    private String type;
    private Integer botJobId;
    private Integer blockId;
    private String blockName;
    private Integer homeBankingId;
    private String sessionId;
    private List<InstructionLoad> instructions;
}
