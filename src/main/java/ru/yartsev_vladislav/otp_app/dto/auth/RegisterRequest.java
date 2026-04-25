package ru.yartsev_vladislav.otp_app.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ru.yartsev_vladislav.otp_app.domain.Role;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 64) String login,
        @NotBlank @Size(min = 6, max = 128) String password,
        Role role
) {
}
