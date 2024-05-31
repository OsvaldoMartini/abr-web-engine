package com.allinweb.ch.supportTypes;

import java.util.Arrays;

public class PaymentInfo {

    private String beneficiary;
    private String address;
    private String city;
    private String nation;
    private String IBAN;
    private String reason;
    private double amount;
    private String divisa;
    private String id;
    private String user;
    private String password;

    public PaymentInfo(
            String beneficiary,
            String address,
            String city,
            String nation,
            String IBAN,
            String reason,
            String amount,
            String divisa) {
        this.beneficiary = beneficiary;
        this.address = address;
        this.city = city;
        this.nation = nation;
        this.IBAN = IBAN;
        this.reason = reason;
        this.amount = convert(amount);
        this.divisa = divisa;
    }

    public PaymentInfo(
            String beneficiary,
            String address,
            String city,
            String nation,
            String IBAN,
            String reason,
            double amount,
            String divisa) {
        this.beneficiary = beneficiary;
        this.address = address;
        this.city = city;
        this.nation = nation;
        this.IBAN = IBAN;
        this.reason = reason;
        this.amount = amount;
        this.divisa = divisa;
    }

    public PaymentInfo(
            String beneficiary,
            String address,
            String city,
            String nation,
            String IBAN,
            String reason,
            String amount,
            String divisa,
            String id) {
        this.beneficiary = beneficiary;
        this.address = address;
        this.city = city;
        this.nation = nation;
        this.IBAN = IBAN;
        this.reason = reason;
        this.amount = convert(amount);
        this.divisa = divisa;
        this.id = id;
    }

    public PaymentInfo(
            String beneficiary,
            String address,
            String city,
            String nation,
            String IBAN,
            String reason,
            double amount,
            String divisa,
            String id) {
        this.beneficiary = beneficiary;
        this.address = address;
        this.city = city;
        this.nation = nation;
        this.IBAN = IBAN;
        this.reason = reason;
        this.amount = amount;
        this.divisa = divisa;
        this.id = id;
    }

    public PaymentInfo(
            String beneficiary,
            String address,
            String city,
            String nation,
            String IBAN,
            String reason,
            double amount,
            String divisa,
            String user,
            String password) {
        this.beneficiary = beneficiary;
        this.address = address;
        this.city = city;
        this.nation = nation;
        this.IBAN = IBAN;
        this.reason = reason;
        this.amount = amount;
        this.divisa = divisa;
        this.user = user;
        this.password = password;
    }

    public PaymentInfo(
            String beneficiary,
            String address,
            String city,
            String nation,
            String IBAN,
            String reason,
            String amount,
            String divisa,
            String user,
            String password) {
        this.beneficiary = beneficiary;
        this.address = address;
        this.city = city;
        this.nation = nation;
        this.IBAN = IBAN;
        this.reason = reason;
        this.amount = convert(amount);
        this.divisa = divisa;
        this.user = user;
        this.password = password;
    }

    public String getDivisa() {
        return divisa;
    }

    public void setDivisa(String divisa) {
        this.divisa = divisa;
    }

    public String getBeneficiary() {
        return beneficiary;
    }

    public void setBeneficiary(String beneficiary) {
        this.beneficiary = beneficiary;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getNation() {
        return nation;
    }

    public void setNation(String nation) {
        this.nation = nation;
    }

    public String getIBAN() {
        return IBAN;
    }

    public void setIBAN(String iBAN) {
        IBAN = iBAN;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double convert(String amount) {

        double sum = Double.parseDouble(amount);
        if (sum < 0) {
            throw new RuntimeException("negative amount : " + amount);
        }

        return sum;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String toString() {
        String s = beneficiary + " " + address + " " + city + " " + nation + " " + IBAN + " " + reason + " " + amount
                + " " + divisa + " " + user + " " + password;
        if (id != null) {
            s += " " + id;
        }
        return s;
    }

    public String toCSV() {
        String s = beneficiary + "," + address + "," + city + "," + nation + "," + IBAN + "," + reason + "," + amount
                + "," + divisa + "," + user + "," + password;
        if (id != null) {
            s += "," + id;
        }
        return s;
    }

    public String[] toArray() {
        String[] data = new String[11];
        data[0] = beneficiary;
        data[1] = address;
        data[2] = city;
        data[3] = nation;
        data[4] = IBAN;
        data[5] = reason;
        data[6] = String.valueOf(amount);
        data[7] = divisa;
        data[8] = user;
        data[9] = password;
        data[10] = id;
        if (id == null) {
            data = Arrays.copyOf(data, data.length - 1);
        }
        return data;
    }
}
