package com.project.digitalwallet.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String storeFile(MultipartFile file, String directoryName);
    String createSignedUrl(String objectPath, int expiresInSeconds);
}