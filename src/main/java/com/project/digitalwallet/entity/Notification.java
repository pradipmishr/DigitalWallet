package com.project.digitalwallet.entity;



import com.project.digitalwallet.common.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    private String referenceId;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean read = false;
}