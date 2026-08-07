package com.project.digitalwallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordWithTokenRequest {

    @NotBlank(message = "Reset token is required")
    private String resetToken;

    @NotBlank(message = "New password is required")
    private String newPassword;
}