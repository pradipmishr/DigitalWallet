package com.project.digitalwallet.service;



import com.project.digitalwallet.dto.NotificationDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    Page<NotificationDto> getUserNotifications(String phoneNumber, Pageable pageable);
//    long getUnreadCount(String email);
//    void markAsRead(Long notificationId, String email);
//    void markAllAsRead(String email);
}
