package com.project.digitalwallet.service.impl;


import com.project.digitalwallet.dto.NotificationDto;
import com.project.digitalwallet.entity.Notification;
import com.project.digitalwallet.entity.User;
import com.project.digitalwallet.mapper.NotificationMapper;
import com.project.digitalwallet.repository.NotificationRepository;
import com.project.digitalwallet.repository.UserRepository;
import com.project.digitalwallet.service.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDto> getUserNotifications(String email, Pageable pageable) {
        User user = getUserByEmail(email);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(NotificationMapper::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(String email) {
        User user = getUserByEmail(email);
        return notificationRepository.countByUserIdAndReadFalse(user.getId());
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, String email) {
        User user = getUserByEmail(email);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found: " + notificationId));

        if (!notification.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("Unauthorized to update this notification.");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(String email) {
        User user = getUserByEmail(email);
        notificationRepository.markAllAsReadForUser(user.getId());
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + email));
    }


}
