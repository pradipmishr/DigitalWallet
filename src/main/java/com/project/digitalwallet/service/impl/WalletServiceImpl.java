package com.project.digitalwallet.service.impl;

import com.project.digitalwallet.dto.*;
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
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final WalletNumberGenerator walletNumberGenerator;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

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
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No wallet found for user ID: " + userId));

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Cannot deposit funds into an inactive wallet.");
        }

        BigDecimal updatedBalance = wallet.getBalance().add(request.getAmount());
        wallet.setBalance(updatedBalance);
        walletRepository.save(wallet);

        // Clean entity creation via Mapper
        Transaction transaction = TransactionMapper.createDepositEntity(wallet, request.getAmount(), request.getDescription());
        Transaction savedTransaction = transactionRepository.save(transaction);

        TransactionDto dto = TransactionMapper.toTransactionDto(savedTransaction);
        //dto.setCurrentBalance(updatedBalance);
        return dto;
    }

    @Transactional
    @Override
    public TransactionDto transfer(Long senderUserId, TransferRequest request) {
        // 1. Fetch Sender User and Validate PIN
        User senderUser = userRepository.findById(senderUserId)
                .orElseThrow(() -> new IllegalArgumentException("Sender user not found."));

        if (senderUser.getTransactionPin() == null) {
            throw new IllegalStateException("Transaction PIN is not set. Please set a transaction PIN first.");
        }

        if (!passwordEncoder.matches(request.getPin(), senderUser.getTransactionPin())) {
            throw new IllegalArgumentException("Invalid Transaction PIN.");
        }

        // 2. Fetch Sender Wallet
        Wallet senderWallet = walletRepository.findByUserId(senderUserId)
                .orElseThrow(() -> new IllegalArgumentException("Sender wallet not found."));

        if (senderWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Sender wallet is inactive.");
        }

        // 3. Fetch Recipient User & Wallet
        User recipientUser = userRepository.findByPhoneNumber(request.getRecipientPhoneNumber())
                .orElseThrow(() -> new IllegalArgumentException("Recipient not found with phone number: " + request.getRecipientPhoneNumber()));

        if (recipientUser.getId().equals(senderUserId)) {
            throw new IllegalArgumentException("Cannot transfer money to yourself.");
        }

        Wallet receiverWallet = walletRepository.findByUserId(recipientUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Recipient wallet not found."));

        if (receiverWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Recipient wallet is inactive.");
        }

        // 4. Check Available Balance
        if (senderWallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalArgumentException("Insufficient wallet balance.");
        }

        // 5. Atomic Balance Updates
        BigDecimal newSenderBalance = senderWallet.getBalance().subtract(request.getAmount());
        BigDecimal newReceiverBalance = receiverWallet.getBalance().add(request.getAmount());

        senderWallet.setBalance(newSenderBalance);
        receiverWallet.setBalance(newReceiverBalance);

        walletRepository.save(senderWallet);
        walletRepository.save(receiverWallet);

        // 6. Record Transaction & Map Response
        Transaction transaction = TransactionMapper.createTransferEntity(senderWallet, receiverWallet, request.getAmount(), request.getDescription());
        Transaction savedTransaction = transactionRepository.save(transaction);

        TransactionDto dto = TransactionMapper.toTransactionDto(savedTransaction);
       // dto.setCurrentBalance(newSenderBalance);
        return dto;
    }


    @Transactional(readOnly = true)
    @Override
    public Page<TransactionDto> getTransactionHistory(Long userId, int page, int size) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for user ID: " + userId));

        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactionPage = transactionRepository.findAllByWalletId(wallet.getId(), pageable);

        return transactionPage.map(TransactionMapper::toTransactionDto);
    }
}