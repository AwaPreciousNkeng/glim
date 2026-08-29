package com.codewithpcodes.glimserver.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Map;
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

    @Query("""
        SELECT d FROM NotificationDelivery d
        WHERE d.batchId = :batchId AND d.status = :status
        """)
    List<NotificationDelivery> findByBatchAndStatus(@Param("batchId") UUID batchId,
                                                    @Param("status") String status);

    /** Rolling health check across all pushes in a window. */
    @Query(value = """
        SELECT COUNT(*) FILTER (WHERE status = 'SENT')      AS sent,
               COUNT(*) FILTER (WHERE status = 'FAILED')    AS failed,
               COUNT(*) FILTER (WHERE status = 'NO_DEVICE') AS noDevice
        FROM notification_deliveries
        WHERE channel = 'PUSH' AND created_at > :since
        """, nativeQuery = true)
    Map<String, Object> healthSince(@Param("since") Instant since);

    @Modifying
    @Query("DELETE FROM NotificationDelivery d WHERE d.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
