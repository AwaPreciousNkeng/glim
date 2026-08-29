package com.codewithpcodes.glimserver.notification.push;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "device_tokens",
        indexes = @Index(name = "idx_device_tokens_user", columnList = "user_id"))
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "fcm_token", nullable = false, unique = true, length = 512)
    private String fcmToken;

    @Column(nullable = false, length = 10)
    private String platform;          // ANDROID | IOS

    @Column(name = "device_model", length = 120)
    private String deviceModel;

    @Column(name = "app_version", length = 20)
    private String appVersion;

    @Column(name = "failure_count", nullable = false)
    private short failureCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;
}
