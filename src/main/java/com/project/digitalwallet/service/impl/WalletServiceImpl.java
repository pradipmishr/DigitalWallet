package com.project.digitalwallet.service.impl;

import com.project.digitalwallet.dto.UserDto;
import com.project.digitalwallet.dto.WalletDto;
import com.project.digitalwallet.entity.User;
import com.project.digitalwallet.entity.Wallet;
import com.project.digitalwallet.common.enums.WalletStatus;
import com.project.digitalwallet.mapper.WalletMapper;
import com.project.digitalwallet.repository.UserRepository;
import com.project.digitalwallet.repository.WalletRepository;
import com.project.digitalwallet.service.WalletService;
import com.project.digitalwallet.common.util.WalletNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final WalletNumberGenerator walletNumberGenerator;

    @Override
    public WalletDto createWallet(UserDto userDto) {
        User user = userRepository.findById(userDto.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userDto.getId()));

        Wallet wallet = new Wallet();
        wallet.setWalletNumber(walletNumberGenerator.generate());
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setStatus(WalletStatus.ACTIVE);
        wallet.setUser(user);

        Wallet savedWallet = walletRepository.save(wallet);
        return WalletMapper.toWalletDto(savedWallet);
    }
}