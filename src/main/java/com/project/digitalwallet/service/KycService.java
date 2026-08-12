package com.project.digitalwallet.service;

import com.project.digitalwallet.common.enums.KycStatus;
import com.project.digitalwallet.dto.KycStatusResponse;
import com.project.digitalwallet.dto.ReviewKycRequest;
import com.project.digitalwallet.dto.SubmitKycRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface KycService {

    // User submission & status retrieval
    KycStatusResponse submitKyc(Long userId, SubmitKycRequest request, MultipartFile frontImage);
    KycStatusResponse getKycStatusForUser(Long authenticatedUserId);

    // Legacy/Internal method (can delegate to getKycStatusForUser or be kept for backward compatibility)
    KycStatusResponse getKycStatus(Long userId);

    // Admin endpoints
    KycStatusResponse reviewKyc(Long kycId, ReviewKycRequest request);
    Page<KycStatusResponse> getAllKycs(KycStatus status, Pageable pageable);
    KycStatusResponse getKycById(Long id);
    KycStatusResponse getKycByUserIdForAdmin(Long targetUserId);
}