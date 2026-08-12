package com.project.digitalwallet.service.impl;

import com.project.digitalwallet.common.enums.KycStatus;
import com.project.digitalwallet.common.enums.NotificationType;
import com.project.digitalwallet.common.enums.ScheduledPaymentStatus;
import com.project.digitalwallet.common.enums.TransactionStatus;
import com.project.digitalwallet.common.enums.TransactionType;
import com.project.digitalwallet.common.util.TransactionLimitValidator;
import com.project.digitalwallet.common.util.WalletTransactionEvent;
import com.project.digitalwallet.dto.CancelScheduledPaymentRequest;
import com.project.digitalwallet.dto.CreateScheduledPaymentRequest;
import com.project.digitalwallet.dto.ScheduledPaymentDto;
import com.project.digitalwallet.entity.KycDetails;
import com.project.digitalwallet.entity.ScheduledPayment;
import com.project.digitalwallet.entity.Transaction;
import com.project.digitalwallet.entity.User;
import com.project.digitalwallet.entity.Wallet;
import com.project.digitalwallet.mapper.ScheduleMapper;
import com.project.digitalwallet.repository.KycDetailsRepository;
import com.project.digitalwallet.repository.ScheduledPaymentRepository;
import com.project.digitalwallet.repository.TransactionRepository;
import com.project.digitalwallet.repository.UserRepository;
import com.project.digitalwallet.repository.WalletRepository;
import com.project.digitalwallet.service.AuditLogService;
import com.project.digitalwallet.service.ScheduledPaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.project.digitalwallet.mapper.ScheduleMapper.mapToDto;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledPaymentServiceImpl implements ScheduledPaymentService {

    private final ScheduledPaymentRepository scheduledPaymentRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final KycDetailsRepository kycDetailsRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;
    private final HttpServletRequest httpServletRequest;
    private final TransactionLimitValidator limitValidator;

    @Override
    @Transactional
    public ScheduledPaymentDto createSchedule(Long userId, CreateScheduledPaymentRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // 1. Enforce KYC Verification Access Control
        validateKycStatus(user.getId());

        // 2. Ensure recipient exists
        if (!userRepository.existsByPhoneNumber(request.getRecipientPhoneNumber())) {
            throw new IllegalArgumentException("Recipient phone number not registered: " + request.getRecipientPhoneNumber());
        }

        ScheduledPayment schedule = ScheduledPayment.builder()
                .user(user)
                .recipientPhoneNumber(request.getRecipientPhoneNumber())
                .amount(request.getAmount())
                .frequency(request.getFrequency())
                .status(ScheduledPaymentStatus.ACTIVE)
                .nextRunAt(request.getStartAt())
                .totalOccurrences(request.getTotalOccurrences())
                .completedOccurrences(0)
                .failedAttempts(0)
                .description(request.getDescription())
                .build();

        ScheduledPayment saved = scheduledPaymentRepository.save(schedule);
        auditLogService.logEvent(
                user.getId(),
                "SCHEDULED_PAYMENT_CREATED",
                String.format("Created %s scheduled payment of NPR %.2f to %s",
                        request.getFrequency(), request.getAmount(), request.getRecipientPhoneNumber()),
                httpServletRequest
        );
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduledPaymentDto> getUserSchedules(Long userId) {
        return scheduledPaymentRepository.findByUserId(userId)
                .stream()
                .map(ScheduleMapper::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancelSchedule(Long userId, Long scheduleId, CancelScheduledPaymentRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // Verify Transaction PIN
        validateTransactionPin(user, request.getPin());

        ScheduledPayment schedule = scheduledPaymentRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found with ID: " + scheduleId));

        if (!schedule.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Unauthorized to cancel this schedule.");
        }

        if (schedule.getStatus() == ScheduledPaymentStatus.CANCELLED) {
            throw new IllegalStateException("Schedule is already cancelled.");
        }

        schedule.setStatus(ScheduledPaymentStatus.CANCELLED);
        auditLogService.logEvent(
                user.getId(),
                "SCHEDULED_PAYMENT_CANCELLED",
                String.format("Cancelled scheduled payment ID: %d (Recipient: %s, Amount: NPR %.2f)",
                        scheduleId, schedule.getRecipientPhoneNumber(), schedule.getAmount()),
                httpServletRequest
        );
        scheduledPaymentRepository.save(schedule);
    }

    /**
     * Cron Job that runs every minute to process pending scheduled payments.
     */
    @Scheduled(cron = "0 * * * * *")
    public void processDueScheduledPayments() {
        List<ScheduledPayment> dueSchedules = scheduledPaymentRepository
                .findByStatusAndNextRunAtLessThanEqual(ScheduledPaymentStatus.ACTIVE, LocalDateTime.now());

        for (ScheduledPayment schedule : dueSchedules) {
            try {
                executeScheduledPayment(schedule);
            } catch (Exception e) {
                log.error("Failed to execute scheduled payment ID {}: {}", schedule.getId(), e.getMessage());
                handleFailedExecution(schedule);
            }
        }
    }


    @Transactional
    public void executeScheduledPayment(ScheduledPayment schedule) {
        User sender = schedule.getUser();
        User recipient = userRepository.findByPhoneNumber(schedule.getRecipientPhoneNumber())
                .orElseThrow(() -> new IllegalArgumentException("Recipient user not found: " + schedule.getRecipientPhoneNumber()));

        Wallet senderWallet = walletRepository.findByUserId(sender.getId())
                .orElseThrow(() -> new IllegalArgumentException("Sender wallet not found."));
        Wallet recipientWallet = walletRepository.findByUserId(recipient.getId())
                .orElseThrow(() -> new IllegalArgumentException("Recipient wallet not found."));

        // 1. Balance verification
        if (senderWallet.getBalance().compareTo(schedule.getAmount()) < 0) {
            throw new IllegalStateException("Insufficient wallet balance for scheduled payment.");
        }

        // 2. Validate Tiered Transaction Limits
        limitValidator.validateTieredLimits(sender, senderWallet, schedule.getAmount());

        // 3. Perform balance updates
        senderWallet.setBalance(senderWallet.getBalance().subtract(schedule.getAmount()));
        recipientWallet.setBalance(recipientWallet.getBalance().add(schedule.getAmount()));

        walletRepository.save(senderWallet);
        walletRepository.save(recipientWallet);

        // 4. Save transaction ledger record
        String refNo = "SCH-TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Transaction transaction = Transaction.builder()
                .senderWallet(senderWallet)
                .receiverWallet(recipientWallet)
                .amount(schedule.getAmount())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.SUCCESS)
                .referenceNumber(refNo)
                .description("Automated scheduled payment #" + schedule.getId())
                .build();

        transactionRepository.save(transaction);

        // 5. Update schedule state & calculate next execution time
        schedule.setCompletedOccurrences(schedule.getCompletedOccurrences() + 1);
        schedule.setFailedAttempts(0);

        if (schedule.getTotalOccurrences() != null && schedule.getCompletedOccurrences() >= schedule.getTotalOccurrences()) {
            schedule.setStatus(ScheduledPaymentStatus.COMPLETED);
        } else {
            schedule.setNextRunAt(calculateNextRunDate(schedule.getNextRunAt(), schedule.getFrequency()));
        }

        scheduledPaymentRepository.save(schedule);

        // 6. Notify both users (Debit for sender, Credit for recipient)
        eventPublisher.publishEvent(new WalletTransactionEvent(
                sender.getId(),
                sender.getPhoneNumber(),
                NotificationType.TRANSACTION_DEBIT,
                schedule.getAmount(),
                "NPR",
                refNo,
                String.format("Scheduled payment of NPR %.2f sent to %s.", schedule.getAmount(), recipient.getPhoneNumber())
        ));

        eventPublisher.publishEvent(new WalletTransactionEvent(
                recipient.getId(),
                recipient.getPhoneNumber(),
                NotificationType.TRANSACTION_CREDIT,
                schedule.getAmount(),
                "NPR",
                refNo,
                String.format("Received NPR %.2f from %s %s via scheduled payment.",
                        schedule.getAmount(), sender.getFirstName(), sender.getLastName())
        ));
    }

    private void handleFailedExecution(ScheduledPayment schedule) {
        int failedAttempts = schedule.getFailedAttempts() + 1;
        schedule.setFailedAttempts(failedAttempts);

        // Pause schedule if it fails 3 consecutive times
        if (failedAttempts >= 3) {
            schedule.setStatus(ScheduledPaymentStatus.PAUSED);
        }

        scheduledPaymentRepository.save(schedule);
    }

    private LocalDateTime calculateNextRunDate(LocalDateTime currentRun, com.project.digitalwallet.common.enums.ScheduleFrequency frequency) {
        return switch (frequency) {
            case DAILY -> currentRun.plusDays(1);
            case WEEKLY -> currentRun.plusWeeks(1);
            case MONTHLY -> currentRun.plusMonths(1);
        };
    }

    private void validateKycStatus(Long userId) {
        Optional<KycDetails> kycOptional = kycDetailsRepository.findByUserId(userId);
        KycStatus status = kycOptional.map(KycDetails::getStatus).orElse(KycStatus.UNVERIFIED);

        if (status != KycStatus.VERIFIED) {
            throw new IllegalStateException("KYC verification is required to use scheduled payments. Please complete your KYC verification first.");
        }
    }

    private void validateTransactionPin(User user, String rawPin) {
        if (user.getTransactionPin() == null) {
            throw new IllegalStateException("Transaction PIN is not set. Please set a transaction PIN first.");
        }
        if (!passwordEncoder.matches(rawPin, user.getTransactionPin())) {
            throw new IllegalArgumentException("Invalid transaction PIN.");
        }
    }
}