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

/**
 * Сохраняет OTP-код в текстовый файл в директории, заданной через
 * app.otp.file-output-dir (по умолчанию это корень проекта). На каждую операцию
 * свой файл с именем operation-{operationId}-otp.txt
 *
 * Если файл уже есть, дописываем строку в конец, чтобы сохранять историю
 * перевыпусков для одной и той же операции
 */
@Component
public class FileOtpNotificationSender implements OtpNotificationSender {

    private static final Logger log = LoggerFactory.getLogger(FileOtpNotificationSender.class);

    private final Path outputDir;
    private final Object writeLock = new Object();

    public FileOtpNotificationSender(OtpProperties props) {
        this.outputDir = Path.of(props.fileOutputDir()).toAbsolutePath();
        ensureDirExists();
    }

    @Override
    public DeliveryChannel channel() {
        return DeliveryChannel.FILE;
    }

    @Override
    public void send(NotificationPayload payload) {
        Path target = outputDir.resolve(buildFileName(payload.operationId()));
        String line = formatLine(payload);
        synchronized (writeLock) {
            try {
                Files.writeString(
                        target,
                        line,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                );
            } catch (IOException ex) {
                throw new IllegalStateException("could not write OTP code to file: " + target, ex);
            }
        }
        log.info("OTP saved to file {} (userId={} operationId={})",
                target, payload.userId(), payload.operationId());
    }

    /** Формирует имя файла вида operation-{id}-otp.txt, заранее почистив id от лишних символов,
     * которые в некоторых операционных системах не могут быть в имени файла */
    private static String buildFileName(String operationId) {
        return "operation-" + sanitize(operationId) + "-otp.txt";
    }

    private static String sanitize(String raw) {
        return raw.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String formatLine(NotificationPayload p) {
        return DateTimeFormatter.ISO_INSTANT.format(Instant.now())
                + "|code=" + p.code()
                + "|expiresAt=" + DateTimeFormatter.ISO_INSTANT.format(p.expiresAt())
                + System.lineSeparator();
    }

    private void ensureDirExists() {
        try {
            Files.createDirectories(outputDir);
        } catch (IOException ex) {
            throw new IllegalStateException("could not create OTP output directory: " + outputDir, ex);
        }
    }
}
