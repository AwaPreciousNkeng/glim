package com.codewithpcodes.glimserver.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, UUID> {

    /** FILTER is Postgres syntax for conditional aggregation — one pass, no subqueries. */
    @Query(value = """
        SELECT batch_id            AS batchId,
               type                AS type,
               MIN(created_at)     AS sentAt,
               COUNT(DISTINCT user_id) AS recipients,
               COUNT(*) FILTER (WHERE status = 'SENT')              AS sent,
               COUNT(*) FILTER (WHERE status = 'FAILED')            AS failed,
               COUNT(*) FILTER (WHERE status = 'NO_DEVICE')         AS noDevice,
               COUNT(*) FILTER (WHERE status = 'SKIPPED_OPTED_OUT') AS optedOut
        FROM notification_deliveries
        WHERE channel = 'PUSH'
        GROUP BY batch_id, type
        ORDER BY MIN(created_at) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<BatchStatsProjection> recentBatches(@Param("limit") int limit);

    @Query(value = """
        SELECT batch_id AS batchId, type AS type, MIN(created_at) AS sentAt,
               COUNT(DISTINCT user_id) AS recipients,
               COUNT(*) FILTER (WHERE status = 'SENT')              AS sent,
               COUNT(*) FILTER (WHERE status = 'FAILED')            AS failed,
               COUNT(*) FILTER (WHERE status = 'NO_DEVICE')         AS noDevice,
               COUNT(*) FILTER (WHERE status = 'SKIPPED_OPTED_OUT') AS optedOut
        FROM notification_deliveries
        WHERE batch_id = :batchId AND channel = 'PUSH'
        GROUP BY batch_id, type
        """, nativeQuery = true)
    Optional<BatchStatsProjection> statsFor(@Param("batchId") UUID batchId);

    Optional<NotificationDelivery> findByProviderId(String sid);
}
