package com.project.digitalwallet.service.impl;

import com.project.digitalwallet.common.util.OtpGenerator;
import com.project.digitalwallet.entity.Otp;
import com.project.digitalwallet.repository.OtpRepository;
import com.project.digitalwallet.service.OtpService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {
    private final OtpRepository otpRepository;
    private final OtpGenerator otpGenerator;
    private final TwilioSmsService twilioSmsService;

    @Override
    @Transactional
    public void sendOtp(String phoneNumber) {
        otpRepository.deleteByPhoneNumber(phoneNumber);

        String code = OtpGenerator.generateOtp();

        Otp otp = Otp.builder()
                .phoneNumber(phoneNumber)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .verified(false)
                .build();

        otpRepository.save(otp);

        // Send real SMS code via Twilio
        //twilioSmsService.sendSms(phoneNumber, "Your Digital Wallet code is: " + code);
        System.out.println("Your Digital Wallet code is: " + code);
    }

    @Override
    @Transactional
    public boolean verifyOtp(String phoneNumber, String code) {

        Otp otp = otpRepository.findTopByPhoneNumberOrderByCreatedAtDesc(phoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("No OTP found for this phone number."));

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
}


