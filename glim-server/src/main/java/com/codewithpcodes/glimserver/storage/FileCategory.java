package com.codewithpcodes.glimserver.storage;

import lombok.Getter;

import java.util.Set;

@Getter
public enum FileCategory {

    SERMON_AUDIO("sermons/audio", 120 * 1024 * 1024L, Visibility.PUBLIC,
            Set.of("audio/mpeg", "audio/mp4", "audio/aac", "audio/ogg")),

    DEVOTIONAL_AUDIO("devotionals/audio", 40 * 1024 * 1024L, Visibility.PUBLIC,
            Set.of("audio/mpeg", "audio/mp4", "audio/aac", "audio/ogg")),

    DEVOTIONAL_IMAGE("devotionals/images", 5 * 1024 * 1024L, Visibility.PUBLIC,
            Set.of("image/jpeg", "image/png", "image/webp")),

    ANNOUNCEMENT_IMAGE("announcements", 5 * 1024 * 1024L, Visibility.PUBLIC,
            Set.of("image/jpeg", "image/png", "image/webp")),

    EVENT_IMAGE("events", 5 * 1024 * 1024L, Visibility.PUBLIC,
            Set.of("image/jpeg", "image/png", "image/webp")),

    MINISTRY_IMAGE("ministries", 5 * 1024 * 1024L, Visibility.PUBLIC,
            Set.of("image/jpeg", "image/png", "image/webp")),

    VERSE_SHARE_IMAGE("verses/share", 3 * 1024 * 1024L, Visibility.PUBLIC,
            Set.of("image/jpeg", "image/png", "image/webp")),

    AVATAR("avatars", 2 * 1024 * 1024L, Visibility.PUBLIC,
            Set.of("image/jpeg", "image/png", "image/webp")),

    PARTNER_IMPORT_CSV("imports/partners", 10 * 1024 * 1024L, Visibility.PRIVATE,
            Set.of("text/csv", "text/plain", "application/vnd.ms-excel"));

    private final String prefix;
    private final long maxBytes;
    private final Visibility visibility;
    private final Set<String> allowedContentTypes;

    FileCategory(String prefix, long maxBytes, Visibility visibility, Set<String> allowedContentTypes) {
        this.prefix = prefix;
        this.maxBytes = maxBytes;
        this.visibility = visibility;
        this.allowedContentTypes = allowedContentTypes;
    }

    public enum Visibility { PUBLIC, PRIVATE }
}
