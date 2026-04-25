package ru.yartsev_vladislav.otp_app.service.stub;

import org.springframework.stereotype.Service;
import ru.yartsev_vladislav.otp_app.dto.admin.OtpConfigRequest;
import ru.yartsev_vladislav.otp_app.dto.admin.OtpConfigResponse;
import ru.yartsev_vladislav.otp_app.service.OtpConfigService;

@Service
public class OtpConfigServiceStub implements OtpConfigService {

    @Override
    public OtpConfigResponse get() {
        throw new UnsupportedOperationException("OtpConfigService.get is not implemented yet");
    }

    @Override
    public OtpConfigResponse update(OtpConfigRequest request) {
        throw new UnsupportedOperationException("OtpConfigService.update is not implemented yet");
    }
}
