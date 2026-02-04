package com.allinweb.ch.util;

import com.allinweb.ch.model.InstructionLoad;
import com.allinweb.ch.model.ReferenceLoadDTO;
import com.allinweb.ch.model.TargetElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class InstructionLoadUpdater {

    private InstructionLoadUpdater() {
        // utility class
    }

    public static void applyMatchToInstruction(InstructionLoad currentInstruction, TargetElement matchScanned) {
        if (currentInstruction == null || matchScanned == null) return;

        // 1) Update xpath
        String newXpath = pickXpath(matchScanned);
        if (newXpath != null && !newXpath.trim().isEmpty()) {
            currentInstruction.setXpath(newXpath.trim());
        }

        // 1b) IMPORTANT: Update iFrame / shadow / cssSelector fields
        // (only overwrite when scanned value is non-blank)
        setIfNotBlank(currentInstruction::setIFrameXPath, matchScanned.getIFrameXPath());
        setIfNotBlank(currentInstruction::setShadowHost, matchScanned.getShadowHost());
        setIfNotBlank(currentInstruction::setShadowRoot, matchScanned.getShadowRoot());
        setIfNotBlank(currentInstruction::setCssSelector, matchScanned.getCssSelector());

        // 2) Update ReferenceLoadDTO list from savedReferences map
        Map<String, String> saved = matchScanned.getSavedReferences();
        if (saved == null || saved.isEmpty()) return;

        List<ReferenceLoadDTO> refs = currentInstruction.getReferenceLoadDTOList();
        if (refs == null) {
            refs = new ArrayList<>();
            currentInstruction.setReferenceLoadDTOList(refs);
        }

        Map<String, ReferenceLoadDTO> byType = new HashMap<>();
        for (ReferenceLoadDTO r : refs) {
            if (r == null) continue;
            if (r.getReferenceType() == null) continue;
            byType.put(r.getReferenceType(), r);
        }

        for (Map.Entry<String, String> e : saved.entrySet()) {
            String type = e.getKey();
            String value = e.getValue();
            if (type == null || type.trim().isEmpty()) continue;

            ReferenceLoadDTO existing = byType.get(type);
            if (existing != null) {
                existing.setValue(value);
                existing.setInstructionId(currentInstruction.getId());
                existing.setHomeBankingId(currentInstruction.getHomeBankingId());
                existing.setBotJobId(currentInstruction.getBotJobId());
            } else {
                ReferenceLoadDTO r = new ReferenceLoadDTO();
                r.setReferenceType(type);
                r.setValue(value);
                r.setInstructionId(currentInstruction.getId());
                r.setHomeBankingId(currentInstruction.getHomeBankingId());
                r.setBotJobId(currentInstruction.getBotJobId());
                refs.add(r);
            }
        }
    }

    /** Helper: only set target if value is not null/blank. */
    private static void setIfNotBlank(java.util.function.Consumer<String> setter, String value) {
        if (setter == null) return;
        if (value == null) return;
        String v = value.trim();
        if (!v.isEmpty()) {
            setter.accept(v);
        }
    }

    private static String pickXpath(TargetElement t) {
        // Pick ONE source, in your preferred priority order:
        if (!isBlank(t.getCustomXPath())) return t.getCustomXPath();
        if (!isBlank(t.getCurrentXPath())) return t.getCurrentXPath();
        if (!isBlank(t.getXPath())) return t.getXPath(); // field name is "XPath" in your class
        if (!isBlank(t.getXPathWorkedFirst())) return t.getXPathWorkedFirst();
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
