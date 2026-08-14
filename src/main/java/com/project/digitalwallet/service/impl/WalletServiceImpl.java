package com.project.digitalwallet.service.impl;

import com.project.digitalwallet.common.enums.KycStatus;
import com.project.digitalwallet.common.enums.NotificationType;
import com.project.digitalwallet.common.util.TransactionLimitValidator;
import com.project.digitalwallet.common.util.WalletTransactionEvent;
import com.project.digitalwallet.dto.*;
import com.project.digitalwallet.entity.KycDetails;
import com.project.digitalwallet.entity.Transaction;
import com.project.digitalwallet.entity.User;
import com.project.digitalwallet.entity.Wallet;
import com.project.digitalwallet.common.enums.WalletStatus;
import com.project.digitalwallet.mapper.TransactionMapper;
import com.project.digitalwallet.mapper.WalletMapper;
import com.project.digitalwallet.repository.KycDetailsRepository;
import com.project.digitalwallet.repository.TransactionRepository;
import com.project.digitalwallet.repository.UserRepository;
import com.project.digitalwallet.repository.WalletRepository;
import com.project.digitalwallet.service.AdminService;
import com.project.digitalwallet.service.AuditLogService;
import com.project.digitalwallet.service.WalletService;
import com.project.digitalwallet.common.util.WalletNumberGenerator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Optional;

@Slf4j
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
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionLimitValidator limitValidator;

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

        eventPublisher.publishEvent(new WalletTransactionEvent(
                user.getId(),
                user.getPhoneNumber(),
                NotificationType.WELCOME,
                BigDecimal.ZERO,
                "NPR",
                savedWallet.getWalletNumber(),
                "Welcome to Digital Wallet!"
        ));
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

        // Acquire Pessimistic Lock on the wallet during deposit to prevent race conditions
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
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

        // Publish notification event (fires asynchronously AFTER database commit)
        eventPublisher.publishEvent(new WalletTransactionEvent(
                user.getId(),
                user.getPhoneNumber(),
                NotificationType.TRANSACTION_CREDIT,
                request.getAmount(),
                "NPR",
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

        // 1. Verify PIN and Log Failures before taking locks
        if (!passwordEncoder.matches(request.getPin(), senderUser.getTransactionPin())) {
            auditLogService.logEvent(
                    senderUser.getId(),
                    "FAILED_PIN_VERIFICATION",
                    "Failed transfer attempt: Invalid PIN entered.",
                    httpServletRequest
            );
            throw new IllegalArgumentException("Invalid Transaction PIN.");
        }

        User recipientUser = userRepository.findByPhoneNumber(request.getRecipientPhoneNumber())
                .orElseThrow(() -> new IllegalArgumentException("Recipient not found with phone number: " + request.getRecipientPhoneNumber()));

        if (recipientUser.getId().equals(senderUserId)) {
            throw new IllegalArgumentException("Cannot transfer money to yourself.");
        }

        // 2. Fetch un-locked Wallet references to identify Wallet Primary Keys (IDs) for ordering
        Wallet un_lockedSenderWallet = walletRepository.findByUserId(senderUserId)
                .orElseThrow(() -> new IllegalArgumentException("Sender wallet not found."));

        Wallet un_lockedReceiverWallet = walletRepository.findByUserId(recipientUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Recipient wallet not found."));

        Long senderWalletId = un_lockedSenderWallet.getId();
        Long receiverWalletId = un_lockedReceiverWallet.getId();

        // 3. DETERMINISTIC LOCK ORDERING (Deadlock Prevention)
        // Always acquire the lock on the lower Wallet ID first
        Long firstLockWalletId = senderWalletId.compareTo(receiverWalletId) < 0 ? senderWalletId : receiverWalletId;
        Long secondLockWalletId = senderWalletId.compareTo(receiverWalletId) < 0 ? receiverWalletId : senderWalletId;

        log.info("Pessimistically locking wallets in order: Wallet ID {} -> Wallet ID {}", firstLockWalletId, secondLockWalletId);

        Wallet firstWallet = walletRepository.findByIdForUpdate(firstLockWalletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found with ID: " + firstLockWalletId));

        Wallet secondWallet = walletRepository.findByIdForUpdate(secondLockWalletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found with ID: " + secondLockWalletId));

        // Re-assign sender and receiver references from the locked instances
        Wallet senderWallet = senderWalletId.equals(firstWallet.getId()) ? firstWallet : secondWallet;
        Wallet receiverWallet = receiverWalletId.equals(firstWallet.getId()) ? firstWallet : secondWallet;

        // 4. Validate Wallet Statuses
        if (senderWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Sender wallet is inactive.");
        }

        if (receiverWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Recipient wallet is inactive.");
        }

        // 5. Validate Balances & Tiered Limits
        if (senderWallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalArgumentException("Insufficient wallet balance.");
        }

        limitValidator.validateTieredLimits(senderUser, senderWallet, request.getAmount());

        // 6. Perform Balance Updates Safely
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