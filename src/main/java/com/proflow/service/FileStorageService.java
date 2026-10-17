package com.proflow.service;

import com.proflow.config.ApplicationProperties;
import com.proflow.exception.BadRequestException;
import com.proflow.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final ApplicationProperties applicationProperties;
    private Path fileStorageLocation;

    private static final List<String> ALLOWED_PROJECT_MIME_TYPES = Arrays.asList(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain",
            "image/jpeg",
            "image/png",
            "image/gif"
    );

    @PostConstruct
    public void init() {
        this.fileStorageLocation = Paths.get(applicationProperties.getUpload().getDir())
                .toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            log.error("Could not create upload directory: {}", this.fileStorageLocation, ex);
        }
    }

    public StoredFile storeFile(MultipartFile file, boolean validateMimeType) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file uploaded or file is empty");
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        if (originalFilename.contains("..")) {
            throw new BadRequestException("Invalid filename sequence: " + originalFilename);
        }

        String contentType = file.getContentType();
        if (validateMimeType && contentType != null && !ALLOWED_PROJECT_MIME_TYPES.contains(contentType)) {
            throw new BadRequestException("Invalid file type. Only documents, images, and PDFs are allowed.");
        }

        String fileExtension = "";
        int dotIdx = originalFilename.lastIndexOf('.');
        if (dotIdx > 0) {
            fileExtension = originalFilename.substring(dotIdx);
        }

        String storedFilename = System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8) + fileExtension;
        Path targetLocation = this.fileStorageLocation.resolve(storedFilename);

        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return new StoredFile(originalFilename, storedFilename, targetLocation.toString(), contentType != null ? contentType : "application/octet-stream", file.getSize());
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + originalFilename + ". Please try again!", ex);
        }
    }

    public Resource loadFileAsResource(String filename) {
        try {
            Path filePath = this.fileStorageLocation.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found: " + filename);
            }
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("File not found: " + filename);
        }
    }

    public void deleteFile(String filename) {
        try {
            Path filePath = this.fileStorageLocation.resolve(filename).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            log.error("Failed to delete file {}: {}", filename, ex.getMessage());
        }
    }

    public record StoredFile(String originalFilename, String storedFilename, String path, String mimeType, long size) {}
}
