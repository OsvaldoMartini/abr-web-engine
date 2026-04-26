package com.allinweb.ch.facade;

import com.allinweb.ch.model.AttributeData;
import com.allinweb.ch.model.ElementDTO;
import com.allinweb.ch.model.ElementLocatorEntity;
import com.allinweb.ch.model.ElementLocatorRenameEntity;
import com.allinweb.ch.model.OcrConfig;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * JDBC access for the Roadmap 3 locator tables.
 *
 * <p>{@link #upsertOnPick(ElementDTO, Integer, Integer)} is the hook called from SEARCH_TOOL
 * and {@code PerformListElements.runScan} after the text resolver runs. First sight of a
 * (hbId, homeUrlId, definedName) tuple inserts a row with all {@code *_original} columns
 * populated; every subsequent sight rewrites the {@code *_current} columns and bumps
 * {@code pick_count} + {@code updated_at}.
 */
@Slf4j
public class ElementLocatorRepository {

    private static volatile ElementLocatorRepository instance;
    private static final PerformDBEngine performDBEngine = PerformDBEngine.getInstance();
    private static final org.slf4j.Logger logScanner = org.slf4j.LoggerFactory.getLogger("com.allinweb.scanner");

    public static ElementLocatorRepository getInstance() {
        if (instance == null) {
            synchronized (ElementLocatorRepository.class) {
                if (instance == null) instance = new ElementLocatorRepository();
            }
        }
        return instance;
    }

    private ElementLocatorRepository() {}

    // ── Read ─────────────────────────────────────────────────────────────

    public ElementLocatorEntity findByKey(Integer homebankingId, Integer homeUrlId, String definedName) {
        if (definedName == null || definedName.isBlank()) return null;
        String sql = "SELECT * FROM element_locator"
                + " WHERE defined_name = ?"
                + " AND " + nullableEq("homebanking_id", homebankingId)
                + " AND " + nullableEq("home_url_id", homeUrlId);
        try (Connection conn = performDBEngine.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, definedName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            log.warn("findByKey({}, {}, {}) failed: {}", homebankingId, homeUrlId, definedName, e.getMessage());
        }
        return null;
    }

    public List<ElementLocatorEntity> listForScope(Integer homebankingId, Integer homeUrlId) {
        List<ElementLocatorEntity> out = new ArrayList<>();
        String sql = "SELECT * FROM element_locator"
                + " WHERE " + nullableEq("homebanking_id", homebankingId)
                + " AND " + nullableEq("home_url_id", homeUrlId)
                + " ORDER BY defined_name";
        try (Connection conn = performDBEngine.getConnection();
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(mapRow(rs));
        } catch (SQLException e) {
            log.warn("listForScope({}, {}) failed: {}", homebankingId, homeUrlId, e.getMessage());
        }
        return out;
    }

    // ── Upsert ───────────────────────────────────────────────────────────

    /**
     * Insert the locator on first sight, or update {@code *_current} fields on subsequent picks.
     * No-op when the DTO has no resolved {@code definedName} (Phase 3a should always populate it).
     *
     * <p>Honours three skip filters from the active OcrConfig profile:
     * {@code output.skip_hidden_inputs} (drops {@code type="hidden"}),
     * {@code output.skip_label_only_elements} (drops {@code <label>} tags),
     * {@code output.min_defined_name_length} (drops slugs shorter than the threshold).
     * Defaults are all permissive — existing profiles see no change until they opt in.
     */
    public void upsertOnPick(ElementDTO dto, Integer homebankingId, Integer homeUrlId) {
        if (dto == null) return;
        String definedName = dto.getDefinedName();
        if (definedName == null || definedName.isBlank()) {
            log.debug("upsertOnPick: skipping element with empty definedName (xpath={})", dto.getXPath());
            return;
        }

        // Apply the DOM-First skip filters (Phase 3a quality follow-up).
        OcrConfig cfg = OcrConfigService.getInstance().resolveFor(homebankingId, homeUrlId);
        if (shouldSkip(cfg, dto, definedName)) return;

        ElementLocatorEntity existing = findByKey(homebankingId, homeUrlId, definedName);
        try {
            if (existing == null) {
                insertNew(dto, homebankingId, homeUrlId);
                logScanner.info(
                        "LOCATOR INSERT — hbId={} homeUrlId={} defined='{}' xpath={}",
                        homebankingId,
                        homeUrlId,
                        definedName,
                        truncate(dto.getXPath(), 80));
            } else {
                updateCurrent(existing, dto);
                logScanner.info(
                        "LOCATOR UPDATE — hbId={} homeUrlId={} defined='{}' picks={}",
                        homebankingId,
                        homeUrlId,
                        definedName,
                        existing.getPickCount() == null ? 1 : existing.getPickCount() + 1);
            }
        } catch (SQLException e) {
            log.warn(
                    "upsertOnPick failed for hbId={} homeUrlId={} defined={}: {}",
                    homebankingId,
                    homeUrlId,
                    definedName,
                    e.getMessage());
        }
    }

    /** Convenience: bulk version. */
    public void upsertOnPickBatch(ElementDTO[] elements, Integer homebankingId, Integer homeUrlId) {
        if (elements == null) return;
        int n = 0;
        for (ElementDTO e : elements) {
            if (e == null) continue;
            upsertOnPick(e, homebankingId, homeUrlId);
            n++;
        }
        logScanner.info("LOCATOR BATCH — hbId={} homeUrlId={} elements={}", homebankingId, homeUrlId, n);
    }

    private void insertNew(ElementDTO dto, Integer hbId, Integer homeUrlId) throws SQLException {
        String sql = "INSERT INTO element_locator ("
                + "homebanking_id, home_url_id, defined_name,"
                + " x_path_original, custom_x_path_original, css_selector_original,"
                + " attrib_id_original, attrib_name_original, tag_name_original,"
                + " some_text_original, coords_original, ocr_text_original,"
                + " iframe_xpath_original, shadow_host_original,"
                + " x_path_current, custom_x_path_current, css_selector_current,"
                + " attrib_id_current, attrib_name_current, tag_name_current,"
                + " some_text_current, coords_current, ocr_text_current,"
                + " iframe_xpath_current, shadow_host_current,"
                + " pick_count"
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)";
        try (Connection conn = performDBEngine.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            setNullableInt(ps, i++, hbId);
            setNullableInt(ps, i++, homeUrlId);
            ps.setString(i++, dto.getDefinedName());
            // originals
            ps.setString(i++, nz(dto.getXPath()));
            setNullableString(ps, i++, dto.getCustomXPath());
            setNullableString(ps, i++, dto.getCssSelector());
            setNullableString(ps, i++, dto.getAttribId());
            setNullableString(ps, i++, dto.getAttribName());
            setNullableString(ps, i++, dto.getTagName());
            setNullableString(ps, i++, dto.getSomeText());
            setNullableString(ps, i++, dto.getCoordinates());
            setNullableString(
                    ps, i++, ""); // ocr_text_original: not on the DTO, will be added when Phase 3c reads it back
            setNullableString(ps, i++, dto.getIFrameXPath());
            setNullableString(ps, i++, dto.getShadowHost());
            // current = same as original on first sight
            ps.setString(i++, nz(dto.getXPath()));
            setNullableString(ps, i++, dto.getCustomXPath());
            setNullableString(ps, i++, dto.getCssSelector());
            setNullableString(ps, i++, dto.getAttribId());
            setNullableString(ps, i++, dto.getAttribName());
            setNullableString(ps, i++, dto.getTagName());
            setNullableString(ps, i++, dto.getSomeText());
            setNullableString(ps, i++, dto.getCoordinates());
            setNullableString(ps, i++, "");
            setNullableString(ps, i++, dto.getIFrameXPath());
            setNullableString(ps, i++, dto.getShadowHost());
            ps.executeUpdate();
        }
    }

    private void updateCurrent(ElementLocatorEntity existing, ElementDTO dto) throws SQLException {
        // Phase 3c-i: detect drift before overwriting *_current. One audit row per changed field.
        detectAndLogDrift(existing, dto);

        String sql = "UPDATE element_locator SET"
                + " x_path_current = ?,"
                + " custom_x_path_current = ?,"
                + " css_selector_current = ?,"
                + " attrib_id_current = ?,"
                + " attrib_name_current = ?,"
                + " tag_name_current = ?,"
                + " some_text_current = ?,"
                + " coords_current = ?,"
                + " iframe_xpath_current = ?,"
                + " shadow_host_current = ?,"
                + " pick_count = pick_count + 1,"
                + " updated_at = CURRENT_TIMESTAMP"
                + " WHERE id = ?";
        try (Connection conn = performDBEngine.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, nz(dto.getXPath()));
            setNullableString(ps, i++, dto.getCustomXPath());
            setNullableString(ps, i++, dto.getCssSelector());
            setNullableString(ps, i++, dto.getAttribId());
            setNullableString(ps, i++, dto.getAttribName());
            setNullableString(ps, i++, dto.getTagName());
            setNullableString(ps, i++, dto.getSomeText());
            setNullableString(ps, i++, dto.getCoordinates());
            setNullableString(ps, i++, dto.getIFrameXPath());
            setNullableString(ps, i++, dto.getShadowHost());
            ps.setLong(i++, existing.getId());
            ps.executeUpdate();
        }
    }

    // ── Drift detection (Phase 3c-i) ─────────────────────────────────────

    /** Compare each new pick value against the row's existing {@code *_current}; emit one rename row per change. */
    private void detectAndLogDrift(ElementLocatorEntity existing, ElementDTO dto) {
        if (existing == null || existing.getId() == null || dto == null) return;

        maybeLogChange(existing.getId(), "XPATH", "x_path", existing.getXPathCurrent(), dto.getXPath());
        maybeLogChange(
                existing.getId(), "XPATH", "custom_x_path", existing.getCustomXPathCurrent(), dto.getCustomXPath());
        maybeLogChange(
                existing.getId(), "OTHER", "css_selector", existing.getCssSelectorCurrent(), dto.getCssSelector());
        maybeLogChange(existing.getId(), "ATTRIB_ID", "attrib_id", existing.getAttribIdCurrent(), dto.getAttribId());
        maybeLogChange(
                existing.getId(), "ATTRIB_NAME", "attrib_name", existing.getAttribNameCurrent(), dto.getAttribName());
        maybeLogChange(existing.getId(), "TEXT", "some_text", existing.getSomeTextCurrent(), dto.getSomeText());
        maybeLogChange(existing.getId(), "COORDS", "coords", existing.getCoordsCurrent(), dto.getCoordinates());
        maybeLogChange(
                existing.getId(), "OTHER", "iframe_xpath", existing.getIframeXPathCurrent(), dto.getIFrameXPath());
        maybeLogChange(existing.getId(), "OTHER", "shadow_host", existing.getShadowHostCurrent(), dto.getShadowHost());
    }

    private void maybeLogChange(long locatorId, String changeType, String fieldName, String oldVal, String newVal) {
        String safeOld = oldVal == null ? "" : oldVal;
        String safeNew = newVal == null ? "" : newVal;
        if (safeOld.equals(safeNew)) return;

        ElementLocatorRenameEntity row = new ElementLocatorRenameEntity();
        row.setLocatorId(locatorId);
        row.setChangeType(changeType);
        row.setFieldName(fieldName);
        row.setOldValue(safeOld);
        row.setNewValue(safeNew);
        row.setRecoveryStrategy("DETECTED_AT_PICK");
        insertRename(row);

        logScanner.info(
                "LOCATOR DRIFT — id={} {} {}: '{}' -> '{}'",
                locatorId,
                changeType,
                fieldName,
                truncate(safeOld, 50),
                truncate(safeNew, 50));
    }

    // ── Delete (Phase 3 cleanup) ─────────────────────────────────────────

    /**
     * Delete a single locator row. Cascade-deletes its rename audit rows on Postgres / SQLite
     * (FK ON DELETE CASCADE); on Access we delete the rename rows manually first because
     * Access doesn't enforce foreign keys.
     */
    public void deleteLocator(long id) throws SQLException {
        try (Connection conn = performDBEngine.getConnection()) {
            try (PreparedStatement ps =
                    conn.prepareStatement("DELETE FROM element_locator_rename WHERE locator_id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM element_locator WHERE id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
        }
    }

    // ── Rename audit ─────────────────────────────────────────────────────

    /** Phase 3c hook — write a single audit row when drift is detected. */
    public void insertRename(ElementLocatorRenameEntity row) {
        if (row == null || row.getLocatorId() == null) return;
        String sql = "INSERT INTO element_locator_rename"
                + " (locator_id, change_type, field_name, old_value, new_value, match_confidence, recovery_strategy)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = performDBEngine.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, row.getLocatorId());
            ps.setString(2, row.getChangeType());
            setNullableString(ps, 3, row.getFieldName());
            setNullableString(ps, 4, row.getOldValue());
            setNullableString(ps, 5, row.getNewValue());
            if (row.getMatchConfidence() != null) ps.setBigDecimal(6, row.getMatchConfidence());
            else ps.setNull(6, Types.DECIMAL);
            setNullableString(ps, 7, row.getRecoveryStrategy());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warn("insertRename failed: {}", e.getMessage());
        }
    }

    // ── Mapping ──────────────────────────────────────────────────────────

    private ElementLocatorEntity mapRow(ResultSet rs) throws SQLException {
        ElementLocatorEntity e = new ElementLocatorEntity();
        e.setId(rs.getLong("id"));
        e.setHomebankingId((Integer) rs.getObject("homebanking_id"));
        e.setHomeUrlId((Integer) rs.getObject("home_url_id"));
        e.setDefinedName(rs.getString("defined_name"));
        e.setXPathOriginal(rs.getString("x_path_original"));
        e.setCustomXPathOriginal(rs.getString("custom_x_path_original"));
        e.setCssSelectorOriginal(rs.getString("css_selector_original"));
        e.setAttribIdOriginal(rs.getString("attrib_id_original"));
        e.setAttribNameOriginal(rs.getString("attrib_name_original"));
        e.setTagNameOriginal(rs.getString("tag_name_original"));
        e.setSomeTextOriginal(rs.getString("some_text_original"));
        e.setCoordsOriginal(rs.getString("coords_original"));
        e.setOcrTextOriginal(rs.getString("ocr_text_original"));
        e.setIframeXPathOriginal(rs.getString("iframe_xpath_original"));
        e.setShadowHostOriginal(rs.getString("shadow_host_original"));
        e.setXPathCurrent(rs.getString("x_path_current"));
        e.setCustomXPathCurrent(rs.getString("custom_x_path_current"));
        e.setCssSelectorCurrent(rs.getString("css_selector_current"));
        e.setAttribIdCurrent(rs.getString("attrib_id_current"));
        e.setAttribNameCurrent(rs.getString("attrib_name_current"));
        e.setTagNameCurrent(rs.getString("tag_name_current"));
        e.setSomeTextCurrent(rs.getString("some_text_current"));
        e.setCoordsCurrent(rs.getString("coords_current"));
        e.setOcrTextCurrent(rs.getString("ocr_text_current"));
        e.setIframeXPathCurrent(rs.getString("iframe_xpath_current"));
        e.setShadowHostCurrent(rs.getString("shadow_host_current"));
        Object pc = rs.getObject("pick_count");
        e.setPickCount(pc instanceof Number ? ((Number) pc).intValue() : null);
        e.setCreatedAt(readTs(rs, "created_at"));
        e.setUpdatedAt(readTs(rs, "updated_at"));
        return e;
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /** Apply the configurable skip filters; logs the reason at debug level when skipping. */
    private boolean shouldSkip(OcrConfig cfg, ElementDTO dto, String definedName) {
        if (cfg == null) return false;
        // 1) skip <input type="hidden">
        if (cfg.getBool("output", "skip_hidden_inputs", false)) {
            String type = readAttr(dto, "type");
            if ("hidden".equalsIgnoreCase(type)) {
                log.debug("upsertOnPick: skipping hidden input (xpath={})", dto.getXPath());
                return true;
            }
        }
        // 2) skip <label> tags
        if (cfg.getBool("output", "skip_label_only_elements", false)) {
            if ("label".equalsIgnoreCase(dto.getTagName())) {
                log.debug("upsertOnPick: skipping <label> element (xpath={})", dto.getXPath());
                return true;
            }
        }
        // 3) skip short / nameless definedName
        int minLen = cfg.getInt("output", "min_defined_name_length", 0);
        if (minLen > 0 && definedName.length() < minLen) {
            log.debug(
                    "upsertOnPick: skipping definedName='{}' (length {} < min {})",
                    definedName,
                    definedName.length(),
                    minLen);
            return true;
        }
        return false;
    }

    private static String readAttr(ElementDTO dto, String name) {
        if (dto == null || dto.getAttributeData() == null || name == null) return null;
        for (AttributeData a : dto.getAttributeData()) {
            if (a == null || a.getName() == null) continue;
            if (name.equalsIgnoreCase(a.getName())) return a.getValue();
        }
        return null;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private static String nullableEq(String column, Integer value) {
        // SQLite + most drivers: `col = NULL` never matches. Use `IS NULL` when value is null.
        return value == null ? "(" + column + " IS NULL)" : "(" + column + " = " + value + ")";
    }

    private static void setNullableInt(PreparedStatement ps, int idx, Integer v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.INTEGER);
        else ps.setInt(idx, v);
    }

    private static void setNullableString(PreparedStatement ps, int idx, String v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.VARCHAR);
        else ps.setString(idx, v);
    }

    private static Timestamp readTs(ResultSet rs, String col) {
        try {
            Object o = rs.getObject(col);
            if (o instanceof Timestamp) return (Timestamp) o;
            if (o instanceof String) {
                try {
                    return Timestamp.valueOf(((String) o).replace("T", " ").replace("Z", ""));
                } catch (IllegalArgumentException ignore) {
                    return null;
                }
            }
        } catch (SQLException ignore) {
        }
        return null;
    }

    @SuppressWarnings("unused")
    private static BigDecimal touch() {
        return BigDecimal.ZERO;
    }
}
