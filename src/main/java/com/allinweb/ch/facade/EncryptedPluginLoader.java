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
 * <p>Usage:
 * <pre>
 *   String js = EncryptedPluginLoader.getInstance().loadPlugin("searchListAsync/searchListAsync.min.enc");
 * </pre>
 * </p>
 */
@Slf4j
public class EncryptedPluginLoader {

    private static volatile EncryptedPluginLoader instance;

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final int TAG_LENGTH_BYTES = 16;

    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();
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
     * @param relativePath path relative to plugins folder (e.g. "searchListAsync/searchListAsync.min.enc")
     * @return decrypted JavaScript string
     */
    public String loadPlugin(String relativePath) {
        String cached = cache.get(relativePath);
        if (cached != null) return cached;

        ensureKey();

        String pluginsDir = ARPropertyManager.getInstance().getProperty(ARPropertyEnum.PATH_PLUGINS);
        if (pluginsDir == null || pluginsDir.isBlank()) {
            throw new ARPropertyManager.PluginLoadException(
                    "Plugins folder not configured", "path_plugins is not set in ARWeb.config", null, null);
        }

        Path encPath = Paths.get(pluginsDir).resolve(relativePath);
        if (!Files.exists(encPath)) {
            // Fallback: try plain .min.js (backward compatibility)
            String jsPath = relativePath.replace(".min.enc", ".min.js");
            Path plainPath = Paths.get(pluginsDir).resolve(jsPath);
            if (Files.exists(plainPath)) {
                log.info("EncryptedPluginLoader — no .enc found, falling back to plain .min.js: {}", jsPath);
                try {
                    String js = Files.readString(plainPath, StandardCharsets.UTF_8);
                    cache.put(relativePath, js);
                    return js;
                } catch (IOException e) {
                    throw new ARPropertyManager.PluginLoadException(
                            "Failed to read plugin", e.getMessage(), null, null, e);
                }
            }
            throw new ARPropertyManager.PluginLoadException(
                    "Encrypted plugin not found", "File not found: " + encPath.toAbsolutePath(), null, null);
        }

        try {
            byte[] fileData = Files.readAllBytes(encPath);
            String js = decrypt(fileData);
            cache.put(relativePath, js);
            log.info("EncryptedPluginLoader — decrypted {} ({} chars)", relativePath, js.length());
            return js;
        } catch (Exception e) {
            throw new ARPropertyManager.PluginLoadException(
                    "Plugin decryption failed",
                    "Could not decrypt: " + encPath.toAbsolutePath(),
                    e.getMessage(),
                    null,
                    e);
        }
    }

    public void reloadAll() {
        cache.clear();
        key = null;
        log.info("EncryptedPluginLoader — cache and key cleared");
    }

    private String decrypt(byte[] fileData) throws Exception {
        if (fileData.length < IV_LENGTH + TAG_LENGTH_BYTES) {
            throw new IllegalArgumentException("Encrypted file too short — invalid format");
        }

        byte[] iv = Arrays.copyOfRange(fileData, 0, IV_LENGTH);
        byte[] tag = Arrays.copyOfRange(fileData, IV_LENGTH, IV_LENGTH + TAG_LENGTH_BYTES);
        byte[] encrypted = Arrays.copyOfRange(fileData, IV_LENGTH + TAG_LENGTH_BYTES, fileData.length);

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

    private void ensureKey() {
        if (key != null) return;

        synchronized (this) {
            if (key != null) return;

            key = PluginKeyManager.getInstance().getPluginKey();

            if (key != null) {
                log.info("EncryptedPluginLoader — key loaded via PluginKeyManager ({} bytes)", key.length);
            } else {
                log.warn("EncryptedPluginLoader — no key available, encrypted plugins will fail to load");
            }
        }
    }
}
