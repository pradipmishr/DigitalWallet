package com.project.digitalwallet.mapper;

import com.project.digitalwallet.dto.KycStatusResponse;
import com.project.digitalwallet.entity.KycDetails;
import com.project.digitalwallet.service.FileStorageService;
import com.project.digitalwallet.service.impl.SupabaseStorageServiceImpl;

public class KycMapper {

    public static KycStatusResponse mapToKycStatusResponse(KycDetails kyc, FileStorageService storageService) {
        if (kyc == null) {
            return null;
        }

        String signedUrl = null;
        if (kyc.getFrontImagePath() != null && storageService != null) {
            signedUrl = storageService.createSignedUrl(
                    kyc.getFrontImagePath(),
                    SupabaseStorageServiceImpl.EXPIRATION_5_MINUTES
            );
        }

        return KycStatusResponse.builder()
                .id(kyc.getId())
                .status(kyc.getStatus())
                .documentType(kyc.getDocumentType())
                .documentNumber(kyc.getDocumentNumber())
                .issueDate(kyc.getIssueDate())
                .dateOfBirth(kyc.getDateOfBirth())
                .frontImageUrl(signedUrl)
                .adminRemarks(kyc.getAdminRemarks())
                .verifiedAt(kyc.getVerifiedAt())
                .build();
    }
}