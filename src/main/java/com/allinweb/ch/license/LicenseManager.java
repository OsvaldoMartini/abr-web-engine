package com.allinweb.ch.license;

import com.allinweb.ch.facade.PerformMessage;
import com.allinweb.ch.util.ARPropertyEnum;
import com.allinweb.ch.util.ARPropertyManager;
import com.google.common.base.Strings;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.KnownFolders;
import com.sun.jna.platform.win32.Shell32;
import com.sun.jna.ptr.PointerByReference;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LicenseManager {
    private static final String KEY = "0123456789abcdef"; // 16-byte key for AES-128
    public static final String API_URL = "https://multiplugins.ch/api";
    public static final String APP_VERSION = "4.7";
    private static final PerformMessage performMessage;
    private static final ARPropertyManager arPropertyManager;

    static {
        performMessage = PerformMessage.getInstance();
        arPropertyManager = ARPropertyManager.getInstance();
    }

    /**
     * Returns true if the input is a valid email address (RFC 5322 simplified).
     */
    public static boolean isEmail(String input) {
        if (input == null || input.isBlank()) return false;
        return input.matches(
                "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~\\-]+@[a-zA-Z0-9](?:[a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?)*\\.[a-zA-Z]{2,}$");
    }

    /**
     * Encrypted format:
     *   organization|owner|pcName|domainName|userName|requestDate|email  (7 parts)
     * Owner and email are independent both optional.
     */
    private static final String DEFAULT_EMAIL = "o.martini@allinweb.ch";

    public static void generateRequestFile(String fileFolder, String organization, String owner, String email)
            throws Exception {
        String emailTag = (email != null && !email.isBlank()) ? "email_client:" + email : "email_client:empty";
        String safeOwner = (owner != null) ? owner : "";
        String requestData = organization + "|" + safeOwner + "|" + SystemDetails.getSystemDetails() + "|" + emailTag
                + "|" + APP_VERSION;
        String encryptedRequest = encrypt(requestData, KEY);
        String safeEmail = (email != null && !email.isBlank()) ? email : "";
        String fileLabel = !safeOwner.isEmpty() ? safeOwner : (!safeEmail.isEmpty() ? safeEmail : "request");
        String fileName = organization + "-" + fileLabel + ".request";
        File newFile = new File(fileFolder, fileName);

        try (FileWriter writer = new FileWriter(newFile)) {
            writer.write(encryptedRequest);
            log.info("File saved to: " + newFile.getAbsolutePath());
        } catch (IOException error) {
            log.warn("Error reading/writing to the file: " + error.getMessage());
        }
    }

    /**
     * Sends the encrypted license request directly to the MultiPlugins API.
     * Returns "SUCCESS" on success, or the error message on failure.
     */
    public static String sendRequestOnline(String organization, String owner, String email) throws Exception {
        String emailTag = (email != null && !email.isBlank()) ? "email_client:" + email : "email_client:empty";
        String safeOwner = (owner != null) ? owner : "";
        String requestData = organization + "|" + safeOwner + "|" + SystemDetails.getSystemDetails() + "|" + emailTag
                + "|" + APP_VERSION;
        String encryptedRequest = encrypt(requestData, KEY);

        HttpClient client =
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

        String json = "{\"content\":\"" + escapeJson(encryptedRequest) + "\",\"organization\":\""
                + escapeJson(organization) + "\",\"app_version\":\"" + APP_VERSION + "\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/client/license-request"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            String body = response.body();
            if (body.contains("\"ok\":true") || body.contains("\"ok\": true")) {
                return "SUCCESS";
            }
            return body;
        }
        return "HTTP Error: " + response.statusCode() + " - " + response.body();
    }

    /**
     * Pings the API to check connectivity.
     * Returns true if the server responds with ok:true, false otherwise.
     */
    public static boolean pingApi() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/client/ping"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200
                    && (response.body().contains("\"ok\":true")
                            || response.body().contains("\"ok\": true"));
        } catch (Exception e) {
            System.err.println("[PING] API ping failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return false;
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    public static boolean importResponseFile(String filePath) throws Exception {
        // Use SHGetKnownFolderPath to get Desktop path
        String desktopPath;
        File responseFile = new File(filePath);

        if (responseFile.exists()) {
            String content = Files.readString(responseFile.toPath());
            String decryptedResponse = decrypt(content, KEY);
            generateLicFile(decryptedResponse);
            return true;
        }

        return false;
    }

    private static void generateLicFile(String data) throws Exception {
        String licensePath = arPropertyManager.getProperty(ARPropertyEnum.PATH_LICENSE);
        if (Strings.isNullOrEmpty(licensePath)) {
            licensePath = System.getProperty("user.dir");
        }

        licensePath += "/ARWeb.lic";
        Files.writeString(Paths.get(licensePath), encrypt(data, KEY));
    }

    public static String encrypt(String data, String key) throws Exception {
        Key aesKey = new SecretKeySpec(key.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        byte[] encrypted = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public static String decrypt(String encryptedData, String key) throws Exception {
        try {

            Key aesKey = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, aesKey);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(decrypted);
        } catch (Exception error) {
            log.error("An error occurred while decrypting the license file.");
            performMessage.errorMessage(
                    "An error occurred while decrypting the license file.",
                    "File Name:",
                    "ARWeb.lic",
                    "Please verify if the file is corrupted or tampered.",
                    null,
                    0);
        }
        return null;
    }

    public static LicenceVal checkLicenseFile(String licensePath) throws Exception {
        Path licPath = Paths.get(licensePath + "/ARWeb.lic");
        if (!Files.exists(licPath)) {
            return LicenceVal.MISSING; // File is absent
        } else {
            String licContent = new String(Files.readAllBytes(licPath));
            String decryptedContent = decrypt(licContent, KEY);
            return validateLicense(decryptedContent);
        }
    }

    public static String getDecryptedResponseFile(String requestFile) throws Exception {
        Path licPath = Paths.get(requestFile);

        String fileName = licPath.getFileName().toString(); //

        if (!fileName.endsWith(".request")) {
            performMessage.errorMessage(
                    "Invalid file selected!", "Must have a '.request' extension.", "File selected:", fileName, null, 0);
            return "Invalid file selected";
        }

        if (!Files.exists(licPath)) {
            return "noFileFound"; // File is absent
        } else {
            String licContent = new String(Files.readAllBytes(licPath));
            String decryptedContent = decrypt(licContent, KEY);
            return decryptedContent;
        }
    }

    private static LicenceVal validateLicense(String decryptedContent) {
        // .lic format: pcName|domainName|userName|expiryDate|orgKey|organization|version
        // Minimum 4 parts required, rest optional
        String[] parts = decryptedContent.split("\\|");
        if (parts.length < 4) return LicenceVal.MISSING;

        String pcID = parts[0];
        String domainName = parts[1];
        String userName = parts[2];
        LocalDate expiryDate = LocalDate.parse(parts[3], DateTimeFormatter.ISO_LOCAL_DATE);
        String formatted = expiryDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        arPropertyManager.setProperty(ARPropertyEnum.EXPIRATION.getValue(), formatted);

        // orgKey at [4] (64-char hex), organization at [5], version at [6]
        if (parts.length >= 5) {
            arPropertyManager.setProperty("license.orgKey", parts[4]);
        }
        if (parts.length >= 6) {
            arPropertyManager.setProperty("license.organization", parts[5]);
        }
        if (parts.length >= 7) {
            arPropertyManager.setProperty("license.version", parts[6]);
        }

        if (LocalDate.now().isAfter(expiryDate)) return LicenceVal.EXPIRED;
        if (!SystemDetails.getSystemComputerName().equals(pcID)) return LicenceVal.PCNOTMATCH;
        if (!SystemDetails.getSystemDomainName().equals(domainName)) return LicenceVal.DOMAINNOTMATCH;
        if (!SystemDetails.getSystemUserName().equals(userName)) return LicenceVal.USRNOTMATCH;
        return LicenceVal.VALID;
    }

    public String genereteResponseFile(String decryptedContent, int numDays) {
        // Format: organization|owner|pcName|domainName|userName|requestDate|email (7 parts)
        // Compat:  organization|owner|pcName|domainName|userName|requestDate (6 parts)
        // Legacy:  ownerName|pcName|domainName|userName|requestDate (5 parts)
        try {
            String[] parts = decryptedContent.split("\\|");

            int offset = parts.length >= 6 ? 2 : 1; // skip organization+owner or just owner
            String pcID = parts[offset];
            String domainName = parts[offset + 1];
            String userName = parts[offset + 2];

            LocalDate expiryDate = LocalDate.parse(parts[offset + 3], DateTimeFormatter.ISO_LOCAL_DATE);
            expiryDate = expiryDate.plusDays(numDays);

            String encryptedResponse = encrypt(pcID + "|" + domainName + "|" + userName + "|" + expiryDate, KEY);

            // Use SHGetKnownFolderPath to get Desktop path
            PointerByReference ppszPath = new PointerByReference();
            if (Shell32.INSTANCE
                            .SHGetKnownFolderPath(KnownFolders.FOLDERID_Desktop, 0, null, ppszPath)
                            .intValue()
                    != 0) {
                throw new IOException("Failed to get desktop directory.");
            }

            // Convert pointer to string
            String desktopPath = ppszPath.getValue().getWideString(0);
            Native.free(Pointer.nativeValue(ppszPath.getValue()));

            // Create the file in the desktop directory
            File newFile = new File(desktopPath, "ARWeb 1.1.0.response");

            // Write the encrypted data to the file
            try (FileWriter writer = new FileWriter(newFile)) {
                writer.write(encryptedResponse);
                log.info("File saved to: " + newFile.getAbsolutePath());
                return "File creation success";
            } catch (IOException e) {
                log.error("Error writing to file: " + e.getMessage());
                performMessage.errorMessage(
                        "Error writing to the file!",
                        "File Name:",
                        "ARWeb 1.1.0.response",
                        "Please verify that you have permission to read/write to the Desktop.",
                        null,
                        0);
                return "Denied permission to read/write";
            }

        } catch (Exception error) {
            performMessage.errorMessage(
                    "Error generating Response  file!",
                    "Please verify that you have permission to read/write to the Desktop.",
                    "Please verify if the file is corrupted or tampered.",
                    null,
                    null,
                    0);

            return "file is corrupted or tampered.";
        }
    }
}
