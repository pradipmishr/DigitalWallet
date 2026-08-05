package com.project.digitalwallet.service.impl;

import com.project.digitalwallet.common.enums.TransactionStatus;
import com.project.digitalwallet.common.enums.TransactionType;
import com.project.digitalwallet.dto.DepositRequest;
import com.project.digitalwallet.dto.TransactionDto;
import com.project.digitalwallet.dto.UserDto;
import com.project.digitalwallet.dto.WalletDto;
import com.project.digitalwallet.entity.Transaction;
import com.project.digitalwallet.entity.User;
import com.project.digitalwallet.entity.Wallet;
import com.project.digitalwallet.common.enums.WalletStatus;
import com.project.digitalwallet.mapper.TransactionMapper;
import com.project.digitalwallet.mapper.WalletMapper;
import com.project.digitalwallet.repository.TransactionRepository;
import com.project.digitalwallet.repository.UserRepository;
import com.project.digitalwallet.repository.WalletRepository;
import com.project.digitalwallet.service.WalletService;
import com.project.digitalwallet.common.util.WalletNumberGenerator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final WalletNumberGenerator walletNumberGenerator;
    private final TransactionRepository transactionRepository;

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
    @Transactional
    @Override
    public TransactionDto deposit(Long userId, DepositRequest request) {
        // 1. Automatically fetch wallet belonging to this user
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No wallet found for user ID: " + userId));

        // 2. Validate wallet status
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Cannot deposit funds into an inactive wallet.");
        }

        // 3. Update balance
        BigDecimal updatedBalance = wallet.getBalance().add(request.getAmount());
        wallet.setBalance(updatedBalance);
        walletRepository.save(wallet);

        // 4. Record transaction
        Transaction transaction = new Transaction();
        transaction.setSenderWallet(null);
        transaction.setReceiverWallet(wallet);
        transaction.setAmount(request.getAmount());
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setReferenceNumber("DEP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        transaction.setDescription(request.getDescription() != null ? request.getDescription() : "Self Deposit");

        Transaction savedTransaction = transactionRepository.save(transaction);

        // 5. Return mapped response
        TransactionDto dto = TransactionMapper.toTransactionDto(savedTransaction);
        dto.setCurrentBalance(updatedBalance);

        return dto;
    }
}