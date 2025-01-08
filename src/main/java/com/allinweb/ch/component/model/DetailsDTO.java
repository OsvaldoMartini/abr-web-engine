package com.allinweb.ch.component.model;

import java.util.List;
import lombok.Data;

@Data
public class DetailsDTO {
    private BlockDetailsDTO originalBlock;
    private BlockDetailsDTO newBlock;
    private List<BlockOrderDetailDTO> updatedBlocks;
}
