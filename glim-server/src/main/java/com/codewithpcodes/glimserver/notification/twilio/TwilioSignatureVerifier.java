package com.codewithpcodes.glimserver.notification.twilio;

import com.twilio.security.RequestValidator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TwilioSignatureVerifier {

    private final TwilioProperties props;
    private RequestValidator validator;

    private RequestValidator validator() {
        if (validator == null) validator = new RequestValidator(props.getAuthToken());
        return validator;
    }

    public boolean isValid(HttpServletRequest request, Map<String, String> form) {
        String signature = request.getHeader("X-Twilio-Signature");
        if (signature == null) return false;

        boolean valid = validator().validate(
                props.getStatusCallbackUrl(), new HashMap<>(form), signature);

        if (!valid) log.warn("Rejected Twilio webhook with bad signature from {}",
                request.getRemoteAddr());
        return valid;
    }
}
