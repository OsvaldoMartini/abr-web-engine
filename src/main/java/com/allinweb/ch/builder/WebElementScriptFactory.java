package com.allinweb.ch.builder;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebElement;

@Slf4j
public class WebElementScriptFactory {

    private WebElementScriptBuilder builder = new WebElementScriptBuilder();

    public WebElementScriptBuilder forElement(WebElement element) {
        return builder.addElement(element);
    }
}
