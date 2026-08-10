package com.project.digitalwallet.mapper;

import com.project.digitalwallet.dto.ScheduledPaymentDto;
import com.project.digitalwallet.entity.ScheduledPayment;

public class ScheduleMapper {
    public static ScheduledPaymentDto mapToDto(ScheduledPayment entity) {
        return ScheduledPaymentDto.builder()
                .id(entity.getId())
                .recipientPhoneNumber(entity.getRecipientPhoneNumber())
                .amount(entity.getAmount())
                .frequency(entity.getFrequency())
                .status(entity.getStatus())
                .nextRunAt(entity.getNextRunAt())
                .lastRunAt(entity.getLastRunAt())
                .completedOccurrences(entity.getCompletedOccurrences())
                .totalOccurrences(entity.getTotalOccurrences())
                .description(entity.getDescription())
                .build();
    }
}
