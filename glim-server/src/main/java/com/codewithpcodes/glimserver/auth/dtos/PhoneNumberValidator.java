package com.codewithpcodes.glimserver.auth.dtos;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {

    private String defaultRegion;

    @Override
    public void initialize(ValidPhoneNumber constraintAnnotation) {
        this.defaultRegion = constraintAnnotation.defaultRegion();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }

        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        try {
            Phonenumber.PhoneNumber numberProto = phoneNumberUtil.parse(value, defaultRegion);
            return phoneNumberUtil.isValidNumber(numberProto);
        } catch (NumberParseException e) {
            return false;
        }
    }
}
