package ru.yartsev_vladislav.otp_app.service;

import ru.yartsev_vladislav.otp_app.dto.admin.UserResponse;

import java.util.List;

public interface UserAdminService {

    List<UserResponse> listNonAdminUsers();

    void deleteUser(long userId);
}
