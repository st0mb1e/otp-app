package ru.yartsev_vladislav.otp_app.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.yartsev_vladislav.otp_app.config.OtpProperties;
import ru.yartsev_vladislav.otp_app.domain.DeliveryChannel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

@Component
public class FileOtpNotificationSender implements OtpNotificationSender {

    private static final Logger log = LoggerFactory.getLogger(FileOtpNotificationSender.class);

    private final Path outputPath;
    private final Object writeLock = new Object();

    public FileOtpNotificationSender(OtpProperties props) {
        this.outputPath = Path.of(props.fileOutput()).toAbsolutePath();
        ensureParentDirExists();
    }

    @Override
    public DeliveryChannel channel() {
        return DeliveryChannel.FILE;
    }

    @Override
    public void send(NotificationPayload payload) {
        String line = formatLine(payload);
        synchronized (writeLock) {
            try {
                Files.writeString(
                        outputPath,
                        line,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                );
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to write OTP code to file: " + outputPath, ex);
            }
        }
        log.info("OTP code written to file {} for userId={} operationId={}",
                outputPath, payload.userId(), payload.operationId());
    }

    private String formatLine(NotificationPayload p) {
        return DateTimeFormatter.ISO_INSTANT.format(Instant.now())
                + "|userId=" + p.userId()
                + "|login=" + p.login()
                + "|operationId=" + p.operationId()
                + "|code=" + p.code()
                + "|expiresAt=" + DateTimeFormatter.ISO_INSTANT.format(p.expiresAt())
                + System.lineSeparator();
    }

    private void ensureParentDirExists() {
        Path parent = outputPath.getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot create directory for OTP output file: " + parent, ex);
        }
    }
}
