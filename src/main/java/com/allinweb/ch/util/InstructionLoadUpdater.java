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
        // Choose the field you want to use as source:
        String newXpath = pickXpath(matchScanned);
        if (newXpath != null && !newXpath.trim().isEmpty()) {
            currentInstruction.setXpath(newXpath.trim());
        }

        // 2) Update ReferenceLoadDTO list from savedReferences map
        Map<String, String> saved = matchScanned.getSavedReferences();
        if (saved == null || saved.isEmpty()) return;

        List<ReferenceLoadDTO> refs = currentInstruction.getReferenceLoadDTOList();
        if (refs == null) {
            refs = new ArrayList<>();
            currentInstruction.setReferenceLoadDTOList(refs);
        }

        // Index existing by referenceType (case-sensitive; change to lower-case if you want)
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
                // update existing
                existing.setValue(value);
                existing.setInstructionId(currentInstruction.getId());
                existing.setHomeBankingId(currentInstruction.getHomeBankingId());
                existing.setBotJobId(currentInstruction.getBotJobId());
            } else {
                // create new
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
