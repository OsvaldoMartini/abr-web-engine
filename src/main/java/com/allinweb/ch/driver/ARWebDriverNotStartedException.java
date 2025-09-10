package com.allinweb.ch.driver;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ARWebDriverNotStartedException extends RuntimeException {

    public ARWebDriverNotStartedException() {
        super("The web driver has not been started. Please start the web driver first.");
    }
}
