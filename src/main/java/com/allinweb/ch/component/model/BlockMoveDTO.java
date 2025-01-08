package com.allinweb.ch.component.model;

import java.util.List;
import lombok.Data;

@Data
public class BlockMoveDTO {
    private String type;
    private List<BlockOrderDetailDTO> updatedBlocks;
}
