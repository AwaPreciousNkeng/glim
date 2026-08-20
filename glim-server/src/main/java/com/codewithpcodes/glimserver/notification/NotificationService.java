package com.codewithpcodes.glimserver.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationDispatcher dispatcher;

    /**
     * Send to one member. BLOCKING types (OTP) propagate failure so the
     * caller's transaction rolls back. ASYNC types return immediately.
     */
    public void notify(NotificationDispatcher.Recipient recipient, NotificationType type,
                       String deepLink, Object... args) {

        if (type.getMode() == NotificationType.Mode.BLOCKING) {
            dispatcher.dispatchToOne(recipient, type, deepLink, args);
        } else {
            notifyAsync(recipient, type, deepLink, args);
        }
    }

    @Async("notificationExecutor")
    public void notifyAsync(NotificationDispatcher.Recipient recipient, NotificationType type,
                            String deepLink, Object... args) {
        try {
            dispatcher.dispatchToOne(recipient, type, deepLink, args);
        } catch (Exception e) {
            // Fire and forget, per your decision. Logged, never retried,
            // never allowed to break the caller.
            log.error("Notification {} failed for user {}", type, recipient.userId(), e);
        }
    }

    /** Returns the batch id so the admin stats screen can look it up. */
    @Async("notificationExecutor")
    public void broadcast(Audience audience, NotificationType type,
                          String deepLink, Object... args) {
        try {
            dispatcher.dispatchToAudience(audience, type, deepLink, args);
        } catch (Exception e) {
            log.error("Broadcast {} failed", type, e);
        }
    }

    public UUID broadcastNow(Audience audience, NotificationType type,
                             String deepLink, Object... args) {
        return dispatcher.dispatchToAudience(audience, type, deepLink, args);
    }
}