package com.codewithpcodes.glimserver.storage;

public record StoredObject(
        String key,
        String url,
        String contentType,
        long size,
        String etag
) {
}
