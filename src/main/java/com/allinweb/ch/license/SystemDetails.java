package com.allinweb.ch.license;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SystemDetails {

    public static String getSystemDetails() {
        try {
            // Computer name
            String computerName = System.getenv("COMPUTERNAME"); // Windows
            if (computerName == null || computerName.isEmpty()) {
                // Linux fallback
                computerName = InetAddress.getLocalHost().getHostName();
            }

            // Domain name (Windows only)
            String domainName = System.getenv("USERDOMAIN");
            if (domainName == null || domainName.isEmpty()) {
                // Linux fallback

                domainName = System.getenv("DOMAIN"); // sometimes set
                if (domainName == null) {
                    domainName = SystemDetails.getSystemDomainName();

                    if (domainName == null) {
                        domainName = "local"; // default if no domain
                    }
                }
            }

            // Logged-in user
            String userName = System.getProperty("user.name");

            // Current date
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String dateTime = sdf.format(new Date());

            // Concatenate info
            return computerName + "|" + domainName + "|" + userName + "|" + dateTime;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String hashSystemID(String systemId) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(systemId.getBytes());
        byte[] digest = md.digest();

        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < digest.length; i++) {
            String hex = Integer.toHexString(0xff & digest[i]);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static String getSystemID() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            byte[] mac = network.getHardwareAddress();

            String systemId = "";
            if (mac != null) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < mac.length; i++) {
                    sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                }
                systemId = sb.toString();
            }

            String hostName = ip.getHostName();
            systemId += "-" + hostName;

            // Optionally, hash the constructed ID to get a consistent format
            return hashSystemID(systemId);
        } catch (UnknownHostException | SocketException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getSystemComputerName() {
        try {
            String computerName = System.getenv("COMPUTERNAME"); // Windows
            if (computerName == null || computerName.isEmpty()) {
                // Linux fallback
                computerName = InetAddress.getLocalHost().getHostName();
            }
            return computerName;
        } catch (Exception e) {
            e.printStackTrace();
            return "unknown";
        }
    }

    public static String getSystemDomainName() {
        try {
            String domainName = System.getenv("USERDOMAIN"); // Windows
            if (domainName == null || domainName.isEmpty()) {
                // Linux fallback: try hostname -d
                try {
                    Process proc = new ProcessBuilder("hostname", "-d").start();
                    java.io.BufferedReader reader =
                            new java.io.BufferedReader(new java.io.InputStreamReader(proc.getInputStream()));
                    domainName = reader.readLine();
                    reader.close();
                    if (domainName == null || domainName.isEmpty()) {
                        domainName = "local"; // default if no domain
                    }
                } catch (Exception ex) {
                    domainName = "local";
                }
            }
            return domainName;
        } catch (Exception e) {
            e.printStackTrace();
            return "local";
        }
    }

    public static String getSystemUserName() {
        try {
            return System.getProperty("user.name");
        } catch (Exception e) {
            e.printStackTrace();
            return "unknown";
        }
    }

    public static void main(String[] args) {
        String details = getSystemDetails();
        log.info("System Details: " + details);
    }
}
