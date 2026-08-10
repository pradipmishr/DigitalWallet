package com.project.digitalwallet.entity;

import com.project.digitalwallet.common.enums.ScheduleFrequency;
import com.project.digitalwallet.common.enums.ScheduledPaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "scheduled_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduledPayment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String recipientPhoneNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleFrequency frequency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduledPaymentStatus status;

    @Column(nullable = false)
    private LocalDateTime nextRunAt;

    private LocalDateTime lastRunAt;

    private Integer totalOccurrences;

    @Builder.Default
    private Integer completedOccurrences = 0;

    @Builder.Default
    private Integer failedAttempts = 0;

    private String description;

    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = ScheduledPaymentStatus.ACTIVE;
        }
    }
}