package com.codewithpcodes.glimserver.notification;

import com.codewithpcodes.glimserver.notification.schedule.ScheduledNotification;
import com.codewithpcodes.glimserver.notification.schedule.ScheduledNotificationRepository;
import com.codewithpcodes.glimserver.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationService notificationService;
    private final ScheduledNotificationRepository scheduledRepository;
    private final NotificationDeliveryRepository deliveryRepository;

    @PostMapping("/broadcast")
    @PreAuthorize("hasAnyRole('PASTOR','ADMIN')")
    public ResponseEntity<?> broadcast(@AuthenticationPrincipal User principal,
                                       @RequestBody @Valid BroadcastRequest request) {

        if (request.scheduledFor() != null && request.scheduledFor().isAfter(Instant.now())) {
            var saved = scheduledRepository.save(ScheduledNotification.from(request, principal.getId()));
            return ResponseEntity.accepted().body(Map.of("scheduledId", saved.getId()));
        }

        UUID batchId = notificationService.broadcastNow(
                new Audience(request.audienceType(), request.audienceRef()),
                request.type(), request.deepLink(), request.variables().toArray());

        return ResponseEntity.ok(Map.of("batchId", batchId));
    }

    @GetMapping("/batches")
    @PreAuthorize("hasAnyRole('PASTOR','ADMIN','MEDIA')")
    public List<BatchStatsProjection> batches(@RequestParam(defaultValue = "20") int limit) {
        return deliveryRepository.recentBatches(limit);
    }

    @DeleteMapping("/scheduled/{id}")
    @PreAuthorize("hasAnyRole('PASTOR','ADMIN')")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        scheduledRepository.findById(id)
                .filter(s -> "PENDING".equals(s.getStatus()))
                .ifPresent(s -> s.setStatus("CANCELLED"));
        return ResponseEntity.noContent().build();
    }
}
