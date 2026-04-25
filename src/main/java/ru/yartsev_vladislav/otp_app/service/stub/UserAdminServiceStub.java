package ru.yartsev_vladislav.otp_app.service.stub;

import org.springframework.stereotype.Service;
import ru.yartsev_vladislav.otp_app.dto.admin.UserResponse;
import ru.yartsev_vladislav.otp_app.service.UserAdminService;

import java.util.List;

@Service
public class UserAdminServiceStub implements UserAdminService {

    @Override
    public List<UserResponse> listNonAdminUsers() {
        throw new UnsupportedOperationException("UserAdminService.listNonAdminUsers is not implemented yet");
    }

    @Override
    public void deleteUser(long userId) {
        throw new UnsupportedOperationException("UserAdminService.deleteUser is not implemented yet");
    }
}
