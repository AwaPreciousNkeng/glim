package com.codewithpcodes.glimserver.notification;

import com.google.firebase.messaging.*;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FcmPushSender {

    private static final int FCM_BATCH_LIMIT = 500;

    private final DeviceTokenRepository deviceTokenRepository;

    public record PushOutcome(UUID userId, boolean success, String messageId, String errorCode) { }

    @Transactional
    public List<PushOutcome> send(List<DeviceToken> devices, String title, String body, String deepLink) {
        List<PushOutcome> outcomes = new ArrayList<>();
        if (devices.isEmpty()) return outcomes;

        for (int start = 0; start < devices.size(); start += FCM_BATCH_LIMIT) {
            List<DeviceToken> chunk = devices.subList(start, Math.min(start + FCM_BATCH_LIMIT, devices.size()));
            MulticastMessage message = MulticastMessage.builder()
                    .addAllFids(chunk.stream().map(DeviceToken::getFcmToken).toList())
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("url", deepLink == null ? "/" : deepLink)
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setIcon("ic_notification")
                                    .setColor("#1B6B3A")
                                    .setChannelId("glim.default")
                                    .build())
                            .build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder().setSound("default").build())
                            .build())
                    .build();
            try {
                BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
                outcomes.addAll(processResponse(chunk, response));
            } catch (FirebaseMessagingException e) {
                log.error("FCM batch failed entirely", e);
                chunk.forEach(d -> outcomes.add(new PushOutcome(d.getUserId(), false, null, "BATCH_FAILURE")));
            }
        }
        return outcomes;
    }

    private List<PushOutcome> processResponse(List<DeviceToken> chunk, BatchResponse response) {
        List<PushOutcome> outcomes = new ArrayList<>();
        List<DeviceToken> dead = new ArrayList<>();

        for (int i = 0; i < chunk.size(); i++) {
            DeviceToken device = chunk.get(i);
            SendResponse result = response.getResponses().get(i);

            if (result.isSuccessful()) {
                device.setFailureCount((short) 0);
                device.setLastUsedAt(Instant.now());
                outcomes.add(new PushOutcome(device.getUserId(), true, result.getMessageId(), null));
                continue;
            }

            var code = result.getException().getMessagingErrorCode();
            outcomes.add(new PushOutcome(device.getUserId(), false, null,
                    code == null ? "UNKNOWN" : code.name()));

            // UNREGISTERED = app uninstalled. INVALID_ARGUMENT = token malformed.
            // Neither will ever recover, so delete rather than retry forever.
            if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                dead.add(device);
            } else {
                device.setFailureCount((short) (device.getFailureCount() + 1));
                if (device.getFailureCount() >= 5) dead.add(device);
            }
        }

        if (!dead.isEmpty()) {
            deviceTokenRepository.deleteAll(dead);
            log.info("Removed {} dead device tokens", dead.size());
        }
        return outcomes;
    }
}
