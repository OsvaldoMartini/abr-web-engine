package com.allinweb.ch.builder;

import org.openqa.selenium.WebElement;

public class WebElementScriptFactory {

    private WebElementScriptBuilder builder = new WebElementScriptBuilder();

    public WebElementScriptBuilder forElement(WebElement element) {
        return builder.addElement(element);
    }
}
