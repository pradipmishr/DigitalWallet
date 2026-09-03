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

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;


    /*
     * Redis key used for pending registration.
     */
    private static final String PENDING_REG_PREFIX =
            "pending_reg:";


    /*
     * Pending registration expires after 10 minutes.
     */
    private static final Duration PENDING_REG_EXPIRY =
            Duration.ofMinutes(10);


    /*
     * Maximum failed OTP attempts.
     */
    private static final int MAX_OTP_ATTEMPTS = 3;


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
    // REGISTRATION
    // =========================================================

    @Override
    public void initiateRegistration(RegisterRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase();

        // 1. Check existing email
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "Email is already registered."
            );
        }

        // 2. Check existing phone
        if (userRepository.existsByPhoneNumber(
                request.getPhoneNumber()
        )) {
            throw new IllegalArgumentException(
                    "Phone number is already registered."
            );
        }

        // 3. Hash password
        String hashedPassword =
                passwordEncoder.encode(
                        request.getPassword()
                );

        // 4. Create pending registration data
        RegisterPendingDataDto pendingData =
                RegisterPendingDataDto.builder()
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .email(email)
                        .dateOfBirth(request.getDateOfBirth())
                        .phoneNumber(request.getPhoneNumber())
                        .password(hashedPassword)
                        .attempts(0)
                        .build();

        try {

            // 5. Convert to JSON
            String pendingJson =
                    objectMapper.writeValueAsString(
                            pendingData
                    );

            // 6. Store pending registration
            redisTemplate.opsForValue().set(
                    PENDING_REG_PREFIX + email,
                    pendingJson,
                    Duration.ofMinutes(10)
            );

            // 7. Send registration OTP
            otpService.sendRegistrationOtp(email);

        } catch (Exception e) {

            redisTemplate.delete(
                    PENDING_REG_PREFIX + email
            );

            throw new RuntimeException(
                    "Unable to initiate registration.",
                    e
            );
        }
    }



    @Override
    @Transactional
    public UserDto completeRegistration(
            RegisterVerifyRequest verifyRequest
    ) {

        String email = verifyRequest.getEmail()
                .trim()
                .toLowerCase();

        String pendingKey =
                PENDING_REG_PREFIX + email;

        /*
         * 1. Get pending registration from Redis.
         */
        String pendingJson =
                redisTemplate.opsForValue().get(
                        pendingKey
                );

        if (pendingJson == null) {

            throw new IllegalArgumentException(
                    "Registration session has expired. Please register again."
            );
        }

        try {

            /*
             * 2. Convert JSON back to DTO.
             */
            RegisterPendingDataDto pendingData =
                    objectMapper.readValue(
                            pendingJson,
                            RegisterPendingDataDto.class
                    );


            /*
             * 3. Check attempts.
             *
             * The attempts are stored inside
             * RegisterPendingDataDto.
             */
            if (pendingData.getAttempts()
                    >= MAX_OTP_ATTEMPTS) {

                redisTemplate.delete(pendingKey);

                throw new IllegalStateException(
                        "Too many failed attempts. Please register again."
                );
            }


            /*
             * 4. Verify OTP.
             *
             * This will:
             * - check OTP exists
             * - check expiration
             * - check OTP
             * - enforce 3 attempts
             * - delete OTP after successful verification
             */
            otpService.verifyRegistrationOtp(
                    email,
                    verifyRequest.getOtp()
            );

            /*
             * 5. Double-check email before saving.
             */
            if (userRepository.existsByEmail(email)) {

                redisTemplate.delete(pendingKey);

                throw new IllegalArgumentException(
                        "Email is already registered."
                );
            }


            /*
             * 6. Double-check phone before saving.
             */
            if (userRepository.existsByPhoneNumber(
                    pendingData.getPhoneNumber()
            )) {

                redisTemplate.delete(pendingKey);

                throw new IllegalArgumentException(
                        "Phone number is already registered."
                );
            }


            /*
             * 7. Create User entity.
             */
            User user = new User();

            user.setFirstName(
                    pendingData.getFirstName()
            );

            user.setLastName(
                    pendingData.getLastName()
            );

            user.setEmail(
                    pendingData.getEmail()
            );

            user.setPhoneNumber(
                    pendingData.getPhoneNumber()
            );

            user.setDateOfBirth(
                    pendingData.getDateOfBirth()
            );

            /*
             * Password is ALREADY HASHED.
             *
             * Do NOT encode it again.
             */
            user.setPassword(
                    pendingData.getPassword()
            );


            /*
             * 8. Save user to database.
             */
            User savedUser =
                    userRepository.save(user);


            /*
             * 9. Delete pending registration.
             */
            redisTemplate.delete(
                    pendingKey
            );


            /*
             * 10. Convert User to DTO.
             */
            UserDto userDto =
                    UserMapper.toUserDto(savedUser);


            /*
             * 11. Create associated wallet.
             */
            WalletDto createdWalletDto =
                    walletService.createWallet(
                            userDto
                    );

            userDto.setWallet(
                    createdWalletDto
            );


            /*
             * 12. Audit successful registration.
             */
            auditLogService.logEvent(
                    savedUser.getId(),
                    "USER_REGISTRATION_SUCCESS",
                    String.format(
                            "User registered successfully with email: %s",
                            email
                    ),
                    httpServletRequest
            );


            return userDto;

        } catch (IllegalArgumentException e) {

            throw e;

        } catch (IllegalStateException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to complete registration.",
                    e
            );
        }
    }


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
