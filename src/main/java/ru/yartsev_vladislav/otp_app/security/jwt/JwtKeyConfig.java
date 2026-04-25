package ru.yartsev_vladislav.otp_app.security.jwt;

import io.jsonwebtoken.security.Keys;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Configuration
public class JwtKeyConfig {
    @Bean
    public SecretKey jwtSigningKey(JwtProperties props) {
        try {
            byte[] hashed = MessageDigest.getInstance("SHA-256")
                    .digest(props.secret().getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }
}
