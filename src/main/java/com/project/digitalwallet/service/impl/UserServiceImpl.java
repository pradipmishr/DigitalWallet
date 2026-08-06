package com.project.digitalwallet.service.impl;

import com.project.digitalwallet.dto.RegisterRequest;
import com.project.digitalwallet.dto.RegisterVerifyRequest;
import com.project.digitalwallet.dto.UserDto;
import com.project.digitalwallet.dto.WalletDto;
import com.project.digitalwallet.entity.User;
import com.project.digitalwallet.mapper.UserMapper;
import com.project.digitalwallet.repository.OtpRepository;
import com.project.digitalwallet.repository.UserRepository;
import com.project.digitalwallet.service.AuditLogService;
import com.project.digitalwallet.service.OtpService;
import com.project.digitalwallet.service.UserService;
import com.project.digitalwallet.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

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

        userRepository.save(user);
    }
}