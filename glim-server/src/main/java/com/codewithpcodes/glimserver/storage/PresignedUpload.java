package com.codewithpcodes.glimserver.storage;

import java.time.Instant;

public record PresignedUpload(
        String key,
        String uploadUrl,
        String requiredContentType,
        long maxBytes,
        Instant expiresAt
) {
}
