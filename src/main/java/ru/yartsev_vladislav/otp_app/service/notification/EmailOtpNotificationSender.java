package ru.yartsev_vladislav.otp_app.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.yartsev_vladislav.otp_app.domain.DeliveryChannel;

// TODO replace with a real SMTP / email-emulator integration.
@Component
public class EmailOtpNotificationSender implements OtpNotificationSender {

    private static final Logger log = LoggerFactory.getLogger(EmailOtpNotificationSender.class);

    @Override
    public DeliveryChannel channel() {
        return DeliveryChannel.EMAIL;
    }

    @Override
    public void send(NotificationPayload p) {
        log.info("[EMAIL-stub] -> {} : code={} (operationId={}, expiresAt={})",
                p.destination(), p.code(), p.operationId(), p.expiresAt());
    }
}
