package com.project.digitalwallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPinRequest {

    @NotBlank(message = "OTP code is required")
    @Size(min = 6, max = 6, message = "OTP must be 6 digits")
    private String otp;

    @NotBlank(message = "New PIN is required")
    @Pattern(regexp = "^[0-9]{4}$", message = "Transaction PIN must be 4 digits")
    private String newPin;
}