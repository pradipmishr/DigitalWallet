package com.project.digitalwallet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VerifyResetOtpResponse {
    private String resetToken;
}