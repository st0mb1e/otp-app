package ru.yartsev_vladislav.otp_app.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yartsev_vladislav.otp_app.domain.Role;
import ru.yartsev_vladislav.otp_app.domain.User;
import ru.yartsev_vladislav.otp_app.dto.auth.LoginRequest;
import ru.yartsev_vladislav.otp_app.dto.auth.RegisterRequest;
import ru.yartsev_vladislav.otp_app.dto.auth.RegisterResponse;
import ru.yartsev_vladislav.otp_app.dto.auth.TokenResponse;
import ru.yartsev_vladislav.otp_app.exception.ConflictException;
import ru.yartsev_vladislav.otp_app.exception.UnauthorizedException;
import ru.yartsev_vladislav.otp_app.repository.UserRepository;
import ru.yartsev_vladislav.otp_app.security.jwt.IssuedToken;
import ru.yartsev_vladislav.otp_app.security.jwt.JwtIssuer;
import ru.yartsev_vladislav.otp_app.service.AuthService;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtIssuer jwtIssuer;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtIssuer jwtIssuer
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtIssuer = jwtIssuer;
    }

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        Role role = request.role() == null ? Role.USER : request.role();

        if (userRepository.existsByLogin(request.login())) {
            throw new ConflictException("Login '" + request.login() + "' is already taken");
        }
        if (role == Role.ADMIN && userRepository.existsByRole(Role.ADMIN)) {
            throw new ConflictException("Administrator already exists; only one is allowed");
        }

        User user = new User();
        user.setLogin(request.login());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(role);

        User saved = userRepository.save(user);
        log.info("Registered user id={} login={} role={}", saved.getId(), saved.getLogin(), saved.getRole());

        return new RegisterResponse(saved.getId(), saved.getLogin(), saved.getRole());
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByLogin(request.login())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        IssuedToken issued = jwtIssuer.issue(user.getId(), user.getLogin(), user.getRole());
        log.info("Issued token for userId={} login={} expiresAt={}", user.getId(), user.getLogin(), issued.expiresAt());

        return TokenResponse.bearer(issued.token(), issued.expiresAt());
    }
}
