package com.codewithpcodes.glimserver.notification.schedule;

import com.codewithpcodes.glimserver.notification.Audience;
import com.codewithpcodes.glimserver.notification.NotificationService;
import com.codewithpcodes.glimserver.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledNotificationRunner {

    private final ScheduledNotificationRepository repository;
    private final NotificationService notificationService;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void dispatchDue() {
        var due = repository.lockDueBatch(Instant.now(), 10);

        for (var scheduled : due) {
            try {
                var batchId = notificationService.broadcastNow(
                        new Audience(Audience.Type.valueOf(scheduled.getAudienceType()),
                                scheduled.getAudienceRef()),
                        NotificationType.valueOf(scheduled.getType()),
                        scheduled.getDeepLink(),
                        scheduled.variableArray());

                scheduled.setStatus("SENT");
                scheduled.setBatchId(batchId);
                scheduled.setDispatchedAt(Instant.now());

            } catch (Exception e) {
                log.error("Scheduled notification {} failed", scheduled.getId(), e);
                scheduled.setStatus("FAILED");
                scheduled.setError(e.getMessage());
            }
        }
    }
}
