package ru.yartsev_vladislav.otp_app.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.otp")
public record OtpProperties(
        @NotBlank String fileOutputDir,
        @Min(4) @Max(10) int defaultCodeLength,
        @Min(10) long defaultTtlSeconds
) {
}
