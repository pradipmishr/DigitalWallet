package com.project.digitalwallet.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScanQrResponse {
    private Long recipientUserId;
    private String recipientName;
    private String recipientPhoneNumber;
    private String type;
}