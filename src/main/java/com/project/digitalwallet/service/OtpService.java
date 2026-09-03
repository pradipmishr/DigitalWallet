package com.project.digitalwallet.service;

public interface OtpService {

    // =========================================================
    // REGISTRATION OTP
    // =========================================================

    void sendRegistrationOtp(String email);

    boolean verifyRegistrationOtp(
            String email,
            String code
    );

    void resendRegistrationOtp(String email);


    // =========================================================
    // PIN RESET OTP
    // =========================================================

    void sendPinResetOtp(
            Long userId,
            String email
    );

    boolean verifyPinResetOtp(
            String email,
            String code
    );

    void resendPinResetOtp(String email);


    // =========================================================
    // FORGOT PASSWORD OTP
    // =========================================================

    void sendForgotPasswordOtp(
            Long userId,
            String email
    );

    boolean verifyForgotPasswordOtp(
            String email,
            String code
    );

    void resendForgotPasswordOtp(String email);
}
