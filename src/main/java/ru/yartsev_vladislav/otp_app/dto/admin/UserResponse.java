package ru.yartsev_vladislav.otp_app.dto.admin;

import ru.yartsev_vladislav.otp_app.domain.Role;

public record UserResponse(
        Long id,
        String login,
        Role role
) {
}
