package com.allinweb.ch.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Append-only audit row in {@code element_locator_rename}. Phase 3c writes one row per
 * detected drift (xpath/text/attribute change) when the recovery service finds the
 * element via a non-original strategy. Phase 3b creates the table only — no writes yet.
 */
@Data
@NoArgsConstructor
public class ElementLocatorRenameEntity {
    private Long id;
    private Long locatorId;
    private Timestamp observedAt;
    /** TEXT | XPATH | ATTRIB_ID | ATTRIB_NAME | COORDS | OTHER */
    private String changeType;

    private String fieldName;
    private String oldValue;
    private String newValue;
    private BigDecimal matchConfidence; // 0..100
    /** TEXT_EXACT | TEXT_FUZZY | OCR | COORDS | CSS | COMBINED */
    private String recoveryStrategy;
}
