package com.project.digitalwallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ResetPinWithTokenRequest {

    @NotBlank(message = "Reset token is required")
    private String resetToken;

    @NotBlank(message = "New PIN is required")
    @Pattern(regexp = "^[0-9]{4}$", message = "Transaction PIN must be 4 digits")
    private String newPin;
}