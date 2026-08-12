package com.project.digitalwallet.service;

import com.project.digitalwallet.dto.QrCodeResponse;

public interface QrCodeService {
    QrCodeResponse generateAndSaveStaticQr(Long userId);
}