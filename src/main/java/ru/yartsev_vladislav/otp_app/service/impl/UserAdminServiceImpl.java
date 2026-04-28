package ru.yartsev_vladislav.otp_app.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yartsev_vladislav.otp_app.domain.Role;
import ru.yartsev_vladislav.otp_app.domain.User;
import ru.yartsev_vladislav.otp_app.dto.admin.UserResponse;
import ru.yartsev_vladislav.otp_app.exception.ForbiddenException;
import ru.yartsev_vladislav.otp_app.exception.NotFoundException;
import ru.yartsev_vladislav.otp_app.repository.OtpCodeRepository;
import ru.yartsev_vladislav.otp_app.repository.UserRepository;
import ru.yartsev_vladislav.otp_app.service.UserAdminService;

import java.util.List;

@Service
public class UserAdminServiceImpl implements UserAdminService {

    private static final Logger log = LoggerFactory.getLogger(UserAdminServiceImpl.class);

    private final UserRepository userRepository;
    private final OtpCodeRepository otpCodeRepository;

    public UserAdminServiceImpl(UserRepository userRepository, OtpCodeRepository otpCodeRepository) {
        this.userRepository = userRepository;
        this.otpCodeRepository = otpCodeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> listNonAdminUsers() {
        return userRepository.findAllByRoleNot(Role.ADMIN).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteUser(long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("user not found: " + userId));

        if (user.getRole() == Role.ADMIN) {
            throw new ForbiddenException("cannot delete admin user");
        }

        long deletedCodes = otpCodeRepository.deleteByUserId(userId);
        userRepository.delete(user);

        log.info("user deleted: userId={} login={} (otp codes deleted: {})",
                user.getId(), user.getLogin(), deletedCodes);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getLogin(), user.getRole());
    }
}
