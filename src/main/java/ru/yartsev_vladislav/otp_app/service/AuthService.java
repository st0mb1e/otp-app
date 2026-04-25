package ru.yartsev_vladislav.otp_app.service;

import ru.yartsev_vladislav.otp_app.dto.auth.LoginRequest;
import ru.yartsev_vladislav.otp_app.dto.auth.RegisterRequest;
import ru.yartsev_vladislav.otp_app.dto.auth.RegisterResponse;
import ru.yartsev_vladislav.otp_app.dto.auth.TokenResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    TokenResponse login(LoginRequest request);
}
