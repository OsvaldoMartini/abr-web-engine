package com.allinweb.ch.component.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BlockOrderDetailDTO {
    private Integer homeBankId;
    private Integer blockId;
    private Integer botJobId;
    private Integer blockOrderNumber;
    private String blockName;
}
