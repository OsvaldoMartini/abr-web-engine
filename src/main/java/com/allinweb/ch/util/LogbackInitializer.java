package com.allinweb.ch.util;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import java.io.InputStream;
import org.slf4j.LoggerFactory;

public class LogbackInitializer {

    public static void loadLogbackFromResources() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            context.reset(); // Clear previous config

            // Load logback.xml from resources
            InputStream configStream = LogbackInitializer.class.getClassLoader().getResourceAsStream("logback.xml");
            if (configStream == null) {
                throw new IllegalStateException("logback.xml not found in classpath");
            }

            configurator.doConfigure(configStream);

        } catch (JoranException e) {
            System.err.println("Logback configuration failed: " + e.getMessage());
        }
    }
}
