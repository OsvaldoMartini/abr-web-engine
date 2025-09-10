package com.allinweb.ch.socket;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NumberFormatter {

    public static String formatNumber(String numberString, String localFormat) {
        try {
            numberString = removeAllCurrencySymbols(numberString);

            String decimalPart = "";
            String integerPart = "";

            // Find last occurrence of "," or "." as decimal separator
            int decimalIndex = Math.max(numberString.lastIndexOf(','), numberString.lastIndexOf('.'));
            if (decimalIndex != -1) {
                decimalPart = numberString.substring(decimalIndex + 1);
                integerPart = numberString.substring(0, decimalIndex).replaceAll("[^0-9]", "");
            } else {
                integerPart = numberString.replaceAll("[^0-9]", "");
            }

            // Determine formatting style
            String groupingSeparator;
            String decimalSeparator;

            if ("US".equalsIgnoreCase(localFormat)) {
                groupingSeparator = ",";
                decimalSeparator = ".";
            } else if ("EU".equalsIgnoreCase(localFormat)) {
                groupingSeparator = ".";
                decimalSeparator = ",";
            } else { // Default
                groupingSeparator = ",";
                decimalSeparator = ".";
            }

            // Rebuild integer part with grouping
            String groupedInteger = insertGroupingSeparators(integerPart, groupingSeparator);

            return decimalPart.isEmpty() ? groupedInteger : groupedInteger + decimalSeparator + decimalPart;

        } catch (Exception e) {
            log.error("Error formatting number: " + numberString + " - " + e.getMessage());
            return numberString;
        }
    }

    private static String insertGroupingSeparators(String number, String separator) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = number.length() - 1; i >= 0; i--) {
            sb.insert(0, number.charAt(i));
            count++;
            if (count % 3 == 0 && i != 0) {
                sb.insert(0, separator);
            }
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        // Example usage with the provided numbers and local formats
        String[] numbers = {
            "198,51",
            "1.056,45",
            "3.345.556,35",
            "198.51",
            "1000",
            "1,000,000.55",
            "1000000,55",
            "197,66 $",
            "85.225,66 $",
            "1234,56"
        };
        String[] formats = {"US", "EU"};

        for (String number : numbers) {
            for (String format : formats) {
                String formattedNumber = formatNumber(number, format);
                log.info("Number: " + number + ", Format: " + format + " -> Formatted: " + formattedNumber);
            }
        }
    }

    /**
     * Removes all characters that are not numbers or the decimal separator.
     *
     * @param input The string to clean.
     * @return A cleaned version of the string.
     */
    private static String removeAllCurrencySymbols(String input) {
        // Remove all non-numeric and non-decimal characters (e.g., $, €, etc.)
        return input.replaceAll("[^0-9.,]", "");
    }
}
