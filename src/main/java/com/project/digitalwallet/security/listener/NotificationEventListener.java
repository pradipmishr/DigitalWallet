package com.project.digitalwallet.security.listener;

import com.project.digitalwallet.common.enums.NotificationType;
import com.project.digitalwallet.common.util.WalletTransactionEvent;
import com.project.digitalwallet.dto.NotificationDto;
import com.project.digitalwallet.entity.Notification;
import com.project.digitalwallet.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleWalletTransactionEvent(WalletTransactionEvent event) {
        log.info("Processing notification event for User ID: {}, Type: {}", event.userId(), event.type());

        String title;
        String message;

        // Formatted amount representation (e.g., 500 instead of 500.0000)
        String formattedAmount = (event.amount() != null)
                ? event.amount().stripTrailingZeros().toPlainString()
                : "0";

        // 1. Dynamically determine Title and Message based on NotificationType
        switch (event.type()) {
            case TRANSACTION_CREDIT:
                title = "Money Received";
                message = buildTransactionMessage(
                        "Received Rs. " + formattedAmount,
                        event.description()
                );
                break;

            case TRANSACTION_DEBIT:
                title = "Money Sent";
                message = buildTransactionMessage(
                        "Money sent Rs. " + formattedAmount,
                        event.description()
                );
                break;

            case SECURITY_ALERT:
                title = "Security Alert";
                message = (event.description() != null && !event.description().isBlank())
                        ? event.description()
                        : "A security action was performed on your account.";
                break;

            case WELCOME:
                title = "Welcome!";
                message = (event.description() != null && !event.description().isBlank())
                        ? event.description()
                        : "Welcome to Digital Wallet!";
                break;

            case KYC_VERIFIED:
                title = "KYC Approved";
                message = (event.description() != null && !event.description().isBlank())
                        ? event.description()
                        : "Your KYC verification has been approved successfully.";
                break;

            case KYC_REJECTED:
                title = "KYC Rejected";
                message = (event.description() != null && !event.description().isBlank())
                        ? event.description()
                        : "Your KYC verification was rejected. Please review and resubmit.";
                break;

            default:
                title = "Account Notification";
                message = (event.description() != null && !event.description().isBlank())
                        ? event.description()
                        : "You have a new notification.";
                break;
        }

        // 2. Save Notification Entity to Database
        Notification notification = Notification.builder()
                .userId(event.userId())
                .type(event.type())
                .title(title)
                .message(message)
                .referenceId(event.referenceId())
                .read(false)
                .build();

        Notification savedNotification = notificationRepository.save(notification);

        // 3. Map to DTO
        NotificationDto dto = NotificationDto.builder()
                .id(savedNotification.getId())
                .type(savedNotification.getType())
                .title(savedNotification.getTitle())
                .message(savedNotification.getMessage())
                .referenceId(savedNotification.getReferenceId())
                .read(savedNotification.isRead())
                .createdAt(savedNotification.getCreatedAt())
                .build();

        log.info("Sending STOMP message to userPhoneNumber: '{}' at /queue/notifications", event.userPhoneNumber());

        // 4. Push via STOMP / WebSocket
        messagingTemplate.convertAndSendToUser(
                event.userPhoneNumber(),
                "/queue/notifications",
                dto
        );

        log.info("STOMP message sent successfully to userPhoneNumber: {}", event.userPhoneNumber());
    }

    /**
     * Helper method to combine base message with description/remarks
     */
    private String buildTransactionMessage(String baseText, String description) {
        if (description == null || description.isBlank()) {
            return baseText;
        }

        // If description already starts with base action details, avoid duplicating it
        if (description.startsWith("Transfer to ") || description.startsWith("Received money from ")) {
            return baseText + " (" + description + ")";
        }

        return baseText + " (" + description + ")";
    }
}