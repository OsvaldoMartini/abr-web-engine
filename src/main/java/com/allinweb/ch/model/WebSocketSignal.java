package com.allinweb.ch.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WebSocketSignal {
    private String operationId; // e.g. "activate-insert-all"
    private String sessionId; // so the frontend knows if it applies
    private String message; // optional, e.g. "Insert All re-enabled"
    private SplitDTO splitDTO; // optional, e.g. "Insert All re-enabled"
}
