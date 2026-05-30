package com.playmotech.ghostcoach.util;

import com.playmotech.ghostcoach.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

/**
 * Saves uploaded images to the local filesystem and resolves their paths.
 * In production this would delegate to S3 / GCS; the interface stays the same.
 */
@Component
@Slf4j
public class FileStorageUtil {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_BYTES = 5 * 1024 * 1024L; // 5 MB

    private final Path uploadRoot;

    public FileStorageUtil(@Value("${app.upload.dir}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadRoot);
            log.info("Upload directory: {}", uploadRoot);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + uploadRoot, e);
        }
    }

    /**
     * Validates and persists an uploaded file.
     *
     * @return relative storage path (e.g. "2024/abc123.jpg")
     */
    public String store(MultipartFile file, Long userId) throws IOException {
        validateFile(file);

        String extension = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + "." + extension;
        // Bucket by userId to keep directories manageable
        Path userDir = uploadRoot.resolve(String.valueOf(userId));
        Files.createDirectories(userDir);

        Path target = userDir.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        log.debug("Stored upload for user {}: {}", userId, target);

        // Return relative path stored in DB
        return userId + "/" + filename;
    }

    /** Resolve a relative storage path to an absolute filesystem path. */
    public Path resolve(String storagePath) {
        return uploadRoot.resolve(storagePath).normalize();
    }

    /**
     * Read the file bytes for AI processing.
     * Throws if the file has gone missing (defensive).
     */
    public byte[] readBytes(String storagePath) throws IOException {
        Path path = resolve(storagePath);
        if (!Files.exists(path)) {
            throw new BadRequestException("Image file not found: " + storagePath);
        }
        return Files.readAllBytes(path);
    }

    public void delete(String storagePath) {
        try {
            Files.deleteIfExists(resolve(storagePath));
        } catch (IOException e) {
            log.warn("Could not delete file {}: {}", storagePath, e.getMessage());
        }
    }

    // ---- private helpers ----

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file provided");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Only JPEG, PNG, and WebP images are accepted");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("File size exceeds the 5 MB limit");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
