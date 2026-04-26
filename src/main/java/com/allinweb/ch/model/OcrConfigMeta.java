package com.allinweb.ch.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Metadata for every {@link OcrConfigDefaults#CANONICAL} param:
 * <ul>
 *   <li>{@link #DESCRIPTIONS} — short help text shown as a tooltip</li>
 *   <li>{@link #ENUMS} — allowed values for ComboBox controls</li>
 *   <li>{@link #RANGES} — min / max / step for numeric Spinners</li>
 * </ul>
 * Adding a new param to {@code OcrConfigDefaults} should also add an entry here,
 * otherwise the editor renders a plain TextField with no help.
 *
 * <p>Authoritative prose reference lives in {@code specifications/OCR_CONFIG_PARAMS.md}.
 */
public final class OcrConfigMeta {

    public static final class Range {
        public final double min;
        public final double max;
        public final double step;

        public Range(double min, double max, double step) {
            this.min = min;
            this.max = max;
            this.step = step;
        }
    }

    public static final Map<String, String> DESCRIPTIONS;
    public static final Map<String, List<String>> ENUMS;
    public static final Map<String, Range> RANGES;

    static {
        Map<String, String> d = new LinkedHashMap<>();

        // correlation
        d.put(
                "correlation.proximity_px_global",
                "Default max distance (CSS px) from an element's center to an OCR word center for a PROXIMITY match. Lower = stricter.");
        d.put(
                "correlation.proximity_px_input",
                "Proximity threshold (CSS px) used specifically for <input> / <textarea> / <select>. Raise to ~80 so form inputs pick up their sibling <label> above them.");
        d.put(
                "correlation.proximity_px_button",
                "Proximity threshold (CSS px) used specifically for <button> elements.");
        d.put(
                "correlation.dedupe_iou",
                "IoU threshold (0..1). When two OCR words from different passes overlap above this, the higher-confidence one wins. 0.6 is a good default.");
        d.put(
                "correlation.ocr_exact_contain_weight",
                "Score weight for OCR EXACT_CONTAIN candidates inside ElementTextResolver. Default 0.85 means raw OCR text outscores DOM-derived text (scanner.someText 0.40 ×1.5 corroborated = 0.60). Lower to ~0.55 to make DOM-corroborated text always win when both exist — kills orphan rows from OCR misreads (e.g. 'access_oblems').");
        d.put(
                "correlation.ocr_overlap_weight",
                "Score weight for OCR OVERLAP candidates. Default 0.70. Lower to ~0.45 in the DOM-First profile so OVERLAP doesn't outscore corroborated DOM text either.");
        d.put(
                "correlation.ocr_proximity_weight",
                "Score weight for OCR PROXIMITY candidates. Default 0.55. Lower to ~0.35 in the DOM-First profile.");

        // engine
        d.put(
                "engine.languages",
                "Tess4J language codes joined with '+'. Example: eng+ita+fra+deu. Only codes with traineddata bundled in resources are honoured.");
        d.put(
                "engine.psm_mode",
                "Tesseract Page Segmentation Mode. 3=AUTO, 4=single column, 6=single uniform block, 11=sparse text, 12=sparse text with OSD. Try 11/12 when AUTO misses scattered labels.");
        d.put(
                "engine.user_defined_dpi",
                "Pixel DPI hint passed to Tesseract. 300 is the sweet spot for web screenshots; raise to 400 for small fonts.");

        // screenshot
        d.put(
                "screenshot.scope",
                "viewport = only visible area; full_page = scroll-stitched capture (NOT YET IMPLEMENTED — planned Roadmap 2 s2).");
        d.put(
                "screenshot.pre_capture_delay_ms",
                "Wait N ms after the page ready event before snapping. Useful for pages with fade-in animations.");

        // preprocessing
        d.put(
                "preprocessing.enable_clahe_pass",
                "When true, run a SECOND OCR pass on a CLAHE-thresholded + upscaled copy of the screenshot and merge results. Catches low-contrast text (e.g., dark-blue on light-blue info boxes).");
        d.put(
                "preprocessing.clahe_clip",
                "CLAHE clip limit. Higher = more aggressive contrast. 3.0 balances clarity and noise.");
        d.put(
                "preprocessing.clahe_tile",
                "CLAHE tile size (both dimensions). Smaller = finer local contrast. 8 is a typical starting point.");
        d.put(
                "preprocessing.adaptive_block",
                "Adaptive-threshold block size. MUST BE ODD. Larger = smoother binarization; smaller = captures thin glyphs.");
        d.put(
                "preprocessing.adaptive_c",
                "Adaptive-threshold constant C subtracted from the local mean. Positive = darker result.");
        d.put(
                "preprocessing.upscale_factor",
                "Integer upscale applied to the preprocessed image before OCR. Default 2. Must match the upscale used by OcrPreprocessorOpenCv.preprocess (×2) — changing this will misalign the mapped-back word bboxes.");

        // color_mapping
        d.put(
                "color_mapping.ops",
                "Ordered JSON array of ColorMapper ops applied to the screenshot BEFORE Tesseract. Four modes: replace_in_hsv_range, keep_in_range_binarize, desaturate_to_black_white, invert. See specifications/OCR_CONFIG_PARAMS.md for examples.");

        // button_detection
        d.put(
                "button_detection.enable_red",
                "Detect red rectangular buttons via OpenCV HSV filter, crop each to a ROI, upscale ×3, and OCR. Catches white-on-red labels like BancaStato's Send button.");
        d.put("button_detection.enable_blue", "Same as enable_red but for blue buttons.");
        d.put(
                "button_detection.enable_any",
                "Color-agnostic button detection via Canny edges + morphology + aspect-ratio heuristic. Fallback when neither red nor blue matches the theme.");

        // output
        d.put("output.save_raw_ocr", "Write ocr-HP.json with the full OCR word list. Keep on for debugging.");
        d.put(
                "output.save_correlation",
                "Write ocr-correlation-HP.json with per-element DOM↔OCR match quality. Keep on for Roadmap 3 consumption.");
        d.put(
                "output.save_annotated_png",
                "Write page-HP-annotated.png with OCR boxes (green), DOM rects (red), EXACT_CONTAIN highlights (thick green) overlaid on the screenshot. Expensive to render — enable only while tuning.");
        d.put(
                "output.log_level",
                "Log level for the OCR pipeline logger. Not yet consumed; reserved for future fine-grained control.");
        d.put(
                "output.approved_xpaths",
                "JSON array of xPaths the user marked as 'approved' in the Test On Current Page review grid. Persists user QA across modal sessions. Read by the editor only — not consumed by the OCR pipeline.");
        d.put(
                "output.skip_hidden_inputs",
                "When true, ElementLocatorRepository.upsertOnPick skips elements with attribute type='hidden'. Recommended ON to keep CSRF tokens / hidden state out of the locator table.");
        d.put(
                "output.skip_label_only_elements",
                "When true, locator persistence skips <label> tags. Recommended ON because labels usually duplicate the data of their associated <input> via aria/for-id — persisting both creates user_number + user_number_2 noise.");
        d.put(
                "output.min_defined_name_length",
                "Minimum character length a definedName must reach before the locator persists. Set to ~3 to drop nameless elements (e.g. bare <a> resolving to 'a'). 0 disables the filter.");

        DESCRIPTIONS = Collections.unmodifiableMap(d);

        Map<String, List<String>> e = new LinkedHashMap<>();
        e.put("engine.psm_mode", List.of("3", "4", "6", "11", "12"));
        e.put("screenshot.scope", List.of("viewport", "full_page"));
        e.put("output.log_level", List.of("DEBUG", "INFO", "WARN", "ERROR"));
        ENUMS = Collections.unmodifiableMap(e);

        Map<String, Range> r = new LinkedHashMap<>();
        r.put("correlation.proximity_px_global", new Range(0, 500, 5));
        r.put("correlation.proximity_px_input", new Range(0, 500, 5));
        r.put("correlation.proximity_px_button", new Range(0, 500, 5));
        r.put("correlation.dedupe_iou", new Range(0.0, 1.0, 0.05));
        r.put("correlation.ocr_exact_contain_weight", new Range(0.0, 1.5, 0.05));
        r.put("correlation.ocr_overlap_weight", new Range(0.0, 1.5, 0.05));
        r.put("correlation.ocr_proximity_weight", new Range(0.0, 1.5, 0.05));
        r.put("output.min_defined_name_length", new Range(0, 32, 1));
        r.put("engine.user_defined_dpi", new Range(72, 600, 10));
        r.put("screenshot.pre_capture_delay_ms", new Range(0, 10000, 100));
        r.put("preprocessing.clahe_clip", new Range(0.0, 20.0, 0.5));
        r.put("preprocessing.clahe_tile", new Range(1, 64, 1));
        r.put("preprocessing.adaptive_block", new Range(3, 99, 2));
        r.put("preprocessing.adaptive_c", new Range(-50, 50, 1));
        r.put("preprocessing.upscale_factor", new Range(1, 8, 1));
        RANGES = Collections.unmodifiableMap(r);
    }

    private OcrConfigMeta() {}
}
