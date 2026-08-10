package com.project.digitalwallet.dto;

import com.project.digitalwallet.common.enums.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationDto {
    private Long id;
    private NotificationType type;
    private String title;
    private String message;
    private String referenceId;
    private boolean read;
    private LocalDateTime createdAt;
}
