package com.allinweb.ch.facade;

import com.allinweb.ch.model.ElementLocatorEntity;
import com.allinweb.ch.model.ElementLocatorRenameEntity;
import com.allinweb.ch.util.TextSimilarity;
import java.math.BigDecimal;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Roadmap 3 Phase 3c-ii — multi-strategy element recovery for bot-run time.
 *
 * <p>Given a saved {@link ElementLocatorEntity}, walk a confidence-ranked ladder of
 * lookup strategies and return the first hit. The Engine will call this in place of
 * its current xPath-only resolution so that picks survive client-side renames.
 *
 * <p>Strategy order (most reliable first):
 * <ol>
 *   <li>{@code XPATH_CURRENT} — try the latest known xPath as-is. 100% confidence on hit.</li>
 *   <li>{@code XPATH_ORIGINAL} — fall back to the frozen original xPath; 90% on hit.</li>
 *   <li>{@code CSS_SELECTOR} — current css selector. 85%.</li>
 *   <li>{@code ATTRIB_ID} — {@code By.id(attrib_id_current)}. 80%.</li>
 *   <li>{@code ATTRIB_NAME} — {@code By.name(attrib_name_current)}. 75%.</li>
 *   <li>{@code TEXT_FUZZY} — Tesseract-style fuzzy match on visible text within the
 *       same tagName. Confidence = {@code max(levenshtein_ratio, token_set_ratio) * 70}.</li>
 *   <li>{@code COORDS} — {@code document.elementFromPoint} at the original CSS-px center;
 *       confidence capped at 60% because layout shifts make this fragile.</li>
 * </ol>
 *
 * <p>This service performs <strong>read-only</strong> DOM lookups and <strong>writes</strong> a single
 * {@link ElementLocatorRenameEntity} row when the winning strategy is anything other than
 * {@code XPATH_CURRENT}, so the audit trail captures every drift recovery.
 */
@Slf4j
public final class ElementRecoveryService {

    private static final org.slf4j.Logger logScanner = org.slf4j.LoggerFactory.getLogger("com.allinweb.scanner");

    /** Minimum acceptable confidence for any non-direct strategy. Below this we return NOT_FOUND. */
    public static final double MIN_ACCEPTABLE_CONFIDENCE = 50.0;

    /** Levenshtein/token-set ratio above which TEXT_FUZZY is considered a match. */
    public static final double TEXT_FUZZY_THRESHOLD = 0.70;

    /** Coordinate-fallback tolerance in CSS px. */
    public static final double COORDS_TOLERANCE_PX = 25.0;

    private static volatile ElementRecoveryService instance;

    public static ElementRecoveryService getInstance() {
        if (instance == null) {
            synchronized (ElementRecoveryService.class) {
                if (instance == null) instance = new ElementRecoveryService();
            }
        }
        return instance;
    }

    private ElementRecoveryService() {}

    /** Outcome of a lookup attempt. {@code element == null} means recovery failed. */
    public static final class Recovery {
        public final WebElement element;
        public final double confidence;
        public final String strategy;

        public Recovery(WebElement element, double confidence, String strategy) {
            this.element = element;
            this.confidence = confidence;
            this.strategy = strategy;
        }

        public static Recovery notFound() {
            return new Recovery(null, 0.0, "NOT_FOUND");
        }

        public boolean found() {
            return element != null;
        }
    }

    /**
     * Walk the strategy ladder; return the first hit at or above {@link #MIN_ACCEPTABLE_CONFIDENCE}.
     * Writes a rename row when the winning strategy is not {@code XPATH_CURRENT}.
     */
    public Recovery findOrRecover(WebDriver driver, ElementLocatorEntity loc) {
        if (driver == null || loc == null) return Recovery.notFound();

        Recovery winner = walkStrategies(driver, loc);

        if (winner.found() && !"XPATH_CURRENT".equals(winner.strategy)) {
            recordRecovery(loc, winner);
            logScanner.info(
                    "LOCATOR RECOVERY — id={} defined='{}' strategy={} confidence={}",
                    loc.getId(),
                    loc.getDefinedName(),
                    winner.strategy,
                    String.format("%.1f", winner.confidence));
        } else if (!winner.found()) {
            logScanner.info(
                    "LOCATOR RECOVERY — id={} defined='{}' strategy=NOT_FOUND", loc.getId(), loc.getDefinedName());
        }

        return winner;
    }

    private Recovery walkStrategies(WebDriver driver, ElementLocatorEntity loc) {
        WebElement el;

        // 1. XPATH_CURRENT
        el = tryFind(driver, By.xpath(loc.getXPathCurrent()));
        if (el != null) return new Recovery(el, 100.0, "XPATH_CURRENT");

        // 2. XPATH_ORIGINAL — only useful when CURRENT was edited away from ORIGINAL and ORIGINAL still resolves.
        if (notBlankAndDifferent(loc.getXPathCurrent(), loc.getXPathOriginal())) {
            el = tryFind(driver, By.xpath(loc.getXPathOriginal()));
            if (el != null) return new Recovery(el, 90.0, "XPATH_ORIGINAL");
        }

        // 3. CSS_SELECTOR
        if (notBlank(loc.getCssSelectorCurrent())) {
            el = tryFind(driver, By.cssSelector(loc.getCssSelectorCurrent()));
            if (el != null) return new Recovery(el, 85.0, "CSS_SELECTOR");
        }

        // 4. ATTRIB_ID
        if (notBlank(loc.getAttribIdCurrent())) {
            el = tryFind(driver, By.id(loc.getAttribIdCurrent()));
            if (el != null) return new Recovery(el, 80.0, "ATTRIB_ID");
        }

        // 5. ATTRIB_NAME
        if (notBlank(loc.getAttribNameCurrent())) {
            el = tryFind(driver, By.name(loc.getAttribNameCurrent()));
            if (el != null) return new Recovery(el, 75.0, "ATTRIB_NAME");
        }

        // 6. TEXT_FUZZY — scan tagName candidates and pick the best text match.
        Recovery fuzzy = tryFuzzyText(driver, loc);
        if (fuzzy.found()) return fuzzy;

        // 7. COORDS — last-resort hit-test at the original viewport position.
        Recovery coords = tryCoordinateMatch(driver, loc);
        if (coords.found()) return coords;

        return Recovery.notFound();
    }

    // ── Individual strategies ─────────────────────────────────────────────

    private Recovery tryFuzzyText(WebDriver driver, ElementLocatorEntity loc) {
        String target = loc.getSomeTextOriginal();
        if (target == null || target.isBlank()) target = loc.getSomeTextCurrent();
        if (target == null || target.isBlank()) return Recovery.notFound();

        String tag = loc.getTagNameOriginal();
        if (tag == null || tag.isBlank()) tag = loc.getTagNameCurrent();
        if (tag == null || tag.isBlank()) return Recovery.notFound();

        List<WebElement> candidates;
        try {
            candidates = driver.findElements(By.tagName(tag));
        } catch (RuntimeException e) {
            return Recovery.notFound();
        }
        if (candidates == null || candidates.isEmpty()) return Recovery.notFound();

        WebElement best = null;
        double bestRatio = 0.0;
        for (WebElement c : candidates) {
            String text;
            try {
                text = c.getText();
            } catch (RuntimeException e) {
                continue;
            }
            if (text == null || text.isBlank()) continue;
            double r =
                    Math.max(TextSimilarity.levenshteinRatio(text, target), TextSimilarity.tokenSetRatio(text, target));
            if (r > bestRatio) {
                bestRatio = r;
                best = c;
            }
        }
        if (best == null || bestRatio < TEXT_FUZZY_THRESHOLD) return Recovery.notFound();
        // Map ratio in [0.7, 1.0] to confidence in [50, 70].
        double conf = 50.0 + (bestRatio - TEXT_FUZZY_THRESHOLD) * (20.0 / (1.0 - TEXT_FUZZY_THRESHOLD));
        return new Recovery(best, conf, "TEXT_FUZZY");
    }

    private Recovery tryCoordinateMatch(WebDriver driver, ElementLocatorEntity loc) {
        String coordsStr = loc.getCoordsOriginal();
        if (coordsStr == null || coordsStr.isBlank()) coordsStr = loc.getCoordsCurrent();
        if (coordsStr == null || !coordsStr.contains(",")) return Recovery.notFound();

        double cx;
        double cy;
        try {
            String[] parts = coordsStr.split(",");
            cx = Double.parseDouble(parts[0].trim());
            cy = Double.parseDouble(parts[1].trim());
        } catch (Exception e) {
            return Recovery.notFound();
        }

        try {
            Object result = ((JavascriptExecutor) driver)
                    .executeScript("return document.elementFromPoint(arguments[0], arguments[1]);", cx, cy);
            if (result instanceof WebElement) {
                return new Recovery((WebElement) result, 60.0, "COORDS");
            }
        } catch (RuntimeException e) {
            // Some drivers return a wrapped object that's not directly a WebElement; ignore.
        }

        // Try a small radius if exact center missed (layout shift, sub-pixel).
        for (double dx = -COORDS_TOLERANCE_PX; dx <= COORDS_TOLERANCE_PX; dx += COORDS_TOLERANCE_PX) {
            for (double dy = -COORDS_TOLERANCE_PX; dy <= COORDS_TOLERANCE_PX; dy += COORDS_TOLERANCE_PX) {
                if (dx == 0 && dy == 0) continue;
                try {
                    Object r = ((JavascriptExecutor) driver)
                            .executeScript(
                                    "return document.elementFromPoint(arguments[0], arguments[1]);", cx + dx, cy + dy);
                    if (r instanceof WebElement) {
                        return new Recovery((WebElement) r, 55.0, "COORDS");
                    }
                } catch (RuntimeException ignore) {
                    // continue
                }
            }
        }
        return Recovery.notFound();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static WebElement tryFind(WebDriver driver, By by) {
        if (by == null) return null;
        try {
            return driver.findElement(by);
        } catch (RuntimeException e) {
            // RuntimeException covers NoSuchElementException, StaleElementReferenceException,
            // InvalidSelectorException, and any driver-side WebDriverException — all "not found here".
            return null;
        }
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static boolean notBlankAndDifferent(String current, String original) {
        if (!notBlank(original)) return false;
        return current == null || !current.equals(original);
    }

    private void recordRecovery(ElementLocatorEntity loc, Recovery winner) {
        ElementLocatorRenameEntity row = new ElementLocatorRenameEntity();
        row.setLocatorId(loc.getId());
        row.setChangeType("XPATH"); // Recovery implies the live xpath wasn't usable.
        row.setFieldName("recovery");
        row.setOldValue(loc.getXPathCurrent());
        row.setNewValue("(recovered via " + winner.strategy + ")");
        row.setRecoveryStrategy(winner.strategy);
        row.setMatchConfidence(BigDecimal.valueOf(Math.round(winner.confidence * 10.0) / 10.0));
        ElementLocatorRepository.getInstance().insertRename(row);
    }
}
