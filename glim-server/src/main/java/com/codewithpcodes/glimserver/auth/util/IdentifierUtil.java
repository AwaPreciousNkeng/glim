package com.codewithpcodes.glimserver.auth.util;

import com.codewithpcodes.glimserver.auth.IdentifierType;
import lombok.experimental.UtilityClass;

import java.util.Locale;
import java.util.regex.Pattern;

@UtilityClass
public class IdentifierUtil {

    private static final Pattern EMAIL =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[\\w.-]{2,}$");

    public static IdentifierType typeOf(String raw) {
        return EMAIL.matcher(raw.trim()).matches()
                ? IdentifierType.EMAIL
                : IdentifierType.PHONE;
    }

    public static String normalise(String raw, String defaultCountryCode) {
        String value = raw.trim();
        if (typeOf(value) == IdentifierType.EMAIL) {
            return value.toLowerCase(Locale.ROOT);
        }
        String digits = value.replaceAll("[^0-9+]", "");
        if (digits.startsWith("+")) return digits;
        if (digits.startsWith(defaultCountryCode)) return "+" + digits;
        if (digits.startsWith("0")) digits = digits.substring(1);
        return "+" + defaultCountryCode + digits;
    }

    public static String mask(String identifier) {
        if (typeOf(identifier) == IdentifierType.EMAIL) {
            int at = identifier.indexOf('@');
            return identifier.charAt(0) + "***" + identifier.substring(at);
        }
        return identifier.substring(0, 5) + " *** " + identifier.substring(identifier.length() - 3);
    }
}
