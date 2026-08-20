package com.codewithpcodes.glimserver.notification;

import com.codewithpcodes.glimserver.notification.twilio.ValidBroadcast;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ValidBroadcast
public record BroadcastRequest(
        /* Must be a type marked broadcastable — enforced in the dispatcher. */
        @NotNull
        NotificationType type,

        @NotNull
        Audience.Type audienceType,

        /* Required when audienceType is MINISTRY or SINGLE_USER. */
        UUID audienceRef,

        /*
         * Fills the {0}, {1}… placeholders in the message template.
         * NEW_ANNOUNCEMENT expects [title, body].
         * SERVICE_LIVE expects [serviceName].
         * EVENT_REMINDER expects [eventName, dayLabel, time].
         */
        @NotNull
        @Size(max = 6)
        List<String> variables,

        @Size(max = 500) String deepLink,

        /* Null or past = send immediately. Future = queue it. */
        @Future
        Instant scheduledFor
) {
}
