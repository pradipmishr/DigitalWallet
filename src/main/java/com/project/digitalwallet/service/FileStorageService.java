package com.project.digitalwallet.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String storeFile(MultipartFile file, String directoryName);
    Resource loadFileAsResource(String filePath);
}