package co.com.bancolombia.model.loanapplication;

import java.util.Locale;

public enum Decision {
    APPROVED, REJECTED;


    public int toStateId() {
        return this == APPROVED ? 2 : 3;
    }

    public static Decision from(String raw) {
        if (raw == null) throw new IllegalArgumentException("INVALID_DECISION");
        String d = raw.trim().toUpperCase(Locale.ROOT);
        if (d.startsWith("APPROV") || d.startsWith("APROB")) return APPROVED;
        if (d.startsWith("REJECT") || d.startsWith("RECHAZ")) return REJECTED;
        throw new IllegalArgumentException("INVALID_DECISION");
    }
}
