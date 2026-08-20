package com.codewithpcodes.glimserver.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    default NotificationPreference findByIdOrDefault(UUID userId) {
        return findById(userId).orElseGet(() -> NotificationPreference.defaultsFor(userId));
    }
}
