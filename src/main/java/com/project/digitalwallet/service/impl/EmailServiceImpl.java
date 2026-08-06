package com.project.digitalwallet.service.impl;

import com.project.digitalwallet.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendOtpEmail(String toEmail, String otpCode) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Your Digital Wallet Verification Code");

            // HTML Email Template
            String htmlContent = buildOtpEmailTemplate(otpCode);

            // Second parameter 'true' enables HTML parsing
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to send verification email.", e);
        }
    }

    private String buildOtpEmailTemplate(String otpCode) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Verification Code</title>
            </head>
            <body style="margin: 0; padding: 0; background-color: #f4f6f9; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;">
                <table role="presentation" style="width: 100%%; border-collapse: collapse; padding: 20px 0;">
                    <tr>
                        <td align="center">
                            <table role="presentation" style="width: 100%%; max-width: 520px; background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08); overflow: hidden; margin: 20px 0;">
                                <!-- Header -->
                                <tr>
                                    <td style="background-color: #1e293b; padding: 28px 24px; text-align: center;">
                                        <h1 style="color: #ffffff; margin: 0; font-size: 22px; font-weight: 600; letter-spacing: 0.5px;">Digital Wallet</h1>
                                    </td>
                                </tr>
                                
                                <!-- Content Body -->
                                <tr>
                                    <td style="padding: 32px 28px; text-align: center;">
                                        <h2 style="color: #0f172a; margin: 0 0 12px 0; font-size: 20px; font-weight: 600;">Verify Your Account</h2>
                                        <p style="color: #475569; font-size: 15px; line-height: 1.5; margin: 0 0 24px 0;">
                                            Please use the verification code below to complete your registration.
                                        </p>
                                        
                                        <!-- OTP Box -->
                                        <div style="background-color: #f1f5f9; border: 1px solid #e2e8f0; border-radius: 8px; padding: 18px; margin: 20px 0; display: inline-block; width: 80%%;">
                                            <span style="font-family: 'Courier New', Courier, monospace; font-size: 32px; font-weight: 700; color: #2563eb; letter-spacing: 8px;">%s</span>
                                        </div>
                                        
                                        <p style="color: #64748b; font-size: 13px; line-height: 1.5; margin: 20px 0 0 0;">
                                            This code is valid for <strong>5 minutes</strong>. If you did not request this code, please ignore this email.
                                        </p>
                                    </td>
                                </tr>
                                
                                <!-- Footer -->
                                <tr>
                                    <td style="background-color: #f8fafc; padding: 20px; text-align: center; border-top: 1px solid #f1f5f9;">
                                        <p style="color: #94a3b8; font-size: 12px; margin: 0;">
                                            &copy; %d Digital Wallet Application. All rights reserved.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(otpCode, java.time.Year.now().getValue());
    }
}
