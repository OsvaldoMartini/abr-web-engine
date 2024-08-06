package com.allinweb.ch.driver;

public class ABRWebDriverNotStartedException extends RuntimeException {

    public ABRWebDriverNotStartedException() {
        super("The web driver has not been started. Please start the web driver first.");
    }
}
