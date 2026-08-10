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

        // 1. Dynamically determine Title and Message based on NotificationType
        switch (event.type()) {
            case TRANSACTION_CREDIT:
                title = "Money Received";
                message = event.description() != null ? event.description()
                        : String.format("Received %s %s. Ref: %s", event.currency(), event.amount(), event.referenceId());
                break;

            case TRANSACTION_DEBIT:
                title = "Money Sent";
                message = event.description() != null ? event.description()
                        : String.format("Sent %s %s. Ref: %s", event.currency(), event.amount(), event.referenceId());
                break;

            case SECURITY_ALERT:
                title = "Security Alert";
                message = event.description() != null ? event.description()
                        : "A security action was performed on your account.";
                break;

            case WELCOME:
                title = "Welcome!";
                message = event.description() != null ? event.description()
                        : "Welcome to Digital Wallet!";
                break;

            default:
                title = "Account Notification";
                message = event.description() != null ? event.description()
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
}