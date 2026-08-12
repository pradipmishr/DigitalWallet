package com.project.digitalwallet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ScanQrRequest {
    @NotBlank(message = "QR content string is required")
    private String qrContent;
}