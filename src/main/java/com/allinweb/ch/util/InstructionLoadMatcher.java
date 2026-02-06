package com.allinweb.ch.util;

import com.allinweb.ch.model.AttributeData;
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

        String targetName = normalize(currentInstruction.getName());
        String tagName = normalize(currentInstruction.getTagName());
        String action = normalize(currentInstruction.getActions());

        // Nothing meaningful to match
        if (isBlank(targetName)) {
            return null;
        }

        // 1) Action-aware primary match (definedName + allowed tagName by action)
        for (TargetElement el : currentElements) {
            if (el == null) continue;

            if (!equalsIgnoreIgnoreBlank(targetName, el.getDefinedName())) {
                continue;
            }

            String elTag = normalize(el.getTagName());

            // Apply tagName rules only for the first match
            if (isAllowedTagForAction(action, tagName, elTag)) {
                return el;
            }
        }

        // Only By Defined Name
        for (TargetElement el : currentElements) {
            if (el == null) continue;

            if (equalsIgnoreIgnoreBlank(targetName, el.getDefinedName())) {
                return el;
            }
        }

        // Searches as Field "someText"
        for (TargetElement el : currentElements) {
            if (el == null) continue;

            if (equalsIgnoreIgnoreBlank(targetName, el.getSomeText())) {
                return el;
            }
        }

        // Searches  someText in AttributeData
        for (TargetElement el : currentElements) {
            if (el == null) continue;

            if (hasSomeTextAttribute(el, targetName)) {
                return el;
            }
        }

        // Searches  At Least Parts of someText in AttributeData
        for (TargetElement el : currentElements) {
            if (el == null) continue;

            if (hasSomeTextContainsAttribute(el, targetName)) {
                return el;
            }
        }

        return null;
    }

    private static boolean hasSomeTextAttribute(TargetElement el, String targetName) {
        if (el == null || isBlank(targetName)) return false;

        AttributeData[] attrs = el.getAttributeData();
        if (attrs == null || attrs.length == 0) return false;

        String target = normalize(targetName);

        for (AttributeData ad : attrs) {
            if (ad == null) continue;

            if ("someText".equalsIgnoreCase(normalize(ad.getName()))
                    && equalsIgnoreIgnoreBlank(target, ad.getValue())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasSomeTextContainsAttribute(TargetElement el, String targetName) {
        if (el == null || isBlank(targetName)) return false;

        AttributeData[] attrs = el.getAttributeData();
        if (attrs == null || attrs.length == 0) return false;

        String target = normalize(targetName);

        for (AttributeData ad : attrs) {
            if (ad == null) continue;

            if ("sometext".equalsIgnoreCase(normalize(ad.getName())) && containsIgnoreCase(ad.getValue(), target)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsIgnoreCase(String source, String part) {
        if (isBlank(source) || isBlank(part)) return false;
        return normalize(source).contains(normalize(part));
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

    private static boolean isAllowedTagForAction(String action, String instructionTagName, String elementTagName) {
        String a = normalize(action);
        String instrTag = normalize(instructionTagName);
        String elTag = normalize(elementTagName);

        // If you have an instruction tagName and want it enforced as part of matching:
        // You can require either exact tag match OR membership in the allowed set.
        // (Here we prioritize the allowed-set behavior per your rules.)
        if (isBlank(a)) return true; // if action not known, don't restrict

        if (a.startsWith("I:")) {
            return isInputTag(elTag) && (isBlank(instrTag) || elTag.equals(instrTag));
        }

        if (a.startsWith("O:")) {
            return isOutputTag(elTag) && (isBlank(instrTag) || elTag.equals(instrTag));
        }

        // Click action: "C" or "C:" (you mentioned "C")
        if (a.equals("C")) {
            return isClickTag(elTag) && (isBlank(instrTag) || elTag.equals(instrTag));
        }

        // Any other action -> no restriction (or tighten if you want)
        return true;
    }

    private static boolean isInputTag(String tag) {
        // INPUT sequence: input/select/textarea (add others if needed)
        return "input".equalsIgnoreCase(tag) || "select".equalsIgnoreCase(tag) || "textarea".equalsIgnoreCase(tag);
    }

    private static boolean isOutputTag(String tag) {
        // OUTPUT sequence: label / link / text containers (adjust to your needs)
        return "label".equalsIgnoreCase(tag)
                || "a".equalsIgnoreCase(tag)
                || "span".equalsIgnoreCase(tag)
                || "p".equalsIgnoreCase(tag)
                || "div".equalsIgnoreCase(tag);
    }

    private static boolean isClickTag(String tag) {
        // CLICK sequence: button / link (add others if your UI uses them)
        return "button".equalsIgnoreCase(tag) || "a".equalsIgnoreCase(tag);
    }
}
