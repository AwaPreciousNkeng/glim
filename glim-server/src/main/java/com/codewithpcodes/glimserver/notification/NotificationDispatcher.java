package com.codewithpcodes.glimserver.notification;

import com.codewithpcodes.glimserver.notification.email.EmailTemplateRenderer;
import com.codewithpcodes.glimserver.notification.email.SmtpEmailSender;
import com.codewithpcodes.glimserver.notification.push.DeliveryStatus;
import com.codewithpcodes.glimserver.notification.push.DeviceToken;
import com.codewithpcodes.glimserver.notification.push.DeviceTokenRepository;
import com.codewithpcodes.glimserver.notification.push.FcmPushSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final ResourceBundleMessageSource messages;
    private final FcmPushSender pushSender;
    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationRepository inboxRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final RecipientQueryRepository recipientRepository;
    private final SmtpEmailSender emailSender;
    private final EmailTemplateRenderer emailRenderer;




    // SINGLE RECIPIENT
    @Transactional
    public void dispatchToOne(Recipient recipient, NotificationType type,
                              String deepLink, Object... args) {

        UUID batchId = UUID.randomUUID();

        // Opt-out check. Absence of a preference row means "everything on",
        // so we never need to create rows for members who never change settings.
        if (type.isOptOutAllowed() && !recipientRepository.isTypeEnabled(recipient.userId(), type)) {
            recordDelivery(batchId, recipient.userId(), type,
                    NotificationType.Channel.PUSH, DeliveryStatus.SKIPPED_OPTED_OUT, null, null);
            return;
        }

        Rendered text = render(type, recipient.language(), args);
        deliver(batchId, List.of(recipient), type, text, deepLink);
    }

    // ---------------------------------------------------------------
    // BROADCAST
    // ---------------------------------------------------------------

    @Transactional
    public UUID dispatchToAudience(Audience audience, NotificationType type,
                                   String deepLink, Object... args) {

        if (!type.isBroadcastable()) {
            throw new IllegalArgumentException(type + " cannot be broadcast.");
        }

        UUID batchId = UUID.randomUUID();

        // ONE query: active members, their language and phone, already
        // filtered by their preference for this notification type.
        List<Recipient> recipients = recipientRepository.resolve(audience, type);

        if (recipients.isEmpty()) {
            log.info("Broadcast {} matched no recipients", type);
            return batchId;
        }

        // Render once per language, not once per member.
        Map<String, List<Recipient>> byLanguage = recipients.stream()
                .collect(Collectors.groupingBy(Recipient::language));

        byLanguage.forEach((language, group) -> {
            Rendered text = render(type, language, args);
            deliver(batchId, group, type, text, deepLink);
        });

        log.info("Broadcast {} dispatched to {} recipients (batch {})",
                type, recipients.size(), batchId);
        return batchId;
    }

    // ---------------------------------------------------------------
    // SHARED DELIVERY PATH
    // ---------------------------------------------------------------

    private void deliver(UUID batchId, List<Recipient> recipients,
                         NotificationType type, Rendered text, String deepLink) {

        if (type.getChannels().contains(NotificationType.Channel.PUSH)) {
            deliverPush(batchId, recipients, type, text, deepLink);
        }
        if (type.getChannels().contains(NotificationType.Channel.EMAIL)) {
            deliverEmail(batchId, recipients, type, text, deepLink);
        }
        if (type.getChannels().contains(NotificationType.Channel.INBOX) && type.isStoredInInbox()) {
            deliverInbox(batchId, recipients, type, text, deepLink);
        }
    }

    private void deliverEmail(UUID batchId, List<Recipient> recipients, NotificationType type,
                              Rendered text, String deepLink) {
        String actionUrl = deepLink == null ? null : absoluteUrl(deepLink);
        for (Recipient r : recipients) {
            if (r.email() == null) {
                recordDelivery(batchId, r.userId(), type,
                        NotificationType.Channel.EMAIL, DeliveryStatus.NO_DEVICE, null, null);
                continue;
            }
            emailSender.send(r.email(), text.title(),
                    emailRenderer.render(text.title(), text.body(), actionUrl, "Open GLIM City"));

            recordDelivery(batchId, r.userId(), type,
                    NotificationType.Channel.EMAIL, DeliveryStatus.SENT, null, null);
        }
    }

    private void deliverPush(UUID batchId, List<Recipient> recipients,
                             NotificationType type, Rendered text, String deepLink) {

        List<UUID> userIds = recipients.stream().map(Recipient::userId).toList();
        List<DeviceToken> devices = deviceTokenRepository.findByUserIdIn(userIds);

        // Members with an account but no installed app. Recorded deliberately —
        // this is the number that tells you whether push actually reaches
        // your congregation.
        Set<UUID> withDevice = devices.stream()
                .map(DeviceToken::getUserId).collect(Collectors.toSet());

        userIds.stream()
                .filter(id -> !withDevice.contains(id))
                .forEach(id -> recordDelivery(batchId, id, type,
                        NotificationType.Channel.PUSH, DeliveryStatus.NO_DEVICE, null, null));

        if (devices.isEmpty()) return;

        var outcomes = pushSender.send(devices, text.title(), text.body(), deepLink);

        outcomes.forEach(o -> recordDelivery(batchId, o.userId(), type,
                NotificationType.Channel.PUSH,
                o.success() ? DeliveryStatus.SENT : DeliveryStatus.FAILED,
                o.messageId(), o.errorCode()));
    }

    private void deliverInbox(UUID batchId, List<Recipient> recipients,
                              NotificationType type, Rendered text, String deepLink) {

        List<Notification> rows = recipients.stream()
                .map(r -> Notification.builder()
                        .userId(r.userId())
                        .type(type.name())
                        .title(text.title())
                        .body(text.body())
                        .deepLink(deepLink)
                        .build())
                .toList();

        inboxRepository.saveAll(rows);   // one batch insert, not 500 inserts
    }

    // ---------------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------------

    private record Rendered(String title, String body) {}

    private Rendered render(NotificationType type, String language, Object... args) {
        Locale locale = Locale.of(language == null ? "english" : language.toLowerCase());
        String title = messages.getMessage(type.getMessageKey() + ".title", args, locale);
        String body  = messages.getMessage(type.getMessageKey() + ".body",  args, locale);
        return new Rendered(title, body);
    }

    private String absoluteUrl(String deepLink) {
        return deepLink.startsWith("http") ? deepLink : null;
    }

    private boolean isEnabled(UUID userId, NotificationType type) {
        return recipientRepository.isTypeEnabled(userId, type);
    }

    private void recordDelivery(UUID batchId, UUID userId, NotificationType type,
                                NotificationType.Channel channel, DeliveryStatus status,
                                String providerId, String errorCode) {
        deliveryRepository.save(NotificationDelivery.builder()
                .batchId(batchId).userId(userId).type(type.name())
                .channel(channel.name()).status(status.name())
                .providerId(providerId).errorCode(errorCode)
                .build());
    }
}
