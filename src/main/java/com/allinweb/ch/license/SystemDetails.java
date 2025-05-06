package com.allinweb.ch.license;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SystemDetails {

    public static String getSystemDetails() {
        try {
            // Ottiene il nome del computer
            String computerName = System.getenv("COMPUTERNAME");

            // Ottiene il nome dell'utente loggato
            String userName = System.getProperty("user.name");

            // Ottiene il dominio, che può essere derivato in ambienti specifici; esempio sotto è solo
            // illustrativo
            String domainName = System.getenv("USERDOMAIN");

            // Formatta la data e ora corrente
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String dateTime = sdf.format(new Date());

            // Concatena le informazioni
            return computerName + "|" + domainName + "|" + userName + "|" + dateTime; // "2025-02-01"; // ;
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
            // Ottiene il nome del computer
            String computerName = System.getenv("COMPUTERNAME");
            ;

            return computerName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getSystemDomainName() {
        try {
            // Ottiene il nome del computer
            String domainName = System.getenv("USERDOMAIN");

            return domainName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getSystemUserName() {
        try {
            // Ottiene il nome dell'utente loggato
            String userName = System.getProperty("user.name");

            // Concatena le informazioni
            return userName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        String details = getSystemDetails();
        System.out.println("System Details: " + details);
    }
}
