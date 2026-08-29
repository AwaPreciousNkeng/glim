package com.codewithpcodes.glimserver.notification;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;


public record BroadcastRequest(
        /* Must be a type marked broadcastable — enforced in the dispatcher. */
        @NotNull(message = "Notification type is required")
        NotificationType type,

        @NotNull(message = "Audience Type is required")
        Audience.Type audienceType,

        // Required when audienceType is MINISTRY or SINGLE_USER
        UUID audienceRef,

        /*
         * Fills the {0}, {1}… placeholders in the message template.
         * NEW_ANNOUNCEMENT expects [title, body].
         * SERVICE_LIVE expects [serviceName].
         * EVENT_REMINDER expects [eventName, dayLabel, time].
         */
        @NotNull(message = "Variables are required.")
        @Size(max = 6, message = "The max number of variables is 6.")
        List<String> variables,

        @Size(max = 500, message = "The max length of the deep link is 500 characters.")
        String deepLink,

        // Null or past = send immediately. Future = queue it.
        @Future
        Instant scheduledFor
) {
        public boolean isScheduled() {
                return scheduledFor != null && scheduledFor.isAfter(Instant.now());
        }
}
