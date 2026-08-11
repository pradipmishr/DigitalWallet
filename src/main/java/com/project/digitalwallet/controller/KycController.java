package com.project.digitalwallet.controller;

import com.project.digitalwallet.common.util.ResponseWrapper;
import com.project.digitalwallet.dto.KycStatusResponse;
import com.project.digitalwallet.dto.SubmitKycRequest;
import com.project.digitalwallet.security.UserPrincipal;
import com.project.digitalwallet.service.KycService;
import com.project.digitalwallet.service.impl.OcrExtractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/kyc")
@PreAuthorize("hasRole('USER')")
@RequiredArgsConstructor
public class KycController {

    private final KycService kycService;
    //private final FileStorageService fileStorageService;
    private final OcrExtractionService ocrExtractionService;


    @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseWrapper<KycStatusResponse>> submitKyc(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @ModelAttribute SubmitKycRequest request,
            @RequestPart("frontImage") MultipartFile frontImage) {

        KycStatusResponse response = kycService.submitKyc(
                userPrincipal.getUser().getId(),
                request,
                frontImage
        );

        return ResponseEntity.ok(new ResponseWrapper<>(
                response,
                "KYC documents and application submitted successfully.",
                HttpStatus.OK.value(),
                true
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<ResponseWrapper<KycStatusResponse>> getKycStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        KycStatusResponse response = kycService.getKycStatus(userPrincipal.getUser().getId());

        return ResponseEntity.ok(new ResponseWrapper<>(
                response,
                "KYC status retrieved successfully.",
                HttpStatus.OK.value(),
                true
        ));
    }

    @PostMapping(value = "/scan-document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseWrapper<SubmitKycRequest>> scanDocument(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestPart("frontImage") MultipartFile frontImage) {

        SubmitKycRequest extractedFields = ocrExtractionService.extractKycDetails(frontImage);

        return ResponseEntity.ok(new ResponseWrapper<>(
                extractedFields,
                "Document scanned successfully. Please verify extracted details.",
                HttpStatus.OK.value(),
                true
        ));
    }


    //This should also be checked!
//    @GetMapping("/files/{dir}/{fileName:.+}")
//    public ResponseEntity<Resource> getKycFile(
//            @PathVariable String dir,
//            @PathVariable String fileName) {
//
//        Resource resource = fileStorageService.loadFileAsResource(dir + "/" + fileName);
//
//        return ResponseEntity.ok()
//                .contentType(MediaType.IMAGE_JPEG)
//                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
//                .body(resource);
//    }
}