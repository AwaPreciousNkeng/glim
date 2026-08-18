package com.codewithpcodes.glimserver.storage;

import java.io.InputStream;
import java.time.Duration;

public interface StorageService {

    /** Server-side upload. Use for small files and generated content. */
    StoredObject upload(FileCategory category, String originalFilename,
                        String contentType, long size, InputStream data);

    /** Presigned PUT. Use for anything large — the browser uploads directly. */
    PresignedUpload createUploadUrl(FileCategory category, String originalFilename,
                                    String contentType, long declaredSize);

    /** Confirms a presigned upload actually landed, and returns real metadata. */
    StoredObject confirmUpload(String key);

    /** Public CDN URL, or a time-limited presigned URL for private objects. */
    String resolveUrl(String key, FileCategory category);

    String presignedDownloadUrl(String key, Duration ttl);

    void delete(String key);

    boolean exists(String key);
}
