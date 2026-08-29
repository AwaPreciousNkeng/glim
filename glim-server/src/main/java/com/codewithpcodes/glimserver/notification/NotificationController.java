package com.codewithpcodes.glimserver.notification;

import com.codewithpcodes.glimserver.notification.push.DeviceTokenRepository;
import com.codewithpcodes.glimserver.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationRepository inboxRepository;
    private final NotificationPreferenceRepository preferenceRepository;

    public record RegisterDeviceRequest(
            @NotBlank String fcmToken,
            @NotBlank @Pattern(regexp = "ANDROID|IOS") String platform,
            String deviceModel,
            String appVersion) {}

    public record PreferencesRequest(
            boolean partnershipReminders,
            boolean announcements,
            boolean devotionals,
            boolean events) {}

    /** Call on every app launch — FCM tokens rotate silently. */
    @PostMapping("/devices")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registerDevice(@AuthenticationPrincipal User principal,
                               @RequestBody @Valid RegisterDeviceRequest request) {
        deviceTokenRepository.upsert(principal.getId(), request.fcmToken(),
                request.platform(), request.deviceModel(), request.appVersion());
    }

    @DeleteMapping("/devices")
    public ResponseEntity<Void> removeDevice(@RequestParam String fcmToken) {
        deviceTokenRepository.deleteByFcmToken(fcmToken);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public Page<Notification> inbox(@AuthenticationPrincipal User principal,
                         @PageableDefault(size = 20) Pageable pageable) {
        return inboxRepository.findByUserIdOrderByCreatedAtDesc(principal.getId(), pageable);
    }

    @GetMapping("/unread-count")
    public long unreadCount(@AuthenticationPrincipal User principal) {
        return inboxRepository.countByUserIdAndReadAtIsNull(principal.getId());
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@AuthenticationPrincipal User principal,
                                         @PathVariable UUID id) {
        inboxRepository.findById(id)
                .filter(n -> n.getUserId().equals(principal.getId()))
                .ifPresent(n -> n.setReadAt(Instant.now()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(@AuthenticationPrincipal User principal) {
        inboxRepository.markAllRead(principal.getId(), Instant.now());
    }

    @GetMapping("/preferences")
    public NotificationPreference preferences(@AuthenticationPrincipal User principal) {
        return preferenceRepository.findByIdOrDefault(principal.getId());
    }

    @PatchMapping("/preferences")
    public ResponseEntity<Void> updatePreferences(@AuthenticationPrincipal User principal,
                                                  @RequestBody PreferencesRequest request) {
        preferenceRepository.save(NotificationPreference.builder()
                        .userId(principal.getId())
                        .partnershipReminders(request.partnershipReminders())
                        .announcements(request.announcements())
                        .devotionals(request.devotionals())
                        .events(request.events())
                .build());
        return ResponseEntity.noContent().build();
    }
}
