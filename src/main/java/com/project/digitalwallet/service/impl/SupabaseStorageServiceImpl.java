package com.project.digitalwallet.service.impl;

import com.project.digitalwallet.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
public class SupabaseStorageServiceImpl implements FileStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-key}")
    private String serviceKey;

    @Value("${supabase.bucket.kyc:kyc-documents}")
    private String kycBucket;

    private final RestTemplate restTemplate = new RestTemplate();

    public static final int EXPIRATION_5_MINUTES = 300;

    @Override
    public String storeFile(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty file.");
        }

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.jpg";
        String cleanFileName = originalName.replaceAll("\\s+", "_").replaceAll("[^a-zA-Z0-9._-]", "");
        String safeName = UUID.randomUUID() + "-" + cleanFileName;

        // Consistent folder path format: user_1/uuid-filename.ext
        String objectPath = folder + "/" + safeName;

        return uploadFile(file, kycBucket, objectPath);
    }

    private String uploadFile(MultipartFile file, String bucketName, String objectPath) {
        try {
            String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + objectPath;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + serviceKey);
            headers.set("apikey", serviceKey);

            String contentType = file.getContentType();
            headers.setContentType(contentType != null && !contentType.isBlank()
                    ? MediaType.parseMediaType(contentType)
                    : MediaType.IMAGE_JPEG);

            // Explicitly set disposition to inline so browsers render it instead of downloading
            headers.set("Content-Disposition", "inline; filename=\"" + objectPath + "\"");

            HttpEntity<byte[]> request = new HttpEntity<>(file.getBytes(), headers);
            ResponseEntity<String> response = restTemplate.exchange(uploadUrl, HttpMethod.POST, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Supabase upload failed: " + response.getStatusCode() + " - " + response.getBody());
            }

            return objectPath;

        } catch (Exception e) {
            log.error("Error uploading file to Supabase Storage: {}", e.getMessage(), e);
            throw new RuntimeException("Error uploading file to Supabase Storage: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a dynamic Supabase Signed URL.
     * Catches network or path resolution exceptions and returns null gracefully
     * to prevent entire API requests from crashing on missing/corrupted paths.
     */
    @Override
    public String createSignedUrl(String objectPath, int expiresInSeconds) {
        if (objectPath == null || objectPath.isBlank()) {
            return null;
        }

        try {
            String signUrl = supabaseUrl + "/storage/v1/object/sign/" + kycBucket + "/" + objectPath;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + serviceKey);
            headers.set("apikey", serviceKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String jsonBody = "{\"expiresIn\": " + expiresInSeconds + "}";
            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<SupabaseSignResponse> response = restTemplate.exchange(
                    signUrl, HttpMethod.POST, request, SupabaseSignResponse.class);

            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null
                    && response.getBody().signedURL() != null) {
                return supabaseUrl + "/storage/v1" + response.getBody().signedURL();
            }

            log.warn("Supabase returned non-OK or empty signed URL for path: {}", objectPath);
            return null;

        } catch (HttpStatusCodeException e) {
            log.warn("Failed to generate signed URL for path '{}'. HTTP Status: {} - Body: {}",
                    objectPath, e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("Unexpected error generating signed URL for object '{}': {}", objectPath, e.getMessage());
            return null;
        }
    }

    private record SupabaseSignResponse(String signedURL) {}
}