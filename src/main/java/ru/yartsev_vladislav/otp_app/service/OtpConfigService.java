package ru.yartsev_vladislav.otp_app.service;

import ru.yartsev_vladislav.otp_app.dto.admin.OtpConfigRequest;
import ru.yartsev_vladislav.otp_app.dto.admin.OtpConfigResponse;

public interface OtpConfigService {

    OtpConfigResponse get();

    OtpConfigResponse update(OtpConfigRequest request);
}
