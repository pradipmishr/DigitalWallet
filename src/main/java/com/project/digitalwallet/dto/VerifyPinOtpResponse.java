package com.project.digitalwallet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VerifyPinOtpResponse {
    private String resetToken;
}