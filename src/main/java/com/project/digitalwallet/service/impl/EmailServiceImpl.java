package com.project.digitalwallet.service.impl;

import com.project.digitalwallet.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendOtpEmail(String toEmail, String otpCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("your-email@gmail.com");
        message.setTo(toEmail);
        message.setSubject("Your Digital Wallet Verification Code");
        message.setText("Hello,\n\nYour verification code is: " + otpCode +
                "\n\nThis code will expire in 5 minutes.\n\nThank you!");

        mailSender.send(message);
    }
}
