package com.project.digitalwallet.mapper;

import com.project.digitalwallet.dto.KycStatusResponse;
import com.project.digitalwallet.entity.KycDetails;

public class KycMapper {
    public static KycStatusResponse mapToResponse(KycDetails entity) {
        return KycStatusResponse.builder()
                .id(entity.getId())
                .status(entity.getStatus())
                .documentType(entity.getDocumentType())
                .documentNumber(entity.getDocumentNumber())
                .issueDate(entity.getIssueDate())
                .dateOfBirth(entity.getDateOfBirth())
                .adminRemarks(entity.getAdminRemarks())
                .verifiedAt(entity.getVerifiedAt())
                .build();
    }
    public static KycStatusResponse mapToKycStatusResponse(KycDetails kyc) {
        return KycStatusResponse.builder()
                .id(kyc.getId())
                .status(kyc.getStatus())
                .documentType(kyc.getDocumentType())
                .documentNumber(kyc.getDocumentNumber())
                .issueDate(kyc.getIssueDate())
                .dateOfBirth(kyc.getDateOfBirth())
                .frontImageUrl(kyc.getFrontImagePath() != null ? "/api/kyc/files/" + kyc.getFrontImagePath() : null)
                .adminRemarks(kyc.getAdminRemarks())
                .verifiedAt(kyc.getVerifiedAt())
                .build();
    }
}
