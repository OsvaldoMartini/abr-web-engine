package com.allinweb.ch.model;

import java.sql.Timestamp;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Row in {@code element_locator}. Natural key: {@code (homebankingId, homeUrlId, definedName)}.
 *
 * <p>Fields ending in {@code Original} are frozen at first sight; fields ending in {@code Current}
 * are rewritten on every pick. Phase 3c will diff the two on read and write rename rows when drift
 * is detected — Phase 3b just keeps both sides in sync.
 */
@Data
@NoArgsConstructor
public class ElementLocatorEntity {
    private Long id;
    private Integer homebankingId;
    private Integer homeUrlId;
    private String definedName;

    // ── Frozen at creation ────────────────────────────────────────────
    private String xPathOriginal;
    private String customXPathOriginal;
    private String cssSelectorOriginal;
    private String attribIdOriginal;
    private String attribNameOriginal;
    private String tagNameOriginal;
    private String someTextOriginal;
    private String coordsOriginal;
    private String ocrTextOriginal;
    private String iframeXPathOriginal;
    private String shadowHostOriginal;

    // ── Mutable, rewritten on every pick ──────────────────────────────
    private String xPathCurrent;
    private String customXPathCurrent;
    private String cssSelectorCurrent;
    private String attribIdCurrent;
    private String attribNameCurrent;
    private String tagNameCurrent;
    private String someTextCurrent;
    private String coordsCurrent;
    private String ocrTextCurrent;
    private String iframeXPathCurrent;
    private String shadowHostCurrent;

    private Integer pickCount;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
