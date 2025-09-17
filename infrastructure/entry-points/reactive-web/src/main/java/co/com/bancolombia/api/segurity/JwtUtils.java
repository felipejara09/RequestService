package co.com.bancolombia.api.segurity;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public final class JwtUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private JwtUtils() {}

    @SuppressWarnings("unchecked")
    public static Map<String, Object> decodeClaims(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return Map.of();
        String token = authHeader.substring("Bearer ".length()).trim();
        String[] parts = token.split("\\.");
        if (parts.length < 2) return Map.of();
        try {
            String json = new String(Base64.getUrlDecoder().decode(pad(parts[1])), StandardCharsets.UTF_8);
            return MAPPER.readValue(json, Map.class);
        } catch (Exception e) { return Map.of(); }
    }

    private static String pad(String s) {
        int pad = (4 - s.length() % 4) % 4;
        return s + "====".substring(0, pad);
    }

    public static String claimString(Map<String, Object> claims, String key) {
        Object v = claims.get(key);
        return v == null ? null : String.valueOf(v);
    }

    public static Integer claimInt(Map<String, Object> claims, String key) {
        Object v = claims.get(key);
        if (v == null) return null;
        try { return Integer.valueOf(String.valueOf(v)); } catch (Exception e) { return null; }
    }
}
