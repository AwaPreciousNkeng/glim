package com.codewithpcodes.glimserver.notification.twilio;

import com.codewithpcodes.glimserver.notification.BroadcastRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidBroadcastValidator implements ConstraintValidator<ValidBroadcast, BroadcastRequest> {

    @Override
    public boolean isValid(BroadcastRequest request, ConstraintValidatorContext context) {
        if (request == null) return true;

        boolean needsRef = request.audienceType() == Audience.Type.MINISTRY
                || request.audienceType() == Audience.Type.SINGLE_USER;

        if (needsRef && request.audienceRef() == null) {
            reject(context, "audienceRef is required for " + request.audienceType());
            return false;
        }

        if (!request.type().isBroadcastable()) {
            reject(context, request.type() + " cannot be broadcast.");
            return false;
        }

        return true;
    }
    private void reject(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
