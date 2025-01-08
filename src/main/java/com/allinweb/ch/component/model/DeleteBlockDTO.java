package com.allinweb.ch.component.model;

import java.util.List;
import lombok.Data;

@Data
public class DeleteBlockDTO {
    private String type;
    private int blockId;
    private int botJobId;
    private List<BlockOrderDetailDTO> updatedBlocks;
}
