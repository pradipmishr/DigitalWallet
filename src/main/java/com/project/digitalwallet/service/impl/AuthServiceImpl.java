package com.project.digitalwallet.service.impl;

import com.project.digitalwallet.common.enums.NotificationType;
import com.project.digitalwallet.common.util.WalletTransactionEvent;
import com.project.digitalwallet.dto.*;
import com.project.digitalwallet.entity.User;
import com.project.digitalwallet.mapper.UserMapper;
import com.project.digitalwallet.repository.UserRepository;
import com.project.digitalwallet.security.JwtUtil;
import com.project.digitalwallet.security.UserPrincipal;
import com.project.digitalwallet.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

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

    private final StringRedisTemplate redisTemplate;

    private final TokenBlacklistService tokenBlacklistService;

    private final ObjectMapper objectMapper;
    private final WalletService walletService;


    /*
     * =========================================================
     * PASSWORD RESET TOKEN
     * =========================================================
     *
     * Redis key:
     *
     * reset-password:token:<token>
     *
     * Redis value:
     *
     * user email
     *
     * Expiry:
     *
     * 10 minutes
     */
    private static final String RESET_TOKEN_PREFIX =
            "reset-password:token:";

    private static final Duration RESET_TOKEN_EXPIRY =
            Duration.ofMinutes(10);

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



    @Transactional
    @Override
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
    // LOGIN
    // =========================================================

    @Override
    public LoginResponse login(LoginRequest request) {

        String phoneNumber =
                request.getPhoneNumber();


        /*
         * Check whether the account is currently blocked.
         */
        if (loginAttemptService.isBlocked(phoneNumber)) {

            long remainingSeconds =
                    loginAttemptService
                            .getRemainingLockoutTimeSeconds(
                                    phoneNumber
                            );

            long remainingMinutes =
                    (long) Math.ceil(
                            remainingSeconds / 60.0
                    );

            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed login attempts. Account locked for "
                            + remainingMinutes
                            + " minute(s)."
            );
        }


        try {

            /*
             * Authenticate user.
             */
            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    phoneNumber,
                                    request.getPassword()
                            )
                    );


            /*
             * Login successful.
             *
             * Clear failed login attempts.
             */
            loginAttemptService.loginSucceeded(
                    phoneNumber
            );


            UserPrincipal principal =
                    (UserPrincipal)
                            authentication.getPrincipal();


            User user =
                    principal.getUser();


            String token =
                    jwtUtil.generateToken(
                            principal
                    );


            return UserMapper.toLoginResponse(
                    user,
                    token
            );

        } catch (BadCredentialsException ex) {

            /*
             * Login failed.
             *
             * Increment failed attempts in Redis.
             */
            loginAttemptService.loginFailed(
                    phoneNumber
            );

            throw ex;
        }
    }


    // =========================================================
    // FORGOT PASSWORD - SEND OTP
    // =========================================================

    @Override
    public void initiateForgotPassword(
            ForgotPasswordRequest request
    ) {

        String email =
                request.getEmail()
                        .trim()
                        .toLowerCase();


        /*
         * Find user.
         */
        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "User not found with email: "
                                                + email
                                )
                        );


        /*
         * Generate and send forgot-password OTP.
         *
         * OtpService is responsible for:
         *
         * - generating OTP
         * - hashing OTP
         * - storing OTP in Redis
         * - storing attempts in Redis
         * - 3-attempt limit
         * - 60-second resend cooldown
         * - OTP expiration
         */
        otpService.sendForgotPasswordOtp(
                user.getId(),
                email
        );


        auditLogService.logEvent(
                user.getId(),
                "FORGOT_PASSWORD_REQUESTED",
                "Password reset OTP sent to email: "
                        + email,
                httpServletRequest
        );
    }


    // =========================================================
    // FORGOT PASSWORD - RESEND OTP
    // =========================================================

    /*
     * This method is optional in AuthService if your controller
     * directly calls otpService.resendForgotPasswordOtp().
     *
     * You can keep the resend endpoint directly on OtpService.
     */


    // =========================================================
    // FORGOT PASSWORD - VERIFY OTP
    // =========================================================

    @Override
    @Transactional
    public VerifyResetOtpResponse verifyResetOtp(
            VerifyResetOtpRequest request
    ) {

        String email =
                request.getEmail()
                        .trim()
                        .toLowerCase();


        /*
         * Verify OTP.
         *
         * OtpService handles:
         *
         * - OTP existence
         * - OTP expiration
         * - OTP matching
         * - maximum 3 attempts
         * - deleting OTP after successful verification
         */
        otpService.verifyForgotPasswordOtp(
                email,
                request.getOtp()
        );


        /*
         * Find user.
         */
        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "User not found"
                                )
                        );


        /*
         * Generate a random reset token.
         */
        String resetToken =
                UUID.randomUUID().toString();


        /*
         * Store reset token in Redis.
         *
         * Key:
         *
         * reset-password:token:<token>
         *
         * Value:
         *
         * email
         *
         * Expiry:
         *
         * 10 minutes
         */
        redisTemplate.opsForValue().set(
                RESET_TOKEN_PREFIX + resetToken,
                email,
                RESET_TOKEN_EXPIRY
        );


        auditLogService.logEvent(
                user.getId(),
                "RESET_OTP_VERIFIED",
                "Password reset OTP verified successfully. "
                        + "Generated reset token.",
                httpServletRequest
        );


        /*
         * Return reset token to client.
         */
        return new VerifyResetOtpResponse(
                resetToken
        );
    }


    // =========================================================
    // RESET PASSWORD USING TOKEN
    // =========================================================

    @Override
    @Transactional
    public void resetPasswordWithToken(
            ResetPasswordWithTokenRequest request
    ) {

        String token =
                request.getResetToken();


        String tokenKey =
                RESET_TOKEN_PREFIX + token;


        /*
         * Get email associated with reset token.
         *
         * Redis automatically returns null if the token
         * does not exist or has expired.
         */
        String email =
                redisTemplate.opsForValue()
                        .get(tokenKey);


        /*
         * Token doesn't exist or has expired.
         */
        if (email == null) {

            throw new IllegalArgumentException(
                    "Invalid or expired reset token."
            );
        }


        /*
         * Find user.
         */
        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "User not found"
                                )
                        );


        /*
         * Update password.
         */
        user.setPassword(
                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );


        userRepository.save(user);


        /*
         * Invalidate reset token immediately.
         *
         * This prevents the same token from being used
         * again.
         */
        redisTemplate.delete(
                tokenKey
        );


        /*
         * Audit successful password reset.
         */
        auditLogService.logEvent(
                user.getId(),
                "PASSWORD_RESET_SUCCESS",
                "Password updated successfully using reset token.",
                httpServletRequest
        );


        /*
         * Send security notification.
         */
        eventPublisher.publishEvent(
                new WalletTransactionEvent(
                        user.getId(),
                        user.getPhoneNumber(),
                        NotificationType.SECURITY_ALERT,
                        BigDecimal.ZERO,
                        "NPR",
                        "PWD-" + System.currentTimeMillis(),
                        "Your account password has been reset successfully. "
                                + "If you did not perform this action, contact support immediately."
                )
        );
    }


    // =========================================================
    // LOGOUT
    // =========================================================

    @Override
    public void logout(HttpServletRequest request) {

        String authHeader =
                request.getHeader("Authorization");

        if (authHeader == null ||
                !authHeader.startsWith("Bearer ")) {

            throw new IllegalArgumentException(
                    "Authorization token is missing"
            );
        }

        String jwt = authHeader.substring(7);

        String jti = jwtUtil.extractJti(jwt);

        Date expiration = jwtUtil.extractExpiration(jwt);

        long remainingMillis =
                expiration.getTime()
                        - System.currentTimeMillis();

        if (remainingMillis > 0) {

            tokenBlacklistService.blacklist(
                    jti,
                    Duration.ofMillis(remainingMillis)
            );
        }
    }

}
