package com.allinweb.ch.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Canonical param list consumed by BOTH:
 * <ul>
 *   <li>{@code M20260427_OcrConfig} — seeds this list into the {@code default} profile on first boot</li>
 *   <li>{@code OcrConfigService.reconcileDefaultProfile} — fills in missing params at runtime, so adding a new knob only requires editing this file</li>
 * </ul>
 * Adding a new param here is all that's needed; no new migration is necessary.
 * The migration tracks creation of the table only, not the content of params.
 */
public final class OcrConfigDefaults {

    public static final List<OcrConfigParam> CANONICAL;

    static {
        List<OcrConfigParam> p = new ArrayList<>();
        p.add(param("correlation", "proximity_px_global", "double", "30.0"));
        p.add(param("correlation", "proximity_px_input", "double", "30.0"));
        p.add(param("correlation", "proximity_px_button", "double", "30.0"));
        p.add(param("correlation", "dedupe_iou", "double", "0.6"));
        p.add(param("correlation", "ocr_exact_contain_weight", "double", "0.85"));
        p.add(param("correlation", "ocr_overlap_weight", "double", "0.70"));
        p.add(param("correlation", "ocr_proximity_weight", "double", "0.55"));

        p.add(param("engine", "languages", "string", "eng+ita+fra+deu"));
        p.add(param("engine", "psm_mode", "int", "3")); // PSM_AUTO
        p.add(param("engine", "user_defined_dpi", "int", "300"));

        p.add(param("screenshot", "scope", "string", "viewport"));
        p.add(param("screenshot", "pre_capture_delay_ms", "int", "0"));

        p.add(param("preprocessing", "enable_clahe_pass", "bool", "false"));
        p.add(param("preprocessing", "clahe_clip", "double", "3.0"));
        p.add(param("preprocessing", "clahe_tile", "int", "8"));
        p.add(param("preprocessing", "adaptive_block", "int", "21"));
        p.add(param("preprocessing", "adaptive_c", "int", "10"));
        p.add(param("preprocessing", "upscale_factor", "int", "2"));

        p.add(param("color_mapping", "ops", "json", "[]"));

        p.add(param("button_detection", "enable_red", "bool", "false"));
        p.add(param("button_detection", "enable_blue", "bool", "false"));
        p.add(param("button_detection", "enable_any", "bool", "false"));

        p.add(param("output", "save_raw_ocr", "bool", "true"));
        p.add(param("output", "save_correlation", "bool", "true"));
        p.add(param("output", "save_annotated_png", "bool", "false"));
        p.add(param("output", "log_level", "string", "INFO"));
        p.add(param("output", "approved_xpaths", "json", "[]"));
        p.add(param("output", "skip_hidden_inputs", "bool", "false"));
        p.add(param("output", "skip_label_only_elements", "bool", "false"));
        p.add(param("output", "min_defined_name_length", "int", "0"));

        CANONICAL = Collections.unmodifiableList(p);
    }

    /** Categories in preferred UI order. */
    public static final List<String> CATEGORIES_IN_ORDER = List.of(
            "correlation", "engine", "screenshot", "preprocessing", "color_mapping", "button_detection", "output");

    private static OcrConfigParam param(String category, String name, String type, String value) {
        return new OcrConfigParam(null, null, category, name, type, value);
    }

    private OcrConfigDefaults() {}
}
