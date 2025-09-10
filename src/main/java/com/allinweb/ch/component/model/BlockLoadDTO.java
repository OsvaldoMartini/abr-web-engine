package com.allinweb.ch.component.model;

import java.util.List;
import lombok.Data;

@Data
public class BlockLoadDTO {
    private String homeBankingName;
    private Integer homeBankingId;
    private Integer id;
    private Integer blockOrderNumber;
    private String name;
    private String description;
    private Integer typeId;
    private Integer botJobId;
    private String botJobName;
    private String exportFile;
    private Boolean active;
    private Integer wait;
    private boolean hasAnyInput;

    private List<InstructionLoad> instructionLoad;
}
