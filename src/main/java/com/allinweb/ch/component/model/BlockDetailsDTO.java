package com.allinweb.ch.component.model;

import java.util.List;
import lombok.Data;

@Data
public class BlockDetailsDTO {
    private Integer homeBankingId;
    private Integer blockId;
    private String blockName;
    private String blockDescription;
    private Integer typeId;
    private Boolean active;
    private Integer wait;
    private Integer blockOrderNumber;
    private Integer botJobId;
    private String botJobName;
    private Boolean forceOrder;
    private String exportFile;
    private String sessionId;
    private List<UpdatedRow> updatedInstructions; // For originalBlock
    private List<UpdatedRow> instructions; // For newBlock
}
