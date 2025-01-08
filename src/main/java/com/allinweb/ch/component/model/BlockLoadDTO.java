package com.allinweb.ch.component.model;

import java.util.List;
import lombok.Data;

@Data
public class BlockLoadDTO {
    private Integer id;
    private Integer blockOrderNumber;
    private String name;
    private String description;
    private Integer typeId;
    private Integer botJobId;
    private String botJobName;
    private String exportFile;
    private boolean active;
    private Integer wait;

    private List<BlockLoopInstructionLoadDTO> blockLoopInstructionLoadDTOS;
}
