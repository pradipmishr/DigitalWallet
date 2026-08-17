package com.project.digitalwallet.service.impl;

import com.project.digitalwallet.common.enums.NotificationType;
import com.project.digitalwallet.common.util.WalletTransactionEvent;
import com.project.digitalwallet.dto.*;
import com.project.digitalwallet.entity.BlacklistedToken;
import com.project.digitalwallet.entity.User;
import com.project.digitalwallet.mapper.UserMapper;
import com.project.digitalwallet.repository.BlacklistedTokenRepository;
import com.project.digitalwallet.repository.UserRepository;
import com.project.digitalwallet.security.JwtUtil;
import com.project.digitalwallet.security.UserPrincipal;
import com.project.digitalwallet.service.AuditLogService;
import com.project.digitalwallet.service.AuthService;
import com.project.digitalwallet.service.OtpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final LoginAttemptService loginAttemptService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final OtpService otpService;
    private final AuditLogService auditLogService;
    private final HttpServletRequest httpServletRequest;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final BlacklistedTokenRepository blacklistedTokenRepository;



    // In-memory token store (Token -> ResetTokenInfo). For production clusters, persist this in DB or Redis.
    private final Map<String, ResetTokenInfo> resetTokenStore = new ConcurrentHashMap<>();

    private record ResetTokenInfo(String email, LocalDateTime expiresAt) {}

//    public LoginResponse login(LoginRequest request) {
//
//        Authentication authentication =
//                authenticationManager.authenticate(
//                        new UsernamePasswordAuthenticationToken(
//                                request.getPhoneNumber(),
//                                request.getPassword()
//                        )
//                );
//
//        UserPrincipal principal =
//                (UserPrincipal) authentication.getPrincipal();
//
//        User user = principal.getUser();
//
//        String token = jwtUtil.generateToken(principal);
//
//        return UserMapper.toLoginResponse(user, token);
//    }
public LoginResponse login(LoginRequest request) {
    String phoneNumber = request.getPhoneNumber();

    // 1. Check if the user is currently locked out in Redis
    if (loginAttemptService.isBlocked(phoneNumber)) {
        long remainingSeconds = loginAttemptService.getRemainingLockoutTimeSeconds(phoneNumber);
        long remainingMinutes = (long) Math.ceil(remainingSeconds / 60.0);

        throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many failed login attempts. Account locked for " + remainingMinutes + " minute(s)."
        );
    }

    try {
        // 2. Attempt authentication
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        phoneNumber,
                        request.getPassword()
                )
        );

        // 3. Clear failed attempts on successful login
        loginAttemptService.loginSucceeded(phoneNumber);

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = principal.getUser();
        String token = jwtUtil.generateToken(principal);

        return UserMapper.toLoginResponse(user, token);

    } catch (BadCredentialsException ex) {
        // 4. Increment failed attempt counter in Redis
        loginAttemptService.loginFailed(phoneNumber);
        throw ex;
    }
}
    @Override
    public void initiateForgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + request.getEmail()));

        // Generates 6-digit OTP, sets 5-min expiry, saves to DB, sends email
        otpService.sendOtp(user.getEmail());

        auditLogService.logEvent(
                user.getId(),
                "FORGOT_PASSWORD_REQUESTED",
                "Password reset OTP sent to email: " + request.getEmail(),
                httpServletRequest
        );
    }

    @Override
    @Transactional
    public VerifyResetOtpResponse verifyResetOtp(VerifyResetOtpRequest request) {
        // 1. Verify OTP (validates correctness, expiry, and unverified state)
        otpService.verifyOtp(request.getEmail(), request.getOtp());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + request.getEmail()));

        // 2. Generate short-lived reset token (valid for 10 minutes)
        String resetToken = UUID.randomUUID().toString();
        resetTokenStore.put(resetToken, new ResetTokenInfo(request.getEmail(), LocalDateTime.now().plusMinutes(10)));

        auditLogService.logEvent(
                user.getId(),
                "RESET_OTP_VERIFIED",
                "Password reset OTP verified successfully. Generated reset token.",
                httpServletRequest
        );

        return new VerifyResetOtpResponse(resetToken);
    }

    @Override
    @Transactional
    public void resetPasswordWithToken(ResetPasswordWithTokenRequest request) {
        String token = request.getResetToken();
        ResetTokenInfo tokenInfo = resetTokenStore.get(token);

        // 1. Validate Token
        if (tokenInfo == null) {
            throw new IllegalArgumentException("Invalid or expired reset token.");
        }

        if (tokenInfo.expiresAt().isBefore(LocalDateTime.now())) {
            resetTokenStore.remove(token);
            throw new IllegalStateException("Reset token has expired. Please request a new password reset.");
        }

        // 2. Fetch User and update password
        User user = userRepository.findByEmail(tokenInfo.email())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // 3. Invalidate reset token immediately after use
        resetTokenStore.remove(token);

        auditLogService.logEvent(
                user.getId(),
                "PASSWORD_RESET_SUCCESS",
                "Password updated successfully using reset token.",
                httpServletRequest
        );
        eventPublisher.publishEvent(new WalletTransactionEvent(
                user.getId(),
                user.getPhoneNumber(),
                NotificationType.SECURITY_ALERT, // or NotificationType.PASSWORD_RESET / NotificationType.SYSTEM_ALERT
                BigDecimal.ZERO,
                "NPR",
                "PWD-" + System.currentTimeMillis(), // Unique security reference ID
                "Your account password has been reset successfully. If you did not perform this action, contact support immediately."
        ));
    }

    @Override
    public void logout(HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization token is missing");
        }

        String jwt = authHeader.substring(7);

        // Avoid duplicate tokens
        if (!blacklistedTokenRepository.existsByToken(jwt)) {

            BlacklistedToken blacklistedToken =
                    new BlacklistedToken();

            blacklistedToken.setToken(jwt);

            blacklistedTokenRepository.save(blacklistedToken);
        }
    }

}