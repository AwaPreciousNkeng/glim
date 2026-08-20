package com.codewithpcodes.glimserver.notification;

import java.lang.reflect.Type;
import java.util.UUID;

public record Audience(Type type, UUID reference) {
    public enum Type { ALL_MEMBERS, MINISTRY, PARTNERS_ACTIVE, SINGLE_USER }

    public static Audience allMembers() { return new Audience(Type.ALL_MEMBERS, null); }
    public static Audience ministry(UUID id) { return new Audience(Type.MINISTRY, id); }
    public static Audience activePartners() { return new Audience(Type.PARTNERS_ACTIVE, null); }
}
