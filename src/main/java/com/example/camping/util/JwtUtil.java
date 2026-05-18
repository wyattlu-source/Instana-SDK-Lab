package com.example.camping.util;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public final class JwtUtil {
    private static final long EXPIRY_SECONDS = 24 * 60 * 60;
    private static final String HEADER = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    private JwtUtil() {}

    public static String generate(String secret, String userId, String email, String name) {
        long now = System.currentTimeMillis() / 1000;
        String claimsJson = "{\"sub\":\"" + esc(userId) + "\","
                + "\"email\":\"" + esc(email) + "\","
                + "\"name\":\"" + esc(name) + "\","
                + "\"iat\":" + now + ","
                + "\"exp\":" + (now + EXPIRY_SECONDS) + "}";
        String payload = base64url(claimsJson);
        String data = HEADER + "." + payload;
        return data + "." + sign(secret, data);
    }

    public static Map<String, String> verify(String secret, String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) throw new IllegalArgumentException("Invalid JWT format");

        String data = parts[0] + "." + parts[1];
        if (!sign(secret, data).equals(parts[2])) {
            throw new SecurityException("Invalid JWT signature");
        }

        String claimsJson = new String(
                Base64.getUrlDecoder().decode(pad(parts[1])), StandardCharsets.UTF_8);
        Map<String, String> claims = parseClaims(claimsJson);

        long exp = Long.parseLong(claims.getOrDefault("exp", "0"));
        if (System.currentTimeMillis() / 1000 > exp) {
            throw new SecurityException("JWT token expired");
        }
        return claims;
    }

    private static Map<String, String> parseClaims(String json) {
        Map<String, String> claims = new HashMap<>();
        JsonObject obj = Json.createReader(new StringReader(json)).readObject();
        obj.forEach((k, v) -> {
            String raw = v.toString();
            claims.put(k, raw.startsWith("\"") ? raw.substring(1, raw.length() - 1) : raw);
        });
        return claims;
    }

    private static String base64url(String input) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    private static String sign(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("JWT signing failed", e);
        }
    }

    private static String pad(String s) {
        return s + "=".repeat((4 - s.length() % 4) % 4);
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
