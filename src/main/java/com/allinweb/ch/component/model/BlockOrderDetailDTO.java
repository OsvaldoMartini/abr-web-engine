package com.allinweb.ch.component.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BlockOrderDetailDTO {
    private int blockId;
    private int botJobId;
    private int blockOrderNumber;
    private String blockName;
}
