package com.project.digitalwallet.controller;

import com.project.digitalwallet.common.util.ResponseWrapper;
import com.project.digitalwallet.dto.QrCodeResponse;
import com.project.digitalwallet.security.UserPrincipal;
import com.project.digitalwallet.service.QrCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/qr")
@RequiredArgsConstructor
public class QrCodeController {

    private final QrCodeService qrCodeService;

    @PostMapping
    public ResponseEntity<ResponseWrapper<QrCodeResponse>> generateStaticQr(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        QrCodeResponse response = qrCodeService.generateAndSaveStaticQr(currentUser.getUser().getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseWrapper<>(response, "Static QR code generated and saved successfully.", HttpStatus.CREATED.value(), true));
    }
}