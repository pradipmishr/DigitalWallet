package com.project.digitalwallet.dto;

import com.project.digitalwallet.common.enums.ScheduleFrequency;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateScheduledPaymentRequest {

    @NotBlank(message = "Recipient phone number is required")
    private String recipientPhoneNumber;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Frequency is required")
    private ScheduleFrequency frequency;

    @NotNull(message = "First run date is required")
    @Future(message = "Start date must be in the future")
    private LocalDateTime startAt;

    private Integer totalOccurrences; // Optional

    private String description;

    @NotBlank(message = "Transaction PIN is required")
    @Pattern(regexp = "^\\d{4}$", message = "Transaction PIN must be exactly 4 digits")
    private String pin;
}
