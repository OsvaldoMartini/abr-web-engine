package com.allinweb.ch.util;

import java.util.Random;
import org.openqa.selenium.WebElement;

public class UtilsMethods {

    public static String generateRandomID(int length) {
        String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(AB.charAt(rnd.nextInt(AB.length())));
        }
        return sb.toString();
    }

    public static void exceptionIfNullWebElement(WebElement element) throws Exception {
        if (element == null) {
            throw new Exception("null web element");
        }
    }

    public static boolean testFixedCheck(String valueToCheck) {
        String[] splittedValue = valueToCheck.split(Constants.RND_ID_SEPARATOR);
        if (splittedValue.length != 2) {
            return false;
        }
        String regex = "[0-9A-Za-z]{6}";
        return splittedValue[1].matches(regex);
    }

    public static String[] splitIfContains(String input, String specificCharStr) {
        // Check if the input contains the specific character
        if (input.contains(specificCharStr)) {
            // Split the input using the specific character
            return input.split(specificCharStr);
        } else {
            // Return the original string as a single-element array
            return new String[] {input};
        }
    }
}
