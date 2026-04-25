package ru.yartsev_vladislav.otp_app.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yartsev_vladislav.otp_app.domain.Role;
import ru.yartsev_vladislav.otp_app.dto.admin.OtpConfigRequest;
import ru.yartsev_vladislav.otp_app.dto.admin.OtpConfigResponse;
import ru.yartsev_vladislav.otp_app.dto.admin.UserResponse;
import ru.yartsev_vladislav.otp_app.security.RequiresAuth;
import ru.yartsev_vladislav.otp_app.service.OtpConfigService;
import ru.yartsev_vladislav.otp_app.service.UserAdminService;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiresAuth(roles = Role.ADMIN)
public class AdminController {

    private final OtpConfigService otpConfigService;
    private final UserAdminService userAdminService;

    public AdminController(OtpConfigService otpConfigService, UserAdminService userAdminService) {
        this.otpConfigService = otpConfigService;
        this.userAdminService = userAdminService;
    }

    @GetMapping("/otp-config")
    public OtpConfigResponse getOtpConfig() {
        return otpConfigService.get();
    }

    @PutMapping("/otp-config")
    public OtpConfigResponse updateOtpConfig(@Valid @RequestBody OtpConfigRequest request) {
        return otpConfigService.update(request);
    }

    @GetMapping("/users")
    public List<UserResponse> listUsers() {
        return userAdminService.listNonAdminUsers();
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") long id) {
        userAdminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
