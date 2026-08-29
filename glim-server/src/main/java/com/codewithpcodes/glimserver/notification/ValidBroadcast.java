package com.codewithpcodes.glimserver.notification;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidBroadcastValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidBroadcast {
    String message() default "Invalid broadcast request";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
