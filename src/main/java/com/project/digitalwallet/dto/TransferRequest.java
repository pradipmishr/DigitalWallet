package com.project.digitalwallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {

    @NotBlank(message = "Recipient phone number is required")
    private String recipientPhoneNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Minimum transfer amount is 1.00")
    private BigDecimal amount;

    @NotBlank(message = "Transaction PIN is required")
    @Pattern(regexp = "^\\d{4}$", message = "Transaction PIN must be exactly 4 digits")
    private String pin;

    private String description;
}