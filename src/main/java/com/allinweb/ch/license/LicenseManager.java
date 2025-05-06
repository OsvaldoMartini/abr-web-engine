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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class LicenseManager {
    private static final String KEY = "0123456789abcdef"; // 16-byte key for AES-128
    private static final PerformMessage performMessage;
    private static final ARPropertyManager arPropertyManager;

    static {
        performMessage = PerformMessage.getInstance();
        arPropertyManager = ARPropertyManager.getInstance();
    }

    public static void generateRequestFile(String fileFolder, String ownerLicence) throws Exception {
        String encryptedRequest = encrypt(ownerLicence + "|" + SystemDetails.getSystemDetails(), KEY);
        String fileName = "ARWeb 1.1.0.request";
        File newFile;

        newFile = new File(fileFolder, fileName);

        // Write the encrypted data to the file
        try (FileWriter writer = new FileWriter(newFile)) {
            writer.write(encryptedRequest);
            System.out.println("File saved to: " + newFile.getAbsolutePath());
        } catch (IOException error) {
            System.err.println("Error writing to file: " + error.getMessage());

            performMessage.errorMessage(
                    "Error reading/writing to the file!",
                    "<span style='font-style: italic;'>Please ensure the application has the necessary write permissions for the specified directory</span>",
                    "<span style='color: #E65100; font-weight: bold;'>Attempted to read/write:</span> <span style='font-weight: bold;'>"
                            + fileFolder + "</span>",
                    "<span style='color: #E65100; font-weight: bold;'>File name:</span> <span style='color: #6A1B9A; font-weight: bold;'>"
                            + fileName + "</span>",
                    "<span style='font-style: italic;'>Details: " + "error.getMessage()" + "</span>",
                    0);
        }
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

        licensePath += "\\ARWeb.lic";
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
        // Suppose the decrypted content is formatted as "PCID|expiryDate" (e.g., "PC12345|2025-12-31")
        String[] parts = decryptedContent.split("\\|");
        if (parts.length != 4) return LicenceVal.MISSING; // Invalid data format

        System.out.println(parts);

        String pcID = parts[0];
        String domainName = parts[1];
        String userName = parts[2];
        LocalDate expiryDate = LocalDate.parse(parts[3], DateTimeFormatter.ISO_LOCAL_DATE);
        String formatted = expiryDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        arPropertyManager.setProperty(ARPropertyEnum.EXPIRATION.getValue(), formatted);

        // System.out.println(" expiryDate is " + expiryDate);
        // Check if the PC ID matches and the current date is before the expiry date
        if (LocalDate.now().isAfter(expiryDate)) return LicenceVal.EXPIRED; // date has expired

        if (!SystemDetails.getSystemComputerName().equals(pcID)) return LicenceVal.PCNOTMATCH;
        if (!SystemDetails.getSystemDomainName().equals(domainName)) return LicenceVal.DOMAINNOTMATCH;
        if (!SystemDetails.getSystemUserName().equals(userName)) return LicenceVal.USRNOTMATCH; // PC ID does not match
        return LicenceVal.VALID; // License is valid
    }

    public String genereteResponseFile(String decryptedContent, int numDays) {
        // Suppose the decrypted content is formatted as "PCID|expiryDate" (e.g., "PC12345|2025-12-31")
        try {
            String[] parts = decryptedContent.split("\\|");

            String pcID = parts[1];
            String domainName = parts[2];
            String userName = parts[3];
            // LocalDate expiryDate = LocalDate.parse(parts[4], DateTimeFormatter.ISO_LOCAL_DATE);

            LocalDate expiryDate = LocalDate.parse(parts[4], DateTimeFormatter.ISO_LOCAL_DATE);
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
                System.out.println("File saved to: " + newFile.getAbsolutePath());
                return "File creation success";
            } catch (IOException e) {
                System.err.println("Error writing to file: " + e.getMessage());
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
