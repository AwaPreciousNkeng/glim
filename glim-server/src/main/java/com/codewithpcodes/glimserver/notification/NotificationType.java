package com.codewithpcodes.glimserver.notification;

import lombok.Getter;

import java.util.Set;
import java.util.function.Function;

@Getter
public enum NotificationType {
    OTP_REGISTRATION("otp.registration",
            Set.of(Channel.SMS), Mode.BLOCKING, null, false, false),

    OTP_PASSWORD_RESET("otp.password.reset",
            Set.of(Channel.SMS), Mode.BLOCKING, null, false, false),

    STAFF_INVITATION("staff.invitation",
            Set.of(Channel.SMS), Mode.BLOCKING, null, false, false),

    PAYMENT_SUCCESS("payment.success",
            Set.of(Channel.PUSH, Channel.INBOX), Mode.ASYNC, null, false, true),

    PAYMENT_FAILED("payment.failed",
            Set.of(Channel.PUSH, Channel.INBOX), Mode.ASYNC, null, false, true),

    PARTNERSHIP_REMINDER("partnership.reminder",
            Set.of(Channel.PUSH, Channel.INBOX), Mode.ASYNC,
            NotificationPreference::isPartnershipReminders, false, true),

    PARTNERSHIP_LAPSED("partnership.lapsed",
            Set.of(Channel.PUSH, Channel.INBOX), Mode.ASYNC,
            NotificationPreference::isPartnershipReminders, false, true),

    NEW_ANNOUNCEMENT("announcement.new",
            Set.of(Channel.PUSH, Channel.INBOX), Mode.ASYNC,
            NotificationPreference::isAnnouncements, true, true),

    DAILY_DEVOTIONAL("devotional.daily",
            Set.of(Channel.PUSH), Mode.ASYNC,
            NotificationPreference::isDevotionals, true, false),

    SERVICE_LIVE("service.live",
            Set.of(Channel.PUSH), Mode.ASYNC,
            NotificationPreference::isEvents, true, false),

    EVENT_REMINDER("event.reminder",
            Set.of(Channel.PUSH, Channel.INBOX), Mode.ASYNC,
            NotificationPreference::isEvents, true, true),

    TESTIMONY_APPROVED("testimony.approved",
            Set.of(Channel.PUSH, Channel.INBOX), Mode.ASYNC, null, false, true);

    private final String messageKey;
    private final Set<Channel> channels;
    private final Mode mode;
    /** null means this type cannot be opted out of. */
    private final Function<NotificationPreference, Boolean> preferenceAccessor;
    private final boolean broadcastable;
    private final boolean storedInInbox;

    NotificationType(String messageKey, Set<Channel> channels, Mode mode,
                     Function<NotificationPreference, Boolean> accessor,
                     boolean broadcastable, boolean storedInInbox) {
        this.messageKey = messageKey;
        this.channels = channels;
        this.mode = mode;
        this.preferenceAccessor = accessor;
        this.broadcastable = broadcastable;
        this.storedInInbox = storedInInbox;
    }

    public boolean isOptOutAllowed() { return preferenceAccessor != null; }

    public enum Channel { SMS, PUSH, INBOX }
    public enum Mode { BLOCKING, ASYNC }
}
