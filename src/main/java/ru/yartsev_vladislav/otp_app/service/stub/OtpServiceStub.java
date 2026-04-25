package ru.yartsev_vladislav.otp_app.service.stub;

import org.springframework.stereotype.Service;
import ru.yartsev_vladislav.otp_app.dto.otp.GenerateOtpRequest;
import ru.yartsev_vladislav.otp_app.dto.otp.GenerateOtpResponse;
import ru.yartsev_vladislav.otp_app.dto.otp.ValidateOtpRequest;
import ru.yartsev_vladislav.otp_app.dto.otp.ValidateOtpResponse;
import ru.yartsev_vladislav.otp_app.service.OtpService;

@Service
public class OtpServiceStub implements OtpService {

    @Override
    public GenerateOtpResponse generate(long userId, GenerateOtpRequest request) {
        throw new UnsupportedOperationException("OtpService.generate is not implemented yet");
    }

    @Override
    public ValidateOtpResponse validate(long userId, ValidateOtpRequest request) {
        throw new UnsupportedOperationException("OtpService.validate is not implemented yet");
    }
}
