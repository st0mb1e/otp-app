package ru.yartsev_vladislav.otp_app.security.jwt;

public interface JwtParser {

    JwtClaims parse(String token) throws InvalidTokenException;

    class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
