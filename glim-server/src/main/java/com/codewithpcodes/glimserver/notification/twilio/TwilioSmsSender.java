package com.codewithpcodes.glimserver.notification.twilio;

import com.codewithpcodes.glimserver.exceptions.SmsDeliveryException;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;

@Slf4j
@Component
@RequiredArgsConstructor
public class TwilioSmsSender implements SmsSender {

    private final TwilioProperties props;


    @Override
    public void send(String toE164, String body) {
        try {
            Message message = create(toE164, body);
            log.info("SMS queued to {} — sid={} segments={}",
                    mask(toE164), message.getSid(), message.getNumSegments());
        } catch (ApiException e) {
            log.error("Twilio rejected SMS to {} — code={}", mask(toE164), e.getCode());
            throw new SmsDeliveryException(friendly(e.getCode()), e);
        } catch (Exception e) {
            log.error("Unexpected SMS failure to {}", mask(toE164), e);
            throw new SmsDeliveryException("Could not send the message. Please try again.", e);
        }
    }

    @Override
    public SmsOutcome sendAndReport(String toE164, String body) {
        try {
            Message message = create(toE164, body);
            return new SmsOutcome(true, message.getSid(), null);
        } catch (ApiException e) {
            log.warn("SMS failed to {} — code={}", mask(toE164), e.getCode());
            return new SmsOutcome(false, null, String.valueOf(e.getCode()));
        } catch (Exception e) {
            log.warn("SMS failed to {}", mask(toE164), e);
            return new SmsOutcome(false, null, "UNKNOWN");
        }
    }

    private Message create(String toE164, String body) {
        PhoneNumber to = new PhoneNumber(toE164);
        MessageCreator creator;

        if (props.getMessagingServiceSid() != null && !props.getMessagingServiceSid().isBlank()) {
            creator = Message.creator(to, props.getMessagingServiceSid(), body);
        } else if (props.getFromNumber() != null && !props.getFromNumber().isBlank()) {
            creator = Message.creator(to, new PhoneNumber(props.getFromNumber()), body);
        } else {
            throw new IllegalStateException(
                    "Configure glim.twilio.messaging-service-sid or from-number.");
        }

        if (props.getStatusCallbackUrl() != null && !props.getStatusCallbackUrl().isBlank()) {
            creator.setStatusCallback(URI.create(props.getStatusCallbackUrl()));
        }

        return creator.create();
    }

    private String friendly(int code) {
        return switch (code) {
            case 21211 -> "That phone number is not valid.";
            case 21408, 21606 -> "We cannot send messages to that number yet.";
            case 21610 -> "That number has opted out of our messages.";
            case 21614 -> "That number cannot receive SMS.";
            case 20003 -> "Messaging is temporarily unavailable. Please try again shortly.";
            default -> "Could not send the message. Please try again.";
        };
    }

    private String mask(String e164) {
        return e164 == null || e164.length() < 8 ? "***"
                : e164.substring(0, 5) + "***" + e164.substring(e164.length() - 2);
    }
}
