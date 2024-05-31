package com.allinweb.ch.supportTypes;

import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

public class WebElementPlus {

    private WebElement webElement;
    private int area;

    public WebElementPlus(WebElement element) {
        this.webElement = element;
        this.area = calculateArea();
    }

    private int calculateArea() {
        Rectangle rect = webElement.getRect();
        return rect.getWidth() * rect.getHeight();
    }
}
