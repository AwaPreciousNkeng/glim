package com.codewithpcodes.glimserver.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "partnership_reminders", nullable = false)
    @Builder.Default
    private boolean partnershipReminders = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean announcements = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean devotionals = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean events = true;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    /** Used when a member has no stored row yet. */
    public static NotificationPreference defaultsFor(UUID userId) {
        return NotificationPreference.builder().userId(userId).build();
    }
}
