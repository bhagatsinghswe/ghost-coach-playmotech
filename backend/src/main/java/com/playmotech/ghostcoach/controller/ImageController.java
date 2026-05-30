package com.playmotech.ghostcoach.controller;

import com.playmotech.ghostcoach.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Path;

/**
 * Serves uploaded stance images.
 * Path pattern: /api/images/{userId}/{filename}
 * This matches the storagePath format stored in CoachingSession.
 */
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Slf4j
public class ImageController {

    private final FileStorageUtil fileStorage;

    @GetMapping("/{userId}/{filename}")
    public ResponseEntity<Resource> serveImage(
            @PathVariable String userId,
            @PathVariable String filename) throws IOException {

        // Prevent path traversal — only allow alphanumeric names + safe characters
        if (!userId.matches("\\d+") || !filename.matches("[a-zA-Z0-9\\-_.]+")) {
            return ResponseEntity.badRequest().build();
        }

        String storagePath = userId + "/" + filename;
        Path filePath = fileStorage.resolve(storagePath);
        Resource resource = new PathResource(filePath);

        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        String contentType = URLConnection.guessContentTypeFromName(filename);
        if (contentType == null) contentType = "image/jpeg";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
}
