package com.codewithpcodes.glimserver.notification;

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
@Table(name = "notification_deliveries",
        indexes = {
                @Index(name = "idx_deliveries_batch", columnList = "batch_id"),
                @Index(name = "idx_deliveries_type_time", columnList = "type, created_at")
        })
public class NotificationDelivery {

    @Id
    @GeneratedValue
    private UUID id;

    /** Groups every row from a single broadcast. Drives the stats screen. */
    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false, length = 10)
    private String channel;          // SMS | PUSH | INBOX

    @Column(nullable = false, length = 20)
    private String status;           // SENT | FAILED | NO_DEVICE | SKIPPED_OPTED_OUT

    /** Twilio SID or FCM message id — for tracing a specific message. */
    @Column(name = "provider_id", length = 120)
    private String providerId;

    @Column(name = "error_code", length = 40)
    private String errorCode;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
