package ru.yartsev_vladislav.otp_app.service.notification;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yartsev_vladislav.otp_app.domain.DeliveryChannel;

import java.time.format.DateTimeFormatter;
import java.util.Properties;

@Component
public class EmailOtpNotificationSender implements OtpNotificationSender {

    private static final Logger log = LoggerFactory.getLogger(EmailOtpNotificationSender.class);

    private final String username;
    private final String fromEmail;
    private final Session session;
    private final boolean enabled;

    public EmailOtpNotificationSender(
            @Value("${app.email.username:}") String username,
            @Value("${app.email.password:}") String password,
            @Value("${app.email.from:}") String from,
            @Value("${app.email.smtp.host:smtp.example.com}") String smtpHost,
            @Value("${app.email.smtp.port:587}") int smtpPort,
            @Value("${app.email.smtp.auth:true}") boolean smtpAuth,
            @Value("${app.email.smtp.starttls.enable:true}") boolean smtpStarttlsEnable,
            @Value("${app.email.smtp.connectiontimeout:10000}") int smtpConnectionTimeout,
            @Value("${app.email.smtp.timeout:10000}") int smtpTimeout,
            @Value("${app.email.smtp.writetimeout:10000}") int smtpWriteTimeout
    ) {
        this.username = username == null ? "" : username.trim();
        String pwd = password == null ? "" : password;
        String configuredFrom = from == null ? "" : from.trim();
        this.fromEmail = configuredFrom.isEmpty() ? this.username : configuredFrom;
        this.enabled = !this.username.isEmpty() && !pwd.isEmpty();

        if (enabled) {
            Properties mailProps = toJavaMailProperties(
                    smtpHost, smtpPort, smtpAuth, smtpStarttlsEnable,
                    smtpConnectionTimeout, smtpTimeout, smtpWriteTimeout);

            String authUser = this.username;
            this.session = Session.getInstance(mailProps, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(authUser, pwd);
                }
            });
            log.info("Email OTP sender initialized (smtp host={}, port={}, from={})",
                    smtpHost, smtpPort, fromEmail);
        } else {
            this.session = null;
            log.warn("Email OTP sender is disabled");
        }
    }

    @Override
    public DeliveryChannel channel() {
        return DeliveryChannel.EMAIL;
    }

    @Override
    public void send(NotificationPayload payload) {
        if (!enabled) {
            throw new IllegalStateException(
                    "Email channel is not configured (set app.email.username / app.email.password)");
        }
        InternetAddress to = parseAddress(payload.destination());
        InternetAddress from = parseAddress(fromEmail);

        try {
            Message message = new MimeMessage(session);
            message.setFrom(from);
            message.setRecipient(Message.RecipientType.TO, to);
            message.setSubject("Your OTP Code");
            message.setText(buildBody(payload));

            Transport.send(message);

            log.info("OTP email sent to {} for userId={} operationId={}",
                    payload.destination(), payload.userId(), payload.operationId());
        } catch (MessagingException ex) {
            throw new IllegalStateException(
                    "Failed to send OTP email to " + payload.destination(), ex);
        }
    }

    private String buildBody(NotificationPayload p) {
        StringBuilder sb = new StringBuilder();
        sb.append("Your verification code is: ").append(p.code()).append(System.lineSeparator());
        if (p.expiresAt() != null) {
            sb.append("Expires at: ")
                    .append(DateTimeFormatter.ISO_INSTANT.format(p.expiresAt()))
                    .append(System.lineSeparator());
        }
        return sb.toString();
    }

    private InternetAddress parseAddress(String raw) {
        try {
            return new InternetAddress(raw, true);
        } catch (AddressException ex) {
            throw new IllegalArgumentException("Invalid email address: " + raw, ex);
        }
    }

    private static Properties toJavaMailProperties(
            String host,
            int port,
            boolean auth,
            boolean starttlsEnable,
            int connectionTimeout,
            int timeout,
            int writeTimeout
    ) {
        Properties props = new Properties();
        props.setProperty("mail.smtp.host", host);
        props.setProperty("mail.smtp.port", String.valueOf(port));
        props.setProperty("mail.smtp.auth", String.valueOf(auth));
        props.setProperty("mail.smtp.starttls.enable", String.valueOf(starttlsEnable));
        props.setProperty("mail.smtp.connectiontimeout", String.valueOf(connectionTimeout));
        props.setProperty("mail.smtp.timeout", String.valueOf(timeout));
        props.setProperty("mail.smtp.writetimeout", String.valueOf(writeTimeout));
        return props;
    }
}
