package com.project.digitalwallet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AcceptMoneyRequest {

    @NotBlank(message = "Transaction PIN is required")
    private String pin;
}