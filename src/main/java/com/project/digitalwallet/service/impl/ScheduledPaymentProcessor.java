package com.project.digitalwallet.service.impl;

import com.project.digitalwallet.common.enums.NotificationType;
import com.project.digitalwallet.common.enums.ScheduleFrequency;
import com.project.digitalwallet.common.enums.ScheduledPaymentStatus;
import com.project.digitalwallet.common.util.WalletTransactionEvent;
import com.project.digitalwallet.dto.TransferRequest;
import com.project.digitalwallet.entity.ScheduledPayment;
import com.project.digitalwallet.repository.ScheduledPaymentRepository;
import com.project.digitalwallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledPaymentProcessor {

    private final ScheduledPaymentRepository scheduledPaymentRepository;
    private final WalletService walletService;
    private final ApplicationEventPublisher eventPublisher;

    private static final int MAX_FAILED_ATTEMPTS = 3;

    @Scheduled(fixedDelay = 60000) // Runs every 60 seconds
    public void processDueScheduledPayments() {
        LocalDateTime now = LocalDateTime.now();
        List<ScheduledPayment> duePayments = scheduledPaymentRepository.findDuePayments(
                ScheduledPaymentStatus.ACTIVE, now
        );

        if (duePayments.isEmpty()) {
            return;
        }

        log.info("Found {} due scheduled payment(s) to process.", duePayments.size());

        for (ScheduledPayment payment : duePayments) {
            try {
                processSinglePayment(payment);
            } catch (Exception e) {
                log.error("Failed to process scheduled payment ID: {}", payment.getId(), e);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSinglePayment(ScheduledPayment payment) {
        try {
            // Build TransferRequest for scheduled transfer
            // Note: If walletService.transfer validates the PIN, either pass the PIN or
            // use an internal transfer method: walletService.internalTransfer(payment.getUser().getId(), ...)
            TransferRequest transferRequest = new TransferRequest();
            transferRequest.setRecipientPhoneNumber(payment.getRecipientPhoneNumber());
            transferRequest.setAmount(payment.getAmount());
            transferRequest.setDescription(payment.getDescription() != null ? payment.getDescription() : "Scheduled Recurring Transfer");

            // If your transfer method strictly checks request.getPin(), bypass it inside WalletService or pass a system token
            var txDto = walletService.transfer(payment.getUser().getId(), transferRequest);

            // Update execution metadata
            payment.setLastRunAt(LocalDateTime.now());
            payment.setCompletedOccurrences(payment.getCompletedOccurrences() + 1);
            payment.setFailedAttempts(0);

            if (payment.getTotalOccurrences() != null
                    && payment.getCompletedOccurrences() >= payment.getTotalOccurrences()) {
                payment.setStatus(ScheduledPaymentStatus.COMPLETED);
            } else {
                payment.setNextRunAt(calculateNextRunTime(payment.getNextRunAt(), payment.getFrequency()));
            }

            scheduledPaymentRepository.save(payment);
            log.info("Successfully executed scheduled payment ID: {}, TxRef: {}", payment.getId(), txDto.getReferenceNumber());

        } catch (Exception ex) {
            log.warn("Scheduled payment ID: {} failed. Reason: {}", payment.getId(), ex.getMessage());

            int failedCount = payment.getFailedAttempts() + 1;
            payment.setFailedAttempts(failedCount);

            if (failedCount >= MAX_FAILED_ATTEMPTS) {
                payment.setStatus(ScheduledPaymentStatus.FAILED);

                eventPublisher.publishEvent(new WalletTransactionEvent(
                        payment.getUser().getId(),
                        payment.getUser().getPhoneNumber(),
                        NotificationType.SECURITY_ALERT,
                        payment.getAmount(),
                        "NPR",
                        "SCHED-FAIL-" + payment.getId(),
                        String.format("Scheduled payment to %s failed: %s. Schedule marked as FAILED.",
                                payment.getRecipientPhoneNumber(), ex.getMessage())
                ));
            } else {
                // Retry in 1 hour
                payment.setNextRunAt(LocalDateTime.now().plusHours(1));
            }

            scheduledPaymentRepository.save(payment);
        }
    }

    private LocalDateTime calculateNextRunTime(LocalDateTime currentRun, ScheduleFrequency frequency) {
        return switch (frequency) {
            case DAILY -> currentRun.plusDays(1);
            case WEEKLY -> currentRun.plusWeeks(1);
            case MONTHLY -> currentRun.plusMonths(1);
        };
    }
}