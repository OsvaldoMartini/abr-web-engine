package com.allinweb.ch.util;

import com.allinweb.ch.model.InstructionLoad;
import com.allinweb.ch.model.TargetElement;
import com.google.common.base.Strings;
import java.util.List;

public final class InstructionLoadMatcher {

    private InstructionLoadMatcher() {
        // utility class
    }

    public static TargetElement findMatchingTargetElementByXPath(
            List<TargetElement> currentElements, InstructionLoad currentInstruction) {

        if (currentElements == null || currentElements.isEmpty() || currentInstruction == null) {
            return null;
        }

        String targetXPath = normalize(currentInstruction.getXpath());
        if (isBlank(targetXPath)) {
            return null;
        }

        boolean isShadowInstruction = !Strings.isNullOrEmpty(currentInstruction.getShadowHost())
                && !Strings.isNullOrEmpty(currentInstruction.getCssSelector());

        // Only needed if shadow instruction
        String targetCssSelector = isShadowInstruction ? normalize(currentInstruction.getCssSelector()) : null;

        for (TargetElement el : currentElements) {
            if (el == null) continue;

            // 1) XPath must always match
            if (!equalsIgnoreIgnoreBlank(targetXPath, el.getXPath())) {
                continue;
            }

            // 2) If shadow instruction, also require CSS selector match
            if (isShadowInstruction) {
                if (equalsIgnoreIgnoreBlank(targetCssSelector, el.getCssSelector())) {
                    return el;
                }
                continue;
            }

            // Non-shadow: XPath match is enough
            return el;
        }

        return null;
    }

    /** ✅ Overload: accept List<TargetElement> */
    public static TargetElement findMatchingTargetElement(
            List<TargetElement> currentElements, InstructionLoad currentInstruction) {
        if (currentElements == null || currentElements.isEmpty() || currentInstruction == null) {
            return null;
        }

        String targetTag = normalize(currentInstruction.getTagName());
        String targetName = normalize(currentInstruction.getName());

        // Nothing meaningful to match
        if (isBlank(targetTag) && isBlank(targetName)) {
            return null;
        }

        for (TargetElement el : currentElements) {
            if (el == null) continue;

            if (equalsIgnoreIgnoreBlank(targetTag, el.getTagName())
                    && equalsIgnoreIgnoreBlank(targetName, el.getDefinedName())) {
                return el;
            }
        }

        return null;
    }

    /**
     * true if:
     * - expected is blank → ignore constraint
     * - otherwise equalsIgnoreCase after trim
     */
    private static boolean equalsIgnoreIgnoreBlank(String expected, String actual) {
        if (isBlank(expected)) return true;
        if (isBlank(actual)) return false;
        return expected.equalsIgnoreCase(normalize(actual));
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
