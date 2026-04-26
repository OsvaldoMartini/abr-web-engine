package com.allinweb.ch.model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;

/**
 * Runtime denormalized view of an {@link OcrConfigProfile} + its {@link OcrConfigParam}s.
 * Typed getters for the OCR pipeline; fall back to hardcoded defaults when a key is absent
 * so older seeded profiles (or hand-rolled DB edits) never crash the pipeline.
 */
public class OcrConfig {

    private static final Gson GSON = new Gson();

    @Getter
    private final OcrConfigProfile profile;

    /** category → name → param. */
    private final Map<String, Map<String, OcrConfigParam>> byCategory = new HashMap<>();

    public OcrConfig(OcrConfigProfile profile, List<OcrConfigParam> params) {
        this.profile = profile;
        if (params != null) {
            for (OcrConfigParam p : params) {
                byCategory
                        .computeIfAbsent(p.getCategory(), k -> new HashMap<>())
                        .put(p.getName(), p);
            }
        }
    }

    public String getString(String category, String name, String fallback) {
        OcrConfigParam p = get(category, name);
        return p == null || p.getValue() == null ? fallback : p.getValue();
    }

    public int getInt(String category, String name, int fallback) {
        OcrConfigParam p = get(category, name);
        if (p == null || p.getValue() == null) return fallback;
        try {
            return Integer.parseInt(p.getValue().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public double getDouble(String category, String name, double fallback) {
        OcrConfigParam p = get(category, name);
        if (p == null || p.getValue() == null) return fallback;
        try {
            return Double.parseDouble(p.getValue().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public boolean getBool(String category, String name, boolean fallback) {
        OcrConfigParam p = get(category, name);
        if (p == null || p.getValue() == null) return fallback;
        String v = p.getValue().trim().toLowerCase();
        return "true".equals(v) || "1".equals(v) || "yes".equals(v) || "y".equals(v);
    }

    /** JSON param → list of maps (e.g. color_mapping ops). Never returns null. */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getJsonArray(String category, String name) {
        OcrConfigParam p = get(category, name);
        if (p == null || p.getValue() == null || p.getValue().isBlank()) return Collections.emptyList();
        try {
            Type t = new TypeToken<List<Map<String, Object>>>() {}.getType();
            List<Map<String, Object>> out = GSON.fromJson(p.getValue(), t);
            return out == null ? Collections.emptyList() : out;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private OcrConfigParam get(String category, String name) {
        Map<String, OcrConfigParam> m = byCategory.get(category);
        return m == null ? null : m.get(name);
    }

    /** Returns a flat copy of all params for editing / UI. */
    public List<OcrConfigParam> allParams() {
        List<OcrConfigParam> out = new ArrayList<>();
        for (Map<String, OcrConfigParam> m : byCategory.values()) out.addAll(m.values());
        return out;
    }
}
