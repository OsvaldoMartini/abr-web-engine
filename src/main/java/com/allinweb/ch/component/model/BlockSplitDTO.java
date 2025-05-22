package com.allinweb.ch.component.model;

import lombok.Data;

@Data
public class BlockSplitDTO {
    private String type;
    private Integer homeBankingId;
    private Integer botJobId;
    private String botJobName;
    private String sessionId;
    private DetailsDTO details;
}
