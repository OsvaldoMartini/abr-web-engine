package com.allinweb.ch.util;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorMessage {
    private String errorTitle;
    private String errorHeader;
    private String errorMessage;
}
