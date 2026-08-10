package com.project.digitalwallet.service.impl;

import com.project.digitalwallet.common.enums.NotificationType;
import com.project.digitalwallet.common.util.WalletTransactionEvent;
import com.project.digitalwallet.dto.*;
import com.project.digitalwallet.entity.User;
import com.project.digitalwallet.mapper.UserMapper;
import com.project.digitalwallet.repository.OtpRepository;
import com.project.digitalwallet.repository.UserRepository;
import com.project.digitalwallet.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
    private final OtpRepository otpRepository;
    private final AuditLogService auditLogService;
    private final HttpServletRequest httpServletRequest;
    //private final TransactionService transactionService;
    private final ApplicationEventPublisher eventPublisher;


    private final Map<String, PinResetTokenInfo> pinResetTokenStore = new ConcurrentHashMap<>();

    private record PinResetTokenInfo(Long userId, String email, LocalDateTime expiresAt) {}

    @Override
    public void initiateRegistration(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered.");
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number is already registered.");
        }

        otpService.sendOtp(request.getEmail());
    }

    @Override
    @Transactional
    public UserDto completeRegistration(RegisterVerifyRequest verifyRequest) {
        RegisterRequest registerRequest = verifyRequest.getRegisterRequest();
        String email = registerRequest.getEmail();

        // 1. Verify OTP tied to the email
        otpService.verifyOtp(email, verifyRequest.getOtp());

        // 2. Double-check duplicate email/phone before saving
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered.");
        }
        if (userRepository.existsByPhoneNumber(registerRequest.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number is already registered.");
        }

        // 3. Create and populate User entity
        User user = new User();
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setEmail(email);
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

        User savedUser = userRepository.save(user);

        // 4. Convert User to UserDto and create associated Wallet
        UserDto userDto = UserMapper.toUserDto(savedUser);
        WalletDto createdWalletDto = walletService.createWallet(userDto);
        userDto.setWallet(createdWalletDto);

        // 5. Clean up used OTP records for this email
        otpRepository.deleteByEmail(email);

        // 6. Audit Log successful registration
        auditLogService.logEvent(
                savedUser.getId(),
                "USER_REGISTRATION_SUCCESS",
                String.format("User registered successfully with email: %s", email),
                httpServletRequest
        );

        return userDto;
    }

    @Override
    public List<UserDto> getAllUsers() {
        return UserMapper.toUserDto(userRepository.findAll());
    }

    @Override
    @Transactional
    public void setTransactionPin(String phoneNumber, String pin) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );


        user.setTransactionPin(
                passwordEncoder.encode(pin)
        );
        eventPublisher.publishEvent(new WalletTransactionEvent(
                user.getId(),
                user.getPhoneNumber(),
                NotificationType.SECURITY_ALERT, // or NotificationType.PIN_CHANGE / NotificationType.SYSTEM_ALERT
                BigDecimal.ZERO,
                "NPR",
                "SEC-" + System.currentTimeMillis(), // Generated security reference number
                "Your transaction PIN has been successfully set."
        ));

        userRepository.save(user);
    }

    @Override
    public void initiatePinReset(UserDto userDto) {
        otpService.sendOtp(userDto.getEmail());

        auditLogService.logEvent(
                userDto.getId(),
                "FORGOT_PIN_REQUESTED",
                "Transaction PIN reset OTP sent to email: " + userDto.getEmail(),
                httpServletRequest
        );
    }


    @Override
    @Transactional
    public VerifyPinOtpResponse verifyPinOtp(UserDto userDto, VerifyPinOtpRequest request) {
        // 1. Verify OTP against user's email
        otpService.verifyOtp(userDto.getEmail(), request.getOtp());

        // 2. Generate short-lived reset token (valid for 10 minutes)
        String resetToken = UUID.randomUUID().toString();
        pinResetTokenStore.put(resetToken, new PinResetTokenInfo(userDto.getId(), userDto.getEmail(), LocalDateTime.now().plusMinutes(10)));

        auditLogService.logEvent(
                userDto.getId(),
                "PIN_RESET_OTP_VERIFIED",
                "Transaction PIN reset OTP verified successfully. Generated reset token.",
                httpServletRequest
        );

        return new VerifyPinOtpResponse(resetToken);
    }


    @Override
    @Transactional
    public void resetPinWithToken(UserDto userDto, ResetPinWithTokenRequest request) {
        String token = request.getResetToken();
        PinResetTokenInfo tokenInfo = pinResetTokenStore.get(token);

        // 1. Validate Token presence
        if (tokenInfo == null) {
            throw new IllegalArgumentException("Invalid or expired PIN reset token.");
        }

        // 2. Validate Token expiration
        if (tokenInfo.expiresAt().isBefore(LocalDateTime.now())) {
            pinResetTokenStore.remove(token);
            throw new IllegalStateException("PIN reset token has expired. Please request a new PIN reset.");
        }

        // 3. Ensure token belongs to the logged-in user
        if (!tokenInfo.userId().equals(userDto.getId())) {
            throw new SecurityException("Unauthorized attempt to reset PIN using a token issued to another user.");
        }

        // 4. Update Transaction PIN
        User currentUser = userRepository.findById(userDto.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userDto.getId()));

        currentUser.setTransactionPin(passwordEncoder.encode(request.getNewPin()));
        userRepository.save(currentUser);

        // 5. Invalidate token immediately
        pinResetTokenStore.remove(token);

        auditLogService.logEvent(
                currentUser.getId(),
                "TRANSACTION_PIN_RESET_SUCCESS",
                "Transaction PIN updated successfully using reset token.",
                httpServletRequest
        );
    }
}