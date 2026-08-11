package com.project.digitalwallet.dto;

import com.project.digitalwallet.common.enums.RequestMoneyStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class RequestMoneyDto {
    private Long id;
    private Long requesterId;
    private String requesterName;
    private String requesterPhoneNumber;
    private Long payerId;
    private String payerName;
    private String payerPhoneNumber;
    private BigDecimal amount;
    private String description;
    private RequestMoneyStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}