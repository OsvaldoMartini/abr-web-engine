package com.allinweb.ch.facade;

import com.allinweb.ch.model.ElementLocatorEntity;
import com.allinweb.ch.model.ElementLocatorRenameEntity;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import lombok.extern.slf4j.Slf4j;

/**
 * Engine-side slim port of the Scanner's ElementLocatorRepository (Roadmap 3 Phase 3c-iii cross-repo).
 *
 * <p>The Engine never picks elements (the Scanner does that), so it only needs READ access to
 * {@code element_locator} for recovery lookups + WRITE access to {@code element_locator_rename}
 * for the audit trail when {@link ElementRecoveryService#findOrRecover} resolves an element via
 * a non-XPATH_CURRENT strategy.
 *
 * <p>Tables are created by the Scanner's migration ({@code M20260428_ElementLocator}); the Engine
 * just reads/writes against the shared DB file.
 */
@Slf4j
public class ElementLocatorRepository {

    private static volatile ElementLocatorRepository instance;
    private static final PerformDBEngine performDBEngine = PerformDBEngine.getInstance();

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

    /**
     * Look up the saved locator by its natural key. Returns {@code null} when no row matches
     * or the table doesn't exist yet (older Scanner DB without the migration applied).
     */
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

    // ── Write (audit only) ───────────────────────────────────────────────

    /** Phase 3c hook — write a single audit row when drift is detected during recovery. */
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

    private static String nullableEq(String column, Integer value) {
        return value == null ? "(" + column + " IS NULL)" : "(" + column + " = " + value + ")";
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
}
