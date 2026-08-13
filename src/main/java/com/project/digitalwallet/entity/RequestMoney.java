package com.project.digitalwallet.entity;

import com.project.digitalwallet.common.enums.RequestMoneyStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "request_money")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestMoney extends BaseEntity{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester; // User A

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id", nullable = false)
    private User payer; // User B

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestMoneyStatus status;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime completedAt;


}