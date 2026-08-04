package com.project.digitalwallet.service.impl;

import com.project.digitalwallet.dto.RegisterRequest;
import com.project.digitalwallet.dto.RegisterVerifyRequest;
import com.project.digitalwallet.dto.UserDto;
import com.project.digitalwallet.dto.WalletDto;
import com.project.digitalwallet.entity.User;
import com.project.digitalwallet.mapper.UserMapper;
import com.project.digitalwallet.repository.UserRepository;
import com.project.digitalwallet.service.OtpService;
import com.project.digitalwallet.service.UserService;
import com.project.digitalwallet.service.WalletService;
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

    @Override
    public void initiateRegistration(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered.");
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number is already registered.");
        }

        otpService.sendOtp(request.getPhoneNumber());
    }

    @Transactional
    @Override
    public UserDto completeRegistration(RegisterVerifyRequest verifyRequest) {

        RegisterRequest request = verifyRequest.getRegisterRequest();

        // 1. Verify OTP
        boolean isVerified = otpService.verifyOtp(
                request.getPhoneNumber(),
                verifyRequest.getOtp()
        );

        if (!isVerified) {
            throw new IllegalArgumentException("Invalid or expired OTP.");
        }

        // 2. Build User
        User user = new User();

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());

        // Encrypt password before saving
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        // 3. Save User
        User savedUser = userRepository.save(user);

        // 4. Create Wallet
        UserDto savedUserDto = UserMapper.toUserDto(savedUser);

        WalletDto createdWalletDto =
                walletService.createWallet(savedUserDto);

        savedUserDto.setWallet(createdWalletDto);

        return savedUserDto;
    }

    @Override
    public List<UserDto> getAllUsers() {
        return UserMapper.toUserDto(userRepository.findAll());
    }
}