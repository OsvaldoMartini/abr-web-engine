package com.allinweb.ch.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.slf4j.Logger;

public class LoggingOutputStream extends ByteArrayOutputStream {
    private final Logger log;
    private final boolean isError;

    public LoggingOutputStream(Logger log, boolean isError) {
        this.log = log;
        this.isError = isError;
    }

    @Override
    public void flush() throws IOException {
        String contents = this.toString("UTF-8").trim();
        super.reset();
        if (!contents.isEmpty()) {
            if (isError) {
                log.error(contents);
            } else {
                log.info(contents);
            }
        }
    }
}
