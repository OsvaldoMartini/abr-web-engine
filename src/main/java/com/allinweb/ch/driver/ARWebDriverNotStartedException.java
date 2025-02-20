package com.allinweb.ch.driver;

public class ARWebDriverNotStartedException extends RuntimeException {

    public ARWebDriverNotStartedException() {
        super("The web driver has not been started. Please start the web driver first.");
    }
}
