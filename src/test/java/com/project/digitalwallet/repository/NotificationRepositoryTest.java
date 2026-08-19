package com.project.digitalwallet.repository;

import com.project.digitalwallet.entity.Notification;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void findByUserIdOrderByCreatedAtDesc_shouldReturnNotificationsInDescendingOrder() {
        LocalDateTime now = LocalDateTime.now();

        Notification older = new Notification();
        older.setUserId(1L);
        older.setCreatedAt(now.minusMinutes(10));
        older.setRead(false);

        Notification newer = new Notification();
        newer.setUserId(1L);
        newer.setCreatedAt(now.minusMinutes(2));
        newer.setRead(false);

        Notification otherUser = new Notification();
        otherUser.setUserId(2L);
        otherUser.setCreatedAt(now);
        otherUser.setRead(false);

        notificationRepository.save(older);
        notificationRepository.save(newer);
        notificationRepository.save(otherUser);

        Page<Notification> result =
                notificationRepository.findByUserIdOrderByCreatedAtDesc(
                        1L,
                        PageRequest.of(0, 10)
                );

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getId())
                .isEqualTo(newer.getId());
        assertThat(result.getContent().get(1).getId())
                .isEqualTo(older.getId());
    }

    @Test
    void findByUserIdOrderByCreatedAtDesc_shouldApplyPagination() {
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < 5; i++) {
            Notification notification = new Notification();
            notification.setUserId(1L);
            notification.setCreatedAt(now.minusMinutes(i));
            notification.setRead(false);

            notificationRepository.save(notification);
        }

        Page<Notification> result =
                notificationRepository.findByUserIdOrderByCreatedAtDesc(
                        1L,
                        PageRequest.of(0, 2)
                );

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getTotalPages()).isEqualTo(3);
    }

    @Test
    void countByUserIdAndReadFalse_shouldReturnUnreadNotificationCount() {
        Notification unread1 = new Notification();
        unread1.setUserId(1L);
        unread1.setCreatedAt(LocalDateTime.now());
        unread1.setRead(false);

        Notification unread2 = new Notification();
        unread2.setUserId(1L);
        unread2.setCreatedAt(LocalDateTime.now());
        unread2.setRead(false);

        Notification read = new Notification();
        read.setUserId(1L);
        read.setCreatedAt(LocalDateTime.now());
        read.setRead(true);

        Notification otherUser = new Notification();
        otherUser.setUserId(2L);
        otherUser.setCreatedAt(LocalDateTime.now());
        otherUser.setRead(false);

        notificationRepository.save(unread1);
        notificationRepository.save(unread2);
        notificationRepository.save(read);
        notificationRepository.save(otherUser);

        long count =
                notificationRepository.countByUserIdAndReadFalse(1L);

        assertThat(count).isEqualTo(2);
    }

    @Test
    void countByUserIdAndReadFalse_shouldReturnZeroWhenNoUnreadNotifications() {
        Notification notification = new Notification();
        notification.setUserId(1L);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(true);

        notificationRepository.save(notification);

        long count =
                notificationRepository.countByUserIdAndReadFalse(1L);

        assertThat(count).isZero();
    }

    @Test
    void markAllAsReadForUser_shouldMarkAllUnreadNotificationsAsRead() {
        Notification unread1 = new Notification();
        unread1.setUserId(1L);
        unread1.setCreatedAt(LocalDateTime.now());
        unread1.setRead(false);

        Notification unread2 = new Notification();
        unread2.setUserId(1L);
        unread2.setCreatedAt(LocalDateTime.now());
        unread2.setRead(false);

        Notification alreadyRead = new Notification();
        alreadyRead.setUserId(1L);
        alreadyRead.setCreatedAt(LocalDateTime.now());
        alreadyRead.setRead(true);

        Notification otherUser = new Notification();
        otherUser.setUserId(2L);
        otherUser.setCreatedAt(LocalDateTime.now());
        otherUser.setRead(false);

        notificationRepository.save(unread1);
        notificationRepository.save(unread2);
        notificationRepository.save(alreadyRead);
        notificationRepository.save(otherUser);

        int updated =
                notificationRepository.markAllAsReadForUser(1L);

        assertThat(updated).isEqualTo(2);

        notificationRepository.flush();

        Notification updatedUnread1 =
                notificationRepository.findById(unread1.getId()).orElseThrow();

        Notification updatedUnread2 =
                notificationRepository.findById(unread2.getId()).orElseThrow();

        Notification updatedOtherUser =
                notificationRepository.findById(otherUser.getId()).orElseThrow();

        assertThat(updatedUnread1.isRead()).isTrue();
        assertThat(updatedUnread2.isRead()).isTrue();

        // Other user's notification must remain unread
        assertThat(updatedOtherUser.isRead()).isFalse();
    }
}