package com.codewithpcodes.glimserver.storage;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
@Tag(name = "Storage Management", description = "Storage Management Endpoints")
public class StorageController {

    private final StorageService storageService;

    public record UploadUrlRequest(
            @NotNull FileCategory category,
            @NotBlank String filename,
            @NotBlank String contentType,
            @Positive long size
    ) {}

    /** Step 1 — ask for a URL. */
    @PostMapping("/upload-url")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTOR', 'FINANCE', 'DEPARTMENT_HEAD', 'MEDIA')")
    public ResponseEntity<PresignedUpload> createUploadUrl(@RequestBody @Valid UploadUrlRequest req) {
        return ResponseEntity.ok(storageService.createUploadUrl(
                req.category(), req.filename(), req.contentType(), req.size()));
    }

    /** Step 3 — tell the server the upload landed. */
    @PostMapping("/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTOR', 'FINANCE', 'DEPARTMENT_HEAD', 'MEDIA')")
    public ResponseEntity<StoredObject> confirm(@RequestParam @NotBlank String key) {
        return ResponseEntity.ok(storageService.confirmUpload(key));
    }

    /** Direct upload — avatars and other small files only. */
    @PostMapping(value = "/avatar", consumes = "multipart/form-data")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StoredObject> uploadAvatar(@RequestParam MultipartFile file) throws IOException {
        StoredObject stored = storageService.upload(
                FileCategory.AVATAR,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getInputStream());
        return ResponseEntity.ok(stored);
    }
}
