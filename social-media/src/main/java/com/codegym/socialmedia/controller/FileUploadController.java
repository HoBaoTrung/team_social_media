package com.codegym.socialmedia.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * File Upload Serving Endpoint
 *
 * Serves uploaded files from /uploads directory
 * Public endpoint for downloading/viewing files
 */
@Slf4j
@Controller
@RequestMapping("/api/uploads")
public class FileUploadController {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /**
     * Serve uploaded file
     *
     * Endpoint: GET /api/uploads/{filename}
     * Returns the file with appropriate Content-Type
     *
     * @param filename UUID-based filename stored in uploads folder
     * @return File content with proper headers
     */
    @GetMapping("/{filename}")
    public ResponseEntity<byte[]> serveFile(@PathVariable String filename) {

        try {
            // Security: Prevent path traversal attacks
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                log.warn("❌ Suspicious filename requested: {}", filename);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Path filePath = Paths.get(uploadDir, filename);
            File file = filePath.toFile();

            // Check if file exists
            if (!file.exists()) {
                log.warn("❌ File not found: {}", filename);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // Read file bytes
            byte[] fileContent = Files.readAllBytes(filePath);

            // Detect MIME type
            String mediaType = detectMediaType(filename);

            // Build response with appropriate headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(mediaType));
            headers.setContentLength(fileContent.length);
            headers.setContentDispositionFormData("inline", filename);

            log.info("✅ File served: {}", filename);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileContent);

        } catch (IOException e) {
            log.error("❌ Error reading file {}: {}", filename, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Detect MIME type from filename
     *
     * @param filename Filename with extension
     * @return MIME type string
     */
    private String detectMediaType(String filename) {
        String extension = getFileExtension(filename).toLowerCase();

        return switch (extension) {
            // Images
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";

            // Documents
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "txt" -> "text/plain";

            // Videos
            case "mp4" -> "video/mp4";
            case "mov" -> "video/quicktime";
            case "avi" -> "video/x-msvideo";
            case "webm" -> "video/webm";

            // Audio
            case "mp3" -> "audio/mpeg";
            case "wav" -> "audio/wav";
            case "m4a" -> "audio/mp4";

            // Default
            default -> "application/octet-stream";
        };
    }

    /**
     * Get file extension from filename
     *
     * @param filename Filename
     * @return Extension without dot (e.g., "jpg")
     */
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf(".");
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }
}
