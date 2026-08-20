package com.codewithpcodes.glimserver.notification.schedule;

import com.codewithpcodes.glimserver.notification.BroadcastRequest;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "scheduled_notifications",
        indexes = @Index(name = "idx_scheduled_due", columnList = "status, scheduled_for"))
public class ScheduledNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(name = "audience_type", nullable = false, length = 20)
    private String audienceType;

    @Column(name = "audience_ref")
    private UUID audienceRef;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private List<String> variables = new ArrayList<>();

    @Column(name = "deep_link", length = 500)
    private String deepLink;

    @Column(name = "scheduled_for", nullable = false)
    private Instant scheduledFor;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";   // PENDING | SENT | FAILED | CANCELLED

    @Column(name = "batch_id")
    private UUID batchId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "dispatched_at")
    private Instant dispatchedAt;

    @Column(columnDefinition = "text")
    private String error;

    public Object[] variableArray() {
        return variables == null ? new Object[0] : variables.toArray();
    }

    public static ScheduledNotification from(BroadcastRequest request, UUID createdBy) {
        return ScheduledNotification.builder()
                .type(request.type().name())
                .audienceType(request.audienceType().name())
                .audienceRef(request.audienceRef())
                .variables(new ArrayList<>(request.variables()))
                .deepLink(request.deepLink())
                .scheduledFor(request.scheduledFor())
                .createdBy(createdBy)
                .build();
    }
}
