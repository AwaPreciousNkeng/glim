package com.codewithpcodes.glimserver.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    /* Everything that ever happened to one record — the dispute view. */
    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, UUID entityId);

    /* Everything one person did — the accountability view. */
    Page<AuditLog> findByActorIdOrderByCreatedAtDesc(UUID actorId, Pageable pageable);

    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:action     IS NULL OR a.action = :action)
          AND (:entityType IS NULL OR a.entityType = :entityType)
          AND (:actorId    IS NULL OR a.actorId = :actorId)
          AND (:from       IS NULL OR a.createdAt >= :from)
          AND (:to         IS NULL OR a.createdAt <= :to)
        ORDER BY a.createdAt DESC
        """)
    Page<AuditLog> search(@Param("action") String action,
                          @Param("entityType") String entityType,
                          @Param("actorId") UUID actorId,
                          @Param("from") Instant from,
                          @Param("to") Instant to,
                          Pageable pageable);
}
