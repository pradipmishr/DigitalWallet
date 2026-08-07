package com.project.digitalwallet.service.impl;

import com.project.digitalwallet.common.util.OtpGenerator;
import com.project.digitalwallet.entity.Otp;
import com.project.digitalwallet.repository.OtpRepository;
import com.project.digitalwallet.service.EmailService;
import com.project.digitalwallet.service.OtpService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final OtpRepository otpRepository;
    private final EmailService emailService;

    @Override
    @Transactional
    public void sendOtp(String email) {
        // Delete any existing unverified OTP for this email
        otpRepository.deleteByEmail(email);

        String code = OtpGenerator.generateOtp();

        Otp otp = Otp.builder()
                .email(email)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .verified(false)
                .build();

        otpRepository.save(otp);

        // Send Email
        emailService.sendOtpEmail(email, code);
        System.out.println("Sent OTP " + code + " to email: " + email);
    }

    @Override
    @Transactional
    public boolean verifyOtp(String email, String code) {
        Otp otp = otpRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new IllegalArgumentException("No OTP found for this email address."));

        if (otp.isVerified()) {
            throw new IllegalStateException("This OTP has already been used.");
        }

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("OTP has expired. Please request a new one.");
        }

        if (!otp.getCode().equals(code)) {
            throw new IllegalArgumentException("Invalid OTP code.");
        }

        otp.setVerified(true);
        return true;
    }
    @Override
    @Transactional
    public void resendOtp(String email) {
        // 1. Check if an OTP was recently generated to enforce a 60-second cooldown
        Optional<Otp> existingOtp = otpRepository.findTopByEmailOrderByCreatedAtDesc(email);

        if (existingOtp.isPresent()) {
            Otp otp = existingOtp.get();
            if (otp.getCreatedAt() != null &&
                    otp.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(60))) {
                throw new IllegalStateException("Please wait at least 60 seconds before requesting a new OTP.");
            }
        }

        // 2. Reuse sendOtp logic (deletes old OTP, creates new code, saves, sends email)
        sendOtp(email);
    }
}


