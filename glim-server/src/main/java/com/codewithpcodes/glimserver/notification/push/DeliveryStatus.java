package com.codewithpcodes.glimserver.notification.push;

public enum DeliveryStatus {
    SENT,               // provider accepted it
    FAILED,             // provider rejected it
    NO_DEVICE,          // member has no app installed / no phone
    SKIPPED_OPTED_OUT   // member turned this category off
}
