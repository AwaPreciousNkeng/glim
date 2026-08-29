package com.codewithpcodes.glimserver.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    long countByUserIdAndReadAtIsNull(UUID userId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE Notification n set n.readAt = :now where n.userId = :userId and n.readAt is null ")
    void markAllRead(@Param("userId") UUID userId, @Param("now") Instant now);
}
