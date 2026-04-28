package ru.yartsev_vladislav.otp_app.service.notification;

import org.jsmpp.bean.Alphabet;
import org.jsmpp.bean.BindType;
import org.jsmpp.bean.ESMClass;
import org.jsmpp.bean.GeneralDataCoding;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.RegisteredDelivery;
import org.jsmpp.bean.SMSCDeliveryReceipt;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yartsev_vladislav.otp_app.domain.DeliveryChannel;

import java.nio.charset.StandardCharsets;

/**
 * Отправляет OTP-код по SMS
 * Под капотом jsmpp: на каждый send открываем сессию, делаем bind в режиме
 * BIND_TX, отправляем submitShortMessage и закрываем
 *
 * Все настройки тянем стандартным @Value из application.properties
 *
 * Если не задан system-id или пароль, считается что SMS не настроены:
 * бин всё равно создаётся (чтобы приложение поднялось), но при попытке
 * отправить SMS получим ошибку
 */
@Component
public class SmsOtpNotificationSender implements OtpNotificationSender {

    private static final Logger log = LoggerFactory.getLogger(SmsOtpNotificationSender.class);

    private final boolean enabled;
    private final String host;
    private final int port;
    private final String systemId;
    private final String password;
    private final String systemType;
    private final String sourceAddress;

    public SmsOtpNotificationSender(
            @Value("${app.sms.smpp.host:localhost}") String host,
            @Value("${app.sms.smpp.port:2775}") int port,
            @Value("${app.sms.smpp.system-id:}") String systemId,
            @Value("${app.sms.smpp.password:}") String password,
            @Value("${app.sms.smpp.system-type:OTP}") String systemType,
            @Value("${app.sms.smpp.source-addr:OTPService}") String sourceAddress
    ) {
        this.host = host;
        this.port = port;
        this.systemId = systemId == null ? "" : systemId.trim();
        this.password = password == null ? "" : password;
        this.systemType = systemType;
        this.sourceAddress = sourceAddress;
        this.enabled = !this.systemId.isEmpty() && !this.password.isEmpty();

        if (enabled) {
            log.info("SMS sender started (smpp host={}, port={}, system-id={}, source-addr={})",
                    host, port, this.systemId, sourceAddress);
        } else {
            log.warn("SMS sender is off: app.sms.smpp.system-id and/or app.sms.smpp.password "
                    + "are not set, SMS channel won't work");
        }
    }

    @Override
    public DeliveryChannel channel() {
        return DeliveryChannel.SMS;
    }

    @Override
    public void send(NotificationPayload payload) {
        if (!enabled) {
            throw new IllegalStateException(
                    "SMS channel is not configured, set app.sms.smpp.system-id and app.sms.smpp.password");
        }
        if (payload.destination() == null || payload.destination().isBlank()) {
            throw new IllegalArgumentException("destination is required for SMS");
        }

        SMPPSession session = new SMPPSession();
        try {
            BindParameter bindParameter = new BindParameter(
                    BindType.BIND_TX,
                    systemId,
                    password,
                    systemType,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    sourceAddress
            );
            session.connectAndBind(host, port, bindParameter);

            session.submitShortMessage(
                    systemType,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    sourceAddress,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    payload.destination(),
                    new ESMClass(),
                    (byte) 0,
                    (byte) 1,
                    null,
                    null,
                    new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT),
                    (byte) 0,
                    new GeneralDataCoding(Alphabet.ALPHA_DEFAULT),
                    (byte) 0,
                    buildBody(payload).getBytes(StandardCharsets.UTF_8)
            );

            log.info("OTP sent via SMS to {} (userId={} operationId={})",
                    payload.destination(), payload.userId(), payload.operationId());
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "could not send OTP via SMS to " + payload.destination(), ex);
        } finally {
            try {
                session.unbindAndClose();
            } catch (Exception closeEx) {
                log.warn("could not close SMPP session", closeEx);
            }
        }
    }

    /** Текст SMS делаю максимально коротким - SMPP/GSM-7 ограничивает 160 символов на одно сообщение */
    private static String buildBody(NotificationPayload p) {
        StringBuilder sb = new StringBuilder();
        sb.append("Your verification code: ").append(p.code());
        return sb.toString();
    }
}
