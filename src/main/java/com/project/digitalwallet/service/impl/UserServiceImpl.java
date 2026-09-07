package com.project.digitalwallet.service.impl;

import com.project.digitalwallet.common.enums.NotificationType;
import com.project.digitalwallet.common.util.WalletTransactionEvent;
import com.project.digitalwallet.dto.*;
import com.project.digitalwallet.entity.User;
import com.project.digitalwallet.mapper.UserMapper;
import com.project.digitalwallet.repository.UserRepository;
import com.project.digitalwallet.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final WalletService walletService;

    private final OtpService otpService;

    private final PasswordEncoder passwordEncoder;

    private final AuditLogService auditLogService;

    private final HttpServletRequest httpServletRequest;

    private final ApplicationEventPublisher eventPublisher;






    /*
     * Existing PIN reset token store.
     */
    private final Map<String, PinResetTokenInfo> pinResetTokenStore =
            new ConcurrentHashMap<>();

    private record PinResetTokenInfo(
            Long userId,
            String email,
            LocalDateTime expiresAt
    ) {}





    // =========================================================
    // USER MANAGEMENT
    // =========================================================

    @Override
    public List<UserDto> getAllUsers() {

        return UserMapper.toUserDto(
                userRepository.findAll()
        );
    }


    // =========================================================
    // TRANSACTION PIN
    // =========================================================

    @Override
    @Transactional
    public void setTransactionPin(
            String phoneNumber,
            String pin
    ) {

        User user =
                userRepository.findByPhoneNumber(
                                phoneNumber
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"
                                )
                        );


        user.setTransactionPin(
                passwordEncoder.encode(pin)
        );


        eventPublisher.publishEvent(
                new WalletTransactionEvent(
                        user.getId(),
                        user.getPhoneNumber(),
                        NotificationType.SECURITY_ALERT,
                        BigDecimal.ZERO,
                        "NPR",
                        "SEC-" + System.currentTimeMillis(),
                        "Your transaction PIN has been successfully set."
                )
        );


        userRepository.save(user);
    }


    // =========================================================
    // PIN RESET
    // =========================================================

    @Override
    public void initiatePinReset(
            UserDto userDto
    ) {

        String email =
                userDto.getEmail()
                        .trim()
                        .toLowerCase();

        otpService.sendPinResetOtp(
                userDto.getId(),
                email
        );

        auditLogService.logEvent(
                userDto.getId(),
                "FORGOT_PIN_REQUESTED",
                "Transaction PIN reset OTP sent to email: "
                        + email,
                httpServletRequest
        );
    }


    @Override
    @Transactional
    public VerifyPinOtpResponse verifyPinOtp(
            UserDto userDto,
            VerifyPinOtpRequest request
    ) {

        String email =
                userDto.getEmail()
                        .trim()
                        .toLowerCase();


        /*
         * Verify PIN reset OTP.
         *
         * OtpService handles:
         * - expiration
         * - 3 attempts
         * - attempt persistence
         * - OTP deletion
         */
        otpService.verifyPinResetOtp(
                email,
                request.getOtp()
        );


        /*
         * Generate short-lived reset token.
         */
        String resetToken =
                UUID.randomUUID().toString();


        pinResetTokenStore.put(
                resetToken,
                new PinResetTokenInfo(
                        userDto.getId(),
                        email,
                        LocalDateTime.now()
                                .plusMinutes(10)
                )
        );


        auditLogService.logEvent(
                userDto.getId(),
                "PIN_RESET_OTP_VERIFIED",
                "Transaction PIN reset OTP verified successfully. "
                        + "Generated reset token.",
                httpServletRequest
        );


        return new VerifyPinOtpResponse(
                resetToken
        );
    }



    @Override
    @Transactional
    public void resetPinWithToken(
            UserDto userDto,
            ResetPinWithTokenRequest request
    ) {

        String token =
                request.getResetToken();


        PinResetTokenInfo tokenInfo =
                pinResetTokenStore.get(token);


        /*
         * Validate token presence.
         */
        if (tokenInfo == null) {

            throw new IllegalArgumentException(
                    "Invalid or expired PIN reset token."
            );
        }


        /*
         * Validate token expiration.
         */
        if (tokenInfo.expiresAt()
                .isBefore(LocalDateTime.now())) {

            pinResetTokenStore.remove(token);

            throw new IllegalStateException(
                    "PIN reset token has expired. "
                            + "Please request a new PIN reset."
            );
        }


        /*
         * Ensure token belongs to logged-in user.
         */
        if (!tokenInfo.userId()
                .equals(userDto.getId())) {

            throw new SecurityException(
                    "Unauthorized attempt to reset PIN using "
                            + "a token issued to another user."
            );
        }


        /*
         * Update transaction PIN.
         */
        User currentUser =
                userRepository.findById(
                                userDto.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "User not found with ID: "
                                                + userDto.getId()
                                )
                        );


        currentUser.setTransactionPin(
                passwordEncoder.encode(
                        request.getNewPin()
                )
        );


        userRepository.save(
                currentUser
        );


        /*
         * Invalidate token immediately.
         */
        pinResetTokenStore.remove(token);


        auditLogService.logEvent(
                currentUser.getId(),
                "TRANSACTION_PIN_RESET_SUCCESS",
                "Transaction PIN updated successfully "
                        + "using reset token.",
                httpServletRequest
        );
    }
}
