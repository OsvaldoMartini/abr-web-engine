package com.allinweb.ch.facade;

import com.allinweb.ch.model.OcrConfig;
import com.allinweb.ch.model.OcrConfigParam;
import com.allinweb.ch.model.OcrConfigProfile;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/** JDBC access for the OCR configuration system. Dialect-agnostic (uses ANSI SQL only). */
@Slf4j
public class OcrConfigRepository {

    private static volatile OcrConfigRepository instance;
    private static final PerformDBEngine performDBEngine = PerformDBEngine.getInstance();

    public static OcrConfigRepository getInstance() {
        if (instance == null) {
            synchronized (OcrConfigRepository.class) {
                if (instance == null) instance = new OcrConfigRepository();
            }
        }
        return instance;
    }

    private OcrConfigRepository() {}

    // ── Profiles ─────────────────────────────────────────────────────────

    public List<OcrConfigProfile> listProfiles() {
        List<OcrConfigProfile> out = new ArrayList<>();
        String sql = "SELECT id, name, description, homebanking_id, home_url_id, is_default,"
                + " created_at, updated_at FROM ocr_config_profile ORDER BY is_default DESC, name ASC";
        try (Connection conn = performDBEngine.getConnection();
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(mapProfile(rs));
        } catch (SQLException e) {
            log.warn("listProfiles failed: {}", e.getMessage(), e);
        }
        return out;
    }

    public OcrConfigProfile findProfileById(int id) {
        String sql = "SELECT id, name, description, homebanking_id, home_url_id, is_default,"
                + " created_at, updated_at FROM ocr_config_profile WHERE id = ?";
        try (Connection conn = performDBEngine.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapProfile(rs);
            }
        } catch (SQLException e) {
            log.warn("findProfileById({}) failed: {}", id, e.getMessage());
        }
        return null;
    }

    public OcrConfigProfile findProfileByName(String name) {
        String sql = "SELECT id, name, description, homebanking_id, home_url_id, is_default,"
                + " created_at, updated_at FROM ocr_config_profile WHERE name = ?";
        try (Connection conn = performDBEngine.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapProfile(rs);
            }
        } catch (SQLException e) {
            log.warn("findProfileByName({}) failed: {}", name, e.getMessage());
        }
        return null;
    }

    /**
     * Resolve the most-specific active profile for (homebankingId, homeUrlId).
     * Order: matching home_url_id → matching homebanking_id (home_url_id IS NULL) → is_default.
     */
    public OcrConfigProfile resolveActive(Integer homebankingId, Integer homeUrlId) {
        try (Connection conn = performDBEngine.getConnection()) {
            if (homeUrlId != null) {
                OcrConfigProfile p = querySingle(
                        conn,
                        "SELECT id, name, description, homebanking_id, home_url_id, is_default,"
                                + " created_at, updated_at FROM ocr_config_profile"
                                + " WHERE home_url_id = ? ORDER BY updated_at DESC",
                        homeUrlId);
                if (p != null) return p;
            }
            if (homebankingId != null) {
                OcrConfigProfile p = querySingle(
                        conn,
                        "SELECT id, name, description, homebanking_id, home_url_id, is_default,"
                                + " created_at, updated_at FROM ocr_config_profile"
                                + " WHERE homebanking_id = ? AND home_url_id IS NULL ORDER BY updated_at DESC",
                        homebankingId);
                if (p != null) return p;
            }
            // Fallback to default
            try (Statement st = conn.createStatement();
                    ResultSet rs = st.executeQuery(
                            "SELECT id, name, description, homebanking_id, home_url_id, is_default,"
                                    + " created_at, updated_at FROM ocr_config_profile WHERE is_default = 1 OR is_default = TRUE LIMIT 1")) {
                if (rs.next()) return mapProfile(rs);
            } catch (SQLException first) {
                // Retry without LIMIT for Access / SQLServer dialects.
                try (Statement st2 = conn.createStatement();
                        ResultSet rs2 = st2.executeQuery(
                                "SELECT id, name, description, homebanking_id, home_url_id, is_default,"
                                        + " created_at, updated_at FROM ocr_config_profile WHERE is_default = 1 OR is_default = TRUE")) {
                    if (rs2.next()) return mapProfile(rs2);
                }
            }
        } catch (SQLException e) {
            log.warn("resolveActive failed: {}", e.getMessage(), e);
        }
        return null;
    }

    /** Insert a new profile. Returns the generated id. Does NOT insert params. */
    public int insertProfile(OcrConfigProfile p) throws SQLException {
        String sql = "INSERT INTO ocr_config_profile (name, description, homebanking_id, home_url_id, is_default)"
                + " VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = performDBEngine.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getName());
            setNullableString(ps, 2, p.getDescription());
            setNullableInt(ps, 3, p.getHomebankingId());
            setNullableInt(ps, 4, p.getHomeUrlId());
            ps.setBoolean(5, p.isDefault());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        OcrConfigProfile reloaded = findProfileByName(p.getName());
        if (reloaded != null) return reloaded.getId();
        throw new SQLException("Could not retrieve inserted profile id for name=" + p.getName());
    }

    public void updateProfile(OcrConfigProfile p) throws SQLException {
        String sql = "UPDATE ocr_config_profile SET name = ?, description = ?, homebanking_id = ?,"
                + " home_url_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = performDBEngine.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            setNullableString(ps, 2, p.getDescription());
            setNullableInt(ps, 3, p.getHomebankingId());
            setNullableInt(ps, 4, p.getHomeUrlId());
            ps.setInt(5, p.getId());
            ps.executeUpdate();
        }
    }

    /** Bump {@code updated_at} — call after param edits so "Modified" reflects config changes. */
    public void touchProfile(int id) {
        try (Connection conn = performDBEngine.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "UPDATE ocr_config_profile SET updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.debug("touchProfile({}) failed: {}", id, e.getMessage());
        }
    }

    public void deleteProfile(int id) throws SQLException {
        // Profile delete cascades to params via FK (Postgres/SQLite). Access has no FK → delete params first.
        try (Connection conn = performDBEngine.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM ocr_config_param WHERE profile_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM ocr_config_profile WHERE id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        }
    }

    // ── Params ───────────────────────────────────────────────────────────

    public List<OcrConfigParam> listParamsForProfile(int profileId) {
        List<OcrConfigParam> out = new ArrayList<>();
        String sql = "SELECT id, profile_id, category, name, value_type, value"
                + " FROM ocr_config_param WHERE profile_id = ? ORDER BY category, name";
        try (Connection conn = performDBEngine.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, profileId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapParam(rs));
            }
        } catch (SQLException e) {
            log.warn("listParamsForProfile({}) failed: {}", profileId, e.getMessage());
        }
        return out;
    }

    public void upsertParam(OcrConfigParam p) throws SQLException {
        try (Connection conn = performDBEngine.getConnection()) {
            String selSql = "SELECT id FROM ocr_config_param WHERE profile_id = ? AND category = ? AND name = ?";
            Integer existingId = null;
            try (PreparedStatement ps = conn.prepareStatement(selSql)) {
                ps.setInt(1, p.getProfileId());
                ps.setString(2, p.getCategory());
                ps.setString(3, p.getName());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) existingId = rs.getInt(1);
                }
            }
            if (existingId == null) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO ocr_config_param (profile_id, category, name, value_type, value) VALUES (?, ?, ?, ?, ?)")) {
                    ps.setInt(1, p.getProfileId());
                    ps.setString(2, p.getCategory());
                    ps.setString(3, p.getName());
                    ps.setString(4, p.getValueType());
                    ps.setString(5, p.getValue());
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps =
                        conn.prepareStatement("UPDATE ocr_config_param SET value_type = ?, value = ? WHERE id = ?")) {
                    ps.setString(1, p.getValueType());
                    ps.setString(2, p.getValue());
                    ps.setInt(3, existingId);
                    ps.executeUpdate();
                }
            }
        }
    }

    /** Bulk upsert; no transaction wrapper (each call is autoCommit). */
    public void upsertParams(int profileId, List<OcrConfigParam> params) throws SQLException {
        for (OcrConfigParam p : params) {
            p.setProfileId(profileId);
            upsertParam(p);
        }
    }

    // ── Compose a full OcrConfig ─────────────────────────────────────────

    public OcrConfig loadFull(OcrConfigProfile profile) {
        if (profile == null) return null;
        return new OcrConfig(profile, listParamsForProfile(profile.getId()));
    }

    // ── Mapping helpers ──────────────────────────────────────────────────

    private OcrConfigProfile mapProfile(ResultSet rs) throws SQLException {
        OcrConfigProfile p = new OcrConfigProfile();
        p.setId(rs.getInt("id"));
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));
        p.setHomebankingId((Integer) rs.getObject("homebanking_id"));
        p.setHomeUrlId((Integer) rs.getObject("home_url_id"));
        p.setDefault(readBool(rs, "is_default"));
        p.setCreatedAt(readTimestamp(rs, "created_at"));
        p.setUpdatedAt(readTimestamp(rs, "updated_at"));
        return p;
    }

    private OcrConfigParam mapParam(ResultSet rs) throws SQLException {
        return new OcrConfigParam(
                rs.getInt("id"),
                rs.getInt("profile_id"),
                rs.getString("category"),
                rs.getString("name"),
                rs.getString("value_type"),
                rs.getString("value"));
    }

    private boolean readBool(ResultSet rs, String col) throws SQLException {
        Object o = rs.getObject(col);
        if (o instanceof Boolean) return (Boolean) o;
        if (o instanceof Number) return ((Number) o).intValue() != 0;
        if (o instanceof String) {
            String s = ((String) o).trim().toLowerCase();
            return "true".equals(s) || "1".equals(s) || "yes".equals(s);
        }
        return false;
    }

    private Timestamp readTimestamp(ResultSet rs, String col) {
        try {
            Object o = rs.getObject(col);
            if (o instanceof Timestamp) return (Timestamp) o;
            if (o instanceof String) {
                // SQLite returns ISO string — let Timestamp.valueOf handle it when possible.
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

    private OcrConfigProfile querySingle(Connection conn, String sql, int param) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapProfile(rs);
            }
        }
        return null;
    }

    private static void setNullableInt(PreparedStatement ps, int idx, Integer v) throws SQLException {
        if (v == null) ps.setNull(idx, java.sql.Types.INTEGER);
        else ps.setInt(idx, v);
    }

    private static void setNullableString(PreparedStatement ps, int idx, String v) throws SQLException {
        if (v == null) ps.setNull(idx, java.sql.Types.VARCHAR);
        else ps.setString(idx, v);
    }
}
