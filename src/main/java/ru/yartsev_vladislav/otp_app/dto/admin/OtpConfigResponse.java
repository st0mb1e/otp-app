package ru.yartsev_vladislav.otp_app.dto.admin;

public record OtpConfigResponse(
        int codeLength,
        long ttlSeconds
) {
}
