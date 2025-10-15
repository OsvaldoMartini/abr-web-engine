package com.allinweb.ch.model;

import lombok.Data;

@Data
public class UpdatedRow {
    private Integer blockId;
    private Integer blockOrderNumber;
    private Integer instructionId;
    private Integer instructionOrderNumber;
    private Integer parentBlockId;
    private Integer parentId;
}
