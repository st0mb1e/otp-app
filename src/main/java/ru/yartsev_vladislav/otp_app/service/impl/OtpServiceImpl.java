package ru.yartsev_vladislav.otp_app.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yartsev_vladislav.otp_app.domain.DeliveryChannel;
import ru.yartsev_vladislav.otp_app.domain.OtpCode;
import ru.yartsev_vladislav.otp_app.domain.OtpStatus;
import ru.yartsev_vladislav.otp_app.domain.User;
import ru.yartsev_vladislav.otp_app.dto.admin.OtpConfigResponse;
import ru.yartsev_vladislav.otp_app.dto.otp.GenerateOtpRequest;
import ru.yartsev_vladislav.otp_app.dto.otp.GenerateOtpResponse;
import ru.yartsev_vladislav.otp_app.dto.otp.ValidateOtpRequest;
import ru.yartsev_vladislav.otp_app.dto.otp.ValidateOtpResponse;
import ru.yartsev_vladislav.otp_app.exception.NotFoundException;
import ru.yartsev_vladislav.otp_app.repository.OtpCodeRepository;
import ru.yartsev_vladislav.otp_app.repository.UserRepository;
import ru.yartsev_vladislav.otp_app.service.OtpConfigService;
import ru.yartsev_vladislav.otp_app.service.OtpService;
import ru.yartsev_vladislav.otp_app.service.notification.NotificationPayload;
import ru.yartsev_vladislav.otp_app.service.notification.OtpNotificationSender;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class OtpServiceImpl implements OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpServiceImpl.class);

    private final UserRepository userRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final OtpConfigService otpConfigService;
    private final Map<DeliveryChannel, OtpNotificationSender> sendersByChannel;
    private final SecureRandom random = new SecureRandom();

    public OtpServiceImpl(
            UserRepository userRepository,
            OtpCodeRepository otpCodeRepository,
            OtpConfigService otpConfigService,
            List<OtpNotificationSender> senders
    ) {
        this.userRepository = userRepository;
        this.otpCodeRepository = otpCodeRepository;
        this.otpConfigService = otpConfigService;
        this.sendersByChannel = new EnumMap<>(DeliveryChannel.class);
        for (OtpNotificationSender sender : senders) {
            this.sendersByChannel.put(sender.channel(), sender);
        }
    }

    @Override
    @Transactional
    public GenerateOtpResponse generate(long userId, GenerateOtpRequest request) {
        DeliveryChannel channel = request.channel();
        validateDestination(channel, request.destination());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        OtpConfigResponse config = otpConfigService.get();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofSeconds(config.ttlSeconds()));
        String code = generateNumericCode(config.codeLength());

        OtpCode entity = new OtpCode();
        entity.setUserId(user.getId());
        entity.setOperationId(request.operationId());
        entity.setCode(code);
        entity.setStatus(OtpStatus.ACTIVE);
        entity.setCreatedAt(now);
        entity.setExpiresAt(expiresAt);
        OtpCode saved = otpCodeRepository.save(entity);

        OtpNotificationSender sender = sendersByChannel.get(channel);
        if (sender == null) {
            throw new IllegalStateException("No notification sender registered for channel " + channel);
        }
        NotificationPayload payload = new NotificationPayload(
                user.getId(),
                user.getLogin(),
                request.operationId(),
                code,
                request.destination(),
                expiresAt
        );
        sender.send(payload);

        log.info("OTP generated id={} userId={} operationId={} channel={} expiresAt={}",
                saved.getId(), user.getId(), request.operationId(), channel, expiresAt);

        return new GenerateOtpResponse(saved.getId(), saved.getOperationId(), channel, expiresAt);
    }

    @Override
    @Transactional
    public ValidateOtpResponse validate(long userId, ValidateOtpRequest request) {
        OtpCode active = otpCodeRepository
                .findFirstByUserIdAndOperationIdAndStatusOrderByCreatedAtDesc(
                        userId, request.operationId(), OtpStatus.ACTIVE
                )
                .orElse(null);

        if (active == null) {
            log.info("OTP validation: no active code for userId={} operationId={}", userId, request.operationId());
            return ValidateOtpResponse.invalid("No active code for this operation");
        }

        Instant now = Instant.now();
        if (active.getExpiresAt().isBefore(now)) {
            active.setStatus(OtpStatus.EXPIRED);
            otpCodeRepository.save(active);
            log.info("OTP validation: code id={} expired", active.getId());
            return ValidateOtpResponse.invalid("Code expired");
        }

        if (!active.getCode().equals(request.code())) {
            log.info("OTP validation: code mismatch for userId={} operationId={}", userId, request.operationId());
            return ValidateOtpResponse.invalid("Wrong code");
        }

        active.setStatus(OtpStatus.USED);
        otpCodeRepository.save(active);
        log.info("OTP validation OK: code id={} userId={} operationId={}",
                active.getId(), userId, request.operationId());
        return ValidateOtpResponse.ok();
    }

    private void validateDestination(DeliveryChannel channel, String destination) {
        if (channel == DeliveryChannel.FILE) {
            return;
        }
        if (destination == null || destination.isBlank()) {
            throw new IllegalArgumentException("destination is required for channel " + channel);
        }
    }

    private String generateNumericCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append((char) ('0' + random.nextInt(10)));
        }
        return sb.toString();
    }
}
