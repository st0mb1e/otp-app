package ru.yartsev_vladislav.otp_app.security.jwt;

import java.time.Instant;

public record IssuedToken(String token, Instant expiresAt) {
}
