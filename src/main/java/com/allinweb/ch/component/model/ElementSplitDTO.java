package com.allinweb.ch.component.model;

import lombok.Data;

@Data
public class ElementSplitDTO {
    private String type;
    private String operationId;
    private Integer botJobId;
    private Integer homeBankingId;
    private String sessionId;
    private ElementDTO[] details;
}
