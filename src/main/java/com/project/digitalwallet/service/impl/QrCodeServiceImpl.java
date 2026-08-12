package com.project.digitalwallet.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.project.digitalwallet.dto.QrCodeResponse;
import com.project.digitalwallet.entity.User;
import com.project.digitalwallet.repository.UserRepository;
import com.project.digitalwallet.service.QrCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QrCodeServiceImpl implements QrCodeService {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Folder location on server/disk
    private static final String QR_CODE_DIR = "qr-codes/";

    @Override
    public QrCodeResponse generateAndSaveStaticQr(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        try {
            // 1. Create QR payload JSON structure
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("userId", user.getId());
            payloadMap.put("name", user.getFirstName() + " " + user.getLastName());
            payloadMap.put("phoneNumber", user.getPhoneNumber());
            payloadMap.put("type", "STATIC_WALLET_QR");

            String qrContent = objectMapper.writeValueAsString(payloadMap);

            // 2. Ensure destination directory exists
            Path dirPath = Paths.get(QR_CODE_DIR);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            // 3. Define filename and target path
            String fileName = "qr_user_" + user.getId() + "_" + user.getPhoneNumber() + ".png";
            Path targetPath = FileSystems.getDefault().getPath(QR_CODE_DIR + fileName);

            // 4. Configure ZXing parameters (300x300 px, margin, error correction)
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 1);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, 300, 300, hints);

            // 5. Write PNG to filesystem
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", targetPath);

            return QrCodeResponse.builder()
                    .fileName(fileName)
                    .filePath(targetPath.toAbsolutePath().toString())
                    .qrContent(qrContent)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate and save QR code for user ID: " + userId, e);
        }
    }
}