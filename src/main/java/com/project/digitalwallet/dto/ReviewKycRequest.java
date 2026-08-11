package com.project.digitalwallet.dto;

import com.project.digitalwallet.common.enums.KycStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewKycRequest {

    @NotNull(message = "Status is required (VERIFIED or REJECTED)")
    private KycStatus status;

    private String adminRemarks;
}