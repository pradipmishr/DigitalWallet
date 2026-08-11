package com.project.digitalwallet.service.impl;

import com.project.digitalwallet.common.enums.KycStatus;
import com.project.digitalwallet.common.enums.NotificationType;
import com.project.digitalwallet.common.exception.ResourceNotFoundException;
import com.project.digitalwallet.common.util.WalletTransactionEvent;
import com.project.digitalwallet.dto.KycStatusResponse;
import com.project.digitalwallet.dto.ReviewKycRequest;
import com.project.digitalwallet.dto.SubmitKycRequest;
import com.project.digitalwallet.entity.KycDetails;
import com.project.digitalwallet.entity.User;
import com.project.digitalwallet.mapper.KycMapper;
import com.project.digitalwallet.repository.KycDetailsRepository;
import com.project.digitalwallet.repository.UserRepository;
import com.project.digitalwallet.service.AuditLogService;
import com.project.digitalwallet.service.FileStorageService;
import com.project.digitalwallet.service.KycService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

import static com.project.digitalwallet.mapper.KycMapper.mapToKycStatusResponse;
import static com.project.digitalwallet.mapper.KycMapper.mapToResponse;

@Service
@RequiredArgsConstructor
public class KycServiceImpl implements KycService {

    private final KycDetailsRepository kycDetailsRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final AuditLogService auditLogService;
    private final HttpServletRequest httpServletRequest;
    private final ApplicationEventPublisher eventPublisher;



    @Override
    @Transactional
    public KycStatusResponse submitKyc(Long userId, SubmitKycRequest request, MultipartFile frontImage) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        if (frontImage == null || frontImage.isEmpty()) {
            throw new IllegalArgumentException("Front image of document is required.");
        }

        KycDetails kycDetails = kycDetailsRepository.findByUserId(userId)
                .orElse(KycDetails.builder().user(user).build());

        if (kycDetails.getStatus() == KycStatus.VERIFIED) {
            throw new IllegalStateException("Your account is already verified.");
        }

        // Store file uploads on disk under "kyc-documents"
        String frontPath = fileStorageService.storeFile(frontImage, "kyc-documents");

        kycDetails.setDocumentType(request.getDocumentType());
        kycDetails.setDocumentNumber(request.getDocumentNumber());
        kycDetails.setIssueDate(request.getIssueDate());
        kycDetails.setDateOfBirth(request.getDateOfBirth());
        kycDetails.setFrontImagePath(frontPath);
        kycDetails.setStatus(KycStatus.PENDING);
        kycDetails.setAdminRemarks(null);

        KycDetails saved = kycDetailsRepository.save(kycDetails);
        auditLogService.logEvent(
                user.getId(),
                "KYC_SUBMITTED",
                String.format("KYC submitted for review. Type: %s, Number: %s",
                        request.getDocumentType(), request.getDocumentNumber()),
                httpServletRequest
        );
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public KycStatusResponse getKycStatus(Long userId) {
        return kycDetailsRepository.findByUserId(userId)
                .map(KycMapper::mapToResponse)
                .orElseGet(() -> KycStatusResponse.builder()
                        .status(KycStatus.UNVERIFIED)
                        .build());
    }

    @Override
    @Transactional
    public KycStatusResponse reviewKyc(Long kycId, ReviewKycRequest request) {
        KycDetails kyc = kycDetailsRepository.findById(kycId)
                .orElseThrow(() -> new IllegalArgumentException("KYC record not found with ID: " + kycId));

        if (request.getStatus() != KycStatus.VERIFIED && request.getStatus() != KycStatus.REJECTED) {
            throw new IllegalArgumentException("Review status must be either VERIFIED or REJECTED.");
        }

        boolean isVerified = request.getStatus() == KycStatus.VERIFIED;

        kyc.setStatus(request.getStatus());
        kyc.setAdminRemarks(request.getAdminRemarks());

        if (isVerified) {
            kyc.setVerifiedAt(LocalDate.now());
        }

        KycDetails saved = kycDetailsRepository.save(kyc);

        // 1. Audit Logging
        String eventType = isVerified ? "KYC_VERIFIED" : "KYC_REJECTED";
        String description = String.format("KYC (ID: %d) status updated to %s. Admin Remarks: %s",
                kycId,
                request.getStatus(),
                request.getAdminRemarks() != null ? request.getAdminRemarks() : "None");

        auditLogService.logEvent(
                kyc.getUser().getId(),
                eventType,
                description,
                httpServletRequest
        );

        // 2. Publish Notification Event
        if (kyc.getUser() != null) {
            NotificationType notificationType = isVerified
                    ? NotificationType.KYC_VERIFIED
                    : NotificationType.KYC_REJECTED;

            String notificationMessage = isVerified
                    ? "Your KYC verification has been approved successfully."
                    : String.format("Your KYC verification was rejected. Reason: %s",
                    request.getAdminRemarks() != null ? request.getAdminRemarks() : "Please resubmit with valid documents.");

            eventPublisher.publishEvent(new WalletTransactionEvent(
                    kyc.getUser().getId(),
                    kyc.getUser().getPhoneNumber(),
                    notificationType,
                    null,
                    "NPR",
                    "KYC-" + kyc.getId(),
                    notificationMessage
            ));
        }

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<KycStatusResponse> getAllKycs(KycStatus status, Pageable pageable) {
        Page<KycDetails> kycPage;

        if (status != null) {
            kycPage = kycDetailsRepository.findByStatus(status, pageable);
        } else {
            kycPage = kycDetailsRepository.findAll(pageable);
        }

        return kycPage.map(KycMapper::mapToKycStatusResponse);
    }
    @Override
    @Transactional(readOnly = true)
    public KycStatusResponse getKycById(Long id) {
        KycDetails kyc = kycDetailsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("KYC record not found with id: " + id));

        return mapToKycStatusResponse(kyc);
    }
}