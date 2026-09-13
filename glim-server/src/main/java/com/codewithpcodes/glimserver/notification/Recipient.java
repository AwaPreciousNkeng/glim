package com.codewithpcodes.glimserver.notification;

import com.codewithpcodes.glimserver.user.User;

import java.util.UUID;

public record Recipient(
        UUID userId,
        String language,
        String email
) {
    public static Recipient from(User user) {
        return new Recipient(
                user.getId(),
                user.getLanguage().name(),
                user.getEmail()
        );
    }
}
