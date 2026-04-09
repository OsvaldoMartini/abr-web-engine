package com.allinweb.ch.facade;

import com.allinweb.ch.util.ARPropertyEnum;
import com.allinweb.ch.util.ARPropertyManager;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages the plugin encryption key with online activation via Supabase.
 * Adapted for ar-web-engine (no JavaFX - headless/engine mode).
 */
@Slf4j
public class PluginKeyManager {

    private static volatile PluginKeyManager instance;

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final int PBKDF2_ITERATIONS = 100_000;
    private static final int KEY_LENGTH_BITS = 256;

    private static final String ACTIVATED_PREFIX = "ACTIVATED:";
    private static final String PROTECTED_PREFIX = "PROTECTED:";
    private static final String LIC_KEY = "0123456789abcdef";
    private static final String LIC_ALGORITHM = "AES/ECB/PKCS5Padding";

    private static final String SUPABASE_URL =
            System.getProperty("arweb.supabase.url", "https://tlqjpxitggzetgsgvxmp.supabase.co");
    private static final String SUPABASE_ANON_KEY =
            System.getProperty("arweb.supabase.key", "sb_publishable_ivT0y1WFZGyI_4n76eTj0Q_OadxuJIW");

    private static final int VALIDATE_INTERVAL_DAYS = 7;

    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    private byte[] pluginKey;
    private String machineId;

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

    public byte[] getPluginKey() {
        if (pluginKey != null) return pluginKey;

        synchronized (this) {
            if (pluginKey != null) return pluginKey;

            try {
                String pluginsDir = ARPropertyManager.getInstance().getProperty(ARPropertyEnum.PATH_PLUGINS);
                if (pluginsDir == null) {
                    log.error("PluginKeyManager - path_plugins not configured");
                    return null;
                }

                Path keyFile = Paths.get(pluginsDir, "plugins.key");
                String licenseFingerprint = getLicenseFingerprint();
                String machId = getMachineId();

                if (licenseFingerprint == null) {
                    log.error("PluginKeyManager - could not read license fingerprint");
                    return null;
                }

                // Priority 1: Extract org key embedded in ARWeb.lic
                String orgKeyHex = extractOrgKeyFromLicense();
                if (orgKeyHex != null && !orgKeyHex.isEmpty()) {
                    pluginKey = hexToBytes(orgKeyHex);
                    log.info("PluginKeyManager — org key found in ARWeb.lic ({}-bit)", pluginKey.length * 8);
                    scheduleValidation(licenseFingerprint, machId);
                }

                // Priority 2: plugins.key file or online activation
                if (pluginKey == null && !Files.exists(keyFile)) {
                    pluginKey = activateOnline(keyFile, licenseFingerprint, machId);
                } else if (pluginKey == null) {
                    String content =
                            Files.readString(keyFile, StandardCharsets.UTF_8).trim();

                    if (content.startsWith(ACTIVATED_PREFIX)) {
                        String encoded = content.substring(ACTIVATED_PREFIX.length());
                        pluginKey = unwrapKey(encoded, machId, licenseFingerprint);
                        log.info("PluginKeyManager - key unlocked (machine-bound)");
                        scheduleValidation(licenseFingerprint, machId);
                    } else if (content.startsWith(PROTECTED_PREFIX)) {
                        log.info("PluginKeyManager - legacy PROTECTED key, re-activating online");
                        pluginKey = activateOnline(keyFile, licenseFingerprint, machId);
                    } else {
                        pluginKey = hexToBytes(content);
                        log.info("PluginKeyManager - loaded plain key");
                    }
                }
            } catch (Exception e) {
                log.error("PluginKeyManager - failed: {}", e.getMessage(), e);
                pluginKey = null;
            }
        }

        return pluginKey;
    }

    public void clearKey() {
        if (pluginKey != null) Arrays.fill(pluginKey, (byte) 0);
        pluginKey = null;
    }

    // ── Online activation ───────────────────────────────────────────────────

    private byte[] activateOnline(Path keyFile, String licenseFingerprint, String machId) throws Exception {
        log.info("PluginKeyManager - activating online...");

        String hostname = getHostname();
        String osInfo = System.getProperty("os.name") + " " + System.getProperty("os.version");
        String javaVersion = System.getProperty("java.version");

        String json = String.format(
                "{\"p_license_hash\":\"%s\",\"p_machine_id\":\"%s\",\"p_hostname\":\"%s\","
                        + "\"p_os_info\":\"%s\",\"p_java_version\":\"%s\"}",
                escapeJson(licenseFingerprint),
                escapeJson(machId),
                escapeJson(hostname),
                escapeJson(osInfo),
                escapeJson(javaVersion));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + "/rest/v1/rpc/activate_client"))
                .header("Content-Type", "application/json")
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("PluginKeyManager - activation HTTP error: {} {}", response.statusCode(), response.body());
            return null;
        }

        String body = response.body();
        boolean ok = body.contains("\"ok\":true") || body.contains("\"ok\": true");
        if (!ok) {
            String error = extractJsonString(body, "error");
            log.error("PluginKeyManager - activation rejected: {}", error);
            return null;
        }

        String pluginKeyHex = extractJsonString(body, "plugin_key");
        if (pluginKeyHex == null || pluginKeyHex.isEmpty() || pluginKeyHex.equals("YOUR_HEX_KEY_HERE")) {
            log.error("PluginKeyManager - server returned empty plugin key");
            return null;
        }

        byte[] key = hexToBytes(pluginKeyHex);
        String wrapped = wrapKey(key, machId, licenseFingerprint);
        Files.writeString(keyFile, wrapped, StandardCharsets.UTF_8);

        String clientName = extractJsonString(body, "client_name");
        log.info("PluginKeyManager - activated for client: {}", clientName);
        return key;
    }

    // ── Periodic validation ─────────────────────────────────────────────────

    private void scheduleValidation(String licenseFingerprint, String machId) {
        try {
            String pluginsDir = ARPropertyManager.getInstance().getProperty(ARPropertyEnum.PATH_PLUGINS);
            Path stampFile = Paths.get(pluginsDir, ".last-validation");

            if (Files.exists(stampFile)) {
                long lastValidation = Long.parseLong(
                        Files.readString(stampFile, StandardCharsets.UTF_8).trim());
                long daysSince = (System.currentTimeMillis() - lastValidation) / (1000L * 60 * 60 * 24);
                if (daysSince < VALIDATE_INTERVAL_DAYS) return;
            }

            new Thread(() -> {
                        try {
                            boolean valid = validateOnline(licenseFingerprint, machId);
                            if (valid) {
                                Files.writeString(
                                        Paths.get(pluginsDir, ".last-validation"),
                                        String.valueOf(System.currentTimeMillis()),
                                        StandardCharsets.UTF_8);
                            } else {
                                log.warn("PluginKeyManager - validation failed, clearing key");
                                clearKey();
                                Files.deleteIfExists(Paths.get(pluginsDir, "plugins.key"));
                            }
                        } catch (Exception e) {
                            log.warn("PluginKeyManager - validation network error: {}", e.getMessage());
                        }
                    })
                    .start();
        } catch (Exception e) {
            log.warn("PluginKeyManager - could not schedule validation: {}", e.getMessage());
        }
    }

    private boolean validateOnline(String licenseFingerprint, String machId) throws Exception {
        String json = String.format(
                "{\"p_license_hash\":\"%s\",\"p_machine_id\":\"%s\"}",
                escapeJson(licenseFingerprint), escapeJson(machId));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + "/rest/v1/rpc/validate_client"))
                .header("Content-Type", "application/json")
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) return true; // network error → don't block
        String body = response.body();
        return body.contains("\"ok\":true") || body.contains("\"ok\": true");
    }

    // ── Key wrapping ────────────────────────────────────────────────────────

    private String wrapKey(byte[] pluginKey, String machId, String licenseFingerprint) throws Exception {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        byte[] wrapperKey = deriveKey(machId, licenseFingerprint, salt);
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(wrapperKey, "AES"), new GCMParameterSpec(TAG_BITS, iv));
        byte[] encrypted = cipher.doFinal(pluginKey);

        byte[] combined = new byte[salt.length + iv.length + encrypted.length];
        System.arraycopy(salt, 0, combined, 0, salt.length);
        System.arraycopy(iv, 0, combined, salt.length, iv.length);
        System.arraycopy(encrypted, 0, combined, salt.length + iv.length, encrypted.length);

        return ACTIVATED_PREFIX + Base64.getEncoder().encodeToString(combined);
    }

    private byte[] unwrapKey(String encoded, String machId, String licenseFingerprint) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encoded);
        byte[] salt = Arrays.copyOfRange(combined, 0, SALT_LENGTH);
        byte[] iv = Arrays.copyOfRange(combined, SALT_LENGTH, SALT_LENGTH + IV_LENGTH);
        byte[] encrypted = Arrays.copyOfRange(combined, SALT_LENGTH + IV_LENGTH, combined.length);

        byte[] wrapperKey = deriveKey(machId, licenseFingerprint, salt);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(wrapperKey, "AES"), new GCMParameterSpec(TAG_BITS, iv));
        return cipher.doFinal(encrypted);
    }

    private byte[] deriveKey(String machId, String licenseFingerprint, byte[] salt) throws Exception {
        String combined = machId + "|" + licenseFingerprint;
        KeySpec spec = new PBEKeySpec(combined.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .getEncoded();
    }

    // ── Machine fingerprint ─────────────────────────────────────────────────

    public String getMachineId() {
        if (machineId != null) return machineId;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(getHostname()).append("|");
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            while (nets.hasMoreElements()) {
                byte[] mac = nets.nextElement().getHardwareAddress();
                if (mac != null && mac.length > 0) {
                    for (byte b : mac) sb.append(String.format("%02x", b));
                    sb.append(",");
                }
            }
            sb.append(System.getProperty("os.name")).append("|");
            sb.append(System.getProperty("user.name"));
            machineId = bytesToHex(sha256(sb.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            machineId = bytesToHex(
                    sha256((getHostname() + "|" + System.getProperty("user.name")).getBytes(StandardCharsets.UTF_8)));
        }
        return machineId;
    }

    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            String cn = System.getenv("COMPUTERNAME");
            return cn != null ? cn : "unknown";
        }
    }

    private String getLicenseFingerprint() {
        try {
            String licensePath = ARPropertyManager.getInstance().getProperty(ARPropertyEnum.PATH_LICENSE);
            if (licensePath == null) licensePath = System.getProperty("user.dir");
            Path licFile = Paths.get(licensePath, "ARWeb.lic");
            if (!Files.exists(licFile)) {
                log.error("PluginKeyManager - license not found: {}", licFile);
                return null;
            }
            return bytesToHex(sha256(Files.readAllBytes(licFile)));
        } catch (Exception e) {
            log.error("PluginKeyManager - failed to read license: {}", e.getMessage());
            return null;
        }
    }

    // ── Extract org key from ARWeb.lic ────────────────────────────────────────

    /**
     * Decrypt ARWeb.lic and extract the org key from part[4].
     * Format: pcName|domainName|userName|expiryDate|orgKey
     * Returns null if no org key embedded (legacy 4-part format).
     */
    private String extractOrgKeyFromLicense() {
        try {
            String licensePath = ARPropertyManager.getInstance().getProperty(ARPropertyEnum.PATH_LICENSE);
            if (licensePath == null) licensePath = System.getProperty("user.dir");

            Path licFile = Paths.get(licensePath, "ARWeb.lic");
            if (!Files.exists(licFile)) return null;

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
            return null;
        } catch (Exception e) {
            log.debug("PluginKeyManager — no org key in ARWeb.lic: {}", e.getMessage());
            return null;
        }
    }

    // ── Utilities ───────────────────────────────────────────────────────────

    private static String extractJsonString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) {
            search = "\"" + key + "\":";
            start = json.indexOf(search);
            if (start < 0) return null;
            start += search.length();
            while (start < json.length() && json.charAt(start) == ' ') start++;
            if (start >= json.length() || json.charAt(start) == 'n') return null;
            if (json.charAt(start) == '"') {
                start++;
                int end = json.indexOf('"', start);
                return end > start ? json.substring(start, end) : null;
            }
            return null;
        }
        start += search.length();
        int end = json.indexOf('"', start);
        return end > start ? json.substring(start, end) : null;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] bytes = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
            bytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
        return bytes;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
