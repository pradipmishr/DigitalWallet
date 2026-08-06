package com.project.digitalwallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GlobalLimitRequest {

    @NotNull(message = "Daily limit is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Daily limit must be greater than zero")
    private BigDecimal dailyLimit;
}