package com.allinweb.ch.model;

import java.util.List;

/**
 * Root wrapper that matches the top-level structure of manifest.json:
 *
 * {
 *   "version": "1.0.0",
 *   "updated": "2026-03-26",
 *   "plugins": [ { ... }, { ... } ]
 * }
 */
public class PluginManifestDTO {

    private String version;
    private String updated;
    private List<PluginDTO> plugins;

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getVersion() {
        return version;
    }

    public String getUpdated() {
        return updated;
    }

    public List<PluginDTO> getPlugins() {
        return plugins;
    }

    // ── Setters (required by Gson) ────────────────────────────────────────────

    public void setVersion(String version) {
        this.version = version;
    }

    public void setUpdated(String updated) {
        this.updated = updated;
    }

    public void setPlugins(List<PluginDTO> plugins) {
        this.plugins = plugins;
    }
}
