package ru.yartsev_vladislav.otp_app.service.notification;

import java.time.Instant;

public record NotificationPayload(
        long userId,
        String login,
        String operationId,
        String code,
        String destination,
        Instant expiresAt
) {
}
