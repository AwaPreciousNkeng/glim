package com.codewithpcodes.glimserver.notification.twilio;

import com.codewithpcodes.glimserver.notification.NotificationDeliveryRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks/twilio")
@RequiredArgsConstructor
public class TwilioWebhookController {

    private final TwilioSignatureVerifier verifier;
    private final NotificationDeliveryRepository deliveryRepository;

    @PostMapping(value = "/status", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Transactional
    public ResponseEntity<Void> status(HttpServletRequest request,
                                       @RequestParam Map<String, String> form) {

        if (!verifier.isValid(request, form)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String sid = form.get("MessageSid");
        String status = form.get("MessageStatus");
        String errorCode = form.get("ErrorCode");

        deliveryRepository.findByProviderId(sid).ifPresent(delivery -> {
            delivery.setStatus(mapStatus(status));
            delivery.setErrorCode(errorCode);
        });

        if ("failed".equals(status) || "undelivered".equals(status)) {
            log.warn("SMS {} failed — error {}", sid, errorCode);
        }

        return ResponseEntity.ok().build();
    }

    private String mapStatus(String twilioStatus) {
        return switch (twilioStatus) {
            case "failed", "undelivered" -> "FAILED";
            default -> "SENT";
        };
    }
}
