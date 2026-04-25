package ru.yartsev_vladislav.otp_app.security.jwt;

import ru.yartsev_vladislav.otp_app.domain.Role;

import java.time.Instant;

public record JwtClaims(
        long userId,
        String login,
        Role role,
        Instant expiresAt
) {
}
