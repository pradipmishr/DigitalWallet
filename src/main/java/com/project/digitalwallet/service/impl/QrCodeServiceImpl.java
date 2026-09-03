package com.project.digitalwallet.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.project.digitalwallet.dto.QrCodeResponse;
import com.project.digitalwallet.dto.ScanQrRequest;
import com.project.digitalwallet.dto.ScanQrResponse;
import com.project.digitalwallet.entity.User;
import com.project.digitalwallet.repository.UserRepository;
import com.project.digitalwallet.service.QrCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QrCodeServiceImpl implements QrCodeService {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public QrCodeResponse generateStaticQr(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found with ID: " + userId
                        )
                );

        try {
            // 1. Create QR payload
            Map<String, Object> payloadMap = new HashMap<>();

            payloadMap.put("userId", user.getId());
            payloadMap.put(
                    "name",
                    user.getFirstName() + " " + user.getLastName()
            );
            payloadMap.put("phoneNumber", user.getPhoneNumber());
            payloadMap.put("type", "STATIC_WALLET_QR");

            // Convert payload to JSON
            String qrContent = objectMapper.writeValueAsString(payloadMap);

            // 2. Configure QR code
            Map<EncodeHintType, Object> hints = new HashMap<>();

            hints.put(
                    EncodeHintType.ERROR_CORRECTION,
                    ErrorCorrectionLevel.H
            );

            hints.put(EncodeHintType.MARGIN, 1);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            // 3. Generate QR code in memory
            QRCodeWriter qrCodeWriter = new QRCodeWriter();

            BitMatrix bitMatrix = qrCodeWriter.encode(
                    qrContent,
                    BarcodeFormat.QR_CODE,
                    300,
                    300,
                    hints
            );

            // 4. Convert QR image to PNG bytes in memory
            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            MatrixToImageWriter.writeToStream(
                    bitMatrix,
                    "PNG",
                    outputStream
            );

            // 5. Convert PNG bytes to Base64
            String imageBase64 = Base64.getEncoder()
                    .encodeToString(outputStream.toByteArray());

            // 6. Return QR content + image
            return QrCodeResponse.builder()
                    .qrContent(qrContent)
                    .imageBase64(imageBase64)
                    .build();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to generate QR code for user ID: " + userId,
                    e
            );
        }
    }

    @Override
    public ScanQrResponse parseAndValidateQr(ScanQrRequest request) {

        try {
            // Parse JSON payload from scanned QR string
            Map<String, Object> payload =
                    objectMapper.readValue(
                            request.getQrContent(),
                            Map.class
                    );

            // Validate QR type
            String type = (String) payload.get("type");

            if (!"STATIC_WALLET_QR".equals(type)) {
                throw new IllegalArgumentException(
                        "Invalid QR code type."
                );
            }

            // Get phone number from QR payload
            String phoneNumber =
                    (String) payload.get("phoneNumber");

            if (phoneNumber == null || phoneNumber.isBlank()) {
                throw new IllegalArgumentException(
                        "QR code does not contain a valid phone number."
                );
            }

            // Validate recipient exists
            User recipient =
                    userRepository.findByPhoneNumber(phoneNumber)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Target user associated with this QR code does not exist."
                                    )
                            );

            // Return recipient information
            return ScanQrResponse.builder()
                    .recipientUserId(recipient.getId())
                    .recipientName(
                            recipient.getFirstName()
                                    + " "
                                    + recipient.getLastName()
                    )
                    .recipientPhoneNumber(
                            recipient.getPhoneNumber()
                    )
                    .type(type)
                    .build();

        } catch (IllegalArgumentException e) {

            throw e;

        } catch (Exception e) {

            throw new IllegalArgumentException(
                    "Failed to decode QR code. Invalid format or payload.",
                    e
            );
        }
    }
}
