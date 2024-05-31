package com.allinweb.ch.util;

import com.allinweb.ch.supportTypes.WebElementPlus;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

public class PageElements {

    // page = yMap
    private HashMap<Integer, HashMap<Integer, HashMap<Integer, HashMap<Integer, List<WebElementPlus>>>>> page;

    private int elementsCount = 0;

    public PageElements() {
        page = new HashMap<>();
    }

    public synchronized void addElement(WebElement element) {
        Rectangle rectangle = element.getRect();
        int y = rectangle.y;
        int yMax = y + rectangle.height;
        int x = rectangle.x;
        int xMax = x + rectangle.width;

        if (page.get(y) == null) {
            page.put(y, new HashMap());
        }
        if (page.get(y).get(yMax) == null) {
            page.get(y).put(yMax, new HashMap());
        }
        if (page.get(y).get(yMax).get(x) == null) {
            page.get(y).get(yMax).put(x, new HashMap<>());
        }
        if (page.get(y).get(yMax).get(x).get(xMax) == null) {
            page.get(y).get(yMax).get(x).put(xMax, new ArrayList<>());
        }

        elementsCount++;
        page.get(y).get(yMax).get(x).get(xMax).add(new WebElementPlus(element));
    }

    public int getElementsCount() {
        return elementsCount;
    }
}
