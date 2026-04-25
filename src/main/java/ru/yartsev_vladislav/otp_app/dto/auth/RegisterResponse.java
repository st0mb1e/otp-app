package ru.yartsev_vladislav.otp_app.dto.auth;

import ru.yartsev_vladislav.otp_app.domain.Role;

public record RegisterResponse(
        Long id,
        String login,
        Role role
) {
}
