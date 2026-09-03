package com.project.digitalwallet.service.impl;

import com.project.digitalwallet.common.util.OtpGenerator;
import com.project.digitalwallet.dto.ForgotPasswordPendingDataDto;
import com.project.digitalwallet.dto.PinResetPendingDataDto;
import com.project.digitalwallet.dto.RegisterPendingDataDto;
import com.project.digitalwallet.service.EmailService;
import com.project.digitalwallet.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final StringRedisTemplate redisTemplate;

    private final PasswordEncoder passwordEncoder;

    private final EmailService emailService;

    private final ObjectMapper objectMapper;


    // =========================================================
    // CONSTANTS
    // =========================================================

    private static final int MAX_ATTEMPTS = 3;

    private static final Duration OTP_EXPIRY =
            Duration.ofMinutes(5);

    private static final Duration PENDING_EXPIRY =
            Duration.ofMinutes(10);

    private static final Duration RESEND_COOLDOWN =
            Duration.ofSeconds(60);


    // =========================================================
    // REGISTRATION KEYS
    // =========================================================

    private static final String REGISTRATION_OTP_PREFIX =
            "otp:registration:";

    private static final String PENDING_REG_PREFIX =
            "pending_reg:";

    private static final String REGISTRATION_RESEND_PREFIX =
            "rate_limit:registration:";


    // =========================================================
    // PIN RESET KEYS
    // =========================================================

    private static final String PIN_RESET_OTP_PREFIX =
            "otp:pin-reset:";

    private static final String PIN_RESET_PENDING_PREFIX =
            "pending_pin_reset:";

    private static final String PIN_RESET_RESEND_PREFIX =
            "rate_limit:pin-reset:";

    //FORGOT PASSWORD
    private static final String FORGOT_PASSWORD_OTP_PREFIX =
            "otp:forgot-password:";

    private static final String FORGOT_PASSWORD_PENDING_PREFIX =
            "pending_forgot_password:";

    private static final String FORGOT_PASSWORD_RESEND_PREFIX =
            "rate_limit:forgot-password:";

    // =========================================================
    // REGISTRATION
    // =========================================================

    @Override
    public void sendRegistrationOtp(String email) {

        email = normalizeEmail(email);

        String otpKey =
                REGISTRATION_OTP_PREFIX + email;

        String pendingKey =
                PENDING_REG_PREFIX + email;

        String resendKey =
                REGISTRATION_RESEND_PREFIX + email;


        /*
         * Make sure registration session exists.
         */
        String pendingJson =
                redisTemplate.opsForValue()
                        .get(pendingKey);

        if (pendingJson == null) {

            throw new IllegalArgumentException(
                    "Registration session has expired. Please register again."
            );
        }


        /*
         * Check 60 second resend cooldown.
         */
        if (Boolean.TRUE.equals(
                redisTemplate.hasKey(resendKey)
        )) {

            throw new IllegalStateException(
                    "Please wait 60 seconds before requesting another OTP."
            );
        }


        /*
         * Generate OTP.
         */
        String rawOtp =
                OtpGenerator.generateOtp();


        /*
         * Hash OTP before storing.
         */
        String hashedOtp =
                passwordEncoder.encode(rawOtp);


        /*
         * Store OTP.
         */
        redisTemplate.opsForValue().set(
                otpKey,
                hashedOtp,
                OTP_EXPIRY
        );


        /*
         * Store resend cooldown.
         */
        redisTemplate.opsForValue().set(
                resendKey,
                "LOCKED",
                RESEND_COOLDOWN
        );


        /*
         * Send email.
         */
        emailService.sendOtpEmail(
                email,
                rawOtp
        );
    }
    @Override
    public void resendRegistrationOtp(String email) {
        sendRegistrationOtp(email);
    }



    // =========================================================
    // VERIFY REGISTRATION OTP
    // =========================================================

    @Override
    public boolean verifyRegistrationOtp(
            String email,
            String code
    ) {

        email = normalizeEmail(email);

        String otpKey =
                REGISTRATION_OTP_PREFIX + email;

        String pendingKey =
                PENDING_REG_PREFIX + email;


        String hashedOtp =
                redisTemplate.opsForValue()
                        .get(otpKey);

        String pendingJson =
                redisTemplate.opsForValue()
                        .get(pendingKey);


        if (hashedOtp == null || pendingJson == null) {

            throw new IllegalArgumentException(
                    "OTP has expired or registration session is invalid."
            );
        }


        try {

            RegisterPendingDataDto pendingData =
                    objectMapper.readValue(
                            pendingJson,
                            RegisterPendingDataDto.class
                    );


            int attempts =
                    pendingData.getAttempts();


            /*
             * Maximum attempts reached.
             */
            if (attempts >= MAX_ATTEMPTS) {

                redisTemplate.delete(otpKey);
                redisTemplate.delete(pendingKey);

                throw new IllegalStateException(
                        "Too many failed attempts. Please register again."
                );
            }


            /*
             * Verify OTP.
             */
            if (!passwordEncoder.matches(
                    code,
                    hashedOtp
            )) {

                attempts++;

                pendingData.setAttempts(
                        attempts
                );


                /*
                 * Third failed attempt.
                 */
                if (attempts >= MAX_ATTEMPTS) {

                    redisTemplate.delete(otpKey);
                    redisTemplate.delete(pendingKey);

                    throw new IllegalStateException(
                            "Too many failed attempts. Please register again."
                    );
                }


                /*
                 * Preserve remaining pending-registration TTL.
                 */
                Long ttl =
                        redisTemplate.getExpire(
                                pendingKey
                        );


                if (ttl != null && ttl > 0) {

                    redisTemplate.opsForValue().set(
                            pendingKey,
                            objectMapper.writeValueAsString(
                                    pendingData
                            ),
                            Duration.ofSeconds(ttl)
                    );

                } else {

                    redisTemplate.opsForValue().set(
                            pendingKey,
                            objectMapper.writeValueAsString(
                                    pendingData
                            ),
                            PENDING_EXPIRY
                    );
                }


                throw new IllegalArgumentException(
                        "Invalid OTP code. Remaining attempts: "
                                + (MAX_ATTEMPTS - attempts)
                );
            }


            /*
             * OTP verified successfully.
             *
             * Delete OTP so it cannot be reused.
             *
             * Keep pending registration until
             * UserService saves the user.
             */
            redisTemplate.delete(otpKey);

            return true;

        } catch (IllegalArgumentException e) {

            throw e;

        } catch (IllegalStateException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to verify registration OTP.",
                    e
            );
        }
    }


    // =========================================================
    // PIN RESET
    // =========================================================

    @Override
    public void sendPinResetOtp(
            Long userId,
            String email
    ) {

        email = normalizeEmail(email);

        String otpKey =
                PIN_RESET_OTP_PREFIX + email;

        String pendingKey =
                PIN_RESET_PENDING_PREFIX + email;

        String resendKey =
                PIN_RESET_RESEND_PREFIX + email;


        /*
         * Check resend cooldown.
         */
        if (Boolean.TRUE.equals(
                redisTemplate.hasKey(resendKey)
        )) {

            throw new IllegalStateException(
                    "Please wait 60 seconds before requesting another OTP."
            );
        }


        /*
         * Check whether a PIN reset session
         * already exists.
         */
        String pendingJson =
                redisTemplate.opsForValue()
                        .get(pendingKey);


        if (pendingJson == null) {

            /*
             * First PIN reset request.
             *
             * Start attempts at zero.
             */
            PinResetPendingDataDto pendingData =
                    PinResetPendingDataDto.builder()
                            .userId(userId)
                            .email(email)
                            .attempts(0)
                            .build();


            try {

                pendingJson =
                        objectMapper.writeValueAsString(
                                pendingData
                        );

            } catch (Exception e) {

                throw new RuntimeException(
                        "Unable to create PIN reset session.",
                        e
                );
            }


            /*
             * Store pending PIN reset session.
             */
            redisTemplate.opsForValue().set(
                    pendingKey,
                    pendingJson,
                    PENDING_EXPIRY
            );
        }


        /*
         * Generate new OTP.
         */
        String rawOtp =
                OtpGenerator.generateOtp();


        /*
         * Hash OTP.
         */
        String hashedOtp =
                passwordEncoder.encode(rawOtp);


        /*
         * Store OTP.
         */
        redisTemplate.opsForValue().set(
                otpKey,
                hashedOtp,
                OTP_EXPIRY
        );


        /*
         * Store resend cooldown.
         */
        redisTemplate.opsForValue().set(
                resendKey,
                "LOCKED",
                RESEND_COOLDOWN
        );


        /*
         * Send OTP email.
         */
        emailService.sendOtpEmail(
                email,
                rawOtp
        );
    }


    // =========================================================
    // VERIFY PIN RESET OTP
    // =========================================================

    @Override
    public boolean verifyPinResetOtp(
            String email,
            String code
    ) {

        email = normalizeEmail(email);

        String otpKey =
                PIN_RESET_OTP_PREFIX + email;

        String pendingKey =
                PIN_RESET_PENDING_PREFIX + email;


        /*
         * Get OTP.
         */
        String hashedOtp =
                redisTemplate.opsForValue()
                        .get(otpKey);


        /*
         * Get pending PIN reset session.
         */
        String pendingJson =
                redisTemplate.opsForValue()
                        .get(pendingKey);


        if (hashedOtp == null || pendingJson == null) {

            throw new IllegalArgumentException(
                    "OTP has expired or PIN reset session is invalid."
            );
        }


        try {

            PinResetPendingDataDto pendingData =
                    objectMapper.readValue(
                            pendingJson,
                            PinResetPendingDataDto.class
                    );


            int attempts =
                    pendingData.getAttempts();


            /*
             * Check maximum attempts.
             */
            if (attempts >= MAX_ATTEMPTS) {

                redisTemplate.delete(otpKey);
                redisTemplate.delete(pendingKey);

                throw new IllegalStateException(
                        "Too many failed attempts. Please request a new PIN reset."
                );
            }


            /*
             * Verify OTP.
             */
            if (!passwordEncoder.matches(
                    code,
                    hashedOtp
            )) {

                attempts++;

                pendingData.setAttempts(
                        attempts
                );


                /*
                 * Third failed attempt.
                 */
                if (attempts >= MAX_ATTEMPTS) {

                    redisTemplate.delete(otpKey);
                    redisTemplate.delete(pendingKey);

                    throw new IllegalStateException(
                            "Too many failed attempts. Please request a new PIN reset."
                    );
                }


                /*
                 * Preserve remaining pending-session TTL.
                 */
                Long ttl =
                        redisTemplate.getExpire(
                                pendingKey
                        );


                if (ttl != null && ttl > 0) {

                    redisTemplate.opsForValue().set(
                            pendingKey,
                            objectMapper.writeValueAsString(
                                    pendingData
                            ),
                            Duration.ofSeconds(ttl)
                    );

                } else {

                    redisTemplate.opsForValue().set(
                            pendingKey,
                            objectMapper.writeValueAsString(
                                    pendingData
                            ),
                            PENDING_EXPIRY
                    );
                }


                throw new IllegalArgumentException(
                        "Invalid OTP code. Remaining attempts: "
                                + (MAX_ATTEMPTS - attempts)
                );
            }


            /*
             * OTP successfully verified.
             *
             * Delete OTP.
             *
             * Delete pending session too because
             * OTP verification is now complete.
             */
            redisTemplate.delete(otpKey);
            redisTemplate.delete(pendingKey);

            return true;

        } catch (IllegalArgumentException e) {

            throw e;

        } catch (IllegalStateException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to verify PIN reset OTP.",
                    e
            );
        }
    }


    // =========================================================
    // RESEND PIN RESET OTP
    // =========================================================

    @Override
    public void resendPinResetOtp(
            String email
    ) {

        email = normalizeEmail(email);

        String pendingKey =
                PIN_RESET_PENDING_PREFIX + email;


        /*
         * A PIN reset session must already exist.
         */
        String pendingJson =
                redisTemplate.opsForValue()
                        .get(pendingKey);


        if (pendingJson == null) {

            throw new IllegalArgumentException(
                    "No active PIN reset session. Please request a new PIN reset."
            );
        }


        try {

            PinResetPendingDataDto pendingData =
                    objectMapper.readValue(
                            pendingJson,
                            PinResetPendingDataDto.class
                    );


            /*
             * Do not allow resend if all attempts
             * have already been consumed.
             */
            if (pendingData.getAttempts()
                    >= MAX_ATTEMPTS) {

                redisTemplate.delete(pendingKey);

                redisTemplate.delete(
                        PIN_RESET_OTP_PREFIX + email
                );

                throw new IllegalStateException(
                        "Too many failed attempts. Please request a new PIN reset."
                );
            }


            /*
             * IMPORTANT:
             *
             * We call sendPinResetOtp(), but because the
             * pending session already exists, it DOES NOT
             * reset attempts to zero.
             */
            sendPinResetOtp(
                    pendingData.getUserId(),
                    email
            );

        } catch (IllegalArgumentException e) {

            throw e;

        } catch (IllegalStateException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to resend PIN reset OTP.",
                    e
            );
        }
    }


    // =========================================================
    // EMAIL NORMALIZATION
    // =========================================================

    private String normalizeEmail(
            String email
    ) {

        return email
                .trim()
                .toLowerCase();
    }
    @Override
    public void sendForgotPasswordOtp(
            Long userId,
            String email
    ) {

        email = normalizeEmail(email);

        String otpKey =
                FORGOT_PASSWORD_OTP_PREFIX + email;

        String pendingKey =
                FORGOT_PASSWORD_PENDING_PREFIX + email;

        String resendKey =
                FORGOT_PASSWORD_RESEND_PREFIX + email;


        /*
         * Check resend cooldown.
         */
        if (Boolean.TRUE.equals(
                redisTemplate.hasKey(resendKey)
        )) {

            throw new IllegalStateException(
                    "Please wait 60 seconds before requesting another OTP."
            );
        }


        /*
         * Check whether an existing password-reset
         * session exists.
         */
        String pendingJson =
                redisTemplate.opsForValue()
                        .get(pendingKey);


        /*
         * First request.
         *
         * Create attempts = 0.
         */
        if (pendingJson == null) {

            ForgotPasswordPendingDataDto pendingData =
                    ForgotPasswordPendingDataDto.builder()
                            .userId(userId)
                            .email(email)
                            .attempts(0)
                            .build();

            try {

                pendingJson =
                        objectMapper.writeValueAsString(
                                pendingData
                        );

            } catch (Exception e) {

                throw new RuntimeException(
                        "Unable to create password reset session.",
                        e
                );
            }


            redisTemplate.opsForValue().set(
                    pendingKey,
                    pendingJson,
                    PENDING_EXPIRY
            );
        }


        /*
         * Generate OTP.
         */
        String rawOtp =
                OtpGenerator.generateOtp();


        /*
         * Hash OTP before storing.
         */
        String hashedOtp =
                passwordEncoder.encode(rawOtp);


        /*
         * Store OTP for 5 minutes.
         */
        redisTemplate.opsForValue().set(
                otpKey,
                hashedOtp,
                OTP_EXPIRY
        );


        /*
         * 60-second resend cooldown.
         */
        redisTemplate.opsForValue().set(
                resendKey,
                "LOCKED",
                RESEND_COOLDOWN
        );


        /*
         * Send OTP email.
         */
        emailService.sendOtpEmail(
                email,
                rawOtp
        );
    }
    @Override
    public boolean verifyForgotPasswordOtp(
            String email,
            String code
    ) {

        email = normalizeEmail(email);

        String otpKey =
                FORGOT_PASSWORD_OTP_PREFIX + email;

        String pendingKey =
                FORGOT_PASSWORD_PENDING_PREFIX + email;


        String hashedOtp =
                redisTemplate.opsForValue()
                        .get(otpKey);

        String pendingJson =
                redisTemplate.opsForValue()
                        .get(pendingKey);


        if (hashedOtp == null || pendingJson == null) {

            throw new IllegalArgumentException(
                    "OTP has expired or password reset session is invalid."
            );
        }


        try {

            ForgotPasswordPendingDataDto pendingData =
                    objectMapper.readValue(
                            pendingJson,
                            ForgotPasswordPendingDataDto.class
                    );


            int attempts =
                    pendingData.getAttempts();


            /*
             * Check maximum attempts.
             */
            if (attempts >= MAX_ATTEMPTS) {

                redisTemplate.delete(otpKey);
                redisTemplate.delete(pendingKey);

                throw new IllegalStateException(
                        "Too many failed attempts. Please request a new password reset."
                );
            }


            /*
             * Verify OTP.
             */
            if (!passwordEncoder.matches(
                    code,
                    hashedOtp
            )) {

                attempts++;

                pendingData.setAttempts(
                        attempts
                );


                /*
                 * Third failed attempt.
                 */
                if (attempts >= MAX_ATTEMPTS) {

                    redisTemplate.delete(otpKey);
                    redisTemplate.delete(pendingKey);

                    throw new IllegalStateException(
                            "Too many failed attempts. Please request a new password reset."
                    );
                }


                /*
                 * Preserve existing pending-session TTL.
                 */
                Long ttl =
                        redisTemplate.getExpire(
                                pendingKey
                        );


                if (ttl != null && ttl > 0) {

                    redisTemplate.opsForValue().set(
                            pendingKey,
                            objectMapper.writeValueAsString(
                                    pendingData
                            ),
                            Duration.ofSeconds(ttl)
                    );

                } else {

                    redisTemplate.opsForValue().set(
                            pendingKey,
                            objectMapper.writeValueAsString(
                                    pendingData
                            ),
                            PENDING_EXPIRY
                    );
                }


                throw new IllegalArgumentException(
                        "Invalid OTP code. Remaining attempts: "
                                + (MAX_ATTEMPTS - attempts)
                );
            }


            /*
             * OTP successfully verified.
             */
            redisTemplate.delete(otpKey);
            redisTemplate.delete(pendingKey);

            return true;

        } catch (IllegalArgumentException e) {

            throw e;

        } catch (IllegalStateException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to verify password reset OTP.",
                    e
            );
        }
    }
    @Override
    public void resendForgotPasswordOtp(
            String email
    ) {

        email = normalizeEmail(email);

        String pendingKey =
                FORGOT_PASSWORD_PENDING_PREFIX + email;


        String pendingJson =
                redisTemplate.opsForValue()
                        .get(pendingKey);


        if (pendingJson == null) {

            throw new IllegalArgumentException(
                    "No active password reset session. Please request a new password reset."
            );
        }


        try {

            ForgotPasswordPendingDataDto pendingData =
                    objectMapper.readValue(
                            pendingJson,
                            ForgotPasswordPendingDataDto.class
                    );


            /*
             * Do not allow resend after 3 failed attempts.
             */
            if (pendingData.getAttempts()
                    >= MAX_ATTEMPTS) {

                redisTemplate.delete(pendingKey);

                redisTemplate.delete(
                        FORGOT_PASSWORD_OTP_PREFIX + email
                );

                throw new IllegalStateException(
                        "Too many failed attempts. Please request a new password reset."
                );
            }


            /*
             * sendForgotPasswordOtp() detects that the
             * pending session already exists, therefore
             * attempts will NOT be reset.
             */
            sendForgotPasswordOtp(
                    pendingData.getUserId(),
                    email
            );

        } catch (IllegalArgumentException e) {

            throw e;

        } catch (IllegalStateException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to resend password reset OTP.",
                    e
            );
        }
    }



}
