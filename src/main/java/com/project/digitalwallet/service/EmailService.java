package com.project.digitalwallet.service;

public interface EmailService {
    void sendOtpEmail(String toEmail, String otpCode);
}