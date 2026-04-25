package ru.yartsev_vladislav.otp_app.dto.otp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ru.yartsev_vladislav.otp_app.domain.DeliveryChannel;

public record GenerateOtpRequest(
        @NotBlank String operationId,
        @NotNull DeliveryChannel channel,
        String destination
) {
}
