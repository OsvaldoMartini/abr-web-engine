package com.allinweb.ch.component.model;

import java.util.List;
import lombok.Data;

@Data
public class BlockOrderDTO {
    private String type;
    private Integer botJobId;
    private String sessionId;
    private Integer homeBankingId;
    private List<BlockOrderDetailDTO> updatedBlocks;

    // Constructors
    public BlockOrderDTO() {}

    public BlockOrderDTO(String type, List<BlockOrderDetailDTO> updatedBlocks) {
        this.type = type;
        this.updatedBlocks = updatedBlocks;
    }
}
