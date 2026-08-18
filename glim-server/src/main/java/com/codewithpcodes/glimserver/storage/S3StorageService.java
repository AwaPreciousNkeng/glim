package com.codewithpcodes.glimserver.storage;

import com.codewithpcodes.glimserver.exceptions.InvalidFileException;
import com.codewithpcodes.glimserver.exceptions.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class S3StorageService implements StorageService {

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final StorageProperties props;
    private final Tika tika = new Tika();

    public static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM");

    private String buildKey(FileCategory category, String originalFileName) {
        String ext = extensionOf(originalFileName);
        return "%s/%s/%s%s".formatted(
                category.getPrefix(),
                LocalDate.now().format(DATE_PATH),
                UUID.randomUUID(),
                ext
        );
    }

    private String extensionOf(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf(".");
        if (dot < 0 || dot == filename.length() - 1) return "";
        String ext = filename.substring(dot).toLowerCase(Locale.ROOT);
        return ext.matches("\\.[a-z0-9]{1,8}") ? ext : "";
    }

    private void validate(FileCategory category, String contentType, long size) {
        if (size <= 0) {
            throw new InvalidFileException("File is empty.");
        }
        if (size > category.getMaxBytes()) {
            throw new InvalidFileException(
                    "File is too large. Maximum is %d MB."
                            .formatted(category.getMaxBytes() / (1024 * 1024))
            );
        }
        if (contentType == null
                ||!category.getAllowedContentTypes().contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new InvalidFileException("File type %s is not allowed here."
                    .formatted(contentType));
        }
    }

    private String detectContentType(InputStream markSupported, String fallback) {
        try {
            String detected = tika.detect(markSupported);
            return detected != null ? detected : fallback;
        } catch (IOException e) {
            log.warn("Content type detection failed, falling back to {}", fallback, e);
            return fallback;
        }
    }

    @Override
    public StoredObject upload(FileCategory category, String originalFilename, String contentType, long size, InputStream data) {

        validate(category, contentType, size);
        InputStream stream = data.markSupported() ? data : new BufferedInputStream(data);
        stream.mark(Integer.MAX_VALUE);
        String actualType = detectContentType(stream, contentType);
        try {
            stream.reset();
        } catch (IOException e) {
            throw new StorageException("Could not rewind upload stream.", e);
        }

        if (!category.getAllowedContentTypes().contains(actualType)) {
            throw new InvalidFileException("File contents do not match the declared type.");
        }

        String key = buildKey(category, originalFilename);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .contentType(actualType)
                .cacheControl(cacheControlFor(category))
                .build();

        try {
            PutObjectResponse response = s3Client.putObject(request, RequestBody.fromInputStream(stream, size));
            log.info("Uploaded {} ({} bytes) to {}", key, size, props.getBucket());
            return new StoredObject(key, resolveUrl(key, category), actualType, size, response.eTag());
        } catch (S3Exception e) {
            throw new StorageException("Upload failed for key " + key, e);
        }
    }

    private String cacheControlFor(FileCategory category) {
        return switch (category) {
            case AVATAR -> "public, max-age=86400";
            case PARTNER_IMPORT_CSV -> "private, no-store";
            default -> "public, max-age=31536000, immutable";
        };
    }


    @Override
    public PresignedUpload createUploadUrl(FileCategory category, String originalFilename, String contentType, long declaredSize) {
         validate(category, contentType, declaredSize);

         String key = buildKey(category, originalFilename);

         PutObjectRequest objectRequest = PutObjectRequest.builder()
                 .bucket(props.getBucket())
                 .key(key)
                 .contentType(contentType)
                 .cacheControl(cacheControlFor(category))
                 .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(props.getPresignUploadTtl())
                .putObjectRequest(objectRequest)
                .build();

        var presigned = presigner.presignPutObject(presignRequest);

        return new PresignedUpload(
                key,
                presigned.url().toString(),
                contentType,
                category.getMaxBytes(),
                Instant.now().plus(props.getPresignUploadTtl())
        );
    }

    @Override
    public StoredObject confirmUpload(String key) {
        try {
            HeadObjectResponse head = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(props.getBucket())
                    .key(key)
                    .build()
            );
            FileCategory category = categoryFromKey(key);

            if (head.contentLength() > category.getMaxBytes()) {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(props.getBucket()).key(key).build());
                throw new InvalidFileException("Uploaded file exceeded the size limit and was removed.");
            }

            return new StoredObject(
                    key,
                    resolveUrl(key, category),
                    head.contentType(),
                    head.contentLength(),
                    head.eTag());

        } catch (NoSuchKeyException e) {
            throw new StorageException("Upload was not completed. No object at " + key, e);
        }
    }

    private FileCategory categoryFromKey(String key) {
        for (FileCategory c : FileCategory.values()) {
            if (key.startsWith(c.getPrefix() + "/")) return c;
        }
        throw new InvalidFileException("Unrecognised storage key: " + key);
    }

    @Override
    public String resolveUrl(String key, FileCategory category) {
        if (category.getVisibility() == FileCategory.Visibility.PUBLIC) {
            return props.getPublicBaseUrl() + "/" + key;
        }
        return presignedDownloadUrl(key, props.getPresignDownloadTtl());
    }

    @Override
    public String presignedDownloadUrl(String key, Duration ttl) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(getRequest)
                .build();

        return presigner.presignGetObject(presignRequest).url().toString();
    }

    @Override
    public void delete(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(props.getBucket())
                    .key(key)
                    .build());
            log.info("Deleted {}", key);
        } catch (S3Exception e) {
            throw new StorageException("Delete failed for key " + key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(props.getBucket()).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }
}
