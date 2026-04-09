package com.allinweb.ch.facade;

import com.allinweb.ch.util.ARPropertyEnum;
import com.allinweb.ch.util.ARPropertyManager;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages the plugin encryption key — local only, no network calls, no disk writes.
 *
 * <p>Reads the org key embedded in ARWeb.lic at runtime (in memory).
 * Format: pcName|domainName|userName|expiryDate|orgKey[|organization][|version]
 * The orgKey at position [4] is the AES-256 hex key used to decrypt .enc plugins.</p>
 */
@Slf4j
public class PluginKeyManager {

    private static volatile PluginKeyManager instance;

    private static final String LIC_KEY = "0123456789abcdef";
    private static final String LIC_ALGORITHM = "AES/ECB/PKCS5Padding";

    /** The unwrapped plugin AES key — null until successfully loaded from ARWeb.lic */
    private byte[] pluginKey;

    private PluginKeyManager() {}

    public static PluginKeyManager getInstance() {
        if (instance == null) {
            synchronized (PluginKeyManager.class) {
                if (instance == null) {
                    instance = new PluginKeyManager();
                }
            }
        }
        return instance;
    }

    /**
     * Get the plugin encryption key. Extracted from ARWeb.lic in memory.
     * Returns null if the license doesn't contain an org key.
     */
    public byte[] getPluginKey() {
        if (pluginKey != null) return pluginKey;

        synchronized (this) {
            if (pluginKey != null) return pluginKey;

            try {
                String orgKeyHex = extractOrgKeyFromLicense();
                if (orgKeyHex != null && !orgKeyHex.isEmpty()) {
                    pluginKey = hexToBytes(orgKeyHex);
                    log.info("PluginKeyManager — org key loaded from ARWeb.lic ({}-bit)", pluginKey.length * 8);
                } else {
                    log.error("PluginKeyManager — no org key found in ARWeb.lic");
                }
            } catch (Exception e) {
                log.error("PluginKeyManager — failed to load key: {}", e.getMessage());
                pluginKey = null;
            }
        }

        return pluginKey;
    }

    /** Clear cached key from memory. */
    public void clearKey() {
        if (pluginKey != null) Arrays.fill(pluginKey, (byte) 0);
        pluginKey = null;
    }

    // ── Extract org key from ARWeb.lic (in memory, no disk writes) ───────────

    /**
     * Decrypt ARWeb.lic and extract the org key from part[4].
     * Format: pcName|domainName|userName|expiryDate|orgKey[|organization][|version]
     * Returns null if no org key embedded (legacy 4-part format).
     */
    private String extractOrgKeyFromLicense() {
        try {
            String licensePath = ARPropertyManager.getInstance().getProperty(ARPropertyEnum.PATH_LICENSE);
            if (licensePath == null) licensePath = System.getProperty("user.dir");

            Path licFile = Paths.get(licensePath, "ARWeb.lic");
            if (!Files.exists(licFile)) {
                log.error("PluginKeyManager — ARWeb.lic not found: {}", licFile);
                return null;
            }

            String content = Files.readString(licFile, StandardCharsets.UTF_8).trim();
            SecretKeySpec keySpec = new SecretKeySpec(LIC_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance(LIC_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(content));
            String plain = new String(decrypted, StandardCharsets.UTF_8);
            String[] parts = plain.split("\\|");
            if (parts.length >= 5) {
                log.info("PluginKeyManager — org key found in ARWeb.lic");
                return parts[4];
            }
            log.warn("PluginKeyManager — ARWeb.lic has {} parts (need >= 5 for org key)", parts.length);
            return null;
        } catch (Exception e) {
            log.debug("PluginKeyManager — could not extract org key: {}", e.getMessage());
            return null;
        }
    }

    // ── Utilities ───────────────────────────────────────────────────────────

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] bytes = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
            bytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
        return bytes;
    }
}
