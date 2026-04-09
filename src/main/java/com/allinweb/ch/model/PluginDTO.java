package com.allinweb.ch.model;

/**
 * Represents a single plugin entry from the remote manifest.json.
 *
 * JSON shape (must match manifest exactly - Gson field-name mapping):
 * {
 *   "id":          "pageScanner",
 *   "name":        "Page Scanner",
 *   "description": "DOM traversal ...",
 *   "version":     "4.7.1",
 *   "icon":        "🔍",
 *   "size":        "20 KB",
 *   "fileName":    "pageScanner.zip",
 *   "downloadUrl": "/plugins/pageScanner.zip"
 * }
 */
public class PluginDTO {

    private String id;
    private String name;
    private String description;
    private String version;
    private String icon;
    private String size;
    private String fileName;
    private String downloadUrl;

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getVersion() {
        return version;
    }

    public String getIcon() {
        return icon;
    }

    public String getSize() {
        return size;
    }

    public String getFileName() {
        return fileName;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    // ── Setters (required by Gson) ────────────────────────────────────────────

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    /** Display label used in TableView / ListView cells. */
    @Override
    public String toString() {
        return (icon != null ? icon + "  " : "") + name + "  v" + version;
    }
}
