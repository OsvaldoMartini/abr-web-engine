package com.allinweb.ch.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Lightweight string similarity helpers used by {@link com.allinweb.ch.facade.ElementTextResolver}
 * to corroborate DOM-derived text candidates with OCR output, and (later in Roadmap 3) to recover
 * an element when its xPath drifts.
 *
 * <p>All methods are case-insensitive and tolerant of {@code null} inputs.
 */
public final class TextSimilarity {

    private TextSimilarity() {}

    /**
     * Levenshtein-derived similarity ratio in {@code [0..1]}. {@code 1.0} means identical
     * (after lower-case + trim); {@code 0.0} means maximally different.
     */
    public static double levenshteinRatio(String a, String b) {
        if (a == null || b == null) return 0.0;
        String x = a.toLowerCase().trim();
        String y = b.toLowerCase().trim();
        if (x.isEmpty() && y.isEmpty()) return 1.0;
        int maxLen = Math.max(x.length(), y.length());
        if (maxLen == 0) return 1.0;
        return 1.0 - ((double) levenshteinDistance(x, y) / maxLen);
    }

    /** Classic Levenshtein edit distance, two-row optimised. */
    public static int levenshteinDistance(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";
        int n = a.length();
        int m = b.length();
        if (n == 0) return m;
        if (m == 0) return n;
        int[] prev = new int[m + 1];
        int[] cur = new int[m + 1];
        for (int j = 0; j <= m; j++) prev[j] = j;
        for (int i = 1; i <= n; i++) {
            cur[0] = i;
            char ai = a.charAt(i - 1);
            for (int j = 1; j <= m; j++) {
                int cost = ai == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = cur;
            cur = tmp;
        }
        return prev[m];
    }

    /**
     * Token-set Jaccard ratio in {@code [0..1]}: split on non-word chars, lower-case,
     * compute |intersection| / |union|. Robust to word-order shuffles and tiny OCR misreads
     * that affect a single token rather than character-level edits.
     */
    public static double tokenSetRatio(String a, String b) {
        if (a == null || b == null) return 0.0;
        Set<String> ta = tokenize(a);
        Set<String> tb = tokenize(b);
        if (ta.isEmpty() && tb.isEmpty()) return 1.0;
        if (ta.isEmpty() || tb.isEmpty()) return 0.0;
        Set<String> inter = new HashSet<>(ta);
        inter.retainAll(tb);
        Set<String> union = new HashSet<>(ta);
        union.addAll(tb);
        return union.isEmpty() ? 0.0 : (double) inter.size() / union.size();
    }

    /** True when either levenshtein or token-set agreement crosses {@code threshold}. */
    public static boolean fuzzyMatches(String a, String b, double threshold) {
        return levenshteinRatio(a, b) >= threshold || tokenSetRatio(a, b) >= threshold;
    }

    private static Set<String> tokenize(String s) {
        if (s == null) return Collections.emptySet();
        String[] parts = s.toLowerCase().split("\\W+");
        Set<String> out = new HashSet<>();
        for (String p : parts) {
            if (p != null && !p.isBlank()) out.add(p);
        }
        return out;
    }

    /**
     * Convert an HTML id / name into a human label.
     * Examples: {@code firstName -> "First Name"},
     *           {@code user_id -> "User Id"},
     *           {@code home-page -> "Home Page"}.
     */
    public static String humanize(String identifier) {
        if (identifier == null || identifier.isBlank()) return "";
        String spaced = identifier
                .replaceAll("([a-z])([A-Z])", "$1 $2") // camelCase split
                .replaceAll("([A-Z])([A-Z][a-z])", "$1 $2") // ABCWord -> ABC Word
                .replaceAll("[_\\-]+", " ") // snake / kebab
                .replaceAll("\\s+", " ")
                .trim();
        if (spaced.isEmpty()) return "";
        // Capitalize each word's first letter.
        StringBuilder sb = new StringBuilder();
        for (String word : spaced.split(" ")) {
            if (word.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) sb.append(word.substring(1));
        }
        return sb.toString();
    }

    /** Lower-case slug suitable for {@code ElementDTO.definedName}. {@code "User Number" -> "user_number"}. */
    public static String slug(String text) {
        if (text == null) return "";
        String s = text.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return s.length() > 64 ? s.substring(0, 64) : s;
    }

    /** Append a numeric suffix until the result is not in {@code used}. */
    public static String uniquify(String base, Set<String> used) {
        String b = (base == null || base.isBlank()) ? "element" : base;
        if (!used.contains(b)) return b;
        int i = 2;
        while (used.contains(b + "_" + i)) i++;
        return b + "_" + i;
    }

    // Visible for tests
    static Set<String> tokens(String s) {
        return tokenize(s);
    }

    // Suppresses an unused-warning some IDEs flag for the import.
    @SuppressWarnings("unused")
    private static void touch() {
        Arrays.asList("a", "b");
    }
}
