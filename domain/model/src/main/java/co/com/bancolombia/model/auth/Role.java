package co.com.bancolombia.model.auth;

public enum Role {
    ADMIN(1), ADVISOR(2), CLIENT(3);

    private final int id;
    Role(int id) { this.id = id; }
    public int id() { return id; }

    public static Role from(Integer id) {
        if (id == null) return null;
        for (Role r : values()) if (r.id == id) return r;
        return null;
    }

    public boolean isAdminOrAdvisor() {
        return this == ADMIN || this == ADVISOR;
    }
}
