package ru.yartsev_vladislav.otp_app.security;

public interface CurrentUserProvider {

    CurrentUser require();
}
