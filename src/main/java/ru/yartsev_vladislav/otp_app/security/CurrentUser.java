package ru.yartsev_vladislav.otp_app.security;

import ru.yartsev_vladislav.otp_app.domain.Role;

public record CurrentUser(long id, String login, Role role) {
}
