package com.codewithpcodes.glimserver.notification;

import com.codewithpcodes.glimserver.notification.schedule.ScheduledNotification;
import com.codewithpcodes.glimserver.notification.schedule.ScheduledNotificationRepository;
import com.codewithpcodes.glimserver.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
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
    public ResponseEntity<Map<String, Object>> broadcast(@AuthenticationPrincipal User principal,
                                       @RequestBody @Valid BroadcastRequest request) {

        if (request.isScheduled()) {
            var saved = scheduledRepository.save(
                    ScheduledNotification.from(request, principal.getId())
            );

            return ResponseEntity.accepted().body(Map.of(
                    "scheduled", true,
                    "scheduledId", saved.getId(),
                    "scheduledFor", saved.getScheduledFor()
            ));
        }
        UUID batchId =  notificationService.broadcastNow(
                 new Audience(request.audienceType(), request.audienceRef()),
                request.type(),
                request.deepLink(),
                request.variables().toArray()
        );

        return deliveryRepository.statsFor(batchId)
                .map(p ->
                        ResponseEntity.ok(Map.of("scheduled", false, "stats", BatchStats.from(p))))
                .orElseGet(() -> ResponseEntity.ok(Map.of("scheduled", false, "batchId", batchId)));
    }

    @GetMapping("/batches")
    @PreAuthorize("hasAnyRole('PASTOR','ADMIN','MEDIA')")
    public List<BatchStats> batches(@RequestParam(defaultValue = "20") int limit) {
        return deliveryRepository.recentBatches(Math.min(limit, 100)).stream()
                .map(BatchStats::from)
                .toList();
    }

    @GetMapping("/batches/{batchId}")
    @PreAuthorize("hasAnyRole('PASTOR','SUPER_ADMIN','MEDIA')")
    public ResponseEntity<BatchStats> batch(@PathVariable UUID batchId) {
        return deliveryRepository.statsFor(batchId)
                .map(BatchStats::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // "Who didn't get it" — user ids only, so the panel can resolve names.
    @GetMapping("/batches/{batchId}/failures")
    @PreAuthorize("hasAnyRole('PASTOR','ADMIN')")
    public List<Map<String, Object>> failures(@PathVariable UUID batchId,
                                              @RequestParam(defaultValue = "FAILED") String status) {
        return deliveryRepository.findByBatchAndStatus(batchId, status).stream()
                .map(d -> Map.<String, Object>of(
                        "userId", d.getUserId(),
                        "channel", d.getChannel(),
                        "errorCode", d.getErrorCode() == null ? "" : d.getErrorCode()))
                .toList();
    }

    // Rolling 30-day push health — the number to watch.
    @GetMapping("/health")
    @PreAuthorize("hasAnyRole('PASTOR','ADMIN','MEDIA')")
    public Map<String, Object> health() {
        return deliveryRepository.healthSince(Instant.now().minus(30, ChronoUnit.DAYS));
    }

    // ---------------- scheduled ----------------

    @GetMapping("/scheduled")
    @PreAuthorize("hasAnyRole('PASTOR','ADMIN','MEDIA')")
    public List<ScheduledNotification> pending() {
        return scheduledRepository.findByStatusOrderByScheduledForAsc("PENDING");
    }

    @DeleteMapping("/scheduled/{id}")
    @PreAuthorize("hasAnyRole('PASTOR','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable UUID id) {
        scheduledRepository.findById(id)
                .filter(s -> "PENDING".equals(s.getStatus()))   // can't cancel one already sent
                .ifPresent(s -> s.setStatus("CANCELLED"));
    }

    // What the admin UI needs to build its broadcast form.
    @GetMapping("/types")
    @PreAuthorize("hasAnyRole('PASTOR','ADMIN','MEDIA')")
    public List<Map<String, Object>> broadcastableTypes() {
        return Arrays.stream(NotificationType.values())
                .filter(NotificationType::isBroadcastable)
                .map(t -> Map.of(
                        "type", t.name(),
                        "channels", t.getChannels().stream().map(Enum::name).toList(),
                        "optOutAllowed", t.isOptOutAllowed()))
                .toList();
    }
}
