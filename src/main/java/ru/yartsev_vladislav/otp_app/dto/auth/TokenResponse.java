package ru.yartsev_vladislav.otp_app.dto.auth;

import java.time.Instant;

public record TokenResponse(
        String token,
        String tokenType,
        Instant expiresAt
) {
    public static TokenResponse bearer(String token, Instant expiresAt) {
        return new TokenResponse(token, "Bearer", expiresAt);
    }
}
