package com.allinweb.ch.facade;

import com.allinweb.ch.util.ARPropertyManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;

/**
 * Decrypts and loads encrypted plugin scripts (.enc files) at runtime.
 *
 * <p>Encryption: AES-256-GCM with 12-byte IV and 16-byte auth tag.
 * File format: [IV (12 bytes)] [Auth Tag (16 bytes)] [Encrypted Data]</p>
 *
 * <p>The encryption key is loaded from:
 *   1. System property: {@code arweb.plugin.key}
 *   2. Environment variable: {@code ARWEB_PLUGIN_KEY}
 *   3. Key file: {@code {path_plugins}/plugins.key}
 * </p>
 *
 * <p>Usage by plugin loaders:
 * <pre>
 *   String js = EncryptedPluginLoader.getInstance().loadPlugin("hoverPick/build/hoverPick.min.enc");
 *   // js contains the decrypted JavaScript, ready for Selenium.executeScript()
 * </pre>
 * </p>
 *
 * <p>Build pipeline:
 * <ol>
 *   <li>{@code node build-plugins.js} - esbuild + obfuscate → .min.js</li>
 *   <li>{@code node encrypt-plugins.js} - AES-256-GCM encrypt → .min.enc</li>
 *   <li>Distribute .enc files only (never .min.js)</li>
 *   <li>Java decrypts in memory at runtime → injects into browser</li>
 * </ol>
 * </p>
 */
@Slf4j
public class EncryptedPluginLoader {

    private static volatile EncryptedPluginLoader instance;

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128; // 16 bytes * 8
    private static final int TAG_LENGTH_BYTES = 16;

    /** Cached decrypted scripts - cleared by reloadAll() */
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    /** The AES-256 key - loaded once, kept in memory */
    private byte[] key;

    private EncryptedPluginLoader() {}

    public static EncryptedPluginLoader getInstance() {
        if (instance == null) {
            synchronized (EncryptedPluginLoader.class) {
                if (instance == null) {
                    instance = new EncryptedPluginLoader();
                }
            }
        }
        return instance;
    }

    /**
     * Load and decrypt a plugin script.
     *
     * @param relativePath path relative to plugins folder (e.g. "hoverPick/build/hoverPick.min.enc")
     * @return decrypted JavaScript string
     * @throws PerformPreLoad.PluginLoadException if decryption fails
     */
    public String loadPlugin(String relativePath) {
        // Check cache first
        String cached = cache.get(relativePath);
        if (cached != null) return cached;

        // Only encrypted .enc bundles are supported. Plain .min.js loading
        // has been removed — callers must pass the .enc path.
        if (!relativePath.endsWith(".enc")) {
            throw new PerformPreLoad.PluginLoadException(
                    "Unsupported plugin path: " + relativePath,
                    "Only encrypted .enc bundles are supported.",
                    "Set useNoEncrypted = false in the plugin facade and pass the .enc path.",
                    null);
        }

        // Load key if not yet loaded
        ensureKey();

        // Resolve file path using resolvePluginsDir (with fallback logic)
        String pluginsDir = ARPropertyManager.getInstance().resolvePluginsDir();
        if (pluginsDir == null || pluginsDir.isBlank()) {
            log.error("EncryptedPluginLoader — plugins folder not configured. Set path_plugins in ARWeb.config.");
            throw new PerformPreLoad.PluginLoadException(
                    "Plugins folder not configured",
                    "path_plugins is not set in ARWeb.config",
                    "Open Settings and set the path_plugins property.",
                    null);
        }

        Path pluginsDirPath = Paths.get(pluginsDir);
        String pluginId = relativePath.split("[/\\\\]")[0];

        if (!Files.isDirectory(pluginsDirPath)) {
            log.error("EncryptedPluginLoader — plugins directory does not exist: {}", pluginsDirPath);
            throw new PerformPreLoad.PluginLoadException(
                    "Plugins directory not found",
                    "Directory does not exist: " + pluginsDirPath,
                    "Verify that path_plugins in ARWeb.config points to a valid folder.",
                    null);
        }

        Path encPath = pluginsDirPath.resolve(relativePath);
        Path zipFile = pluginsDirPath.resolve(pluginId + ".zip");

        // Read the encrypted bytes — prefer a loose .enc file if already present
        // (legacy layout), otherwise pull the entry straight out of the .zip in
        // memory. No extraction to disk, ever.
        byte[] fileData = null;
        String source = null;
        if (Files.exists(encPath)) {
            try {
                fileData = Files.readAllBytes(encPath);
                source = encPath.toString();
            } catch (IOException e) {
                throw new PerformPreLoad.PluginLoadException("Failed to read plugin", e.getMessage(), null, null, e);
            }
        } else if (Files.exists(zipFile)) {
            String encEntryName = encPath.getFileName().toString(); // e.g. "hoverPick.min.enc"
            try {
                fileData = readEncFromZip(zipFile, encEntryName);
                source = zipFile.getFileName() + "!/" + encEntryName;
            } catch (IOException e) {
                throw new PerformPreLoad.PluginLoadException(
                        "Failed to read plugin zip",
                        "Error reading " + zipFile.getFileName() + ": " + e.getMessage(),
                        null,
                        null,
                        e);
            }
        }

        if (fileData == null) {
            log.error(
                    "EncryptedPluginLoader — plugin '{}' not found. Looked for {} and {} in {}.",
                    pluginId,
                    encPath.getFileName(),
                    zipFile.getFileName(),
                    pluginsDirPath);

            throw new PerformPreLoad.PluginLoadException(
                    "Plugin not found: " + pluginId,
                    "Expected: " + encPath.toAbsolutePath() + "  or  " + zipFile.toAbsolutePath(),
                    "Plugin '" + pluginId + "' is not installed in: " + pluginsDirPath,
                    "Use the Plugin Update button to download and install plugins.");
        }

        // Decrypt in memory
        try {
            String js = decrypt(fileData);
            cache.put(relativePath, js);
            log.info("EncryptedPluginLoader — decrypted '{}' ({} chars) from {}", pluginId, js.length(), source);
            return js;
        } catch (javax.crypto.AEADBadTagException e) {
            log.error(
                    "EncryptedPluginLoader — decryption key mismatch for plugin '{}'. "
                            + "The org key in ARWeb.lic does not match the key used to encrypt this plugin. "
                            + "Re-download the plugin from the portal or request a new license.",
                    pluginId);
            throw new PerformPreLoad.PluginLoadException(
                    "Plugin key mismatch: " + pluginId,
                    "The encryption key in ARWeb.lic does not match this plugin.",
                    "The plugin was encrypted with a different org key than the one in your license.",
                    "Re-download the plugin from the portal, or request a new license for your organization.",
                    e);
        } catch (Exception e) {
            log.error("EncryptedPluginLoader — failed to decrypt plugin '{}': {}", pluginId, e.getMessage());
            throw new PerformPreLoad.PluginLoadException(
                    "Plugin decryption failed: " + pluginId,
                    "Could not decrypt: " + encPath.getFileName(),
                    "Check that ARWeb.lic is valid and the plugin was downloaded correctly.",
                    null,
                    e);
        }
    }

    /**
     * Clear all cached decrypted scripts.
     * Next loadPlugin() call will re-read and re-decrypt from disk.
     */
    public void reloadAll() {
        cache.clear();
        key = null; // force re-authentication on next load
        log.info("EncryptedPluginLoader - cache and key cleared");
    }

    // ── Decryption ──────────────────────────────────────────────────────────

    private String decrypt(byte[] fileData) throws Exception {
        if (fileData.length < IV_LENGTH + TAG_LENGTH_BYTES) {
            throw new IllegalArgumentException("Encrypted file too short - invalid format");
        }

        // Parse: [IV (12)] [Tag (16)] [Encrypted Data]
        byte[] iv = Arrays.copyOfRange(fileData, 0, IV_LENGTH);
        byte[] tag = Arrays.copyOfRange(fileData, IV_LENGTH, IV_LENGTH + TAG_LENGTH_BYTES);
        byte[] encrypted = Arrays.copyOfRange(fileData, IV_LENGTH + TAG_LENGTH_BYTES, fileData.length);

        // GCM expects tag appended to ciphertext
        byte[] cipherWithTag = new byte[encrypted.length + tag.length];
        System.arraycopy(encrypted, 0, cipherWithTag, 0, encrypted.length);
        System.arraycopy(tag, 0, cipherWithTag, encrypted.length, tag.length);

        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

        byte[] decrypted = cipher.doFinal(cipherWithTag);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    // ── Key loading ─────────────────────────────────────────────────────────

    private void ensureKey() {
        if (key != null) return;

        synchronized (this) {
            if (key != null) return;

            key = PluginKeyManager.getInstance().getPluginKey();

            if (key != null) {
                log.info("EncryptedPluginLoader — key loaded via PluginKeyManager ({} bytes)", key.length);
            } else {
                log.error("EncryptedPluginLoader — no decryption key available. "
                        + "ARWeb.lic may be missing, expired, or does not contain an org key. "
                        + "Encrypted plugins will not load.");
            }
        }
    }

    // ── In-memory ZIP read ──────────────────────────────────────────────────

    /**
     * Stream the zip and return the bytes of the first entry matching the
     * requested file name. Matches by basename so the entry can be at the zip
     * root ({@code hoverPick.min.enc}) or nested ({@code build/hoverPick.min.enc}) —
     * whichever packaging the encryption script produced. Nothing is written
     * to disk.
     */
    private byte[] readEncFromZip(Path zipFile, String entryBasename) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    zis.closeEntry();
                    continue;
                }
                String name = entry.getName();
                int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
                String base = slash >= 0 ? name.substring(slash + 1) : name;
                if (base.equalsIgnoreCase(entryBasename)) {
                    ByteArrayOutputStream buf = new ByteArrayOutputStream(Math.max(1024, (int) entry.getSize()));
                    byte[] chunk = new byte[8192];
                    int len;
                    while ((len = zis.read(chunk)) != -1) buf.write(chunk, 0, len);
                    return buf.toByteArray();
                }
                zis.closeEntry();
            }
        }
        throw new IOException("Entry '" + entryBasename + "' not found in " + zipFile.getFileName());
    }
}
