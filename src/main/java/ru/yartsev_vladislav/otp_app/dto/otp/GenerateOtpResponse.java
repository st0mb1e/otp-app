package ru.yartsev_vladislav.otp_app.dto.otp;

import ru.yartsev_vladislav.otp_app.domain.DeliveryChannel;

import java.time.Instant;

public record GenerateOtpResponse(
        Long otpId,
        String operationId,
        DeliveryChannel channel,
        Instant expiresAt
) {
}
