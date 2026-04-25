package ru.yartsev_vladislav.otp_app.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yartsev_vladislav.otp_app.domain.Role;
import ru.yartsev_vladislav.otp_app.dto.otp.GenerateOtpRequest;
import ru.yartsev_vladislav.otp_app.dto.otp.GenerateOtpResponse;
import ru.yartsev_vladislav.otp_app.dto.otp.ValidateOtpRequest;
import ru.yartsev_vladislav.otp_app.dto.otp.ValidateOtpResponse;
import ru.yartsev_vladislav.otp_app.security.CurrentUser;
import ru.yartsev_vladislav.otp_app.security.CurrentUserProvider;
import ru.yartsev_vladislav.otp_app.security.RequiresAuth;
import ru.yartsev_vladislav.otp_app.service.OtpService;

@RestController
@RequestMapping("/api/otp")
@RequiresAuth(roles = Role.USER)
public class OtpController {

    private final OtpService otpService;
    private final CurrentUserProvider currentUserProvider;

    public OtpController(OtpService otpService, CurrentUserProvider currentUserProvider) {
        this.otpService = otpService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/generate")
    public ResponseEntity<GenerateOtpResponse> generate(@Valid @RequestBody GenerateOtpRequest request) {
        CurrentUser user = currentUserProvider.require();
        GenerateOtpResponse response = otpService.generate(user.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/validate")
    public ValidateOtpResponse validate(@Valid @RequestBody ValidateOtpRequest request) {
        CurrentUser user = currentUserProvider.require();
        return otpService.validate(user.id(), request);
    }

    @DeleteMapping("/codes/{id}")
    public ResponseEntity<Void> deleteOwnedCode(@PathVariable("id") long id) {
        CurrentUser user = currentUserProvider.require();
        otpService.deleteOwnedCode(user.id(), id);
        return ResponseEntity.noContent().build();
    }
}
