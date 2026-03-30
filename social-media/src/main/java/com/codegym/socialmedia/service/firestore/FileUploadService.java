package com.codegym.socialmedia.service.firestore;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * File Upload Service
 *
 * Handles file uploads to local backend storage.
 * - Validates file size and type
 * - Renames files with UUID
 * - Stores in /uploads directory
 * - Returns public file URLs
 */
@Slf4j
@Service
public class FileUploadService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.base-url:http://localhost:8080/api/uploads}")
    private String baseUrl;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_IMAGE_TYPES = new HashSet<>(
        Arrays.asList("image/jpeg", "image/png", "image/gif", "image/webp")
    );
    private static final Set<String> ALLOWED_DOCUMENT_TYPES = new HashSet<>(
        Arrays.asList(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    );

    /**
     * Upload file to local storage
     *
     * @param file MultipartFile from request
     * @param userId User ID uploading the file (for organization)
     * @param allowedType Type of file: 'image' or 'document'
     * @return FileUploadResult with fileUrl, fileName, fileType
     * @throws IllegalArgumentException if file validation fails
     */
    public FileUploadResult uploadFile(MultipartFile file, Long userId, String allowedType) {
        try {
            // Validate file
            validateFile(file, allowedType);

            // Create upload directory if not exists
            File uploadDirectory = new File(uploadDir);
            if (!uploadDirectory.exists()) {
                boolean created = uploadDirectory.mkdirs();
                if (!created) {
                    throw new IOException("Failed to create upload directory: " + uploadDir);
                }
                log.info("✅ Upload directory created: {}", uploadDir);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID() + "_" + fileExtension;

            // Save file
            Path uploadPath = Paths.get(uploadDir, uniqueFilename);
            file.transferTo(uploadPath.toFile());

            log.info("✅ File uploaded: {} → {}", originalFilename, uniqueFilename);

            // Build response
            String fileUrl = baseUrl + "/" + uniqueFilename;
            String fileType = detectFileType(file.getContentType());

            return FileUploadResult.builder()
                .fileUrl(fileUrl)
                .fileName(originalFilename)
                .fileType(fileType)
                .fileSize(file.getSize())
                .uploadedAt(System.currentTimeMillis())
                .build();

        } catch (IllegalArgumentException e) {
            log.warn("❌ File validation failed: {}", e.getMessage());
            throw e;
        } catch (IOException e) {
            log.error("❌ File upload error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    /**
     * Upload image file
     *
     * @param file Image file
     * @param userId User uploading
     * @return FileUploadResult
     */
    public FileUploadResult uploadImage(MultipartFile file, Long userId) {
        return uploadFile(file, userId, "image");
    }

    /**
     * Upload document file
     *
     * @param file Document file
     * @param userId User uploading
     * @return FileUploadResult
     */
    public FileUploadResult uploadDocument(MultipartFile file, Long userId) {
        return uploadFile(file, userId, "document");
    }

    /**
     * Validate uploaded file
     *
     * @param file File to validate
     * @param allowedType Type of file expected: 'image' or 'document'
     * @throws IllegalArgumentException if validation fails
     */
    private void validateFile(MultipartFile file, String allowedType) {
        // Check if file is empty
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                "File size exceeds limit. Max: " + formatBytes(MAX_FILE_SIZE) +
                ", Got: " + formatBytes(file.getSize())
            );
        }

        // Get MIME type
        String contentType = file.getContentType();
        if (contentType == null || contentType.isEmpty()) {
            throw new IllegalArgumentException("File MIME type cannot be determined");
        }

        // Validate based on type
        if ("image".equalsIgnoreCase(allowedType)) {
            if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
                throw new IllegalArgumentException(
                    "Invalid image type. Allowed: " + ALLOWED_IMAGE_TYPES +
                    ", Got: " + contentType
                );
            }
        } else if ("document".equalsIgnoreCase(allowedType)) {
            if (!ALLOWED_DOCUMENT_TYPES.contains(contentType)) {
                throw new IllegalArgumentException(
                    "Invalid document type. Allowed: " + ALLOWED_DOCUMENT_TYPES +
                    ", Got: " + contentType
                );
            }
        }

        log.info("✅ File validation passed: {}, type: {}, size: {}",
            file.getOriginalFilename(), contentType, formatBytes(file.getSize()));
    }

    /**
     * Detect file type from MIME type
     *
     * @param contentType MIME type
     * @return File type: 'IMAGE', 'DOCUMENT', or 'UNKNOWN'
     */
    private String detectFileType(String contentType) {
        if (contentType == null) return "UNKNOWN";
        if (ALLOWED_IMAGE_TYPES.contains(contentType)) return "IMAGE";
        if (ALLOWED_DOCUMENT_TYPES.contains(contentType)) return "DOCUMENT";
        return "UNKNOWN";
    }

    /**
     * Get file extension from filename
     *
     * @param filename Original filename
     * @return Extension with dot (e.g., ".jpg")
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) return "";
        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex > 0) {
            return filename.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }

    /**
     * Format bytes to human-readable size
     *
     * @param bytes Size in bytes
     * @return Formatted string (e.g., "5.2 MB")
     */
    private String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format("%.1f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    /**
     * Delete file from storage
     *
     * @param filename Filename to delete
     * @return true if deleted successfully
     */
    public boolean deleteFile(String filename) {
        try {
            Path filePath = Paths.get(uploadDir, filename);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("⚠️ Failed to delete file {}: {}", filename, e.getMessage());
            return false;
        }
    }

    /**
     * File Upload Result DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class FileUploadResult {
        private String fileUrl;       // Public URL to access file
        private String fileName;      // Original filename
        private String fileType;      // IMAGE, DOCUMENT, etc.
        private long fileSize;        // Size in bytes
        private long uploadedAt;      // Timestamp
    }
}
