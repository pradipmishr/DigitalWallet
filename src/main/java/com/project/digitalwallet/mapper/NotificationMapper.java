package com.project.digitalwallet.mapper;

import com.project.digitalwallet.dto.NotificationDto;
import com.project.digitalwallet.entity.Notification;

public class NotificationMapper {
     public static NotificationDto mapToDto(Notification notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .referenceId(notification.getReferenceId())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
