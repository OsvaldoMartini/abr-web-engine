package com.allinweb.ch.model;

import java.util.List;
import lombok.Data;

@Data
public class BlockOrderDTO {
    private String type;
    private Integer botJobId;
    private String sessionId;
    private Integer homeBankingId;
    private List<BlockOrderDetailDTO> updatedBlocks;
}
