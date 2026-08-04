package com.project.digitalwallet.service;

public interface OtpService {
    void sendOtp(String phoneNumber);
    boolean verifyOtp(String phoneNumber, String otp);
}
