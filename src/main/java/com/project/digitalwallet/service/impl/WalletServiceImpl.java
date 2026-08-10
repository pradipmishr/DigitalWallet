package com.project.digitalwallet.service.impl;

import com.project.digitalwallet.common.enums.NotificationType;
import com.project.digitalwallet.common.util.WalletTransactionEvent;
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
import com.project.digitalwallet.service.AdminService;
import com.project.digitalwallet.service.AuditLogService;
import com.project.digitalwallet.service.WalletService;
import com.project.digitalwallet.common.util.WalletNumberGenerator;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
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
    private final AuditLogService auditLogService;
    private final HttpServletRequest httpServletRequest;
    private final AdminService adminService;
    private final ApplicationEventPublisher eventPublisher;


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
    @Transactional(readOnly = true)
    @Override
    public WalletDto getCurrentUserWallet(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for user ID: " + userId));

        return WalletMapper.toWalletDto(wallet);
    }

    @Transactional
    @Override
    public TransactionDto deposit(Long userId, DepositRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No wallet found for user ID: " + userId));

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Cannot deposit funds into an inactive wallet.");
        }

        wallet.setBalance(wallet.getBalance().add(request.getAmount()));
        walletRepository.save(wallet);

        Transaction transaction = TransactionMapper.createDepositEntity(wallet, request.getAmount(), request.getDescription());
        Transaction savedTransaction = transactionRepository.save(transaction);

        // Log deposit action
        auditLogService.logEvent(
                user.getId(),
                "DEPOSIT_SUCCESS",
                String.format("Deposited %s into wallet.", request.getAmount()),
                httpServletRequest
        );

        // Publish notification event (will fire asynchronously AFTER database commit)
        eventPublisher.publishEvent(new WalletTransactionEvent(
                user.getId(),
                user.getPhoneNumber(),
                NotificationType.TRANSACTION_CREDIT,
                request.getAmount(),
                "NPR", // Replace with your default wallet currency or wallet.getCurrency() if available
                savedTransaction.getReferenceNumber(),
                request.getDescription() != null ? request.getDescription() : "Deposit to wallet"
        ));

        return TransactionMapper.toTransactionDto(savedTransaction);
    }

    @Transactional
    @Override
    public TransactionDto transfer(Long senderUserId, TransferRequest request) {
        User senderUser = userRepository.findById(senderUserId)
                .orElseThrow(() -> new IllegalArgumentException("Sender user not found."));

        if (senderUser.getTransactionPin() == null) {
            throw new IllegalStateException("Transaction PIN is not set.");
        }

        // Verify PIN and Log Failures
        if (!passwordEncoder.matches(request.getPin(), senderUser.getTransactionPin())) {
            auditLogService.logEvent(
                    senderUser.getId(),
                    "FAILED_PIN_VERIFICATION",
                    "Failed transfer attempt: Invalid PIN entered.",
                    httpServletRequest
            );
            throw new IllegalArgumentException("Invalid Transaction PIN.");
        }

        Wallet senderWallet = walletRepository.findByUserId(senderUserId)
                .orElseThrow(() -> new IllegalArgumentException("Sender wallet not found."));

        if (senderWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Sender wallet is inactive.");
        }

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

        if (senderWallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalArgumentException("Insufficient wallet balance.");
        }


        validateDailyLimit(senderWallet, request.getAmount());

        // Perform balance update
        senderWallet.setBalance(senderWallet.getBalance().subtract(request.getAmount()));
        receiverWallet.setBalance(receiverWallet.getBalance().add(request.getAmount()));

        walletRepository.save(senderWallet);
        walletRepository.save(receiverWallet);

        Transaction transaction = TransactionMapper.createTransferEntity(
                senderWallet, receiverWallet, request.getAmount(), request.getDescription()
        );
        Transaction savedTransaction = transactionRepository.save(transaction);

        // Log successful transfer
        auditLogService.logEvent(
                senderUser.getId(),
                "TRANSFER_SUCCESS",
                String.format("Transferred %s to phone %s", request.getAmount(), request.getRecipientPhoneNumber()),
                httpServletRequest
        );
        // Publish Notification Event for Sender (Debit)
        eventPublisher.publishEvent(new WalletTransactionEvent(
                senderUser.getId(),
                senderUser.getPhoneNumber(),
                NotificationType.TRANSACTION_DEBIT,
                request.getAmount(),
                "NPR",
                savedTransaction.getReferenceNumber(),
                request.getDescription() != null ? request.getDescription() : "Transfer to " + recipientUser.getPhoneNumber()
        ));

        // Publish Notification Event for Recipient (Credit)
        eventPublisher.publishEvent(new WalletTransactionEvent(
                recipientUser.getId(),
                recipientUser.getPhoneNumber(),
                NotificationType.TRANSACTION_CREDIT,
                request.getAmount(),
                "NPR",
                savedTransaction.getReferenceNumber(),
                request.getDescription() != null ? request.getDescription() : "Received money from " + senderUser.getPhoneNumber()
        ));

        return TransactionMapper.toTransactionDto(savedTransaction);
    }
    private void validateDailyLimit(Wallet senderWallet, BigDecimal transferAmount) {
        java.time.LocalDateTime startOfDay = java.time.LocalDate.now().atStartOfDay();
        java.time.LocalDateTime endOfDay = java.time.LocalDate.now().atTime(java.time.LocalTime.MAX);

        BigDecimal totalToday = transactionRepository.findTotalTransferredToday(
                senderWallet.getId(),
                startOfDay,
                endOfDay
        );

        BigDecimal globalLimit = adminService.getGlobalDailyLimit();
        BigDecimal projectedTotal = totalToday.add(transferAmount);

        if (projectedTotal.compareTo(globalLimit) > 0) {
            BigDecimal remainingLimit = globalLimit.subtract(totalToday);
            if (remainingLimit.compareTo(BigDecimal.ZERO) < 0) {
                remainingLimit = BigDecimal.ZERO;
            }

            throw new IllegalStateException(String.format(
                    "Transfer exceeds the global daily limit of %s. Total transferred today: %s. Remaining daily limit: %s",
                    globalLimit, totalToday, remainingLimit
            ));
        }
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