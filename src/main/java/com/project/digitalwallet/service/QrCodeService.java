package com.project.digitalwallet.service;

import com.project.digitalwallet.dto.QrCodeResponse;
import com.project.digitalwallet.dto.ScanQrRequest;
import com.project.digitalwallet.dto.ScanQrResponse;

public interface QrCodeService {

    QrCodeResponse generateStaticQr(Long userId);

    ScanQrResponse parseAndValidateQr(ScanQrRequest request);
}
