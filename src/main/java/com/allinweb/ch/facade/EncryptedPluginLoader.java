package com.allinweb.ch.facade;

import com.allinweb.ch.util.ARPropertyEnum;
import com.allinweb.ch.util.ARPropertyManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
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
 *   <li>{@code node build-plugins.js} — esbuild + obfuscate → .min.js</li>
 *   <li>{@code node encrypt-plugins.js} — AES-256-GCM encrypt → .min.enc</li>
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

    /** Cached decrypted scripts — cleared by reloadAll() */
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    /** The AES-256 key — loaded once, kept in memory */
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

        // Load key if not yet loaded
        ensureKey();

        // Resolve file path
        String pluginsDir = ARPropertyManager.getInstance().getProperty(ARPropertyEnum.PATH_PLUGINS);
        if (pluginsDir == null || pluginsDir.isBlank()) {
            throw new PerformPreLoad.PluginLoadException(
                    "Plugins folder not configured", "path_plugins is not set in ARWeb.config", null, null);
        }

        Path encPath = Paths.get(pluginsDir).resolve(relativePath);
        if (!Files.exists(encPath)) {
            // Fallback: try plain .min.js (for backward compatibility)
            String jsPath = relativePath.replace(".min.enc", ".min.js");
            Path plainPath = Paths.get(pluginsDir).resolve(jsPath);
            if (Files.exists(plainPath)) {
                log.info("EncryptedPluginLoader — no .enc found, falling back to plain .min.js: {}", jsPath);
                try {
                    String js = Files.readString(plainPath, StandardCharsets.UTF_8);
                    cache.put(relativePath, js);
                    return js;
                } catch (IOException e) {
                    throw new PerformPreLoad.PluginLoadException(
                            "Failed to read plugin", e.getMessage(), null, null, e);
                }
            }
            throw new PerformPreLoad.PluginLoadException(
                    "Encrypted plugin not found", "File not found: " + encPath.toAbsolutePath(), null, null);
        }

        // Read and decrypt
        try {
            byte[] fileData = Files.readAllBytes(encPath);
            String js = decrypt(fileData);
            cache.put(relativePath, js);
            log.info("EncryptedPluginLoader — decrypted {} ({} chars)", relativePath, js.length());
            return js;
        } catch (Exception e) {
            throw new PerformPreLoad.PluginLoadException(
                    "Plugin decryption failed",
                    "Could not decrypt: " + encPath.toAbsolutePath(),
                    e.getMessage(),
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
        log.info("EncryptedPluginLoader — cache and key cleared");
    }

    // ── Decryption ──────────────────────────────────────────────────────────

    private String decrypt(byte[] fileData) throws Exception {
        if (fileData.length < IV_LENGTH + TAG_LENGTH_BYTES) {
            throw new IllegalArgumentException("Encrypted file too short — invalid format");
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

            // Use PluginKeyManager — handles password prompt + license binding
            key = PluginKeyManager.getInstance().getPluginKey();

            if (key != null) {
                log.info("EncryptedPluginLoader — key loaded via PluginKeyManager ({} bytes)", key.length);
            } else {
                log.warn("EncryptedPluginLoader — no key available, encrypted plugins will fail to load");
            }
        }
    }
}
