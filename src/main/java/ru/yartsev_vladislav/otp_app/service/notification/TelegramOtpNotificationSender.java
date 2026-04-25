package ru.yartsev_vladislav.otp_app.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.yartsev_vladislav.otp_app.domain.DeliveryChannel;

@Component
public class TelegramOtpNotificationSender implements OtpNotificationSender {

    private static final Logger log = LoggerFactory.getLogger(TelegramOtpNotificationSender.class);

    @Override
    public DeliveryChannel channel() {
        return DeliveryChannel.TELEGRAM;
    }

    @Override
    public void send(NotificationPayload p) {
        log.info("[TELEGRAM-stub] -> {} : code={} (operationId={}, expiresAt={})",
                p.destination(), p.code(), p.operationId(), p.expiresAt());
    }
}
