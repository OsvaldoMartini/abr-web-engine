package com.allinweb.ch.util;

public class ARExecution {
    public enum ConditionStatus {
        NONE, // No active condition
        IF_PASSED, // IF condition was met
        IF_FAILED, // IF condition failed
        ELSEIF_PASSED, // ELSEIF condition was met
        ELSEIF_FAILED, // ELSEIF condition failed
        ELSE_PASSED,
        ELSE_FAILED,
        IF, // ELSE block is active
        ELSEIF,
        ELSE, // ELSE block is active
        ENDIF,
        BY_PASS
    }

    public enum DialogModal {
        NONE,
        OK,
        STOP,
        EXIT
    }
}
