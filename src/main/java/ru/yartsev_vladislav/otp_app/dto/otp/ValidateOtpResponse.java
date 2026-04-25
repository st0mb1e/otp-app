package ru.yartsev_vladislav.otp_app.dto.otp;

public record ValidateOtpResponse(
        boolean valid,
        String message
) {
    public static ValidateOtpResponse ok() {
        return new ValidateOtpResponse(true, "Code accepted");
    }

    public static ValidateOtpResponse invalid(String reason) {
        return new ValidateOtpResponse(false, reason);
    }
}
