package com.codewithpcodes.glimserver.notification.twilio;

public interface SmsSender {
    /** Throws on failure. Used for OTP, where failure must break the request. */
    void send(String toE164, String message);

    /** Never throws. Used by the dispatcher, which records outcomes as data. */
    SmsOutcome sendAndReport(String toE164, String message);

    record SmsOutcome(boolean success, String sid, String errorCode) {}
}
