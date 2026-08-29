package com.codewithpcodes.glimserver.notification.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ScheduledNotificationRepository extends JpaRepository<ScheduledNotification, UUID> {

    @Query(value = """
        SELECT * FROM scheduled_notifications
        WHERE status = 'PENDING' AND scheduled_for <= :now
        ORDER BY scheduled_for
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<ScheduledNotification> lockDueBatch(@Param("now") Instant now, @Param("limit") int limit);

    List<ScheduledNotification> findByStatusOrderByScheduledForAsc(String status);

    long countByStatus(String status);
}
