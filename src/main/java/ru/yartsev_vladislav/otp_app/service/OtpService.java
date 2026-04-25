package ru.yartsev_vladislav.otp_app.service;

import ru.yartsev_vladislav.otp_app.dto.otp.GenerateOtpRequest;
import ru.yartsev_vladislav.otp_app.dto.otp.GenerateOtpResponse;
import ru.yartsev_vladislav.otp_app.dto.otp.ValidateOtpRequest;
import ru.yartsev_vladislav.otp_app.dto.otp.ValidateOtpResponse;

public interface OtpService {

    GenerateOtpResponse generate(long userId, GenerateOtpRequest request);

    ValidateOtpResponse validate(long userId, ValidateOtpRequest request);
}
