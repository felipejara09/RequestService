package co.com.bancolombia.model.auth;


import lombok.Builder;

import java.util.Set;

@Builder
public record UserInfo(
        String identificationNumber,
        String email,
        Set<String> roles
) {
    public boolean hasRole(String role) { return roles != null && roles.contains(role); }
}
