package com.project.digitalwallet.dto;

import com.project.digitalwallet.common.enums.DocumentType;
import com.project.digitalwallet.common.enums.KycStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class KycStatusResponse {
    private Long id;
    private KycStatus status;
    private String name;
    private DocumentType documentType;
    private String documentNumber;
    private LocalDate issueDate;
    private LocalDate dateOfBirth;
    private String frontImageUrl;
    private String adminRemarks;
    private String street;
    private String zipCode;
    private String state;
    private String city;
    private LocalDate verifiedAt;
}