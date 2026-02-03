package com.allinweb.ch.util;

import com.allinweb.ch.model.InputInfo;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;

public class DomIntrospectionUtil {

    private static final Set<String> IMPORTANT_TAGS = Set.of(
            "a",
            "button",
            "input",
            "textarea",
            "select",
            "option",
            "label",
            "form",
            "img",
            "ul",
            "ol",
            "li",
            "h1",
            "h2",
            "h3",
            "h4",
            "h5",
            "h6",
            "p",
            "span",
            "div",
            "section",
            "article",
            "main",
            "nav",
            "aside",
            "footer");

    private static final Set<String> ALWAYS_IGNORE_TAGS = Set.of(
            "html",
            "head",
            "meta",
            "link",
            "script",
            "style",
            "noscript",
            "base",
            "svg",
            "path",
            "g",
            "circle",
            "polygon",
            "defs",
            "use",
            "title");

    // enum-like values for InputInfo.controlKind
    private static final String CK_TYPE = "TYPE";
    private static final String CK_OPEN_DROPDOWN = "OPEN_DROPDOWN";
    private static final String CK_SELECT_OPTION = "SELECT_OPTION";

    public static List<String> listImportantElementsFromPageSource(WebDriver driver) {
        Document doc = Jsoup.parse(driver.getPageSource());
        Elements all = doc.getAllElements();

        LinkedHashSet<String> unique = new LinkedHashSet<>();

        for (Element el : all) {
            String tag = safeLower(el.tagName());
            if (tag.isBlank()) continue;
            if (ALWAYS_IGNORE_TAGS.contains(tag)) continue;
            if (!IMPORTANT_TAGS.contains(tag)) continue;
            if (isNoisyContainer(el, tag)) continue;

            String idPart = bestIdentifierForAny(el, tag);
            unique.add(tag + " - " + idPart);
        }

        return new ArrayList<>(unique);
    }

    /**
     * Editable / selectable controls list (generic for any page).
     * Includes native + ARIA pattern controls + common framework hints.
     */
    public static List<InputInfo> listInputsWithLabelsFromPageSource(WebDriver driver) {
        Document doc = Jsoup.parse(driver.getPageSource());

        Map<String, String> labelForMap = buildLabelForMap(doc);
        Map<String, String> idToTextMap = buildIdToTextMap(doc);

        Elements controls = collectEditableControls(doc);

        LinkedHashMap<String, InputInfo> unique = new LinkedHashMap<>();

        for (Element el : controls) {
            String tag = safeLower(el.tagName());
            if (tag.isBlank()) continue;
            if (ALWAYS_IGNORE_TAGS.contains(tag)) continue;

            // Skip hidden inputs unless you want them
            if ("input".equals(tag)) {
                String inputType = normalize(el.attr("type")).toLowerCase(Locale.ROOT);
                if ("hidden".equals(inputType)) continue;
            }

            String type = inferControlType(el);

            String id = normalize(el.id());
            String name = normalize(el.attr("name"));

            String labelText = inferLabelText(el, labelForMap, idToTextMap);

            String identifier = bestLocatorForControl(el);

            ControlMeta meta = inferControlKindAndEditable(el);

            String safeLabel = labelText.replace("|", "\\|");
            String printable = tag + "|"
                    + identifier
                    + (type.isBlank() ? "" : "|type=" + type)
                    + (safeLabel.isBlank() ? "" : "|label=" + safeLabel)
                    + "|kind=" + meta.controlKind
                    + "|editable=" + meta.isEditable;

            String dedupKey = buildControlDedupKey(el, tag, id, name, type);

            unique.putIfAbsent(
                    dedupKey,
                    new InputInfo(
                            tag, id, name, type, labelText, identifier, printable, meta.controlKind, meta.isEditable));
        }

        return new ArrayList<>(unique.values());
    }

    // ----------------- control classification -----------------

    private static final class ControlMeta {
        final String controlKind;
        final boolean isEditable;

        ControlMeta(String controlKind, boolean isEditable) {
            this.controlKind = controlKind;
            this.isEditable = isEditable;
        }
    }

    private static ControlMeta inferControlKindAndEditable(Element el) {
        String tag = safeLower(el.tagName());
        String role = normalize(el.attr("role")).toLowerCase(Locale.ROOT);
        String type = normalize(el.attr("type")).toLowerCase(Locale.ROOT);

        // --- SELECT OPTION targets ---
        if ("option".equals(tag)) {
            return new ControlMeta(CK_SELECT_OPTION, false);
        }
        if ("option".equals(role)) {
            return new ControlMeta(CK_SELECT_OPTION, false);
        }
        // common listbox option patterns (React/Angular)
        if ("li".equals(tag) && ("option".equals(role) || "treeitem".equals(role) || "menuitem".equals(role))) {
            return new ControlMeta(CK_SELECT_OPTION, false);
        }

        // --- TYPE targets (typing) ---
        if ("textarea".equals(tag)) {
            return new ControlMeta(CK_TYPE, true);
        }
        if ("input".equals(tag)) {
            // treat these as typing targets
            if (type.isBlank()
                    || Set.of("text", "email", "password", "search", "tel", "url", "number")
                            .contains(type)) {
                return new ControlMeta(CK_TYPE, true);
            }
            // input that behaves like dropdown opener
            if (Set.of("button", "submit", "reset").contains(type)) {
                return new ControlMeta(CK_OPEN_DROPDOWN, false);
            }
            // default: not editable (e.g. checkbox/radio/date etc.) -> could still be actionable, but not "typing"
            return new ControlMeta(CK_OPEN_DROPDOWN, false);
        }
        if (isContentEditable(el)) {
            return new ControlMeta(CK_TYPE, true);
        }
        if (Set.of("textbox", "searchbox").contains(role)) {
            return new ControlMeta(CK_TYPE, true);
        }

        // --- OPEN DROPDOWN targets ---
        if ("select".equals(tag)) {
            return new ControlMeta(CK_OPEN_DROPDOWN, false);
        }
        if (Set.of("combobox", "listbox", "menu").contains(role)) {
            return new ControlMeta(CK_OPEN_DROPDOWN, false);
        }
        if (!normalize(el.attr("aria-haspopup")).isBlank()) {
            // buttons/divs acting as dropdown triggers
            return new ControlMeta(CK_OPEN_DROPDOWN, false);
        }

        // Fallback: treat as dropdown opener if it has aria-controls+aria-expanded
        if (el.hasAttr("aria-controls") && el.hasAttr("aria-expanded")) {
            return new ControlMeta(CK_OPEN_DROPDOWN, false);
        }

        // Otherwise: if it has tabindex and role, it's often interactive (open/click)
        String tabindex = normalize(el.attr("tabindex"));
        if (!tabindex.isBlank() && !role.isBlank()) {
            return new ControlMeta(CK_OPEN_DROPDOWN, false);
        }

        // Default: OPEN_DROPDOWN (safer than TYPE)
        return new ControlMeta(CK_OPEN_DROPDOWN, false);
    }

    // ----------------- collect controls -----------------

    private static Elements collectEditableControls(Document doc) {
        String nativeSelector = "input, textarea, select, option";

        String ariaSelector = "[role=textbox], [role=searchbox], [role=combobox], [role=listbox], [role=option], "
                + "[role=spinbutton], [role=tree], [role=treeitem], "
                + "[role=grid], [role=row], [role=gridcell], [role=cell], "
                + "[role=menu], [role=menuitem], [role=menuitemcheckbox], [role=menuitemradio]";

        String editableSelector = "[contenteditable=true], [contenteditable=''], [contenteditable=yes]";

        String frameworkHints = "[aria-haspopup=listbox], [aria-haspopup=menu], [aria-controls][aria-expanded]";

        Elements controls = new Elements();
        controls.addAll(doc.select(nativeSelector));
        controls.addAll(doc.select(ariaSelector));
        controls.addAll(doc.select(editableSelector));
        controls.addAll(doc.select(frameworkHints));

        LinkedHashSet<Element> ordered = new LinkedHashSet<>(controls);
        return new Elements(ordered);
    }

    private static String inferControlType(Element el) {
        String tag = safeLower(el.tagName());

        if ("input".equals(tag)) return normalize(el.attr("type"));
        if ("textarea".equals(tag)) return "textarea";
        if ("select".equals(tag)) return "select";
        if ("option".equals(tag)) return "option";

        String role = normalize(el.attr("role")).toLowerCase(Locale.ROOT);
        if (!role.isBlank()) return role;

        if (isContentEditable(el)) return "contenteditable";

        if (!normalize(el.attr("aria-haspopup")).isBlank())
            return "aria-haspopup=" + normalize(el.attr("aria-haspopup"));

        return "";
    }

    private static boolean isContentEditable(Element el) {
        String ce = normalize(el.attr("contenteditable")).toLowerCase(Locale.ROOT);
        return "true".equals(ce) || "yes".equals(ce) || (el.hasAttr("contenteditable") && ce.isBlank());
    }

    private static String buildControlDedupKey(Element el, String tag, String id, String name, String type) {
        if (!id.isBlank()) return tag + "#id=" + id;

        if (!name.isBlank()) return tag + "#name=" + name;

        String role = normalize(el.attr("role"));
        if (!role.isBlank()) return tag + "#role=" + role + "#type=" + type;

        String testId = normalize(el.attr("data-testid"));
        if (!testId.isBlank()) return tag + "#data-testid=" + testId;

        String ariaLabel = normalize(el.attr("aria-label"));
        String placeholder = normalize(el.attr("placeholder"));
        String text = normalize(el.ownText());

        return tag + "#fallback=" + type + "|" + placeholder + "|" + ariaLabel + "|" + text;
    }

    // ----------------- label inference -----------------

    private static Map<String, String> buildLabelForMap(Document doc) {
        Map<String, String> map = new HashMap<>();
        for (Element label : doc.select("label[for]")) {
            String key = normalize(label.attr("for"));
            String val = normalize(label.text());
            if (!key.isBlank() && !val.isBlank()) {
                map.putIfAbsent(key, val);
            }
        }
        return map;
    }

    private static Map<String, String> buildIdToTextMap(Document doc) {
        Map<String, String> map = new HashMap<>();
        for (Element el : doc.select("[id]")) {
            String id = normalize(el.id());
            if (id.isBlank()) continue;

            String text = normalize(el.ownText());
            if (!isGoodLabel(text)) {
                text = normalize(el.text());
            }

            if (isGoodLabel(text)) {
                map.putIfAbsent(id, text);
            }
        }
        return map;
    }

    private static String inferLabelText(
            Element inputEl, Map<String, String> labelForMap, Map<String, String> idToTextMap) {

        String id = normalize(inputEl.id());
        if (!id.isBlank()) {
            String label = labelForMap.get(id);
            if (isGoodLabel(label)) return shorten(label, 80);
        }

        String ariaLabelledBy = normalize(inputEl.attr("aria-labelledby"));
        if (!ariaLabelledBy.isBlank()) {
            for (String token : ariaLabelledBy.split("\\s+")) {
                String refText = idToTextMap.get(token.trim());
                if (isGoodLabel(refText)) return shorten(refText, 80);
            }
        }

        String ariaLabel = normalize(inputEl.attr("aria-label"));
        if (isGoodLabel(ariaLabel)) return shorten(ariaLabel, 80);

        String placeholder = normalize(inputEl.attr("placeholder"));
        if (isGoodLabel(placeholder)) return shorten(placeholder, 80);

        String nearest = findNearestPrecedingLabelLikeText(inputEl);
        if (isGoodLabel(nearest)) return shorten(nearest, 80);

        String name = normalize(inputEl.attr("name"));
        if (isGoodLabel(name)) return shorten(name, 80);

        return "";
    }

    private static String findNearestPrecedingLabelLikeText(Element inputEl) {
        Element cursor = inputEl;

        final int maxParentHops = 5;
        final int maxSiblingScan = 8;

        for (int hop = 0; hop <= maxParentHops && cursor != null; hop++) {
            String fromSiblings = scanPreviousSiblingsForText(cursor, maxSiblingScan);
            if (isGoodLabel(fromSiblings)) return fromSiblings;

            cursor = cursor.parent();
        }
        return "";
    }

    private static String scanPreviousSiblingsForText(Element start, int maxScan) {
        Element sib = start.previousElementSibling();
        int scanned = 0;

        while (sib != null && scanned < maxScan) {
            String tag = safeLower(sib.tagName());

            if ("label".equals(tag)) {
                String t = normalize(sib.text());
                if (isGoodLabel(t)) return t;
            }

            if (tag.equals("div")
                    || tag.equals("span")
                    || tag.equals("p")
                    || tag.equals("strong")
                    || tag.equals("em")
                    || tag.equals("small")
                    || tag.equals("legend")) {

                String own = normalize(sib.ownText());
                if (isGoodLabel(own)) return own;

                String full = normalize(sib.text());
                if (isGoodLabel(full)) return full;
            }

            String nested = findLastLabelLikeTextInside(sib);
            if (isGoodLabel(nested)) return nested;

            sib = sib.previousElementSibling();
            scanned++;
        }

        return "";
    }

    private static String findLastLabelLikeTextInside(Element container) {
        Elements labels = container.select("label");
        for (int i = labels.size() - 1; i >= 0; i--) {
            String t = normalize(labels.get(i).text());
            if (isGoodLabel(t)) return t;
        }

        Elements nodes = container.select("span, div, p, strong, em, small, legend");
        for (int i = nodes.size() - 1; i >= 0; i--) {
            Element e = nodes.get(i);

            String t = normalize(e.ownText());
            if (!isGoodLabel(t)) {
                t = normalize(e.text());
            }
            if (isGoodLabel(t)) return t;
        }

        return "";
    }

    private static boolean isGoodLabel(String s) {
        String t = normalize(s);
        if (t.isBlank()) return false;
        if (t.length() > 90) return false;
        if (t.contains("{") || t.contains("}") || t.contains(";")) return false;
        if (t.replaceAll("[\\p{Punct}\\s]+", "").length() < 2) return false;

        String low = t.toLowerCase(Locale.ROOT);
        if (low.equals("javascript")) return false;
        if (low.contains("all rights reserved")) return false;
        if (low.contains("©")) return false;

        if (low.contains("cookie")) return false;
        if (low.contains("privacy")) return false;
        if (low.contains("terms")) return false;

        return true;
    }

    // ----------------- locator / formatting -----------------

    private static String bestLocatorForControl(Element el) {
        String id = normalize(el.id());
        if (!id.isBlank()) return "id=" + shorten(id, 90);

        String name = normalize(el.attr("name"));
        if (!name.isBlank()) return "name=" + shorten(name, 90);

        String testId = normalize(el.attr("data-testid"));
        if (!testId.isBlank()) return "data-testid=" + shorten(testId, 90);

        String role = normalize(el.attr("role"));
        if (!role.isBlank()) return "role=" + shorten(role, 90);

        String aria = normalize(el.attr("aria-label"));
        if (!aria.isBlank()) return "aria-label=" + shorten(aria, 90);

        String labelledBy = normalize(el.attr("aria-labelledby"));
        if (!labelledBy.isBlank()) return "aria-labelledby=" + shorten(labelledBy, 90);

        String placeholder = normalize(el.attr("placeholder"));
        if (!placeholder.isBlank()) return "placeholder=" + shorten(placeholder, 90);

        if ("option".equalsIgnoreCase(el.tagName())) {
            String val = normalize(el.attr("value"));
            if (!val.isBlank()) return "value=" + shorten(val, 90);
            String txt = normalize(el.text());
            if (!txt.isBlank()) return "text=" + shorten(txt, 60);
        }

        String text = normalize(el.ownText());
        if (!text.isBlank()) return "text=" + shorten(text, 60);

        return "(no-id)";
    }

    private static String bestIdentifierForAny(Element el, String tag) {
        List<String> keys = List.of(
                "id",
                "name",
                "data-testid",
                "data-test",
                "data-test-id",
                "aria-label",
                "aria-labelledby",
                "role",
                "title",
                "href",
                "src",
                "placeholder",
                "alt");

        for (String k : keys) {
            String v = normalize(el.attr(k));
            if (!v.isBlank()) return k + "=" + shorten(v, 90);
        }

        String text = normalize(el.ownText());
        if (!text.isBlank()) return "text=" + shorten(text, 60);

        return "(no-id)";
    }

    private static boolean isNoisyContainer(Element el, String tag) {
        boolean isContainer = tag.equals("div")
                || tag.equals("span")
                || tag.equals("section")
                || tag.equals("article")
                || tag.equals("nav")
                || tag.equals("main")
                || tag.equals("aside")
                || tag.equals("footer")
                || tag.equals("p");

        if (!isContainer) return false;

        if (hasAnyAttr(el, "id", "name", "data-testid", "data-test", "aria-label", "role", "href")) {
            return false;
        }

        String text = normalize(el.ownText());
        return text.isBlank() || text.length() < 3;
    }

    private static boolean hasAnyAttr(Element el, String... attrs) {
        for (String a : attrs) {
            if (!normalize(el.attr(a)).isBlank()) return true;
        }
        return false;
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return s.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
    }

    private static String shorten(String s, int maxLen) {
        if (s == null) return "";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen - 1) + "…";
    }

    private static String safeLower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).trim();
    }

    // ----------------- NEW: merged list -----------------

    public static List<InputInfo> listAllRelevantElements(WebDriver driver) {
        LinkedHashMap<String, InputInfo> result = new LinkedHashMap<>();

        // 1) Real inputs / controls
        List<InputInfo> inputs = listInputsWithLabelsFromPageSource(driver);
        for (InputInfo in : inputs) {
            String key = "input:" + safeLower(in.tag()) + ":" + normalize(in.identifier());
            result.putIfAbsent(key, in);
        }

        // 2) Important non-input elements -> mapped to InputInfo
        //        List<String> important = listImportantElementsFromPageSource(driver);
        //        for (String s : important) {
        //            int sep = s.indexOf(" - ");
        //            String tag = sep > 0 ? s.substring(0, sep) : "";
        //            String ident = sep > 0 ? s.substring(sep + 3) : s;
        //
        //            InputInfo pseudo = new InputInfo(
        //                    tag,
        //                    "", // id
        //                    "", // name
        //                    "", // type
        //                    "", // labelText
        //                    ident, // identifier
        //                    "", // printable (unused)
        //                    CK_OPEN_DROPDOWN, // default
        //                    false // isEditable
        //                    );
        //
        //            String key = "important:" + safeLower(tag) + ":" + normalize(ident);
        //            result.putIfAbsent(key, pseudo);
        //        }

        return new ArrayList<>(result.values());
    }
}
