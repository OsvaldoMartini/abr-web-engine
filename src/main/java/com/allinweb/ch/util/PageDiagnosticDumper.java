package com.allinweb.ch.util;

import com.allinweb.ch.model.ElementDTO;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

@Slf4j
public final class PageDiagnosticDumper {

    public static final String SUBFOLDER = "page_diagnostics";

    public record RectTarget(String xpath, String iframeXPath) {}

    // ---- Top-level document ----

    private static final String JS_OUTER_HTML = "return document.documentElement.outerHTML;";

    private static final String JS_META =
            """
            return JSON.stringify({
              url: location.href,
              userAgent: navigator.userAgent,
              devicePixelRatio: window.devicePixelRatio,
              innerWidth: window.innerWidth,
              innerHeight: window.innerHeight,
              scrollX: window.scrollX,
              scrollY: window.scrollY,
              documentWidth: document.documentElement.scrollWidth,
              documentHeight: document.documentElement.scrollHeight,
              readyState: document.readyState,
              timestamp: new Date().toISOString()
            });
            """;

    private static final String JS_SHADOW_SURVEY =
            """
            function walk(n, path, out) {
              if (n.shadowRoot) {
                out.push({host: path, hostTag: n.tagName, mode: n.shadowRoot.mode});
                for (const c of n.shadowRoot.children) walk(c, path + '>' + c.tagName, out);
              }
              for (const c of (n.children || [])) walk(c, path + '>' + c.tagName, out);
            }
            const out = [];
            walk(document.documentElement, 'html', out);
            return JSON.stringify(out);
            """;

    // ---- Iframe survey (Tier: iframe patch) ----

    private static final String JS_IFRAMES_SURVEY =
            """
            function xpathOf(node) {
              if (!node || node.nodeType !== 1) return '';
              if (node === document.documentElement) return '/html';
              const parts = [];
              let n = node;
              while (n && n.nodeType === 1 && n !== document.documentElement) {
                let idx = 1;
                let sib = n.previousElementSibling;
                while (sib) { if (sib.tagName === n.tagName) idx++; sib = sib.previousElementSibling; }
                parts.unshift(n.tagName.toLowerCase() + '[' + idx + ']');
                n = n.parentElement;
              }
              return '/html/' + parts.join('/');
            }
            const out = [];
            const frames = document.querySelectorAll('iframe');
            frames.forEach((f, i) => {
              const b = f.getBoundingClientRect();
              const rect = {x: b.x, y: b.y, width: b.width, height: b.height};
              let sameOrigin = false, innerWidth = null, innerHeight = null, readyState = null;
              try {
                const doc = f.contentDocument;
                if (doc) {
                  sameOrigin = true;
                  if (f.contentWindow) {
                    innerWidth = f.contentWindow.innerWidth;
                    innerHeight = f.contentWindow.innerHeight;
                  }
                  readyState = doc.readyState;
                }
              } catch (e) { sameOrigin = false; }
              out.push({
                index: i,
                iframeXPath: xpathOf(f),
                src: f.src || '',
                name: f.name || '',
                id: f.id || '',
                rect: rect,
                sameOrigin: sameOrigin,
                innerWidth: innerWidth,
                innerHeight: innerHeight,
                readyState: readyState
              });
            });
            return JSON.stringify(out);
            """;

    // ---- Overlay / cookie-consent fingerprint (Tier B) ----

    private static final String JS_OVERLAYS_SURVEY =
            """
            const patterns = /onetrust|cookie|consent|gdpr|ccpa|didomi|usercentrics|truste|cookiebot|iubenda|termly|quantcast|axeptio|osano|trustarc/i;
            const out = [];
            function check(el, sourceHint) {
              const id = el.id || '';
              const cls = (el.className && el.className.baseVal !== undefined
                           ? el.className.baseVal
                           : (el.className || ''));
              const role = el.getAttribute ? (el.getAttribute('role') || '') : '';
              const tag = el.tagName;
              let matched = false;
              if (patterns.test(id) || patterns.test(cls)) matched = true;
              if (role === 'dialog' || role === 'alertdialog') matched = true;
              if (tag === 'DIALOG' && el.hasAttribute && el.hasAttribute('open')) matched = true;
              if (el.hasAttribute && el.hasAttribute('popover')) matched = true;
              if (!matched) return null;
              const b = el.getBoundingClientRect();
              const cs = getComputedStyle(el);
              return {
                tag: tag, id: id, class: cls, role: role,
                rect: {x: b.x, y: b.y, width: b.width, height: b.height},
                zIndex: cs.zIndex, position: cs.position,
                display: cs.display, visibility: cs.visibility,
                visible: b.width > 0 && b.height > 0
                         && cs.display !== 'none' && cs.visibility !== 'hidden',
                source: sourceHint
              };
            }
            function walk(root, sourceHint) {
              const all = root.querySelectorAll ? root.querySelectorAll('*') : [];
              all.forEach(el => {
                const hit = check(el, sourceHint);
                if (hit) out.push(hit);
                if (el.shadowRoot && el.shadowRoot.mode === 'open') {
                  walk(el.shadowRoot, sourceHint + '>shadow:' + el.tagName);
                }
              });
            }
            walk(document, 'top');
            document.querySelectorAll('iframe').forEach((f, i) => {
              try {
                const doc = f.contentDocument;
                if (doc) walk(doc, 'iframe[' + i + ']');
              } catch (e) {}
            });
            return JSON.stringify(out);
            """;

    // ---- Per-element rects enriched with occlusion + stacking (Tier A) ----

    private static final String JS_RECTS =
            """
            const targets = arguments[0];
            const out = [];
            function classOf(n) {
              if (!n) return '';
              if (n.className && n.className.baseVal !== undefined) return n.className.baseVal;
              return n.className || '';
            }
            function resolveInDoc(doc, xp) {
              try {
                return doc.evaluate(xp, doc, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
              } catch (e) { return null; }
            }
            function createsStackingContext(cs) {
              if (cs.position !== 'static' && cs.zIndex !== 'auto') return true;
              if (parseFloat(cs.opacity) < 1) return true;
              if (cs.transform && cs.transform !== 'none') return true;
              if (cs.filter && cs.filter !== 'none') return true;
              if (cs.mixBlendMode && cs.mixBlendMode !== 'normal') return true;
              if (cs.isolation === 'isolate') return true;
              if (cs.willChange && /transform|opacity|filter/.test(cs.willChange)) return true;
              return false;
            }
            function stackingChain(node) {
              const chain = [];
              let n = node && node.parentElement;
              while (n && n !== document.documentElement) {
                const cs = getComputedStyle(n);
                if (createsStackingContext(cs)) {
                  chain.push({
                    tag: n.tagName, id: n.id || '', class: classOf(n),
                    zIndex: cs.zIndex, position: cs.position,
                    opacity: cs.opacity, transform: cs.transform
                  });
                }
                n = n.parentElement;
              }
              return chain;
            }
            function ancestorPointerEventsNone(node) {
              let n = node;
              while (n && n.nodeType === 1 && n !== document.documentElement) {
                if (getComputedStyle(n).pointerEvents === 'none') return true;
                n = n.parentElement;
              }
              return false;
            }
            for (const t of targets) {
              let doc = document;
              let frameInfo = null;
              if (t.iframeXPath && t.iframeXPath.length > 0) {
                frameInfo = {iframeXPath: t.iframeXPath, crossOrigin: false};
                const frameNode = resolveInDoc(document, t.iframeXPath);
                if (!frameNode || frameNode.tagName !== 'IFRAME') {
                  out.push({xpath: t.xpath, iframeXPath: t.iframeXPath, found: false,
                            rect: null, frame: frameInfo, error: 'iframe-xpath-not-found'});
                  continue;
                }
                try {
                  doc = frameNode.contentDocument;
                  if (!doc) {
                    frameInfo.crossOrigin = true;
                    out.push({xpath: t.xpath, iframeXPath: t.iframeXPath, found: false,
                              rect: null, frame: frameInfo, error: 'cross-origin-iframe'});
                    continue;
                  }
                } catch (e) {
                  frameInfo.crossOrigin = true;
                  out.push({xpath: t.xpath, iframeXPath: t.iframeXPath, found: false,
                            rect: null, frame: frameInfo, error: 'cross-origin-iframe'});
                  continue;
                }
              }
              const node = resolveInDoc(doc, t.xpath);
              if (!node) {
                out.push({xpath: t.xpath, iframeXPath: t.iframeXPath || '', found: false,
                          rect: null, frame: frameInfo});
                continue;
              }
              const b = node.getBoundingClientRect();
              // Capture both coordinate systems so OcrDomCorrelator can pick the right one
              // based on screenshot.scope. viewport-relative for scope=viewport (default);
              // page-relative when the OCR pass ran over a scroll-stitched full_page image.
              const sx = (typeof window.pageXOffset === 'number' ? window.pageXOffset : window.scrollX) || 0;
              const sy = (typeof window.pageYOffset === 'number' ? window.pageYOffset : window.scrollY) || 0;
              const rect = {x: b.x, y: b.y, width: b.width, height: b.height,
                            top: b.top, left: b.left, bottom: b.bottom, right: b.right,
                            pageX: b.x + sx, pageY: b.y + sy,
                            pageTop: b.top + sy, pageLeft: b.left + sx,
                            pageBottom: b.bottom + sy, pageRight: b.right + sx};
              const cs = getComputedStyle(node);
              const computedStyle = {
                position: cs.position, zIndex: cs.zIndex, display: cs.display,
                visibility: cs.visibility, opacity: cs.opacity,
                pointerEvents: cs.pointerEvents, overflow: cs.overflow
              };
              const cx = b.left + b.width / 2;
              const cy = b.top + b.height / 2;
              let elementAtCenter = null;
              try {
                const e = doc.elementFromPoint(cx, cy);
                if (e) {
                  const containsNode = e.contains && e.contains(node);
                  const isDescendant = node.contains && node.contains(e);
                  elementAtCenter = {
                    tag: e.tagName, id: e.id || '', class: classOf(e),
                    isSameNode: e === node,
                    isAncestor: !!containsNode,
                    isDescendant: !!isDescendant,
                    occluded: e !== node && !isDescendant
                  };
                }
              } catch (e) {}
              let inTopLayer = false;
              try {
                if (node.matches && node.matches(':modal')) inTopLayer = true;
              } catch (e) {}
              try {
                if (node.matches && node.matches(':popover-open')) inTopLayer = true;
              } catch (e) {}
              out.push({
                xpath: t.xpath,
                iframeXPath: t.iframeXPath || '',
                found: true,
                tag: node.tagName,
                id: node.id || '',
                class: classOf(node),
                rect: rect,
                computedStyle: computedStyle,
                elementAtCenter: elementAtCenter,
                inTopLayer: inTopLayer,
                ancestorPointerEventsNone: ancestorPointerEventsNone(node),
                stackingAncestors: stackingChain(node),
                frame: frameInfo
              });
            }
            return JSON.stringify(out);
            """;

    private PageDiagnosticDumper() {}

    /** Page-level one-shot: HTML + meta + shadow + iframes + overlays. Use at clone-mode activation. */
    public static void dumpAll(WebDriver driver, String pathDb, String prefix) {
        dumpPage(driver, pathDb, prefix);
        dumpIframes(driver, pathDb, prefix);
        dumpOverlays(driver, pathDb, prefix);
    }

    /** HTML + live outerHTML + meta + shadow. Subset of dumpAll. */
    public static void dumpPage(WebDriver driver, String pathDb, String prefix) {
        if (driver == null || pathDb == null || prefix == null) {
            log.warn("dumpPage skipped — driver/pathDb/prefix is null");
            return;
        }
        try {
            Path diagDir = ensureSubfolder(pathDb);
            JavascriptExecutor js = (JavascriptExecutor) driver;

            write(diagDir, prefix + ".html", safe(driver.getPageSource()));
            write(diagDir, prefix + "-live.html", safe((String) js.executeScript(JS_OUTER_HTML)));
            write(diagDir, prefix + "-meta.json", safe((String) js.executeScript(JS_META)));
            write(diagDir, prefix + "-shadow.json", safe((String) js.executeScript(JS_SHADOW_SURVEY)));

            log.info("Page diagnostic dumped to {}", diagDir);
        } catch (Exception ex) {
            log.warn("PageDiagnosticDumper.dumpPage failed: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Iframe survey + per-iframe HTML (same-origin only).
     * Writes {prefix}-iframes.json and {prefix}-iframe-N.html per accessible iframe.
     */
    public static void dumpIframes(WebDriver driver, String pathDb, String prefix) {
        if (driver == null || pathDb == null || prefix == null) return;
        try {
            Path diagDir = ensureSubfolder(pathDb);
            JavascriptExecutor js = (JavascriptExecutor) driver;

            String surveyJson = (String) js.executeScript(JS_IFRAMES_SURVEY);
            write(diagDir, prefix + "-iframes.json", safe(surveyJson));

            List<WebElement> frames = driver.findElements(By.tagName("iframe"));
            int i = 0;
            for (WebElement f : frames) {
                try {
                    driver.switchTo().frame(f);
                    String html = safe(driver.getPageSource());
                    write(diagDir, prefix + "-iframe-" + i + ".html", html);
                } catch (Exception frameEx) {
                    log.debug("iframe[{}] not dumpable ({}): {}", i, prefix, frameEx.getMessage());
                } finally {
                    try {
                        driver.switchTo().defaultContent();
                    } catch (Exception ignored) {
                        // caller context lost — not our problem to recover
                    }
                }
                i++;
            }
        } catch (Exception ex) {
            log.warn("PageDiagnosticDumper.dumpIframes failed: {}", ex.getMessage(), ex);
        }
    }

    /** Cookie / consent / modal fingerprint across top doc, open shadow roots, and same-origin iframes. */
    public static void dumpOverlays(WebDriver driver, String pathDb, String prefix) {
        if (driver == null || pathDb == null || prefix == null) return;
        try {
            Path diagDir = ensureSubfolder(pathDb);
            JavascriptExecutor js = (JavascriptExecutor) driver;
            String json = (String) js.executeScript(JS_OVERLAYS_SURVEY);
            write(diagDir, prefix + "-overlays.json", safe(json));
        } catch (Exception ex) {
            log.warn("PageDiagnosticDumper.dumpOverlays failed: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Per-pick rects enriched with computed style, elementFromPoint occlusion check,
     * top-layer / popover membership, pointer-events ancestor walk, and stacking-context chain.
     * Supports iframe resolution via RectTarget.iframeXPath (same-origin only).
     */
    public static void dumpRects(WebDriver driver, List<RectTarget> targets, String pathDb, String prefix) {
        if (driver == null || pathDb == null || prefix == null) return;
        if (targets == null || targets.isEmpty()) {
            log.debug("dumpRects skipped — no targets");
            return;
        }
        try {
            Path diagDir = ensureSubfolder(pathDb);
            JavascriptExecutor js = (JavascriptExecutor) driver;

            List<Object> payload = new ArrayList<>(targets.size());
            for (RectTarget t : targets) {
                payload.add(java.util.Map.of(
                        "xpath", t.xpath() == null ? "" : t.xpath(),
                        "iframeXPath", t.iframeXPath() == null ? "" : t.iframeXPath()));
            }
            String json = (String) js.executeScript(JS_RECTS, payload);
            write(diagDir, prefix + "-rects.json", safe(json));
            log.info("Page rects dumped for {} targets to {}", targets.size(), diagDir);
        } catch (Exception ex) {
            log.warn("PageDiagnosticDumper.dumpRects failed: {}", ex.getMessage(), ex);
        }
    }

    /** Convenience: build RectTargets from ElementDTO[] and delegate. */
    public static void dumpRectsFromElements(WebDriver driver, ElementDTO[] elements, String pathDb, String prefix) {
        if (elements == null) return;
        List<RectTarget> targets = Arrays.stream(elements)
                .filter(e -> e != null && e.getXPath() != null)
                .map(e -> new RectTarget(e.getXPath(), e.getIFrameXPath() == null ? "" : e.getIFrameXPath()))
                .collect(Collectors.toList());
        dumpRects(driver, targets, pathDb, prefix);
    }

    private static Path ensureSubfolder(String pathDb) throws IOException {
        Path diagDir = Paths.get(pathDb, SUBFOLDER);
        Files.createDirectories(diagDir);
        return diagDir;
    }

    private static void write(Path dir, String name, String content) throws IOException {
        Files.writeString(dir.resolve(name), content, StandardCharsets.UTF_8);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
