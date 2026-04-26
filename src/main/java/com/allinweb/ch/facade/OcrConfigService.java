package com.allinweb.ch.facade;

import com.allinweb.ch.model.OcrConfig;
import com.allinweb.ch.model.OcrConfigDefaults;
import com.allinweb.ch.model.OcrConfigParam;
import com.allinweb.ch.model.OcrConfigProfile;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

/**
 * Caches resolved {@link OcrConfig} per (homebankingId, homeUrlId) key for the JVM lifetime.
 * Explicit {@link #invalidateAll()} / {@link #invalidateForProfile(int)} after UI edits.
 */
@Slf4j
public class OcrConfigService {

    private static volatile OcrConfigService instance;
    private final OcrConfigRepository repo = OcrConfigRepository.getInstance();
    private final ConcurrentHashMap<String, OcrConfig> cache = new ConcurrentHashMap<>();

    public static OcrConfigService getInstance() {
        if (instance == null) {
            synchronized (OcrConfigService.class) {
                if (instance == null) instance = new OcrConfigService();
            }
        }
        return instance;
    }

    private OcrConfigService() {
        // Fill in any canonical params missing from the default profile (lets us add new
        // knobs to OcrConfigDefaults without writing a new DB migration). Best-effort —
        // on first call the DB tables may not yet exist; catch and continue.
        try {
            reconcileDefaultProfile();
        } catch (Throwable t) {
            log.debug("reconcileDefaultProfile deferred: {}", t.getMessage());
        }
    }

    /**
     * Insert any {@link OcrConfigDefaults#CANONICAL} entries missing from the default profile.
     * No-op when every canonical key is already present. Invalidates the resolve cache if anything was added.
     */
    public void reconcileDefaultProfile() {
        OcrConfigProfile def = repo.findProfileByName("default");
        if (def == null) {
            log.debug("reconcileDefaultProfile: no default profile in DB yet");
            return;
        }
        List<OcrConfigParam> existing = repo.listParamsForProfile(def.getId());
        Set<String> have = new HashSet<>();
        for (OcrConfigParam p : existing) have.add(p.getCategory() + "." + p.getName());

        int added = 0;
        for (OcrConfigParam canon : OcrConfigDefaults.CANONICAL) {
            String key = canon.getCategory() + "." + canon.getName();
            if (have.contains(key)) continue;
            OcrConfigParam row = new OcrConfigParam(
                    null, def.getId(), canon.getCategory(), canon.getName(), canon.getValueType(), canon.getValue());
            try {
                repo.upsertParam(row);
                added++;
            } catch (SQLException e) {
                log.warn("reconcileDefaultProfile upsert '{}' failed: {}", key, e.getMessage());
            }
        }
        if (added > 0) {
            log.info("reconcileDefaultProfile: added {} new canonical param(s) to default profile", added);
            invalidateAll();
        }
    }

    /** Returns the list of canonical "category.name" keys that are absent from {@code cfg}. */
    public List<String> detectMissingCanonicalKeys(OcrConfig cfg) {
        List<String> missing = new ArrayList<>();
        if (cfg == null) return missing;
        Set<String> have = new HashSet<>();
        for (OcrConfigParam p : cfg.allParams()) have.add(p.getCategory() + "." + p.getName());
        for (OcrConfigParam canon : OcrConfigDefaults.CANONICAL) {
            String key = canon.getCategory() + "." + canon.getName();
            if (!have.contains(key)) missing.add(key);
        }
        return missing;
    }

    /** Resolve the active config for the given scope; falls back to default profile if no scope match. */
    public OcrConfig resolveFor(Integer homebankingId, Integer homeUrlId) {
        String key = keyOf(homebankingId, homeUrlId);
        return cache.computeIfAbsent(key, k -> loadFresh(homebankingId, homeUrlId));
    }

    /** Bypass cache; forces a fresh DB read. */
    public OcrConfig loadFresh(Integer homebankingId, Integer homeUrlId) {
        OcrConfigProfile profile = repo.resolveActive(homebankingId, homeUrlId);
        if (profile == null) {
            log.warn(
                    "No OCR profile found (homebankingId={}, homeUrlId={}); pipeline will use hardcoded fallbacks",
                    homebankingId,
                    homeUrlId);
            return null;
        }
        OcrConfig cfg = repo.loadFull(profile);
        log.info(
                "OCR profile resolved: name='{}' id={} scope(hbId={}, homeUrlId={})",
                profile.getName(),
                profile.getId(),
                profile.getHomebankingId(),
                profile.getHomeUrlId());
        return cfg;
    }

    public void invalidateAll() {
        cache.clear();
    }

    public void invalidateForProfile(int profileId) {
        // Cheap + safe: nuke everything. Profiles can be shared across scopes.
        cache.clear();
    }

    private String keyOf(Integer hbId, Integer homeUrlId) {
        return (hbId == null ? "_" : String.valueOf(hbId)) + ":"
                + (homeUrlId == null ? "_" : String.valueOf(homeUrlId));
    }
}
