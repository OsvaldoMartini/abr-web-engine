package com.allinweb.ch.dto;

import java.util.List;
import lombok.Data;

@Data
public class BlockLoadDTO {
    private int id;
    private int blockOrderNumber;
    private String name;
    private String description;
    private int typeId;
    private int botJobId;
    private String botJobName;
    private String exportFile;

    private List<BlockLoopInstructionLoadDTO> blockLoopInstructionLoadDTOS;
}
