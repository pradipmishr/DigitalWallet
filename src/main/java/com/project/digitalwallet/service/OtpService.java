package com.project.digitalwallet.service;

public interface OtpService {
    void sendOtp(String phoneNumber);
    void resendOtp(String email);
    boolean verifyOtp(String phoneNumber, String otp);
}
