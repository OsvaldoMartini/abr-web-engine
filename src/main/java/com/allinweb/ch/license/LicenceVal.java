package com.allinweb.ch.license;

public enum LicenceVal {
    // Definisci le possibili costanti per l'enumerazione
    VALID,
    EXPIRED,
    REVOKED,
    PENDING_ACTIVATION,
    MISSING,
    PCMISSMATCH,
    PCNOTMATCH,
    DOMAINNOTMATCH,
    USRNOTMATCH;

    // Metodo di esempio per ottenere una descrizione umana leggibile delle costanti
    @Override
    public String toString() {
        switch (this) {
            case VALID:
                return "Valid license";
            case EXPIRED:
                return "Expired license";
            case REVOKED:
                return "Revoked license";
            case PENDING_ACTIVATION:
                return "License pending activation";
            case MISSING:
                return "Missing license";
            case PCMISSMATCH:
                return "License not valid for this PC";
            case PCNOTMATCH:
                return "License not valid for this PC";
            case DOMAINNOTMATCH:
                return "License not valid for the domain of this PC";
            case USRNOTMATCH:
                return "License not valid for this user on this PC";
            default:
                return "License status undefined";
        }
    }

    // Metodo per verificare se la licenza  attiva
    public boolean isActive() {
        return this == VALID /*|| this == PENDING_ACTIVATION*/;
    }

    public boolean isExpired() {
        return this == EXPIRED;
    }

    public boolean isMissing() {
        return this == MISSING;
    }

    public String getStaus() {
        return this.toString();
    }
}
