package com.allinweb.ch.model;

import java.util.List;
import lombok.Data;

@Data
public class DeleteBlockDTO {
    private String type;
    private int blockId;
    private int botJobId;
    private Integer homeBankingId;
    private String sessionId;
    private List<BlockOrderDetailDTO> updatedBlocks;
}
