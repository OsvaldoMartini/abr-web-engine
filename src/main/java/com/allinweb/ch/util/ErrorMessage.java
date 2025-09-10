package com.allinweb.ch.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class ErrorMessage {
    private String errorTitle = "";
    private String errorHeader = "";
    private String errorMessage = "";
}
