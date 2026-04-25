package ru.yartsev_vladislav.otp_app.service.notification;

import ru.yartsev_vladislav.otp_app.domain.DeliveryChannel;

public interface OtpNotificationSender {

    DeliveryChannel channel();

    void send(NotificationPayload payload);
}
