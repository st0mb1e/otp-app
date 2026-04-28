package ru.yartsev_vladislav.otp_app.service.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.yartsev_vladislav.otp_app.domain.OtpStatus;
import ru.yartsev_vladislav.otp_app.repository.OtpCodeRepository;

import java.time.Instant;

/**
 * Шедулер, который периодически переводит просроченные OTP-коды
 * из статуса {@link OtpStatus#ACTIVE} в {@link OtpStatus#EXPIRED}.
 */
@Component
public class OtpExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(OtpExpirationScheduler.class);

    private final OtpCodeRepository otpCodeRepository;

    public OtpExpirationScheduler(OtpCodeRepository otpCodeRepository) {
        this.otpCodeRepository = otpCodeRepository;
    }

    /**
     * Помечает все активные коды с истёкшим сроком жизни как {@link OtpStatus#EXPIRED}.
     * Запускается с задержкой {@code app.otp.expiration-scan-interval} после
     * предыдущего завершения.
     */
    @Scheduled(
            fixedDelayString = "${app.otp.expiration-scan-interval:PT1M}",
            initialDelayString = "${app.otp.expiration-scan-initial-delay:PT10S}"
    )
    @Transactional
    public void markExpiredCodes() {
        Instant now = Instant.now();
        int updated = otpCodeRepository.markExpired(OtpStatus.ACTIVE, OtpStatus.EXPIRED, now);
        if (updated > 0) {
            log.info("OTP expiration sweep: marked {} code(s) as EXPIRED at {}", updated, now);
        } else {
            log.debug("OTP expiration sweep: no active expired codes at {}", now);
        }
    }
}
