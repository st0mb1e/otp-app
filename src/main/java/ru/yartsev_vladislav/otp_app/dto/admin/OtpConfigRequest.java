package ru.yartsev_vladislav.otp_app.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OtpConfigRequest(
        @NotNull @Min(4) @Max(10) Integer codeLength,
        @NotNull @Min(10) Long ttlSeconds
) {
}
