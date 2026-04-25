package ru.yartsev_vladislav.otp_app.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yartsev_vladislav.otp_app.config.OtpProperties;
import ru.yartsev_vladislav.otp_app.domain.OtpConfig;
import ru.yartsev_vladislav.otp_app.dto.admin.OtpConfigRequest;
import ru.yartsev_vladislav.otp_app.dto.admin.OtpConfigResponse;
import ru.yartsev_vladislav.otp_app.repository.OtpConfigRepository;
import ru.yartsev_vladislav.otp_app.service.OtpConfigService;

@Service
public class OtpConfigServiceImpl implements OtpConfigService {

    private static final Logger log = LoggerFactory.getLogger(OtpConfigServiceImpl.class);

    private final OtpConfigRepository repository;
    private final OtpProperties properties;

    public OtpConfigServiceImpl(OtpConfigRepository repository, OtpProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Override
    @Transactional
    public OtpConfigResponse get() {
        OtpConfig config = loadOrInitialize();
        return toResponse(config);
    }

    @Override
    @Transactional
    public OtpConfigResponse update(OtpConfigRequest request) {
        OtpConfig config = repository.findById(OtpConfig.SINGLETON_ID).orElseGet(this::newSingleton);
        config.setCodeLength(request.codeLength());
        config.setTtlSeconds(request.ttlSeconds());
        OtpConfig saved = repository.save(config);
        log.info("OTP config updated: codeLength={} ttlSeconds={}", saved.getCodeLength(), saved.getTtlSeconds());
        return toResponse(saved);
    }

    private OtpConfig loadOrInitialize() {
        return repository.findById(OtpConfig.SINGLETON_ID).orElseGet(() -> {
            OtpConfig defaults = newSingleton();
            defaults.setCodeLength(properties.defaultCodeLength());
            defaults.setTtlSeconds(properties.defaultTtlSeconds());
            OtpConfig saved = repository.save(defaults);
            log.info("OTP config initialized with defaults: codeLength={} ttlSeconds={}",
                    saved.getCodeLength(), saved.getTtlSeconds());
            return saved;
        });
    }

    private OtpConfig newSingleton() {
        OtpConfig config = new OtpConfig();
        config.setId(OtpConfig.SINGLETON_ID);
        return config;
    }

    private OtpConfigResponse toResponse(OtpConfig config) {
        return new OtpConfigResponse(config.getCodeLength(), config.getTtlSeconds());
    }
}
