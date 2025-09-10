package com.allinweb.ch.util;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

@Slf4j
public class ARWebUtil {

    public static String extractXPath(String input) {
        String marker = "-> xpath: ";
        int markerIndex = input.indexOf(marker);

        if (markerIndex != -1) {
            // Extract the substring starting from the marker and trim any extra spaces
            String extractedXPath =
                    input.substring(markerIndex + marker.length()).trim();

            // Replace double ']]' with single ']'
            if (extractedXPath.contains("]]")) {
                extractedXPath = extractedXPath.replace("]]", "]");
            }
            // Replace double ']]' with single ']'
            if (extractedXPath.contains("[[")) {
                extractedXPath = extractedXPath.replace("[[", "[");
            }

            return extractedXPath;
        } else {
            return null; // or throw an exception if preferred
        }
    }

    public static String extractWebElementXPath(WebElement element) {
        return generateXPath(element, "");
    }

    private static String generateXPath(WebElement element, String current) {
        String tag = element.getTagName();
        if (tag.equals("html")) {
            return "/html" + current;
        }
        WebElement parentElement = element.findElement(By.xpath(".."));
        int count = 0;
        int index = 1;
        List<WebElement> children = parentElement.findElements(By.xpath("*"));
        for (WebElement child : children) {
            String childTag = child.getTagName();
            if (childTag.equals(tag)) {
                if (child.equals(element)) {
                    index = count + 1;
                }
                count++;
            }
        }
        return generateXPath(parentElement, "/" + tag + "[" + index + "]" + current);
    }
}
