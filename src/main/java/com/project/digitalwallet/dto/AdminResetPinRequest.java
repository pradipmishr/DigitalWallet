package com.project.digitalwallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminResetPinRequest {

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^\\+?[0-9]{10,15}$",
            message = "Invalid phone number format"
    )
    private String phoneNumber;

    @NotBlank(message = "New PIN is required")
    @Size(min = 4, max = 6, message = "PIN must be between 4 and 6 digits")
    private String newPin;
}