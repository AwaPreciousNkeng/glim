package com.codewithpcodes.glimserver.notification;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidBroadcastValidator implements ConstraintValidator<ValidBroadcast, BroadcastRequest> {


    @Override
    public boolean isValid(BroadcastRequest request, ConstraintValidatorContext context) {
        if (request == null) return true;

        boolean needsRef = request.audienceType() == Audience.Type.MINISTRY
                || request.audienceType() == Audience.Type.SINGLE_USER;

        if (needsRef && request.audienceRef() == null) {
            return reject(context, "audienceRef is required for " + request.audienceType());
        }

        if (!request.type().isBroadcastable()) {
            return reject(context, request.type() + " cannot be broadcast.");
        }

        int expected = expectedVariableCount(request.type());
        if (expected >= 0 && request.variables().size() != expected) {
            return reject(context, request.type() + " expects " + expected + " variables.");
        }
        return true;
    }

    private int expectedVariableCount(NotificationType type) {
        return switch (type) {
            case NEW_ANNOUNCEMENT, PARTNERSHIP_REMINDER -> 2;
            case SERVICE_LIVE, DAILY_DEVOTIONAL -> 1;
            case EVENT_REMINDER -> 3;
            default -> -1;
        };
    }

    private boolean reject(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}
