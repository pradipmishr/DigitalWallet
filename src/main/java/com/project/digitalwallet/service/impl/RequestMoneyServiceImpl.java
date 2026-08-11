package com.project.digitalwallet.service.impl;

import com.project.digitalwallet.common.enums.NotificationType;
import com.project.digitalwallet.common.enums.RequestMoneyStatus;
import com.project.digitalwallet.common.enums.TransactionStatus;
import com.project.digitalwallet.common.enums.TransactionType;
import com.project.digitalwallet.common.util.WalletTransactionEvent;
import com.project.digitalwallet.dto.AcceptMoneyRequest;
import com.project.digitalwallet.dto.CreateMoneyRequest;
import com.project.digitalwallet.dto.RequestMoneyDto;
import com.project.digitalwallet.entity.RequestMoney;
import com.project.digitalwallet.entity.Transaction;
import com.project.digitalwallet.entity.User;
import com.project.digitalwallet.entity.Wallet;
import com.project.digitalwallet.repository.RequestMoneyRepository;
import com.project.digitalwallet.repository.TransactionRepository;
import com.project.digitalwallet.repository.UserRepository;
import com.project.digitalwallet.repository.WalletRepository;
import com.project.digitalwallet.service.AuditLogService;
import com.project.digitalwallet.service.RequestMoneyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RequestMoneyServiceImpl implements RequestMoneyService {

    private final RequestMoneyRepository requestMoneyRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;
    private final HttpServletRequest httpServletRequest;

    @Override
    @Transactional
    public RequestMoneyDto createRequest(Long requesterId, CreateMoneyRequest request) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("Requester user not found."));

        // 1. Validate PIN
        validatePin(requester, request.getPin());

        // 2. Prevent self-requesting
        if (requester.getPhoneNumber().equals(request.getPayerPhoneNumber())) {
            throw new IllegalArgumentException("You cannot request money from yourself.");
        }

        // 3. Ensure recipient exists
        User payer = userRepository.findByPhoneNumber(request.getPayerPhoneNumber())
                .orElseThrow(() -> new IllegalArgumentException("Target user with phone number " + request.getPayerPhoneNumber() + " not found."));

        RequestMoney requestMoney = RequestMoney.builder()
                .requester(requester)
                .payer(payer)
                .amount(request.getAmount())
                .description(request.getDescription())
                .status(RequestMoneyStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusHours(24)) // 24-hour expiration
                .build();

        RequestMoney saved = requestMoneyRepository.save(requestMoney);

        // Audit log
        auditLogService.logEvent(
                requester.getId(),
                "MONEY_REQUEST_CREATED",
                String.format("Requested NPR %.2f from %s", request.getAmount(), payer.getPhoneNumber()),
                httpServletRequest
        );

        // Notify User B
        eventPublisher.publishEvent(new WalletTransactionEvent(
                payer.getId(),
                payer.getPhoneNumber(),
                NotificationType.MONEY_REQUEST_RECEIVED,
                request.getAmount(),
                "NPR",
                "REQ-" + saved.getId(),
                String.format("%s %s has requested NPR %.2f from you. Note: %s",
                        requester.getFirstName(), requester.getLastName(), request.getAmount(),
                        request.getDescription() != null ? request.getDescription() : "None")
        ));

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public RequestMoneyDto acceptRequest(Long payerId, Long requestId, AcceptMoneyRequest request) {
        User payer = userRepository.findById(payerId)
                .orElseThrow(() -> new IllegalArgumentException("Payer user not found."));

        // 1. Validate Payer's PIN
        validatePin(payer, request.getPin());

        RequestMoney requestMoney = requestMoneyRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Money request not found with ID: " + requestId));

        // 2. Authorization & Status check
        if (!requestMoney.getPayer().getId().equals(payerId)) {
            throw new IllegalStateException("Unauthorized to accept this request.");
        }

        if (requestMoney.getStatus() != RequestMoneyStatus.PENDING) {
            throw new IllegalStateException("Request is no longer pending.");
        }

        if (requestMoney.getExpiresAt().isBefore(LocalDateTime.now())) {
            requestMoneyRepository.delete(requestMoney);
            throw new IllegalStateException("Request has expired and has been removed.");
        }

        User requester = requestMoney.getRequester();
        Wallet payerWallet = walletRepository.findByUserId(payer.getId())
                .orElseThrow(() -> new IllegalArgumentException("Payer wallet not found."));
        Wallet requesterWallet = walletRepository.findByUserId(requester.getId())
                .orElseThrow(() -> new IllegalArgumentException("Requester wallet not found."));

        // 3. Balance verification
        if (payerWallet.getBalance().compareTo(requestMoney.getAmount()) < 0) {
            throw new IllegalStateException("Insufficient balance to honor this money request.");
        }

        // 4. Debit & Credit Wallet balances
        payerWallet.setBalance(payerWallet.getBalance().subtract(requestMoney.getAmount()));
        requesterWallet.setBalance(requesterWallet.getBalance().add(requestMoney.getAmount()));

        walletRepository.save(payerWallet);
        walletRepository.save(requesterWallet);

        // 5. Save Ledger Transaction
        String refNo = "REQ-TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Transaction transaction = Transaction.builder()
                .senderWallet(payerWallet)
                .receiverWallet(requesterWallet)
                .amount(requestMoney.getAmount())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.SUCCESS)
                .referenceNumber(refNo)
                .description("Paid money request #" + requestMoney.getId())
                .build();

        transactionRepository.save(transaction);

        // 6. Update Request Status
        requestMoney.setStatus(RequestMoneyStatus.COMPLETED);
        requestMoney.setCompletedAt(LocalDateTime.now());
        RequestMoney saved = requestMoneyRepository.save(requestMoney);

        // 7. Audit log
        auditLogService.logEvent(
                payer.getId(),
                "MONEY_REQUEST_ACCEPTED",
                String.format("Accepted request #%d and sent NPR %.2f to %s",
                        requestId, requestMoney.getAmount(), requester.getPhoneNumber()),
                httpServletRequest
        );

        // 8. Send Notifications to both users
        eventPublisher.publishEvent(new WalletTransactionEvent(
                requester.getId(),
                requester.getPhoneNumber(),
                NotificationType.MONEY_REQUEST_ACCEPTED,
                requestMoney.getAmount(),
                "NPR",
                refNo,
                String.format("%s %s accepted your money request of NPR %.2f.",
                        payer.getFirstName(), payer.getLastName(), requestMoney.getAmount())
        ));

        eventPublisher.publishEvent(new WalletTransactionEvent(
                payer.getId(),
                payer.getPhoneNumber(),
                NotificationType.TRANSACTION_DEBIT,
                requestMoney.getAmount(),
                "NPR",
                refNo,
                String.format("Paid NPR %.2f to %s %s for money request.",
                        requestMoney.getAmount(), requester.getFirstName(), requester.getLastName())
        ));

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public void declineRequest(Long payerId, Long requestId) {
        RequestMoney requestMoney = requestMoneyRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Money request not found with ID: " + requestId));

        if (!requestMoney.getPayer().getId().equals(payerId)) {
            throw new IllegalStateException("Unauthorized to decline this request.");
        }

        if (requestMoney.getStatus() != RequestMoneyStatus.PENDING) {
            throw new IllegalStateException("Request is no longer pending.");
        }

        requestMoney.setStatus(RequestMoneyStatus.DECLINED);
        requestMoneyRepository.save(requestMoney);

        auditLogService.logEvent(
                payerId,
                "MONEY_REQUEST_DECLINED",
                "Declined money request ID: " + requestId,
                httpServletRequest
        );

        eventPublisher.publishEvent(new WalletTransactionEvent(
                requestMoney.getRequester().getId(),
                requestMoney.getRequester().getPhoneNumber(),
                NotificationType.MONEY_REQUEST_DECLINED,
                null,
                "NPR",
                "REQ-" + requestId,
                String.format("%s declined your money request of NPR %.2f.",
                        requestMoney.getPayer().getFirstName(), requestMoney.getAmount())
        ));
    }

    @Override
    @Transactional
    public void cancelRequest(Long requesterId, Long requestId) {
        RequestMoney requestMoney = requestMoneyRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Money request not found with ID: " + requestId));

        if (!requestMoney.getRequester().getId().equals(requesterId)) {
            throw new IllegalStateException("Unauthorized to cancel this request.");
        }

        if (requestMoney.getStatus() != RequestMoneyStatus.PENDING) {
            throw new IllegalStateException("Request is no longer pending.");
        }

        requestMoney.setStatus(RequestMoneyStatus.CANCELLED);
        requestMoneyRepository.save(requestMoney);

        auditLogService.logEvent(
                requesterId,
                "MONEY_REQUEST_CANCELLED",
                "Cancelled money request ID: " + requestId,
                httpServletRequest
        );

        eventPublisher.publishEvent(new WalletTransactionEvent(
                requestMoney.getPayer().getId(),
                requestMoney.getPayer().getPhoneNumber(),
                NotificationType.MONEY_REQUEST_CANCELLED,
                null,
                "NPR",
                "REQ-" + requestId,
                String.format("%s cancelled their money request of NPR %.2f.",
                        requestMoney.getRequester().getFirstName(), requestMoney.getAmount())
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RequestMoneyDto> getSentRequests(Long userId) {
        return requestMoneyRepository.findByRequesterIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RequestMoneyDto> getReceivedRequests(Long userId) {
        return requestMoneyRepository.findByPayerIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // Cron job: Deletes all expired pending requests every hour
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    @Override
    public void cleanupExpiredRequests() {
        int deletedCount = requestMoneyRepository.deleteExpiredRequests(
                RequestMoneyStatus.PENDING,
                LocalDateTime.now()
        );
        if (deletedCount > 0) {
            System.out.println("Cron Job: Purged " + deletedCount + " expired money requests from database.");
        }
    }

    private void validatePin(User user, String rawPin) {
        if (user.getTransactionPin() == null) {
            throw new IllegalStateException("Transaction PIN is not set. Please set a transaction PIN first.");
        }
        if (!passwordEncoder.matches(rawPin, user.getTransactionPin())) {
            throw new IllegalArgumentException("Invalid transaction PIN.");
        }
    }

    private RequestMoneyDto mapToDto(RequestMoney r) {
        return RequestMoneyDto.builder()
                .id(r.getId())
                .requesterId(r.getRequester().getId())
                .requesterName(r.getRequester().getFirstName() + " " + r.getRequester().getLastName())
                .requesterPhoneNumber(r.getRequester().getPhoneNumber())
                .payerId(r.getPayer().getId())
                .payerName(r.getPayer().getFirstName() + " " + r.getPayer().getLastName())
                .payerPhoneNumber(r.getPayer().getPhoneNumber())
                .amount(r.getAmount())
                .description(r.getDescription())
                .status(r.getStatus())
                .expiresAt(r.getExpiresAt())
                .createdAt(r.getCreatedAt())
                .completedAt(r.getCompletedAt())
                .build();
    }
}