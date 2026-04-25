package ru.yartsev_vladislav.otp_app.security.jwt;

import ru.yartsev_vladislav.otp_app.domain.Role;

public interface JwtIssuer {

    IssuedToken issue(long userId, String login, Role role);
}
