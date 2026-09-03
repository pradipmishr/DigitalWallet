package com.project.digitalwallet.service;

public interface OtpService {

    /*
     * Registration OTP
     */
    void sendRegistrationOtp(String email);

    boolean verifyRegistrationOtp(
            String email,
            String code
    );
    void resendRegistrationOtp(
            String email
    );

    /*
     * PIN reset OTP
     */
    void sendPinResetOtp(
            Long userId,
            String email
    );

    boolean verifyPinResetOtp(
            String email,
            String code
    );

    void resendPinResetOtp(
            String email
    );
}
