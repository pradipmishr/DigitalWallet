package com.project.digitalwallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminReverseTransactionRequest {

    @NotBlank(message = "Original transaction reference number is required")
    private String referenceNumber;

    @NotBlank(message = "Reversal reason is required")
    private String reason;
}