package com.codewithpcodes.glimserver.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/audit")
@PreAuthorize("hasAnyRole('ADMIN', 'PASTOR')")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    public Page<AuditLog> search(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID actionID,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(size = 50) Pageable pageable
    ) {
        return auditLogRepository.search(action, entityType, actionID, from, to, pageable);
    }

    @GetMapping("/actors/{actorId}")
    public Page<AuditLog> byActor(
            @PathVariable UUID actorId,
            @PageableDefault(size = 50) Pageable pageable
    ) {
        return auditLogRepository.findByActorIdOrderByCreatedAtDesc(actorId, pageable);
    }
}
