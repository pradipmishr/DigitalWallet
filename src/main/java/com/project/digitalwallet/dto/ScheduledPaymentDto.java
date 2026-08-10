package com.project.digitalwallet.dto;

import com.project.digitalwallet.common.enums.ScheduleFrequency;
import com.project.digitalwallet.common.enums.ScheduledPaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ScheduledPaymentDto {
    private Long id;
    private String recipientPhoneNumber;
    private BigDecimal amount;
    private ScheduleFrequency frequency;
    private ScheduledPaymentStatus status;
    private LocalDateTime nextRunAt;
    private LocalDateTime lastRunAt;
    private Integer completedOccurrences;
    private Integer totalOccurrences;
    private String description;
}
