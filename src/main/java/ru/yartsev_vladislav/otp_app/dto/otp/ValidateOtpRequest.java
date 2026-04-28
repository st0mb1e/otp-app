package ru.yartsev_vladislav.otp_app.dto.otp;

import jakarta.validation.constraints.NotBlank;

public record ValidateOtpRequest(
        @NotBlank String code
) {
}
