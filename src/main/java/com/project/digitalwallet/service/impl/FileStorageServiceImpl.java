package com.project.digitalwallet.service.impl;

import com.project.digitalwallet.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageServiceImpl(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create directory where uploaded files will be stored.", ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file, String directoryName) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store an empty file.");
        }

        // Validate basic file extension / type
        String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String fileExtension = "";
        int i = originalFileName.lastIndexOf('.');
        if (i > 0) {
            fileExtension = originalFileName.substring(i);
        }

        if (!isSupportedImageFormat(fileExtension)) {
            throw new IllegalArgumentException("Only JPG, JPEG, and PNG image files are allowed.");
        }

        try {
            Path targetDir = this.fileStorageLocation.resolve(directoryName);
            Files.createDirectories(targetDir);

            // Generate unique filename to prevent collisions
            String newFileName = UUID.randomUUID().toString() + fileExtension;
            Path targetLocation = targetDir.resolve(newFileName);

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Return relative path to store in database
            return directoryName + "/" + newFileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file. Please try again!", ex);
        }
    }

    @Override
    public Resource loadFileAsResource(String filePath) {
        try {
            Path file = this.fileStorageLocation.resolve(filePath).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new IllegalArgumentException("File not found: " + filePath);
            }
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException("File not found: " + filePath, ex);
        }
    }

    private boolean isSupportedImageFormat(String extension) {
        String ext = extension.toLowerCase();
        return ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png");
    }
}