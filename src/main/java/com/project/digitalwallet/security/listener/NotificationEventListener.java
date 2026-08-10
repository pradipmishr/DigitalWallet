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
        log.info("Processing notification event for User ID: {}", event.userId());

        String title = (event.type() == NotificationType.TRANSACTION_CREDIT)
                ? "Money Received"
                : "Money Sent";

        String message = String.format("%s of %s was successful. Ref: %s",
                title, event.amount(), event.referenceId());

        // 1. Save to Database
        Notification notification = Notification.builder()
                .userId(event.userId())
                .type(event.type())
                .title(title)
                .message(message)
                .referenceId(event.referenceId())
                .read(false)
                .build();

        Notification savedNotification = notificationRepository.save(notification);

        // 2. Map to DTO
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

        // 3. Send to User
        messagingTemplate.convertAndSendToUser(
                event.userPhoneNumber(),
                "/queue/notifications",
                dto
        );

        log.info("STOMP message sent to messagingTemplate!");
    }
}