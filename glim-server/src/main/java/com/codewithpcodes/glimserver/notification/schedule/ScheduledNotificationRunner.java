package com.codewithpcodes.glimserver.notification.schedule;

import com.codewithpcodes.glimserver.notification.Audience;
import com.codewithpcodes.glimserver.notification.NotificationDeliveryRepository;
import com.codewithpcodes.glimserver.notification.NotificationService;
import com.codewithpcodes.glimserver.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledNotificationRunner {

    private final ScheduledNotificationRepository repository;
    private final NotificationService notificationService;
    private final NotificationDeliveryRepository deliveryRepository;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void dispatchDue() {
        var due = repository.lockDueBatch(Instant.now(), 10);
        if (due.isEmpty()) return;

        for (var scheduled : due) {
            try {
                UUID batchId = notificationService.broadcastNow(
                        new Audience(Audience.Type.valueOf(scheduled.getAudienceType()),
                                scheduled.getAudienceRef()),
                        NotificationType.valueOf(scheduled.getType()),
                        scheduled.getDeepLink(),
                        scheduled.variableArray());

                scheduled.setStatus("SENT");
                scheduled.setBatchId(batchId);
                scheduled.setDispatchedAt(Instant.now());
                log.info("Dispatch scheduled notification {} as batch {}",
                        scheduled.getId(), batchId);

            } catch (Exception e) {
                log.error("Scheduled notification {} failed", scheduled.getId(), e);
                scheduled.setStatus("FAILED");
                scheduled.setError(truncate(e.getMessage()));
                scheduled.setDispatchedAt(Instant.now());
            }
        }
    }

    @Scheduled(cron = "0 0 4 * * SUN")
    @Transactional
    public void purgeOldDeliveries() {
        int removed = deliveryRepository.deleteOlderThan(Instant.now().minus(90, ChronoUnit.DAYS));
        if (removed > 0) log.info("Purged {} delivery records older than 90 days", removed);
    }

    private String truncate(String message) {
        if (message == null) return "Unknown error";
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
